/*
 * Copyright (C) 2020 The Android Open Source Project
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

import android.util.Log;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.RecyclerView;

/**
 * A {@link RecyclerView.Adapter} that can limit its content based on a given length limit which
 * can change at run-time.
 *
 * @param <T> type of the {@link RecyclerView.ViewHolder} objects used by base classes.
 */
public abstract class ContentLimitingAdapter<T extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements ContentLimiting {
    private static final String TAG = "ContentLimitingAdapter";

    private static final int SCROLLING_LIMITED_MESSAGE_VIEW_TYPE = Integer.MAX_VALUE;

    private Integer mScrollingLimitedMessageResId;
    private RangeFilter mRangeFilter = new PassThroughFilter();
    private RecyclerView mRecyclerView;
    private boolean mIsLimiting = false;

    /**
     * Returns the viewType value to use for the scrolling limited message views.
     *
     * Override this method to provide your own alternative value if {@link Integer#MAX_VALUE} is
     * a viewType value already in-use by your adapter.
     */
    public int getScrollingLimitedMessageViewType() {
        return SCROLLING_LIMITED_MESSAGE_VIEW_TYPE;
    }

    @Override
    @NonNull
    public final RecyclerView.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {
        if (viewType == getScrollingLimitedMessageViewType()) {
            return ScrollingLimitedViewHolder.create(parent);
        }

        return onCreateViewHolderImpl(parent, viewType);
    }

    /** See {@link RangeFilter#indexToPosition}. */
    protected int indexToPosition(int index) {
        return mRangeFilter.indexToPosition(index);
    }

    /** See {@link RangeFilter#positionToIndex}. */
    protected int positionToIndex(int position) {
        return mRangeFilter.positionToIndex(position);
    }

    /**
     * Returns a {@link androidx.recyclerview.widget.RecyclerView.ViewHolder} of type {@code T}.
     *
     * <p>It is delegated to by {@link #onCreateViewHolder(ViewGroup, int)} to handle any
     * {@code viewType}s other than the one corresponding to the "scrolling is limited" message.
     */
    protected abstract T onCreateViewHolderImpl(
            @NonNull ViewGroup parent, int viewType);

