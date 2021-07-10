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

import com.android.car.ui.matchers.PaddingMatcher.Side;

import org.hamcrest.Matcher;

public class ViewMatchers {
    public static Matcher<View> withDrawable(int drawableId) {
        return new DrawableMatcher(drawableId);
    }

    public static Matcher<View> nthChildOfView(Matcher<View> parentMatcher, int n) {
        return new NthChildMatcher(parentMatcher, n);
    }

    public static Matcher<View> withIndex(Matcher<View> matcher, int index) {
        return new IndexMatcher(matcher, index);
    }

    public static Matcher<View> withPadding(Side side, int exactly) {
        return new PaddingMatcher(side, exactly, exactly);
    }

    public static Matcher<View> withPaddingAtLeast(Side side, int min) {
        return new PaddingMatcher(side, min, -1);
    }
}
