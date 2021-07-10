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

import androidx.annotation.NonNull;

/**
 * Definition of list item header that can be inserted into {@link CarUiListItemAdapter}.
 */
public class CarUiHeaderListItem extends CarUiListItem {

    private final CharSequence mTitle;
    private final CharSequence mBody;

    public CarUiHeaderListItem(@NonNull CharSequence title) {
        this(title, "");
    }

    public CarUiHeaderListItem(@NonNull CharSequence title, @NonNull CharSequence body) {
        mTitle = title;
        mBody = body;
    }

    /**
     * Returns the title text for the header.
     */
    public CharSequence getTitle() {
        return mTitle;
    }

    /**
     * Returns the body text for the header.
     */
    public CharSequence getBody() {
        return mBody;
    }
}
