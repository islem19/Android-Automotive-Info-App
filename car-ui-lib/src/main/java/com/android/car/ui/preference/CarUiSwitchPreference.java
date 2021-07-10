/*
 * Copyright 2020 The Android Open Source Project
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
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.SwitchPreference;

import com.android.car.ui.R;
import com.android.car.ui.utils.CarUiUtils;

/**
 * This class extends the base {@link SwitchPreference} class. Adds the functionality to show
 * message when preference is disabled.
 */
public class CarUiSwitchPreference extends SwitchPreference implements DisabledPreferenceCallback {

    private String mMessageToShowWhenDisabledPreferenceClicked;

    private boolean mShouldShowRippleOnDisabledPreference;
    private Drawable mBackground;
    private View mPreference;
    private Context mContext;

    public CarUiSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    public CarUiSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public CarUiSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CarUiSwitchPreference(Context context) {
        super(context);
        init(context, null);
    }

    private void init(Context context, AttributeSet attrs) {
        mContext = context;
        TypedArray preferenceAttributes = getContext().obtainStyledAttributes(attrs,
                R.styleable.CarUiPreference);
        mShouldShowRippleOnDisabledPreference = preferenceAttributes.getBoolean(
                R.styleable.CarUiPreference_showRippleOnDisabledPreference, false);
        preferenceAttributes.recycle();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        mPreference = holder.itemView;
        mBackground = CarUiUtils.setPreferenceViewEnabled(isEnabled(), holder.itemView, mBackground,
                mShouldShowRippleOnDisabledPreference);
    }

    /**
     * This is similar to {@link Preference#performClick()} with the only difference that we do not
     * return when view is not enabled.
     */
    @Override
    @SuppressWarnings("RestrictTo")
    public void performClick() {
        if (isEnabled()) {
            super.performClick();
        } else if (mMessageToShowWhenDisabledPreferenceClicked != null
                && !mMessageToShowWhenDisabledPreferenceClicked.isEmpty()) {
            Toast.makeText(mContext, mMessageToShowWhenDisabledPreferenceClicked,
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Sets the ripple on the disabled preference.
     */
    @Override
    public void setShouldShowRippleOnDisabledPreference(boolean showRipple) {
        mShouldShowRippleOnDisabledPreference = showRipple;
        CarUiUtils.updateRippleStateOnDisabledPreference(isEnabled(),
                mShouldShowRippleOnDisabledPreference, mBackground, mPreference);
    }

    @Override
    public void setMessageToShowWhenDisabledPreferenceClicked(@NonNull String message) {
        mMessageToShowWhenDisabledPreferenceClicked = message;
    }
}
