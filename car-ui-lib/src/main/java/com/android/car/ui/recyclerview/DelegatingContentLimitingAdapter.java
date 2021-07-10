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

import android.view.ViewGroup;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

/**
 * A delegating implementation of {@link ContentLimiting} interface.
 *
 * <p>This class will provide content limiting capability to any {@link RecyclerView.Adapter} that
 * is wrapped in it.
 *
 * @param <T> type of the {@link RecyclerView.ViewHolder} objects used by the delegate.
 */
public class DelegatingContentLimitingAdapter<T extends RecyclerView.ViewHolder>
        extends ContentLimitingAdapter<T> {
    private static final int SCROLLING_LIMITED_MESSAGE_VIEW_TYPE = Integer.MAX_VALUE;
    private static final int SCROLLING_LIMITED_MESSAGE_DEFAULT_POSITION_OFFSET = -1;

    private final RecyclerView.Adapter<T> mDelegate;
    private final int mScrollingLimitedMessageViewType;
    private final int mScrollingLimitedMessagePositionOffset;
    @IdRes
    private final int mConfigId;

    /**
     * Provides the abilities to delegate {@link ContentLimitingAdapter} callback functions.
     */
    public interface ContentLimiting {
        /**
         * @see ContentLimitingAdapter#getScrollToPositionWhenRestricted()
         */
        int getScrollToPositionWhenRestricted();

        /**
         * @see ContentLimitingAdapter#computeAnchorIndexWhenRestricting()
         */
        int computeAnchorIndexWhenRestricting();
    }

    /**
     * Constructs a {@link DelegatingContentLimitingAdapter} that uses {@link Integer#MAX_VALUE}
     * for the scrolling limited message viewType and positions it at the very bottom of the list
     * being content limited.
     *
     * <p>Use {@link #DelegatingContentLimitingAdapter(RecyclerView.Adapter, int, int, int)} if you
     * need to customize any of the two default values above.
     *
     * @param delegate - the {@link RecyclerView.Adapter} whose content needs to be limited.
     * @param configId - an Id Resource that can be used to identify said adapter.
     */
    public DelegatingContentLimitingAdapter(
            RecyclerView.Adapter<T> delegate,
            @IdRes int configId) {
        this(delegate,
                configId,
                SCROLLING_LIMITED_MESSAGE_VIEW_TYPE,
                SCROLLING_LIMITED_MESSAGE_DEFAULT_POSITION_OFFSET);
    }

    /**
     * Constructs a {@link DelegatingContentLimitingAdapter}.
     *
     * @param delegate - the {@link RecyclerView.Adapter} whose content needs to be limited.
     * @param configId - an Id Resource that can be used to identify said adapter.
     * @param viewType - viewType value for the scrolling limited message
     * @param offset   - offset of the position of the scrolling limited message. Negative values
     *                 will be treated as a "bottom offset", i.e. they represent the value to
     *                 subtract from {@link #getItemCount()} to get to the actual position of the
     *                 message. For example, by default the offset is -1, meaning the position of
     *                 the scrolling limited message will be getItemCount() - 1, which in a list
     *                 indexed at 0 means the very last item. Positive values will be treated as
     *                 "top offset", so an offset of 0 will put the scrolling limited message at the
     *                 very top of the list.
     * @deprecated offset is not supported in the {@link ContentLimitingAdapter} any more.
     */
    @Deprecated
    public DelegatingContentLimitingAdapter(RecyclerView.Adapter<T> delegate,
            @IdRes int configId,
            int viewType,
            int offset) {
        mDelegate = delegate;
        mConfigId = configId;
        mScrollingLimitedMessageViewType = viewType;
        mScrollingLimitedMessagePositionOffset = offset;
        mDelegate.registerAdapterDataObserver(new Observer());
    }

    private class Observer extends RecyclerView.AdapterDataObserver {

        @Override
        public void onChanged() {
            DelegatingContentLimitingAdapter.this.notifyDataSetChanged();
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            DelegatingContentLimitingAdapter.this
                    .notifyItemRangeChanged(positionStart, itemCount);
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount, @Nullable Object payload) {
            DelegatingContentLimitingAdapter.this
                    .notifyItemRangeChanged(positionStart, itemCount, payload);
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            DelegatingContentLimitingAdapter.this
                    .notifyItemRangeInserted(positionStart, itemCount);
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            DelegatingContentLimitingAdapter.this
                    .notifyItemRangeRemoved(positionStart, itemCount);
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            DelegatingContentLimitingAdapter.this.notifyDataSetChanged();
        }
    }

    @Override
    @NonNull
    public T onCreateViewHolderImpl(@NonNull ViewGroup parent, int viewType) {
        return mDelegate.onCreateViewHolder(parent, viewType);
    }

    @Override
    public void onBindViewHolderImpl(T holder, int position) {
        mDelegate.onBindViewHolder(holder, position);
    }

    @Override
    public int getItemViewTypeImpl(int position) {
        return mDelegate.getItemViewType(position);
    }

    @Override
    protected void onViewRecycledImpl(@NonNull T holder) {
        mDelegate.onViewRecycled(holder);
    }

    @Override
    protected boolean onFailedToRecycleViewImpl(@NonNull T holder) {
        return mDelegate.onFailedToRecycleView(holder);
    }

    @Override
    protected void onViewAttachedToWindowImpl(@NonNull T holder) {
        mDelegate.onViewAttachedToWindow(holder);
    }

    @Override
    protected void onViewDetachedFromWindowImpl(@NonNull T holder) {
        mDelegate.onViewDetachedFromWindow(holder);
    }

    @Override
    public void setHasStableIds(boolean hasStableIds) {
        mDelegate.setHasStableIds(hasStableIds);
    }

    @Override
    public long getItemId(int position) {
        return mDelegate.getItemId(position);
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mDelegate.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        mDelegate.onDetachedFromRecyclerView(recyclerView);
    }

    @Override
    protected int computeAnchorIndexWhenRestricting() {
        if (mDelegate instanceof DelegatingContentLimitingAdapter.ContentLimiting) {
            return ((DelegatingContentLimitingAdapter.ContentLimiting) mDelegate)
                    .computeAnchorIndexWhenRestricting();
        } else {
            return 0;
        }
    }

    @Override
    protected int getScrollToPositionWhenRestricted() {
        if (mDelegate instanceof DelegatingContentLimitingAdapter.ContentLimiting) {
            return ((DelegatingContentLimitingAdapter.ContentLimiting) mDelegate)
                    .getScrollToPositionWhenRestricted();
        } else {
            return -1;
        }
    }

    @Override
    public int getUnrestrictedItemCount() {
        return mDelegate.getItemCount();
    }

    @Override
    @IdRes
    public int getConfigurationId() {
        return mConfigId;
    }

    @Override
    public int getScrollingLimitedMessageViewType() {
        return mScrollingLimitedMessageViewType;
    }

    @Override
    protected int getScrollingLimitedMessagePosition() {
        if (mScrollingLimitedMessagePositionOffset < 0) {
            // For negative values, treat them as a bottom offset.
            return getItemCount() + mScrollingLimitedMessagePositionOffset;
        } else {
            // For positive values, treat them like a top offset.
            return mScrollingLimitedMessagePositionOffset;
        }
    }
}
