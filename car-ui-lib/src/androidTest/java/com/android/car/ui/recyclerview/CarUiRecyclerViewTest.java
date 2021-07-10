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
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.PositionAssertions.isBottomAlignedWith;
import static androidx.test.espresso.assertion.PositionAssertions.isCompletelyAbove;
import static androidx.test.espresso.assertion.PositionAssertions.isLeftAlignedWith;
import static androidx.test.espresso.assertion.PositionAssertions.isRightAlignedWith;
import static androidx.test.espresso.assertion.PositionAssertions.isTopAlignedWith;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.scrollToPosition;
import static androidx.test.espresso.matcher.ViewMatchers.assertThat;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static com.android.car.ui.actions.LowLevelActions.performDrag;
import static com.android.car.ui.actions.LowLevelActions.pressAndHold;
import static com.android.car.ui.actions.LowLevelActions.release;
import static com.android.car.ui.actions.LowLevelActions.touchDownAndUp;
import static com.android.car.ui.actions.ViewActions.waitForView;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.OrientationHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import com.android.car.ui.TestActivity;
import com.android.car.ui.recyclerview.decorations.grid.GridDividerItemDecoration;
import com.android.car.ui.test.R;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Unit tests for {@link CarUiRecyclerView}. */
public class CarUiRecyclerViewTest {

    @Rule
    public ActivityScenarioRule<TestActivity> mActivityRule =
            new ActivityScenarioRule<>(TestActivity.class);

    ActivityScenario<TestActivity> mScenario = ActivityScenario.launch(TestActivity.class);

    private TestActivity mActivity;
    private Context mTestableContext;
    private Resources mTestableResources;

    @Before
    public void setUp() {
        mScenario.onActivity(activity -> {
            mActivity = activity;
            mTestableContext = spy(mActivity);
            mTestableResources = spy(mActivity.getResources());
        });

        when(mTestableContext.getResources()).thenReturn(mTestableResources);
    }

    @After
    public void tearDown() {
        for (IdlingResource idlingResource : IdlingRegistry.getInstance().getResources()) {
            IdlingRegistry.getInstance().unregister(idlingResource);
        }
    }

    @Test
    public void testIsScrollbarPresent_scrollbarEnabled() {
        doReturn(true).when(mTestableResources).getBoolean(R.bool.car_ui_scrollbar_enable);
        CarUiRecyclerView carUiRecyclerView = new CarUiRecyclerView(mTestableContext);
        ViewGroup container = mActivity.findViewById(R.id.test_container);
        container.post(() -> {
            container.addView(carUiRecyclerView);
            carUiRecyclerView.setAdapter(new TestAdapter(100));
        });

        onView(withId(R.id.car_ui_scroll_bar)).check(matches(isDisplayed()));
    }

    @Test
    public void testIsScrollbarPresent_scrollbarDisabled() {
        doReturn(false).when(mTestableResources).getBoolean(R.bool.car_ui_scrollbar_enable);
        CarUiRecyclerView carUiRecyclerView = new CarUiRecyclerView(mTestableContext);
        ViewGroup container = mActivity.findViewById(R.id.test_container);
        container.post(() -> {
            container.addView(carUiRecyclerView);
            carUiRecyclerView.setAdapter(new TestAdapter(100));
        });

        onView(withId(R.id.car_ui_scroll_bar)).check(doesNotExist());
    }

    @Test
    public void testGridLayout() {
        TypedArray typedArray = spy(mActivity.getBaseContext().obtainStyledAttributes(
                null, R.styleable.CarUiRecyclerView));

        doReturn(typedArray).when(mTestableContext).obtainStyledAttributes(
                any(),
                eq(R.styleable.CarUiRecyclerView),
                anyInt(),
                anyInt());
        when(typedArray.getInt(eq(R.styleable.CarUiRecyclerView_layoutStyle), anyInt()))
                .thenReturn(CarUiRecyclerView.CarUiRecyclerViewLayout.GRID);
        when(typedArray.getInt(eq(R.styleable.CarUiRecyclerView_numOfColumns), anyInt()))
                .thenReturn(3);

        // Ensure the CarUiRecyclerViewLayout constant matches the styleable attribute enum value
        assertEquals(CarUiRecyclerView.CarUiRecyclerViewLayout.GRID, 1);

        CarUiRecyclerView carUiRecyclerView = new CarUiRecyclerView(mTestableContext);
        ViewGroup container = mActivity.findViewById(R.id.test_container);
        TestAdapter adapter = new TestAdapter(4);
        container.post(() -> {
            container.addView(carUiRecyclerView);
            carUiRecyclerView.setAdapter(adapter);
        });

        assertTrue(carUiRecyclerView.getLayoutManager() instanceof GridLayoutManager);

        // Check that all items in the first row are top-aligned.
        onView(withText(adapter.getItemText(0))).check(
                isTopAlignedWith(withText(adapter.getItemText(1))));
        onView(withText(adapter.getItemText(1))).check(
                isTopAlignedWith(withText(adapter.getItemText(2))));

        // Check that all items in the first row are bottom-aligned.
        onView(withText(adapter.getItemText(0))).check(
                isBottomAlignedWith(withText(adapter.getItemText(1))));
        onView(withText(adapter.getItemText(1))).check(
                isBottomAlignedWith(withText(adapter.getItemText(2))));

        // Check that items in second row are rendered correctly below the first row.
        onView(withText(adapter.getItemText(0))).check(
                isCompletelyAbove(withText(adapter.getItemText(3))));
        onView(withText(adapter.getItemText(0))).check(
                isLeftAlignedWith(withText(adapter.getItemText(3))));
        onView(withText(adapter.getItemText(0))).check(
                isRightAlignedWith(withText(adapter.getItemText(3))));
    }

