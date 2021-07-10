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

/**
 * Constants for the rotary controller.
 *
 * @hide
 */
public final class RotaryConstants {
    /**
     * Content description indicating that the view is a rotary container.
     * <p>
     * A rotary container contains focusable elements. When initializing focus, the first element
     * in the rotary container is prioritized to take focus. When searching for nudge target, the
     * bounds of the rotary container is the minimum bounds containing its descendants.
     * <p>
     * A rotary container shouldn't be focusable unless it's a scrollable container. Though it
     * can't be focused, it can be scrolled as a side-effect of moving the focus within it.
     */
    public static final String ROTARY_CONTAINER =
            "com.android.car.ui.utils.ROTARY_CONTAINER";

    /**
     * Content description indicating that the view is a scrollable container and can be scrolled
     * horizontally by the rotary controller.
     * <p>
     * A scrollable container is a focusable rotary container. When it's focused, it can be scrolled
     * when the rotary controller rotates. A scrollable container is often used to show long text.
     */
    public static final String ROTARY_HORIZONTALLY_SCROLLABLE =
            "com.android.car.ui.utils.HORIZONTALLY_SCROLLABLE";

    /**
     * Content description indicating that the view is a scrollable container and can be scrolled
     * vertically by the rotary controller.
     * <p>
     * A scrollable container is a focusable rotary container. When it's focused, it can be scrolled
     * when the rotary controller rotates. A scrollable container is often used to show long text.
     */
    public static final String ROTARY_VERTICALLY_SCROLLABLE =
            "com.android.car.ui.utils.VERTICALLY_SCROLLABLE";

    /**
     * Content description indicating that the view is a focus delegating container. When
     * restoring focus, FocusParkingView and FocusArea will skip non-focusable views unless it's
     * a focus delegating container. The focus delegating container can delegate focus to one of
     * its descendants.
     */
    public static final String ROTARY_FOCUS_DELEGATING_CONTAINER =
            "com.android.car.ui.utils.FOCUS_DELEGATING_CONTAINER";

    /** The key to store the offset of the FocusArea's left bound in the node's extras. */
    public static final String FOCUS_AREA_LEFT_BOUND_OFFSET =
            "com.android.car.ui.utils.FOCUS_AREA_LEFT_BOUND_OFFSET";

    /** The key to store the offset of the FocusArea's right bound in the node's extras. */
    public static final String FOCUS_AREA_RIGHT_BOUND_OFFSET =
            "com.android.car.ui.utils.FOCUS_AREA_RIGHT_BOUND_OFFSET";

    /** The key to store the offset of the FocusArea's top bound in the node's extras. */
    public static final String FOCUS_AREA_TOP_BOUND_OFFSET =
            "com.android.car.ui.utils.FOCUS_AREA_TOP_BOUND_OFFSET";

    /** The key to store the offset of the FocusArea's bottom bound in the node's extras. */
    public static final String FOCUS_AREA_BOTTOM_BOUND_OFFSET =
            "com.android.car.ui.utils.FOCUS_AREA_BOTTOM_BOUND_OFFSET";

    /** The key to store nudge direction in the Bundle. */
    public static final String NUDGE_DIRECTION =
            "com.android.car.ui.utils.NUDGE_DIRECTION";

    /**
     * Action performed on a FocusArea to move focus to the nudge shortcut within the same
     * FocusArea.
     * <p>
     * This action and the actions below only use the most significant 8 bits to avoid
     * conflicting with legacy standard actions (which don't use the most significant 8 bits),
     * e.g. ACTION_FOCUS. The actions only use one bit to avoid conflicting with IDs defined in
     * framework (which start with 0x0102), e.g. R.id.accessibilityActionScrollUp.
     */
    public static final int ACTION_NUDGE_SHORTCUT = 0x01000000;

    /** Action performed on a FocusArea to move focus to another FocusArea. */
    public static final int ACTION_NUDGE_TO_ANOTHER_FOCUS_AREA = 0x02000000;

    /** Action performed on a FocusParkingView to restore the focus in the window. */
    public static final int ACTION_RESTORE_DEFAULT_FOCUS = 0x04000000;

    /** Action performed on a FocusParkingView to hide the IME. */
    public static final int ACTION_HIDE_IME = 0x08000000;

    /** Prevent instantiation. */
    private RotaryConstants() {
    }
}
