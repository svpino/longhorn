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

import static com.svpino.longhorn.lite.artifacts.Extensions.isHoneycombOrLater;

import java.util.List;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.support.v4.app.Fragment;
import android.util.SparseArray;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnDragListener;
import android.view.View.OnLongClickListener;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TableRow.LayoutParams;

import com.svpino.longhorn.lite.R;
import com.svpino.longhorn.lite.artifacts.Extensions.PatternBackgroundColor;
import com.svpino.longhorn.lite.model.Stock;

public class StockTileProcessor {

	public static void create(
		Fragment fragment,
		TableLayout tableLayout,
		List<Stock> stocks,
		SparseArray<View> tiles,
		boolean restart) {

		if (restart) {
			tableLayout.removeAllViews();
			if (tiles != null) {
				tiles.clear();
			}
		}

		tableLayout.setStretchAllColumns(true);
		tableLayout.setShrinkAllColumns(true);

		int margin = Extensions.dpToPixels(fragment.getResources(), 3);
		int height = Extensions.dpToPixels(fragment.getResources(), 90);

		int index = createFixedHeaderRow(fragment, tableLayout, stocks, tiles, height, margin);

		int row = index == 3
			? 1
			: 0;

		while (index < stocks.size()) {
			index = createStandardRow(fragment, tableLayout, stocks, tiles, height, margin, index, row);
			row++;
		}

		while (tableLayout.getChildCount() > row) {
			tableLayout.removeViewAt(tableLayout.getChildCount() - 1);
		}

		if (stocks.size() % 2 != 0) {
			TableRow tableRow = new TableRow(fragment.getActivity());

			View addNewStockTile = createTileForAddingNewStock(fragment);
			tableRow.addView(addNewStockTile, getSpannedLayoutParams(row, margin, height));

			tableLayout.addView(tableRow);
		}
		else {
			TableRow tableRow = (TableRow) tableLayout.getChildAt(tableLayout.getChildCount() - 1);
			LayoutParams layoutParams = (TableRow.LayoutParams) tableRow.getChildAt(0).getLayoutParams();
			layoutParams.bottomMargin = margin;
			layoutParams.height = height;
		}
	}

	private static int createFixedHeaderRow(
		Fragment fragment,
		TableLayout tableLayout,
		List<Stock> stocks,
		SparseArray<View> tiles,
		int height,
		int margin) {

		View view = tableLayout.getChildAt(0);

		if (view == null || view.getTag() != "fixed-header") {
			TableRow indexesTableRow = new TableRow(fragment.getActivity());

			TableRow.LayoutParams indixesLayoutParams = new TableRow.LayoutParams();
			indixesLayoutParams.topMargin = margin;
			indixesLayoutParams.rightMargin = margin;
			indixesLayoutParams.bottomMargin = margin;
			indixesLayoutParams.height = height;

			TableRow.LayoutParams lastIndexLayoutParams = new TableRow.LayoutParams();
			lastIndexLayoutParams.topMargin = margin;
			lastIndexLayoutParams.bottomMargin = margin;
			lastIndexLayoutParams.height = height;

			View tile1 = createTile(fragment, stocks.get(0), 0, false);
			View tile2 = createTile(fragment, stocks.get(1), 1, false);
			View tile3 = createTile(fragment, stocks.get(2), 2, false);

			tiles.put(0, tile1);
			tiles.put(1, tile2);
			tiles.put(2, tile3);

			indexesTableRow.addView(tile1, indixesLayoutParams);
			indexesTableRow.addView(tile2, indixesLayoutParams);
			indexesTableRow.addView(tile3, lastIndexLayoutParams);
			indexesTableRow.setTag("fixed-header");

			tableLayout.addView(indexesTableRow);
		}

		return 3;
	}

