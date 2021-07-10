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
package com.android.car.ui;

import static android.view.WindowInsets.Type.ime;

import static com.android.car.ui.imewidescreen.CarUiImeWideScreenController.ADD_DESC_TITLE_TO_CONTENT_AREA;
import static com.android.car.ui.imewidescreen.CarUiImeWideScreenController.ADD_DESC_TO_CONTENT_AREA;
import static com.android.car.ui.imewidescreen.CarUiImeWideScreenController.WIDE_SCREEN_ACTION;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import androidx.annotation.ArrayRes;
import androidx.annotation.AttrRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.car.ui.recyclerview.CarUiListItemAdapter;
import com.android.car.ui.recyclerview.CarUiRadioButtonListItemAdapter;
import com.android.car.ui.utils.CarUiUtils;

/**
 * Wrapper for AlertDialog.Builder
 */
public class AlertDialogBuilder {

    private AlertDialog.Builder mBuilder;
    private Context mContext;
    private boolean mPositiveButtonSet;
    private boolean mNeutralButtonSet;
    private boolean mNegativeButtonSet;
    private CharSequence mTitle;
    private CharSequence mSubtitle;
    private Drawable mIcon;
    private boolean mIconTinted;
    private boolean mAllowDismissButton = true;
    private boolean mHasSingleChoiceBodyButton = false;
    private EditText mCarUiEditText;
    private InputMethodManager mInputMethodManager;
    private String mWideScreenTitle;
    private String mWideScreenTitleDesc;
    private ViewGroup mRoot;

