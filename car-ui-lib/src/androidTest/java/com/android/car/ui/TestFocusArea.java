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

package com.android.car.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

/** A {@link FocusArea} used for testing. */
public class TestFocusArea extends FocusArea {

    /** Whether {@link #onDraw(Canvas)} was called. */
    private boolean mOnDrawCalled;

    /** Whether {@link #draw(Canvas)} was called. */
    private boolean mDrawCalled;

    private int mLayoutDirection = LAYOUT_DIRECTION_LTR;

    public TestFocusArea(Context context) {
        super(context);
    }

    public TestFocusArea(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public TestFocusArea(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public TestFocusArea(Context context, @Nullable AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void onDraw(Canvas canvas) {
        mOnDrawCalled = true;
        super.onDraw(canvas);
    }

    @Override
    public void draw(Canvas canvas) {
        mDrawCalled = true;
        super.draw(canvas);
    }

    public boolean onDrawCalled() {
        return mOnDrawCalled;
    }

    public void setOnDrawCalled(boolean onDrawCalled) {
        mOnDrawCalled = onDrawCalled;
    }

    public boolean drawCalled() {
        return mDrawCalled;
    }

    public void setDrawCalled(boolean drawCalled) {
        mDrawCalled = drawCalled;
    }

    @Override
    public void setLayoutDirection(int layoutDirection) {
        // The real setLayoutDirection doesn't work in the test, so let's mock it.
        if (mLayoutDirection != layoutDirection) {
            mLayoutDirection = layoutDirection;
            // To trigger the highlight padding update, we need to call onLayout. Note: the
            // parameters don't matter.
            onLayout(false, 0, 0, 0, 0);
        }
    }

    @Override
    public int getLayoutDirection() {
        return mLayoutDirection;
    }
}