	@TargetApi(11)
	private static int createStandardRow(
		Fragment fragment,
		TableLayout tableLayout,
		List<Stock> stocks,
		SparseArray<View> tiles,
		int height,
		int margin,
		int index,
		int row) {

		Stock stock1 = stocks.get(index);
		Stock stock2 = (index + 1 < stocks.size())
			? stocks.get(index + 1)
			: null;

		if (shouldUpdateTableRow(tableLayout, row, stock1, stock2)) {
			TableRow tableRow = new TableRow(fragment.getActivity());

			boolean shouldSpanFirstTile = row % 2 != 0;
			boolean shouldSpanSecondTile = !shouldSpanFirstTile;

			if (stock2 != null) {
				View tile1 = createTile(fragment, stock1, index, shouldSpanFirstTile);
				tiles.put(index, tile1);
				tableRow.addView(tile1, shouldSpanFirstTile
					? getPartialSpannedLayoutParams(row, height, margin)
					: getNotSpannedLayoutParams(row, height, margin));

				View tile2 = createTile(fragment, stock2, index + 1, shouldSpanSecondTile);
				tiles.put(index + 1, tile2);
				tableRow.addView(tile2, shouldSpanSecondTile
					? getLastPartialSpannedLayoutParams(row, height, margin)
					: getLastNotSpannedLayoutParams(row, height, margin));
			}
			else {
				View tile1 = createTile(fragment, stock1, index, shouldSpanFirstTile);
				tiles.put(index, tile1);
				tableRow.addView(tile1, shouldSpanFirstTile
					? getPartialSpannedLayoutParams(row, height, margin)
					: getNotSpannedLayoutParams(row, height, margin));

				View tile2 = createTileForAddingNewStock(fragment);
				tiles.put(index + 1, tile2);
				tableRow.addView(tile2, shouldSpanSecondTile
					? getLastPartialSpannedLayoutParams(row, height, margin)
					: getLastNotSpannedLayoutParams(row, height, margin));
			}

			if (row < tableLayout.getChildCount()) {
				tableLayout.removeViewAt(row);
			}

			tableLayout.addView(tableRow, row);
		}

		return index + 2;
	}

	private static boolean shouldUpdateTableRow(TableLayout tableLayout, int row, Stock stock1, Stock stock2) {
		boolean shouldUpdateTableRow = true;

		TableRow currentTableRow = (TableRow) tableLayout.getChildAt(row);
		if (currentTableRow != null) {
			StockTileViewHolder tile1 = (StockTileViewHolder) currentTableRow.getChildAt(0).getTag();
			StockTileViewHolder tile2 = currentTableRow.getChildCount() == 2
				? (StockTileViewHolder) currentTableRow.getChildAt(1).getTag()
				: null;

			if (tile1 != null && (stock2 == null || tile2 != null)) {
				if (tile1.getStock().equals(stock1)) {
					if (stock2 != null && tile2.getStock().equals(stock2)) {
						shouldUpdateTableRow = false;
					}
					else if (stock2 == null && tile2 == null) {
						shouldUpdateTableRow = false;
					}
				}
			}
		}

		return shouldUpdateTableRow;
	}

