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
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.DialogPreference;

import com.android.car.ui.R;
import com.android.car.ui.utils.CarUiUtils;

/** A class implements some basic methods of a seekbar dialog preference. */
public class CarUiSeekBarDialogPreference extends DialogPreference
        implements DialogFragmentCallbacks {

    private int mSeekBarProgress;
    private SeekBar mSeekBar;

    private int mSeekBarTopTextViewVisibility;
    private TextView mSeekBarTopTextView;
    private String mSeekBarTopText;

    private int mSeekBarLeftTextViewVisibility;
    private TextView mSeekBarLeftTextView;
    private String mSeekBarLeftText;

    private int mSeekBarRightTextViewVisibility;
    private TextView mSeekBarRightTextView;
    private String mSeekBarRightText;

    private SeekBar.OnSeekBarChangeListener mOnSeekBarChangeListener;
    private int mMaxProgress = 100;

    public CarUiSeekBarDialogPreference(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public CarUiSeekBarDialogPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public CarUiSeekBarDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CarUiSeekBarDialogPreference(Context context) {
        super(context);
        init();
    }

    private void init() {
        setDialogLayoutResource(R.layout.car_ui_seekbar_dialog);
        setPositiveButtonText(R.string.car_ui_dialog_preference_positive);
        setNegativeButtonText(R.string.car_ui_dialog_preference_negative);
    }

    @Override
    public void onAttached() {
        super.onAttached();
        mSeekBarProgress = getPersistedInt(0);
    }

    @Override
    public void onBindDialogView(@NonNull View view) {
        mSeekBar = CarUiUtils.findViewByRefId(view, R.id.seek_bar);
        mSeekBarTopTextView = CarUiUtils.findViewByRefId(view, R.id.seek_bar_text_top);
        mSeekBarLeftTextView = CarUiUtils.findViewByRefId(view, R.id.seek_bar_text_left);
        mSeekBarRightTextView = CarUiUtils.findViewByRefId(view, R.id.seek_bar_text_right);

        setProgress(mSeekBarProgress);

        setSeekBarTopTextViewVisibility(mSeekBarTopTextViewVisibility);
        setSeekBarTopTextViewText(mSeekBarTopText);

        setSeekBarLeftTextViewVisibility(mSeekBarLeftTextViewVisibility);
        setSeekBarLeftTextViewText(mSeekBarLeftText);

        setSeekBarRightTextViewVisibility(mSeekBarRightTextViewVisibility);
        setSeekBarRightTextViewText(mSeekBarRightText);

        setMaxProgress(mMaxProgress);
        setOnSeekBarChangeListener(mOnSeekBarChangeListener);
    }

    /**
     * Get the progress bar's current level of progress. Return 0 when the progress bar is in
     * indeterminate mode.
     */
    public int getProgress() {
        if (mSeekBar != null) {
            return mSeekBar.getProgress();
        }
        return mSeekBarProgress;
    }

    /**
     * Sets the current progress to the specified value.
     */
    public void setProgress(int progress) {
        if (mSeekBar != null) {
            mSeekBar.setProgress(progress);
        }
        mSeekBarProgress = progress;
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            mSeekBarProgress = mSeekBar.getProgress();
            persistInt(mSeekBarProgress);
            notifyChanged();
        }

        mSeekBarTopTextView = null;
        mSeekBarRightTextView = null;
        mSeekBarLeftTextView = null;
        mSeekBar = null;
    }

    /**
     * Sets the text view visibility on top of the seekbar.
     */
    public void setSeekBarTopTextViewVisibility(int visibility) {
        if (mSeekBarTopTextView != null) {
            mSeekBarTopTextView.setVisibility(visibility);
        }
        mSeekBarTopTextViewVisibility = visibility;
    }

    /**
     * Gets the text on top of the seekbar.
     */
    @Nullable
    public String getSeekBarTopTextViewText() {
        if (mSeekBarTopTextView != null) {
            return mSeekBarTopTextView.getText().toString();
        }
        return mSeekBarTopText;
    }

    /**
     * Sets the text on top of the seekbar.
     */
    public void setSeekBarTopTextViewText(String text) {
        if (mSeekBarTopTextView != null) {
            mSeekBarTopTextView.setText(text);
        }
        mSeekBarTopText = text;
    }

    /**
     * Sets the text view visibility on left of the seekbar.
     */
    public void setSeekBarLeftTextViewVisibility(int visibility) {
        if (mSeekBarLeftTextView != null) {
            mSeekBarLeftTextView.setVisibility(visibility);
        }
        mSeekBarLeftTextViewVisibility = visibility;
    }

    /**
     * Gets the text on left of the seekbar.
     */
    @Nullable
    public String getSeekBarLeftTextViewText() {
        if (mSeekBarLeftTextView != null) {
            return mSeekBarLeftTextView.getText().toString();
        }
        return mSeekBarLeftText;
    }

    /**
     * Sets the text on Left of the seekbar.
     */
    public void setSeekBarLeftTextViewText(@Nullable String text) {
        if (mSeekBarLeftTextView != null) {
            mSeekBarLeftTextView.setText(text);
        }
        mSeekBarLeftText = text;
    }


    /**
     * Sets the text view visibility on right of the seekbar.
     */
    public void setSeekBarRightTextViewVisibility(int visibility) {
        if (mSeekBarRightTextView != null) {
            mSeekBarRightTextView.setVisibility(visibility);
        }
        mSeekBarRightTextViewVisibility = visibility;
    }

    /**
     * Gets the text on right of the seekbar.
     */
    @Nullable
    public String getSeekBarRightTextViewText() {
        if (mSeekBarRightTextView != null) {
            return mSeekBarRightTextView.getText().toString();
        }
        return mSeekBarRightText;
    }


    /**
     * Sets the text on right of the seekbar.
     */
    public void setSeekBarRightTextViewText(@Nullable String text) {
        if (mSeekBarRightTextView != null) {
            mSeekBarRightTextView.setText(text);
        }
        mSeekBarRightText = text;
    }

    /**
     * Sets a listener to receive notifications of changes to the SeekBar's progress level. Also
     * provides notifications of when the user starts and stops a touch gesture within the SeekBar.
     *
     * @param listener The seek bar notification listener
     * @see SeekBar.OnSeekBarChangeListener
     */
    public void setOnSeekBarChangeListener(SeekBar.OnSeekBarChangeListener listener) {
        if (mSeekBar != null) {
            mSeekBar.setOnSeekBarChangeListener(listener);
        }
        mOnSeekBarChangeListener = listener;
    }

    /** Get the upper range of the progress bar */
    public int getMaxProgress() {
        if (mSeekBar != null) {
            return mSeekBar.getMax();
        }
        return mMaxProgress;
    }

    /** Set the upper range of the progress bar */
    public void setMaxProgress(int maxProgress) {
        if (mSeekBar != null) {
            mSeekBar.setMax(maxProgress);
        }
        mMaxProgress = maxProgress;
    }
}
