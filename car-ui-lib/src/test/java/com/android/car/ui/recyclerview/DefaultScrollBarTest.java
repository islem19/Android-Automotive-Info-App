/*
 * Copyright 2019 The Android Open Source Project
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertThrows;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import androidx.recyclerview.widget.RecyclerView;

import com.android.car.ui.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class DefaultScrollBarTest {

    private Context mContext;
    private ScrollBar mScrollBar;

    @Mock
    private RecyclerView mRecyclerView;
    @Mock
    private FrameLayout mParent;
    @Mock
    private FrameLayout.LayoutParams mLayoutParams;
    @Mock
    private RecyclerView.RecycledViewPool mRecycledViewPool;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;

        mScrollBar = new DefaultScrollBar();
    }

    @Test
    public void initialize_shouldInitializeScrollListener() {
        when(mRecyclerView.getContext()).thenReturn(mContext);
        when(mRecyclerView.getParent()).thenReturn(mParent);
        when(mRecyclerView.getRecycledViewPool()).thenReturn(mRecycledViewPool);
        when(mParent.generateLayoutParams(any())).thenReturn(mLayoutParams);

        View scrollView = LayoutInflater.from(mContext).inflate(
                R.layout.car_ui_recyclerview_scrollbar, null);
        mScrollBar.initialize(mRecyclerView, scrollView);

        // called once in DefaultScrollBar and once in SnapHelper while setting up the call backs
        // when we use attachToRecyclerView(recyclerview)
        verify(mRecyclerView, times(2)).addOnScrollListener(
                any(RecyclerView.OnScrollListener.class));
    }

    @Test
    public void initialize_shouldSetMaxRecyclerViews() {
        when(mRecyclerView.getContext()).thenReturn(mContext);
        when(mRecyclerView.getParent()).thenReturn(mParent);
        when(mRecyclerView.getRecycledViewPool()).thenReturn(mRecycledViewPool);
        when(mParent.generateLayoutParams(any())).thenReturn(mLayoutParams);

        View scrollView = LayoutInflater.from(mContext).inflate(
                R.layout.car_ui_recyclerview_scrollbar, null);
        mScrollBar.initialize(mRecyclerView, scrollView);

        verify(mRecycledViewPool).setMaxRecycledViews(0, 12);
    }

    @Test
    public void initialize_shouldNotHaveFlingListener() {
        when(mRecyclerView.getContext()).thenReturn(mContext);
        when(mRecyclerView.getParent()).thenReturn(mParent);
        when(mRecyclerView.getRecycledViewPool()).thenReturn(mRecycledViewPool);
        when(mParent.generateLayoutParams(any())).thenReturn(mLayoutParams);

        View scrollView = LayoutInflater.from(mContext).inflate(
                R.layout.car_ui_recyclerview_scrollbar, null);
        mScrollBar.initialize(mRecyclerView, scrollView);

        verify(mRecyclerView).setOnFlingListener(null);
    }

    @Test
    public void setPadding_shouldSetStartAndEndPadding() {
        when(mRecyclerView.getContext()).thenReturn(mContext);
        when(mRecyclerView.getParent()).thenReturn(mParent);
        when(mRecyclerView.getRecycledViewPool()).thenReturn(mRecycledViewPool);
        when(mParent.generateLayoutParams(any())).thenReturn(mLayoutParams);

        View scrollView = LayoutInflater.from(mContext).inflate(
                R.layout.car_ui_recyclerview_scrollbar, null);
        mScrollBar.initialize(mRecyclerView, scrollView);
        mScrollBar.setPadding(10, 20);

        assertThat(scrollView.getPaddingTop()).isEqualTo(10);
        assertThat(scrollView.getPaddingBottom()).isEqualTo(20);
    }

    @Test
    public void setPadding_shouldThrowErrorWithoutInitialization() {
        assertThrows(NullPointerException.class, () -> mScrollBar.setPadding(10, 20));
    }

    @Test
    public void requestLayout_shouldThrowErrorWithoutInitialization() {
        assertThrows(NullPointerException.class, () -> mScrollBar.requestLayout());
    }
}
