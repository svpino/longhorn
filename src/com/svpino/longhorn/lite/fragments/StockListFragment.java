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
package com.svpino.longhorn.lite.fragments;

import static com.svpino.longhorn.lite.artifacts.Extensions.isHoneycombOrLater;
import static com.svpino.longhorn.lite.artifacts.Extensions.isIceCreamSandwichOrLater;
import static com.svpino.longhorn.lite.artifacts.Extensions.isPriorIceCreamSandwich;

import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.ActionMode;
import android.view.DragEvent;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.DragShadowBuilder;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.ShareActionProvider;
import android.widget.ShareActionProvider.OnShareTargetSelectedListener;
import android.widget.TableLayout;
import android.widget.TableRow.LayoutParams;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.svpino.longhorn.lite.R;
import com.svpino.longhorn.lite.artifacts.Extensions;
import com.svpino.longhorn.lite.artifacts.Extensions.PatternBackgroundColor;
import com.svpino.longhorn.lite.artifacts.StockTileProcessor;
import com.svpino.longhorn.lite.artifacts.StockTileProcessor.OnDragTileListener;
import com.svpino.longhorn.lite.artifacts.StockTileViewHolder;
import com.svpino.longhorn.lite.artifacts.TabFragment;
import com.svpino.longhorn.lite.artifacts.back.BackStack;
import com.svpino.longhorn.lite.artifacts.back.StockOverviewBackStackItem;
import com.svpino.longhorn.lite.layouts.StockOverviewLayout;
import com.svpino.longhorn.lite.model.Stock;

public class StockListFragment extends Fragment implements TabFragment, OnClickListener, OnLongClickListener, OnDragTileListener {

	private final static String LOG_TAG = StockListFragment.class.getName();

	private final static int SWIPE_THRESHOLD_VELOCITY = 150;
	private final static int STOCK_OVERVIEW_HEIGHT = 185;

	public enum StockOverviewState {
		HIDDEN, OVERVIEW
	}

	private TableLayout tableLayout;
	private LayoutInflater inflater;
	private View currentlySelectedTile;
	private SparseArray<View> tiles;
	private SearchView searchView;
	private View focusedTileDuringDragAndDrop = null;
	private ActionMode.Callback actionModeCallback;
	private ActionMode actionMode;
	private List<Integer> selectedTiles;
	private AlertDialog removeSelectedStocksDialog;
	private MenuItem searchMenuItem;
	private StockListFragmentCallback callback;

