/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.car.ui.recyclerview;

import static com.android.car.ui.utils.CarUiUtils.requireViewByRefId;

import android.content.res.Resources;
import android.os.Handler;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.OrientationHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.android.car.ui.R;
import com.android.car.ui.utils.CarUiUtils;

/**
 * The default scroll bar widget for the {@link CarUiRecyclerView}.
 *
 * <p>Inspired by {@link androidx.car.widget.PagedListView}. Most pagination and scrolling logic
 * has been ported from the PLV with minor updates.
 */
class DefaultScrollBar implements ScrollBar {


    private float mButtonDisabledAlpha;
    private CarUiSnapHelper mSnapHelper;

    private View mScrollView;
    private View mScrollTrack;
    private View mScrollThumb;
    private View mUpButton;
    private View mDownButton;
    private int mScrollbarThumbMinHeight;

    private RecyclerView mRecyclerView;

    private final Interpolator mPaginationInterpolator = new AccelerateDecelerateInterpolator();

    private final Handler mHandler = new Handler();

    private OrientationHelper mOrientationHelper;

    private OnContinuousScrollListener mPageUpOnContinuousScrollListener;
    private OnContinuousScrollListener mPageDownOnContinuousScrollListener;

    @Override
    public void initialize(RecyclerView rv, View scrollView) {
        mRecyclerView = rv;

        mScrollView = scrollView;

        Resources res = rv.getContext().getResources();

        mButtonDisabledAlpha = CarUiUtils.getFloat(res, R.dimen.car_ui_button_disabled_alpha);
        mScrollbarThumbMinHeight = rv.getContext().getResources()
                .getDimensionPixelSize(R.dimen.car_ui_scrollbar_min_thumb_height);

        getRecyclerView().addOnScrollListener(mRecyclerViewOnScrollListener);
        getRecyclerView().getRecycledViewPool().setMaxRecycledViews(0, 12);

        mUpButton = requireViewByRefId(mScrollView, R.id.car_ui_scrollbar_page_up);
        View.OnClickListener paginateUpButtonOnClickListener = v -> pageUp();
        mUpButton.setOnClickListener(paginateUpButtonOnClickListener);
        mPageUpOnContinuousScrollListener = new OnContinuousScrollListener(rv.getContext(),
                paginateUpButtonOnClickListener);
        mUpButton.setOnTouchListener(mPageUpOnContinuousScrollListener);


        mDownButton = requireViewByRefId(mScrollView, R.id.car_ui_scrollbar_page_down);
        View.OnClickListener paginateDownButtonOnClickListener = v -> pageDown();
        mDownButton.setOnClickListener(paginateDownButtonOnClickListener);
        mPageDownOnContinuousScrollListener = new OnContinuousScrollListener(rv.getContext(),
                paginateDownButtonOnClickListener);
        mDownButton.setOnTouchListener(mPageDownOnContinuousScrollListener);

        mScrollTrack = requireViewByRefId(mScrollView, R.id.car_ui_scrollbar_track);
        mScrollThumb = requireViewByRefId(mScrollView, R.id.car_ui_scrollbar_thumb);

        mSnapHelper = new CarUiSnapHelper(rv.getContext());
        getRecyclerView().setOnFlingListener(null);
        mSnapHelper.attachToRecyclerView(getRecyclerView());

        // enables fast scrolling.
        FastScroller fastScroller = new FastScroller(mRecyclerView, mScrollTrack, mScrollView);
        fastScroller.enable();

        mScrollView.setVisibility(View.INVISIBLE);
        mScrollView.addOnLayoutChangeListener(
                (View v,
                        int left,
                        int top,
                        int right,
                        int bottom,
                        int oldLeft,
                        int oldTop,
                        int oldRight,
                        int oldBottom) -> mHandler.post(this::updatePaginationButtons));
    }

    public RecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    @Override
    public void requestLayout() {
        mScrollView.requestLayout();
    }

