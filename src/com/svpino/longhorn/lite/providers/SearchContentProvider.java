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
package com.svpino.longhorn.lite.providers;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;

import com.svpino.longhorn.lite.data.DataProvider;

public class SearchContentProvider extends ContentProvider {

	public static String AUTHORITY = "com.svpino.longhorn.lite.providers.SearchContentProvider";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/stocks");

	private static final int SEARCH_STOCKS = 0;
	private static final int GET_STOCK = 1;
	private static final int SEARCH_SUGGEST = 2;
	private static final int REFRESH_SHORTCUT = 3;

	private static final UriMatcher uriMatcher = buildUriMatcher();

	@Override
	public boolean onCreate() {
		return true;
	}

	private static UriMatcher buildUriMatcher() {
		UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);

		matcher.addURI(AUTHORITY, "stocks", SEARCH_STOCKS);
		matcher.addURI(AUTHORITY, "stocks/#", GET_STOCK);

		matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH_SUGGEST);
		matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH_SUGGEST);

		matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_SHORTCUT, REFRESH_SHORTCUT);
		matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_SHORTCUT + "/*", REFRESH_SHORTCUT);
		return matcher;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		switch (SearchContentProvider.uriMatcher.match(uri)) {
			case SEARCH_SUGGEST:
				return retrieveStockSuggestions(selectionArgs[0]);
			case SEARCH_STOCKS:
				return null;
			case GET_STOCK:
				return null;
			case REFRESH_SHORTCUT:
				return null;
			default:
				throw new IllegalArgumentException("Unknown Uri: " + uri);
		}
	}

	private Cursor retrieveStockSuggestions(String query) {
		return DataProvider.search(getContext(), query);
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		return null;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		return 0;
	}

}
