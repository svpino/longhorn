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
package com.svpino.longhorn.data;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.util.Log;

import com.svpino.longhorn.MarketCollectorService;
import com.svpino.longhorn.artifacts.Constants;
import com.svpino.longhorn.artifacts.Extensions;
import com.svpino.longhorn.model.Stock;
import com.svpino.longhorn.receivers.ConnectivityBroadcastReceiver;

public class DataProvider {

	private final static String LOG_TAG = DataProvider.class.getName();

	private static LonghornDatabase longhornDatabase = null;
	private static List<Stock> stocks;
	private static HashMap<String, WeakReference<StockQuoteCollectorObserver>> observers;

	static {
		DataProvider.stocks = null;
		DataProvider.observers = new HashMap<String, WeakReference<StockQuoteCollectorObserver>>();
	}

	public static void initialize(Context context) {
		if (DataProvider.longhornDatabase == null) {
			DataProvider.longhornDatabase = new LonghornDatabase(context);
		}
	}

	public static void registerObserver(StockQuoteCollectorObserver observer) {
		DataProvider.observers.put(observer.getClass().getName(), new WeakReference<DataProvider.StockQuoteCollectorObserver>(observer));
	}

	public static void startStockQuoteCollectorService(Context context, String ticker) {
		Intent intent = new Intent(context, MarketCollectorService.class);
		if (ticker != null) {
			intent.putExtra(MarketCollectorService.EXTRA_TICKER, ticker);
		}
		context.startService(intent);
	}

	public static Cursor search(Context context, String query) {
		query = query.toLowerCase();

		String[] columns = new String[] {
				BaseColumns._ID,
				LonghornDatabase.KEY_EXCHANGE_SYMBOL,
				SearchManager.SUGGEST_COLUMN_TEXT_1,
				SearchManager.SUGGEST_COLUMN_TEXT_2,
				SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID };

		return getLonghornDatabase(context).search(query, columns);
	}

	public static List<Stock> getWatchList(Context context) {
		if (DataProvider.stocks == null) {
			DataProvider.stocks = new ArrayList<Stock>();

			Cursor cursor = DataProvider.getLonghornDatabase(context).getWatchList();
			for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
				Stock stock = new Stock(
					cursor.getString(cursor.getColumnIndex(LonghornDatabase.KEY_STOCK_SYMBOL)),
					cursor.getString(cursor.getColumnIndex(LonghornDatabase.KEY_STOCK_ALIAS)),
					cursor.getInt(cursor.getColumnIndex(LonghornDatabase.KEY_STOCK_EXCHANGE)),
					cursor.getString(cursor.getColumnIndex(LonghornDatabase.KEY_STOCK_NAME)),
					getFloatFromCursor(cursor, LonghornDatabase.KEY_STOCK_PRICE),
					getFloatFromCursor(cursor, LonghornDatabase.KEY_STOCK_OPEN),
					getFloatFromCursor(cursor, LonghornDatabase.KEY_STOCK_CHANGE),
					getFloatFromCursor(cursor, LonghornDatabase.KEY_STOCK_CHANGE_PERCENTAGE),
					getFloatFromCursor(cursor, LonghornDatabase.KEY_STOCK_MARKET_CAPITAL),
					getFloatFromCursor(cursor, LonghornDatabase.KEY_STOCK_VOLUME),
					getFloatFromCursor(cursor, LonghornDatabase.KEY_STOCK_AVERAGE_VOLUME),
					getFloatFromCursor(cursor, LonghornDatabase.KEY_STOCK_DAY_LOW),
					getFloatFromCursor(cursor, LonghornDatabase.KEY_STOCK_DAY_HIGH),
					getFloatFromCursor(cursor, LonghornDatabase.KEY_STOCK_YEAR_LOW),
					getFloatFromCursor(cursor, LonghornDatabase.KEY_STOCK_YEAR_HIGH),
					getFloatFromCursor(cursor, LonghornDatabase.KEY_STOCK_PE_RATIO),
					getFloatFromCursor(cursor, LonghornDatabase.KEY_STOCK_DIVIDEND),
					getFloatFromCursor(cursor, LonghornDatabase.KEY_STOCK_EPS),
					cursor.getString(cursor.getColumnIndex(LonghornDatabase.KEY_STOCK_LAST_TRADE_DATE)),
					getFloatFromCursor(cursor, LonghornDatabase.KEY_STOCK_TARGET_PRICE));

				DataProvider.stocks.add(stock);
			}

			cursor.close();
		}

