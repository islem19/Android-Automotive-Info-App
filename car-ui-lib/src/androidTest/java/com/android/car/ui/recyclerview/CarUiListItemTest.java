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
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isNotChecked;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import androidx.test.rule.ActivityTestRule;

import com.android.car.ui.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for {@link CarUiListItem}.
 */
public class CarUiListItemTest {

    private CarUiRecyclerView mCarUiRecyclerView;

    @Rule
    public ActivityTestRule<CarUiRecyclerViewTestActivity> mActivityRule =
            new ActivityTestRule<>(CarUiRecyclerViewTestActivity.class);

    @Before
    public void setUp() {
        mCarUiRecyclerView = mActivityRule.getActivity().requireViewById(R.id.list);
    }

    @Test
    public void testItemVisibility_withTitle() {
        List<CarUiListItem> items = new ArrayList<>();

        CarUiContentListItem item = new CarUiContentListItem(CarUiContentListItem.Action.NONE);
        item.setTitle("Test title");
        items.add(item);

        mCarUiRecyclerView.post(
                () -> mCarUiRecyclerView.setAdapter(new CarUiListItemAdapter(items)));

        onView(withId(R.id.title)).check(matches(isDisplayed()));
        onView(withId(R.id.body)).check(matches(not(isDisplayed())));
        onView(withId(R.id.icon_container)).check(matches(not(isDisplayed())));
        onView(withId(R.id.action_container)).check(matches(not(isDisplayed())));
    }

    @Test
    public void testItemVisibility_withBody() {
        List<CarUiListItem> items = new ArrayList<>();

        CarUiContentListItem item = new CarUiContentListItem(CarUiContentListItem.Action.NONE);
        item.setBody("Test body");
        items.add(item);

        mCarUiRecyclerView.post(
                () -> mCarUiRecyclerView.setAdapter(new CarUiListItemAdapter(items)));

        onView(withId(R.id.body)).check(matches(isDisplayed()));
        onView(withId(R.id.title)).check(matches(not(isDisplayed())));
        onView(withId(R.id.icon_container)).check(matches(not(isDisplayed())));
        onView(withId(R.id.action_container)).check(matches(not(isDisplayed())));
    }

    @Test
    public void testItemVisibility_withTitle_withBodyAndIcon() {
        List<CarUiListItem> items = new ArrayList<>();

        CarUiContentListItem item = new CarUiContentListItem(CarUiContentListItem.Action.NONE);
        item.setTitle("Test title");
        item.setBody("Test body");
        item.setIcon(mActivityRule.getActivity().getDrawable(R.drawable.car_ui_icon_close));
        items.add(item);

        mCarUiRecyclerView.post(
                () -> mCarUiRecyclerView.setAdapter(new CarUiListItemAdapter(items)));

        onView(withId(R.id.title)).check(matches(isDisplayed()));
        onView(withId(R.id.body)).check(matches(isDisplayed()));
        onView(withId(R.id.icon_container)).check(matches(isDisplayed()));
        onView(withId(R.id.action_container)).check(matches(not(isDisplayed())));
    }

    @Test
    public void testItem_withCheckbox() {
        List<CarUiListItem> items = new ArrayList<>();

        CarUiContentListItem.OnCheckedChangeListener mockOnCheckedChangeListener = mock(
                CarUiContentListItem.OnCheckedChangeListener.class);

        CarUiContentListItem item = new CarUiContentListItem(CarUiContentListItem.Action.CHECK_BOX);
        item.setTitle("Test item with checkbox");
        item.setOnCheckedChangeListener(mockOnCheckedChangeListener);
        items.add(item);

        mCarUiRecyclerView.post(
                () -> mCarUiRecyclerView.setAdapter(new CarUiListItemAdapter(items)));

        onView(withId(R.id.title)).check(matches(isDisplayed()));
        onView(withId(R.id.checkbox_widget)).check(matches(isDisplayed()));
        onView(withId(R.id.action_divider)).check(matches(not(isDisplayed())));

        // List item with checkbox should be initially unchecked.
        onView(withId(R.id.checkbox_widget)).check(matches(isNotChecked()));
        // Clicks anywhere on the item should toggle the checkbox
        onView(withId(R.id.title)).perform(click());
        onView(withId(R.id.checkbox_widget)).check(matches(isChecked()));
        // Check that onCheckChangedListener was invoked.
        verify(mockOnCheckedChangeListener, times(1)).onCheckedChanged(item, true);

        // Uncheck checkbox with click on the action container
        onView(withId(R.id.action_container)).perform(click());
        onView(withId(R.id.checkbox_widget)).check(matches(isNotChecked()));
        // Check that onCheckChangedListener was invoked.
        verify(mockOnCheckedChangeListener, times(1)).onCheckedChanged(item, false);
    }

