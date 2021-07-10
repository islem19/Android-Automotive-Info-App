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
package com.android.car.ui.recyclerview;

import android.content.Context;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

import androidx.annotation.VisibleForTesting;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import com.android.car.ui.R;
import com.android.car.ui.utils.CarUiUtils;

/**
 * Code drop from {androidx.car.widget.CarUiSmoothScroller}
 *
 * <p>Custom {@link LinearSmoothScroller} that has:
 *
 * <ul>
 * <li>Custom control over the speed of scrolls.
 * <li>Scrolling that snaps to start of a child view.
 * </ul>
 */
public class CarUiSmoothScroller extends LinearSmoothScroller {
    @VisibleForTesting
    float mMillisecondsPerInch;
    @VisibleForTesting
    float mDecelerationTimeDivisor;
    @VisibleForTesting
    float mMillisecondsPerPixel;
    @VisibleForTesting
    Interpolator mInterpolator;
    @VisibleForTesting
    int mDensityDpi;

    public CarUiSmoothScroller(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        mMillisecondsPerInch = CarUiUtils.getFloat(context.getResources(),
                R.dimen.car_ui_scrollbar_milliseconds_per_inch);
        mDecelerationTimeDivisor = CarUiUtils.getFloat(context.getResources(),
                R.dimen.car_ui_scrollbar_deceleration_times_divisor);
        mInterpolator =
                new DecelerateInterpolator(
                        CarUiUtils.getFloat(context.getResources(),
                                R.dimen.car_ui_scrollbar_decelerate_interpolator_factor));
        mDensityDpi = context.getResources().getDisplayMetrics().densityDpi;
        mMillisecondsPerPixel = mMillisecondsPerInch / mDensityDpi;
    }

    @Override
    protected int getVerticalSnapPreference() {
        // Returning SNAP_TO_START will ensure that if the top (start) row is partially visible it
        // will be scrolled downward (END) to make the row fully visible.
        return SNAP_TO_START;
    }

    @Override
    protected void onTargetFound(View targetView, RecyclerView.State state, Action action) {
        int dy = calculateDyToMakeVisible(targetView, SNAP_TO_START);

        if (dy == 0) {
            return;
        }

        final int time = calculateTimeForDeceleration(dy);
        if (time > 0) {
            action.update(0, -dy, time, mInterpolator);
        }
    }

    @Override
    protected int calculateTimeForScrolling(int dx) {
        return (int) Math.ceil(Math.abs(dx) * mMillisecondsPerPixel);
    }

    @Override
    protected int calculateTimeForDeceleration(int dx) {
        return (int) Math.ceil(calculateTimeForScrolling(dx) / mDecelerationTimeDivisor);
    }
}