    // Whenever the IME is closed and opened again, the title and desc information needs to be
    // passed to the IME to be rendered. If the information is not passed to the IME the content
    // area of the IME will render nothing into the content area.
    private final View.OnApplyWindowInsetsListener mOnApplyWindowInsetsListener = (v, insets) -> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            // WindowInsets.isVisible() is only available on R or above
            return v.onApplyWindowInsets(insets);
        }

        if (insets.isVisible(ime())) {
            Bundle bundle = new Bundle();
            String title = mWideScreenTitle != null ? mWideScreenTitle : mTitle.toString();
            bundle.putString(ADD_DESC_TITLE_TO_CONTENT_AREA, title);
            if (mWideScreenTitleDesc != null) {
                bundle.putString(ADD_DESC_TO_CONTENT_AREA, mWideScreenTitleDesc);
            }
            mInputMethodManager.sendAppPrivateCommand(mCarUiEditText, WIDE_SCREEN_ACTION,
                    bundle);
        }
        return v.onApplyWindowInsets(insets);
    };

    private final AlertDialog.OnDismissListener mOnDismissListener = dialog -> {
        if (mRoot != null) {
            mRoot.setOnApplyWindowInsetsListener(null);
        }
    };

    public AlertDialogBuilder(Context context) {
        // Resource id specified as 0 uses the parent contexts resolved value for alertDialogTheme.
        this(context, /* themeResId= */0);
    }

    public AlertDialogBuilder(Context context, int themeResId) {
        mBuilder = new AlertDialog.Builder(context, themeResId);
        mInputMethodManager = (InputMethodManager)
                context.getSystemService(Context.INPUT_METHOD_SERVICE);
        mContext = context;
    }

    public Context getContext() {
        return mBuilder.getContext();
    }

    /**
     * Set the title using the given resource id.
     *
     * @return This Builder object to allow for chaining of calls to set methods
     */
    public AlertDialogBuilder setTitle(@StringRes int titleId) {
        return setTitle(mContext.getText(titleId));
    }

    /**
     * Set the title displayed in the {@link Dialog}.
     *
     * @return This Builder object to allow for chaining of calls to set methods
     */
    public AlertDialogBuilder setTitle(CharSequence title) {
        mTitle = title;
        mBuilder.setTitle(title);
        return this;
    }

    /**
     * Sets a subtitle to be displayed in the {@link Dialog}.
     *
     * @return This Builder object to allow for chaining of calls to set methods
     */
    public AlertDialogBuilder setSubtitle(@StringRes int subtitle) {
        return setSubtitle(mContext.getString(subtitle));
    }

    /**
     * Sets a subtitle to be displayed in the {@link Dialog}.
     *
     * @return This Builder object to allow for chaining of calls to set methods
     */
    public AlertDialogBuilder setSubtitle(CharSequence subtitle) {
        mSubtitle = subtitle;
        return this;
    }

    /**
     * Set the message to display using the given resource id.
     *
     * @return This Builder object to allow for chaining of calls to set methods
     */
    public AlertDialogBuilder setMessage(@StringRes int messageId) {
        mBuilder.setMessage(messageId);
        return this;
    }

    /**
     * Set the message to display.
     *
     * @return This Builder object to allow for chaining of calls to set methods
     */
    public AlertDialogBuilder setMessage(CharSequence message) {
        mBuilder.setMessage(message);
        return this;
    }

    /**
     * Set the resource id of the {@link Drawable} to be used in the title.
     * <p>
     * Takes precedence over values set using {@link #setIcon(Drawable)}.
     *
     * @return This Builder object to allow for chaining of calls to set methods
     */
    public AlertDialogBuilder setIcon(@DrawableRes int iconId) {
        return setIcon(mContext.getDrawable(iconId));
    }

    /**
     * Set the {@link Drawable} to be used in the title.
     * <p>
     * <strong>Note:</strong> To ensure consistent styling, the drawable
     * should be inflated or constructed using the alert dialog's themed
     * context obtained via {@link #getContext()}.
     *
     * @return this Builder object to allow for chaining of calls to set
     * methods
     */
    public AlertDialogBuilder setIcon(Drawable icon) {
        mIcon = icon;
        return this;
    }

    /**
     * Whether the icon provided by {@link #setIcon(Drawable)} should be
     * tinted with the default system color.
     *
     * @return this Builder object to allow for chaining of calls to set
     * methods.
     */
    public AlertDialogBuilder setIconTinted(boolean tinted) {
        mIconTinted = tinted;
        return this;
    }

    /**
     * Set an icon as supplied by a theme attribute. e.g.
     * {@link android.R.attr#alertDialogIcon}.
     * <p>
     * Takes precedence over values set using {@link #setIcon(Drawable)}.
     *
     * @param attrId ID of a theme attribute that points to a drawable resource.
     */
    public AlertDialogBuilder setIconAttribute(@AttrRes int attrId) {
        mBuilder.setIconAttribute(attrId);
        return this;
    }

    /**
     * Set a listener to be invoked when the positive button of the dialog is pressed.
     *
     * @param textId The resource id of the text to display in the positive button
     * @param listener The {@link DialogInterface.OnClickListener} to use.
     * @return This Builder object to allow for chaining of calls to set methods
     */
    public AlertDialogBuilder setPositiveButton(@StringRes int textId,
            final DialogInterface.OnClickListener listener) {
        mBuilder.setPositiveButton(textId, listener);
        mPositiveButtonSet = true;
        return this;
    }

    /**
     * Set a listener to be invoked when the positive button of the dialog is pressed.
     *
     * @param text The text to display in the positive button
     * @param listener The {@link DialogInterface.OnClickListener} to use.
     * @return This Builder object to allow for chaining of calls to set methods
     */
    public AlertDialogBuilder setPositiveButton(CharSequence text,
            final DialogInterface.OnClickListener listener) {
        mBuilder.setPositiveButton(text, listener);
        mPositiveButtonSet = true;
        return this;
    }

    /**
     * Set a listener to be invoked when the negative button of the dialog is pressed.
     *
     * @param textId The resource id of the text to display in the negative button
     * @param listener The {@link DialogInterface.OnClickListener} to use.
     * @return This Builder object to allow for chaining of calls to set methods
     */
    public AlertDialogBuilder setNegativeButton(@StringRes int textId,
            final DialogInterface.OnClickListener listener) {
        mBuilder.setNegativeButton(textId, listener);
        mNegativeButtonSet = true;
        return this;
    }

    /**
     * Set a listener to be invoked when the negative button of the dialog is pressed.
     *
     * @param text The text to display in the negative button
     * @param listener The {@link DialogInterface.OnClickListener} to use.
     * @return This Builder object to allow for chaining of calls to set methods
     */
    public AlertDialogBuilder setNegativeButton(CharSequence text,
            final DialogInterface.OnClickListener listener) {
        mBuilder.setNegativeButton(text, listener);
        mNegativeButtonSet = true;
        return this;
    }

    /**
     * Set a listener to be invoked when the neutral button of the dialog is pressed.
     *
     * @param textId The resource id of the text to display in the neutral button
     * @param listener The {@link DialogInterface.OnClickListener} to use.
     * @return This Builder object to allow for chaining of calls to set methods
     */
    public AlertDialogBuilder setNeutralButton(@StringRes int textId,
            final DialogInterface.OnClickListener listener) {
        mBuilder.setNeutralButton(textId, listener);
        mNeutralButtonSet = true;
        return this;
    }

    /**
     * Set a listener to be invoked when the neutral button of the dialog is pressed.
     *
     * @param text The text to display in the neutral button
     * @param listener The {@link DialogInterface.OnClickListener} to use.
     * @return This Builder object to allow for chaining of calls to set methods
     */
    public AlertDialogBuilder setNeutralButton(CharSequence text,
            final DialogInterface.OnClickListener listener) {
        mBuilder.setNeutralButton(text, listener);
        mNeutralButtonSet = true;
        return this;
    }

    /**
     * Sets whether the dialog is cancelable or not.  Default is true.
     *
     * @return This Builder object to allow for chaining of calls to set methods
     */
    public AlertDialogBuilder setCancelable(boolean cancelable) {
        mBuilder.setCancelable(cancelable);
        return this;
    }

    /**
     * Sets the callback that will be called if the dialog is canceled.
     *
     * <p>Even in a cancelable dialog, the dialog may be dismissed for reasons other than
     * being canceled or one of the supplied choices being selected.
     * If you are interested in listening for all cases where the dialog is dismissed
     * and not just when it is canceled, see
     * {@link #setOnDismissListener(android.content.DialogInterface.OnDismissListener)
     * setOnDismissListener}.</p>
     *
     * @return This Builder object to allow for chaining of calls to set methods
     * @see #setCancelable(boolean)
     * @see #setOnDismissListener(android.content.DialogInterface.OnDismissListener)
     */
    public AlertDialogBuilder setOnCancelListener(
            DialogInterface.OnCancelListener onCancelListener) {
        mBuilder.setOnCancelListener(onCancelListener);
        return this;
    }

    /**
     * Sets the callback that will be called when the dialog is dismissed for any reason.
     *
     * @return This Builder object to allow for chaining of calls to set methods
     */
    public AlertDialogBuilder setOnDismissListener(
            DialogInterface.OnDismissListener onDismissListener) {
        mBuilder.setOnDismissListener(onDismissListener);
        return this;
    }

    /**
     * Sets the callback that will be called if a key is dispatched to the dialog.
     *
     * @return This Builder object to allow for chaining of calls to set methods
     */
    public AlertDialogBuilder setOnKeyListener(DialogInterface.OnKeyListener onKeyListener) {
        mBuilder.setOnKeyListener(onKeyListener);
        return this;
    }

    /**
     * Set a list of items to be displayed in the dialog as the content, you will be notified of the
     * selected item via the supplied listener. This should be an array type i.e. R.array.foo
     *
     * @return This Builder object to allow for chaining of calls to set methods
     */
    public AlertDialogBuilder setItems(@ArrayRes int itemsId,
            final DialogInterface.OnClickListener listener) {
        mBuilder.setItems(itemsId, listener);
        mHasSingleChoiceBodyButton = true;
        return this;
    }

    /**
     * Set a list of items to be displayed in the dialog as the content, you will be notified of the
     * selected item via the supplied listener.
     *
     * @return This Builder object to allow for chaining of calls to set methods
     */
    public AlertDialogBuilder setItems(CharSequence[] items,
            final DialogInterface.OnClickListener listener) {
        mBuilder.setItems(items, listener);
        mHasSingleChoiceBodyButton = true;
        return this;
    }

    /**
     * This was not supposed to be in the Chassis API because it allows custom views.
     *
     * @deprecated Use {@link #setAdapter(CarUiListItemAdapter)} instead.
     */
    @Deprecated
    public AlertDialogBuilder setAdapter(final ListAdapter adapter,
            final DialogInterface.OnClickListener listener) {
        mBuilder.setAdapter(adapter, listener);
        mHasSingleChoiceBodyButton = true;
        return this;
    }

    /**
     * Display all the {@link com.android.car.ui.recyclerview.CarUiListItem CarUiListItems} in a
     * {@link CarUiListItemAdapter}. You should set click listeners on the CarUiListItems as
     * opposed to a callback in this function.
     */
    public AlertDialogBuilder setAdapter(final CarUiListItemAdapter adapter) {
        setCustomList(adapter);
        mHasSingleChoiceBodyButton = true;
        return this;
    }

    private void setCustomList(@NonNull CarUiListItemAdapter adapter) {
        View customList = LayoutInflater.from(mContext).inflate(
                R.layout.car_ui_alert_dialog_list, null);
        RecyclerView mList = CarUiUtils.requireViewByRefId(customList, R.id.list);
        mList.setLayoutManager(new LinearLayoutManager(mContext));
        mList.setAdapter(adapter);
        mBuilder.setView(customList);
    }

    /**
     * Set a list of items, which are supplied by the given {@link Cursor}, to be
     * displayed in the dialog as the content, you will be notified of the
     * selected item via the supplied listener.
     *
     * @param cursor The {@link Cursor} to supply the list of items
     * @param listener The listener that will be called when an item is clicked.
     * @param labelColumn The column name on the cursor containing the string to display
     * in the label.
     * @return This Builder object to allow for chaining of calls to set methods
     */
    public AlertDialogBuilder setCursor(final Cursor cursor,
            final DialogInterface.OnClickListener listener,
            String labelColumn) {
        mBuilder.setCursor(cursor, listener, labelColumn);
        mHasSingleChoiceBodyButton = true;
        return this;
    }

    /**
     * Set a list of items to be displayed in the dialog as the content,
     * you will be notified of the selected item via the supplied listener.
     * This should be an array type, e.g. R.array.foo. The list will have
     * a check mark displayed to the right of the text for each checked
     * item. Clicking on an item in the list will not dismiss the dialog.
     * Clicking on a button will dismiss the dialog.
     *
     * @param itemsId the resource id of an array i.e. R.array.foo
     * @param checkedItems specifies which items are checked. It should be null in which case no
     * items are checked. If non null it must be exactly the same length as the array of
     * items.
     * @param listener notified when an item on the list is clicked. The dialog will not be
     * dismissed when an item is clicked. It will only be dismissed if clicked on a
     * button, if no buttons are supplied it's up to the user to dismiss the dialog.
     * @return This Builder object to allow for chaining of calls to set methods
     */
    public AlertDialogBuilder setMultiChoiceItems(@ArrayRes int itemsId, boolean[] checkedItems,
            final DialogInterface.OnMultiChoiceClickListener listener) {
        mBuilder.setMultiChoiceItems(itemsId, checkedItems, listener);
        mHasSingleChoiceBodyButton = false;
        return this;
    }

    /**
     * Set a list of items to be displayed in the dialog as the content,
     * you will be notified of the selected item via the supplied listener.
     * The list will have a check mark displayed to the right of the text
     * for each checked item. Clicking on an item in the list will not
     * dismiss the dialog. Clicking on a button will dismiss the dialog.
     *
     * @param items the text of the items to be displayed in the list.
     * @param checkedItems specifies which items are checked. It should be null in which case no
     * items are checked. If non null it must be exactly the same length as the array of
     * items.
     * @param listener notified when an item on the list is clicked. The dialog will not be
     * dismissed when an item is clicked. It will only be dismissed if clicked on a
     * button, if no buttons are supplied it's up to the user to dismiss the dialog.
     * @return This Builder object to allow for chaining of calls to set methods
     */
    public AlertDialogBuilder setMultiChoiceItems(CharSequence[] items, boolean[] checkedItems,
            final DialogInterface.OnMultiChoiceClickListener listener) {
        mBuilder.setMultiChoiceItems(items, checkedItems, listener);
        mHasSingleChoiceBodyButton = false;
        return this;
    }

    /**
     * Set a list of items to be displayed in the dialog as the content,
     * you will be notified of the selected item via the supplied listener.
     * The list will have a check mark displayed to the right of the text
     * for each checked item. Clicking on an item in the list will not
     * dismiss the dialog. Clicking on a button will dismiss the dialog.
     *
     * @param cursor the cursor used to provide the items.
     * @param isCheckedColumn specifies the column name on the cursor to use to determine
     * whether a checkbox is checked or not. It must return an integer value where 1
     * means checked and 0 means unchecked.
     * @param labelColumn The column name on the cursor containing the string to display in the
     * label.
     * @param listener notified when an item on the list is clicked. The dialog will not be
     * dismissed when an item is clicked. It will only be dismissed if clicked on a
     * button, if no buttons are supplied it's up to the user to dismiss the dialog.
     * @return This Builder object to allow for chaining of calls to set methods
     */
    public AlertDialogBuilder setMultiChoiceItems(Cursor cursor, String isCheckedColumn,
            String labelColumn,
            final DialogInterface.OnMultiChoiceClickListener listener) {
        mBuilder.setMultiChoiceItems(cursor, isCheckedColumn, labelColumn, listener);
        mHasSingleChoiceBodyButton = true;
        return this;
    }

    /**
     * Set a list of items to be displayed in the dialog as the content, you will be notified of
     * the selected item via the supplied listener. This should be an array type i.e.
     * R.array.foo The list will have a check mark displayed to the right of the text for the
     * checked item. Clicking on an item in the list will not dismiss the dialog. Clicking on a
     * button will dismiss the dialog.
     *
     * @param itemsId the resource id of an array i.e. R.array.foo
     * @param checkedItem specifies which item is checked. If -1 no items are checked.
     * @param listener notified when an item on the list is clicked. The dialog will not be
     * dismissed when an item is clicked. It will only be dismissed if clicked on a
     * button, if no buttons are supplied it's up to the user to dismiss the dialog.
     * @return This Builder object to allow for chaining of calls to set methods
     */
    public AlertDialogBuilder setSingleChoiceItems(@ArrayRes int itemsId, int checkedItem,
            final DialogInterface.OnClickListener listener) {
        mBuilder.setSingleChoiceItems(itemsId, checkedItem, listener);
        mHasSingleChoiceBodyButton = true;
        return this;
    }

    /**
     * Set a list of items to be displayed in the dialog as the content, you will be notified of
     * the selected item via the supplied listener. The list will have a check mark displayed to
     * the right of the text for the checked item. Clicking on an item in the list will not
     * dismiss the dialog. Clicking on a button will dismiss the dialog.
     *
     * @param cursor the cursor to retrieve the items from.
     * @param checkedItem specifies which item is checked. If -1 no items are checked.
     * @param labelColumn The column name on the cursor containing the string to display in the
     * label.
     * @param listener notified when an item on the list is clicked. The dialog will not be
     * dismissed when an item is clicked. It will only be dismissed if clicked on a
     * button, if no buttons are supplied it's up to the user to dismiss the dialog.
     * @return This Builder object to allow for chaining of calls to set methods
     */
    public AlertDialogBuilder setSingleChoiceItems(Cursor cursor, int checkedItem,
            String labelColumn,
            final DialogInterface.OnClickListener listener) {
        mBuilder.setSingleChoiceItems(cursor, checkedItem, labelColumn, listener);
        mHasSingleChoiceBodyButton = true;
        return this;
    }

    /**
     * Set a list of items to be displayed in the dialog as the content, you will be notified of
     * the selected item via the supplied listener. The list will have a check mark displayed to
     * the right of the text for the checked item. Clicking on an item in the list will not
     * dismiss the dialog. Clicking on a button will dismiss the dialog.
     *
     * @param items the items to be displayed.
     * @param checkedItem specifies which item is checked. If -1 no items are checked.
     * @param listener notified when an item on the list is clicked. The dialog will not be
     * dismissed when an item is clicked. It will only be dismissed if clicked on a
     * button, if no buttons are supplied it's up to the user to dismiss the dialog.
     * @return This Builder object to allow for chaining of calls to set methods
     */
    public AlertDialogBuilder setSingleChoiceItems(CharSequence[] items, int checkedItem,
            final DialogInterface.OnClickListener listener) {
        mBuilder.setSingleChoiceItems(items, checkedItem, listener);
        mHasSingleChoiceBodyButton = true;
        return this;
    }

    /**
     * This was not supposed to be in the Chassis API because it allows custom views.
     *
     * @deprecated Use {@link #setSingleChoiceItems(CarUiRadioButtonListItemAdapter,
     * DialogInterface.OnClickListener)} instead.
     */
    @Deprecated
    public AlertDialogBuilder setSingleChoiceItems(ListAdapter adapter, int checkedItem,
            final DialogInterface.OnClickListener listener) {
        mBuilder.setSingleChoiceItems(adapter, checkedItem, listener);
        mHasSingleChoiceBodyButton = true;
        return this;
    }

    /**
     * Set a list of items to be displayed in the dialog as the content, you will be notified of
     * the selected item via the supplied listener. The list will have a check mark displayed to
     * the right of the text for the checked item. Clicking on an item in the list will not
     * dismiss the dialog. Clicking on a button will dismiss the dialog.
     *
     * @param adapter The {@link CarUiRadioButtonListItemAdapter} to supply the list of items
     * @param listener notified when an item on the list is clicked. The dialog will not be
     * dismissed when an item is clicked. It will only be dismissed if clicked on a
     * button, if no buttons are supplied it's up to the user to dismiss the dialog.
     * @return This Builder object to allow for chaining of calls to set methods
     * @deprecated Use {@link #setSingleChoiceItems(CarUiRadioButtonListItemAdapter)} instead.
     */
    @Deprecated
    public AlertDialogBuilder setSingleChoiceItems(CarUiRadioButtonListItemAdapter adapter,
            final DialogInterface.OnClickListener listener) {
        setCustomList(adapter);
        mHasSingleChoiceBodyButton = false;
        return this;
    }

    /**
     * Set a list of items to be displayed in the dialog as the content,The list will have a check
     * mark displayed to the right of the text for the checked item. Clicking on an item in the list
     * will not dismiss the dialog. Clicking on a button will dismiss the dialog.
     *
     * @param adapter The {@link CarUiRadioButtonListItemAdapter} to supply the list of items
     * dismissed when an item is clicked. It will only be dismissed if clicked on a
     * button, if no buttons are supplied it's up to the user to dismiss the dialog.
     * @return This Builder object to allow for chaining of calls to set methods
     */
    public AlertDialogBuilder setSingleChoiceItems(CarUiRadioButtonListItemAdapter adapter) {
        setCustomList(adapter);
        mHasSingleChoiceBodyButton = false;
        return this;
    }

    /**
     * Sets a listener to be invoked when an item in the list is selected.
     *
     * @param listener the listener to be invoked
     * @return this Builder object to allow for chaining of calls to set methods
     * @see AdapterView#setOnItemSelectedListener(android.widget.AdapterView.OnItemSelectedListener)
     */
    public AlertDialogBuilder setOnItemSelectedListener(
            final AdapterView.OnItemSelectedListener listener) {
        mBuilder.setOnItemSelectedListener(listener);
        mHasSingleChoiceBodyButton = true;
        return this;
    }

    /**
     * Sets a custom edit text box within the alert dialog.
     *
     * @param prompt the string that will be set on the edit text view
     * @param textChangedListener textWatcher whose methods are called whenever this TextView's text
     * changes {@link null} otherwise.
     * @param inputFilters list of input filters, {@link null} if no filter is needed
     * @param inputType See {@link EditText#setInputType(int)}, except
     *                  {@link android.text.InputType#TYPE_NULL} will not be set.
     * @return this Builder object to allow for chaining of calls to set methods
     */
    public AlertDialogBuilder setEditBox(String prompt, TextWatcher textChangedListener,
            InputFilter[] inputFilters, int inputType) {
        View contentView = LayoutInflater.from(mContext).inflate(
                R.layout.car_ui_alert_dialog_edit_text, null);

        mCarUiEditText = CarUiUtils.requireViewByRefId(contentView, R.id.textbox);
        mCarUiEditText.setText(prompt);

        if (textChangedListener != null) {
            mCarUiEditText.addTextChangedListener(textChangedListener);
        }

        if (inputFilters != null) {
            mCarUiEditText.setFilters(inputFilters);
        }

        if (inputType != 0) {
            mCarUiEditText.setInputType(inputType);
        }

        mBuilder.setView(contentView);
        return this;
    }

    /**
     * Sets a custom edit text box within the alert dialog.
     *
     * @param prompt the string that will be set on the edit text view
     * @param textChangedListener textWatcher whose methods are called whenever this TextView's text
     * changes {@link null} otherwise.
     * @param inputFilters list of input filters, {@link null} if no filter is needed
     * @return this Builder object to allow for chaining of calls to set methods
     */
    public AlertDialogBuilder setEditBox(String prompt, TextWatcher textChangedListener,
            InputFilter[] inputFilters) {
        return setEditBox(prompt, textChangedListener, inputFilters, 0);
    }

    /**
     * By default, the AlertDialogBuilder may add a "Dismiss" button if you don't provide
     * a positive/negative/neutral button. This is so that the dialog is still dismissible
     * using the rotary controller. If however, you add buttons that can close the dialog via
     * {@link #setAdapter(CarUiListItemAdapter)} or a similar method, then you may wish to
     * suppress the addition of the dismiss button, which this method allows for.
     *
     * @param allowDismissButton If true, a "Dismiss" button may be added to the dialog.
     *                           If false, it will never be added.
     * @return this Builder object to allow for chaining of calls to set methods
     */
    public AlertDialogBuilder setAllowDismissButton(boolean allowDismissButton) {
        mAllowDismissButton = allowDismissButton;
        return this;
    }

    /**
     * Sets the title and desc related to the dialog within the IMS templates.
     *
     * @param title title to be set.
     * @param desc description related to the dialog.
     * @return this Builder object to allow for chaining of calls to set methods
     */
    public AlertDialogBuilder setEditTextTitleAndDescForWideScreen(String title, String desc) {
        mWideScreenTitle = title;
        mWideScreenTitleDesc = desc;

        return this;
    }

    /** Final steps common to both {@link #create()} and {@link #show()} */
    private void prepareDialog() {
        View customTitle = LayoutInflater.from(mContext).inflate(
                R.layout.car_ui_alert_dialog_title_with_subtitle, null);

        TextView mTitleView = CarUiUtils.requireViewByRefId(customTitle, R.id.car_ui_alert_title);
        TextView mSubtitleView =
                CarUiUtils.requireViewByRefId(customTitle, R.id.car_ui_alert_subtitle);
        mSubtitleView.setMovementMethod(LinkMovementMethod.getInstance());
        ImageView mIconView = CarUiUtils.requireViewByRefId(customTitle, R.id.car_ui_alert_icon);

        mTitleView.setText(mTitle);
        mTitleView.setVisibility(TextUtils.isEmpty(mTitle) ? View.GONE : View.VISIBLE);
        mSubtitleView.setText(mSubtitle);
        mSubtitleView.setVisibility(TextUtils.isEmpty(mSubtitle) ? View.GONE : View.VISIBLE);
        mIconView.setImageDrawable(mIcon);
        mIconView.setVisibility(mIcon != null ? View.VISIBLE : View.GONE);
        if (mIconTinted) {
            mIconView.setImageTintList(
                    mContext.getColorStateList(R.color.car_ui_dialog_icon_color));
        }
        mBuilder.setCustomTitle(customTitle);

        if (!mAllowDismissButton && !mHasSingleChoiceBodyButton
                && !mNeutralButtonSet && !mNegativeButtonSet && !mPositiveButtonSet) {
            throw new RuntimeException(
                    "The dialog must have at least one button to disable the dismiss button");
        }
        if (mContext.getResources().getBoolean(R.bool.car_ui_alert_dialog_force_dismiss_button)
                && !mNeutralButtonSet && !mNegativeButtonSet && !mPositiveButtonSet
                && mAllowDismissButton) {
            String mDefaultButtonText = mContext.getString(
                    R.string.car_ui_alert_dialog_default_button);
            mBuilder.setNegativeButton(mDefaultButtonText, (dialog, which) -> {
            });
        }
    }

    /**
     * Creates an {@link AlertDialog} with the arguments supplied to this
     * builder.
     * <p>
     * Calling this method does not display the dialog. If no additional
     * processing is needed, {@link #show()} may be called instead to both
     * create and display the dialog.
     */
    public AlertDialog create() {
        prepareDialog();
        AlertDialog alertDialog = mBuilder.create();

        // Put a FocusParkingView at the end of dialog window to prevent rotary controller
        // wrap-around. Android will focus on the first view automatically when the dialog is shown,
        // and we want it to focus on the title instead of the FocusParkingView, so we put the
        // FocusParkingView at the end of dialog window.
        mRoot = (ViewGroup) alertDialog.getWindow().getDecorView().getRootView();
        FocusParkingView fpv = new FocusParkingView(mContext);
        mRoot.addView(fpv);

        // apply window insets listener to know when IME is visible so we can set title and desc.
        mRoot.setOnApplyWindowInsetsListener(mOnApplyWindowInsetsListener);
        setOnDismissListener(mOnDismissListener);

        return alertDialog;
    }

    /**
     * Creates an {@link AlertDialog} with the arguments supplied to this
     * builder and immediately displays the dialog.
     */
    public AlertDialog show() {
        AlertDialog alertDialog = create();
        alertDialog.show();
        return alertDialog;
    }
}
