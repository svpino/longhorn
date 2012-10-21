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

import static com.svpino.longhorn.lite.artifacts.Extensions.toValueOrEmptyString;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.svpino.longhorn.lite.R;
import com.svpino.longhorn.lite.model.Stock;

public class StockOverviewManager {

	private View dividerImageView;
	private TextView priceTextView;
	private TextView symbolTextView;
	private TextView nameTextView;
	private TextView changeTextView;
	private TextView openTextView;
	private TextView marketCapitalTextView;
	private TextView volumeLabelTextView;
	private TextView volumeTextView;
	private TextView rangeTextView;
	private TextView yearRangeTextView;
	private TextView peRatioTextView;
	private TextView epsTextView;
	private TextView dividendTextView;

	public StockOverviewManager(ViewGroup parent) {
		this.dividerImageView = parent.findViewById(R.id.dividerImageView);
		this.priceTextView = (TextView) parent.findViewById(R.id.priceTextView);
		this.symbolTextView = (TextView) parent.findViewById(R.id.symbolTextView);
		this.nameTextView = (TextView) parent.findViewById(R.id.nameTextView);
		this.changeTextView = (TextView) parent.findViewById(R.id.changeTextView);
		this.openTextView = (TextView) parent.findViewById(R.id.openTextView);
		this.marketCapitalTextView = (TextView) parent.findViewById(R.id.marketCapitalTextView);
		this.volumeLabelTextView = (TextView) parent.findViewById(R.id.volumeLabelTextView);
		this.volumeTextView = (TextView) parent.findViewById(R.id.volumeTextView);
		this.rangeTextView = (TextView) parent.findViewById(R.id.rangeTextView);
		this.yearRangeTextView = (TextView) parent.findViewById(R.id.yearRangeTextView);
		this.peRatioTextView = (TextView) parent.findViewById(R.id.peRatioTextView);
		this.epsTextView = (TextView) parent.findViewById(R.id.epsTextView);
		this.dividendTextView = (TextView) parent.findViewById(R.id.dividendTextView);
	}

	public void setStock(Stock stock) {
		if (this.dividerImageView != null) {
			this.dividerImageView.setBackgroundResource(Extensions.dividerLineResourceId(stock));

			this.symbolTextView.setText(stock.isMarketIndex()
				? stock.getAlias()
				: stock.getSymbol());
			this.nameTextView.setText(stock.getName().toUpperCase());
			this.priceTextView.setText(toValueOrEmptyString(stock.getStringPrice()));
			this.changeTextView.setText(toValueOrEmptyString(stock.getStringChangeAndChangePercentage()));

			this.openTextView.setText(stock.getStringOpen());
			this.marketCapitalTextView.setText(stock.getStringMarketCapital());

			if ((stock.getVolume() != null && stock.getAverageVolume() != null) || (stock.getVolume() == null && stock.getAverageVolume() == null)) {
				this.volumeLabelTextView.setText(R.string.stock_overview_volume_label);
			}
			else if (stock.getVolume() == null) {
				this.volumeLabelTextView.setText(R.string.stock_overview_volume_label_average_volume);
			}
			else if (stock.getAverageVolume() == null) {
				this.volumeLabelTextView.setText(R.string.stock_overview_volume_label_volume);
			}

			this.volumeTextView.setText(stock.getStringVolumeAndAverageVolume());

			this.rangeTextView.setText(stock.getStringDaysPerformance());
			this.yearRangeTextView.setText(stock.getStringYearsPerformance());
			this.peRatioTextView.setText(stock.getStringPERatio());
			this.epsTextView.setText(stock.getStringEPS());
			this.dividendTextView.setText(stock.getStringDividend());
		}
	}

	public void setPrice(String value) {
		this.priceTextView.setText(value);
	}

}
