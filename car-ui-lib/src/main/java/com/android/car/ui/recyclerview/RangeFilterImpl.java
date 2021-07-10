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

import androidx.annotation.VisibleForTesting;
import androidx.recyclerview.widget.RecyclerView;

/**
 * An implementation of {@link RangeFilter} interface.
 */
public class RangeFilterImpl implements RangeFilter {

    private static final String TAG = "RangeFilterImpl";

    private final RecyclerView.Adapter<?> mAdapter;
    private final int mMaxItems;
    private final int mMaxItemsFirstHalf;
    private final int mMaxItemsSecondHalf;

    private int mUnlimitedCount;
    private int mPivotIndex;
    private final ListRange mRange = new ListRange();

    /**
     * Constructor
     * @param adapter the adapter to notify of changes in {@link #notifyPivotIndexChanged(int)}.
     * @param maxItems the maximum number of items to show.
     */
    public RangeFilterImpl(RecyclerView.Adapter<?> adapter, int maxItems) {
        mAdapter = adapter;
        if (maxItems <= 0) {
            mMaxItemsFirstHalf = 0;
            mMaxItemsSecondHalf = 0;
            mMaxItems = 0;
        } else {
            mMaxItemsFirstHalf = maxItems / 2;
            mMaxItemsSecondHalf = maxItems - mMaxItemsFirstHalf;
            mMaxItems = maxItems;
        }
    }

    @Override
    public String toString() {
        return "RangeFilterImpl{"
                + "mMaxItemsFirstHalf=" + mMaxItemsFirstHalf
                + "mMaxItemsSecondHalf=" + mMaxItemsSecondHalf
                + ", mUnlimitedCount=" + mUnlimitedCount
                + ", mPivotIndex=" + mPivotIndex
                + ", mRange=" + mRange.toString()
                + '}';
    }

    @Override
    public int getFilteredCount() {
        return mRange.mLimitedCount;
    }

    @Override
    public void invalidateMessagePositions() {
        if (mRange.mClampedHead > 0) {
            mAdapter.notifyItemChanged(0);
        }
        if (mRange.mClampedTail > 0) {
            mAdapter.notifyItemChanged(getFilteredCount() - 1);
        }
    }

    @Override
    public void applyFilter() {
        if (mRange.isTailClamped()) {
            mAdapter.notifyItemInserted(mUnlimitedCount);
            mAdapter.notifyItemRangeRemoved(mRange.mEndIndex, mUnlimitedCount - mRange.mEndIndex);
        }
        if (mRange.isHeadClamped()) {
            mAdapter.notifyItemRangeRemoved(0, mRange.mStartIndex);
            mAdapter.notifyItemInserted(0);
        }
    }

    @Override
    public void removeFilter() {
        if (mRange.isTailClamped()) {
            // Remove the message
            mAdapter.notifyItemRemoved(mRange.mLimitedCount - 1);
            // Add the tail items that were dropped
            mAdapter.notifyItemRangeInserted(mRange.mLimitedCount - 1,
                    mUnlimitedCount - mRange.mEndIndex);
        }
        if (mRange.isHeadClamped()) {
            // Add the head items that were dropped
            mAdapter.notifyItemRangeInserted(1, mRange.mStartIndex);
            // Remove the message
            mAdapter.notifyItemRemoved(0);
        }
    }