    @Override
    public void setPadding(int paddingStart, int paddingEnd) {
        mScrollView.setPadding(mScrollView.getPaddingLeft(), paddingStart,
                mScrollView.getPaddingRight(), paddingEnd);
    }

    @Override
    public void adapterChanged(@Nullable RecyclerView.Adapter adapter) {
        try {
            if (mRecyclerView.getAdapter() != null) {
                mRecyclerView.getAdapter().unregisterAdapterDataObserver(mAdapterChangeObserver);
            }
            if (adapter != null) {
                adapter.registerAdapterDataObserver(mAdapterChangeObserver);
            }
        } catch (IllegalStateException e) {
            // adapter is already registered. and we're trying to register again.
            // or adapter was not registered and we're trying to unregister again.
            // ignore.
        }
    }

    /**
     * Sets whether or not the up button on the scroll bar is clickable.
     *
     * @param enabled {@code true} if the up button is enabled.
     */
    private void setUpEnabled(boolean enabled) {
        // If the button is held down the button is disabled, the MotionEvent.ACTION_UP event on
        // button release will not be sent to cancel pending scrolls. Manually cancel any pending
        // scroll.
        if (!enabled) {
            mPageUpOnContinuousScrollListener.cancelPendingScroll();
        }

        mUpButton.setEnabled(enabled);
        mUpButton.setAlpha(enabled ? 1f : mButtonDisabledAlpha);
    }

    /**
     * Sets whether or not the down button on the scroll bar is clickable.
     *
     * @param enabled {@code true} if the down button is enabled.
     */
    private void setDownEnabled(boolean enabled) {
        // If the button is held down the button is disabled, the MotionEvent.ACTION_UP event on
        // button release will not be sent to cancel pending scrolls. Manually cancel any pending
        // scroll.
        if (!enabled) {
            mPageDownOnContinuousScrollListener.cancelPendingScroll();
        }

        mDownButton.setEnabled(enabled);
        mDownButton.setAlpha(enabled ? 1f : mButtonDisabledAlpha);
    }

    /**
     * Returns whether or not the down button on the scroll bar is clickable.
     *
     * @return {@code true} if the down button is enabled. {@code false} otherwise.
     */
    private boolean isDownEnabled() {
        return mDownButton.isEnabled();
    }

    /**
     * Sets the range, offset and extent of the scroll bar. The range represents the size of a
     * container for the scrollbar thumb; offset is the distance from the start of the container to
     * where the thumb should be; and finally, extent is the size of the thumb.
     *
     * <p>These values can be expressed in arbitrary units, so long as they share the same units.
     * The values should also be positive.
     *
     * @param range  The range of the scrollbar's thumb
     * @param offset The offset of the scrollbar's thumb
     * @param extent The extent of the scrollbar's thumb
     */
    private void setParameters(
            @IntRange(from = 0) int range,
            @IntRange(from = 0) int offset,
            @IntRange(from = 0) int extent) {
        // Not laid out yet, so values cannot be calculated.
        if (!mScrollView.isLaidOut()) {
            return;
        }

        // If the scroll bars aren't visible, then no need to update.
        if (mScrollView.getVisibility() == View.GONE || range == 0) {
            return;
        }

        int thumbLength = calculateScrollThumbLength(range, extent);
        int thumbOffset = calculateScrollThumbOffset(range, offset, thumbLength);

        // Sets the size of the thumb and request a redraw if needed.
        ViewGroup.LayoutParams lp = mScrollThumb.getLayoutParams();

        if (lp.height != thumbLength) {
            lp.height = thumbLength;
            mScrollThumb.requestLayout();
        }

        moveY(mScrollThumb, thumbOffset);
    }