		return DataProvider.stocks;
	}

	public static boolean addStockToWatchList(Context context, String symbol) {
		if (getStock(context, symbol) == null) {
			Cursor cursor = getLonghornDatabase(context).getStockSearch(symbol);
			cursor.moveToFirst();

			Integer exchange = cursor.getInt(cursor.getColumnIndex(LonghornDatabase.KEY_STOCK_SEARCH_EXCHANGE));
			String name = cursor.getString(cursor.getColumnIndex(LonghornDatabase.KEY_STOCK_SEARCH_NAME));
			cursor.close();

			Stock stock = new Stock(symbol, exchange, name);

			if (getLonghornDatabase(context).addStockToWatchList(symbol, exchange, name)) {
				getWatchList(context).add(stock);

				if (Extensions.areWeOnline(context)) {
					startStockQuoteCollectorService(context, stock.getSymbol());
					return true;
				}
				else {
					Log.d(LOG_TAG, "There's no Internet connection to collect the market information for " + symbol + ". Let's start listening for connectivity updates.");

					SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.PREFERENCES, Context.MODE_PRIVATE);
					Editor editor = sharedPreferences.edit();
					editor.putBoolean(Constants.PREFERENCE_STATUS_WAITING_FOR_CONNECTIVITY, true);
					editor.commit();

					ComponentName componentName = new ComponentName(context, ConnectivityBroadcastReceiver.class);
					context.getPackageManager().setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
				}

				return false;
			}
		}

		return true;
	}

	public static void reorderStocks(Context context, Stock stock1, Stock stock2) {
		int stock1Position = DataProvider.stocks.indexOf(stock1);
		int stock2Position = DataProvider.stocks.indexOf(stock2);

		DataProvider.stocks.remove(stock1Position);
		DataProvider.stocks.add(stock1Position, stock2);

		DataProvider.stocks.remove(stock2Position);
		DataProvider.stocks.add(stock2Position, stock1);

		getLonghornDatabase(context).reorderStocks(stock1, stock2);
	}

	public static void removeStocksFromWatchList(Context context, List<Stock> list) {
		for (Stock stock : list) {
			DataProvider.getWatchList(context).remove(stock);
			getLonghornDatabase(context).removeStockFromWatchList(stock.getSymbol());
		}
	}

	public static Stock getStock(Context context, String symbol) {
		for (Stock stock : getWatchList(context)) {
			if (stock.getSymbol().equals(symbol)) {
				return stock;
			}
		}

		return null;
	}

	public static String[] getStockDataTickers(Context context) {
		Cursor cursor = getLonghornDatabase(context).getStockDataTickers();

		String[] tickers = new String[cursor.getCount()];
		int index = 0;
		for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
			String symbol = cursor.getString(cursor.getColumnIndex(LonghornDatabase.KEY_STOCK_SYMBOL));
			tickers[index] = symbol;
			index++;
		}

		cursor.close();

		return tickers;
	}

	public static void updateStockData(Context context, String symbol, Float price, Float open, Float change, Float changePercentage, Float marketCapital, Float volume, Float averageVolume, Float dayLow, Float dayHigh, Float yearLow, Float yearHigh, Float peRatio, Float dividend, Float eps, String lastTradeDate, Float targetPrice) {
		getLonghornDatabase(context).updateStockData(
			symbol,
			price,
			open,
			change,
			changePercentage,
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

	private static Float getFloatFromCursor(Cursor cursor, String column) {
		return cursor.isNull(cursor.getColumnIndex(column))
			? null
			: cursor.getFloat(cursor.getColumnIndex(column));
	}

	public static void notifyDataCollectionIsFinished(Context context, String[] tickers) {
		List<Stock> updatedStockList = new ArrayList<Stock>();
		for (int i = 0; i < tickers.length; i++) {
			String symbol = tickers[i];
			Stock stock = getStock(context, symbol);
			if (stock != null) {
				Cursor cursor = getLonghornDatabase(context).getStock(symbol);
				cursor.moveToFirst();
				stock.update(
					getFloatFromCursor(cursor, LonghornDatabase.KEY_STOCK_PRICE),
					getFloatFromCursor(cursor, LonghornDatabase.KEY_STOCK_OPEN),
					getFloatFromCursor(cursor, LonghornDatabase.KEY_STOCK_CHANGE),
					getFloatFromCursor(cursor, LonghornDatabase.KEY_STOCK_CHANGE_PERCENTAGE),
					getFloatFromCursor(cursor, LonghornDatabase.KEY_STOCK_MARKET_CAPITAL),
					getFloatFromCursor(cursor, LonghornDatabase.KEY_STOCK_VOLUME),
					getFloatFromCursor(cursor, LonghornDatabase.KEY_STOCK_AVERAGE_VOLUME),
					getFloatFromCursor(cursor, LonghornDatabase.KEY_STOCK_DAY_LOW),
					getFloatFromCursor(cursor, LonghornDatabase.KEY_STOCK_DAY_HIGH),
					getFloatFromCursor(cursor, LonghornDatabase.KEY_STOCK_YEAR_LOW),
					getFloatFromCursor(cursor, LonghornDatabase.KEY_STOCK_YEAR_HIGH),
					getFloatFromCursor(cursor, LonghornDatabase.KEY_STOCK_PE_RATIO),
					getFloatFromCursor(cursor, LonghornDatabase.KEY_STOCK_DIVIDEND),
					getFloatFromCursor(cursor, LonghornDatabase.KEY_STOCK_EPS),
					cursor.getString(cursor.getColumnIndex(LonghornDatabase.KEY_STOCK_LAST_TRADE_DATE)),
					getFloatFromCursor(cursor, LonghornDatabase.KEY_STOCK_TARGET_PRICE));

				cursor.close();
				updatedStockList.add(stock);
			}
		}

		for (WeakReference<StockQuoteCollectorObserver> weakReference : DataProvider.observers.values()) {
			weakReference.get().refreshStockInformation(updatedStockList);
		}
	}

	private static LonghornDatabase getLonghornDatabase(Context context) {
		if (DataProvider.longhornDatabase == null) {
			initialize(context);
		}

		return DataProvider.longhornDatabase;
	}

	public static interface StockQuoteCollectorObserver {
		public void refreshStockInformation(List<Stock> stocks);
	}

}
