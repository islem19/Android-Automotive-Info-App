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
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.car.ui.R;
import com.android.car.ui.utils.CarUiUtils;

/**
 * This class extends the base {@link Preference} class. Adds the support to add a drawable icon to
 * the preference if there is one of fragment, intent or onPreferenceClickListener set.
 */
public class CarUiPreference extends Preference implements DisabledPreferenceCallback {

    private Context mContext;
    private boolean mShowChevron;
    private String mMessageToShowWhenDisabledPreferenceClicked;

    private boolean mShouldShowRippleOnDisabledPreference;
    private Drawable mBackground;
    private View mPreference;

    public CarUiPreference(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    public CarUiPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, R.style.Preference_CarUi_Preference);
    }

    public CarUiPreference(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.carUiPreferenceStyle);
    }

    public CarUiPreference(Context context) {
        this(context, null);
    }

    public void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        mContext = context;

        TypedArray a = getContext().obtainStyledAttributes(
                attrs,
                R.styleable.CarUiPreference,
                defStyleAttr,
                defStyleRes);

        mShowChevron = a.getBoolean(R.styleable.CarUiPreference_showChevron, true);
        mShouldShowRippleOnDisabledPreference = a.getBoolean(
                R.styleable.CarUiPreference_showRippleOnDisabledPreference, false);

        a.recycle();
    }


    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        boolean viewEnabled = isEnabled();
        mPreference = holder.itemView;
        mBackground = CarUiUtils.setPreferenceViewEnabled(viewEnabled, holder.itemView, mBackground,
                mShouldShowRippleOnDisabledPreference);
    }

    @Override
    public void onAttached() {
        super.onAttached();

        boolean allowChevron = mContext.getResources().getBoolean(
                R.bool.car_ui_preference_show_chevron);

        if (!allowChevron || !mShowChevron) {
            return;
        }

        if (getOnPreferenceClickListener() != null || getIntent() != null
                || getFragment() != null) {
            setWidgetLayoutResource(R.layout.car_ui_preference_chevron);
        }
    }

    /**
     * An exact copy of {@link androidx.preference.Preference#performClick(View)}
     * This method was added here because super.performClick(View) is not open
     * for app usage.
     */
    @SuppressWarnings("RestrictTo")
    void performClickUnrestricted(View v) {
        performClick();
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

    public void setShowChevron(boolean showChevron) {
        mShowChevron = showChevron;
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