    /**
     * Calculates and returns how big the scroll bar thumb should be based on the given range and
     * extent.
     *
     * @param range  The total amount of space the scroll bar is allowed to roam over.
     * @param extent The amount of space that the scroll bar takes up relative to the range.
     * @return The height of the scroll bar thumb in pixels.
     */
    private int calculateScrollThumbLength(int range, int extent) {
        // Scale the length by the available space that the thumb can fill.
        return Math.max(Math.round(((float) extent / range) * mScrollTrack.getHeight()),
                mScrollbarThumbMinHeight);
    }

    /**
     * Calculates and returns how much the scroll thumb should be offset from the top of where it
     * has been laid out.
     *
     * @param range       The total amount of space the scroll bar is allowed to roam over.
     * @param offset      The amount the scroll bar should be offset, expressed in the same units as
     *                    the given range.
     * @param thumbLength The current length of the thumb in pixels.
     * @return The amount the thumb should be offset in pixels.
     */
    private int calculateScrollThumbOffset(int range, int offset, int thumbLength) {
        // Ensure that if the user has reached the bottom of the list, then the scroll bar is
        // aligned to the bottom as well. Otherwise, scale the offset appropriately.
        // This offset will be a value relative to the parent of this scrollbar, so start by where
        // the top of scrollbar track is.
        return mScrollTrack.getTop()
                + (isDownEnabled()
                ? Math.round(((float) offset / range) * (mScrollTrack.getHeight() - thumbLength))
                : mScrollTrack.getHeight() - thumbLength);
    }

    /**
     * Moves the given view to the specified 'y' position.
     */
    private void moveY(final View view, float newPosition) {
        view.animate()
                .y(newPosition)
                .setDuration(/* duration= */ 0)
                .setInterpolator(mPaginationInterpolator)
                .start();
    }

