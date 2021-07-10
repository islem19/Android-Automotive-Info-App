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

import androidx.annotation.IdRes;
import androidx.annotation.StringRes;

/**
 * An interface for {@link androidx.recyclerview.widget.RecyclerView.Adapter} objects whose
 * content can be limited to a provided maximum number of items.
 */
public interface ContentLimiting {

    /**
     * A value that indicates there should be no limit.
     */
    int UNLIMITED = -1;

    /**
     * Sets the maximum number of items available in the adapter. Use {@link #UNLIMITED} if
     * the list should not be capped.
     */
    void setMaxItems(int maxItems);

    /**
     * Sets the message to show in the UI when the list content length is capped.
     */
    void setScrollingLimitedMessageResId(@StringRes int resId);

    /**
     * Returns the resource ID of a string resource that can uniquely identify the list
     * displayed via this adapter in the UI for the purposes of mapping UXR restriction
     * customizations to it.
     */
    @IdRes
    int getConfigurationId();
}