    @Test
    public void testItem_withSwitch() {
        List<CarUiListItem> items = new ArrayList<>();

        CarUiContentListItem item = new CarUiContentListItem(CarUiContentListItem.Action.SWITCH);
        item.setBody("Test item with switch");
        item.setChecked(true);
        item.setActionDividerVisible(true);
        items.add(item);

        mCarUiRecyclerView.post(
                () -> mCarUiRecyclerView.setAdapter(new CarUiListItemAdapter(items)));

        onView(withId(R.id.body)).check(matches(isDisplayed()));
        onView(withId(R.id.switch_widget)).check(matches(isDisplayed()));
        onView(withId(R.id.action_divider)).check(matches(isDisplayed()));

        // List item with checkbox should be initially checked.
        onView(withId(R.id.switch_widget)).check(matches(isChecked()));
        // Clicks anywhere on the item should toggle the switch
        onView(withId(R.id.switch_widget)).perform(click());
        onView(withId(R.id.switch_widget)).check(matches(isNotChecked()));
        // Uncheck checkbox with click on the action container
        onView(withId(R.id.body)).perform(click());
        onView(withId(R.id.switch_widget)).check(matches(isChecked()));
    }

    @Test
    public void testItem_withRadioButton() {
        List<CarUiListItem> items = new ArrayList<>();

        CarUiContentListItem item = new CarUiContentListItem(
                CarUiContentListItem.Action.RADIO_BUTTON);
        item.setTitle("Test item with radio button");
        item.setChecked(false);
        items.add(item);

        mCarUiRecyclerView.post(
                () -> mCarUiRecyclerView.setAdapter(new CarUiListItemAdapter(items)));

        onView(withId(R.id.title)).check(matches(isDisplayed()));
        onView(withId(R.id.radio_button_widget)).check(matches(isDisplayed()));

        // List item with checkbox should be initially not checked.
        onView(withId(R.id.radio_button_widget)).check(matches(isNotChecked()));
        // Clicks anywhere on the item should toggle the radio button.
        onView(withId(R.id.radio_button_widget)).perform(click());
        onView(withId(R.id.radio_button_widget)).check(matches(isChecked()));

        // Repeated clicks on a selected radio button should not toggle the element once checked.
        onView(withId(R.id.title)).perform(click());
        onView(withId(R.id.radio_button_widget)).check(matches(isChecked()));
    }

    @Test
    public void testItem_withListener() {
        List<CarUiListItem> items = new ArrayList<>();

        CarUiContentListItem.OnClickListener mockOnCheckedChangeListener = mock(
                CarUiContentListItem.OnClickListener.class);

        CarUiContentListItem item = new CarUiContentListItem(
                CarUiContentListItem.Action.NONE);
        item.setIcon(mActivityRule.getActivity().getDrawable(R.drawable.car_ui_icon_close));
        item.setTitle("Test item with listener");
        item.setBody("Body text");
        item.setOnItemClickedListener(mockOnCheckedChangeListener);
        items.add(item);

        mCarUiRecyclerView.post(
                () -> mCarUiRecyclerView.setAdapter(new CarUiListItemAdapter(items)));

        onView(withId(R.id.title)).check(matches(isDisplayed()));

        // Clicks anywhere on the item should toggle the listener
        onView(withId(R.id.title)).perform(click());
        verify(mockOnCheckedChangeListener, times(1)).onClick(item);

        onView(withId(R.id.body)).perform(click());
        verify(mockOnCheckedChangeListener, times(2)).onClick(item);

        onView(withId(R.id.icon_container)).perform(click());
        verify(mockOnCheckedChangeListener, times(3)).onClick(item);
    }

