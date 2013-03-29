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

import static com.svpino.longhorn.artifacts.Extensions.areWeUsingWiFi;
import static com.svpino.longhorn.artifacts.Extensions.isTheBatteryPluggedIn;
import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.util.Log;

import com.svpino.longhorn.artifacts.Constants;
import com.svpino.longhorn.artifacts.Extensions;
import com.svpino.longhorn.data.DataProvider;

public class ConnectivityBroadcastReceiver extends BroadcastReceiver {

	private final static String LOG_TAG = ConnectivityBroadcastReceiver.class.getName();

	@Override
	public void onReceive(Context context, Intent intent) {
		if (Extensions.areWeOnline(context)) {
			SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.PREFERENCES, Context.MODE_PRIVATE);
			boolean wereWeWaitingForConnectivity = sharedPreferences.getBoolean(Constants.PREFERENCE_STATUS_WAITING_FOR_CONNECTIVITY, false);

			if (wereWeWaitingForConnectivity) {
				Log.d(LOG_TAG, "Connectivity was just re-established, so let's make a global market information collection.");

				DataProvider.startStockQuoteCollectorService(context, null);

				ComponentName componentName = new ComponentName(context, ConnectivityBroadcastReceiver.class);
				context.getPackageManager().setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
			}
			else {
				if (areWeUsingWiFi(context)) {
					if (isTheBatteryPluggedIn(context)) {
						Log.d(LOG_TAG, "We are on WiFi and charging, so we are good to start updating the app.");

						((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).setInexactRepeating(
							AlarmManager.RTC_WAKEUP,
							System.currentTimeMillis(),
							AlarmManager.INTERVAL_HALF_HOUR,
							Extensions.createPendingIntent(context, Constants.SCHEDULE_BACKGROUND));
					}
				}
				else {
					Log.d(LOG_TAG, "We aren't using WiFi, so let's no bother the phone with any new updates.");
					((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).cancel(Extensions.createPendingIntent(context, Constants.SCHEDULE_BACKGROUND));
				}
			}
		}
		else {
			Log.d(LOG_TAG, "We aren't online, so let's no bother the phone with any new updates.");
			((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).cancel(Extensions.createPendingIntent(context, Constants.SCHEDULE_BACKGROUND));
		}
	}

}
