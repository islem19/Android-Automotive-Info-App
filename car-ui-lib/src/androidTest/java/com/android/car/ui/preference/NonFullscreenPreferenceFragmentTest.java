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

package com.android.car.ui.preference;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static com.android.car.ui.matchers.ViewMatchers.withPadding;
import static com.android.car.ui.matchers.ViewMatchers.withPaddingAtLeast;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.car.ui.baselayout.Insets;
import com.android.car.ui.baselayout.InsetsChangedListener;
import com.android.car.ui.core.CarUi;
import com.android.car.ui.matchers.PaddingMatcher.Side;
import com.android.car.ui.toolbar.ToolbarController;

import org.junit.Rule;
import org.junit.Test;

public class NonFullscreenPreferenceFragmentTest {

    private static final String EXTRA_FULLSCREEN = "fullscreen";
    private static final String TOOLBAR_DEFAULT_TEXT = "Test!";
    private static final String PREFERENCE_SCREEN_TITLE = "PreferenceScreen Title";
    private static final String LIST_PREFERENCE_TITLE = "List Preference";
    private static final String MULTI_SELECT_LIST_PREFERENCE_TITLE = "MultiSelect List Preference";
    private static final String BACK_CONTENT_DESCRIPTION = "Back";
    private static final String[] ITEMS = { "Item 1", "Item 2", "Item 3" };

    @Rule
    public ActivityScenarioRule<PreferenceTestActivity> mActivityRule =
            new ActivityScenarioRule<>(PreferenceTestActivity.class);

    @Test
    public void test_fullscreen_changesTitle() {
        try (ActivityScenario<MyActivity> scenario =
                     ActivityScenario.launch(MyActivity.newIntent(true))) {

            onView(withText(TOOLBAR_DEFAULT_TEXT)).check(doesNotExist());
            onView(withText(PREFERENCE_SCREEN_TITLE)).check(matches(isDisplayed()));
            onView(isAssignableFrom(RecyclerView.class)).check(
                    matches(withPaddingAtLeast(Side.TOP, 1)));

            onView(withText(MULTI_SELECT_LIST_PREFERENCE_TITLE)).perform(click());
            onView(withText(MULTI_SELECT_LIST_PREFERENCE_TITLE)).check(matches(isDisplayed()));
            onView(withText(ITEMS[0])).check(matches(isDisplayed()));
            onView(isAssignableFrom(RecyclerView.class)).check(
                    matches(withPaddingAtLeast(Side.TOP, 1)));
            onView(withContentDescription(BACK_CONTENT_DESCRIPTION)).perform(click());

            onView(withText(LIST_PREFERENCE_TITLE)).perform(click());
            onView(withText(LIST_PREFERENCE_TITLE)).check(matches(isDisplayed()));
            onView(withText(ITEMS[0])).check(matches(isDisplayed()));
            onView(isAssignableFrom(RecyclerView.class)).check(
                    matches(withPaddingAtLeast(Side.TOP, 1)));
            onView(withContentDescription(BACK_CONTENT_DESCRIPTION)).perform(click());
        }
    }

    @Test
    public void test_nonFullscreen_doesntChangeTitle() {
        try (ActivityScenario<MyActivity> scenario =
                     ActivityScenario.launch(MyActivity.newIntent(false))) {

            onView(withText(TOOLBAR_DEFAULT_TEXT)).check(matches(isDisplayed()));
            onView(withText(PREFERENCE_SCREEN_TITLE)).check(doesNotExist());
            onView(isAssignableFrom(RecyclerView.class)).check(matches(withPadding(Side.TOP, 0)));

            onView(withText(MULTI_SELECT_LIST_PREFERENCE_TITLE)).perform(click());
            onView(withText(MULTI_SELECT_LIST_PREFERENCE_TITLE)).check(doesNotExist());
            onView(withText(TOOLBAR_DEFAULT_TEXT)).check(matches(isDisplayed()));
            onView(withText(ITEMS[0])).check(matches(isDisplayed()));
            onView(isAssignableFrom(RecyclerView.class)).check(matches(withPadding(Side.TOP, 0)));
            onView(withContentDescription(BACK_CONTENT_DESCRIPTION)).check(doesNotExist());
            pressBack();

            onView(withText(LIST_PREFERENCE_TITLE)).perform(click());
            onView(withText(LIST_PREFERENCE_TITLE)).check(doesNotExist());
            onView(withText(TOOLBAR_DEFAULT_TEXT)).check(matches(isDisplayed()));
            onView(withText(ITEMS[0])).check(matches(isDisplayed()));
            onView(isAssignableFrom(RecyclerView.class)).check(matches(withPadding(Side.TOP, 0)));
            onView(withContentDescription(BACK_CONTENT_DESCRIPTION)).check(doesNotExist());
            pressBack();
        }
    }


    public static class MyActivity extends AppCompatActivity implements InsetsChangedListener {

        private boolean mIsFullScreen = false;

        public static Intent newIntent(boolean isFullScreen) {
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            Intent intent = new Intent(context, MyActivity.class);
            intent.putExtra(EXTRA_FULLSCREEN, isFullScreen);
            return intent;
        }

        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            ToolbarController toolbar = CarUi.requireToolbar(this);
            toolbar.setTitle(TOOLBAR_DEFAULT_TEXT);

            mIsFullScreen = getIntent().getBooleanExtra(EXTRA_FULLSCREEN, true);
            if (savedInstanceState == null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(android.R.id.content, new MyPreferenceFragment(mIsFullScreen))
                        .commitNow();
            }
        }

        @Override
        public void onCarUiInsetsChanged(@NonNull Insets insets) {
            if (!mIsFullScreen) {
                requireViewById(android.R.id.content).setPadding(insets.getLeft(), insets.getTop(),
                        insets.getRight(), insets.getBottom());
            } else {
                // No-op marker for the preference fragment to handle it
            }
        }
    }

    public static class MyPreferenceFragment extends PreferenceFragment {

        private final boolean mIsFullScreen;

        public MyPreferenceFragment(boolean isFullScreen) {
            mIsFullScreen = isFullScreen;
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            PreferenceScreen screen = getPreferenceManager()
                    .createPreferenceScreen(requireContext());

            ListPreference listPreference = new CarUiListPreference(getContext());
            listPreference.setTitle(LIST_PREFERENCE_TITLE);
            listPreference.setKey(LIST_PREFERENCE_TITLE);
            listPreference.setEntries(ITEMS);
            listPreference.setEntryValues(new CharSequence[]{"1", "2", "3"});

            MultiSelectListPreference multiSelectListPreference =
                    new CarUiMultiSelectListPreference(getContext());
            multiSelectListPreference.setTitle(MULTI_SELECT_LIST_PREFERENCE_TITLE);
            multiSelectListPreference.setKey(MULTI_SELECT_LIST_PREFERENCE_TITLE);
            multiSelectListPreference.setEntries(ITEMS);
            multiSelectListPreference.setEntryValues(new CharSequence[]{"1", "2", "3"});

            screen.addPreference(listPreference);
            screen.addPreference(multiSelectListPreference);

            screen.setTitle(PREFERENCE_SCREEN_TITLE);
            setPreferenceScreen(screen);
        }

        @Override
        protected boolean isFullScreenFragment() {
            return mIsFullScreen;
        }
    }
}
