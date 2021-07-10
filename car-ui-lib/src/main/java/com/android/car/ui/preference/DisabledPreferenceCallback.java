/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.annotation.NonNull;

/**
 * Interface for preferences to handle clicks when its disabled.
 */
public interface DisabledPreferenceCallback {

    /**
     * Sets if the ripple effect should be shown on disabled preference.
     */
    default void setShouldShowRippleOnDisabledPreference(boolean showRipple) {}

    /**
     * Sets the message to be displayed when the disabled preference is clicked.
     */
    default void setMessageToShowWhenDisabledPreferenceClicked(@NonNull String message) {}
}
