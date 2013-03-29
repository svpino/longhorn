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

import java.util.HashMap;

import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.provider.BaseColumns;

import com.svpino.longhorn.model.Stock;

public class LonghornDatabase {

	public final static String DATABASE_NAME = "longhorn";

	private final static String TABLE_STOCK_SEARCH = "stock_search";
	private final static String TABLE_EXCHANGE = "exchange";
	private final static String TABLE_WATCHLIST = "watchlist";
	private final static String TABLE_STOCK = "stock";

	public final static String KEY_ROWID = "rowid";

	public final static String KEY_STOCK_SEARCH_EXCHANGE = "exchange";
	public final static String KEY_STOCK_SEARCH_SYMBOL = "symbol";
	public final static String KEY_STOCK_SEARCH_NAME = "name";

	public final static String KEY_WATCHLIST_SYMBOL = "watchlist_symbol";
	public final static String KEY_WATCHLIST_ORDER = "watchlist_order";

	public final static String KEY_STOCK_SYMBOL = "stock_symbol";
	public final static String KEY_STOCK_ALIAS = "stock_alias";
	public final static String KEY_STOCK_EXCHANGE = "stock_exchange";
	public final static String KEY_STOCK_NAME = "stock_name";
	public final static String KEY_STOCK_CHANGE_PERCENTAGE = "stock_change_percentage";
	public final static String KEY_STOCK_CHANGE = "stock_change";
	public final static String KEY_STOCK_PRICE = "stock_price";
	public final static String KEY_STOCK_MARKET_CAPITAL = "stock_market_capital";
	public final static String KEY_STOCK_VOLUME = "stock_volume";
	public final static String KEY_STOCK_AVERAGE_VOLUME = "stock_average_volume";
	public final static String KEY_STOCK_DAY_LOW = "stock_day_low";
	public final static String KEY_STOCK_DAY_HIGH = "stock_day_high";
	public final static String KEY_STOCK_YEAR_LOW = "stock_year_low";
	public final static String KEY_STOCK_YEAR_HIGH = "stock_year_high";
	public final static String KEY_STOCK_PE_RATIO = "stock_pe_ratio";
	public final static String KEY_STOCK_EPS = "stock_eps";
	public final static String KEY_STOCK_DIVIDEND = "stock_dividend";
	public final static String KEY_STOCK_OPEN = "stock_open";
	public final static String KEY_STOCK_LAST_TRADE_DATE = "stock_last_trade_date";
	public final static String KEY_STOCK_TARGET_PRICE = "stock_target_price";

	public static final String KEY_EXCHANGE_SYMBOL = "exchange_symbol";
	public static final String KEY_EXCHANGE_NAME = "exchange_name";

	private static HashMap<String, String> searchContentProviderColumnMap;

	private LonghornOpenHelper longhornOpenHelper;

	static {
		LonghornDatabase.searchContentProviderColumnMap = buildSearchContentProviderColumnMap();
	}

	public LonghornDatabase(Context context) {
		this.longhornOpenHelper = new LonghornOpenHelper(context);
		this.longhornOpenHelper.initialize();
	}

	public Cursor getWatchList() {
		SQLiteQueryBuilder builder = new SQLiteQueryBuilder();

		builder.setTables(TABLE_WATCHLIST + " wl INNER JOIN " + TABLE_STOCK + " sd ON wl." + KEY_WATCHLIST_SYMBOL + "= sd." + KEY_STOCK_SYMBOL);
		String[] columns = {
				KEY_STOCK_EXCHANGE,
				KEY_STOCK_SYMBOL,
				KEY_STOCK_ALIAS,
				KEY_STOCK_NAME,
				KEY_STOCK_CHANGE_PERCENTAGE,
				KEY_STOCK_CHANGE,
				KEY_STOCK_PRICE,
				KEY_STOCK_MARKET_CAPITAL,
				KEY_STOCK_VOLUME,
				KEY_STOCK_AVERAGE_VOLUME,
				KEY_STOCK_DAY_LOW,
				KEY_STOCK_DAY_HIGH,
				KEY_STOCK_YEAR_LOW,
				KEY_STOCK_YEAR_HIGH,
				KEY_STOCK_PE_RATIO,
				KEY_STOCK_EPS,
				KEY_STOCK_DIVIDEND,
				KEY_STOCK_OPEN,
				KEY_STOCK_LAST_TRADE_DATE,
				KEY_STOCK_TARGET_PRICE };

		return builder.query(getReadableDatabase(), columns, "", new String[0], null, null, KEY_WATCHLIST_ORDER);
	}