	private static View createTile(
		Fragment fragment,
		Stock stock,
		int index,
		boolean spanned) {

		View view = ((LayoutInflater) fragment.getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(spanned
			? R.layout.stock_tile_spanned
			: R.layout.stock_tile_not_spanned, null);

		StockTileViewHolder stockTileViewHolder = new StockTileViewHolder(fragment.getResources(), view, stock, index, spanned);
		stockTileViewHolder.refresh(fragment.getResources());
		view.setTag(stockTileViewHolder);

		view.setOnClickListener((OnClickListener) fragment);

		if (index > 2) {
			view.setOnLongClickListener((OnLongClickListener) fragment);

			if (!stock.isMarketIndex()) {
				enableTileAsADropLocation(fragment, view);
			}

		}

		return view;
	}

	private static View createTileForAddingNewStock(Fragment fragment) {
		View view = ((LayoutInflater) fragment.getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.stock_tile_add_new_stock, null);
		Extensions.applyPattern(fragment.getResources(), view.findViewById(R.id.tileLayout), PatternBackgroundColor.BLACK);

		view.setOnClickListener((OnClickListener) fragment);

		return view;
	}

	public static void updateTileColor(Fragment fragment, View view, List<Integer> selectedTiles) {
		StockTileViewHolder stockTileViewHolder = (StockTileViewHolder) view.getTag();

		if (selectedTiles != null && selectedTiles.contains(stockTileViewHolder.getPosition())) {
			updateTileColorToSelected(fragment, view);
		}
		else {
			updateTileColorBasedOnStock(fragment, view);
		}
	}

	public static void updateTileColorBasedOnStock(Fragment fragment, View view) {
		StockTileViewHolder stockTileViewHolder = (StockTileViewHolder) view.getTag();
		Extensions.applyPattern(fragment.getResources(), stockTileViewHolder.getTileLayout(), stockTileViewHolder.getStock());
	}

	public static void updateTileColorToSelected(Fragment fragment, View view) {
		StockTileViewHolder stockTileViewHolder = (StockTileViewHolder) view.getTag();
		Extensions.applyPattern(fragment.getResources(), stockTileViewHolder.getTileLayout(), PatternBackgroundColor.BLUE);
	}

	public static void updateTileColorToDropReceptor(View view) {
		StockTileViewHolder stockTileViewHolder = (StockTileViewHolder) view.getTag();
		stockTileViewHolder.getTileLayout().setBackgroundColor(Color.BLACK);
	}

	@TargetApi(11)
	private static void enableTileAsADropLocation(final Fragment fragment, View tile) {
		if (isHoneycombOrLater()) {
			tile.setOnDragListener(new OnDragListener() {

				@Override
				public boolean onDrag(View view, DragEvent event) {
					return ((OnDragTileListener) fragment).onDrag(view, event);
				}
			});
		}
	}

	private static TableRow.LayoutParams getSpannedLayoutParams(int row, int margin, int height) {
		TableRow.LayoutParams layoutParams = new TableRow.LayoutParams();
		layoutParams.span = 3;
		layoutParams.bottomMargin = margin;
		layoutParams.height = height;

		if (row == 0) {
			layoutParams.topMargin = margin;
		}

		return layoutParams;
	}

	private static TableRow.LayoutParams getLastPartialSpannedLayoutParams(int row, int height, int margin) {
		TableRow.LayoutParams layoutParams = new TableRow.LayoutParams();
		layoutParams.span = 2;
		layoutParams.bottomMargin = margin;
		layoutParams.height = height;

		if (row == 0) {
			layoutParams.topMargin = margin;
		}

		return layoutParams;
	}

	private static TableRow.LayoutParams getPartialSpannedLayoutParams(int row, int height, int margin) {
		TableRow.LayoutParams layoutParams = new TableRow.LayoutParams();
		layoutParams.span = 2;
		layoutParams.rightMargin = margin;
		layoutParams.bottomMargin = margin;
		layoutParams.height = height;

		if (row == 0) {
			layoutParams.topMargin = margin;
		}

		return layoutParams;
	}

	private static TableRow.LayoutParams getLastNotSpannedLayoutParams(int row, int height, int margin) {
		TableRow.LayoutParams layoutParams = new TableRow.LayoutParams();
		layoutParams.bottomMargin = margin;
		layoutParams.height = height;

		if (row == 0) {
			layoutParams.topMargin = margin;
		}

		return layoutParams;
	}

	private static TableRow.LayoutParams getNotSpannedLayoutParams(int row, int height, int margin) {
		TableRow.LayoutParams layoutParams = new TableRow.LayoutParams();
		layoutParams.rightMargin = margin;
		layoutParams.bottomMargin = margin;
		layoutParams.height = height;

		if (row == 0) {
			layoutParams.topMargin = margin;
		}

		return layoutParams;
	}

	public interface OnDragTileListener {
		public boolean onDrag(View view, DragEvent event);
	}

}
