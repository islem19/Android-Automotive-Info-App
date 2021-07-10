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
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.CallSuper;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.preference.DialogPreference;

import com.android.car.ui.utils.CarUiUtils;

/**
 * Abstract base class which presents a dialog associated with a {@link
 * androidx.preference.DialogPreference}. Since the preference object may not be available during
 * fragment re-creation, the necessary information for displaying the dialog is read once during
 * the initial call to {@link #onCreate(Bundle)} and saved/restored in the saved instance state.
 * Custom subclasses should also follow this pattern.
 *
 * <p>Note: this is borrowed as-is from {@link androidx.preference.PreferenceDialogFragmentCompat}
 * with updates to formatting to match the project style and the removal of the {@link
 * DialogPreference.TargetFragment} interface requirement. See {@link PreferenceDialogFragment}
 * for a version of this class with the check preserved. Automotive applications should use
 * children of this fragment in order to launch the system themed platform {@link AlertDialog}
 * instead of the one in the support library.
 */

public abstract class CarUiDialogFragment extends DialogFragment implements
        DialogInterface.OnClickListener {

    private static final String SAVE_STATE_TITLE = "CarUiDialogFragment.title";
    private static final String SAVE_STATE_POSITIVE_TEXT = "CarUiDialogFragment.positiveText";
    private static final String SAVE_STATE_NEGATIVE_TEXT = "CarUiDialogFragment.negativeText";
    private static final String SAVE_STATE_MESSAGE = "CarUiDialogFragment.message";
    private static final String SAVE_STATE_LAYOUT = "CarUiDialogFragment.layout";
    private static final String SAVE_STATE_ICON = "CarUiDialogFragment.icon";

    protected CharSequence mDialogTitle;
    protected CharSequence mPositiveButtonText;
    protected CharSequence mNegativeButtonText;
    protected CharSequence mDialogMessage;
    @LayoutRes
    protected int mDialogLayoutRes;

    protected BitmapDrawable mDialogIcon;

    /** Which button was clicked. */
    private int mWhichButtonClicked;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mDialogTitle = savedInstanceState.getCharSequence(SAVE_STATE_TITLE);
            mPositiveButtonText = savedInstanceState.getCharSequence(SAVE_STATE_POSITIVE_TEXT);
            mNegativeButtonText = savedInstanceState.getCharSequence(SAVE_STATE_NEGATIVE_TEXT);
            mDialogMessage = savedInstanceState.getCharSequence(SAVE_STATE_MESSAGE);
            mDialogLayoutRes = savedInstanceState.getInt(SAVE_STATE_LAYOUT, 0);
            Bitmap bitmap = savedInstanceState.getParcelable(SAVE_STATE_ICON);
            if (bitmap != null) {
                mDialogIcon = new BitmapDrawable(getResources(), bitmap);
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putCharSequence(SAVE_STATE_TITLE, mDialogTitle);
        outState.putCharSequence(SAVE_STATE_POSITIVE_TEXT, mPositiveButtonText);
        outState.putCharSequence(SAVE_STATE_NEGATIVE_TEXT, mNegativeButtonText);
        outState.putCharSequence(SAVE_STATE_MESSAGE, mDialogMessage);
        outState.putInt(SAVE_STATE_LAYOUT, mDialogLayoutRes);
        if (mDialogIcon != null) {
            outState.putParcelable(SAVE_STATE_ICON, mDialogIcon.getBitmap());
        }
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Context context = getActivity();
        mWhichButtonClicked = DialogInterface.BUTTON_NEGATIVE;

        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(mDialogTitle)
                .setIcon(mDialogIcon)
                .setPositiveButton(mPositiveButtonText, this)
                .setNegativeButton(mNegativeButtonText, this);

        View contentView = onCreateDialogView(context);
        if (contentView != null) {
            onBindDialogView(contentView);
            builder.setView(contentView);
        } else {
            builder.setMessage(mDialogMessage);
        }

        onPrepareDialogBuilder(builder);

        // Create the dialog
        Dialog dialog = builder.create();
        if (needInputMethod()) {
            // Request input only after the dialog is shown. This is to prevent an issue where the
            // dialog view collapsed the content on small displays.
            dialog.setOnShowListener(d -> requestInputMethod(dialog));
        }

        return dialog;
    }

    /**
     * Prepares the dialog builder to be shown when the preference is clicked. Use this to set
     * custom properties on the dialog.
     *
     * <p>Do not {@link AlertDialog.Builder#create()} or {@link AlertDialog.Builder#show()}.
     */
    protected void onPrepareDialogBuilder(@NonNull AlertDialog.Builder builder) {
    }

    /**
     * Returns whether the preference needs to display a soft input method when the dialog is
     * displayed. Default is false. Subclasses should override this method if they need the soft
     * input method brought up automatically.
     *
     * <p>Note: Ensure your subclass manually requests focus (ideally in {@link
     * #onBindDialogView(View)}) for the input field in order to
     * correctly attach the input method to the field.
     */
    protected boolean needInputMethod() {
        return false;
    }

    /**
     * Sets the required flags on the dialog window to enable input method window to show up.
     */
    private void requestInputMethod(Dialog dialog) {
        Window window = dialog.getWindow();
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }

    /**
     * Creates the content view for the dialog (if a custom content view is required). By default,
     * it inflates the dialog layout resource if it is set.
     *
     * @return the content View for the dialog.
     * @see DialogPreference#setLayoutResource(int)
     */
    protected View onCreateDialogView(Context context) {
        int resId = mDialogLayoutRes;
        if (resId == 0) {
            return null;
        }

        LayoutInflater inflater = LayoutInflater.from(context);
        return inflater.inflate(resId, null);
    }

    /**
     * Binds views in the content View of the dialog to data.
     *
     * <p>Make sure to call through to the superclass implementation.
     *
     * @param view the content View of the dialog, if it is custom.
     */
    @CallSuper
    protected void onBindDialogView(@NonNull View view) {
        View dialogMessageView = CarUiUtils.findViewByRefId(view, android.R.id.message);

        if (dialogMessageView != null) {
            CharSequence message = mDialogMessage;
            int newVisibility = View.GONE;

            if (!TextUtils.isEmpty(message)) {
                if (dialogMessageView instanceof TextView) {
                    ((TextView) dialogMessageView).setText(message);
                }

                newVisibility = View.VISIBLE;
            }

            if (dialogMessageView.getVisibility() != newVisibility) {
                dialogMessageView.setVisibility(newVisibility);
            }
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        mWhichButtonClicked = which;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        onDialogClosed(mWhichButtonClicked == DialogInterface.BUTTON_POSITIVE);
    }

    /**
     * Called when the dialog is dismissed.
     *
     * @param positiveResult {@code true} if the dialog was dismissed with {@link
     *                       DialogInterface#BUTTON_POSITIVE}.
     */
    protected abstract void onDialogClosed(boolean positiveResult);
}