    @Override
    public void recompute(int newCount, int pivotIndex) {
        if (pivotIndex < 0 || newCount <= pivotIndex) {
            Log.e(TAG, "Invalid pivotIndex: " + pivotIndex + " newCount: " + newCount);
            pivotIndex = 0;
        }
        mUnlimitedCount = newCount;
        mPivotIndex = pivotIndex;

        mRange.mClampedHead = 0;
        mRange.mClampedTail = 0;

        if (mUnlimitedCount <= mMaxItems) {
            // Under the cap case.
            mRange.mStartIndex = 0;
            mRange.mEndIndex = mUnlimitedCount;
            mRange.mLimitedCount = mUnlimitedCount;
        } else if (mMaxItems <= 0) {
            // Zero cap case.
            mRange.mStartIndex = 0;
            mRange.mEndIndex = 0;
            mRange.mLimitedCount = 1; // One limit message
            mRange.mClampedTail = 1;
        } else if (mPivotIndex <= mMaxItemsFirstHalf) {
            // No need to clamp the head case
            // For example: P = 2, M/2 = 2 => exactly two items before the pivot.
            // Tail has to be clamped or we'd be in the "under the cap" case.
            mRange.mStartIndex = 0;
            mRange.mEndIndex = mMaxItems;
            mRange.mLimitedCount = mMaxItems + 1; // One limit message at the end
            mRange.mClampedTail = 1;
        } else if ((mUnlimitedCount - 1 - mPivotIndex) <= mMaxItemsSecondHalf) {
            // No need to clamp the tail case
            // For example: C = 5, P = 2 => exactly 2 items after the pivot (count is exclusive).
            // Head has to be clamped or we'd be in the "under the cap" case.
            mRange.mEndIndex = mUnlimitedCount;
            mRange.mStartIndex = mRange.mEndIndex - mMaxItems;
            mRange.mLimitedCount = mMaxItems + 1; // One limit message at the start
            mRange.mClampedHead = 1;
        } else {
            // Both head and tail need clamping
            mRange.mStartIndex = mPivotIndex - mMaxItemsFirstHalf;
            mRange.mEndIndex = mPivotIndex + mMaxItemsSecondHalf;
            mRange.mLimitedCount = mMaxItems + 2; // One limit message at each end.
            mRange.mClampedHead = 1;
            mRange.mClampedTail = 1;
        }
    }

    @Override
    public void notifyPivotIndexChanged(int pivotIndex) {
        // TODO: Implement this function.
    }

    @Override
    public int indexToPosition(int index) {
        if ((mRange.mStartIndex <= index) && (index < mRange.mEndIndex)) {
            return mRange.indexToPosition(index);
        } else {
            return INVALID_POSITION;
        }
    }

    @Override
    public int positionToIndex(int position) {
        return mRange.positionToIndex(position);
    }

    @VisibleForTesting
    ListRange getRange() {
        return mRange;
    }

    /** Represents a portion of the unfiltered list. */
    static class ListRange {
        public static final int INVALID_INDEX = -1;

        @VisibleForTesting
        /* In original data, inclusive. */
                int mStartIndex;
        @VisibleForTesting
        /* In original data, exclusive. */
                int mEndIndex;

        @VisibleForTesting
        /* 1 when clamped, otherwise 0. */
                int mClampedHead;
        @VisibleForTesting
        /* 1 when clamped, otherwise 0. */
                int mClampedTail;

        @VisibleForTesting
        /* The count of the resulting elements, including the truncation message(s). */
                int mLimitedCount;

        /**
         * Deep copy from a ListRange.
         */
        public void copyFrom(ListRange range) {
            mStartIndex = range.mStartIndex;
            mEndIndex = range.mEndIndex;
            mClampedHead = range.mClampedHead;
            mClampedTail = range.mClampedTail;
            mLimitedCount = range.mLimitedCount;
        }

        @Override
        public String toString() {
            return "ListRange{"
                    + "mStartIndex=" + mStartIndex
                    + ", mEndIndex=" + mEndIndex
                    + ", mClampedHead=" + mClampedHead
                    + ", mClampedTail=" + mClampedTail
                    + ", mLimitedCount=" + mLimitedCount
                    + '}';
        }

        /**
         * Returns true if two ranges intersect.
         */
        public boolean intersects(ListRange range) {
            return ((range.mEndIndex > mStartIndex) && (mEndIndex > range.mStartIndex));
        }

        /**
         * Converts an index in the unrestricted list to the position in the restricted one.
         *
         * Unchecked index needed by {@link #notifyPivotIndexChanged(int)}.
         */
        public int indexToPosition(int index) {
            return index - mStartIndex + mClampedHead;
        }

        /** Converts the position in the restricted list to an index in the unrestricted one.*/
        public int positionToIndex(int position) {
            int index = position - mClampedHead + mStartIndex;
            if ((index < mStartIndex) || (mEndIndex <= index)) {
                return INVALID_INDEX;
            } else {
                return index;
            }
        }

        public boolean isHeadClamped() {
            return mClampedHead == 1;
        }

        public boolean isTailClamped() {
            return mClampedTail == 1;
        }
    }
}
