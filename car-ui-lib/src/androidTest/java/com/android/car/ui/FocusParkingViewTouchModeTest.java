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

import static com.android.car.ui.utils.RotaryConstants.ACTION_RESTORE_DEFAULT_FOCUS;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import android.view.View;

import androidx.test.rule.ActivityTestRule;

import com.android.car.ui.test.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/** Unit test for {@link FocusParkingView} in touch mode. */
public class FocusParkingViewTouchModeTest {

    @Rule
    public ActivityTestRule<FocusParkingViewTestActivity> mActivityRule =
            new ActivityTestRule<>(FocusParkingViewTestActivity.class,
                    /* initialTouchMode= */ true);

    private FocusParkingView mFpv;

    @Before
    public void setUp() {
        FocusParkingViewTestActivity activity = mActivityRule.getActivity();
        mFpv = activity.findViewById(R.id.fpv);
    }

    @Test
    public void testRestoreDefaultFocus_doesNothing() {
        mFpv.post(() -> {
            assertThat(mFpv.getRootView().findFocus()).isNull();

            boolean result = mFpv.restoreDefaultFocus();

            assertWithMessage("restoreDefaultFocus returned").that(result).isFalse();
            assertWithMessage("No view should be focused")
                    .that(mFpv.getRootView().findFocus()).isNull();
        });
    }

    @Test
    public void testRequestFocus_doesNothing() {
        mFpv.post(() -> {
            assertThat(mFpv.getRootView().findFocus()).isNull();

            boolean result = mFpv.requestFocus(View.FOCUS_DOWN, /* previouslyFocusedRect= */ null);

            assertWithMessage("requestFocus returned").that(result).isFalse();
            assertWithMessage("No view should be focused")
                    .that(mFpv.getRootView().findFocus()).isNull();
        });
    }

    @Test
    public void testPerformActionRestoreDefaultFocus_exitsTouchMode() {
        mFpv.post(() -> {
            assertThat(mFpv.getRootView().findFocus()).isNull();

            boolean result = mFpv.performAccessibilityAction(
                    ACTION_RESTORE_DEFAULT_FOCUS, /* arguments= */ null);

            assertWithMessage("performAccessibilityAction returned").that(result).isTrue();
            assertWithMessage("A view should be focused")
                    .that(mFpv.getRootView().findFocus()).isNotNull();
        });
    }
}