    @Test
    public void testLinearLayout() {
        TypedArray typedArray = spy(mActivity.getBaseContext().obtainStyledAttributes(
                null, R.styleable.CarUiRecyclerView));

        doReturn(typedArray).when(mTestableContext).obtainStyledAttributes(
                any(),
                eq(R.styleable.CarUiRecyclerView),
                anyInt(),
                anyInt());
        when(typedArray.getInt(eq(R.styleable.CarUiRecyclerView_layoutStyle), anyInt()))
                .thenReturn(CarUiRecyclerView.CarUiRecyclerViewLayout.LINEAR);

        // Ensure the CarUiRecyclerViewLayout constant matches the styleable attribute enum value
        assertEquals(CarUiRecyclerView.CarUiRecyclerViewLayout.LINEAR, 0);

        CarUiRecyclerView carUiRecyclerView = new CarUiRecyclerView(mTestableContext);
        ViewGroup container = mActivity.findViewById(R.id.test_container);
        TestAdapter adapter = new TestAdapter(4);
        container.post(() -> {
            container.addView(carUiRecyclerView);
            carUiRecyclerView.setAdapter(adapter);
        });

        assertTrue(carUiRecyclerView.getLayoutManager() instanceof LinearLayoutManager);

        // Check that item views are laid out linearly.
        onView(withText(adapter.getItemText(0))).check(
                isCompletelyAbove(withText(adapter.getItemText(1))));
        onView(withText(adapter.getItemText(1))).check(
                isCompletelyAbove(withText(adapter.getItemText(2))));
        onView(withText(adapter.getItemText(2))).check(
                isCompletelyAbove(withText(adapter.getItemText(3))));
    }

    @Test
    public void testSetLayoutManager_shouldUpdateItemDecorations() {
        TypedArray typedArray = spy(mActivity.getBaseContext().obtainStyledAttributes(
                null, R.styleable.CarUiRecyclerView));

        doReturn(typedArray).when(mTestableContext).obtainStyledAttributes(
                any(),
                eq(R.styleable.CarUiRecyclerView),
                anyInt(),
                anyInt());
        when(typedArray.getBoolean(eq(R.styleable.CarUiRecyclerView_enableDivider), anyBoolean()))
                .thenReturn(true);
        when(typedArray.getInt(eq(R.styleable.CarUiRecyclerView_layoutStyle), anyInt()))
                .thenReturn(CarUiRecyclerView.CarUiRecyclerViewLayout.GRID);
        when(typedArray.getInt(eq(R.styleable.CarUiRecyclerView_numOfColumns), anyInt()))
                .thenReturn(3);

        CarUiRecyclerView carUiRecyclerView = new CarUiRecyclerView(mTestableContext);
        ViewGroup container = mActivity.findViewById(R.id.test_container);
        TestAdapter adapter = new TestAdapter(4);
        container.post(() -> {
            container.addView(carUiRecyclerView);
            carUiRecyclerView.setAdapter(adapter);
        });

        assertTrue(carUiRecyclerView.getLayoutManager() instanceof GridLayoutManager);
        assertEquals(carUiRecyclerView.getItemDecorationCount(), 3);
        assertTrue(carUiRecyclerView.getItemDecorationAt(0) instanceof GridDividerItemDecoration);

        carUiRecyclerView.setLayoutManager(new LinearLayoutManager(mTestableContext));

        assertTrue(carUiRecyclerView.getLayoutManager() instanceof LinearLayoutManager);
        assertEquals(carUiRecyclerView.getItemDecorationCount(), 3);
        assertFalse(carUiRecyclerView.getItemDecorationAt(0) instanceof GridDividerItemDecoration);
    }

    @Test
    public void testVisibility_goneAtInflationWithChangeToVisible() {
        mActivity.runOnUiThread(
                () -> mActivity.setContentView(
                        R.layout.car_ui_recycler_view_gone_test_activity));

        onView(withId(R.id.list)).check(matches(not(isDisplayed())));

        CarUiRecyclerView carUiRecyclerView = mActivity.requireViewById(R.id.list);
        TestAdapter adapter = new TestAdapter(3);
        mActivity.runOnUiThread(() -> {
            carUiRecyclerView.setAdapter(adapter);
            carUiRecyclerView.setVisibility(View.VISIBLE);
        });

        // Check that items in are displayed.
        onView(withText(adapter.getItemText(0))).check(matches(isDisplayed()));
        onView(withText(adapter.getItemText(1))).check(matches(isDisplayed()));
        onView(withText(adapter.getItemText(2))).check(matches(isDisplayed()));
    }

    @Test
    public void testVisibility_invisibleAtInflationWithChangeToVisible() {
        mActivity.runOnUiThread(
                () -> mActivity.setContentView(
                        R.layout.car_ui_recycler_view_invisible_test_activity));

        onView(withId(R.id.list)).check(matches(not(isDisplayed())));

        CarUiRecyclerView carUiRecyclerView = mActivity.requireViewById(R.id.list);
        TestAdapter adapter = new TestAdapter(3);
        mActivity.runOnUiThread(() -> {
            carUiRecyclerView.setAdapter(adapter);
            carUiRecyclerView.setVisibility(View.VISIBLE);
        });

        // Check that items in are displayed.
        onView(withText(adapter.getItemText(0))).check(matches(isDisplayed()));
        onView(withText(adapter.getItemText(1))).check(matches(isDisplayed()));
        onView(withText(adapter.getItemText(2))).check(matches(isDisplayed()));
    }

