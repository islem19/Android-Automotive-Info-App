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

package com.android.car.ui.recyclerview;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Definition of list items that can be inserted into {@link CarUiListItemAdapter}.
 */
public class CarUiContentListItem extends CarUiListItem {

    /**
     * Callback to be invoked when the checked state of a list item changed.
     */
    public interface OnCheckedChangeListener {
        /**
         * Called when the checked state of a list item has changed.
         *
         * @param item      whose checked state changed.
         * @param isChecked new checked state of list item.
         */
        void onCheckedChanged(@NonNull CarUiContentListItem item, boolean isChecked);
    }

    /**
     * Callback to be invoked when an item is clicked.
     */
    public interface OnClickListener {
        /**
         * Called when the item has been clicked.
         *
         * @param item whose checked state changed.
         */
        void onClick(@NonNull CarUiContentListItem item);
    }

    public enum IconType {
        /**
         * For an icon type of CONTENT, the primary icon is a larger than {@code STANDARD}.
         */
        CONTENT,
        /**
         * For an icon type of STANDARD, the primary icon is the standard size.
         */
        STANDARD,
        /**
         * For an icon type of AVATAR, the primary icon is masked to provide an icon with a modified
         * shape.
         */
        AVATAR
    }

    /**
     * Enum of secondary action types of a list item.
     */
    public enum Action {
        /**
         * For an action value of NONE, no action element is shown for a list item.
         */
        NONE,
        /**
         * For an action value of SWITCH, a switch is shown for the action element of the list item.
         */
        SWITCH,
        /**
         * For an action value of CHECK_BOX, a checkbox is shown for the action element of the list
         * item.
         */
        CHECK_BOX,
        /**
         * For an action value of RADIO_BUTTON, a radio button is shown for the action element of
         * the list item.
         */
        RADIO_BUTTON,
        /**
         * For an action value of ICON, an icon is shown for the action element of the list item.
         */
        ICON,
        /**
         * For an action value CHEVRON, a chevron is shown for the action element of the list
         * item.
         */
        CHEVRON
    }

    private Drawable mIcon;
    @Nullable
    private Drawable mSupplementalIcon;
    private CharSequence mTitle;
    private CharSequence mBody;
    private final Action mAction;
    private IconType mPrimaryIconType;
    private boolean mIsActionDividerVisible;
    private boolean mIsChecked;
    private boolean mIsEnabled = true;
    private boolean mIsActivated;
    private OnClickListener mOnClickListener;
    private OnCheckedChangeListener mOnCheckedChangeListener;
    private OnClickListener mSupplementalIconOnClickListener;


    public CarUiContentListItem(Action action) {
        mAction = action;
        mPrimaryIconType = IconType.STANDARD;
    }

    /**
     * Returns the title of the item.
     */
    @Nullable
    public CharSequence getTitle() {
        return mTitle;
    }

    /**
     * Sets the title of the item.
     *
     * @param title text to display as title.
     */
    public void setTitle(@NonNull CharSequence title) {
        mTitle = title;
    }

    /**
     * Returns the body text of the item.
     */
    @Nullable
    public CharSequence getBody() {
        return mBody;
    }

    /**
     * Sets the body of the item.
     *
     * @param body text to display as body text.
     */
    public void setBody(@NonNull CharSequence body) {
        mBody = body;
    }

    /**
     * Returns the icon of the item.
     */
    @Nullable
    public Drawable getIcon() {
        return mIcon;
    }

    /**
     * Sets the icon of the item.
     *
     * @param icon the icon to display.
     */
    public void setIcon(@Nullable Drawable icon) {
        mIcon = icon;
    }

    /**
     * Returns the primary icon type for the item.
     */
    public IconType getPrimaryIconType() {
        return mPrimaryIconType;
    }

    /**
     * Sets the primary icon type for the item.
     *
     * @param icon the icon type for the item.
     */
    public void setPrimaryIconType(IconType icon) {
        mPrimaryIconType = icon;
    }

    /**
     * Returns {@code true} if the item is activated.
     */
    public boolean isActivated() {
        return mIsActivated;
    }

