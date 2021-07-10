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

import android.view.View;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;

/**
 * Implementation of {@link ProgressBarController}.
 *
 * <p>This class accepts a {@link ProgressBar} in it's constructor and forwards the methods
 * of {@link ProgressBarController} to it.
 */
class ProgressBarControllerImpl implements ProgressBarController {

    @NonNull
    private final ProgressBar mProgressBar;

    ProgressBarControllerImpl(@NonNull ProgressBar progressBar) {
        mProgressBar = progressBar;
    }

    @Override
    public void setVisible(boolean visible) {
        mProgressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean isVisible() {
        return mProgressBar.getVisibility() == View.VISIBLE;
    }

    @Override
    public void setIndeterminate(boolean indeterminate) {
        mProgressBar.setIndeterminate(indeterminate);
    }

    @Override
    public boolean isIndeterminate() {
        return mProgressBar.isIndeterminate();
    }

    @Override
    public void setMax(int max) {
        mProgressBar.setMax(max);
    }

    @Override
    public int getMax() {
        return mProgressBar.getMax();
    }

    @Override
    public void setMin(int min) {
        mProgressBar.setMin(min);
    }

    @Override
    public int getMin() {
        return mProgressBar.getMin();
    }

    @Override
    public void setProgress(int progress) {
        mProgressBar.setProgress(progress);
    }

    @Override
    public int getProgress() {
        return mProgressBar.getProgress();
    }
}