    @Test
    public void testFirstItemAtTop_onInitialLoad() {
        mActivity.runOnUiThread(
                () -> mActivity.setContentView(R.layout.car_ui_recycler_view_test_activity));

        onView(withId(R.id.list)).check(matches(isDisplayed()));

        CarUiRecyclerView carUiRecyclerView = mActivity.requireViewById(R.id.list);
        TestAdapter adapter = new TestAdapter(25);
        mActivity.runOnUiThread(() -> {
            carUiRecyclerView.setAdapter(adapter);
        });

        onView(withText(adapter.getItemText(0))).check(matches(isDisplayed()));

        LinearLayoutManager layoutManager =
                (LinearLayoutManager) carUiRecyclerView.getLayoutManager();
        assertEquals(layoutManager.findFirstVisibleItemPosition(), 0);
    }

    @Test
    public void testPageUpAndDownMoveSameDistance() {
        mActivity.runOnUiThread(
                () -> mActivity.setContentView(R.layout.car_ui_recycler_view_test_activity));

        onView(withId(R.id.list)).check(matches(isDisplayed()));

        CarUiRecyclerView carUiRecyclerView = mActivity.requireViewById(R.id.list);

        // Can't use OrientationHelper here, because it returns 0 when calling getTotalSpace methods
        // until LayoutManager's onLayoutComplete is called. In this case waiting until the first
        // item of the list is displayed guarantees that OrientationHelper is initialized properly.
        int totalSpace = carUiRecyclerView.getHeight()
                - carUiRecyclerView.getPaddingTop()
                - carUiRecyclerView.getPaddingBottom();
        PerfectFitTestAdapter adapter = new PerfectFitTestAdapter(5, totalSpace);
        mActivity.runOnUiThread(() -> carUiRecyclerView.setAdapter(adapter));

        IdlingRegistry.getInstance().register(new ScrollIdlingResource(carUiRecyclerView));
        onView(withText(adapter.getItemText(0))).check(matches(isDisplayed()));

        LinearLayoutManager layoutManager =
                (LinearLayoutManager) carUiRecyclerView.getLayoutManager();

        OrientationHelper orientationHelper = OrientationHelper.createVerticalHelper(layoutManager);
        assertEquals(totalSpace, orientationHelper.getTotalSpace());

        // Move down one page so there will be sufficient pages for up and downs.
        onView(withId(R.id.car_ui_scrollbar_page_down)).perform(click());

        int topPosition = layoutManager.findFirstVisibleItemPosition();

        for (int i = 0; i < 3; i++) {
            onView(withId(R.id.car_ui_scrollbar_page_down)).perform(click());
            onView(withId(R.id.car_ui_scrollbar_page_up)).perform(click());
        }

        assertEquals(layoutManager.findFirstVisibleItemPosition(), topPosition);
    }

    @Test
    public void testContinuousScroll() {
        mActivity.runOnUiThread(
                () -> mActivity.setContentView(R.layout.car_ui_recycler_view_test_activity));

        onView(withId(R.id.list)).check(matches(isDisplayed()));

        CarUiRecyclerView carUiRecyclerView = mActivity.requireViewById(R.id.list);
        TestAdapter adapter = new TestAdapter(50);
        mActivity.runOnUiThread(() -> {
            carUiRecyclerView.setAdapter(adapter);
        });

        IdlingRegistry.getInstance().register(new ScrollIdlingResource(carUiRecyclerView));
        onView(withText(adapter.getItemText(0))).check(matches(isDisplayed()));

        LinearLayoutManager layoutManager =
                (LinearLayoutManager) carUiRecyclerView.getLayoutManager();

        // Press and hold the down button for 2 seconds to scroll the list to bottom.
        onView(withId(R.id.car_ui_scrollbar_page_down)).perform(pressAndHold());
        onView(isRoot()).perform(waitForView(withText("Sample item #49"), 3000));
        onView(withId(R.id.car_ui_scrollbar_page_down)).perform(release());

        assertEquals(layoutManager.findLastCompletelyVisibleItemPosition(), 49);
    }

    @Test
    public void testAlphaJumpToMiddleForThumbWhenTrackClicked() {
        mActivity.runOnUiThread(
                () -> mActivity.setContentView(R.layout.car_ui_recycler_view_test_activity));

        onView(withId(R.id.list)).check(matches(isDisplayed()));

        CarUiRecyclerView carUiRecyclerView = mActivity.requireViewById(R.id.list);
        TestAdapter adapter = new TestAdapter(50);
        mActivity.runOnUiThread(() -> {
            carUiRecyclerView.setAdapter(adapter);
        });

        IdlingRegistry.getInstance().register(new ScrollIdlingResource(carUiRecyclerView));
        onView(withText(adapter.getItemText(0))).check(matches(isDisplayed()));

        View trackView = mActivity.requireViewById(R.id.car_ui_scrollbar_track);
        // scroll to the middle
        onView(withId(R.id.car_ui_scrollbar_track)).perform(
                touchDownAndUp(0f, (trackView.getHeight() / 2f)));
        onView(withText(adapter.getItemText(25))).check(matches(isDisplayed()));
    }

    @Test
    public void testAlphaJumpToEndAndStartForThumbWhenTrackClicked() {
        mActivity.runOnUiThread(
                () -> mActivity.setContentView(R.layout.car_ui_recycler_view_test_activity));

        onView(withId(R.id.list)).check(matches(isDisplayed()));

        CarUiRecyclerView carUiRecyclerView = mActivity.requireViewById(R.id.list);
        TestAdapter adapter = new TestAdapter(50);
        mActivity.runOnUiThread(() -> {
            carUiRecyclerView.setAdapter(adapter);
        });

        IdlingRegistry.getInstance().register(new ScrollIdlingResource(carUiRecyclerView));
        onView(withText(adapter.getItemText(0))).check(matches(isDisplayed()));

        View trackView = mActivity.requireViewById(R.id.car_ui_scrollbar_track);
        View thumbView = mActivity.requireViewById(R.id.car_ui_scrollbar_thumb);
        // scroll to the end
        onView(withId(R.id.car_ui_scrollbar_track)).perform(
                touchDownAndUp(0f, trackView.getHeight() - 1));
        onView(withText(adapter.getItemText(49))).check(matches(isDisplayed()));

        // scroll to the start
        onView(withId(R.id.car_ui_scrollbar_track)).perform(
                touchDownAndUp(0f, 1));
        onView(withText(adapter.getItemText(0))).check(matches(isDisplayed()));
    }

