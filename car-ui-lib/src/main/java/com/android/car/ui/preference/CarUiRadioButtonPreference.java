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

package com.android.car.ui.preference;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RadioButton;

import androidx.preference.PreferenceViewHolder;
import androidx.preference.TwoStatePreference;

import com.android.car.ui.R;
import com.android.car.ui.utils.CarUiUtils;

/** A preference which shows a radio button at the start of the preference. */
public class CarUiRadioButtonPreference extends TwoStatePreference {

    public CarUiRadioButtonPreference(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public CarUiRadioButtonPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public CarUiRadioButtonPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CarUiRadioButtonPreference(Context context) {
        super(context);
        init();
    }

    private void init() {
        setLayoutResource(R.layout.car_ui_preference);
        setWidgetLayoutResource(R.layout.car_ui_radio_button_preference_widget);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        RadioButton radioButton = (RadioButton) CarUiUtils.findViewByRefId(holder.itemView,
                R.id.radio_button);
        radioButton.setChecked(isChecked());
    }
}
