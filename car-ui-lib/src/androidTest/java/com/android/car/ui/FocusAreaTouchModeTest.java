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

import static android.view.View.FOCUS_RIGHT;

import static com.google.common.truth.Truth.assertWithMessage;

import android.graphics.Rect;
import android.view.View;

import androidx.test.rule.ActivityTestRule;

import com.android.car.ui.test.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/** Unit tests for {@link FocusArea} in touch mode. */
public class FocusAreaTouchModeTest {
    @Rule
    public ActivityTestRule<FocusAreaTestActivity> mActivityRule =
            new ActivityTestRule<>(FocusAreaTestActivity.class, /* initialTouchMode= */ true);

    private FocusAreaTestActivity mActivity;
    private TestFocusArea mFocusArea2;
    private View mView1;

    @Before
    public void setUp() {
        mActivity = mActivityRule.getActivity();
        mFocusArea2 = mActivity.findViewById(R.id.focus_area2);
        mView1 = mActivity.findViewById(R.id.view1);
    }

    @Test
    public void testOnRequestFocusInDescendants_doesNothing() {
        mFocusArea2.post(() -> {
            Rect previouslyFocusedRect = new Rect();
            previouslyFocusedRect.left = mView1.getLeft();
            previouslyFocusedRect.top = mView1.getTop();
            previouslyFocusedRect.right = previouslyFocusedRect.left + mView1.getWidth();
            previouslyFocusedRect.bottom = previouslyFocusedRect.top + mView1.getHeight();
            boolean focusTaken =
                    mFocusArea2.onRequestFocusInDescendants(FOCUS_RIGHT, previouslyFocusedRect);

            assertWithMessage("onRequestFocusInDescendants returned").that(focusTaken).isFalse();
            assertWithMessage("No view should be focused")
                    .that(mFocusArea2.getRootView().findFocus()).isNull();
        });
    }
}