    @Test
    public void testThumbDragToCenter() {
        mActivity.runOnUiThread(
                () -> mActivity.setContentView(R.layout.car_ui_recycler_view_test_activity));

        onView(withId(R.id.list)).check(matches(isDisplayed()));

        CarUiRecyclerView carUiRecyclerView = mActivity.requireViewById(R.id.list);
        TestAdapter adapter = new TestAdapter(50);
        mActivity.runOnUiThread(() -> {
            carUiRecyclerView.setAdapter(adapter);
        });

        IdlingRegistry.getInstance().register(new ScrollIdlingResource(carUiRecyclerView));
        onView(withText(adapter.getItemText(0))).check(matches(isDisplayed()));

        View trackView = mActivity.requireViewById(R.id.car_ui_scrollbar_track);
        View thumbView = mActivity.requireViewById(R.id.car_ui_scrollbar_thumb);
        // if you drag too far in a single step you'll stop selecting the thumb view. Hence, drag
        // 5 units at a time for 200 intervals and stop at the center of the track by limitY.

        onView(withId(R.id.car_ui_scrollbar_track)).perform(
                performDrag(0f, (thumbView.getHeight() / 2f), 0,
                        5, 200, Float.MAX_VALUE,
                        trackView.getHeight() / 2f));
        onView(withText(adapter.getItemText(25))).check(matches(isDisplayed()));
    }

    @Test
    public void testPageUpButtonDisabledAtTop() {
        mActivity.runOnUiThread(
                () -> mActivity.setContentView(R.layout.car_ui_recycler_view_test_activity));

        onView(withId(R.id.list)).check(matches(isDisplayed()));

        CarUiRecyclerView carUiRecyclerView = mActivity.requireViewById(R.id.list);
        //  50, because needs to be big enough to make sure content is scrollable.
        TestAdapter adapter = new TestAdapter(50);
        mActivity.runOnUiThread(() -> {
            carUiRecyclerView.setAdapter(adapter);
        });

        IdlingRegistry.getInstance().register(new ScrollIdlingResource(carUiRecyclerView));

        // Initially page_up button is disabled.
        onView(withId(R.id.car_ui_scrollbar_page_up)).check(matches(not(isEnabled())));

        // Moving down, should enable the up bottom.
        onView(withId(R.id.car_ui_scrollbar_page_down)).check(matches(isEnabled()));
        onView(withId(R.id.car_ui_scrollbar_page_down)).perform(click());
        onView(withId(R.id.car_ui_scrollbar_page_up)).check(matches(isEnabled()));

        // Move back up; this should disable the up button again.
        onView(withId(R.id.car_ui_scrollbar_page_up)).perform(click()).check(
                matches(not(isEnabled())));
    }

    @Test
    public void testPageDownScrollsOverLongItem() {
        mActivity.runOnUiThread(
                () -> mActivity.setContentView(R.layout.car_ui_recycler_view_test_activity));

        onView(withId(R.id.list)).check(matches(isDisplayed()));

        int itemCount = 100;
        // Position the long item in the middle.
        int longItemPosition = itemCount / 2;

        Map<Integer, TestAdapter.ItemHeight> heightOverrides = new HashMap<>();
        heightOverrides.put(longItemPosition, TestAdapter.ItemHeight.TALL);
        TestAdapter adapter = new TestAdapter(itemCount, heightOverrides);

        CarUiRecyclerView carUiRecyclerView = mActivity.requireViewById(R.id.list);
        mActivity.runOnUiThread(() -> {
            carUiRecyclerView.setAdapter(adapter);
        });

        IdlingRegistry.getInstance().register(new ScrollIdlingResource(carUiRecyclerView));
        onView(withText(adapter.getItemText(0))).check(matches(isDisplayed()));

        OrientationHelper orientationHelper =
                OrientationHelper.createVerticalHelper(carUiRecyclerView.getLayoutManager());
        int screenHeight = orientationHelper.getTotalSpace();
        // Scroll to a position where long item is partially visible.
        // Scrolling from top, scrollToPosition() aligns the pos-1 item to bottom.
        onView(withId(R.id.list)).perform(scrollToPosition(longItemPosition - 1));
        // Scroll by half the height of the screen so the long item is partially visible.
        mActivity.runOnUiThread(() -> carUiRecyclerView.scrollBy(0, screenHeight / 2));
        // This is needed to make sure scroll is finished before looking for the long item.
        onView(withText(adapter.getItemText(longItemPosition - 1))).check(matches(isDisplayed()));

        // Verify long item is partially shown.
        View longItem = getLongItem(carUiRecyclerView);
        assertThat(
                orientationHelper.getDecoratedStart(longItem),
                is(greaterThan(orientationHelper.getStartAfterPadding())));

        onView(withId(R.id.car_ui_scrollbar_page_down)).perform(click());

        // Verify long item is snapped to top.
        assertThat(orientationHelper.getDecoratedStart(longItem),
                is(equalTo(orientationHelper.getStartAfterPadding())));
        assertThat(orientationHelper.getDecoratedEnd(longItem),
                is(greaterThan(orientationHelper.getEndAfterPadding())));

        // Set a limit to avoid test stuck in non-moving state.
        while (orientationHelper.getDecoratedEnd(longItem)
                > orientationHelper.getEndAfterPadding()) {
            onView(withId(R.id.car_ui_scrollbar_page_down)).perform(click());
        }

        // Verify long item end is aligned to bottom.
        assertThat(orientationHelper.getDecoratedEnd(longItem),
                is(equalTo(orientationHelper.getEndAfterPadding())));

        onView(withId(R.id.car_ui_scrollbar_page_down)).perform(click());
        // Verify that the long item is no longer visible; Should be on the next child
        assertThat(
                orientationHelper.getDecoratedStart(longItem),
                is(lessThan(orientationHelper.getStartAfterPadding())));
    }

