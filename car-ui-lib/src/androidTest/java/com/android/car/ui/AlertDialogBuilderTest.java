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

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.app.AlertDialog;
import android.database.Cursor;
import android.view.View;

import androidx.test.espresso.Root;
import androidx.test.rule.ActivityTestRule;

import com.android.car.ui.recyclerview.CarUiContentListItem;
import com.android.car.ui.recyclerview.CarUiListItemAdapter;
import com.android.car.ui.recyclerview.CarUiRadioButtonListItem;
import com.android.car.ui.recyclerview.CarUiRadioButtonListItemAdapter;
import com.android.car.ui.test.R;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class AlertDialogBuilderTest {

    @Rule
    public ActivityTestRule<TestActivity> mActivityRule =
            new ActivityTestRule<>(TestActivity.class);

    @Test
    public void test_AlertDialogBuilder_works() throws Throwable {
        String title = "Test message from AlertDialogBuilder";
        String subtitle = "Subtitle from AlertDialogBuilder";
        mActivityRule.runOnUiThread(() ->
                new AlertDialogBuilder(mActivityRule.getActivity())
                        .setMessage(title)
                        .setSubtitle(subtitle)
                        .show());

        AlertDialog dialog = checkDefaultButtonExists(true,
                new AlertDialogBuilder(mActivityRule.getActivity())
                        .setMessage(title)
                        .setSubtitle(subtitle));
        onView(withText(title))
                .inRoot(new RootWithDecorMatcher(dialog.getWindow().getDecorView()))
                .check(matches(isDisplayed()));
        onView(withText(subtitle))
                .inRoot(new RootWithDecorMatcher(dialog.getWindow().getDecorView()))
                .check(matches(isDisplayed()));
    }

    @Test
    public void test_showSingleListChoiceItem_StringArray_hidesDefaultButton() throws Throwable {
        AlertDialogBuilder builder = new AlertDialogBuilder(mActivityRule.getActivity())
                .setAllowDismissButton(false)
                .setSingleChoiceItems(new CharSequence[]{"Item 1", "Item 2"}, 0,
                        ((dialog, which) -> {
                        }));

        checkDefaultButtonExists(false, builder);
    }

    @Test
    public void test_showSingleListChoiceItem_StringArrayResource_hidesDefaultButton()
            throws Throwable {
        AlertDialogBuilder builder = new AlertDialogBuilder(mActivityRule.getActivity())
                .setAllowDismissButton(false)
                .setSingleChoiceItems(R.array.test_string_array, 0, ((dialog, which) -> {
                }));

        checkDefaultButtonExists(false, builder);
    }

    @Test
    public void test_showSingleListChoiceItem_CarUiRadioButtonListItemAdapter_forcesDefaultButton()
            throws Throwable {
        CarUiRadioButtonListItem item1 = new CarUiRadioButtonListItem();
        item1.setTitle("Item 1");
        CarUiRadioButtonListItem item2 = new CarUiRadioButtonListItem();
        item2.setTitle("Item 2");
        CarUiRadioButtonListItem item3 = new CarUiRadioButtonListItem();
        item3.setTitle("Item 3");

        CarUiRadioButtonListItemAdapter adapter = new CarUiRadioButtonListItemAdapter(
                Arrays.asList(item1, item2, item3));
        AlertDialogBuilder builder = new AlertDialogBuilder(mActivityRule.getActivity())
                .setAllowDismissButton(false)
                .setSingleChoiceItems(adapter);

        checkDefaultButtonExists(true, builder);
    }

    @Test
    public void test_showSingleListChoiceItem_cursor_hidesDefaultButton() throws Throwable {
        Cursor cursor = new FakeCursor(Arrays.asList("Item 1", "Item 2"), "ColumnName");
        AlertDialogBuilder builder = new AlertDialogBuilder(mActivityRule.getActivity())
                .setTitle("Title")
                .setAllowDismissButton(false)
                .setSingleChoiceItems(cursor, 0, "ColumnName", ((dialog, which) -> {
                }));

        checkDefaultButtonExists(false, builder);
    }

    @Test
    public void test_setItems_StringArrayResource_hidesDefaultButton() throws Throwable {
        AlertDialogBuilder builder = new AlertDialogBuilder(mActivityRule.getActivity())
                .setAllowDismissButton(false)
                .setItems(R.array.test_string_array, ((dialog, which) -> {
                }));

        checkDefaultButtonExists(false, builder);
    }

    @Test
    public void test_setItems_StringArray_hidesDefaultButton() throws Throwable {
        AlertDialogBuilder builder = new AlertDialogBuilder(mActivityRule.getActivity())
                .setAllowDismissButton(false)
                .setItems(new CharSequence[]{"Item 1", "Item 2"}, ((dialog, which) -> {
                }));

        checkDefaultButtonExists(false, builder);
    }

    @Test
    public void test_setAdapter_hidesDefaultButton()
            throws Throwable {
        CarUiContentListItem item1 = new CarUiContentListItem(CarUiContentListItem.Action.NONE);
        item1.setTitle("Item 1");
        CarUiContentListItem item2 = new CarUiContentListItem(CarUiContentListItem.Action.NONE);
        item2.setTitle("Item 2");
        CarUiContentListItem item3 = new CarUiContentListItem(CarUiContentListItem.Action.NONE);
        item3.setTitle("Item 3");

        CarUiListItemAdapter adapter = new CarUiListItemAdapter(
                Arrays.asList(item1, item2, item3));
        AlertDialogBuilder builder = new AlertDialogBuilder(mActivityRule.getActivity())
                .setAllowDismissButton(false)
                .setAdapter(adapter);

        checkDefaultButtonExists(false, builder);
    }

    @Test
    public void test_multichoiceItems_StringArrayResource_forcesDefaultButton()
            throws Throwable {
        AlertDialogBuilder builder = new AlertDialogBuilder(mActivityRule.getActivity())
                .setAllowDismissButton(false)
                .setMultiChoiceItems(R.array.test_string_array, null,
                        ((dialog, which, isChecked) -> {
                        }));

        checkDefaultButtonExists(true, builder);
    }

    @Test
    public void test_multichoiceItems_StringArray_forcesDefaultButton()
            throws Throwable {
        AlertDialogBuilder builder = new AlertDialogBuilder(mActivityRule.getActivity())
                .setAllowDismissButton(false)
                .setMultiChoiceItems(new CharSequence[]{"Test 1", "Test 2"}, null,
                        ((dialog, which, isChecked) -> {
                        }));

        checkDefaultButtonExists(true, builder);
    }

    @Test
    public void test_multichoiceItems_Cursor_forcesDefaultButton()
            throws Throwable {
        Cursor cursor = new FakeCursor(Arrays.asList("Item 1", "Item 2"), "Label");
        AlertDialogBuilder builder = new AlertDialogBuilder(mActivityRule.getActivity())
                .setAllowDismissButton(false)
                .setMultiChoiceItems(cursor, "isChecked", "Label",
                        ((dialog, which, isChecked) -> {
                        }));

        checkDefaultButtonExists(true, builder);
    }

    private AlertDialog checkDefaultButtonExists(boolean shouldExist, AlertDialogBuilder builder)
            throws Throwable {
        AtomicBoolean done = new AtomicBoolean(false);
        AlertDialog[] result = new AlertDialog[1];
        mActivityRule.runOnUiThread(() -> {
            try {
                result[0] = builder.create();
                result[0].show();
            } catch (RuntimeException e) {
                assert e.getMessage() != null;
                assert e.getMessage().contains(
                        "must have at least one button to disable the dismiss button");

                assert shouldExist;
                done.set(true);
            }
        });

        if (done.get()) {
            return result[0];
        }

        if (shouldExist) {
            onView(withText(R.string.car_ui_alert_dialog_default_button))
                    .inRoot(new RootWithDecorMatcher(result[0].getWindow().getDecorView()))
                    .check(matches(isDisplayed()));
        } else {
            onView(withText(R.string.car_ui_alert_dialog_default_button))
                    .inRoot(new RootWithDecorMatcher(result[0].getWindow().getDecorView()))
                    .check(doesNotExist());
        }

        return result[0];
    }

    private static class RootWithDecorMatcher extends TypeSafeMatcher<Root> {

        private View mView;

        RootWithDecorMatcher(View view) {
            mView = view;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("is a root with a certain decor");
        }

        @Override
        protected boolean matchesSafely(Root item) {
            return item.getDecorView() == mView;
        }
    }
}