	public Cursor getStock(String symbol) {
		SQLiteQueryBuilder builder = new SQLiteQueryBuilder();

		builder.setTables(TABLE_STOCK);
		String[] columns = {
				KEY_STOCK_SYMBOL,
				KEY_STOCK_ALIAS,
				KEY_STOCK_EXCHANGE,
				KEY_STOCK_NAME,
				KEY_STOCK_CHANGE_PERCENTAGE,
				KEY_STOCK_CHANGE,
				KEY_STOCK_PRICE,
				KEY_STOCK_MARKET_CAPITAL,
				KEY_STOCK_VOLUME,
				KEY_STOCK_AVERAGE_VOLUME,
				KEY_STOCK_DAY_LOW,
				KEY_STOCK_DAY_HIGH,
				KEY_STOCK_YEAR_LOW,
				KEY_STOCK_YEAR_HIGH,
				KEY_STOCK_PE_RATIO,
				KEY_STOCK_EPS,
				KEY_STOCK_DIVIDEND,
				KEY_STOCK_OPEN,
				KEY_STOCK_LAST_TRADE_DATE,
				KEY_STOCK_TARGET_PRICE };

		String selection = KEY_STOCK_SYMBOL + " = ?";

		return builder.query(getReadableDatabase(), columns, selection, new String[] { symbol }, "", "", "");
	}

	public Cursor getStockSearch(String symbol) {
		SQLiteQueryBuilder builder = new SQLiteQueryBuilder();

		builder.setTables(TABLE_STOCK_SEARCH);
		String[] columns = {
				KEY_STOCK_SEARCH_SYMBOL,
				KEY_STOCK_SEARCH_EXCHANGE,
				KEY_STOCK_SEARCH_NAME };

		String selection = KEY_STOCK_SEARCH_SYMBOL + " = ?";

		return builder.query(getReadableDatabase(), columns, selection, new String[] { symbol }, "", "", "");
	}

	public boolean addStockToWatchList(String symbol, Integer exchange, String name) {
		SQLiteDatabase database = getWritableDatabase();

		SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
		builder.setTables(TABLE_STOCK);
		Cursor cursor = builder.query(database, new String[] { KEY_STOCK_SYMBOL }, KEY_STOCK_SYMBOL + " = ?", new String[] { symbol }, null, null, null);
		boolean isAlreadyInStockTable = cursor.getCount() > 0;
		cursor.close();

		builder = new SQLiteQueryBuilder();
		builder.setTables(TABLE_WATCHLIST);
		cursor = builder.query(database, new String[] { "MAX(" + KEY_WATCHLIST_ORDER + ")" }, null, null, null, null, null);
		cursor.moveToFirst();
		int maxOrder = cursor.getInt(0);
		cursor.close();

		ContentValues watchListValues = new ContentValues();
		watchListValues.put(KEY_WATCHLIST_SYMBOL, symbol);
		watchListValues.put(KEY_WATCHLIST_ORDER, maxOrder + 1);

		ContentValues stockDataValues = new ContentValues();
		stockDataValues.put(KEY_STOCK_SYMBOL, symbol);
		stockDataValues.put(KEY_STOCK_EXCHANGE, exchange);
		stockDataValues.put(KEY_STOCK_NAME, name);

		database.beginTransaction();
		try {
			database.insert(TABLE_WATCHLIST, null, watchListValues);

			if (!isAlreadyInStockTable) {
				database.insert(TABLE_STOCK, null, stockDataValues);
			}

			database.setTransactionSuccessful();
			return true;
		}
		catch (Exception e) {
			return false;
		}
		finally {
			database.endTransaction();
		}
	}

	public Cursor getStockDataTickers() {
		SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
		builder.setTables(TABLE_STOCK);

		String[] columns = { KEY_ROWID, KEY_STOCK_SYMBOL };

		return builder.query(getReadableDatabase(), columns, null, null, "", "", "");
	}

	public void updateStockData(String symbol, Float price, Float open, Float change, Float changePercentage, Float marketCapital, Float volume, Float averageVolume, Float dayLow, Float dayHigh, Float yearLow, Float yearHigh, Float peRatio, Float dividend, Float eps, String lastTradeDate, Float targetPrice) {
		SQLiteDatabase database = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(KEY_STOCK_CHANGE_PERCENTAGE, changePercentage);
		values.put(KEY_STOCK_CHANGE, change);
		values.put(KEY_STOCK_PRICE, price);
		values.put(KEY_STOCK_MARKET_CAPITAL, marketCapital);
		values.put(KEY_STOCK_VOLUME, volume);
		values.put(KEY_STOCK_AVERAGE_VOLUME, averageVolume);
		values.put(KEY_STOCK_DAY_LOW, dayLow);
		values.put(KEY_STOCK_DAY_HIGH, dayHigh);
		values.put(KEY_STOCK_YEAR_LOW, yearLow);
		values.put(KEY_STOCK_YEAR_HIGH, yearHigh);
		values.put(KEY_STOCK_PE_RATIO, peRatio);
		values.put(KEY_STOCK_EPS, eps);
		values.put(KEY_STOCK_DIVIDEND, dividend);
		values.put(KEY_STOCK_OPEN, open);
		values.put(KEY_STOCK_LAST_TRADE_DATE, lastTradeDate);
		values.put(KEY_STOCK_TARGET_PRICE, targetPrice);

		database.update(TABLE_STOCK, values, KEY_STOCK_SYMBOL + "=?", new String[] { symbol });
	}

