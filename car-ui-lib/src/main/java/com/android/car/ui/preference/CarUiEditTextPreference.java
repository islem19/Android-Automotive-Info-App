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
import android.view.View;

import androidx.annotation.VisibleForTesting;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceViewHolder;

import com.android.car.ui.R;
import com.android.car.ui.utils.CarUiUtils;

/**
 * This class extends the base {@link EditTextPreference} class. Adds the drawable icon to
 * the preference.
 */
public class CarUiEditTextPreference extends EditTextPreference {

    private final Context mContext;
    private boolean mShowChevron = true;

    public CarUiEditTextPreference(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mContext = context;
    }

    public CarUiEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
    }

    public CarUiEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public CarUiEditTextPreference(Context context) {
        super(context);
        mContext = context;
    }

    protected void setTwoActionLayout() {
        setLayoutResource(R.layout.car_ui_two_action_preference);
    }

    /**
     * Returns the widget container if {@link #setTwoActionLayout) was called, otherwise null.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    public View getWidgetActionContainer(PreferenceViewHolder holder) {
        return CarUiUtils.findViewByRefId(holder.itemView, R.id.action_widget_container);
    }

    @Override
    public void onAttached() {
        super.onAttached();

        boolean allowChevron = mContext.getResources().getBoolean(
                R.bool.car_ui_preference_show_chevron);

        if (!allowChevron || !mShowChevron) {
            return;
        }

        setWidgetLayoutResource(R.layout.car_ui_preference_chevron);
    }

    public void setShowChevron(boolean showChevron) {
        mShowChevron = showChevron;
    }
}
