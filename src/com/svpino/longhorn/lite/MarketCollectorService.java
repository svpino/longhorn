/*
 * Copyright (C) 2012 Santiago Valdarrama
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package com.svpino.longhorn.lite;

import static com.svpino.longhorn.lite.artifacts.Extensions.fromStringToFloat;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.AlarmManager;
import android.app.IntentService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.util.Log;

import com.svpino.longhorn.lite.artifacts.Constants;
import com.svpino.longhorn.lite.artifacts.Extensions;
import com.svpino.longhorn.lite.data.DataProvider;
import com.svpino.longhorn.lite.receivers.ConnectivityBroadcastReceiver;

public class MarketCollectorService extends IntentService {

	private final static String LOG_TAG = MarketCollectorService.class.getName();

	public final static String EXTRA_TICKER = "ticker";

	private final static String FIELD_SYMBOL = "symbol";
	private final static String FIELD_CHANGE_PERCENTAGE = "ChangeinPercent";
	private final static String FIELD_CHANGE = "ChangeRealtime";
	private final static String FIELD_PRICE = "LastTradePriceOnly";
	private final static String FIELD_MARKET_CAPITAL = "MarketCapitalization";
	private final static String FIELD_VOLUME = "Volume";
	private final static String FIELD_AVERAGE_VOLUME = "AverageDailyVolume";
	private final static String FIELD_DAY_LOW = "DaysLow";
	private final static String FIELD_DAY_HIGH = "DaysHigh";
	private final static String FIELD_YEAR_LOW = "YearLow";
	private final static String FIELD_YEAR_HIGH = "YearHigh";
	private final static String FIELD_PE_RATIO = "PERatio";
	private final static String FIELD_EPS = "EarningsShare";
	private final static String FIELD_DIVIDEND = "DividendYield";
	private final static String FIELD_OPEN = "Open";
	private final static String FIELD_LAST_TRADE_DATE = "LastTradeDate";
	private final static String FIELD_TARGET_PRICE = "OneyrTargetPrice";

	public MarketCollectorService() {
		super("Collector Service");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		SharedPreferences sharedPreferences = getSharedPreferences(Constants.PREFERENCES, Context.MODE_PRIVATE);
		long lastUpdate = sharedPreferences.getLong(Constants.PREFERENCE_COLLECTOR_LAST_UPDATE, 0);
		boolean retrying = sharedPreferences.getBoolean(Constants.PREFERENCE_COLLECTOR_RETRYING, false);
		int retries = sharedPreferences.getInt(Constants.PREFERENCE_COLLECTOR_RETRIES, 0);
		boolean wereWeWaitingForConnectivity = sharedPreferences.getBoolean(Constants.PREFERENCE_STATUS_WAITING_FOR_CONNECTIVITY, false);

		boolean isGlobalCollection = intent.getExtras() == null || (intent.getExtras() != null && !intent.getExtras().containsKey(EXTRA_TICKER));

		if (wereWeWaitingForConnectivity) {
			Editor editor = sharedPreferences.edit();
			editor.putBoolean(Constants.PREFERENCE_STATUS_WAITING_FOR_CONNECTIVITY, false);
			editor.commit();
		}

		if (retrying && isGlobalCollection) {
			Editor editor = sharedPreferences.edit();
			editor.putBoolean(Constants.PREFERENCE_COLLECTOR_RETRYING, false);
			editor.putInt(Constants.PREFERENCE_COLLECTOR_RETRIES, 0);
			editor.commit();

			((AlarmManager) getSystemService(Context.ALARM_SERVICE)).cancel(Extensions.createPendingIntent(this, Constants.SCHEDULE_RETRY));
		}

		long currentTime = System.currentTimeMillis();
		if (retrying || wereWeWaitingForConnectivity || !isGlobalCollection || (isGlobalCollection && currentTime - lastUpdate > Constants.COLLECTOR_MIN_REFRESH_INTERVAL)) {
			String[] tickers = null;

			if (isGlobalCollection) {
				Log.d(LOG_TAG, "Executing global market information collection...");
				tickers = DataProvider.getStockDataTickers(this);
			}
			else {
				String ticker = intent.getExtras().containsKey(EXTRA_TICKER)
					? intent.getExtras().getString(EXTRA_TICKER)
					: null;

				Log.d(LOG_TAG, "Executing market information collection for ticker " + ticker + ".");

				tickers = new String[] { ticker };
			}

			try {
				collect(tickers);

				if (isGlobalCollection) {
					Editor editor = sharedPreferences.edit();
					editor.putLong(Constants.PREFERENCE_COLLECTOR_LAST_UPDATE, System.currentTimeMillis());
					editor.commit();
				}

				DataProvider.notifyDataCollectionIsFinished(this, tickers);

				Log.d(LOG_TAG, "Market information collection was successfully completed");
			}
			catch (Exception e) {
				Log.e(LOG_TAG, "Market information collection failed.", e);

				if (Extensions.areWeOnline(this)) {
					Log.d(LOG_TAG, "Scheduling an alarm for retrying a global market information collection...");

					retries++;

					Editor editor = sharedPreferences.edit();
					editor.putBoolean(Constants.PREFERENCE_COLLECTOR_RETRYING, true);
					editor.putInt(Constants.PREFERENCE_COLLECTOR_RETRIES, retries);
					editor.commit();

					long interval = Constants.COLLECTOR_MIN_RETRY_INTERVAL * retries;
					if (interval > Constants.COLLECTOR_MAX_REFRESH_INTERVAL) {
						interval = Constants.COLLECTOR_MAX_REFRESH_INTERVAL;
					}

					((AlarmManager) getSystemService(Context.ALARM_SERVICE)).set(AlarmManager.RTC, System.currentTimeMillis() + interval, Extensions.createPendingIntent(this, Constants.SCHEDULE_RETRY));
				}
				else {
					Log.d(LOG_TAG, "It appears that we are not online, so let's start listening for connectivity updates.");

					Editor editor = sharedPreferences.edit();
					editor.putBoolean(Constants.PREFERENCE_STATUS_WAITING_FOR_CONNECTIVITY, true);
					editor.commit();

					ComponentName componentName = new ComponentName(this, ConnectivityBroadcastReceiver.class);
					getPackageManager().setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
				}
			}
		}
		else if (isGlobalCollection && currentTime - lastUpdate <= Constants.COLLECTOR_MIN_REFRESH_INTERVAL) {
			Log.d(LOG_TAG, "Global market information collection will be skipped since it was performed less than " + (Constants.COLLECTOR_MIN_REFRESH_INTERVAL / 60 / 1000) + " minutes ago.");
		}

		stopSelf();
	}

	private void collect(String[] tickers) throws Exception {
		if (tickers.length > 0) {
			String response = "";
			DefaultHttpClient client = new DefaultHttpClient();
			HttpGet httpGet = new HttpGet(createRESTServiceURL(tickers));
			HttpResponse execute = client.execute(httpGet);
			InputStream content = execute.getEntity().getContent();
			BufferedReader buffer = new BufferedReader(new InputStreamReader(content));
			String s = "";
			while ((s = buffer.readLine()) != null) {
				response += s;
			}

			JSONTokener json = new JSONTokener(response);
			if (json != null) {
				JSONObject rootJSONObject = (JSONObject) json.nextValue();
				JSONObject queryJSONObject = (JSONObject) rootJSONObject.get("query");
				JSONObject resultsJSONObject = (JSONObject) queryJSONObject.get("results");

				JSONArray quoteJSONArray = resultsJSONObject.optJSONArray("quote");

				if (quoteJSONArray != null) {
					for (int i = 0; i < quoteJSONArray.length(); i++) {
						processJSON(quoteJSONArray.getJSONObject(i));
					}
				}
				else {
					JSONObject quoteJSONObject = resultsJSONObject.optJSONObject("quote");
					if (quoteJSONObject != null) {
						processJSON(quoteJSONObject);
					}
				}
			}
		}
	}

	private void processJSON(JSONObject object) throws JSONException {
		String symbol = object.getString(FIELD_SYMBOL);
		Float price = fromStringToFloat(object.getString(FIELD_PRICE));
		Float change = fromStringToFloat(object.getString(FIELD_CHANGE));
		Float changePercent = fromStringToFloat(object.getString(FIELD_CHANGE_PERCENTAGE).replace("%", ""));
		Float marketCapital = fromStringToFloat(object.getString(FIELD_MARKET_CAPITAL));
		Float volume = fromStringToFloat(object.getString(FIELD_VOLUME));
		Float averageVolume = fromStringToFloat(object.getString(FIELD_AVERAGE_VOLUME));
		Float dayLow = fromStringToFloat(object.getString(FIELD_DAY_LOW));
		Float dayHigh = fromStringToFloat(object.getString(FIELD_DAY_HIGH));
		Float yearLow = fromStringToFloat(object.getString(FIELD_YEAR_LOW));
		Float yearHigh = fromStringToFloat(object.getString(FIELD_YEAR_HIGH));
		Float peRatio = fromStringToFloat(object.getString(FIELD_PE_RATIO));
		Float eps = fromStringToFloat(object.getString(FIELD_EPS));
		Float dividend = fromStringToFloat(object.getString(FIELD_DIVIDEND));
		Float open = fromStringToFloat(object.getString(FIELD_OPEN));
		String lastTradeDate = object.getString(FIELD_LAST_TRADE_DATE);
		Float targetPrice = fromStringToFloat(object.getString(FIELD_TARGET_PRICE));

		DataProvider.updateStockData(this,
			symbol,
			price,
			open,
			change,
			changePercent,
			marketCapital,
			volume,
			averageVolume,
			dayLow,
			dayHigh,
			yearLow,
			yearHigh,
			peRatio,
			dividend,
			eps,
			lastTradeDate,
			targetPrice);
	}

	private String createRESTServiceURL(String[] tickers) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("http://query.yahooapis.com/v1/public/yql?q=use%20'http%3A%2F%2Fwww.datatables.org%2Fyahoo%2Ffinance%2Fyahoo.finance.quotes.xml'%20as%20quotes%3B");
		stringBuilder.append("select%20");

		stringBuilder.append(FIELD_SYMBOL).append("%2C");
		stringBuilder.append(FIELD_PRICE).append("%2C");
		stringBuilder.append(FIELD_CHANGE).append("%2C");
		stringBuilder.append(FIELD_CHANGE_PERCENTAGE).append("%2C");
		stringBuilder.append(FIELD_MARKET_CAPITAL).append("%2C");
		stringBuilder.append(FIELD_VOLUME).append("%2C");
		stringBuilder.append(FIELD_AVERAGE_VOLUME).append("%2C");
		stringBuilder.append(FIELD_DAY_LOW).append("%2C");
		stringBuilder.append(FIELD_DAY_HIGH).append("%2C");
		stringBuilder.append(FIELD_YEAR_LOW).append("%2C");
		stringBuilder.append(FIELD_YEAR_HIGH).append("%2C");
		stringBuilder.append(FIELD_PE_RATIO).append("%2C");
		stringBuilder.append(FIELD_EPS).append("%2C");
		stringBuilder.append(FIELD_DIVIDEND).append("%2C");
		stringBuilder.append(FIELD_OPEN).append("%2C");
		stringBuilder.append(FIELD_LAST_TRADE_DATE).append("%2C");
		stringBuilder.append(FIELD_TARGET_PRICE);

		stringBuilder.append("%20from%20quotes%20where%20").append(FIELD_SYMBOL).append("%20in%20");
		stringBuilder.append("(");

		for (int i = 0; i < tickers.length; i++) {
			String symbol = tickers[i];
			if (!symbol.contains(":")) {
				if (symbol.startsWith("^")) {
					symbol = "%5E" + symbol.substring(1, symbol.length()) + "";
				}

				stringBuilder.append("%22").append(symbol).append("%22");

				if (i < tickers.length - 1) {
					stringBuilder.append("%2C");
				}
			}
		}

		stringBuilder.append(")&format=json");

		return stringBuilder.toString();
	}

}
