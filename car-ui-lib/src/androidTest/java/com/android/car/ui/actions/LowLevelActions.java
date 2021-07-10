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

package com.android.car.ui.actions;

import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayingAtLeast;

import android.view.MotionEvent;
import android.view.View;

import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.action.GeneralLocation;
import androidx.test.espresso.action.MotionEvents;
import androidx.test.espresso.action.Press;

import org.hamcrest.Matcher;

public class LowLevelActions {
    static MotionEvent sMotionEventDownHeldView = null;

    public static PressAndHoldAction pressAndHold() {
        return new PressAndHoldAction();
    }

    public static ReleaseAction release() {
        return new ReleaseAction();
    }

    public static void tearDown() {
        sMotionEventDownHeldView = null;
    }

    static class PressAndHoldAction implements ViewAction {
        @Override
        public Matcher<View> getConstraints() {
            return isDisplayingAtLeast(90);
        }

        @Override
        public String getDescription() {
            return "Press and hold action";
        }

        @Override
        public void perform(final UiController uiController, final View view) {
            if (sMotionEventDownHeldView != null) {
                throw new AssertionError("Only one view can be held at a time");
            }

            float[] precision = Press.FINGER.describePrecision();
            float[] coords = GeneralLocation.CENTER.calculateCoordinates(view);
            sMotionEventDownHeldView = MotionEvents.sendDown(uiController, coords, precision).down;
        }
    }

    static class ReleaseAction implements ViewAction {
        @Override
        public Matcher<View> getConstraints() {
            return isDisplayingAtLeast(90);
        }

        @Override
        public String getDescription() {
            return "Release action";
        }

        @Override
        public void perform(final UiController uiController, final View view) {
            if (sMotionEventDownHeldView == null) {
                throw new AssertionError(
                        "Before calling release(), you must call pressAndHold() on a view");
            }

            float[] coords = GeneralLocation.CENTER.calculateCoordinates(view);
            MotionEvents.sendUp(uiController, sMotionEventDownHeldView, coords);
        }
    }

    public static ViewAction touchDownAndUp(final float x, final float y) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isDisplayed();
            }

            @Override
            public String getDescription() {
                return "Send touch events.";
            }

            @Override
            public void perform(UiController uiController, final View view) {
                // Get view absolute position
                int[] location = new int[2];
                view.getLocationOnScreen(location);

                // Offset coordinates by view position
                float[] coordinates = new float[]{x + location[0], y + location[1]};
                float[] precision = new float[]{1f, 1f};

                // Send down event, pause, and send up
                MotionEvent down = MotionEvents.sendDown(uiController, coordinates, precision).down;
                uiController.loopMainThreadForAtLeast(200);
                MotionEvents.sendUp(uiController, down, coordinates);
            }
        };
    }

    /**
     * Performs the down, move and up touch actions for the given coordinates. deltaX and deltaY
     * coordinates are used for moving the view from the downX and downY position. The interval
     * defines the number of time the value should be increased via delta. Also, limitY or limitX
     * can be provided to stop the movement if that value is reached.
     */
    public static ViewAction performDrag(final float downX, final float downY,
            final float deltaX, final float deltaY, final float interval, final float limitX,
            final float limitY) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isDisplayed();
            }

            @Override
            public String getDescription() {
                return "Send touch events.";
            }

            @Override
            public void perform(UiController uiController, final View view) {
                // Get view absolute position
                int[] location = new int[2];
                view.getLocationOnScreen(location);

                // Offset coordinates by view position
                float[] coordinatesDown = new float[]{downX + location[0], downY + location[1]};
                float[] coordinatesMove = new float[]{downX + location[0], downY + location[1]};
                float[] precision = new float[]{1f, 1f};
                // Send down event, pause, and send up
                MotionEvent down = MotionEvents.sendDown(uiController, coordinatesDown,
                        precision).down;
                uiController.loopMainThreadForAtLeast(200);
                for (int i = 0; i < interval; i++) {
                    MotionEvents.sendMovement(uiController, down, coordinatesMove);
                    uiController.loopMainThreadForAtLeast(100);

                    coordinatesMove[0] = coordinatesMove[0] + deltaX;
                    coordinatesMove[1] = coordinatesMove[1] + deltaY;
                    if (coordinatesMove[1] > limitY + location[1]
                            || coordinatesMove[0] > limitX + location[0]) {
                        break;
                    }
                }
                MotionEvents.sendUp(uiController, down, coordinatesMove);
            }
        };
    }
}