    @Test
    public void testPageDownScrollsOverLongItemAtTheEnd() {
        mActivity.runOnUiThread(
                () -> mActivity.setContentView(R.layout.car_ui_recycler_view_test_activity));

        onView(withId(R.id.list)).check(matches(isDisplayed()));

        int itemCount = 100;
        // Position the long item at the end.
        int longItemPosition = itemCount - 1;

        Map<Integer, TestAdapter.ItemHeight> heightOverrides = new HashMap<>();
        heightOverrides.put(longItemPosition, TestAdapter.ItemHeight.TALL);
        TestAdapter adapter = new TestAdapter(itemCount, heightOverrides);

        CarUiRecyclerView carUiRecyclerView = mActivity.requireViewById(R.id.list);
        mActivity.runOnUiThread(() -> {
            // Setting top padding to any number greater than 0.
            // Not having padding will make this test pass all the time.
            // Also adding bottom padding to make sure the padding
            // after the last content is considered in calculations.
            carUiRecyclerView.setPadding(0, 1, 0, 1);
            carUiRecyclerView.setAdapter(adapter);
        });

        IdlingRegistry.getInstance().register(new ScrollIdlingResource(carUiRecyclerView));
        onView(withText(adapter.getItemText(0))).check(matches(isDisplayed()));

        OrientationHelper orientationHelper =
                OrientationHelper.createVerticalHelper(carUiRecyclerView.getLayoutManager());

        // 20 is just an arbitrary number to make sure we reach the end of the recyclerview.
        for (int i = 0; i < 20; i++) {
            onView(withId(R.id.car_ui_scrollbar_page_down)).perform(click());
        }

        onView(withId(R.id.car_ui_scrollbar_page_down)).check(matches(not(isEnabled())));

        View longItem = getLongItem(carUiRecyclerView);
        // Making sure we've reached end of the recyclerview, after
        // adding bottom padding
        assertThat(orientationHelper.getDecoratedEnd(longItem),
                is(equalTo(orientationHelper.getEndAfterPadding())));
    }

    @Test
    public void testPageUpScrollsOverLongItem() {
        mActivity.runOnUiThread(
                () -> mActivity.setContentView(R.layout.car_ui_recycler_view_test_activity));

        onView(withId(R.id.list)).check(matches(isDisplayed()));

        int itemCount = 100;
        // Position the long item in the middle.
        int longItemPosition = itemCount / 2;

        Map<Integer, TestAdapter.ItemHeight> heightOverrides = new HashMap<>();
        heightOverrides.put(longItemPosition, TestAdapter.ItemHeight.TALL);
        TestAdapter adapter = new TestAdapter(itemCount, heightOverrides);

        CarUiRecyclerView carUiRecyclerView = mActivity.requireViewById(R.id.list);
        mActivity.runOnUiThread(() -> {
            carUiRecyclerView.setAdapter(adapter);
        });

        IdlingRegistry.getInstance().register(new ScrollIdlingResource(carUiRecyclerView));
        onView(withText(adapter.getItemText(0))).check(matches(isDisplayed()));

        OrientationHelper orientationHelper =
                OrientationHelper.createVerticalHelper(carUiRecyclerView.getLayoutManager());

        // Scroll to a position just below the long item.
        onView(withId(R.id.list)).perform(scrollToPosition(longItemPosition + 1));

        // Verify long item is off-screen.
        View longItem = getLongItem(carUiRecyclerView);

        assertThat(
                orientationHelper.getDecoratedEnd(longItem),
                is(lessThanOrEqualTo(orientationHelper.getEndAfterPadding())));

        if (orientationHelper.getStartAfterPadding() - orientationHelper.getDecoratedStart(longItem)
                < orientationHelper.getTotalSpace()) {
            onView(withId(R.id.car_ui_scrollbar_page_up)).perform(click());
            assertThat(orientationHelper.getDecoratedStart(longItem),
                    is(greaterThanOrEqualTo(orientationHelper.getStartAfterPadding())));
        } else {
            int topBeforeClick = orientationHelper.getDecoratedStart(longItem);

            onView(withId(R.id.car_ui_scrollbar_page_up)).perform(click());

            // Verify we scrolled 1 screen
            assertThat(orientationHelper.getStartAfterPadding() - topBeforeClick,
                    is(equalTo(orientationHelper.getTotalSpace())));
        }
    }

