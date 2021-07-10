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

package com.android.car.ui.baselayout;

import java.util.Objects;

/**
 * A representation of the insets into the content view that the user-accessible
 * content should have.
 *
 * See {@link InsetsChangedListener} for more information.
 */
public final class Insets {
    private final int mLeft;
    private final int mRight;
    private final int mTop;
    private final int mBottom;

    public Insets() {
        mLeft = mRight = mTop = mBottom = 0;
    }

    public Insets(int left, int top, int right, int bottom) {
        mLeft = left;
        mRight = right;
        mTop = top;
        mBottom = bottom;
    }

    public int getLeft() {
        return mLeft;
    }

    public int getRight() {
        return mRight;
    }

    public int getTop() {
        return mTop;
    }

    public int getBottom() {
        return mBottom;
    }

    @Override
    public String toString() {
        return "{ left: " + mLeft + ", right: " + mRight
                + ", top: " + mTop + ", bottom: " + mBottom + " }";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Insets insets = (Insets) o;
        return mLeft == insets.mLeft
                && mRight == insets.mRight
                && mTop == insets.mTop
                && mBottom == insets.mBottom;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mLeft, mRight, mTop, mBottom);
    }
}
