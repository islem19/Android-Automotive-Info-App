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

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.android.car.ui.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class CarUiRecyclerViewTest {

    private Context mContext;
    private View mView;
    private CarUiRecyclerView mCarUiRecyclerView;
    private Resources mResources;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mResources = mContext.getResources();
    }

    @Test
    public void setAdapter_shouldInitializeLinearLayoutManager() {
        mView = LayoutInflater.from(mContext)
                .inflate(mResources.getIdentifier("test_linear_car_ui_recycler_view", "layout",
                        mContext.getPackageName()), null);

        mCarUiRecyclerView = mView.findViewById(
                mResources.getIdentifier("test_prv", "id", mContext.getPackageName()));

        assertThat(mCarUiRecyclerView.getLayoutManager()).isInstanceOf(
                LinearLayoutManager.class);
    }

    @Test
    public void setAdapter_shouldInitializeGridLayoutManager() {
        mView = LayoutInflater.from(mContext)
                .inflate(mResources.getIdentifier("test_grid_car_ui_recycler_view", "layout",
                        mContext.getPackageName()), null);

        mCarUiRecyclerView = mView.findViewById(
                mResources.getIdentifier("test_prv", "id", mContext.getPackageName()));

        assertThat(mCarUiRecyclerView.getLayoutManager()).isInstanceOf(
                GridLayoutManager.class);
    }

    @Test
    public void init_shouldContainRecyclerView() {
        mView = LayoutInflater.from(mContext)
                .inflate(mResources.getIdentifier("test_linear_car_ui_recycler_view", "layout",
                        mContext.getPackageName()), null);

        mCarUiRecyclerView = mView.findViewById(
                mResources.getIdentifier("test_prv", "id", mContext.getPackageName()));

        assertThat(mCarUiRecyclerView).isNotNull();
    }

    @Test
    public void init_shouldHaveGridLayout() {
        mCarUiRecyclerView = new CarUiRecyclerView(mContext,
                Robolectric.buildAttributeSet().addAttribute(R.attr.layoutStyle, "grid").build());
        assertThat(mCarUiRecyclerView.getLayoutManager()).isInstanceOf(
                GridLayoutManager.class);
    }
}
