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
package com.svpino.longhorn.model;

import java.text.DecimalFormat;

import com.svpino.longhorn.artifacts.Extensions;

public class Stock {

	private final String symbol;
	private String alias;
	private Integer exchange;
	private String name;
	private Float price;
	private Float open;
	private Float dayLow;
	private Float dayHigh;
	private Float yearLow;
	private Float yearHigh;
	private Float peRatio;
	private Float dividend;
	private Float eps;
	private Float change;
	private Float changePercentage;
	private Float volume;
	private Float averageVolume;
	private Float marketCapital;
	private String lastTradeDate;
	private Float targetPrice;

	public Stock(String symbol, String alias, Integer exchange, String name, Float price, Float open, Float change, Float changePercentage, Float marketCapital, Float volume, Float averageVolume, Float dayLow, Float dayHigh, Float yearLow,
		Float yearHigh, Float peRatio, Float dividend, Float eps, String lastTradeDate, Float targetPrice) {
		this.symbol = symbol;
		this.alias = alias;
		this.exchange = exchange;
		this.name = name;
		this.price = price;
		this.open = open;
		this.change = change;
		this.changePercentage = changePercentage;
		this.marketCapital = marketCapital;
		this.lastTradeDate = lastTradeDate;
		this.targetPrice = targetPrice;
		setVolume(volume);
		setAverageVolume(averageVolume);
		this.dayLow = dayLow;
		this.dayHigh = dayHigh;
		this.yearLow = yearLow;
		this.yearHigh = yearHigh;
		this.peRatio = peRatio;
		this.dividend = dividend;
		this.eps = eps;
	}

	public Stock(String symbol, Integer exchange, String name) {
		this.symbol = symbol;
		this.exchange = exchange;
		this.name = name;
	}

	public void update(Float price, Float open, Float change, Float changePercentage, Float marketCapital, Float volume, Float averageVolume, Float dayLow, Float dayHigh, Float yearLow, Float yearHigh, Float peRatio, Float dividend, Float eps, String lastTradeDate, Float targetPrice) {
		this.price = price;
		this.open = open;
		this.change = change;
		this.changePercentage = changePercentage;
		this.marketCapital = marketCapital;
		setVolume(volume);
		setAverageVolume(averageVolume);
		this.dayLow = dayLow;
		this.dayHigh = dayHigh;
		this.yearLow = yearLow;
		this.yearHigh = yearHigh;
		this.peRatio = peRatio;
		this.dividend = dividend;
		this.eps = eps;
		this.lastTradeDate = lastTradeDate;
		this.targetPrice = targetPrice;
	}

	public Integer getExchange() {
		return this.exchange;
	}

	public String getSymbol() {
		return this.symbol;
	}

	public String getAlias() {
		return this.alias;
	}

	public String getName() {
		return this.name;
	}

	public Float getPrice() {
		return this.price;
	}

	public void setPrice(Float price) {
		this.price = price;
	}

	public Float getOpen() {
		return this.open;
	}

	public void setOpen(Float open) {
		this.open = open;
	}

	public Float getDayLow() {
		return this.dayLow;
	}

	public void setDayLow(Float dayLow) {
		this.dayLow = dayLow;
	}

	public Float getDayHigh() {
		return this.dayHigh;
	}

	public void setDayHigh(Float dayHigh) {
		this.dayHigh = dayHigh;
	}

	public Float getYearLow() {
		return this.yearLow;
	}

	public void setYearLow(Float yearLow) {
		this.yearLow = yearLow;
	}

	public Float getYearHigh() {
		return this.yearHigh;
	}

	public void setYearHigh(Float yearHigh) {
		this.yearHigh = yearHigh;
	}

	public Float getPERatio() {
		return this.peRatio;
	}

	public void setPERatio(Float peRatio) {
		this.peRatio = peRatio;
	}

	public Float getDividend() {
		return this.dividend;
	}

	public void setDividend(Float dividend) {
		this.dividend = dividend;
	}

	public Float getEPS() {
		return this.eps;
	}

	public void setEPS(Float eps) {
		this.eps = eps;
	}

	public Float getChange() {
		return this.change;
	}