	private ViewFlipper viewFlipper;
	private Animation flipInNextAnimation;
	private Animation flipInPreviousAnimation;
	private Animation flipOutNextAnimation;
	private AnimateStockOverviewContentCallback animateStockOverviewContentCallback;
	private Animation flipOutPreviousAnimation;
	private StockOverviewState stockOverviewState = null;
	private GestureDetector viewFlipperGestureDetector;
	private AlertDialog tileContextualMenuDialog;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);

		this.selectedTiles = new ArrayList<Integer>();
		this.tiles = new SparseArray<View>();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_stock_list, container, false);

		setHasOptionsMenu(true);

		if (isHoneycombOrLater()) {
			this.actionModeCallback = new ActionModeCallback();
		}

		this.tableLayout = (TableLayout) view.findViewById(R.id.tableLayout);

		this.viewFlipper = (ViewFlipper) view.findViewById(R.id.viewFlipper);

		StockOverviewLayout stockOverviewLayout1 = new StockOverviewLayout(getActivity());
		StockOverviewLayout stockOverviewLayout2 = new StockOverviewLayout(getActivity());

		this.viewFlipper.addView(stockOverviewLayout1);
		this.viewFlipper.addView(stockOverviewLayout2);

		this.animateStockOverviewContentCallback = new AnimateStockOverviewContentCallback();

		this.flipInNextAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.flip_in_next);
		this.flipInNextAnimation.setAnimationListener(new StockOverviewAnimationListener(this.animateStockOverviewContentCallback));

		this.flipInPreviousAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.flip_in_previous);
		this.flipInPreviousAnimation.setAnimationListener(new StockOverviewAnimationListener(this.animateStockOverviewContentCallback));

		this.flipOutNextAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.flip_out_next);
		this.flipOutPreviousAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.flip_out_previous);

		this.viewFlipperGestureDetector = new GestureDetector(getActivity(), new ViewFlipperGestureDetector());
		this.viewFlipper.setOnTouchListener(new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				if (StockListFragment.this.viewFlipperGestureDetector.onTouchEvent(event)) {
					return false;
				}

				return true;
			}
		});

		createDashboardTiles(true);

		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedState) {
		if (this.stockOverviewState == null) {
			setStockOverviewState(StockOverviewState.HIDDEN);
		}

		if (isOverviewVisible()) {
			StockOverviewBackStackItem lastStockOverviewAddedToBackStack = this.callback.getBackStack().getLastStockOverviewAddedToBackStack();
			if (lastStockOverviewAddedToBackStack != null) {
				displayStockOverview(lastStockOverviewAddedToBackStack.getStock(), false);
			}
		}

		super.onActivityCreated(savedState);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		this.callback = (StockListFragmentCallback) activity;
	}

	@Override
	public void onResume() {
		if (isHoneycombOrLater() && this.actionMode != null) {
			this.actionMode = null;
			showContextualActionBar();

			for (Integer position : this.selectedTiles) {
				selectTile(this.tiles.get(position), false);
			}
		}

		super.onResume();
	}

	@Override
	public void onPause() {
		if (this.removeSelectedStocksDialog != null) {
			this.removeSelectedStocksDialog.dismiss();
		}

		if (this.tileContextualMenuDialog != null) {
			this.tileContextualMenuDialog.dismiss();
		}

		super.onPause();
	}

	@TargetApi(14)
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.fragment_stock_list, menu);
		this.searchMenuItem = menu.findItem(R.id.menu_item_add);

		if (isIceCreamSandwichOrLater()) {
			SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
			this.searchView = (SearchView) this.searchMenuItem.getActionView();
			this.searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
			this.searchView.setSubmitButtonEnabled(false);
			this.searchView.setIconifiedByDefault(true);

			final ShareActionProvider shareActionProvider = (ShareActionProvider) menu.findItem(R.id.menu_item_share).getActionProvider();
			shareActionProvider.setShareIntent(getDefaultShareIntent(null));
			shareActionProvider.setOnShareTargetSelectedListener(new OnShareTargetSelectedListener() {

				@Override
				public boolean onShareTargetSelected(ShareActionProvider source, Intent intent) {
					Intent defaultShareIntent = getDefaultShareIntent(null);
					shareActionProvider.setShareIntent(defaultShareIntent);
					return false;
				}
			});
		}

		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		MenuItem shareMenuItem = menu.findItem(R.id.menu_item_share);
		shareMenuItem.setVisible(isOverviewVisible());

		super.onPrepareOptionsMenu(menu);
	}

	@TargetApi(11)
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_item_add:
				requestSearch();
				return true;
			case R.id.menu_item_share: {
				if (isPriorIceCreamSandwich()) {
					showShareDialog(null);
					return true;
				}
			}
		}

		return false;
	}

	private Intent getDefaultShareIntent(Integer position) {
		Intent intent = new Intent(android.content.Intent.ACTION_SEND);
		intent.setType("text/plain");

		Stock stock = null;
		if (position == null) {
			if (isOverviewVisible() && this.viewFlipper != null && this.viewFlipper.getTag() != null) {
				stock = this.callback.getStockList().get((Integer) this.viewFlipper.getTag());
			}
		}
		else {
			stock = this.callback.getStockList().get(position);
		}

		if (stock != null) {
			Resources resources = getActivity().getResources();
			intent.putExtra("symbol", stock.getSymbol());
			intent.putExtra(android.content.Intent.EXTRA_SUBJECT, String.format(resources.getString(R.string.fragment_stock_overview_share_subject), stock.getSymbol(), stock.getStringPrice()));
			intent.putExtra(android.content.Intent.EXTRA_TEXT,
				String.format(resources.getString(R.string.fragment_stock_overview_share_message),
					stock.getSymbol(),
					stock.getStringPrice(),
					stock.getStringChangeAndChangePercentage(),
					resources.getString(R.string.application_name)));
		}

		return intent;
	}

	private void display(Stock stock) {
		int position = this.callback.getStockList().indexOf(stock);

		if (this.stockOverviewState == StockOverviewState.HIDDEN) {
			displayStockOverview(position, true);
		}
		else {
			animateStockOverviewToItem(position);
		}
	}

	private void displayStockOverview(Integer position, boolean animate) {
		if (position != null) {
			setCurrentTile(position);
			setStockOverviewState(StockOverviewState.OVERVIEW);
		}

		displayStockInViewFlipper(position);

		RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, Extensions.dpToPixels(getResources(), STOCK_OVERVIEW_HEIGHT));
		layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
		layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
		this.viewFlipper.setLayoutParams(layoutParams);

		if (animate) {
			Animation animation = AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.slide_in_from_bottom);
			animation.setAnimationListener(new StockOverviewAnimationListener(this.animateStockOverviewContentCallback));
			this.viewFlipper.setVisibility(View.VISIBLE);
			this.viewFlipper.startAnimation(animation);
		}
		else {
			this.viewFlipper.setVisibility(View.VISIBLE);
		}

		collapseSearchActionView();
		invalidateOptionsMenu();
	}

	@TargetApi(14)
	private void collapseSearchActionView() {
		if (isIceCreamSandwichOrLater() && this.searchMenuItem != null && this.searchMenuItem.isActionViewExpanded()) {
			this.searchMenuItem.collapseActionView();
		}
	}

	public void hideStockOverview() {
		hideStockOverview(null);
	}

	public void hideStockOverview(final AnimationEndCallback animationEndCallback) {
		if (this.stockOverviewState != null && this.stockOverviewState != StockOverviewState.HIDDEN) {
			setStockOverviewState(StockOverviewState.HIDDEN);

			Animation animation = AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.slide_out_to_bottom);
			animation.setAnimationListener(new StockOverviewAnimationListener(new AnimationEndCallback() {

				@Override
				public void execute() {
					StockListFragment.this.viewFlipper.setVisibility(View.GONE);
					StockListFragment.this.viewFlipper.setInAnimation(null);
					StockListFragment.this.viewFlipper.setOutAnimation(null);

					setCurrentTile(null);

					if (animationEndCallback != null) {
						animationEndCallback.execute();
					}
				}
			}));

			this.viewFlipper.startAnimation(animation);

			invalidateOptionsMenu();
		}
		else if (animationEndCallback != null) {
			animationEndCallback.execute();
		}
	}

	@TargetApi(11)
	private void invalidateOptionsMenu() {
		if (isHoneycombOrLater()) {
			getActivity().invalidateOptionsMenu();
		}
	}

	private void animateStockOverviewToNextItem() {
		this.viewFlipper.setInAnimation(this.flipInPreviousAnimation);
		this.viewFlipper.setOutAnimation(this.flipOutPreviousAnimation);

		int position = (Integer) this.viewFlipper.getTag() + 1;
		if (position >= this.callback.getStockList().size()) {
			position = 0;
		}

		if (!((Integer) this.viewFlipper.getTag()).equals(position)) {
			displayStockInViewFlipper(position);
			setCurrentTile((Integer) this.viewFlipper.getTag());
		}

		invalidateOptionsMenu();
	}

	private void animateStockOverviewToPreviousItem() {
		this.viewFlipper.setInAnimation(this.flipInNextAnimation);
		this.viewFlipper.setOutAnimation(this.flipOutNextAnimation);

		int position = (Integer) this.viewFlipper.getTag() - 1;
		if (position < 0) {
			position = this.callback.getStockList().size() - 1;
		}

		if (!((Integer) this.viewFlipper.getTag()).equals(position)) {
			displayStockInViewFlipper(position);
			setCurrentTile((Integer) this.viewFlipper.getTag());
		}

		invalidateOptionsMenu();
	}

	private void displayStockInViewFlipper(Integer position) {
		int availableFlipperChild = this.viewFlipper.getDisplayedChild() == 0
			? 1
			: 0;

		StockOverviewLayout stockOverviewLayout = (StockOverviewLayout) this.viewFlipper.getChildAt(availableFlipperChild);

		if (position != null) {
			stockOverviewLayout.setStock(this.callback.getStockList().get(position));
		}
		else {
			stockOverviewLayout.setStock(null);
		}

		this.viewFlipper.setTag(position);
		this.viewFlipper.setDisplayedChild(availableFlipperChild);
	}

	private void animateStockOverviewToItem(Integer position) {
		Integer currentStock = (Integer) this.viewFlipper.getTag();

		int animateFrom = currentStock == null
			? Integer.MIN_VALUE
			: currentStock;

		int animateTo = position == null
			? Integer.MAX_VALUE
			: position;

		if (animateTo < animateFrom) {
			this.viewFlipper.setInAnimation(this.flipInNextAnimation);
			this.viewFlipper.setOutAnimation(this.flipOutNextAnimation);
		}
		else {
			this.viewFlipper.setInAnimation(this.flipInPreviousAnimation);
			this.viewFlipper.setOutAnimation(this.flipOutPreviousAnimation);
		}

		setCurrentTile(position);
		displayStockInViewFlipper(position);

		invalidateOptionsMenu();
	}

	private void setCurrentTile(Integer position) {
		if (position != null && !position.equals(Integer.MAX_VALUE)) {
			this.callback.getBackStack().add(new StockOverviewBackStackItem(position));
		}
		else {
			this.callback.getBackStack().removeAllStockOverviewItems();
		}

		setSelection(position);
	}

	public boolean back() {
		boolean handled = false;

		if (this.stockOverviewState != StockOverviewState.HIDDEN) {
			if (this.callback.getBackStack().getStockOverviewItemCount() == 0) {
				hideStockOverview();
				handled = true;
			}
			else {
				animateStockOverviewToItem(this.callback.getBackStack().getLastStockOverviewAddedToBackStack().getStock());
				handled = true;
			}
		}

		return handled;
	}

	private boolean isOverviewVisible() {
		return this.stockOverviewState != null && this.stockOverviewState != StockOverviewState.HIDDEN;
	}

	@TargetApi(11)
	private void showContextualActionBar() {
		hideStockOverview();

		if (this.actionMode == null) {
			this.actionMode = ((FragmentActivity) getActivity()).startActionMode(this.actionModeCallback);
			updateContextualActionBar();
		}
	}

	@TargetApi(11)
	@Override
	public void hideContextualActionBar() {
		if (isHoneycombOrLater()) {
			if (this.actionMode != null) {
				this.actionMode.finish();
			}
		}
	}

	@Override
	public void onClick(View view) {
		StockTileViewHolder stockTileViewHolder = (StockTileViewHolder) view.getTag();
		if (stockTileViewHolder != null) {
			if (actionMode != null) {
				selectTile(view, true);
			}
			else {
				if (!view.equals(this.currentlySelectedTile)) {
					display(stockTileViewHolder.getStock());
				}
			}
		}
		else {
			requestSearch();
		}
	}

	@Override
	public boolean onLongClick(View view) {
		if (isHoneycombOrLater()) {
			if (this.actionMode == null) {
				showContextualActionBar();
				selectTile(view, false);
				return true;
			}

			startDraggingTile(view);
			return true;
		}
		else {
			selectTile(view, false);
			showTileContextualMenu(view);
			return true;
		}
	}

	private void showTileContextualMenu(final View view) {
		if (this.tileContextualMenuDialog == null || !this.tileContextualMenuDialog.isShowing()) {
			final StockTileViewHolder stockTileViewHolder = (StockTileViewHolder) view.getTag();
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

			builder.setTitle(String.format(getString(R.string.fragment_stock_list_tile_contextual_menu_title), stockTileViewHolder.getStock().getSymbol()));

			builder.setItems(new CharSequence[] {
					getString(R.string.fragment_stock_list_tile_contextual_menu_share_item),
					getString(R.string.fragment_stock_list_tile_contextual_menu_delete_item),
					getString(R.string.label_cancel) }, new DialogInterface.OnClickListener() {

				public void onClick(DialogInterface dialog, int item) {
					switch (item) {
						case 0:
							showShareDialog(view);
							break;
						case 1:
							dialog.dismiss();
							showDeleteWarningDialog(stockTileViewHolder.getPosition());
							break;
						case 2:
							dialog.dismiss();
							break;
					}
				}
			});

			this.tileContextualMenuDialog = builder.create();
			this.tileContextualMenuDialog.setOnDismissListener(new OnDismissListener() {

				@Override
				public void onDismiss(DialogInterface dialog) {
					StockListFragment.this.selectedTiles.clear();
					StockTileProcessor.updateTileColorBasedOnStock(StockListFragment.this, view);
				}
			});

			this.tileContextualMenuDialog.show();
		}
	}

	private void selectTile(View tile, boolean toggle) {
		StockTileViewHolder stockTileViewHolder = (StockTileViewHolder) tile.getTag();

		if (!stockTileViewHolder.isFixed()) {
			if (toggle && this.selectedTiles.contains(stockTileViewHolder.getPosition())) {
				this.selectedTiles.remove((Integer) stockTileViewHolder.getPosition());

				if (this.selectedTiles.size() == 0 && !isHoneycombOrLater()) {
					hideContextualActionBar();
				}
			}
			else if (!this.selectedTiles.contains(stockTileViewHolder.getPosition())) {
				this.selectedTiles.add(stockTileViewHolder.getPosition());
			}

			StockTileProcessor.updateTileColor(this, tile, this.selectedTiles);
			updateContextualActionBar();
		}
	}

	@TargetApi(11)
	private void updateContextualActionBar() {
		if (isHoneycombOrLater()) {
			if (this.actionMode != null) {
				this.actionMode.setTitle(String.format(getResources().getString(R.string.label_cab_selected), this.selectedTiles.size()));
				this.actionMode.invalidate();
			}
		}
	}

	@TargetApi(11)
	@Override
	public boolean onDrag(View tile2, DragEvent event) {
		View tile1 = (View) event.getLocalState();

		StockTileViewHolder stockTileViewHolder1 = (StockTileViewHolder) tile1.getTag();
		StockTileViewHolder stockTileViewHolder2 = (StockTileViewHolder) tile2.getTag();

		switch (event.getAction()) {
			case DragEvent.ACTION_DRAG_STARTED:
				tile1.setVisibility(View.INVISIBLE);

				if (stockTileViewHolder1.getPosition() != stockTileViewHolder2.getPosition()) {
					return true;
				}

				return false;

			case DragEvent.ACTION_DRAG_ENTERED:
				this.focusedTileDuringDragAndDrop = tile2;
				StockTileProcessor.updateTileColorToDropReceptor(tile2);
				tile2.invalidate();

				return true;

			case DragEvent.ACTION_DRAG_LOCATION:
				return true;

			case DragEvent.ACTION_DRAG_EXITED:
				this.focusedTileDuringDragAndDrop = null;
				StockTileProcessor.updateTileColor(this, tile2, this.selectedTiles);
				tile2.invalidate();

				return true;

			case DragEvent.ACTION_DROP:
				this.focusedTileDuringDragAndDrop = null;
				swapTiles(tile1, tile2);
				return true;

			case DragEvent.ACTION_DRAG_ENDED:
				if (this.focusedTileDuringDragAndDrop != null) {
					swapTiles(tile1, this.focusedTileDuringDragAndDrop);
					this.focusedTileDuringDragAndDrop = null;

					return true;
				}

				StockTileProcessor.updateTileColor(this, tile1, this.selectedTiles);
				StockTileProcessor.updateTileColor(this, tile2, this.selectedTiles);

				tile1.setVisibility(View.VISIBLE);
				tile2.setVisibility(View.VISIBLE);

				tile1.invalidate();
				tile2.invalidate();

				return true;

			default:
				return true;
		}
	}

	@TargetApi(11)
	private void startDraggingTile(View tile) {
		StockTileViewHolder stockTileViewHolder = (StockTileViewHolder) tile.getTag();
		this.selectedTiles.remove((Integer) stockTileViewHolder.getPosition());
		updateContextualActionBar();

		ClipData.Item item = new ClipData.Item(stockTileViewHolder.toString());
		ClipData clipData = new ClipData(stockTileViewHolder.toString(), new String[0], item);

		View.DragShadowBuilder dragShadowBuilder = new DragShadowBuilder(tile);
		tile.startDrag(clipData, dragShadowBuilder, tile, 0);
	}

	private void swapTiles(View tile1, View tile2) {
		StockTileViewHolder stockTileViewHolder1 = (StockTileViewHolder) tile1.getTag();
		StockTileViewHolder stockTileViewHolder2 = (StockTileViewHolder) tile2.getTag();

		this.callback.reorderStocks(stockTileViewHolder1.getStock(), stockTileViewHolder2.getStock());

		this.selectedTiles.remove((Integer) stockTileViewHolder1.getPosition());
		this.selectedTiles.remove((Integer) stockTileViewHolder2.getPosition());
		updateContextualActionBar();

		Stock tempStock = stockTileViewHolder1.getStock();
		stockTileViewHolder1.setStock(stockTileViewHolder2.getStock());
		stockTileViewHolder2.setStock(tempStock);

		tile1.setTag(stockTileViewHolder1);
		tile2.setTag(stockTileViewHolder2);

		this.tiles.remove(stockTileViewHolder1.getPosition());
		this.tiles.remove(stockTileViewHolder2.getPosition());

		this.tiles.put(stockTileViewHolder1.getPosition(), tile1);
		this.tiles.put(stockTileViewHolder2.getPosition(), tile2);

		stockTileViewHolder1.refresh(getResources());
		stockTileViewHolder2.refresh(getResources());

		tile1.setVisibility(View.VISIBLE);
		tile2.setVisibility(View.VISIBLE);

		tile1.invalidate();
		tile2.invalidate();
	}

	public void setSelection(Integer position) {
		if (position != null) {
			View tile = this.tiles.get(position);

			if (this.currentlySelectedTile != null) {
				StockTileProcessor.updateTileColor(this, this.currentlySelectedTile, this.selectedTiles);
			}

			this.currentlySelectedTile = tile;

			StockTileViewHolder stockTileViewHolder = (StockTileViewHolder) tile.getTag();

			Extensions.applyPattern(
				getResources(),
				stockTileViewHolder.getTileLayout(),
				PatternBackgroundColor.BLUE);
		}
		else {
			if (this.currentlySelectedTile != null) {
				StockTileProcessor.updateTileColor(this, this.currentlySelectedTile, this.selectedTiles);
			}

			this.currentlySelectedTile = null;
		}
	}

	public void refresh(boolean hardReset) {
		hideStockOverview();
		collapseSearchActionView();
		createDashboardTiles(hardReset);
	}

	private void createDashboardTiles(boolean restart) {
		StockTileProcessor.create(
			this,
			this.tableLayout,
			this.callback.getStockList(),
			this.tiles,
			restart);
	}

	public void refreshStockInformation(List<Stock> stocks) {
		try {
			for (Stock stock : stocks) {
				View view = findTile(stock);
				StockTileViewHolder stockTileViewHolder = (StockTileViewHolder) view.getTag();
				if (stockTileViewHolder != null) {
					stockTileViewHolder.refresh(getResources(), stock);
				}

				if (this.viewFlipper != null && this.viewFlipper.getTag() != null) {
					StockOverviewLayout stockOverviewLayout = (StockOverviewLayout) this.viewFlipper.getChildAt(this.viewFlipper.getDisplayedChild());
					if (stockOverviewLayout != null && stockOverviewLayout.getStock().getSymbol().equals(stock.getSymbol())) {
						stockOverviewLayout.setStock(stock);
					}
				}
			}
		}
		catch (IllegalStateException e) {
			/*
			 * For some reason, some devices are reporting an IllegalStateException when trying to
			 * access the method
			 * getResources() from above. The full error message is the following:
			 * java.lang.IllegalStateException: Fragment StockListFragment{40540d98} not attached to
			 * Activity
			 * 
			 * Since this error happens while refreshing the stock information, I'm going to ignore
			 * it.
			 */

			Log.e(LOG_TAG, e.getMessage(), e);
		}
	}

	private View findTile(Stock stock) {
		for (int i = 0; i < this.tiles.size(); i++) {
			View view = this.tiles.valueAt(i);

			StockTileViewHolder stockTileViewHolder = (StockTileViewHolder) view.getTag();
			if (stockTileViewHolder.getStock().getSymbol().equals(stock.getSymbol())) {
				return view;
			}
		}

		return null;
	}

	@TargetApi(14)
	private void requestSearch() {
		hideContextualActionBar();

		if (isIceCreamSandwichOrLater()) {
			hideStockOverview(new AnimationEndCallback() {

				@Override
				public void execute() {
					StockListFragment.this.searchMenuItem.expandActionView();
				}
			});
		}
		else {
			getActivity().onSearchRequested();
		}
	}

	private void showDeleteWarningDialog(Integer position) {
		if (this.removeSelectedStocksDialog == null || !this.removeSelectedStocksDialog.isShowing()) {

			if (position == null) {
				if (this.selectedTiles.size() == 1) {
					position = ((StockTileViewHolder) this.tiles.get(this.selectedTiles.get(0)).getTag()).getPosition();
				}
			}

			final Stock stock = position != null
				? ((StockTileViewHolder) this.tiles.get(position).getTag()).getStock()
				: null;

			AlertDialog.Builder removeSelectedStocksDialogBuilder = new AlertDialog.Builder(getActivity());

			String message = this.callback.getRemoveWarningDialogMessage(stock);

			View view = getLayoutInflater().inflate(R.layout.dialog_message, null);
			TextView textView = (TextView) view.findViewById(R.id.textView);
			textView.setText(message);

			removeSelectedStocksDialogBuilder
				.setIcon(R.drawable.ic_warning)
				.setTitle(this.callback.getRemoveWarningDialogTitle(stock))
				.setView(view)
				.setPositiveButton(R.string.label_remove, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						removeSelectedStocks(stock);
					}
				})
				.setNegativeButton(R.string.label_cancel, null);

			this.removeSelectedStocksDialog = removeSelectedStocksDialogBuilder.create();
			this.removeSelectedStocksDialog.show();
		}
	}

	private void showShareDialog(View view) {
		Integer position = null;
		if (view != null) {
			StockTileViewHolder stockTileViewHolder = (StockTileViewHolder) view.getTag();
			position = stockTileViewHolder.getPosition();
		}

		startActivity(Intent.createChooser(getDefaultShareIntent(position), getResources().getString(R.string.fragment_stock_overview_share_dialog_title)));
	}

	private void removeSelectedStocks(Stock stock) {
		List<Stock> stockListToRemove = new ArrayList<Stock>();

		if (stock == null) {
			for (int position : this.selectedTiles) {
				stockListToRemove.add(((StockTileViewHolder) this.tiles.get(position).getTag()).getStock());
			}
		}
		else {
			stockListToRemove.add(stock);
		}

		this.callback.removeStocks(stockListToRemove);

		hideContextualActionBar();
		createDashboardTiles(false);
	}

	public StockOverviewState getStockOverviewState() {
		return this.stockOverviewState;
	}

	public void setStockOverviewState(StockOverviewState stockOverviewState) {
		this.stockOverviewState = stockOverviewState;
	}

	private LayoutInflater getLayoutInflater() {
		if (this.inflater == null) {
			this.inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		return this.inflater;
	}

	@TargetApi(11)
	private class ActionModeCallback implements ActionMode.Callback {

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.fragment_stock_list_cab, menu);

			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			MenuItem deleteMenuItem = menu.findItem(R.id.menu_item_delete);
			deleteMenuItem.setVisible(StockListFragment.this.selectedTiles.size() > 0);

			return false;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			if (item.getItemId() == R.id.menu_item_delete) {
				showDeleteWarningDialog(null);
			}

			return false;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			StockListFragment.this.actionMode = null;

			for (Integer position : StockListFragment.this.selectedTiles) {
				StockTileProcessor.updateTileColorBasedOnStock(StockListFragment.this, StockListFragment.this.tiles.get(position));
			}

			StockListFragment.this.selectedTiles.clear();
		}
	}

	public interface StockListFragmentCallback {
		public List<Stock> getStockList();

		public void removeStocks(List<Stock> stocks);

		public void reorderStocks(Stock stock1, Stock stock2);

		public String getRemoveWarningDialogTitle(Stock stock);

		public String getRemoveWarningDialogMessage(Stock stock);

		public void onStockOverviewAddedToBackStack();

		public void onStockOverviewClearBackStack();

		public BackStack getBackStack();
	}

	private interface AnimationEndCallback {
		public void execute();
	}

	private class StockOverviewAnimationListener implements AnimationListener {

		private final AnimationEndCallback callback;

		public StockOverviewAnimationListener(AnimationEndCallback callback) {
			this.callback = callback;
		}

		@Override
		public void onAnimationStart(Animation animation) {
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			if (this.callback != null) {
				this.callback.execute();
			}
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
		}

	}

	private class AnimateStockOverviewContentCallback implements AnimationEndCallback {

		@Override
		public void execute() {
			StockOverviewLayout stockOverviewLayout = (StockOverviewLayout) StockListFragment.this.viewFlipper.getChildAt(StockListFragment.this.viewFlipper.getDisplayedChild());
			stockOverviewLayout.animateContent();
		}
	}

	private class ViewFlipperGestureDetector extends SimpleOnGestureListener {

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			StockOverviewLayout stockOverviewLayout = (StockOverviewLayout) StockListFragment.this.viewFlipper.getChildAt(StockListFragment.this.viewFlipper.getDisplayedChild());

			int swipeMinDistance = stockOverviewLayout.getHeight() / 3;

			try {
				if (e1.getX() - e2.getX() > swipeMinDistance && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
					animateStockOverviewToNextItem();
					return true;
				}

				if (e2.getX() - e1.getX() > swipeMinDistance && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
					animateStockOverviewToPreviousItem();
					return true;
				}

				if (e2.getY() - e1.getY() > swipeMinDistance && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
					StockListFragment.this.hideStockOverview();
					return true;
				}

				if (e1.getY() - e2.getY() > swipeMinDistance && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
				}
			}
			catch (Exception e) {
			}

			return false;
		}
	}

}
