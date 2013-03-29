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
package com.svpino.longhorn.activities;

import java.text.MessageFormat;
import java.util.List;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter.ViewBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.svpino.longhorn.artifacts.Constants;
import com.svpino.longhorn.artifacts.Extensions;
import com.svpino.longhorn.artifacts.back.BackStack;
import com.svpino.longhorn.artifacts.back.StockOverviewBackStackItem;
import com.svpino.longhorn.data.DataProvider;
import com.svpino.longhorn.data.DataProvider.StockQuoteCollectorObserver;
import com.svpino.longhorn.data.LonghornDatabase;
import com.svpino.longhorn.fragments.StockListFragment;
import com.svpino.longhorn.fragments.StockListFragment.StockListFragmentCallback;
import com.svpino.longhorn.R;
import com.svpino.longhorn.model.Stock;

public class DashboardActivity extends FragmentActivity implements StockListFragmentCallback, StockQuoteCollectorObserver {

	private final static String LOG_TAG = DashboardActivity.class.getName();

	private BackStack backStack;
	private Dialog searchDialog;
	private Dialog noConnectivityDialog;
	private Dialog termsAndConditionsDialog;
	private StockListFragment stockListFragment;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (Extensions.isPriorHoneycomb()) {
			requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		}

		setContentView(R.layout.activity_dashboard);

