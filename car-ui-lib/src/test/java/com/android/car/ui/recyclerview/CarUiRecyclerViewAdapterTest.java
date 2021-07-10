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
import static org.mockito.Mockito.when;

import android.content.Context;
import android.view.ViewGroup;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class CarUiRecyclerViewAdapterTest {

    private Context mContext;
    private CarUiRecyclerViewAdapter mCarUiRecyclerViewAdapter;

    @Mock
    private ViewGroup mParent;
    @Mock
    private ViewGroup.LayoutParams mLayoutParams;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mCarUiRecyclerViewAdapter = new CarUiRecyclerViewAdapter();
    }

    @Test
    public void getItemCount_shouldAlwaysBeOne() {
        assertThat(mCarUiRecyclerViewAdapter.getItemCount()).isEqualTo(1);
    }

    @Test
    public void onCreateViewHolder_frameLayoutNotNull() {

        when(mParent.getContext()).thenReturn(mContext);
        when(mParent.generateLayoutParams(any())).thenReturn(mLayoutParams);

        CarUiRecyclerViewAdapter.NestedRowViewHolder nestedRowViewHolder =
                mCarUiRecyclerViewAdapter.onCreateViewHolder(mParent, 0);

        assertThat(nestedRowViewHolder.frameLayout).isNotNull();
    }
}
