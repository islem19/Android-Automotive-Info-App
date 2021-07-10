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
package com.android.car.ui.utils;

import static android.os.Build.VERSION_CODES.R;

import android.annotation.TargetApi;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;

/** Helper class to toggle direct manipulation mode. */
public final class DirectManipulationHelper {

    /**
     * StateDescription for a {@link View} to support direct manipulation mode. It's also used as
     * class name of {@link AccessibilityEvent} to indicate that the AccessibilityEvent represents
     * a request to toggle direct manipulation mode.
     */
    private static final String DIRECT_MANIPULATION =
            "com.android.car.ui.utils.DIRECT_MANIPULATION";

    /** This is a utility class. */
    private DirectManipulationHelper() {
    }

    /**
     * Enables or disables direct manipulation mode. This method sends an {@link AccessibilityEvent}
     * to tell {@link com.android.car.rotary.RotaryService} to enter or exit direct manipulation
     * mode. Typically pressing the center button of the rotary controller with a direct
     * manipulation view focused will enter direct manipulation mode, while pressing the Back button
     * will exit direct manipulation mode.
     *
     * @param view   the direct manipulation view
     * @param enable true to enter direct manipulation mode, false to exit direct manipulation mode
     * @return whether the AccessibilityEvent was sent
     */
    public static boolean enableDirectManipulationMode(@NonNull View view, boolean enable) {
        AccessibilityManager accessibilityManager = (AccessibilityManager)
                view.getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (accessibilityManager == null || !accessibilityManager.isEnabled()) {
            return false;
        }
        AccessibilityEvent event = AccessibilityEvent.obtain();
        event.setClassName(DIRECT_MANIPULATION);
        event.setSource(view);
        event.setEventType(enable
                ? AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED
                : AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED);
        accessibilityManager.sendAccessibilityEvent(event);
        return true;
    }

    /** Returns whether the given {@code event} is for direct manipulation. */
    public static boolean isDirectManipulation(@NonNull AccessibilityEvent event) {
        return TextUtils.equals(DIRECT_MANIPULATION, event.getClassName());
    }

    /** Returns whether the given {@code node} supports rotate directly. */
    @TargetApi(R)
    public static boolean supportRotateDirectly(@NonNull AccessibilityNodeInfo node) {
        return TextUtils.equals(DIRECT_MANIPULATION, node.getStateDescription());
    }

    /**
     * Sets whether the given {@code view} supports rotate directly.
     * <p>
     * If the view supports rotate directly, when it's focused but not in direct manipulation mode,
     * clicking the center button of the rotary controller will make RotaryService enter direct
     * manipulation mode. In this mode, the view's selected state is toggled, and only controller
     * rotation and Back button press are supported.
     * <ul>
     *   <li>When the controller is rotated, the view will be asked to perform ACTION_SCROLL_FORWARD
     *       or ACTION_SCROLL_BACKWARD.
     *   <li>When Back button is pressed, RotaryService will toggle off the view's selected state
     *       and exit this mode.
     * </ul>
     * To support controller nudges as well in direct manipulation mode, use {@link
     * #enableDirectManipulationMode} instead.
     */
    @TargetApi(R)
    public static void setSupportsRotateDirectly(@NonNull View view, boolean enable) {
        view.setStateDescription(enable ? DIRECT_MANIPULATION : null);
    }

    /**
     * Returns whether the given {@code node} supports rotate directly.
     *
     * @deprecated use {@link #supportRotateDirectly} instead
     */
    @Deprecated
    public static boolean supportDirectManipulation(@NonNull AccessibilityNodeInfo node) {
        return supportRotateDirectly(node);
    }

    /**
     * Sets whether the given {@code view} supports rotate directly.
     *
     * @deprecated use {@link #setSupportsRotateDirectly} instead
     */
    @Deprecated
    public static void setSupportsDirectManipulation(@NonNull View view, boolean enable) {
        setSupportsRotateDirectly(view, enable);
    }
}
