/*
 * Copyright 2019 The Android Open Source Project
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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.DialogPreference;
import androidx.preference.PreferenceFragmentCompat;

/**
 * Abstract base class which presents a dialog associated with a {@link
 * androidx.preference.DialogPreference}. Since the preference object may not be available during
 * fragment re-creation, the necessary information for displaying the dialog is read once during
 * the initial call to {@link #onCreate(Bundle)} and saved/restored in the saved instance state.
 * Custom subclasses should also follow this pattern.
 *
 * <p>Note: this has the same functionality and interface as {@link
 * androidx.preference.PreferenceDialogFragmentCompat} with updates to formatting to match the
 * project style. This class preserves the {@link DialogPreference.TargetFragment} interface
 * requirement that was removed in {@link CarUiDialogFragment}. Automotive applications should use
 * children of this fragment in order to launch the system themed platform {@link AlertDialog}
 * instead of the one in the support library.
 */
public abstract class PreferenceDialogFragment extends CarUiDialogFragment implements
        DialogInterface.OnClickListener {

    protected static final String ARG_KEY = "key";

    private DialogPreference mPreference;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Fragment rawFragment = getTargetFragment();
        if (!(rawFragment instanceof DialogPreference.TargetFragment)) {
            throw new IllegalStateException(
                    "Target fragment must implement TargetFragment interface");
        }

        DialogPreference.TargetFragment fragment =
                (DialogPreference.TargetFragment) rawFragment;

        String key = getArguments().getString(ARG_KEY);
        if (savedInstanceState == null) {
            mPreference = (DialogPreference) fragment.findPreference(key);
            mDialogTitle = mPreference.getDialogTitle();
            mPositiveButtonText = mPreference.getPositiveButtonText();
            mNegativeButtonText = mPreference.getNegativeButtonText();
            mDialogMessage = mPreference.getDialogMessage();
            mDialogLayoutRes = mPreference.getDialogLayoutResource();

            Drawable icon = mPreference.getDialogIcon();
            if (icon == null || icon instanceof BitmapDrawable) {
                mDialogIcon = (BitmapDrawable) icon;
            } else {
                Bitmap bitmap = Bitmap.createBitmap(icon.getIntrinsicWidth(),
                        icon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                icon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                icon.draw(canvas);
                mDialogIcon = new BitmapDrawable(getResources(), bitmap);
            }
        }
    }

    /**
     * Get the preference that requested this dialog. Available after {@link #onCreate(Bundle)} has
     * been called on the {@link PreferenceFragmentCompat} which launched this dialog.
     *
     * @return the {@link DialogPreference} associated with this dialog.
     */
    public DialogPreference getPreference() {
        if (mPreference == null) {
            String key = getArguments().getString(ARG_KEY);
            DialogPreference.TargetFragment fragment =
                    (DialogPreference.TargetFragment) getTargetFragment();
            mPreference = (DialogPreference) fragment.findPreference(key);
        }
        return mPreference;
    }
}