	public void removeStockFromWatchList(String symbol) {
		getWritableDatabase().delete(TABLE_WATCHLIST, KEY_WATCHLIST_SYMBOL + "=?", new String[] { symbol });
		getWritableDatabase().delete(TABLE_STOCK, KEY_STOCK_SYMBOL + "=?", new String[] { symbol });
	}

	public void reorderStocks(Stock stock1, Stock stock2) {
		SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
		builder.setTables(TABLE_WATCHLIST);

		String[] columns = { KEY_WATCHLIST_SYMBOL, KEY_WATCHLIST_ORDER };

		Cursor cursor = builder.query(getReadableDatabase(), columns, KEY_WATCHLIST_SYMBOL + " in (?, ?)", new String[] { stock1.getSymbol(), stock2.getSymbol() }, null, null, null);

		cursor.moveToFirst();
		String symbol1 = cursor.getString(cursor.getColumnIndex(KEY_WATCHLIST_SYMBOL));
		int order1 = cursor.getInt(cursor.getColumnIndex(KEY_WATCHLIST_ORDER));

		cursor.moveToNext();
		String symbol2 = cursor.getString(cursor.getColumnIndex(KEY_WATCHLIST_SYMBOL));
		int order2 = cursor.getInt(cursor.getColumnIndex(KEY_WATCHLIST_ORDER));

		cursor.close();

		SQLiteDatabase database = getWritableDatabase();
		database.beginTransaction();
		try {
			ContentValues values = new ContentValues();
			values.put(KEY_WATCHLIST_ORDER, order1);
			database.update(TABLE_WATCHLIST, values, KEY_WATCHLIST_SYMBOL + "=?", new String[] { symbol2 });

			values = new ContentValues();
			values.put(KEY_WATCHLIST_ORDER, order2);
			database.update(TABLE_WATCHLIST, values, KEY_WATCHLIST_SYMBOL + "=?", new String[] { symbol1 });

			database.setTransactionSuccessful();
		}
		finally {
			database.endTransaction();
		}
	}

	public Cursor search(String query, String[] columns) {
		// boolean restrictSearchToNASDAQ = Constants.COMPILING_LITE_VERSION;
		boolean restrictSearchToNASDAQ = false;

		String exchangeSelection = "";

		if (restrictSearchToNASDAQ) {
			exchangeSelection = " AND e." + KEY_EXCHANGE_SYMBOL + "=?";
		}

		String selection = TABLE_STOCK_SEARCH + " MATCH ?" + exchangeSelection;

		String[] selectionArgs;
		if (restrictSearchToNASDAQ) {
			selectionArgs = new String[] { query + "*", "NASDAQ" };
		}
		else {
			selectionArgs = new String[] { query + "*" };
		}

		SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
		builder.setTables(TABLE_STOCK_SEARCH + " s INNER JOIN " + TABLE_EXCHANGE + " e ON s." + KEY_STOCK_SEARCH_EXCHANGE + " = e." + KEY_ROWID);
		builder.setProjectionMap(LonghornDatabase.searchContentProviderColumnMap);

		Cursor cursor = builder.query(getReadableDatabase(), columns, selection, selectionArgs, null, null, null);

		if (cursor == null) {
			return null;
		}
		else if (!cursor.moveToFirst()) {
			cursor.close();
			return null;
		}

		return cursor;
	}

	private SQLiteDatabase getReadableDatabase() {
		return this.longhornOpenHelper.getReadableDatabase();
	}

	private SQLiteDatabase getWritableDatabase() {
		return this.longhornOpenHelper.getWritableDatabase();
	}

	private static HashMap<String, String> buildSearchContentProviderColumnMap() {
		HashMap<String, String> map = new HashMap<String, String>();
		map.put(KEY_EXCHANGE_SYMBOL, KEY_EXCHANGE_SYMBOL);
		map.put(KEY_STOCK_SEARCH_SYMBOL, KEY_STOCK_SEARCH_SYMBOL);
		map.put(KEY_STOCK_SEARCH_NAME, KEY_STOCK_SEARCH_NAME);
		map.put(SearchManager.SUGGEST_COLUMN_TEXT_1, KEY_STOCK_SEARCH_SYMBOL + " AS " + SearchManager.SUGGEST_COLUMN_TEXT_1);
		map.put(SearchManager.SUGGEST_COLUMN_TEXT_2, KEY_STOCK_SEARCH_NAME + " AS " + SearchManager.SUGGEST_COLUMN_TEXT_2);
		map.put(BaseColumns._ID, "s." + KEY_STOCK_SEARCH_SYMBOL + " AS " + BaseColumns._ID);
		map.put(SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID, "s." + KEY_STOCK_SEARCH_SYMBOL + " AS " + SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID);
		map.put(SearchManager.SUGGEST_COLUMN_SHORTCUT_ID, "s." + KEY_STOCK_SEARCH_SYMBOL + " AS " + SearchManager.SUGGEST_COLUMN_SHORTCUT_ID);
		return map;
	}

}
