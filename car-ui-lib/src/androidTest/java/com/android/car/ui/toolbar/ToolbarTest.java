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

package com.android.car.ui.toolbar;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasChildCount;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static com.android.car.ui.actions.ViewActions.waitForView;
import static com.android.car.ui.matchers.ViewMatchers.nthChildOfView;
import static com.android.car.ui.matchers.ViewMatchers.withDrawable;

import static com.google.common.truth.Truth.assertThat;

import static junit.framework.TestCase.assertEquals;

import static org.hamcrest.core.IsNot.not;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.view.View;

import androidx.test.rule.ActivityTestRule;

import com.android.car.ui.core.CarUi;
import com.android.car.ui.test.R;

import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.function.Consumer;

/** Unit test for {@link ToolbarController}. */
public class ToolbarTest {

    @Rule
    public ActivityTestRule<ToolbarTestActivity> mActivityRule =
            new ActivityTestRule<>(ToolbarTestActivity.class);

    @Test
    public void test_setTitle_displaysTitle() throws Throwable {
        runWithToolbar((toolbar) -> toolbar.setTitle("Test title"));

        onView(withText("Test title")).check(matches(isDisplayed()));
    }

    @Test
    public void test_setSubtitle_displaysSubtitle() throws Throwable {
        runWithToolbar((toolbar) -> toolbar.setSubtitle("Test subtitle"));

        onView(withText("Test subtitle")).check(matches(isDisplayed()));
    }

    @Test
    public void test_setSearchHint_isDisplayed() throws Throwable {
        runWithToolbar((toolbar) -> {
            toolbar.setSearchHint("Test search hint");
            toolbar.setState(Toolbar.State.SEARCH);
        });

        onView(withHint("Test search hint")).check(matches(isDisplayed()));
    }

    @Test
    public void setters_and_getters_test() throws Throwable {
        runWithToolbar((toolbar) -> {
            toolbar.setTitle("Foo");
            toolbar.setSearchHint("Foo2");
            toolbar.setShowMenuItemsWhileSearching(true);
            toolbar.setState(Toolbar.State.SUBPAGE);
            toolbar.setNavButtonMode(Toolbar.NavButtonMode.CLOSE);

            assertThat(toolbar.getTitle().toString()).isEqualTo("Foo");
            assertThat(toolbar.getSearchHint().toString()).isEqualTo("Foo2");
            assertThat(toolbar.getShowMenuItemsWhileSearching()).isEqualTo(true);
            assertThat(toolbar.getState()).isEquivalentAccordingToCompareTo(Toolbar.State.SUBPAGE);
            assertThat(toolbar.getNavButtonMode()).isEquivalentAccordingToCompareTo(
                    Toolbar.NavButtonMode.CLOSE);
        });
    }

    @Test
    public void test_setLogo_displaysLogo() throws Throwable {
        runWithToolbar((toolbar) -> toolbar.setLogo(R.drawable.ic_launcher));

        onView(withDrawable(R.drawable.ic_launcher)).check(matches(isDisplayed()));
    }

    @Test
    public void pressBack_withoutListener_callsActivityOnBack() throws Throwable {
        runWithToolbar((toolbar) -> toolbar.setState(Toolbar.State.SUBPAGE));

        onView(withId(R.id.car_ui_toolbar_nav_icon_container)).perform(click());

        assertEquals(1, mActivityRule.getActivity().getTimesOnBackPressed());
    }

    @Test
    public void pressBack_withListenerThatReturnsFalse_callsActivityOnBack() throws Throwable {
        runWithToolbar((toolbar) -> {
            toolbar.setState(Toolbar.State.SUBPAGE);
            toolbar.registerOnBackListener(() -> false);
        });

        onView(withId(R.id.car_ui_toolbar_nav_icon_container)).perform(click());

        assertEquals(1, mActivityRule.getActivity().getTimesOnBackPressed());
    }

    @Test
    public void pressBack_withListenerThatReturnsTrue_doesntCallActivityOnBack() throws Throwable {
        runWithToolbar((toolbar) -> {
            toolbar.setState(Toolbar.State.SUBPAGE);
            toolbar.registerOnBackListener(() -> true);
        });

        onView(withId(R.id.car_ui_toolbar_nav_icon_container)).perform(click());

        assertEquals(0, mActivityRule.getActivity().getTimesOnBackPressed());
    }