    /**
     * Sets the activated state of the item.
     *
     * @param activated the activated state for the item.
     */
    public void setActivated(boolean activated) {
        mIsActivated = activated;
    }

    /**
     * Returns {@code true} if the item is enabled.
     */
    public boolean isEnabled() {
        return mIsEnabled;
    }

    /**
     * Sets the enabled state of the item.
     *
     * @param enabled the enabled state for the item.
     */
    public void setEnabled(boolean enabled) {
        mIsEnabled = enabled;
    }

    /**
     * Returns {@code true} if the item is checked. Will always return {@code false} when the action
     * type for the item is {@code Action.NONE}.
     */
    public boolean isChecked() {
        return mIsChecked;
    }

    /**
     * Sets the checked state of the item.
     *
     * @param checked the checked state for the item.
     */
    public void setChecked(boolean checked) {
        if (checked == mIsChecked) {
            return;
        }

        // Checked state can only be set when action type is checkbox, radio button or switch.
        if (mAction == Action.CHECK_BOX || mAction == Action.SWITCH
                || mAction == Action.RADIO_BUTTON) {
            mIsChecked = checked;

            if (mOnCheckedChangeListener != null) {
                mOnCheckedChangeListener.onCheckedChanged(this, mIsChecked);
            }
        }
    }

    /**
     * Sets the visibility of the action divider.
     *
     * @param visible visibility of the action divider.
     */
    public void setActionDividerVisible(boolean visible) {
        mIsActionDividerVisible = visible;
    }

    /**
     * Returns {@code true} if the action divider is visible.
     */
    public boolean isActionDividerVisible() {
        return mIsActionDividerVisible;
    }

    /**
     * Returns the action type for the item.
     */
    public Action getAction() {
        return mAction;
    }

    /**
     * Returns the supplemental icon for the item.
     */
    @Nullable
    public Drawable getSupplementalIcon() {
        if (mAction != Action.ICON) {
            return null;
        }

        return mSupplementalIcon;
    }

    /**
     * Sets supplemental icon to be displayed in a list item.
     *
     * @param icon the Drawable to set as the icon, or null to clear the content.
     */
    public void setSupplementalIcon(@Nullable Drawable icon) {
        setSupplementalIcon(icon, null);
    }

    /**
     * Sets supplemental icon to be displayed in a list item.
     *
     * @param icon     the Drawable to set as the icon, or null to clear the content.
     * @param listener the callback that is invoked when the icon is clicked.
     */
    public void setSupplementalIcon(@Nullable Drawable icon,
            @Nullable OnClickListener listener) {
        if (mAction != Action.ICON) {
            throw new IllegalStateException(
                    "Cannot set supplemental icon on list item that does not have an action of "
                            + "type ICON");
        }

        mSupplementalIcon = icon;
        mSupplementalIconOnClickListener = listener;
    }

    @Nullable
    public OnClickListener getSupplementalIconOnClickListener() {
        return mSupplementalIconOnClickListener;
    }

    /**
     * Registers a callback to be invoked when the item is clicked.
     *
     * @param listener callback to be invoked when item is clicked.
     */
    public void setOnItemClickedListener(@Nullable OnClickListener listener) {
        mOnClickListener = listener;
    }

    /**
     * Returns the {@link OnClickListener} registered for this item.
     */
    @Nullable
    public OnClickListener getOnClickListener() {
        return mOnClickListener;
    }

    /**
     * Registers a callback to be invoked when the checked state of list item changes.
     *
     * <p>Checked state changes can take place when the action type is {@code Action.SWITCH} or
     * {@code Action.CHECK_BOX}.
     *
     * @param listener callback to be invoked when the checked state shown in the UI changes.
     */
    public void setOnCheckedChangeListener(
            @Nullable OnCheckedChangeListener listener) {
        mOnCheckedChangeListener = listener;
    }

    /**
     * Returns the {@link OnCheckedChangeListener} registered for this item.
     */
    @Nullable
    public OnCheckedChangeListener getOnCheckedChangeListener() {
        return mOnCheckedChangeListener;
    }
}
