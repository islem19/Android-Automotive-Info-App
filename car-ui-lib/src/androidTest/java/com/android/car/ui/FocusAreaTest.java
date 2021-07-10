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

import static android.view.View.FOCUS_DOWN;
import static android.view.View.FOCUS_LEFT;
import static android.view.View.FOCUS_RIGHT;
import static android.view.View.FOCUS_UP;
import static android.view.View.LAYOUT_DIRECTION_LTR;
import static android.view.View.LAYOUT_DIRECTION_RTL;
import static android.view.accessibility.AccessibilityNodeInfo.ACTION_FOCUS;

import static com.android.car.ui.RotaryCache.CACHE_TYPE_DISABLED;
import static com.android.car.ui.RotaryCache.CACHE_TYPE_NEVER_EXPIRE;
import static com.android.car.ui.utils.RotaryConstants.ACTION_NUDGE_SHORTCUT;
import static com.android.car.ui.utils.RotaryConstants.ACTION_NUDGE_TO_ANOTHER_FOCUS_AREA;
import static com.android.car.ui.utils.RotaryConstants.FOCUS_AREA_BOTTOM_BOUND_OFFSET;
import static com.android.car.ui.utils.RotaryConstants.FOCUS_AREA_LEFT_BOUND_OFFSET;
import static com.android.car.ui.utils.RotaryConstants.FOCUS_AREA_RIGHT_BOUND_OFFSET;
import static com.android.car.ui.utils.RotaryConstants.FOCUS_AREA_TOP_BOUND_OFFSET;
import static com.android.car.ui.utils.RotaryConstants.NUDGE_DIRECTION;

import static com.google.common.truth.Truth.assertThat;

import android.os.Bundle;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.test.rule.ActivityTestRule;

import com.android.car.ui.test.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Unit tests for {@link FocusArea} not in touch mode. */
public class FocusAreaTest {
    private static final long WAIT_TIME_MS = 3000;

    @Rule
    public ActivityTestRule<FocusAreaTestActivity> mActivityRule =
            new ActivityTestRule<>(FocusAreaTestActivity.class);

    private FocusAreaTestActivity mActivity;
    private TestFocusArea mFocusArea1;
    private TestFocusArea mFocusArea2;
    private TestFocusArea mFocusArea3;
    private TestFocusArea mFocusArea4;
    private FocusParkingView mFpv;
    private View mView1;
    private Button mButton1;
    private View mView2;
    private View mDefaultFocus2;
    private View mView3;
    private View mNudgeShortcut3;
    private View mView4;

    @Before
    public void setUp() {
        mActivity = mActivityRule.getActivity();
        mFocusArea1 = mActivity.findViewById(R.id.focus_area1);
        mFocusArea2 = mActivity.findViewById(R.id.focus_area2);
        mFocusArea3 = mActivity.findViewById(R.id.focus_area3);
        mFocusArea4 = mActivity.findViewById(R.id.focus_area4);
        mFpv = mActivity.findViewById(R.id.fpv);
        mView1 = mActivity.findViewById(R.id.view1);
        mButton1 = mActivity.findViewById(R.id.button1);
        mView2 = mActivity.findViewById(R.id.view2);
        mDefaultFocus2 = mActivity.findViewById(R.id.default_focus2);
        mView3 = mActivity.findViewById(R.id.view3);
        mNudgeShortcut3 = mActivity.findViewById(R.id.nudge_shortcut3);
        mView4 = mActivity.findViewById(R.id.view4);
    }

    @Test
    public void testDrawMethodsCalled() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        mView1.post(() -> {
            mView1.requestFocus();
            mFocusArea1.enableForegroundHighlight();
            mFocusArea2.enableForegroundHighlight();
            mFocusArea1.setOnDrawCalled(false);
            mFocusArea1.setDrawCalled(false);
            mFocusArea2.setOnDrawCalled(false);
            mFocusArea2.setDrawCalled(false);

            mView2.requestFocus();
            mView2.post(() -> latch.countDown());
        });

