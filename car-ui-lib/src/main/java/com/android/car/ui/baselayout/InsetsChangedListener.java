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

package com.android.car.ui.baselayout;

import androidx.annotation.NonNull;

/**
 * Interface for receiving changes to {@link Insets}.
 *
 * <p>This interface can be applied to either activities or fragments. CarUi will automatically call
 * it when the insets change.
 *
 * <p>When neither the activity nor any of its fragments implement this interface, the Insets
 * will be applied as padding to the content view.
 */
public interface InsetsChangedListener {
    /** Called when the insets change */
    void onCarUiInsetsChanged(@NonNull Insets insets);
}