		if (Extensions.isPriorHoneycomb()) {
			getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.window_title);
		}

		DataProvider.initialize(getApplicationContext());
		DataProvider.registerObserver(this);

		initializeActionBar();

		this.backStack = new BackStack(savedInstanceState);
		this.stockListFragment = (StockListFragment) getSupportFragmentManager().findFragmentById(R.id.stockListFragment);

		displayTermsAndConditions();

		handleIntent(getIntent());
	}

	private void displayTermsAndConditions() {
		SharedPreferences sharedPreferences = getSharedPreferences(Constants.PREFERENCES, Context.MODE_PRIVATE);
		boolean wereTermsAndConditionsAlreadyAccepted = sharedPreferences.getBoolean(Constants.PREFERENCE_TERMS_AND_CONDITIONS, false);

		if (!wereTermsAndConditionsAlreadyAccepted) {
			AlertDialog.Builder termsAndConditionsDialogBuilder = new AlertDialog.Builder(this);
			View view = getLayoutInflater().inflate(R.layout.dialog_message, null);
			TextView textView = (TextView) view.findViewById(R.id.textView);
			textView.setText(String.format(getString(R.string.terms_and_conditions_message), getString(R.string.application_name)));
			textView.setTextAppearance(getApplicationContext(), android.R.style.TextAppearance_DeviceDefault_Small);

			termsAndConditionsDialogBuilder
				.setTitle(R.string.terms_and_conditions_title)
				.setView(view)
				.setCancelable(false)
				.setPositiveButton(R.string.label_agree, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						SharedPreferences sharedPreferences = getSharedPreferences(Constants.PREFERENCES, Context.MODE_PRIVATE);
						Editor editor = sharedPreferences.edit();
						editor.putBoolean(Constants.PREFERENCE_TERMS_AND_CONDITIONS, true);
						editor.commit();

						DashboardActivity.this.termsAndConditionsDialog.dismiss();
					}
				})
				.setNegativeButton(R.string.label_disagree, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						finish();
					}
				});

			this.termsAndConditionsDialog = termsAndConditionsDialogBuilder.create();
			this.termsAndConditionsDialog.show();
		}
	}

	@TargetApi(11)
	private void initializeActionBar() {
		if (Extensions.isHoneycombOrLater()) {
			ActionBar actionBar = getActionBar();
			actionBar.setDisplayShowTitleEnabled(true);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		this.backStack.saveState(outState);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onStart() {
		super.onStart();

		DataProvider.startStockQuoteCollectorService(this, null);

		((AlarmManager) getSystemService(Context.ALARM_SERVICE)).setInexactRepeating(
			AlarmManager.RTC,
			System.currentTimeMillis(),
			AlarmManager.INTERVAL_FIFTEEN_MINUTES,
			Extensions.createPendingIntent(this, Constants.SCHEDULE_AUTOMATIC));
	}

	@Override
	protected void onPause() {
		((AlarmManager) getSystemService(Context.ALARM_SERVICE)).cancel(Extensions.createPendingIntent(this, Constants.SCHEDULE_AUTOMATIC));

		if (this.searchDialog != null) {
			this.searchDialog.dismiss();
		}

		if (this.noConnectivityDialog != null) {
			this.noConnectivityDialog.dismiss();
		}

		if (this.termsAndConditionsDialog != null) {
			this.termsAndConditionsDialog.dismiss();
		}

		super.onPause();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		handleIntent(intent);
	}

	private void handleIntent(Intent intent) {
		if (Intent.ACTION_VIEW.equals(intent.getAction())) {
			try {
				String symbol = intent.getData().getLastPathSegment();
				addStockToWatchList(symbol);
			}
			catch (Throwable t) {
				Log.e(LOG_TAG, "Error while handling Intent.ACTION_VIEW.", t);
			}
		}
		else if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			performSearch(intent.getStringExtra(SearchManager.QUERY));
		}
	}

	private void performSearch(String query) {
		this.stockListFragment.hideContextualActionBar();
		Cursor cursor = DataProvider.search(getApplicationContext(), query);

		if (this.searchDialog == null || !this.searchDialog.isShowing()) {
			this.searchDialog = new Dialog(this);
			this.searchDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
			this.searchDialog.setContentView(R.layout.dialog);

			TextView messageTextView = (TextView) this.searchDialog.findViewById(R.id.messageTextView);
			TextView titleTextView = (TextView) this.searchDialog.findViewById(R.id.titleTextView);

			ListView listView = (ListView) this.searchDialog.findViewById(R.id.listView);

			if (cursor != null && cursor.getCount() > 0) {
				titleTextView.setText(String.format(getString(R.string.dialog_search_title), query));
				messageTextView.setVisibility(View.GONE);

				String[] from = new String[] { SearchManager.SUGGEST_COLUMN_TEXT_1, SearchManager.SUGGEST_COLUMN_TEXT_2, LonghornDatabase.KEY_EXCHANGE_SYMBOL };
				int[] to = new int[] { R.id.nameTextView, R.id.descriptionTextView, R.id.additionalTextView };

				SimpleCursorAdapter adapter = new SimpleCursorAdapter(getApplicationContext(), R.layout.dialog_item, cursor, from, to, 0);
				adapter.setViewBinder(new ViewBinder() {

					@Override
					public boolean setViewValue(View view, Cursor cursor, int columnIndex) {

						if (columnIndex == 0) {
							((View) view.getParent()).setTag(cursor.getString(columnIndex));
						}

						return false;
					}
				});

				listView.setAdapter(adapter);

				listView.setOnItemClickListener(new OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
						Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
						addStockToWatchList(cursor.getString(0));
						DashboardActivity.this.searchDialog.dismiss();
					}
				});
			}
			else {
				titleTextView.setText(getString(R.string.dialog_search_empty_title));
				messageTextView.setText(String.format(getString(R.string.dialog_search_empty_message), query));
				messageTextView.setVisibility(View.VISIBLE);
				listView.setVisibility(View.GONE);
			}

			this.searchDialog.show();
		}
	}

	private void addStockToWatchList(String symbol) {
		boolean areWeOnline = DataProvider.addStockToWatchList(getApplicationContext(), symbol);
		this.stockListFragment.refresh(false);

		if (!areWeOnline) {
			AlertDialog.Builder noConnectivityDialogBuilder = new AlertDialog.Builder(this);
			View view = getLayoutInflater().inflate(R.layout.dialog_message, null);
			TextView textView = (TextView) view.findViewById(R.id.textView);
			textView.setText(String.format(getString(R.string.activity_dashboard_no_connectivity_message), symbol));

			noConnectivityDialogBuilder
				.setIcon(R.drawable.ic_warning)
				.setTitle(R.string.activity_dashboard_no_connectivity_title)
				.setView(view)
				.setCancelable(false)
				.setPositiveButton(R.string.label_ok, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
					}
				});

			this.noConnectivityDialog = noConnectivityDialogBuilder.create();
			this.noConnectivityDialog.show();
		}
	}

	@Override
	public void onBackPressed() {
		if (this.backStack.size() == 0) {
			super.onBackPressed();
		}
		else {
			Parcelable value = this.backStack.removeLastItem();

			if (value instanceof StockOverviewBackStackItem) {
				boolean handled = this.stockListFragment.back();

				if (!handled) {
					onBackPressed();
				}
			}
		}
	}

	@Override
	public boolean onSearchRequested() {
		if (this.stockListFragment != null) {
			this.stockListFragment.hideStockOverview();
		}

		return super.onSearchRequested();
	}

	@Override
	public BackStack getBackStack() {
		return this.backStack;
	}

	@Override
	public void onStockOverviewAddedToBackStack() {
		this.backStack.add(new StockOverviewBackStackItem(BackStack.BACK_STACK_STOCK_OVERVIEW));
	}

	@Override
	public void onStockOverviewClearBackStack() {
		this.backStack.removeAllStockOverviewItems();
	}

	@Override
	public void removeStocks(List<Stock> stocks) {
		DataProvider.removeStocksFromWatchList(getApplicationContext(), stocks);
	}

	@Override
	public void reorderStocks(Stock stock1, Stock stock2) {
		DataProvider.reorderStocks(getApplicationContext(), stock1, stock2);
	}

	@Override
	public void refreshStockInformation(final List<Stock> stocks) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (DashboardActivity.this.stockListFragment != null) {
					DashboardActivity.this.stockListFragment.refreshStockInformation(stocks);
				}
			}
		});
	}

	@Override
	public String getRemoveWarningDialogTitle(Stock stock) {
		Resources resources = getResources();

		int choice = stock != null
			? 1
			: 2;

		String stockSymbol = stock != null
			? stock.getSymbol()
			: "";

		return MessageFormat.format(resources.getString(R.string.activity_dashboard_delete_stock_title), choice, stockSymbol);
	}

	@Override
	public String getRemoveWarningDialogMessage(Stock stock) {
		Resources resources = getResources();

		int choice = stock != null
			? 1
			: 2;

		String stockSymbol = stock != null
			? stock.getSymbol()
			: "";

		return MessageFormat.format(resources.getString(R.string.activity_dashboard_delete_stock_message), choice, stockSymbol);
	}

	@Override
	public List<Stock> getStockList() {
		return DataProvider.getWatchList(getApplicationContext());
	}

	public String getDisplayMetrics() {
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);

		return "(" + metrics.widthPixels + ", " + metrics.heightPixels + ")";
	}

}