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

package com.android.car.ui.recyclerview;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.scrollToPosition;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.rule.ActivityTestRule;

import com.android.car.ui.test.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ContentLimitingAdapterUiTest {

    @Rule
    public ActivityTestRule<CarUiRecyclerViewTestActivity> mActivityRule =
            new ActivityTestRule<>(CarUiRecyclerViewTestActivity.class);

    private ContentLimitingAdapter<TestViewHolder> mContentLimitingAdapter;
    private RecyclerView mCarUiRecyclerView;

    @Before
    public void setUp() {
        mContentLimitingAdapter = new TestContentLimitingAdapter(50);
        mCarUiRecyclerView = mActivityRule.getActivity().requireViewById(R.id.list);
        mActivityRule.getActivity().runOnUiThread(() -> {
            mCarUiRecyclerView.setAdapter(mContentLimitingAdapter);
        });
    }

    @Test
    public void setMaxItem_toLowerThanTotalItems() throws Throwable {
        onView(withId(R.id.list)).check(matches(isDisplayed()));

        // Switch to limited
        mActivityRule.runOnUiThread(() -> {
            mContentLimitingAdapter.setMaxItems(20);
        });
        Thread.sleep(300);
        onView(withText("Item 0")).check(matches(isDisplayed()));
        onView(withId(R.id.list)).perform(scrollToPosition(20));
        onView(withText("Item 19")).check(matches(isDisplayed()));
        onView(withId(com.android.car.ui.R.id.car_ui_list_limiting_message))
                .check(matches(isDisplayed()));

        // Switch back to unlimited
        mActivityRule.runOnUiThread(() -> {
            mContentLimitingAdapter.setMaxItems(-1);
        });
        Thread.sleep(300);
        onView(withId(com.android.car.ui.R.id.car_ui_list_limiting_message)).check(doesNotExist());
    }

    @Test
    public void setMaxItem_toOne() throws Throwable {
        onView(withId(R.id.list)).check(matches(isDisplayed()));

        mActivityRule.runOnUiThread(() -> {
            mContentLimitingAdapter.setMaxItems(1);
        });
        Thread.sleep(300);
        onView(withText("Item 0")).check(matches(isDisplayed()));
        onView(withText("Item 1")).check(doesNotExist());
        onView(withId(com.android.car.ui.R.id.car_ui_list_limiting_message))
                .check(matches(isDisplayed()));

        // Switch back to unlimited
        mActivityRule.runOnUiThread(() -> {
            mContentLimitingAdapter.setMaxItems(-1);
        });
        Thread.sleep(300);
        onView(withId(com.android.car.ui.R.id.car_ui_list_limiting_message)).check(doesNotExist());
    }

    @Test
    public void setMaxItem_toZero() throws Throwable {
        onView(withId(R.id.list)).check(matches(isDisplayed()));

        mActivityRule.runOnUiThread(() -> {
            mContentLimitingAdapter.setMaxItems(0);
        });
        Thread.sleep(300);
        onView(withText("Item 0")).check(doesNotExist());
        onView(withId(com.android.car.ui.R.id.car_ui_list_limiting_message))
                .check(matches(isDisplayed()));

        mActivityRule.runOnUiThread(() -> {
            mContentLimitingAdapter.setMaxItems(-1);
        });
        Thread.sleep(300);
        onView(withId(com.android.car.ui.R.id.car_ui_list_limiting_message)).check(doesNotExist());
    }

    @Test
    public void setMaxItem_toHigherThanTotalItems() throws Throwable {
        mActivityRule.runOnUiThread(() -> {
            mContentLimitingAdapter.setMaxItems(70);
        });
        Thread.sleep(300);
        onView(withText("Item 0")).check(matches(isDisplayed()));
        onView(withId(R.id.list)).perform(scrollToPosition(49));
        onView(withText("Item 49")).check(matches(isDisplayed()));
        onView(withId(com.android.car.ui.R.id.car_ui_list_limiting_message))
                .check(doesNotExist());

        // Switch back to unlimited
        mActivityRule.runOnUiThread(() -> {
            mContentLimitingAdapter.setMaxItems(-1);
        });
        Thread.sleep(300);
        onView(withId(com.android.car.ui.R.id.car_ui_list_limiting_message)).check(doesNotExist());
    }
}