    @Test
    public void testPageDownScrollsOverVeryLongItem() {
        mActivity.runOnUiThread(
                () -> mActivity.setContentView(R.layout.car_ui_recycler_view_test_activity));

        onView(withId(R.id.list)).check(matches(isDisplayed()));

        int itemCount = 100;
        // Position the long item in the middle.
        int longItemPosition = itemCount / 2;

        Map<Integer, TestAdapter.ItemHeight> heightOverrides = new HashMap<>();
        heightOverrides.put(longItemPosition, TestAdapter.ItemHeight.EXTRA_TALL);
        TestAdapter adapter = new TestAdapter(itemCount, heightOverrides);

        CarUiRecyclerView carUiRecyclerView = mActivity.requireViewById(R.id.list);
        mActivity.runOnUiThread(() -> {
            carUiRecyclerView.setAdapter(adapter);
        });

        IdlingRegistry.getInstance().register(new ScrollIdlingResource(carUiRecyclerView));
        onView(withText(adapter.getItemText(0))).check(matches(isDisplayed()));

        OrientationHelper orientationHelper =
                OrientationHelper.createVerticalHelper(carUiRecyclerView.getLayoutManager());

        int screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
        // Scroll to a position where long item is partially visible.
        // Scrolling from top, scrollToPosition() aligns the pos-1 item to bottom.
        onView(withId(R.id.list)).perform(scrollToPosition(longItemPosition - 1));
        // Scroll by half the height of the screen so the long item is partially visible.
        mActivity.runOnUiThread(() -> carUiRecyclerView.scrollBy(0, screenHeight / 2));

        onView(withText(adapter.getItemText(longItemPosition))).check(matches(isDisplayed()));

        // Verify long item is partially shown.
        View longItem = getLongItem(carUiRecyclerView);
        assertThat(
                orientationHelper.getDecoratedStart(longItem),
                is(greaterThan(orientationHelper.getStartAfterPadding())));

        onView(withId(R.id.car_ui_scrollbar_page_down)).perform(click());

        // Verify long item is snapped to top.
        assertThat(orientationHelper.getDecoratedStart(longItem),
                is(equalTo(orientationHelper.getStartAfterPadding())));
        assertThat(orientationHelper.getDecoratedEnd(longItem),
                is(greaterThan(orientationHelper.getEndAfterPadding())));

        onView(withId(R.id.car_ui_scrollbar_page_down)).perform(click());

        // Verify long item does not snap to bottom.
        assertThat(orientationHelper.getDecoratedEnd(longItem),
                not(equalTo(orientationHelper.getEndAfterPadding())));
    }

    @Test
    public void testPageDownScrollsOverVeryLongItemAtTheEnd() {
        mActivity.runOnUiThread(
                () -> mActivity.setContentView(R.layout.car_ui_recycler_view_test_activity));

        onView(withId(R.id.list)).check(matches(isDisplayed()));

        int itemCount = 100;
        // Position the long item at the end.
        int longItemPosition = itemCount - 1;

        Map<Integer, TestAdapter.ItemHeight> heightOverrides = new HashMap<>();
        heightOverrides.put(longItemPosition, TestAdapter.ItemHeight.EXTRA_TALL);
        TestAdapter adapter = new TestAdapter(itemCount, heightOverrides);

        CarUiRecyclerView carUiRecyclerView = mActivity.requireViewById(R.id.list);
        mActivity.runOnUiThread(() -> {
            // Setting top padding to any number greater than 0.
            // Not having padding will make this test pass all the time.
            // Also adding bottom padding to make sure the padding
            // after the last content is considered in calculations.
            carUiRecyclerView.setPadding(0, 1, 0, 1);
            carUiRecyclerView.setAdapter(adapter);
        });

        IdlingRegistry.getInstance().register(new ScrollIdlingResource(carUiRecyclerView));
        onView(withText(adapter.getItemText(0))).check(matches(isDisplayed()));

        OrientationHelper orientationHelper =
                OrientationHelper.createVerticalHelper(carUiRecyclerView.getLayoutManager());

        // 20 is just an arbitrary number to make sure we reach the end of the recyclerview.
        for (int i = 0; i < 20; i++) {
            onView(withId(R.id.car_ui_scrollbar_page_down)).perform(click());
        }

        onView(withId(R.id.car_ui_scrollbar_page_down)).check(matches(not(isEnabled())));

        View longItem = getLongItem(carUiRecyclerView);
        // Making sure we've reached end of the recyclerview, after
        // adding bottom padding
        assertThat(orientationHelper.getDecoratedEnd(longItem),
                is(equalTo(orientationHelper.getEndAfterPadding())));
    }

    @Test
    public void testPageDownMaintainsMinimumScrollThumbTrackHeight() {
        mActivity.runOnUiThread(
                () -> mActivity.setContentView(R.layout.car_ui_recycler_view_test_activity));

        onView(withId(R.id.list)).check(matches(isDisplayed()));

        int itemCount = 25000;
        TestAdapter adapter = new TestAdapter(itemCount);

        CarUiRecyclerView carUiRecyclerView = mActivity.requireViewById(R.id.list);
        mActivity.runOnUiThread(() -> {
            carUiRecyclerView.setAdapter(adapter);
        });
        IdlingRegistry.getInstance().register(new ScrollIdlingResource(carUiRecyclerView));
        mActivity.runOnUiThread(() -> carUiRecyclerView.requestLayout());

        onView(withId(R.id.car_ui_scrollbar_page_down)).perform(click());

        // Check that thumb track maintains minimum height
        int minThumbViewHeight = mActivity.getResources()
                .getDimensionPixelOffset(R.dimen.car_ui_scrollbar_min_thumb_height);
        View thumbView = mActivity.requireViewById(R.id.car_ui_scrollbar_thumb);
        assertThat(thumbView.getHeight(), is(equalTo(minThumbViewHeight)));
    }

    @Test
    public void testSetPaddingToRecyclerViewContainerWithScrollbar() {
        doReturn(true).when(mTestableResources).getBoolean(R.bool.car_ui_scrollbar_enable);

        CarUiRecyclerView mCarUiRecyclerView = new CarUiRecyclerView(mTestableContext);

        assertThat(mCarUiRecyclerView.getPaddingBottom(), is(equalTo(0)));
        assertThat(mCarUiRecyclerView.getPaddingTop(), is(equalTo(0)));
        assertThat(mCarUiRecyclerView.getPaddingStart(), is(equalTo(0)));
        assertThat(mCarUiRecyclerView.getPaddingEnd(), is(equalTo(0)));

        mCarUiRecyclerView.setPadding(10, 10, 10, 10);

        assertThat(mCarUiRecyclerView.getPaddingTop(), is(equalTo(10)));
        assertThat(mCarUiRecyclerView.getPaddingBottom(), is(equalTo(10)));
        assertThat(mCarUiRecyclerView.getPaddingStart(), is(equalTo(0)));
        assertThat(mCarUiRecyclerView.getPaddingEnd(), is(equalTo(0)));
    }

