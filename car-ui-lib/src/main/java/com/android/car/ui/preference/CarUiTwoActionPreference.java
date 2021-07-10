/*
 * Copyright (C) 2018 The Android Open Source Project
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
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceViewHolder;

import com.android.car.ui.R;
import com.android.car.ui.utils.CarUiUtils;

/**
 * A preference which can perform two actions. The secondary action is shown by default.
 * {@link #showAction(boolean)} may be used to manually set the visibility of the action.
 */
public class CarUiTwoActionPreference extends CarUiPreference {

    private boolean mIsActionShown;

    public CarUiTwoActionPreference(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    public CarUiTwoActionPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    public CarUiTwoActionPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public CarUiTwoActionPreference(Context context) {
        super(context);
        init(/* attrs= */ null);
    }

    /**
     * Sets the custom two action preference layout and attributes.
     * Check {@link #setLayoutResource} for layout requirements.
     */
    private void init(AttributeSet attrs) {
        setLayoutResource(R.layout.car_ui_two_action_preference);
        TypedArray preferenceAttributes = getContext().obtainStyledAttributes(attrs,
                R.styleable.CarUiTwoActionPreference);
        mIsActionShown = preferenceAttributes.getBoolean(
                R.styleable.CarUiTwoActionPreference_actionShown, true);
        setShowChevron(false);
        preferenceAttributes.recycle();
    }

    /**
     * Sets whether the secondary action is visible in the preference.
     *
     * @param isShown {@code true} if the secondary action should be shown.
     */
    public void showAction(boolean isShown) {
        mIsActionShown = isShown;
        notifyChanged();
    }

    /** Returns {@code true} if action is shown. */
    public boolean isActionShown() {
        return mIsActionShown;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        View containerWithoutWidget = CarUiUtils.findViewByRefId(holder.itemView,
                R.id.car_ui_preference_container_without_widget);
        View actionContainer = CarUiUtils.findViewByRefId(holder.itemView,
                R.id.action_widget_container);
        View widgetFrame = CarUiUtils.findViewByRefId(holder.itemView, android.R.id.widget_frame);
        holder.itemView.setFocusable(!mIsActionShown);
        containerWithoutWidget.setOnClickListener(
                mIsActionShown ? this::performClickUnrestricted : null);
        containerWithoutWidget.setClickable(mIsActionShown);
        containerWithoutWidget.setFocusable(mIsActionShown);
        actionContainer.setVisibility(mIsActionShown ? View.VISIBLE : View.GONE);
        widgetFrame.setFocusable(mIsActionShown);
        if (mIsActionShown) {
            onBindWidgetFrame(widgetFrame);
        }
    }

    /**
     * Binds the created View for the second action.
     *
     * <p>This is a good place to set properties on any custom view.
     *
     * @param widgetFrame The widget frame which controls the 2nd action.
     */
    protected void onBindWidgetFrame(@NonNull View widgetFrame) {
    }
}