    private final RecyclerView.OnScrollListener mRecyclerViewOnScrollListener =
            new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    updatePaginationButtons();
                    cacheChildrenHeight(recyclerView.getLayoutManager());
                }
            };
    private final SparseArray<Integer> mChildHeightByAdapterPosition = new SparseArray();

    private final RecyclerView.AdapterDataObserver mAdapterChangeObserver =
            new RecyclerView.AdapterDataObserver() {
                @Override
                public void onChanged() {
                    clearCachedHeights();
                }
                @Override
                public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
                    clearCachedHeights();
                }
                @Override
                public void onItemRangeChanged(int positionStart, int itemCount) {
                    clearCachedHeights();
                }
                @Override
                public void onItemRangeInserted(int positionStart, int itemCount) {
                    clearCachedHeights();
                }
                @Override
                public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
                    clearCachedHeights();
                }
                @Override
                public void onItemRangeRemoved(int positionStart, int itemCount) {
                    clearCachedHeights();
                }
            };

    private void clearCachedHeights() {
        mChildHeightByAdapterPosition.clear();
        cacheChildrenHeight(mRecyclerView.getLayoutManager());
    }

    private void cacheChildrenHeight(@Nullable RecyclerView.LayoutManager layoutManager) {
        if (layoutManager == null) {
            return;
        }
        for (int i = 0; i < layoutManager.getChildCount(); i++) {
            View child = layoutManager.getChildAt(i);
            int childPosition = layoutManager.getPosition(child);
            if (mChildHeightByAdapterPosition.indexOfKey(childPosition) < 0) {
                mChildHeightByAdapterPosition.put(childPosition, child.getHeight());
            }
        }
    }

    private int estimateNextPositionScrollUp(int currentPos, int scrollDistance,
            OrientationHelper orientationHelper) {
        int nextPos = 0;
        int distance = 0;
        for (int i = currentPos - 1; i >= 0; i--) {
            if (mChildHeightByAdapterPosition.indexOfKey(i) < 0) {
                // Use the average height estimate when there is not enough data
                nextPos = mSnapHelper.estimateNextPositionDiffForScrollDistance(orientationHelper,
                        -scrollDistance);
                break;
            }
            if ((distance + mChildHeightByAdapterPosition.get(i)) > Math.abs(scrollDistance)) {
                nextPos = i - currentPos + 1;
                break;
            }
            distance += mChildHeightByAdapterPosition.get(i);
        }
        return nextPos;
    }

    private OrientationHelper getOrientationHelper(RecyclerView.LayoutManager layoutManager) {
        if (mOrientationHelper == null || mOrientationHelper.getLayoutManager() != layoutManager) {
            // CarUiRecyclerView is assumed to be a list that always vertically scrolls.
            mOrientationHelper = OrientationHelper.createVerticalHelper(layoutManager);
        }
        return mOrientationHelper;
    }

    /**
     * Scrolls the contents of the RecyclerView up a page. A page is defined as the height of the
     * {@code CarUiRecyclerView}.
     *
     * <p>The resulting first item in the list will be snapped to so that it is completely visible.
     * If this is not possible due to the first item being taller than the containing {@code
     * CarUiRecyclerView}, then the snapping will not occur.
     */
    void pageUp() {
        int currentOffset = getRecyclerView().computeVerticalScrollOffset();
        RecyclerView.LayoutManager layoutManager = getRecyclerView().getLayoutManager();
        if (layoutManager == null || layoutManager.getChildCount() == 0 || currentOffset == 0) {
            return;
        }

        // Use OrientationHelper to calculate scroll distance in order to match snapping behavior.
        OrientationHelper orientationHelper = getOrientationHelper(layoutManager);
        int screenSize = orientationHelper.getTotalSpace();
        int scrollDistance = screenSize;

        View currentPosView = getFirstMostVisibleChild(orientationHelper);
        int currentPos = currentPosView != null ? mRecyclerView.getLayoutManager().getPosition(
                currentPosView) : 0;
        int nextPos = estimateNextPositionScrollUp(currentPos,
                scrollDistance - Math.max(0, orientationHelper.getStartAfterPadding()
                        - orientationHelper.getDecoratedStart(currentPosView)), orientationHelper);
        if (nextPos == 0) {
            // Distance should always be positive. Negate its value to scroll up.
            mRecyclerView.smoothScrollBy(0, -scrollDistance);
        } else {
            mRecyclerView.smoothScrollToPosition(Math.max(0, currentPos + nextPos));
        }
    }

    private View getFirstMostVisibleChild(OrientationHelper helper) {
        float mostVisiblePercent = 0;
        View mostVisibleView = null;

        for (int i = 0; i < getRecyclerView().getLayoutManager().getChildCount(); i++) {
            View child = getRecyclerView().getLayoutManager().getChildAt(i);
            float visiblePercentage = CarUiSnapHelper.getPercentageVisible(child, helper);
            if (visiblePercentage == 1f) {
                mostVisibleView = child;
                break;
            } else if (visiblePercentage > mostVisiblePercent) {
                mostVisiblePercent = visiblePercentage;
                mostVisibleView = child;
            }
        }

        return mostVisibleView;
    }

    /**
     * Scrolls the contents of the RecyclerView down a page. A page is defined as the height of the
     * {@code CarUiRecyclerView}.
     *
     * <p>This method will attempt to bring the last item in the list as the first item. If the
     * current first item in the list is taller than the {@code CarUiRecyclerView}, then it will be
     * scrolled the length of a page, but not snapped to.
     */
    void pageDown() {
        RecyclerView.LayoutManager layoutManager = getRecyclerView().getLayoutManager();
        if (layoutManager == null || layoutManager.getChildCount() == 0) {
            return;
        }

        OrientationHelper orientationHelper = getOrientationHelper(layoutManager);
        int screenSize = orientationHelper.getTotalSpace();
        int scrollDistance = screenSize;

        View currentPosView = getFirstMostVisibleChild(orientationHelper);

        // If current view is partially visible and bottom of the view is below visible area of
        // the recyclerview either scroll down one page (screenSize) or enough to align the bottom
        // of the view with the bottom of the recyclerview. Note that this will not cause a snap,
        // because the current view is already snapped to the top or it wouldn't be the most
        // visible view.
        if (layoutManager.isViewPartiallyVisible(currentPosView,
                /* completelyVisible= */ false, /* acceptEndPointInclusion= */ false)
                        && orientationHelper.getDecoratedEnd(currentPosView)
                                > orientationHelper.getEndAfterPadding()) {
            scrollDistance = Math.min(screenSize,
                    orientationHelper.getDecoratedEnd(currentPosView)
                            - orientationHelper.getEndAfterPadding());
        }

        // Iterate over the childview (bottom to top) and stop when we find the first
        // view that we can snap to and the scroll size is less than max scroll size (screenSize)
        for (int i = layoutManager.getChildCount() - 1; i >= 0; i--) {
            View child = layoutManager.getChildAt(i);

            // Ignore the child if it's above the currentview, as scrolldown will only move down.
            // Note that in case of gridview, child will not be the same as the currentview.
            if (orientationHelper.getDecoratedStart(child)
                    <= orientationHelper.getDecoratedStart(currentPosView)) {
                break;
            }

            // Ignore the child if the scroll distance is bigger than the max scroll size
            if (orientationHelper.getDecoratedStart(child)
                    - orientationHelper.getStartAfterPadding() <= screenSize) {
                // If the child is already fully visible we can scroll even further.
                if (orientationHelper.getDecoratedEnd(child)
                        <= orientationHelper.getEndAfterPadding()) {
                    scrollDistance = orientationHelper.getDecoratedEnd(child)
                            - orientationHelper.getStartAfterPadding();
                } else {
                    scrollDistance = orientationHelper.getDecoratedStart(child)
                            - orientationHelper.getStartAfterPadding();
                }
                break;
            }
        }

        mRecyclerView.smoothScrollBy(0, scrollDistance);
    }

    /**
     * Determines if scrollbar should be visible or not and shows/hides it accordingly. If this is
     * being called as a result of adapter changes, it should be called after the new layout has
     * been calculated because the method of determining scrollbar visibility uses the current
     * layout. If this is called after an adapter change but before the new layout, the visibility
     * determination may not be correct.
     */
    private void updatePaginationButtons() {

        boolean isAtStart = isAtStart();
        boolean isAtEnd = isAtEnd();
        RecyclerView.LayoutManager layoutManager = getRecyclerView().getLayoutManager();

        // enable/disable the button before the view is shown. So there is no flicker.
        setUpEnabled(!isAtStart);
        setDownEnabled(!isAtEnd);

        if ((isAtStart && isAtEnd) || layoutManager == null || layoutManager.getItemCount() == 0) {
            mScrollView.setVisibility(View.INVISIBLE);
        } else {
            mScrollView.setVisibility(View.VISIBLE);
        }

        if (layoutManager == null) {
            return;
        }

        if (layoutManager.canScrollVertically()) {
            setParameters(
                    getRecyclerView().computeVerticalScrollRange(),
                    getRecyclerView().computeVerticalScrollOffset(),
                    getRecyclerView().computeVerticalScrollExtent());
        } else {
            setParameters(
                    getRecyclerView().computeHorizontalScrollRange(),
                    getRecyclerView().computeHorizontalScrollOffset(),
                    getRecyclerView().computeHorizontalScrollExtent());
        }

        mScrollView.invalidate();
    }

    /**
     * Returns {@code true} if the RecyclerView is completely displaying the first item.
     */
    boolean isAtStart() {
        return mSnapHelper.isAtStart(getRecyclerView().getLayoutManager());
    }

    /**
     * Returns {@code true} if the RecyclerView is completely displaying the last item.
     */
    boolean isAtEnd() {
        return mSnapHelper.isAtEnd(getRecyclerView().getLayoutManager());
    }
}
