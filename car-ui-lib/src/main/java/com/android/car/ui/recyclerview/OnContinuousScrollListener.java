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

import android.content.Context;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;

import androidx.annotation.NonNull;

import com.android.car.ui.R;

/**
 * A class, that can be used as a TouchListener on any view (e.g. a Button). It periodically calls
 * the provided clickListener. The first callback is fired after the initial Delay, and subsequent
 * ones after the defined interval.
 */
public class OnContinuousScrollListener implements OnTouchListener {

    private final Handler mHandler = new Handler();
    private final int mInitialDelay;
    private final int mRepeatInterval;
    private final OnClickListener mOnClickListener;
    private View mTouchedView;
    private boolean mIsLongPressed;

    /**
     * Notifies listener and self schedules to be re-run at next callback interval.
     */
    private final Runnable mPeriodicRunnable = new Runnable() {
        @Override
        public void run() {
            if (mTouchedView.isEnabled()) {
                mHandler.postDelayed(this, mRepeatInterval);
                mOnClickListener.onClick(mTouchedView);
                mIsLongPressed = true;
            } else {
                mIsLongPressed = false;
            }
        }
    };

    /**
     * @param clickListener The OnClickListener, that will be called periodically
     */
    public OnContinuousScrollListener(@NonNull Context context,
            @NonNull OnClickListener clickListener) {
        this.mInitialDelay = context.getResources().getInteger(
                R.integer.car_ui_scrollbar_longpress_initial_delay);
        this.mRepeatInterval = context.getResources().getInteger(
                R.integer.car_ui_scrollbar_longpress_repeat_interval);

        if (mInitialDelay < 0 || mRepeatInterval < 0) {
            throw new IllegalArgumentException("negative intervals are not allowed");
        }
        this.mOnClickListener = clickListener;
    }

    /**
     * Cancel pending scroll operations. Any scroll operations that were scheduled to possibly be
     * performed, as part of a continuous scroll, will be cancelled.
     */
    public void cancelPendingScroll() {
        mHandler.removeCallbacks(mPeriodicRunnable);
        mIsLongPressed = false;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        mTouchedView = view;
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mHandler.removeCallbacks(mPeriodicRunnable);
                mHandler.postDelayed(mPeriodicRunnable, mInitialDelay);
                mTouchedView.setPressed(true);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (!mIsLongPressed) {
                    mOnClickListener.onClick(view);
                }
                mHandler.removeCallbacks(mPeriodicRunnable);
                mTouchedView.setPressed(false);
                mIsLongPressed = false;
                return true;
        }
        return false;
    }
}
