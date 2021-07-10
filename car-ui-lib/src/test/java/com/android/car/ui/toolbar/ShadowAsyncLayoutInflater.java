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

package com.android.car.ui.toolbar;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.asynclayoutinflater.view.AsyncLayoutInflater;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

/**
 * Shadow of {@link AsyncLayoutInflater} that inflates synchronously, so that tests
 * don't have to have complicated code to wait for these inflations.
 */
@Implements(AsyncLayoutInflater.class)
public class ShadowAsyncLayoutInflater {
    @Implementation
    public void inflate(@LayoutRes int resid, @Nullable ViewGroup parent,
            @NonNull AsyncLayoutInflater.OnInflateFinishedListener callback) {
        View result = LayoutInflater.from(parent.getContext())
                .inflate(resid, parent, false);

        callback.onInflateFinished(result, resid, parent);
    }
}
