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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class RangeFilterImplTest {
    private static final int EVEN_MAX_ITEMS = 10;
    private static final int ODD_MAX_ITEMS = 9;
    private static final int UNRESTRICTED_COUNT = 80;
    private static final int UNRESTRICTED_SMALL_COUNT = 6;

    @Mock
    RecyclerView.Adapter mMockAdapter;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testRecompute_contentSizeSmallerThanRange_noLimit() {
        RangeFilterImpl rangeFilter = new RangeFilterImpl(mMockAdapter, EVEN_MAX_ITEMS);
        int pivotPosition = 0;
        rangeFilter.recompute(UNRESTRICTED_SMALL_COUNT, pivotPosition);
        verifyNoClamps(rangeFilter, UNRESTRICTED_SMALL_COUNT);
    }

    @Test
    public void testRecompute_pivotPointInTheMiddleWithEvenMaxItems_limitContent() {
        RangeFilterImpl rangeFilter = new RangeFilterImpl(mMockAdapter, EVEN_MAX_ITEMS);
        int pivotPosition = 30;
        rangeFilter.recompute(UNRESTRICTED_COUNT, pivotPosition);
        verifyClampedBothEnds(rangeFilter, pivotPosition, EVEN_MAX_ITEMS);
        rangeFilter.applyFilter();
        verifyFilterBothEnds(mMockAdapter, pivotPosition, EVEN_MAX_ITEMS, UNRESTRICTED_COUNT);
        rangeFilter.removeFilter();
        verifyRemoveFilterBothEnds(mMockAdapter, pivotPosition, EVEN_MAX_ITEMS, UNRESTRICTED_COUNT);
    }

    @Test
    public void testRecompute_pivotPointInTheMiddleWithOddMaxItems_limitContent() {
        RangeFilterImpl rangeFilter = new RangeFilterImpl(mMockAdapter, ODD_MAX_ITEMS);
        int pivotPosition = 30;
        rangeFilter.recompute(UNRESTRICTED_COUNT, pivotPosition);
        verifyClampedBothEnds(rangeFilter, pivotPosition, ODD_MAX_ITEMS);
        rangeFilter.applyFilter();
        verifyFilterBothEnds(mMockAdapter, pivotPosition, ODD_MAX_ITEMS, UNRESTRICTED_COUNT);
        rangeFilter.removeFilter();
        verifyRemoveFilterBothEnds(mMockAdapter, pivotPosition, ODD_MAX_ITEMS, UNRESTRICTED_COUNT);
    }

    private void verifyClampedBothEnds(
            RangeFilterImpl rangeFilter, int pivotPoint, int maxItemCount) {
        RangeFilterImpl.ListRange range = rangeFilter.getRange();
        int firstHalfCount = maxItemCount / 2;
        int secondHalfCount = maxItemCount - firstHalfCount;
        assertThat(range.mClampedHead).isEqualTo(1);
        assertThat(range.mClampedTail).isEqualTo(1);
        assertThat(range.mStartIndex).isEqualTo(pivotPoint - firstHalfCount);
        assertThat(range.mEndIndex).isEqualTo(pivotPoint + secondHalfCount);
        assertThat(range.mEndIndex - range.mStartIndex).isEqualTo(maxItemCount);
        assertThat(range.mLimitedCount).isEqualTo(maxItemCount + 2 /* two messages*/);

        assertThat(rangeFilter.positionToIndex(0))
                .isEqualTo(RangeFilter.INVALID_INDEX);
        assertThat(rangeFilter.positionToIndex(1))
                .isEqualTo(range.mStartIndex);

        assertThat(rangeFilter.positionToIndex(maxItemCount))
                .isEqualTo(range.mEndIndex - 1);
        assertThat(rangeFilter.positionToIndex(maxItemCount + 1))
                .isEqualTo(RangeFilter.INVALID_INDEX);

        assertThat(rangeFilter.indexToPosition(range.mStartIndex))
                .isEqualTo(1);

        assertThat(rangeFilter.indexToPosition(range.mEndIndex - 1))
                .isEqualTo(maxItemCount /* head message takes an additional position */);
    }

    private void verifyNoClamps(RangeFilterImpl rangeFilter, int unrestrictedCount) {
        RangeFilterImpl.ListRange range = rangeFilter.getRange();
        assertThat(range.mClampedHead).isEqualTo(0);
        assertThat(range.mClampedTail).isEqualTo(0);
        assertThat(range.mStartIndex).isEqualTo(0);
        assertThat(range.mEndIndex).isEqualTo(unrestrictedCount);
        assertThat(range.mEndIndex - range.mStartIndex).isEqualTo(unrestrictedCount);
        assertThat(range.mLimitedCount).isEqualTo(unrestrictedCount);
    }

    private void verifyFilterBothEnds(
            Adapter adapter,
            int pivotPosition,
            int maxCount,
            int unrestrictedCount) {
        int firstHalfRemainingItems = maxCount / 2;
        int secondHalfRemainingItems = maxCount - firstHalfRemainingItems;
        int clampedHeadItemCount = pivotPosition - firstHalfRemainingItems;
        int clampedTailItemCount = unrestrictedCount - clampedHeadItemCount - maxCount;

        verify(adapter).notifyItemInserted(unrestrictedCount);
        verify(adapter).notifyItemRangeRemoved(
                pivotPosition + secondHalfRemainingItems, clampedTailItemCount);
        verify(adapter).notifyItemRangeRemoved(0, clampedHeadItemCount);
        verify(adapter).notifyItemInserted(0);
    }

    private void verifyRemoveFilterBothEnds(
            Adapter adapter,
            int pivotPosition,
            int maxCount,
            int unrestrictedCount) {
        int firstHalfRemainingItems = maxCount / 2;
        int secondHalfRemainingItems = maxCount - firstHalfRemainingItems;
        int clampedHeadItemCount = pivotPosition - firstHalfRemainingItems;
        int clampedTailItemCount = unrestrictedCount - clampedHeadItemCount - maxCount;

        verify(adapter).notifyItemRemoved(maxCount + 1);
        verify(adapter).notifyItemRangeInserted(maxCount + 1, clampedTailItemCount);
        verify(adapter).notifyItemRangeInserted(1, clampedHeadItemCount);
        verify(adapter).notifyItemRemoved(0);
    }
}
