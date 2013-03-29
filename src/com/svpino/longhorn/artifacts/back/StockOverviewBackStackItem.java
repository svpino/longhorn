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
package com.svpino.longhorn.artifacts.back;

import android.os.Parcel;
import android.os.Parcelable;

public class StockOverviewBackStackItem implements Parcelable {

	private int stock;

	public static final Parcelable.Creator<StockOverviewBackStackItem> CREATOR = new Parcelable.Creator<StockOverviewBackStackItem>() {
		public StockOverviewBackStackItem createFromParcel(Parcel parcel) {
			return new StockOverviewBackStackItem(parcel);
		}

		public StockOverviewBackStackItem[] newArray(int size) {
			return new StockOverviewBackStackItem[size];
		}
	};

	public StockOverviewBackStackItem(int stock) {
		this.stock = stock;
	}

	public StockOverviewBackStackItem(Parcel parcel) {
		this(parcel.readInt());
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(this.stock);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public int getStock() {
		return this.stock;
	}

}
