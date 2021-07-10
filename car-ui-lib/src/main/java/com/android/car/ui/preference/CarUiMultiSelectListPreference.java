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

package com.android.car.ui.preference;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.MultiSelectListPreference;

import com.android.car.ui.R;

/**
 * This class extends the base {@link CarUiMultiSelectListPreference} class. Adds the drawable icon
 * to the preference.
 */
public class CarUiMultiSelectListPreference extends MultiSelectListPreference {

    private final Context mContext;

    public CarUiMultiSelectListPreference(Context context) {
        super(context);
        mContext = context;
    }

    public CarUiMultiSelectListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public CarUiMultiSelectListPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
    }

    public CarUiMultiSelectListPreference(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mContext = context;
    }

    /**
     * This is to make getSelectedItems() visible from other classes in
     * com.android.car.ui.preference.
     */
    @Override
    protected boolean[] getSelectedItems() {
        return super.getSelectedItems();
    }

    @Override
    public void onAttached() {
        super.onAttached();

        boolean showChevron = mContext.getResources().getBoolean(
                R.bool.car_ui_preference_show_chevron);

        if (!showChevron) {
            return;
        }

        setWidgetLayoutResource(R.layout.car_ui_preference_chevron);
    }
}
