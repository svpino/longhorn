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
package com.svpino.longhorn.layouts;

import android.content.Context;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.svpino.longhorn.R;
import com.svpino.longhorn.artifacts.Extensions;
import com.svpino.longhorn.artifacts.StockOverviewManager;
import com.svpino.longhorn.model.Stock;

public class StockOverviewLayout extends LinearLayout {

	private final static float STOCK_PRICE_ANIMATION_STEPS = 10;

	private TextView lastTradeDateTextView;
	private TextView targetPriceTextView;
	private StockPriceAnimationAsyncTask stockPriceAnimationAsyncTask;
	private Stock stock;
	private ViewGroup stockOverviewContentLayout;

	private StockOverviewManager stockOverviewManager;

	public StockOverviewLayout(final Context context) {
		super(context);
		this.stock = null;

		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.stock_overview, this);

		this.stockOverviewContentLayout = (ViewGroup) findViewById(R.id.stockOverviewContentLayout);

		this.stockOverviewManager = new StockOverviewManager(this.stockOverviewContentLayout);

		this.lastTradeDateTextView = (TextView) findViewById(R.id.lastTradeDateTextView);
		this.targetPriceTextView = (TextView) findViewById(R.id.targetPriceTextView);
	}

	public void setStock(Stock stock) {
		this.stock = stock;

		Extensions.applyPattern(getResources(), this.stockOverviewContentLayout, stock);

		this.stockOverviewManager.setStock(this.stock);

		if (this.lastTradeDateTextView != null) {
			this.lastTradeDateTextView.setText(this.stock.getLastTradeDate());
		}

		if (this.targetPriceTextView != null) {
			this.targetPriceTextView.setText(this.stock.getStringTargetPrice());
		}
	}

	public Stock getStock() {
		return this.stock;
	}

	public void animateContent() {
		if (this.stock != null) {
			if (this.stockPriceAnimationAsyncTask != null) {
				this.stockPriceAnimationAsyncTask.cancel(true);
				this.stockPriceAnimationAsyncTask = null;
			}

			if (this.stock.getPrice() != null && this.stock.getOpen() != null) {
				this.stockOverviewManager.setPrice(this.stock.getStringOpen());

				float increment = (this.stock.getPrice() - this.stock.getOpen()) / STOCK_PRICE_ANIMATION_STEPS;

				this.stockPriceAnimationAsyncTask = new StockPriceAnimationAsyncTask();
				this.stockPriceAnimationAsyncTask.execute(this.stock.getPrice(), this.stock.getOpen(), increment);
			}
		}
	}

	private class StockPriceAnimationAsyncTask extends AsyncTask<Float, Float, Void> {

		private float price;

		@Override
		protected Void doInBackground(Float... params) {
			this.price = params[0];
			float open = params[1];
			float increment = params[2];

			for (int i = 0; i < STOCK_PRICE_ANIMATION_STEPS; i++) {
				if (!this.isCancelled()) {
					try {
						open += increment;
						publishProgress(open);
						Thread.sleep(40);
					}
					catch (InterruptedException e) {
						return null;
					}
				}
			}

			return null;
		}

		@Override
		protected void onProgressUpdate(Float... values) {
			float value = values[0];
			StockOverviewLayout.this.stockOverviewManager.setPrice(Extensions.format(value));
		}

		@Override
		protected void onPostExecute(Void result) {
			StockOverviewLayout.this.stockOverviewManager.setPrice(Extensions.format(this.price));
		}
	}
}