    @Test
    public void testSetPaddingToRecyclerViewContainerWithoutScrollbar() {
        doReturn(false).when(mTestableResources).getBoolean(R.bool.car_ui_scrollbar_enable);

        CarUiRecyclerView mCarUiRecyclerView = new CarUiRecyclerView(mTestableContext);

        assertThat(mCarUiRecyclerView.getPaddingBottom(), is(equalTo(0)));
        assertThat(mCarUiRecyclerView.getPaddingTop(), is(equalTo(0)));
        assertThat(mCarUiRecyclerView.getPaddingStart(), is(equalTo(0)));
        assertThat(mCarUiRecyclerView.getPaddingEnd(), is(equalTo(0)));

        mCarUiRecyclerView.setPadding(10, 10, 10, 10);

        assertThat(mCarUiRecyclerView.getPaddingTop(), is(equalTo(10)));
        assertThat(mCarUiRecyclerView.getPaddingBottom(), is(equalTo(10)));
        assertThat(mCarUiRecyclerView.getPaddingStart(), is(equalTo(10)));
        assertThat(mCarUiRecyclerView.getPaddingEnd(), is(equalTo(10)));
    }

    @Test
    public void testSetPaddingRelativeToRecyclerViewContainerWithScrollbar() {
        doReturn(true).when(mTestableResources).getBoolean(R.bool.car_ui_scrollbar_enable);

        CarUiRecyclerView mCarUiRecyclerView = new CarUiRecyclerView(mTestableContext);

        assertThat(mCarUiRecyclerView.getPaddingBottom(), is(equalTo(0)));
        assertThat(mCarUiRecyclerView.getPaddingTop(), is(equalTo(0)));
        assertThat(mCarUiRecyclerView.getPaddingStart(), is(equalTo(0)));
        assertThat(mCarUiRecyclerView.getPaddingEnd(), is(equalTo(0)));

        mCarUiRecyclerView.setPaddingRelative(10, 10, 10, 10);

        assertThat(mCarUiRecyclerView.getPaddingTop(), is(equalTo(10)));
        assertThat(mCarUiRecyclerView.getPaddingBottom(), is(equalTo(10)));
        assertThat(mCarUiRecyclerView.getPaddingStart(), is(equalTo(0)));
        assertThat(mCarUiRecyclerView.getPaddingEnd(), is(equalTo(0)));
    }

    @Test
    public void testSetPaddingRelativeToRecyclerViewContainerWithoutScrollbar() {
        doReturn(false).when(mTestableResources).getBoolean(R.bool.car_ui_scrollbar_enable);

        CarUiRecyclerView mCarUiRecyclerView = new CarUiRecyclerView(mTestableContext);

        assertThat(mCarUiRecyclerView.getPaddingBottom(), is(equalTo(0)));
        assertThat(mCarUiRecyclerView.getPaddingTop(), is(equalTo(0)));
        assertThat(mCarUiRecyclerView.getPaddingStart(), is(equalTo(0)));
        assertThat(mCarUiRecyclerView.getPaddingEnd(), is(equalTo(0)));

        mCarUiRecyclerView.setPaddingRelative(10, 10, 10, 10);

        assertThat(mCarUiRecyclerView.getPaddingTop(), is(equalTo(10)));
        assertThat(mCarUiRecyclerView.getPaddingBottom(), is(equalTo(10)));
        assertThat(mCarUiRecyclerView.getPaddingStart(), is(equalTo(10)));
        assertThat(mCarUiRecyclerView.getPaddingEnd(), is(equalTo(10)));
    }

    @Test
    public void testSetAlphaToRecyclerViewWithoutScrollbar() {
        doReturn(false).when(mTestableResources).getBoolean(R.bool.car_ui_scrollbar_enable);

        CarUiRecyclerView mCarUiRecyclerView = new CarUiRecyclerView(mTestableContext);

        assertThat(mCarUiRecyclerView.getAlpha(), is(equalTo(1.0f)));

        mCarUiRecyclerView.setAlpha(0.5f);

        assertThat(mCarUiRecyclerView.getAlpha(), is(equalTo(0.5f)));
    }

    @Test
    public void testSetAlphaToRecyclerViewWithScrollbar() {
        mActivity.runOnUiThread(
                () -> mActivity.setContentView(
                        R.layout.car_ui_recycler_view_test_activity));

        onView(withId(R.id.list)).check(matches(isDisplayed()));

        CarUiRecyclerView carUiRecyclerView = mActivity.requireViewById(R.id.list);

        ViewGroup container = (ViewGroup) carUiRecyclerView.getParent().getParent();

        assertThat(carUiRecyclerView.getAlpha(), is(equalTo(1.0f)));
        assertThat(container.getAlpha(), is(equalTo(1.0f)));

        carUiRecyclerView.setAlpha(0.5f);

        assertThat(carUiRecyclerView.getAlpha(), is(equalTo(1.0f)));
        assertThat(container.getAlpha(), is(equalTo(0.5f)));
    }

    @Test
    public void testCallAnimateOnRecyclerViewWithScrollbar() {
        doReturn(true).when(mTestableResources).getBoolean(R.bool.car_ui_scrollbar_enable);
        CarUiRecyclerView carUiRecyclerView = new CarUiRecyclerView(mTestableContext);

        ViewGroup container = mActivity.findViewById(R.id.test_container);
        container.post(() -> {
            container.addView(carUiRecyclerView);
            carUiRecyclerView.setAdapter(new TestAdapter(100));
        });

        onView(withId(R.id.car_ui_scroll_bar)).check(matches(isDisplayed()));

        ViewGroup recyclerViewContainer = (ViewGroup) carUiRecyclerView.getParent().getParent();

        assertThat(carUiRecyclerView.animate(), is(equalTo(recyclerViewContainer.animate())));
    }

