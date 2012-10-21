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
package com.svpino.longhorn.lite.artifacts.back;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.os.Parcelable;

public class BackStack {
	public final static int BACK_STACK_STOCK_OVERVIEW = -1;

	private List<Parcelable> backStack;

	public BackStack(Bundle savedInstanceState) {
		this.backStack = new ArrayList<Parcelable>();

		if (savedInstanceState != null && savedInstanceState.containsKey("backStack")) {
			Parcelable[] stack = savedInstanceState.getParcelableArray("backStack");

			for (int i = 0; i < stack.length; i++) {
				this.backStack.add(stack[i]);
			}
		}
	}

	public void saveState(Bundle outState) {
		Parcelable[] stack = new Parcelable[this.backStack.size()];
		for (int i = 0; i < this.backStack.size(); i++) {
			stack[i] = this.backStack.get(i);
		}

		outState.putParcelableArray("backStack", stack);
	}

	public int size() {
		return this.backStack.size();
	}

	public Parcelable removeLastItem() {
		return this.backStack.remove(this.backStack.size() - 1);
	}

	public void add(Parcelable parcelable) {
		if (parcelable instanceof StockOverviewBackStackItem) {
			int stock = ((StockOverviewBackStackItem) parcelable).getStock();
			StockOverviewBackStackItem lastStockOverviewAddedToBackStack = getLastStockOverviewAddedToBackStack();
			int lastStockAddedToBackStack = lastStockOverviewAddedToBackStack == null
				? Integer.MAX_VALUE
				: lastStockOverviewAddedToBackStack.getStock();

			if (stock != lastStockAddedToBackStack) {
				this.backStack.add(parcelable);
			}
		}
	}

	public int getStockOverviewItemCount() {
		int count = 0;
		for (int i = 0; i < this.backStack.size(); i++) {
			if (this.backStack.get(i) instanceof StockOverviewBackStackItem) {
				count++;
			}
		}

		return count;
	}

	public void removeAllStockOverviewItems() {
		int index = 0;
		while (index < this.backStack.size()) {
			if (this.backStack.get(index) instanceof StockOverviewBackStackItem) {
				this.backStack.remove(index);
			}
			else {
				index++;
			}
		}
	}

	public Parcelable getLastItem() {
		return this.backStack.get(this.backStack.size() - 1);
	}

	public StockOverviewBackStackItem getLastStockOverviewAddedToBackStack() {
		for (int i = this.backStack.size() - 1; i >= 0; i--) {
			if (this.backStack.get(i) instanceof StockOverviewBackStackItem) {
				return (StockOverviewBackStackItem) this.backStack.get(i);
			}
		}

		return null;
	}
}
