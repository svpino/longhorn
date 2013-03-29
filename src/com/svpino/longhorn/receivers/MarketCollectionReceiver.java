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
package com.svpino.longhorn.receivers;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.svpino.longhorn.artifacts.Constants;
import com.svpino.longhorn.artifacts.Extensions;
import com.svpino.longhorn.data.DataProvider;

public class MarketCollectionReceiver extends BroadcastReceiver {

	private final static String LOG_TAG = MarketCollectionReceiver.class.getName();

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(Constants.SCHEDULE_RETRY)) {
			SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.PREFERENCES, Context.MODE_PRIVATE);
			boolean retrying = sharedPreferences.getBoolean(Constants.PREFERENCE_COLLECTOR_RETRYING, false);

			if (retrying) {
				Log.d(LOG_TAG, "Retrying global market information collection...");
				DataProvider.startStockQuoteCollectorService(context, null);
			}
			else {
				Log.d(LOG_TAG, "Global market information collection was already successfully performed");
			}
		}
		else if (intent.getAction().equals(Constants.SCHEDULE_BACKGROUND)) {
			if (Extensions.isTheBatteryPluggedIn(context) && Extensions.areWeUsingWiFi(context)) {
				Log.d(LOG_TAG, "Performing background global market information collection...");
				DataProvider.startStockQuoteCollectorService(context, null);
			}
			else {
				Log.d(LOG_TAG, "We are not longer able to perform background updates, so let's disable the alarm");
				((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).cancel(Extensions.createPendingIntent(context, Constants.SCHEDULE_BACKGROUND));
			}
		}
		else if (intent.getAction().equals(Constants.SCHEDULE_AUTOMATIC)) {
			Log.d(LOG_TAG, "Performing automatic global market information collection...");
			DataProvider.startStockQuoteCollectorService(context, null);
		}
	}

}
