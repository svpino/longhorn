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
package com.svpino.longhorn.lite.receivers;

import static com.svpino.longhorn.lite.artifacts.Extensions.areWeUsingWiFi;
import static com.svpino.longhorn.lite.artifacts.Extensions.isTheBatteryPluggedIn;
import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.util.Log;

import com.svpino.longhorn.lite.artifacts.Constants;
import com.svpino.longhorn.lite.artifacts.Extensions;

public class BatteryBroadcastReceiver extends BroadcastReceiver {

	private final static String LOG_TAG = BatteryBroadcastReceiver.class.getName();

	@Override
	public void onReceive(Context context, Intent intent) {
		if (isTheBatteryPluggedIn(context)) {
			if (areWeUsingWiFi(context)) {
				Log.d(LOG_TAG, "We are on WiFi and charging, so we are good to start updating the app");

				((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).setInexactRepeating(
					AlarmManager.RTC_WAKEUP,
					System.currentTimeMillis(),
					AlarmManager.INTERVAL_HALF_HOUR,
					Extensions.createPendingIntent(context, Constants.SCHEDULE_BACKGROUND));
			}

			ComponentName componentName = new ComponentName(context, ConnectivityBroadcastReceiver.class);
			context.getPackageManager().setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
		}
		else {
			Log.d(LOG_TAG, "Our phone is not charging anymore, so let's hold off in any new updates");

			((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).cancel(Extensions.createPendingIntent(context, Constants.SCHEDULE_BACKGROUND));

			SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.PREFERENCES, Context.MODE_PRIVATE);
			boolean areWeWaitingForConnectivity = sharedPreferences.getBoolean(Constants.PREFERENCE_STATUS_WAITING_FOR_CONNECTIVITY, false);

			if (!areWeWaitingForConnectivity) {
				ComponentName componentName = new ComponentName(context, ConnectivityBroadcastReceiver.class);
				context.getPackageManager().setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
			}
		}
	}
}
