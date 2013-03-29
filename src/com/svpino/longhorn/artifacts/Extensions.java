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
package com.svpino.longhorn.artifacts;

import java.text.DecimalFormat;
import java.util.List;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.view.View;

import com.svpino.longhorn.R;
import com.svpino.longhorn.model.Stock;
import com.svpino.longhorn.receivers.MarketCollectionReceiver;

public class Extensions {

	public enum PatternBackgroundColor {
		BLACK, BLUE, GREEN, RED, BROWN
	}

	public static int dpToPixels(Resources resources, int dp) {
		final float scale = resources.getDisplayMetrics().density;
		return (int) (dp * scale + 0.5f);
	}

	public static String format(float value) {
		return new DecimalFormat("#,##0.00").format(value);
	}

	public static String formatShares(float value) {
		return new DecimalFormat("#,##0.###").format(value);
	}

	public static String toValueOrEmptyString(String value) {
		if (value.equals("-")) {
			return "";
		}

		return value;
	}

	public static String fromValueToShort(float value) {
		String sign = "";
		if (value > 1000000000) {
			value /= 1000000000;
			sign = "B";
		}
		else if (value > 1000000) {
			value /= 1000000;
			sign = "M";
		}

		return new DecimalFormat("#,##0.##").format(value) + sign;
	}

	public static Float fromStringToFloat(String value) {
		float multiplier = 1f;

		if (value.endsWith("B")) {
			value = value.substring(0, value.length() - 1);
			multiplier = 1000000000f;
		}
		else if (value.endsWith("M")) {
			value = value.substring(0, value.length() - 1);
			multiplier = 1000000f;
		}

		Float result;

		try {
			result = Float.parseFloat(value);
		}
		catch (NumberFormatException e) {
			result = null;
		}

		return result != null
			? result * multiplier
			: null;
	}

	public static int dividerLineResourceId(Stock stock) {
		if (stock.isDown()) {
			return R.drawable.divider_horizontal_red;
		}

		if (stock.isUp()) {
			return R.drawable.divider_horizontal_green;
		}

		return R.drawable.divider_horizontal_black;
	}

	public static int chartFillColor(Stock stock) {
		return stock.isDown()
			? Color.rgb(112, 39, 37)
			: Color.rgb(71, 87, 33);
	}

	public static int chartGridColor(Stock stock) {
		return stock.isDown()
			? Color.rgb(190, 67, 61)
			: Color.rgb(131, 161, 61);
	}

	public static void applyPattern(Resources resources, View content, Stock stock) {
		if (stock.isDown()) {
			applyPattern(resources, content, PatternBackgroundColor.RED);
		}
		else if (stock.isUp()) {
			applyPattern(resources, content, PatternBackgroundColor.GREEN);
		}
		else {
			applyPattern(resources, content, PatternBackgroundColor.BLACK);
		}
	}

	public static void applyPattern(Resources resources, View view, PatternBackgroundColor patternBackgroundColor) {
		StateListDrawable stateListDrawable = new StateListDrawable();
		stateListDrawable.addState(new int[] { android.R.attr.state_pressed }, Extensions.getPatternDrawable(resources, PatternBackgroundColor.BLUE));
		stateListDrawable.addState(new int[] {}, Extensions.getPatternDrawable(resources, getPatternResourceId(patternBackgroundColor)));
		view.setBackgroundDrawable(stateListDrawable);
	}

	private static Drawable getPatternDrawable(Resources resources, PatternBackgroundColor patternBackgroundColor) {
		return getPatternDrawable(resources, getPatternResourceId(patternBackgroundColor));
	}

	private static Drawable getPatternDrawable(Resources resources, int resourceId) {
		BitmapDrawable drawable = new BitmapDrawable(resources, BitmapFactory.decodeResource(resources, resourceId));
		drawable.setTileModeX(Shader.TileMode.REPEAT);
		drawable.setTileModeY(Shader.TileMode.REPEAT);

		return drawable;
	}

	private static int getPatternResourceId(PatternBackgroundColor patternBackgroundColor) {
		int resourceId;
		switch (patternBackgroundColor) {
			case BLUE:
				resourceId = R.drawable.pattern_background_blue;
				break;
			case GREEN:
				resourceId = R.drawable.pattern_background_green;
				break;
			case RED:
				resourceId = R.drawable.pattern_background_red;
				break;
			case BROWN:
				resourceId = R.drawable.pattern_background_brown;
				break;
			default:
				resourceId = R.drawable.pattern_background_black;
				break;
		}

		return resourceId;
	}

	public static boolean isPriorHoneycomb() {
		return Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB;
	}

	public static boolean isHoneycombOrLater() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
	}

	public static boolean isIceCreamSandwichOrLater() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
	}

	public static boolean isPriorIceCreamSandwich() {
		return Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH;
	}

	public static boolean isOrientationPortrait(Resources resources) {
		return resources.getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
	}

	public static Stock findStock(List<Stock> stocks, String symbol) {
		for (Stock stock : stocks) {
			if (stock.getSymbol().toUpperCase().equals(symbol.toUpperCase())) {
				return stock;
			}
		}

		return null;
	}

	public static boolean isTheBatteryPluggedIn(Context context) {
		Intent batteryIntent = context.getApplicationContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		int plugged = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
		return plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB;
	}

	public static boolean areWeOnline(Context context) {
		ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

		return networkInfo != null
			? networkInfo.isConnected()
			: false;
	}

	public static boolean areWeUsingWiFi(Context context) {
		ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

		boolean isConnected = networkInfo != null
			? networkInfo.isConnected()
			: false;
		boolean isWiFi = isConnected
			? networkInfo.getType() == ConnectivityManager.TYPE_WIFI
			: false;

		return isWiFi;
	}

	public static PendingIntent createPendingIntent(Context context, String action) {
		Intent intent = new Intent(context, MarketCollectionReceiver.class);
		intent.setAction(action);
		return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
	}

}