    @Test
    public void testItem_withListenerAndSupplementalIconListener() {
        List<CarUiListItem> items = new ArrayList<>();

        CarUiContentListItem.OnClickListener clickListener = mock(
                CarUiContentListItem.OnClickListener.class);
        CarUiContentListItem.OnClickListener supplementalIconClickListener = mock(
                CarUiContentListItem.OnClickListener.class);

        CarUiContentListItem item = new CarUiContentListItem(
                CarUiContentListItem.Action.ICON);
        item.setTitle("Test item with two listeners");
        item.setOnItemClickedListener(clickListener);
        item.setSupplementalIcon(
                mActivityRule.getActivity().getDrawable(R.drawable.car_ui_icon_close),
                supplementalIconClickListener);
        items.add(item);

        mCarUiRecyclerView.post(
                () -> mCarUiRecyclerView.setAdapter(new CarUiListItemAdapter(items)));

        onView(withId(R.id.title)).check(matches(isDisplayed()));

        // Clicks anywhere on the item (except supplemental icon) should trigger the item click
        // listener.
        onView(withId(R.id.title)).perform(click());
        verify(clickListener, times(1)).onClick(item);
        verify(supplementalIconClickListener, times(0)).onClick(item);

        onView(withId(R.id.supplemental_icon)).perform(click());
        // Check that icon is argument for single call to click listener.
        verify(supplementalIconClickListener, times(1)).onClick(item);

        // Verify that the standard click listener wasn't also fired.
        verify(clickListener, times(1)).onClick(item);
    }

    @Test
    public void testItem_withSupplementalIconAndIconOnClickListener() {
        List<CarUiListItem> items = new ArrayList<>();

        CarUiContentListItem.OnClickListener mockedItemOnClickListener = mock(
                CarUiContentListItem.OnClickListener.class);
        CarUiContentListItem.OnClickListener mockedIconListener = mock(
                CarUiContentListItem.OnClickListener.class);

        CarUiContentListItem item = new CarUiContentListItem(
                CarUiContentListItem.Action.ICON);
        item.setSupplementalIcon(
                mActivityRule.getActivity().getDrawable(R.drawable.car_ui_icon_close),
                mockedIconListener);
        item.setOnItemClickedListener(mockedItemOnClickListener);
        item.setTitle("Test item with listeners");
        items.add(item);

        mCarUiRecyclerView.post(
                () -> mCarUiRecyclerView.setAdapter(new CarUiListItemAdapter(items)));

        onView(withId(R.id.title)).check(matches(isDisplayed()));

        // Clicks anywhere on the item (outside of the icon) should only invoke the item click
        // listener.
        onView(withId(R.id.title)).perform(click());
        verify(mockedItemOnClickListener, times(1)).onClick(item);

        // Clicks anywhere on the icon should invoke both listeners.
        onView(withId(R.id.action_container)).perform(click());
        verify(mockedItemOnClickListener, times(1)).onClick(item);
        verify(mockedIconListener, times(1)).onClick(item);
    }

    @Test
    public void testRadioButtonListItemAdapter() {
        List<CarUiRadioButtonListItem> items = new ArrayList<>();

        CarUiRadioButtonListItem itemOne = new CarUiRadioButtonListItem();
        String itemOneTitle = "Item 1";
        itemOne.setTitle(itemOneTitle);
        items.add(itemOne);

        CarUiRadioButtonListItem itemTwo = new CarUiRadioButtonListItem();
        String itemTwoTitle = "Item 2";
        itemTwo.setTitle(itemTwoTitle);
        items.add(itemTwo);

        CarUiRadioButtonListItem itemThree = new CarUiRadioButtonListItem();
        String itemThreeTitle = "Item 3";
        itemThree.setTitle(itemThreeTitle);
        items.add(itemThree);

        CarUiRadioButtonListItemAdapter adapter = new CarUiRadioButtonListItemAdapter(items);
        mCarUiRecyclerView.post(
                () -> mCarUiRecyclerView.setAdapter(adapter));

        onView(withText(itemOneTitle)).check(matches(isDisplayed()));
        onView(withText(itemTwoTitle)).check(matches(isDisplayed()));
        onView(withText(itemThreeTitle)).check(matches(isDisplayed()));

        // All items are initially unchecked.
        assertFalse(itemOne.isChecked());
        assertFalse(itemTwo.isChecked());
        assertFalse(itemThree.isChecked());
        assertEquals(adapter.getSelectedItemPosition(), -1);

        // Select first item.
        onView(withText(itemOneTitle)).perform(click());
        assertTrue(itemOne.isChecked());
        assertFalse(itemTwo.isChecked());
        assertFalse(itemThree.isChecked());
        assertEquals(adapter.getSelectedItemPosition(), 0);

        // Select second item.
        onView(withText(itemTwoTitle)).perform(click());
        assertFalse(itemOne.isChecked());
        assertTrue(itemTwo.isChecked());
        assertFalse(itemThree.isChecked());
        assertEquals(adapter.getSelectedItemPosition(), 1);

        // Select third item.
        onView(withText(itemThreeTitle)).perform(click());
        assertFalse(itemOne.isChecked());
        assertFalse(itemTwo.isChecked());
        assertTrue(itemThree.isChecked());
        assertEquals(adapter.getSelectedItemPosition(), 2);
    }
}
