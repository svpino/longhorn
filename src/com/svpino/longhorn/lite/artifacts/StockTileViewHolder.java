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
package com.svpino.longhorn.lite.artifacts;

import android.content.res.Resources;
import android.view.View;
import android.widget.TextView;

import com.svpino.longhorn.lite.R;
import com.svpino.longhorn.lite.model.Stock;

public class StockTileViewHolder {
	private Stock stock;
	private int position;
	private final boolean spanned;
	private View tileLayout;
	private TextView symbolTextView;
	private TextView priceTextView;
	private TextView changeTextView;
	private TextView marketCapitalTextView;
	private TextView volumeTextView;
	private TextView rangeTextView;

	public StockTileViewHolder(Resources resources, View view, Stock stock, int position, boolean spanned) {
		this.position = position;
		this.spanned = spanned;
		this.tileLayout = view.findViewById(R.id.tileLayout);
		this.symbolTextView = (TextView) view.findViewById(R.id.symbolTextView);
		this.priceTextView = (TextView) view.findViewById(R.id.priceTextView);
		this.changeTextView = (TextView) view.findViewById(R.id.changeTextView);
		this.marketCapitalTextView = (TextView) view.findViewById(R.id.marketCapitalTextView);
		this.volumeTextView = (TextView) view.findViewById(R.id.volumeTextView);
		this.rangeTextView = (TextView) view.findViewById(R.id.rangeTextView);

		refresh(resources, stock);
	}

	public boolean isFixed() {
		return this.position <= 2;
	}

	public void refresh(Resources resources) {
		refresh(resources, null);
	}

	public void refresh(Resources resources, Stock stock) {
		if (stock != null) {
			this.stock = stock;
		}

		this.symbolTextView.setText(this.stock.isMarketIndex()
			? this.stock.getAlias()
			: this.stock.getSymbol());
		this.priceTextView.setText(toValueOrEmptyString(this.stock.getStringPrice()));

		if (this.spanned) {
			this.changeTextView.setText(toValueOrEmptyString(this.stock.getStringChangeAndChangePercentage()));
			this.marketCapitalTextView.setText(toValueOrEmptyString(this.stock.getStringMarketCapital()));
			this.volumeTextView.setText(toValueOrEmptyString(this.stock.getStringVolumeAndAverageVolume()));
			this.rangeTextView.setText(toValueOrEmptyString(this.stock.getStringDaysPerformance()));
		}
		else {
			this.changeTextView.setText(toValueOrEmptyString(this.stock.getStringChangePercentage()));
		}

		Extensions.applyPattern(resources, this.tileLayout, this.stock);
	}

	private String toValueOrEmptyString(String value) {
		if (value.equals("-")) {
			return "";
		}

		return value;
	}

	public View getTileLayout() {
		return this.tileLayout;
	}

	public Stock getStock() {
		return this.stock;
	}

	public void setStock(Stock stock) {
		this.stock = stock;
	}

	public int getPosition() {
		return this.position;
	}
}