    @Override
    @SuppressWarnings("unchecked")
    public final void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ScrollingLimitedViewHolder) {
            ScrollingLimitedViewHolder vh = (ScrollingLimitedViewHolder) holder;
            vh.bind(mScrollingLimitedMessageResId);
        } else {
            int index = mRangeFilter.positionToIndex(position);
            if (index != RangeFilterImpl.INVALID_INDEX) {
                int size = getUnrestrictedItemCount();
                if (0 <= index && index < size) {
                    onBindViewHolderImpl((T) holder, index);
                } else {
                    Log.e(TAG, "onBindViewHolder pos: " + position + " gave index: "
                            + index + " out of bounds size: " + size
                            + " " + mRangeFilter.toString());
                }
            } else {
                Log.e(TAG, "onBindViewHolder invalid position " + position
                        + " " + mRangeFilter.toString());
            }
        }
    }

    /**
     * Binds {@link androidx.recyclerview.widget.RecyclerView.ViewHolder}s of type {@code T}.
     *
     * <p>It is delegated to by {@link #onBindViewHolder(RecyclerView.ViewHolder, int)} to handle
     * holders that are not of type {@link ScrollingLimitedViewHolder}.
     */
    protected abstract void onBindViewHolderImpl(T holder, int position);

    @Override
    public final int getItemViewType(int position) {
        if (mRangeFilter.positionToIndex(position) == RangeFilterImpl.INVALID_INDEX) {
            return getScrollingLimitedMessageViewType();
        } else {
            return getItemViewTypeImpl(mRangeFilter.positionToIndex(position));
        }
    }

    /**
     * Returns the view type of the item at {@code position}.
     *
     * <p>Defaults to the implementation in {@link RecyclerView.Adapter#getItemViewType(int)}.
     *
     * <p>It is delegated to by {@link #getItemViewType(int)} for all positions other than the
     * {@link #getScrollingLimitedMessagePosition()}.
     */
    protected int getItemViewTypeImpl(int position) {
        return super.getItemViewType(position);
    }

    /**
     * Returns the position where the "scrolling is limited" message should be placed.
     *
     * <p>The default implementation is to put this item at the very end of the limited list.
     * Subclasses can override to choose a different position to suit their needs.
     *
     * @deprecated limiting message offset is not supported any more.
     */
    @Deprecated
    protected int getScrollingLimitedMessagePosition() {
        return getItemCount() - 1;
    }

    @Override
    public final int getItemCount() {
        if (mIsLimiting) {
            return mRangeFilter.getFilteredCount();
        } else {
            return getUnrestrictedItemCount();
        }
    }

    /**
     * Returns the number of items in the unrestricted list being displayed via this adapter.
     */
    protected abstract int getUnrestrictedItemCount();

    @Override
    @SuppressWarnings("unchecked")
    public final void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);

        if (!(holder instanceof ScrollingLimitedViewHolder)) {
            onViewRecycledImpl((T) holder);
        }
    }

    /**
     * Recycles {@link androidx.recyclerview.widget.RecyclerView.ViewHolder}s of type {@code T}.
     *
     * <p>It is delegated to by {@link #onViewRecycled(RecyclerView.ViewHolder)} to handle
     * holders that are not of type {@link ScrollingLimitedViewHolder}.
     */
    @SuppressWarnings("unused")
    protected void onViewRecycledImpl(@NonNull T holder) {
    }

    @Override
    @SuppressWarnings("unchecked")
    public final boolean onFailedToRecycleView(@NonNull RecyclerView.ViewHolder holder) {
        if (!(holder instanceof ScrollingLimitedViewHolder)) {
            return onFailedToRecycleViewImpl((T) holder);
        }
        return super.onFailedToRecycleView(holder);
    }

    /**
     * Handles failed recycle attempts for
     * {@link androidx.recyclerview.widget.RecyclerView.ViewHolder}s of type {@code T}.
     *
     * <p>It is delegated to by {@link #onFailedToRecycleView(RecyclerView.ViewHolder)} for holders
     * that are not of type {@link ScrollingLimitedViewHolder}.
     */
    protected boolean onFailedToRecycleViewImpl(@NonNull T holder) {
        return super.onFailedToRecycleView(holder);
    }

    @Override
    @SuppressWarnings("unchecked")
    public final void onViewAttachedToWindow(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        if (!(holder instanceof ScrollingLimitedViewHolder)) {
            onViewAttachedToWindowImpl((T) holder);
        }
    }

    /**
     * Handles attaching {@link androidx.recyclerview.widget.RecyclerView.ViewHolder}s of type
     * {@code T} to the application window.
     *
     * <p>It is delegated to by {@link #onViewAttachedToWindow(RecyclerView.ViewHolder)} for
     * holders that are not of type {@link ScrollingLimitedViewHolder}.
     */
    @SuppressWarnings("unused")
    protected void onViewAttachedToWindowImpl(@NonNull T holder) {
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        mRecyclerView = recyclerView;
    }

    @Override
    @SuppressWarnings("unchecked")
    public final void onViewDetachedFromWindow(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        if (!(holder instanceof ScrollingLimitedViewHolder)) {
            onViewDetachedFromWindowImpl((T) holder);
        }
    }

    /**
     * Handles detaching {@link androidx.recyclerview.widget.RecyclerView.ViewHolder}s of type
     * {@code T} from the application window.
     *
     * <p>It is delegated to by {@link #onViewDetachedFromWindow(RecyclerView.ViewHolder)} for
     * holders that are not of type {@link ScrollingLimitedViewHolder}.
     */
    @SuppressWarnings("unused")
    protected void onViewDetachedFromWindowImpl(@NonNull T holder) {
    }

    @Override
    public void setMaxItems(int maxItems) {
        if (maxItems >= 0) {
            if (mRangeFilter != null && mIsLimiting) {
                Log.w(TAG, "A new filter range received before parked");
                // remove the original filter first.
                mRangeFilter.removeFilter();
            }
            mIsLimiting = true;
            mRangeFilter = new RangeFilterImpl(this, maxItems);
            mRangeFilter.recompute(getUnrestrictedItemCount(), computeAnchorIndexWhenRestricting());
            mRangeFilter.applyFilter();
            autoScrollWhenRestricted();
        } else {
            mRangeFilter.removeFilter();

            mIsLimiting = false;
            mRangeFilter = new PassThroughFilter();
            mRangeFilter.recompute(getUnrestrictedItemCount(), 0);
        }
    }

    /**
     * Returns the position in the truncated list to scroll to when the list is limited.
     *
     * Returns -1 to disable the scrolling.
     */
    protected int getScrollToPositionWhenRestricted() {
        return -1;
    }

    private void autoScrollWhenRestricted() {
        int scrollToPosition = getScrollToPositionWhenRestricted();
        if (scrollToPosition >= 0) {
            RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();
            if (layoutManager != null) {
                mRecyclerView.getLayoutManager().scrollToPosition(scrollToPosition);
            }
        }
    }

    /**
     * Computes the anchor point index in the original list when limiting starts.
     * Returns position 0 by default.
     *
     * Override this function to return a different anchor point to control the position of the
     * limiting window.
     */
    protected int computeAnchorIndexWhenRestricting() {
        return 0;
    }

    /**
     * Updates the changes from underlying data along with a new anchor.
     */
    public void updateUnderlyingDataChanged(int unrestrictedCount, int newAnchorIndex) {
        mRangeFilter.recompute(unrestrictedCount, newAnchorIndex);
    }

    /**
     * Changes the index where the limiting range surrounds. Items that are added and removed will
     * be notified.
     */
    public void notifyLimitingAnchorChanged(int newPivotIndex) {
        mRangeFilter.notifyPivotIndexChanged(newPivotIndex);
    }

    @Override
    public void setScrollingLimitedMessageResId(@StringRes int resId) {
        if (mScrollingLimitedMessageResId == null || mScrollingLimitedMessageResId != resId) {
            mScrollingLimitedMessageResId = resId;
            mRangeFilter.invalidateMessagePositions();
        }
    }
}
