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

import static android.view.accessibility.AccessibilityNodeInfo.ACTION_FOCUS;

import static com.android.car.ui.utils.RotaryConstants.ACTION_RESTORE_DEFAULT_FOCUS;

import static com.google.common.truth.Truth.assertThat;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.rule.ActivityTestRule;

import com.android.car.ui.recyclerview.TestContentLimitingAdapter;
import com.android.car.ui.test.R;
import com.android.car.ui.utils.CarUiUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/** Unit test for {@link FocusParkingView} not in touch mode. */
public class FocusParkingViewTest {

    private static final int NUM_ITEMS = 40;

    @Rule
    public ActivityTestRule<FocusParkingViewTestActivity> mActivityRule =
            new ActivityTestRule<>(FocusParkingViewTestActivity.class);

    private FocusParkingViewTestActivity mActivity;
    private FocusParkingView mFpv;
    private ViewGroup mParent1;
    private View mView1;
    private View mFocusedByDefault;
    private RecyclerView mList;

    @Before
    public void setUp() {
        mActivity = mActivityRule.getActivity();
        mFpv = mActivity.findViewById(R.id.fpv);
        mParent1 = mActivity.findViewById(R.id.parent1);
        mView1 = mActivity.findViewById(R.id.view1);
        mFocusedByDefault = mActivity.findViewById(R.id.focused_by_default);
        mList = mActivity.findViewById(R.id.list);

        mList.post(() -> {
            mList.setLayoutManager(new LinearLayoutManager(mActivity));
            mList.setAdapter(new TestContentLimitingAdapter(NUM_ITEMS));
            CarUiUtils.setRotaryScrollEnabled(mList, /* isVertical= */ true);
        });
    }

    @Test
    public void testGetWidthAndHeight() {
        assertThat(mFpv.getWidth()).isEqualTo(1);
        assertThat(mFpv.getHeight()).isEqualTo(1);
    }

    @Test
    public void testRequestFocus_focusOnDefaultFocus() {
        mFpv.post(() -> {
            mView1.requestFocus();
            assertThat(mView1.isFocused()).isTrue();

            mFpv.requestFocus();
            assertThat(mFocusedByDefault.isFocused()).isTrue();
        });
    }

    @Test
    public void testRestoreDefaultFocus_focusOnDefaultFocus() {
        mFpv.post(() -> {
            mView1.requestFocus();
            assertThat(mView1.isFocused()).isTrue();

            mFpv.restoreDefaultFocus();
            assertThat(mFocusedByDefault.isFocused()).isTrue();
        });
    }

    @Test
    public void testOnWindowFocusChanged_loseFocus() {
        mFpv.post(() -> {
            mView1.requestFocus();
            assertThat(mView1.isFocused()).isTrue();

            mFpv.onWindowFocusChanged(false);
            assertThat(mFpv.isFocused()).isTrue();
        });
    }

    @Test
    public void testOnWindowFocusChanged_focusOnDefaultFocus() {
        mFpv.post(() -> {
            mFpv.performAccessibilityAction(ACTION_FOCUS, null);
            assertThat(mFpv.isFocused()).isTrue();

            mFpv.onWindowFocusChanged(true);
            assertThat(mFocusedByDefault.isFocused()).isTrue();
        });
    }

    @Test
    public void testPerformAccessibilityAction_actionRestoreDefaultFocus() {
        mFpv.post(() -> {
            mView1.requestFocus();
            assertThat(mView1.isFocused()).isTrue();

            mFpv.performAccessibilityAction(ACTION_RESTORE_DEFAULT_FOCUS, null);
            assertThat(mFocusedByDefault.isFocused()).isTrue();
        });
    }

    @Test
    public void testPerformAccessibilityAction_actionFocus() {
        mFpv.post(() -> {
            mView1.requestFocus();
            assertThat(mView1.isFocused()).isTrue();

            mFpv.performAccessibilityAction(ACTION_FOCUS, null);
            assertThat(mFpv.isFocused()).isTrue();
        });
    }

    @Test
    public void testRestoreFocusInRoot_recyclerViewItemRemoved() {
        mList.post(() -> mList.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        mList.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        View firstItem = mList.getLayoutManager().findViewByPosition(0);
                        firstItem.requestFocus();
                        assertThat(firstItem.isFocused()).isTrue();

                        ViewGroup parent = (ViewGroup) firstItem.getParent();
                        parent.removeView(firstItem);
                        assertThat(mFocusedByDefault.isFocused()).isTrue();
                    }
                })
        );
    }

    @Test
    public void testRestoreFocusInRoot_recyclerViewItemScrolledOffScreen() {
        mList.post(() -> mList.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        mList.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        View firstItem = mList.getLayoutManager().findViewByPosition(0);
                        firstItem.requestFocus();
                        assertThat(firstItem.isFocused()).isTrue();

                        mList.scrollToPosition(NUM_ITEMS - 1);
                        mList.getViewTreeObserver().addOnGlobalLayoutListener(
                                new ViewTreeObserver.OnGlobalLayoutListener() {
                                    @Override
                                    public void onGlobalLayout() {
                                        mList.getViewTreeObserver()
                                                .removeOnGlobalLayoutListener(this);
                                        assertThat(mList.isFocused()).isTrue();
                                    }
                                });
                    }
                }));
    }

    @Test
    public void testRestoreFocusInRoot_focusedViewRemoved() {
        mFpv.post(() -> {
            mView1.requestFocus();
            assertThat(mView1.isFocused()).isTrue();

            ViewGroup parent = (ViewGroup) mView1.getParent();
            parent.removeView(mView1);
            assertThat(mFocusedByDefault.isFocused()).isTrue();
        });
    }

    @Test
    public void testRestoreFocusInRoot_focusedViewDisabled() {
        mFpv.post(() -> {
            mView1.requestFocus();
            assertThat(mView1.isFocused()).isTrue();

            mView1.setEnabled(false);
            assertThat(mFocusedByDefault.isFocused()).isTrue();
        });
    }

    @Test
    public void testRestoreFocusInRoot_focusedViewBecomesInvisible() {
        mFpv.post(() -> {
            mView1.requestFocus();
            assertThat(mView1.isFocused()).isTrue();

            mView1.setVisibility(View.INVISIBLE);
            assertThat(mFocusedByDefault.isFocused()).isTrue();
        });
    }

    @Test
    public void testRestoreFocusInRoot_focusedViewParentBecomesInvisible() {
        mFpv.post(() -> {
            mView1.requestFocus();
            assertThat(mView1.isFocused()).isTrue();

            mParent1.setVisibility(View.INVISIBLE);
            assertThat(mFocusedByDefault.isFocused()).isTrue();
        });
    }

    @Test
    public void testRequestFocus_focusesFpvWhenShouldRestoreFocusIsFalse() {
        mFpv.post(() -> {
            mView1.requestFocus();
            assertThat(mView1.isFocused()).isTrue();
            mFpv.setShouldRestoreFocus(false);

            mFpv.requestFocus();
            assertThat(mFpv.isFocused()).isTrue();
        });
    }

    @Test
    public void testRestoreDefaultFocus_focusesFpvWhenShouldRestoreFocusIsFalse() {
        mFpv.post(() -> {
            mView1.requestFocus();
            assertThat(mView1.isFocused()).isTrue();
            mFpv.setShouldRestoreFocus(false);

            mFpv.restoreDefaultFocus();
            assertThat(mFpv.isFocused()).isTrue();
        });
    }
}