    @Test
    public void pressBack_withUnregisteredListener_doesntCallActivityOnBack() throws Throwable {
        runWithToolbar((toolbar) -> {
            toolbar.setState(Toolbar.State.SUBPAGE);
            Toolbar.OnBackListener listener = () -> true;
            toolbar.registerOnBackListener(listener);
            toolbar.registerOnBackListener(listener);
            toolbar.unregisterOnBackListener(listener);
        });

        onView(withId(R.id.car_ui_toolbar_nav_icon_container)).perform(click());

        assertEquals(1, mActivityRule.getActivity().getTimesOnBackPressed());
    }

    @Test
    public void menuItems_setId_shouldWork() {
        MenuItem item = MenuItem.builder(mActivityRule.getActivity()).build();

        assertThat(item.getId()).isEqualTo(View.NO_ID);

        item.setId(7);

        assertThat(item.getId()).isEqualTo(7);
    }

    @Test
    public void menuItems_whenClicked_shouldCallListener() throws Throwable {
        MenuItem.OnClickListener callback = mock(MenuItem.OnClickListener.class);
        MenuItem menuItem = MenuItem.builder(mActivityRule.getActivity())
                .setTitle("Button!")
                .setOnClickListener(callback)
                .build();
        runWithToolbar((toolbar) -> toolbar.setMenuItems(Collections.singletonList(menuItem)));

        waitForMenuItems();

        onView(firstMenuItem()).perform(click());

        verify(callback).onClick(menuItem);
    }

    @Test
    public void menuItems_null_shouldRemoveExistingMenuItems() throws Throwable {
        runWithToolbar((toolbar) ->
                toolbar.setMenuItems(Arrays.asList(
                        MenuItem.builder(mActivityRule.getActivity())
                                .setTitle("Button!")
                                .build(),
                        MenuItem.builder(mActivityRule.getActivity())
                                .setTitle("Button2!")
                                .build()
                )));
        waitForMenuItems();

        onView(withId(R.id.car_ui_toolbar_menu_items_container)).check(matches(hasChildCount(2)));

        runWithToolbar((toolbar) -> toolbar.setMenuItems(null));

        onView(withId(R.id.car_ui_toolbar_menu_items_container)).check(matches(hasChildCount(0)));
    }

    @Test
    public void menuItems_setVisibility_shouldHide() throws Throwable {
        MenuItem menuItem = MenuItem.builder(mActivityRule.getActivity())
                .setTitle("Button!")
                .build();
        runWithToolbar((toolbar) -> toolbar.setMenuItems(Collections.singletonList(menuItem)));
        waitForMenuItems();

        onView(withText("Button!")).check(matches(isDisplayed()));

        runWithToolbar((toolbar) -> menuItem.setVisible(false));

        onView(withText("Button!")).check(matches(not(isDisplayed())));
    }

    @Test
    public void menuItems_searchScreen_shouldHideMenuItems() throws Throwable {
        runWithToolbar((toolbar) -> {
            toolbar.setMenuItems(Arrays.asList(
                    MenuItem.builder(mActivityRule.getActivity())
                            .setToSearch()
                            .build(),
                    MenuItem.builder(mActivityRule.getActivity())
                            .setTitle("Button!")
                            .build()));
            toolbar.setShowMenuItemsWhileSearching(false);
            toolbar.setState(Toolbar.State.SEARCH);
        });
        waitForMenuItems();

        // All menuitems should be hidden if we're hiding menuitems while searching
        onView(withText("Button!")).check(matches(not(isDisplayed())));
        onView(firstMenuItem()).check(matches(not(isDisplayed())));

        runWithToolbar((toolbar) -> toolbar.setShowMenuItemsWhileSearching(true));

        // Even if not hiding MenuItems while searching, the search MenuItem should still be hidden
        onView(withText("Button!")).check(matches(isDisplayed()));
        onView(firstMenuItem()).check(matches(not(isDisplayed())));
    }

    private void runWithToolbar(Consumer<ToolbarController> toRun) throws Throwable {
        mActivityRule.runOnUiThread(() -> {
            ToolbarController toolbar = CarUi.requireToolbar(mActivityRule.getActivity());
            toRun.accept(toolbar);
        });
    }

    private Matcher<View> firstMenuItem() {
        return nthChildOfView(withId(R.id.car_ui_toolbar_menu_items_container), 0);
    }

    private void waitForMenuItems() {
        onView(isRoot()).perform(waitForView(firstMenuItem(), 500));
    }
}
