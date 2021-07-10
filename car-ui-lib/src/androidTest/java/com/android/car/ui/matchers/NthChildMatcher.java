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
import android.view.ViewGroup;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class NthChildMatcher extends TypeSafeMatcher<View> {

    private Matcher<View> mParentMatcher;
    private int mPosition;

    public NthChildMatcher(Matcher<View> parentMatcher, int position) {
        mParentMatcher = parentMatcher;
        mPosition = position;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("position " + mPosition + " of parent ");
        mParentMatcher.describeTo(description);
    }

    @Override
    public boolean matchesSafely(View view) {
        if (!(view.getParent() instanceof ViewGroup)) {
            return false;
        }

        ViewGroup parent = (ViewGroup) view.getParent();

        return mParentMatcher.matches(parent)
                && parent.getChildCount() > mPosition
                && view.equals(parent.getChildAt(mPosition));
    }
}