    /**
     * Returns an item in the current list view whose height is taller than that of
     * the CarUiRecyclerView. If that item exists, then it is returned; otherwise an {@link
     * IllegalStateException} is thrown.
     *
     * @return An item that is taller than the CarUiRecyclerView.
     */
    private View getLongItem(CarUiRecyclerView recyclerView) {
        OrientationHelper orientationHelper =
                OrientationHelper.createVerticalHelper(recyclerView.getLayoutManager());
        for (int i = 0; i < recyclerView.getLayoutManager().getChildCount(); i++) {
            View item = recyclerView.getLayoutManager().getChildAt(i);

            if (item.getHeight() > orientationHelper.getTotalSpace()) {
                return item;
            }
        }

        throw new IllegalStateException(
                "No item found that is longer than the height of the CarUiRecyclerView.");
    }

    /** A test adapter that handles inflating test views and binding data to it. */
    private static class TestAdapter extends RecyclerView.Adapter<TestViewHolder> {

        public enum ItemHeight {
            STANDARD,
            TALL,
            EXTRA_TALL
        }

        private final List<String> mData;
        private final Map<Integer, ItemHeight> mHeightOverrides;

        TestAdapter(int itemCount, Map<Integer, ItemHeight> overrides) {
            mHeightOverrides = overrides;
            mData = new ArrayList<>(itemCount);

            for (int i = 0; i < itemCount; i++) {
                mData.add(getItemText(i));
            }
        }

        TestAdapter(int itemCount) {
            this(itemCount, new HashMap<>());
        }

        String getItemText(int position) {
            if (position > mData.size()) {
                return null;
            }

            return String.format("Sample item #%d", position);
        }

        @NonNull
        @Override
        public TestViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            return new TestViewHolder(inflater, parent);
        }

        @Override
        public void onBindViewHolder(@NonNull TestViewHolder holder, int position) {
            ItemHeight height = ItemHeight.STANDARD;

            if (mHeightOverrides.containsKey(position)) {
                height = mHeightOverrides.get(position);
            }

            int screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;

            switch (height) {
                case STANDARD:
                    break;
                case TALL:
                    holder.itemView.setMinimumHeight(screenHeight);
                    break;
                case EXTRA_TALL:
                    holder.itemView.setMinimumHeight(screenHeight * 2);
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + height);
            }

            holder.bind(mData.get(position));
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }
    }

    private static class PerfectFitTestAdapter extends RecyclerView.Adapter<TestViewHolder> {

        private static final int MIN_HEIGHT = 30;
        private final List<String> mData;
        private final int mItemHeight;

        private int getMinHeightPerItemToFitScreen(int screenHeight) {
            // When the height is a prime number, there can only be 1 item per page
            int minHeight = screenHeight;
            for (int i = screenHeight; i >= 1; i--) {
                if (screenHeight % i == 0 && screenHeight / i >= MIN_HEIGHT) {
                    minHeight = screenHeight / i;
                    break;
                }
            }
            return minHeight;
        }

        PerfectFitTestAdapter(int numOfPages, int recyclerViewHeight) {
            mItemHeight = getMinHeightPerItemToFitScreen(recyclerViewHeight);
            int itemsPerPage = recyclerViewHeight / mItemHeight;
            int itemCount = itemsPerPage * numOfPages;
            mData = new ArrayList<>(itemCount);
            for (int i = 0; i < itemCount; i++) {
                mData.add(getItemText(i));
            }
        }

        String getItemText(int position) {
            return String.format("Sample item #%d", position);
        }

        @NonNull
        @Override
        public TestViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            return new TestViewHolder(inflater, parent);
        }

        @Override
        public void onBindViewHolder(@NonNull TestViewHolder holder, int position) {
            holder.itemView.setMinimumHeight(mItemHeight);
            holder.bind(mData.get(position));
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }
    }

    private static class TestViewHolder extends RecyclerView.ViewHolder {
        private TextView mTextView;

        TestViewHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.test_list_item, parent, false));
            mTextView = itemView.findViewById(R.id.text);
        }

        void bind(String text) {
            mTextView.setText(text);
        }
    }

    /**
     * An {@link IdlingResource} that will prevent assertions from running while the {@link
     * CarUiRecyclerView} is scrolling.
     */
    private static class ScrollIdlingResource implements IdlingResource {
        private boolean mIdle = true;
        private ResourceCallback mResourceCallback;

        ScrollIdlingResource(CarUiRecyclerView carUiRecyclerView) {
            carUiRecyclerView
                    .addOnScrollListener(
                            new RecyclerView.OnScrollListener() {
                                @Override
                                public void onScrollStateChanged(@NonNull RecyclerView recyclerView,
                                        int newState) {
                                    super.onScrollStateChanged(recyclerView, newState);
                                    mIdle = (newState == RecyclerView.SCROLL_STATE_IDLE
                                            // Treat dragging as idle, or Espresso will
                                            // block itself when swiping.
                                            || newState == RecyclerView.SCROLL_STATE_DRAGGING);
                                    if (mIdle && mResourceCallback != null) {
                                        mResourceCallback.onTransitionToIdle();
                                    }
                                }

                                @Override
                                public void onScrolled(@NonNull RecyclerView recyclerView, int dx,
                                        int dy) {
                                }
                            });
        }

        @Override
        public String getName() {
            return ScrollIdlingResource.class.getName();
        }

        @Override
        public boolean isIdleNow() {
            return mIdle;
        }

        @Override
        public void registerIdleTransitionCallback(ResourceCallback callback) {
            mResourceCallback = callback;
        }
    }
}