	public void setChange(Float change) {
		this.change = change;
	}

	public Float getChangePercentage() {
		return this.changePercentage;
	}

	public void setChangePercentage(Float changePercentage) {
		this.changePercentage = changePercentage;
	}

	public Float getVolume() {
		return this.volume;
	}

	public void setVolume(Float volume) {
		this.volume = volume;

		if (volume != null && volume == 0) {
			this.volume = null;
		}
	}

	public Float getAverageVolume() {
		return this.averageVolume;
	}

	public void setAverageVolume(Float averageVolume) {
		this.averageVolume = averageVolume;

		if (averageVolume != null && averageVolume == 0) {
			this.averageVolume = null;
		}
	}

	public Float getMarketCapital() {
		return this.marketCapital;
	}

	public void setMarketCapital(Float marketCapital) {
		this.marketCapital = marketCapital;
	}

	public String getLastTradeDate() {
		return this.lastTradeDate == null || this.lastTradeDate.equals("") || this.lastTradeDate.equals("null")
			? "-"
			: this.lastTradeDate;
	}

	public Float getTargetPrice() {
		return this.targetPrice;
	}

	public String getStringPrice() {
		return toStringValue(this.price, "-");
	}

	public String getStringOpen() {
		return toStringValue(this.open, "-");
	}

	public String getStringChangeAndChangePercentage() {
		String value1 = this.change == null
			? "-"
			: new DecimalFormat("+0.00;-0.00").format(this.change);

		String value2 = getStringChangePercentage();

		if (!value1.equals("-") && value2.equals("-")) {
			return value1;
		}

		if (value1.equals("-") && !value2.equals("-")) {
			return value2;
		}

		return value1.equals("-") && value2.equals("-")
			? "-"
			: value1 + " (" + value2 + ")";
	}

	public String getStringChangePercentage() {
		String value = this.changePercentage == null
			? "-"
			: new DecimalFormat("+0.00;-0.00").format(this.changePercentage);

		if (!value.equals("-")) {
			value += "%";
		}

		return value;
	}

	public String getStringMarketCapital() {
		return this.marketCapital == null
			? "-"
			: Extensions.fromValueToShort(this.marketCapital);
	}

	public String getStringDaysPerformance() {
		String value1 = this.dayLow == null
			? "-"
			: Extensions.format(this.dayLow);

		String value2 = this.dayHigh == null
			? "-"
			: Extensions.format(this.dayHigh);

		return value1.equals("-") && value2.equals("-")
			? "-"
			:
			value1 + " / " + value2;
	}

	public String getStringYearsPerformance() {
		String value1 = this.yearLow == null
			? "-"
			: Extensions.format(this.yearLow);

		String value2 = this.yearHigh == null
			? "-"
			: Extensions.format(this.yearHigh);

		return value1.equals("-") && value2.equals("-")
			? "-"
			:
			value1 + " / " + value2;
	}

	public String getStringVolumeAndAverageVolume() {
		String value1 = this.volume == null || this.volume == 0
			? "-"
			: Extensions.fromValueToShort(this.volume);

		String value2 = this.averageVolume == null || this.averageVolume == 0
			? "-"
			: Extensions.fromValueToShort(this.averageVolume);

		if (value1.equals("-") && value2.equals("-")) {
			return "-";
		}

		if (value1.equals("-")) {
			return value2;
		}

		if (value2.equals("-")) {
			return value1;
		}

		return value1 + " / " + value2;
	}

	public String getStringPERatio() {
		return toStringValue(this.peRatio, "-");
	}

	public String getStringEPS() {
		return toStringValue(this.eps, "-");
	}

	public String getStringDividend() {
		return toStringValue(this.dividend, "-");
	}

	public String getStringTargetPrice() {
		return toStringValue(this.targetPrice, "-");
	}

	private String toStringValue(Float value, String nullValue) {
		return value == null
			? nullValue
			: Extensions.format(value);
	}

	public boolean isUp() {
		return this.change != null && this.change > 0;
	}

	public boolean isDown() {
		return this.change != null && this.change < 0;
	}

	public boolean isMarketIndex() {
		return this.symbol.startsWith("^");
	}

}
