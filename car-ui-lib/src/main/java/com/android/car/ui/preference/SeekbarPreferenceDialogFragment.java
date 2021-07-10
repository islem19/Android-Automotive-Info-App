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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;

/**
 * A fragment that provides a layout with a seekbar within a dialog.
 */
public class SeekbarPreferenceDialogFragment extends PreferenceDialogFragment {

    /**
     * Returns a new instance of {@link SeekbarPreferenceDialogFragment} for the {@link
     * CarUiSeekBarDialogPreference} with the given {@code key}.
     */
    public static SeekbarPreferenceDialogFragment newInstance(String key) {
        SeekbarPreferenceDialogFragment fragment = new SeekbarPreferenceDialogFragment();
        Bundle b = new Bundle(/* capacity= */ 1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    protected void onBindDialogView(@NonNull View view) {
        super.onBindDialogView(view);

        DialogFragmentCallbacks dialogPreference = (DialogFragmentCallbacks) getPreference();
        dialogPreference.onBindDialogView(view);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        super.onClick(dialog, which);

        DialogFragmentCallbacks dialogPreference = (DialogFragmentCallbacks) getPreference();
        dialogPreference.onClick(dialog, which);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        DialogFragmentCallbacks dialogPreference = (DialogFragmentCallbacks) getPreference();
        dialogPreference.onDialogClosed(positiveResult);
    }

    @Override
    protected void onPrepareDialogBuilder(@NonNull AlertDialog.Builder builder) {
        DialogFragmentCallbacks dialogPreference = (DialogFragmentCallbacks) getPreference();
        dialogPreference.onPrepareDialogBuilder(builder);
    }
}