        // The methods should be called when a FocusArea gains or loses focus.
        assertDrawMethodsCalled(mFocusArea1, latch);
        assertDrawMethodsCalled(mFocusArea2, latch);
    }

    @Test
    public void testPerformAccessibilityAction_actionNudgeShortcut() {
        mFocusArea1.post(() -> {
            // Nudge to the nudgeShortcut view.
            mView3.requestFocus();
            assertThat(mView3.isFocused()).isTrue();
            Bundle arguments = new Bundle();
            arguments.putInt(NUDGE_DIRECTION, FOCUS_RIGHT);
            mFocusArea3.performAccessibilityAction(ACTION_NUDGE_SHORTCUT, arguments);
            assertThat(mNudgeShortcut3.isFocused()).isTrue();

            // nudgeShortcutDirection doesn't match. The focus should stay the same.
            mView3.requestFocus();
            assertThat(mView3.isFocused()).isTrue();
            arguments.putInt(NUDGE_DIRECTION, FOCUS_DOWN);
            mFocusArea3.performAccessibilityAction(ACTION_NUDGE_SHORTCUT, arguments);
            assertThat(mView3.isFocused()).isTrue();

            // No nudgeShortcut view in the current FocusArea. The focus should stay the same.
            mView1.requestFocus();
            assertThat(mView1.isFocused()).isTrue();
            arguments.putInt(NUDGE_DIRECTION, FOCUS_RIGHT);
            mFocusArea1.performAccessibilityAction(ACTION_NUDGE_SHORTCUT, arguments);
            assertThat(mView1.isFocused()).isTrue();
        });
    }


    @Test
    public void testPerformAccessibilityAction_actionFocus() {
        mFocusArea1.post(() -> {
            mFocusArea1.performAccessibilityAction(ACTION_FOCUS, null);
            assertThat(mView1.isFocused()).isTrue();

            // It should focus on the default or the first view in the FocusArea.
            mFocusArea2.performAccessibilityAction(ACTION_FOCUS, null);
            assertThat(mDefaultFocus2.isFocused()).isTrue();
        });
    }

    @Test
    public void testPerformAccessibilityAction_actionFocus_enabledFocusCache() {
        mFocusArea1.post(() -> {
            RotaryCache cache =
                    new RotaryCache(CACHE_TYPE_NEVER_EXPIRE, 0, CACHE_TYPE_NEVER_EXPIRE, 0);
            mFocusArea1.setRotaryCache(cache);

            mButton1.requestFocus();
            assertThat(mButton1.isFocused()).isTrue();
            mView2.requestFocus();
            assertThat(mView2.isFocused()).isTrue();

            // With cache, it should focus on the lastly focused view in the FocusArea.
            mFocusArea1.performAccessibilityAction(ACTION_FOCUS, null);
            assertThat(mButton1.isFocused()).isTrue();
        });
    }

    @Test
    public void testPerformAccessibilityAction_actionFocus_disabledFocusCache() {
        mFocusArea1.post(() -> {
            RotaryCache cache = new RotaryCache(CACHE_TYPE_DISABLED, 0, CACHE_TYPE_NEVER_EXPIRE, 0);
            mFocusArea1.setRotaryCache(cache);

            mButton1.requestFocus();
            assertThat(mButton1.isFocused()).isTrue();
            mView2.requestFocus();
            assertThat(mView2.isFocused()).isTrue();

            // Without cache, it should focus on the default or the first view in the FocusArea.
            mFocusArea1.performAccessibilityAction(ACTION_FOCUS, null);
            assertThat(mView1.isFocused()).isTrue();
        });
    }

    @Test
    public void testPerformAccessibilityAction_actionFocus_lastFocusedViewRemoved() {
        mFocusArea1.post(() -> {
            // Focus on mDefaultFocus2 in mFocusArea2, then mView1 in mFocusArea21.
            mDefaultFocus2.requestFocus();
            assertThat(mDefaultFocus2.isFocused()).isTrue();
            mView1.requestFocus();
            assertThat(mView1.isFocused()).isTrue();

            // Remove mDefaultFocus2, then Perform ACTION_FOCUS on mFocusArea2.
            mFocusArea2.removeView(mDefaultFocus2);
            mFocusArea2.performAccessibilityAction(ACTION_FOCUS, null);

            // mView2 in mFocusArea2 should get focused.
            assertThat(mView2.isFocused()).isTrue();
        });
    }

    @Test
    public void testPerformAccessibilityAction_actionNudgeToAnotherFocusArea_enabledCache() {
        mFocusArea1.post(() -> {
            RotaryCache cache1 =
                    new RotaryCache(CACHE_TYPE_NEVER_EXPIRE, 0, CACHE_TYPE_NEVER_EXPIRE, 0);
            mFocusArea1.setRotaryCache(cache1);
            RotaryCache cache2 =
                    new RotaryCache(CACHE_TYPE_NEVER_EXPIRE, 0, CACHE_TYPE_NEVER_EXPIRE, 0);
            mFocusArea2.setRotaryCache(cache2);

            // Focus on the second view in mFocusArea1, then nudge to mFocusArea2.
            mButton1.requestFocus();
            assertThat(mButton1.isFocused()).isTrue();
            Bundle arguments = new Bundle();
            arguments.putInt(NUDGE_DIRECTION, FOCUS_DOWN);
            mFocusArea2.performAccessibilityAction(ACTION_FOCUS, arguments);
            assertThat(mDefaultFocus2.isFocused()).isTrue();

            // Nudge back. It should focus on the cached view (mButton1) in the cached
            // FocusArea (mFocusArea1).
            arguments.putInt(NUDGE_DIRECTION, FOCUS_UP);
            mFocusArea2.performAccessibilityAction(ACTION_NUDGE_TO_ANOTHER_FOCUS_AREA, arguments);
            assertThat(mButton1.isFocused()).isTrue();

            // Nudge back. It should fail and the focus should stay the same because of one-way
            // nudge history.
            arguments.putInt(NUDGE_DIRECTION, FOCUS_DOWN);
            mFocusArea1.performAccessibilityAction(ACTION_NUDGE_TO_ANOTHER_FOCUS_AREA, arguments);
            assertThat(mButton1.isFocused()).isTrue();
        });
    }

    @Test
    public void testPerformAccessibilityAction_actionNudgeToAnotherFocusArea_mixedCache() {
        mFocusArea1.post(() -> {
            // Disabled FocusCache but enabled FocusAreaCache.
            RotaryCache cache1 =
                    new RotaryCache(CACHE_TYPE_DISABLED, 0, CACHE_TYPE_NEVER_EXPIRE, 0);
            mFocusArea1.setRotaryCache(cache1);
            RotaryCache cache2 =
                    new RotaryCache(CACHE_TYPE_DISABLED, 0, CACHE_TYPE_NEVER_EXPIRE, 0);
            mFocusArea2.setRotaryCache(cache2);

            // Focus on the second view in mFocusArea1, then nudge to mFocusArea2.
            mButton1.requestFocus();
            assertThat(mButton1.isFocused()).isTrue();
            Bundle arguments = new Bundle();
            arguments.putInt(NUDGE_DIRECTION, FOCUS_DOWN);
            mFocusArea2.performAccessibilityAction(ACTION_FOCUS, arguments);
            assertThat(mDefaultFocus2.isFocused()).isTrue();

            // Nudge back. Since FocusCache is disabled, it should focus on the default or the first
            // view (mView1) in the cached FocusArea (mFocusArea1).
            arguments.putInt(NUDGE_DIRECTION, FOCUS_UP);
            mFocusArea2.performAccessibilityAction(ACTION_NUDGE_TO_ANOTHER_FOCUS_AREA, arguments);
            assertThat(mView1.isFocused()).isTrue();
        });
    }

    @Test
    public void testPerformAccessibilityAction_actionNudgeToAnotherFocusArea_mixedCache2() {
        mFocusArea1.post(() -> {
            // Enabled FocusCache but disabled FocusAreaCache.
            RotaryCache cache1 =
                    new RotaryCache(CACHE_TYPE_NEVER_EXPIRE, 0, CACHE_TYPE_DISABLED, 0);
            mFocusArea1.setRotaryCache(cache1);
            RotaryCache cache2 =
                    new RotaryCache(CACHE_TYPE_NEVER_EXPIRE, 0, CACHE_TYPE_DISABLED, 0);
            mFocusArea2.setRotaryCache(cache2);

            // Focus on the second view in mFocusArea1, then nudge to mFocusArea2.
            mButton1.requestFocus();
            assertThat(mButton1.isFocused()).isTrue();
            Bundle arguments = new Bundle();
            arguments.putInt(NUDGE_DIRECTION, FOCUS_DOWN);
            mFocusArea2.performAccessibilityAction(ACTION_FOCUS, arguments);
            assertThat(mDefaultFocus2.isFocused()).isTrue();

            // Nudge back. Since FocusAreaCache is disabled, nudge should fail and the focus should
            // stay the same.
            arguments.putInt(NUDGE_DIRECTION, FOCUS_UP);
            mFocusArea2.performAccessibilityAction(ACTION_NUDGE_TO_ANOTHER_FOCUS_AREA, arguments);
            assertThat(mDefaultFocus2.isFocused()).isTrue();
        });
    }

    @Test
    public void testPerformAccessibilityAction_actionNudgeToAnotherFocusArea_specifiedTarget() {
        mFocusArea1.post(() -> {
            // Nudge to specified FocusArea.
            mView4.requestFocus();
            assertThat(mView4.isFocused()).isTrue();
            Bundle arguments = new Bundle();
            arguments.putInt(NUDGE_DIRECTION, FOCUS_LEFT);
            mFocusArea4.performAccessibilityAction(ACTION_NUDGE_TO_ANOTHER_FOCUS_AREA, arguments);
            assertThat(mDefaultFocus2.isFocused()).isTrue();

            // Direction doesn't match specified FocusArea. The focus should stay the same.
            mView4.requestFocus();
            assertThat(mView4.isFocused()).isTrue();
            arguments.putInt(NUDGE_DIRECTION, FOCUS_UP);
            mFocusArea4.performAccessibilityAction(ACTION_NUDGE_TO_ANOTHER_FOCUS_AREA, arguments);
            assertThat(mView4.isFocused()).isTrue();

            // The FocusArea doesn't specify a target FocusArea. The focus should stay the same.
            mView1.requestFocus();
            assertThat(mView1.isFocused()).isTrue();
            arguments.putInt(NUDGE_DIRECTION, FOCUS_LEFT);
            mFocusArea1.performAccessibilityAction(ACTION_NUDGE_TO_ANOTHER_FOCUS_AREA, arguments);
            assertThat(mView1.isFocused()).isTrue();
        });
    }

    @Test
    public void testDefaultFocusOverridesHistory_override() {
        mFocusArea1.post(() -> {
            RotaryCache cache =
                    new RotaryCache(CACHE_TYPE_NEVER_EXPIRE, 0, CACHE_TYPE_NEVER_EXPIRE, 0);
            mFocusArea2.setRotaryCache(cache);
            mFocusArea2.setDefaultFocusOverridesHistory(true);

            mView2.requestFocus();
            assertThat(mView2.isFocused()).isTrue();
            mView1.requestFocus();
            assertThat(mView1.isFocused()).isTrue();

            // The focused view should be the default focus view rather than the cached view.
            mFocusArea2.performAccessibilityAction(ACTION_FOCUS, null);
            assertThat(mDefaultFocus2.isFocused()).isTrue();
        });
    }

    @Test
    public void testDefaultFocusOverridesHistory_notOverride() {
        mFocusArea1.post(() -> {
            RotaryCache cache =
                    new RotaryCache(CACHE_TYPE_NEVER_EXPIRE, 0, CACHE_TYPE_NEVER_EXPIRE, 0);
            mFocusArea2.setRotaryCache(cache);
            mFocusArea2.setDefaultFocusOverridesHistory(false);

            mView2.requestFocus();
            assertThat(mView2.isFocused()).isTrue();
            mView1.requestFocus();
            assertThat(mView1.isFocused()).isTrue();

            // The focused view should be the cached view rather than the default focus view.
            mFocusArea2.performAccessibilityAction(ACTION_FOCUS, null);
            assertThat(mView2.isFocused()).isTrue();
        });
    }

    @Test
    public void testClearFocusAreaHistoryWhenRotating_clear() {
        mFocusArea1.post(() -> {
            RotaryCache cache1 =
                    new RotaryCache(CACHE_TYPE_NEVER_EXPIRE, 0, CACHE_TYPE_NEVER_EXPIRE, 0);
            mFocusArea1.setRotaryCache(cache1);
            mFocusArea1.setClearFocusAreaHistoryWhenRotating(true);
            RotaryCache cache2 =
                    new RotaryCache(CACHE_TYPE_NEVER_EXPIRE, 0, CACHE_TYPE_NEVER_EXPIRE, 0);
            mFocusArea2.setRotaryCache(cache2);
            mFocusArea2.setClearFocusAreaHistoryWhenRotating(true);

            mView1.requestFocus();
            assertThat(mView1.isFocused()).isTrue();

            // Nudging down from mFocusArea1 to mFocusArea2.
            Bundle arguments = new Bundle();
            arguments.putInt(NUDGE_DIRECTION, FOCUS_DOWN);
            mFocusArea2.performAccessibilityAction(ACTION_FOCUS, arguments);
            assertThat(mDefaultFocus2.isFocused()).isTrue();
            // Rotate.
            mView2.requestFocus();
            assertThat(mView2.isFocused()).isTrue();
            // Since nudge history is cleared, nudging up should fail and the focus should stay
            // the same.
            arguments.putInt(NUDGE_DIRECTION, FOCUS_UP);
            mFocusArea2.performAccessibilityAction(ACTION_NUDGE_TO_ANOTHER_FOCUS_AREA, arguments);
            assertThat(mView2.isFocused()).isTrue();
        });
    }

    @Test
    public void testClearFocusAreaHistoryWhenRotating_notClear() {
        mFocusArea1.post(() -> {
            RotaryCache cache1 =
                    new RotaryCache(CACHE_TYPE_NEVER_EXPIRE, 0, CACHE_TYPE_NEVER_EXPIRE, 0);
            mFocusArea1.setRotaryCache(cache1);
            mFocusArea1.setClearFocusAreaHistoryWhenRotating(false);
            RotaryCache cache2 =
                    new RotaryCache(CACHE_TYPE_NEVER_EXPIRE, 0, CACHE_TYPE_NEVER_EXPIRE, 0);
            mFocusArea2.setRotaryCache(cache2);
            mFocusArea2.setClearFocusAreaHistoryWhenRotating(false);

            mView1.requestFocus();
            assertThat(mView1.isFocused()).isTrue();

            // Nudging down from mFocusArea1 to mFocusArea2.
            Bundle arguments = new Bundle();
            arguments.putInt(NUDGE_DIRECTION, FOCUS_DOWN);
            mFocusArea2.performAccessibilityAction(ACTION_FOCUS, arguments);
            assertThat(mDefaultFocus2.isFocused()).isTrue();
            // Rotate.
            mView2.requestFocus();
            assertThat(mView2.isFocused()).isTrue();
            // Nudging up should move focus to mFocusArea1 according to nudge history.
            arguments.putInt(NUDGE_DIRECTION, FOCUS_UP);
            mFocusArea2.performAccessibilityAction(ACTION_NUDGE_TO_ANOTHER_FOCUS_AREA, arguments);
            assertThat(mView1.isFocused()).isTrue();
        });
    }

    @Test
    public void testBoundsOffset() {
        assertThat(mFocusArea1.getLayoutDirection()).isEqualTo(LAYOUT_DIRECTION_LTR);

        // FocusArea's bounds offset specified in layout file:
        // 10dp(start), 20dp(end), 30dp(top), 40dp(bottom).
        int left = dp2Px(10);
        int right = dp2Px(20);
        int top = dp2Px(30);
        int bottom = dp2Px(40);
        AccessibilityNodeInfo node = mFocusArea1.createAccessibilityNodeInfo();
        assertBoundsOffset(node, left, top, right, bottom);
        node.recycle();
    }

    @Test
    public void testBoundsOffsetWithRtl() {
        mFocusArea1.post(() -> {
            mFocusArea1.setLayoutDirection(LAYOUT_DIRECTION_RTL);
            assertThat(mFocusArea1.getLayoutDirection()).isEqualTo(LAYOUT_DIRECTION_RTL);

            // FocusArea highlight padding specified in layout file:
            // 10dp(start), 20dp(end), 30dp(top), 40dp(bottom).
            int left = dp2Px(20);
            int right = dp2Px(10);
            int top = dp2Px(30);
            int bottom = dp2Px(40);
            AccessibilityNodeInfo node = mFocusArea1.createAccessibilityNodeInfo();
            assertBoundsOffset(node, left, top, right, bottom);
            node.recycle();
        });
    }

    @Test
    public void testSetBoundsOffset() {
        mFocusArea1.setBoundsOffset(50, 60, 70, 80);
        AccessibilityNodeInfo node = mFocusArea1.createAccessibilityNodeInfo();
        assertBoundsOffset(node, 50, 60, 70, 80);
        node.recycle();
    }

    @Test
    public void testHighlightPadding() {
        assertThat(mFocusArea2.getLayoutDirection()).isEqualTo(LAYOUT_DIRECTION_LTR);

        int left = dp2Px(50);
        int right = dp2Px(10);
        int top = dp2Px(40);
        int bottom = dp2Px(20);
        AccessibilityNodeInfo node = mFocusArea2.createAccessibilityNodeInfo();
        assertBoundsOffset(node, left, top, right, bottom);
        node.recycle();
    }

    @Test
    public void testBug170423337() {
        mFocusArea1.post(() -> {
            // Focus on app bar (assume mFocusArea1 is app bar).
            mView1.requestFocus();

            // Nudge down to browse list (assume mFocusArea2 is browse list).
            Bundle arguments = new Bundle();
            arguments.putInt(NUDGE_DIRECTION, FOCUS_DOWN);
            mFocusArea2.performAccessibilityAction(ACTION_FOCUS, arguments);
            assertThat(mDefaultFocus2.isFocused()).isTrue();

            // Nudge down to playback control bar (assume mFocusArea3 is playback control bar).
            mFocusArea3.performAccessibilityAction(ACTION_FOCUS, arguments);
            assertThat(mView3.isFocused()).isTrue();

            // Nudge down to navigation bar (navigation bar is in system window without FocusAreas).
            mFpv.performAccessibilityAction(ACTION_FOCUS, null);

            // Nudge up to playback control bar.
            arguments.putInt(NUDGE_DIRECTION, FOCUS_UP);
            mFocusArea3.performAccessibilityAction(ACTION_FOCUS, arguments);
            assertThat(mView3.isFocused()).isTrue();

            // Nudge up to browse list.
            arguments.putInt(NUDGE_DIRECTION, FOCUS_UP);
            mFocusArea3.performAccessibilityAction(ACTION_NUDGE_TO_ANOTHER_FOCUS_AREA, arguments);
            assertThat(mDefaultFocus2.isFocused()).isTrue();

            // Nudge up, and it should focus on app bar.
            mFocusArea2.performAccessibilityAction(ACTION_NUDGE_TO_ANOTHER_FOCUS_AREA, arguments);
            assertThat(mView1.isFocused()).isTrue();
        });
    }

    private void assertBoundsOffset(
            @NonNull AccessibilityNodeInfo node, int leftPx, int topPx, int rightPx, int bottomPx) {
        Bundle extras = node.getExtras();
        assertThat(extras.getInt(FOCUS_AREA_LEFT_BOUND_OFFSET)).isEqualTo(leftPx);
        assertThat(extras.getInt(FOCUS_AREA_RIGHT_BOUND_OFFSET)).isEqualTo(rightPx);
        assertThat(extras.getInt(FOCUS_AREA_TOP_BOUND_OFFSET)).isEqualTo(topPx);
        assertThat(extras.getInt(FOCUS_AREA_BOTTOM_BOUND_OFFSET)).isEqualTo(bottomPx);
    }

    /** Converts dp unit to equivalent pixels. */
    private int dp2Px(int dp) {
        return (int) (dp * mActivity.getResources().getDisplayMetrics().density + 0.5f);
    }

    private void assertDrawMethodsCalled(@NonNull TestFocusArea focusArea, CountDownLatch latch)
            throws Exception {
        latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS);
        assertThat(focusArea.onDrawCalled()).isTrue();
        assertThat(focusArea.drawCalled()).isTrue();
    }
}
