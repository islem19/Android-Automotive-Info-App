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

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

/**
 * An abstract class that defines required contract for a custom scroll bar for the {@link
 * CarUiRecyclerView}. All custom scroll bar must inherit from this class.
 */
public interface ScrollBar {
    /**
     * The concrete class should implement this method to initialize configuration of a scrollbar
     * view.
     */
    void initialize(RecyclerView recyclerView, View scrollView);

    /**
     * Requests layout of the scrollbar. Should be called when there's been a change that will
     * affect
     * the size of the scrollbar view.
     */
    void requestLayout();

    /** Sets the padding of the scrollbar, relative to the padding of the RecyclerView. */
    void setPadding(int paddingStart, int paddingEnd);

    /**
     * Called when recyclerview's setAdapter is called.
     */
    void adapterChanged(RecyclerView.Adapter adapter);
}
