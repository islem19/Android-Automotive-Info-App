/*
 * Copyright (C) 2019 The Android Open Source Project
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

import android.graphics.Typeface;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowTypeface;

@Implements(Typeface.class)
public class ExtendedShadowTypeface extends ShadowTypeface {
    @Implementation
    protected static Typeface create(Typeface family, int weight, boolean italic) {
        // Increment style by 10 to distinguish when a style has been italicized. This a workaround
        // for ShadowTypeface not supporting italicization for Typeface.
        int style = italic ? weight + 10 : weight;
        return ShadowTypeface.create(family, style);
    }
}
