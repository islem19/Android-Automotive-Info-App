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
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * A custom matcher that allows for the specification of an index when multiple views meet the
 * criteria of a matcher.
 */
public class IndexMatcher extends TypeSafeMatcher<View> {

    private final Matcher<View> mMatcher;
    private final int mIndex;
    int mCurrentIndex = 0;

    public IndexMatcher(Matcher<View> matcher, int index) {
        mMatcher = matcher;
        mIndex = index;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("with index: ");
        description.appendValue(mIndex);
        mMatcher.describeTo(description);
    }

    @Override
    public boolean matchesSafely(View view) {
        return mMatcher.matches(view) && mCurrentIndex++ == mIndex;
    }
}
