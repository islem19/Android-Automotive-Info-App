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

package com.android.car.ui.imewidescreen;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.assertThat;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import static com.android.car.ui.imewidescreen.CarUiImeWideScreenController.REQUEST_RENDER_CONTENT_AREA;
import static com.android.car.ui.imewidescreen.CarUiImeWideScreenController.WIDE_SCREEN_ACTION;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import android.app.Dialog;
import android.content.Context;
import android.inputmethodservice.ExtractEditText;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.InputMethodService.Insets;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.rule.ActivityTestRule;

import com.android.car.ui.test.R;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for {@link CarUiImeWideScreenController}.
 */
public class CarUiImeWideScreenControllerTest {

    private Context mContext = ApplicationProvider.getApplicationContext();

    @Mock
    Context mMockContext;

    @Mock
    InputMethodService mInputMethodService;

    @Mock
    Dialog mDialog;

    @Mock
    Window mWindow;

    private CarUiImeWideScreenTestActivity mActivity;

    @Rule
    public ActivityTestRule<CarUiImeWideScreenTestActivity> mActivityRule =
            new ActivityTestRule<>(CarUiImeWideScreenTestActivity.class);

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mActivity = mActivityRule.getActivity();
    }

    @After
    public void destroy() {
        mActivity.finish();
    }

    @Test
    public void createWideScreenImeView_shouldWrapTheViewInTemplate() {
        // make sure view is wrapped in the template.
        assertNotNull(mActivity.findViewById(R.id.test_ime_input_view_id));

        // check all views in template default visibility.
        onView(withId(R.id.car_ui_wideScreenDescriptionTitle)).check(matches(not(isDisplayed())));
        onView(withId(R.id.car_ui_wideScreenDescription)).check(matches(not(isDisplayed())));
        onView(withId(R.id.car_ui_inputExtractActionAutomotive)).check(matches(not(isDisplayed())));
        onView(withId(R.id.car_ui_wideScreenSearchResultList)).check(matches(not(isDisplayed())));
        onView(withId(R.id.car_ui_wideScreenErrorMessage)).check(matches(not(isDisplayed())));
        onView(withId(R.id.car_ui_wideScreenError)).check(matches(not(isDisplayed())));
        onView(withId(R.id.car_ui_contentAreaAutomotive)).check(matches(not(isDisplayed())));

        onView(withId(R.id.car_ui_wideScreenExtractedTextIcon)).check(matches(isDisplayed()));
        onView(withId(R.id.car_ui_wideScreenClearData)).check(matches(isDisplayed()));
        onView(withId(R.id.car_ui_fullscreenArea)).check(matches(isDisplayed()));
        onView(withId(R.id.car_ui_inputExtractEditTextContainer)).check(matches(isDisplayed()));

        // check if the click listener is installed on the image to clear data.
        View clearDataIcon = mActivity.findViewById(R.id.car_ui_wideScreenClearData);
        assertTrue(clearDataIcon.hasOnClickListeners());
    }

    @Test
    public void onComputeInsets_showContentArea_shouldUpdateEntireAreaAsTouchable() {
        when(mInputMethodService.getWindow()).thenReturn(mDialog);
        when(mDialog.getWindow()).thenReturn(mWindow);
        View view = new FrameLayout(mContext);
        view.setTop(0);
        view.setBottom(200);
        when(mWindow.getDecorView()).thenReturn(view);

        InputMethodService.Insets outInsets = new Insets();
        CarUiImeWideScreenController carUiImeWideScreenController = getController();
        carUiImeWideScreenController.onComputeInsets(outInsets);

        assertThat(outInsets.touchableInsets, is(InputMethodService.Insets.TOUCHABLE_INSETS_FRAME));
        assertThat(outInsets.contentTopInsets, is(200));
        assertThat(outInsets.visibleTopInsets, is(200));
    }

    @Test
    public void onComputeInsets_hideContentArea_shouldUpdateRegionAsTouchable() {
        when(mInputMethodService.getWindow()).thenReturn(mDialog);
        when(mDialog.getWindow()).thenReturn(mWindow);
        View view = new FrameLayout(mContext);
        view.setTop(0);
        view.setBottom(200);
        when(mWindow.getDecorView()).thenReturn(view);

        View imeInputView = LayoutInflater.from(mContext)
                .inflate(R.layout.test_ime_input_view, null, false);
        CarUiImeWideScreenController carUiImeWideScreenController = getController();
        carUiImeWideScreenController.setExtractEditText(new ExtractEditText(mContext));
        carUiImeWideScreenController.createWideScreenImeView(imeInputView);

        Bundle bundle = new Bundle();
        bundle.putBoolean(REQUEST_RENDER_CONTENT_AREA, false);
        carUiImeWideScreenController.onAppPrivateCommand(WIDE_SCREEN_ACTION, bundle);

        InputMethodService.Insets outInsets = new Insets();
        carUiImeWideScreenController.onComputeInsets(outInsets);

        assertThat(outInsets.touchableInsets,
                is(InputMethodService.Insets.TOUCHABLE_INSETS_REGION));
        assertThat(outInsets.contentTopInsets, is(200));
        assertThat(outInsets.visibleTopInsets, is(200));
    }

    private CarUiImeWideScreenController getController() {
        return new CarUiImeWideScreenController(mContext, mInputMethodService) {
            @Override
            public boolean isWideScreenMode() {
                return true;
            }
        };
    }
}
