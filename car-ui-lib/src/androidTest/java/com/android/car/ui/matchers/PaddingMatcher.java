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

package com.android.car.ui.matchers;

import android.view.View;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class PaddingMatcher extends TypeSafeMatcher<View> {

    public enum Side {
        TOP,
        BOTTOM,
        LEFT,
        RIGHT,
        START,
        END
    }

    private Side mSide;
    private int mMin;
    private int mMax;

    public PaddingMatcher(Side side, int min, int max) {
        mSide = side;
        mMin = min;
        mMax = max;
    }

    @Override
    protected boolean matchesSafely(View item) {
        int padding = 0;
        switch (mSide) {
            case TOP:
                padding = item.getPaddingTop();
                break;
            case BOTTOM:
                padding = item.getPaddingBottom();
                break;
            case LEFT:
                padding = item.getPaddingLeft();
                break;
            case RIGHT:
                padding = item.getPaddingRight();
                break;
            case START:
                padding = item.getPaddingStart();
                break;
            case END:
                padding = item.getPaddingEnd();
                break;
        }

        if (mMin >= 0 && padding < mMin) {
            return false;
        }

        return mMax < 0 || padding <= mMax;
    }

    @Override
    public void describeTo(Description description) {
        description
            .appendText("with " + mSide.toString() + " padding between " + mMin + " and " + mMax);
    }
}
