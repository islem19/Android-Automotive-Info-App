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

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.VisibleForTesting;

import com.android.car.ui.R;
import com.android.car.ui.utils.CarUxRestrictionsUtil;

import java.lang.ref.WeakReference;

/**
 * Represents a button to display in the {@link Toolbar}.
 *
 * <p>There are currently 3 types of buttons: icon, text, and switch. Using
 * {@link Builder#setCheckable()} will ensure that you get a switch, after that
 * {@link Builder#setIcon(int)} will ensure an icon, and anything left just requires
 * {@link Builder#setTitle(int)}.
 *
 * <p>Each MenuItem has a {@link DisplayBehavior} that controls if it appears on the {@link Toolbar}
 * itself, or it's overflow menu.
 *
 * <p>If you require a search or settings button, you should use
 * {@link Builder#setToSearch()} or
 * {@link Builder#setToSettings()}.
 *
 * <p>Some properties can be changed after the creating a MenuItem, but others require being set
 * with a {@link Builder}.
 */
public class MenuItem {

    private final Context mContext;
    private final boolean mIsCheckable;
    private final boolean mIsActivatable;
    private final boolean mIsSearch;
    private final boolean mShowIconAndTitle;
    private final boolean mIsTinted;
    private final boolean mIsPrimary;
    @CarUxRestrictions.CarUxRestrictionsInfo

    private int mId;
    private CarUxRestrictions mCurrentRestrictions;
    // This is a WeakReference to allow the Toolbar (and by extension, the whole screen
    // the toolbar is on) to be garbage-collected if the MenuItem is held past the
    // lifecycle of the toolbar.
    private WeakReference<Listener> mListener = new WeakReference<>(null);
    private CharSequence mTitle;
    private Drawable mIcon;
    private OnClickListener mOnClickListener;
    private DisplayBehavior mDisplayBehavior;
    private int mUxRestrictions;
    private boolean mIsEnabled;
    private boolean mIsChecked;
    private boolean mIsVisible;
    private boolean mIsActivated;

    private MenuItem(Builder builder) {
        mContext = builder.mContext;
        mId = builder.mId;
        mIsCheckable = builder.mIsCheckable;
        mIsActivatable = builder.mIsActivatable;
        mTitle = builder.mTitle;
        mIcon = builder.mIcon;
        mOnClickListener = builder.mOnClickListener;
        mDisplayBehavior = builder.mDisplayBehavior;
        mIsEnabled = builder.mIsEnabled;
        mIsChecked = builder.mIsChecked;
        mIsVisible = builder.mIsVisible;
        mIsActivated = builder.mIsActivated;
        mIsSearch = builder.mIsSearch;
        mShowIconAndTitle = builder.mShowIconAndTitle;
        mIsTinted = builder.mIsTinted;
        mIsPrimary = builder.mIsPrimary;
        mUxRestrictions = builder.mUxRestrictions;

        mCurrentRestrictions = CarUxRestrictionsUtil.getInstance(mContext).getCurrentRestrictions();
    }

    private void update() {
        Listener listener = mListener.get();
        if (listener != null) {
            listener.onMenuItemChanged(this);
        }
    }

    /** Sets the id, which is purely for the client to distinguish MenuItems with.  */
    public void setId(int id) {
        mId = id;
        update();
    }

    /** Gets the id, which is purely for the client to distinguish MenuItems with. */
    public int getId() {
        return mId;
    }

    /** Returns whether the MenuItem is enabled */
    public boolean isEnabled() {
        return mIsEnabled;
    }

    /** Sets whether the MenuItem is enabled */
    public void setEnabled(boolean enabled) {
        mIsEnabled = enabled;

        update();
    }

    /** Returns whether the MenuItem is checkable. If it is, it will be displayed as a switch. */
    public boolean isCheckable() {
        return mIsCheckable;
    }

    /**
     * Returns whether the MenuItem is currently checked. Only valid if {@link #isCheckable()}
     * is true.
     */
    public boolean isChecked() {
        return mIsChecked;
    }

    /**
     * Sets whether or not the MenuItem is checked.
     * @throws IllegalStateException When {@link #isCheckable()} is false.
     */
    public void setChecked(boolean checked) {
        if (!isCheckable()) {
            throw new IllegalStateException("Cannot call setChecked() on a non-checkable MenuItem");
        }

        mIsChecked = checked;

        update();
    }

    public boolean isTinted() {
        return mIsTinted;
    }

    /** Returns whether or not the MenuItem is visible */
    public boolean isVisible() {
        return mIsVisible;
    }

    /** Sets whether or not the MenuItem is visible */
    public void setVisible(boolean visible) {
        mIsVisible = visible;

        update();
    }

    /**
     * Returns whether the MenuItem is activatable. If it is, it's every click will toggle
     * the MenuItem's View to appear activated or not.
     */
    public boolean isActivatable() {
        return mIsActivatable;
    }

    /** Returns whether or not this view is selected. Toggles after every click */
    public boolean isActivated() {
        return mIsActivated;
    }

    /** Sets the MenuItem as activated and updates it's View to the activated state */
    public void setActivated(boolean activated) {
        if (!isActivatable()) {
            throw new IllegalStateException(
                    "Cannot call setActivated() on a non-activatable MenuItem");
        }

        mIsActivated = activated;

        update();
    }

    /** Gets the title of this MenuItem. */
    public CharSequence getTitle() {
        return mTitle;
    }

    /** Sets the title of this MenuItem. */
    public void setTitle(CharSequence title) {
        mTitle = title;

        update();
    }

    /** Sets the title of this MenuItem to a string resource. */
    public void setTitle(int resId) {
        setTitle(mContext.getString(resId));
    }

    /** Sets the UxRestrictions of this MenuItem. */
    public void setUxRestrictions(@CarUxRestrictions.CarUxRestrictionsInfo int uxRestrictions) {
        if (mUxRestrictions != uxRestrictions) {
            mUxRestrictions = uxRestrictions;
            update();
        }
    }

    @CarUxRestrictions.CarUxRestrictionsInfo
    public int getUxRestrictions() {
        return mUxRestrictions;
    }

    /** Gets the current {@link OnClickListener} */
    public OnClickListener getOnClickListener() {
        return mOnClickListener;
    }

    public boolean isShowingIconAndTitle() {
        return mShowIconAndTitle;
    }

    /** Sets the {@link OnClickListener} */
    public void setOnClickListener(OnClickListener listener) {
        mOnClickListener = listener;

        update();
    }

    /* package */ void setCarUxRestrictions(CarUxRestrictions restrictions) {
        boolean wasRestricted = isRestricted();
        mCurrentRestrictions = restrictions;

        if (isRestricted() != wasRestricted) {
            update();
        }
    }

    /* package */ boolean isRestricted() {
        return CarUxRestrictionsUtil.isRestricted(mUxRestrictions, mCurrentRestrictions);
    }

    /** Calls the {@link OnClickListener}. */
    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    public void performClick() {
        if (!isEnabled() || !isVisible()) {
            return;
        }

        if (isRestricted()) {
            Toast.makeText(mContext,
                    R.string.car_ui_restricted_while_driving, Toast.LENGTH_LONG).show();
            return;
        }

        if (isActivatable()) {
            setActivated(!isActivated());
        }

        if (isCheckable()) {
            setChecked(!isChecked());
        }

        if (mOnClickListener != null) {
            mOnClickListener.onClick(this);
        }
    }

    /** Gets the current {@link DisplayBehavior} */
    public DisplayBehavior getDisplayBehavior() {
        return mDisplayBehavior;
    }

    /** Gets the current Icon */
    public Drawable getIcon() {
        return mIcon;
    }

    /** Sets the Icon of this MenuItem. */
    public void setIcon(Drawable icon) {
        mIcon = icon;

        update();
    }

    /** Sets the Icon of this MenuItem to a drawable resource. */
    public void setIcon(int resId) {
        setIcon(resId == 0
                ? null
                : mContext.getDrawable(resId));
    }

    /**
     * Returns if this MenuItem is a primary MenuItem, which means it should be visually
     * distinct to indicate that.
     */
    public boolean isPrimary() {
        return mIsPrimary;
    }

    /** Returns if this is the search MenuItem, which has special behavior when searching */
    boolean isSearch() {
        return mIsSearch;
    }

    /** Builder class */
    public static final class Builder {
        private final Context mContext;

        private String mSearchTitle;
        private String mSettingsTitle;
        private Drawable mSearchIcon;
        private Drawable mSettingsIcon;

        private int mId = View.NO_ID;
        private CharSequence mTitle;
        private Drawable mIcon;
        private OnClickListener mOnClickListener;
        private DisplayBehavior mDisplayBehavior = DisplayBehavior.ALWAYS;
        private boolean mIsTinted = true;
        private boolean mShowIconAndTitle = false;
        private boolean mIsEnabled = true;
        private boolean mIsCheckable = false;
        private boolean mIsChecked = false;
        private boolean mIsVisible = true;
        private boolean mIsActivatable = false;
        private boolean mIsActivated = false;
        private boolean mIsSearch = false;
        private boolean mIsSettings = false;
        private boolean mIsPrimary = false;
        @CarUxRestrictions.CarUxRestrictionsInfo
        private int mUxRestrictions = CarUxRestrictions.UX_RESTRICTIONS_BASELINE;

        public Builder(Context c) {
            // Must use getApplicationContext to avoid leaking activities when the MenuItem
            // is held onto for longer than the Activity's lifecycle
            mContext = c.getApplicationContext();
        }

        /** Builds a {@link MenuItem} from the current state of the Builder */
        public MenuItem build() {
            if (mIsActivatable && (mShowIconAndTitle || mIcon == null)) {
                throw new IllegalStateException("Only simple icons can be activatable");
            }
            if (mIsCheckable && (mShowIconAndTitle || mIsActivatable)) {
                throw new IllegalStateException("Unsupported options for a checkable MenuItem");
            }
            if (mIsSearch && mIsSettings) {
                throw new IllegalStateException("Can't have both a search and settings MenuItem");
            }
            if (mIsActivatable && mDisplayBehavior == DisplayBehavior.NEVER) {
                throw new IllegalStateException("Activatable MenuItems not supported as Overflow");
            }

            if (mIsSearch && (!mSearchTitle.contentEquals(mTitle)
                    || !mSearchIcon.equals(mIcon)
                    || mIsCheckable
                    || mIsActivatable
                    || !mIsTinted
                    || mShowIconAndTitle
                    || mDisplayBehavior != DisplayBehavior.ALWAYS)) {
                throw new IllegalStateException("Invalid search MenuItem");
            }

            if (mIsSettings && (!mSettingsTitle.contentEquals(mTitle)
                    || !mSettingsIcon.equals(mIcon)
                    || mIsCheckable
                    || mIsActivatable
                    || !mIsTinted
                    || mShowIconAndTitle
                    || mDisplayBehavior != DisplayBehavior.ALWAYS)) {
                throw new IllegalStateException("Invalid settings MenuItem");
            }

            return new MenuItem(this);
        }

        /** Sets the id, which is purely for the client to distinguish MenuItems with. */
        public Builder setId(int id) {
            mId = id;
            return this;
        }

        /** Sets the title to a string resource id */
        public Builder setTitle(int resId) {
            setTitle(mContext.getString(resId));
            return this;
        }

        /** Sets the title */
        public Builder setTitle(CharSequence title) {
            mTitle = title;
            return this;
        }

        /**
         * Sets the icon to a drawable resource id.
         *
         * <p>The icon's color and size will be changed to match the other MenuItems.
         */
        public Builder setIcon(int resId) {
            mIcon = resId == 0
                    ? null
                    : mContext.getDrawable(resId);
            return this;
        }

        /**
         * Sets the icon to a drawable.
         *
         * <p>The icon's color and size will be changed to match the other MenuItems.
         */
        public Builder setIcon(Drawable icon) {
            mIcon = icon;
            return this;
        }

        /**
         * Sets whether to tint the icon, true by default.
         *
         * <p>Try not to use this, it should only be used if the MenuItem is displaying some
         * kind of logo or avatar and should be colored.
         */
        public Builder setTinted(boolean tinted) {
            mIsTinted = tinted;
            return this;
        }

        /** Sets whether the MenuItem is visible or not. Default true. */
        public Builder setVisible(boolean visible) {
            mIsVisible = visible;
            return this;
        }

        /**
         * Makes the MenuItem activatable, which means it will toggle it's visual state after
         * every click.
         */
        public Builder setActivatable() {
            mIsActivatable = true;
            return this;
        }

        /**
         * Sets whether or not the MenuItem is selected. If it is,
         * {@link View#setSelected(boolean)} will be called on its View.
         */
        public Builder setActivated(boolean activated) {
            setActivatable();
            mIsActivated = activated;
            return this;
        }

        /** Sets the {@link OnClickListener} */
        public Builder setOnClickListener(OnClickListener listener) {
            mOnClickListener = listener;
            return this;
        }

        /**
         * Used to show both the icon and title when displayed on the toolbar. If this
         * is false, only the icon while be displayed when the MenuItem is in the toolbar
         * and only the title will be displayed when the MenuItem is in the overflow menu.
         *
         * <p>Defaults to false.
         */
        public Builder setShowIconAndTitle(boolean showIconAndTitle) {
            mShowIconAndTitle = showIconAndTitle;
            return this;
        }

        /**
         * Sets the {@link DisplayBehavior}.
         *
         * <p>If the DisplayBehavior is {@link DisplayBehavior#NEVER}, the MenuItem must not be
         * {@link #setCheckable() checkable}.
         */
        public Builder setDisplayBehavior(DisplayBehavior behavior) {
            mDisplayBehavior = behavior;
            return this;
        }

        /** Sets whether the MenuItem is enabled or not. Default true. */
        public Builder setEnabled(boolean enabled) {
            mIsEnabled = enabled;
            return this;
        }

        /**
         * Makes the MenuItem checkable, meaning it will be displayed as a
         * switch.
         *
         * <p>The MenuItem is not checkable by default.
         */
        public Builder setCheckable() {
            mIsCheckable = true;
            return this;
        }

        /**
         * Sets whether the MenuItem is checked or not. This will imply {@link #setCheckable()}.
         */
        public Builder setChecked(boolean checked) {
            setCheckable();
            mIsChecked = checked;
            return this;
        }

        /**
         * Sets whether the MenuItem is primary. This is just a visual change.
         */
        public Builder setPrimary(boolean primary) {
            mIsPrimary = primary;
            return this;
        }

        /**
         * Sets under what {@link android.car.drivingstate.CarUxRestrictions.CarUxRestrictionsInfo}
         * the MenuItem should be restricted.
         */
        public Builder setUxRestrictions(
                @CarUxRestrictions.CarUxRestrictionsInfo int restrictions) {
            mUxRestrictions = restrictions;
            return this;
        }

        /**
         * Creates a search MenuItem.
         *
         * <p>The advantage of using this over creating your own is getting an OEM-styled search
         * icon, and this button will always disappear while searching, even when the
         * {@link Toolbar Toolbar's} showMenuItemsWhileSearching is true.
         *
         * <p>If using this, you should only change the id, visibility, or onClickListener.
         */
        public Builder setToSearch() {
            mSearchTitle = mContext.getString(R.string.car_ui_toolbar_menu_item_search_title);
            mSearchIcon = mContext.getDrawable(R.drawable.car_ui_icon_search);
            mIsSearch = true;
            setTitle(mSearchTitle);
            setIcon(mSearchIcon);
            return this;
        }

        /**
         * Creates a settings MenuItem.
         *
         * <p>The advantage of this over creating your own is getting an OEM-styled settings icon,
         * and that the MenuItem will be restricted based on
         * {@link CarUxRestrictions#UX_RESTRICTIONS_NO_SETUP}
         *
         * <p>If using this, you should only change the id, visibility, or onClickListener.
         */
        public Builder setToSettings() {
            mSettingsTitle = mContext.getString(R.string.car_ui_toolbar_menu_item_settings_title);
            mSettingsIcon = mContext.getDrawable(R.drawable.car_ui_icon_settings);
            mIsSettings = true;
            setTitle(mSettingsTitle);
            setIcon(mSettingsIcon);
            setUxRestrictions(CarUxRestrictions.UX_RESTRICTIONS_NO_SETUP);
            return this;
        }

        /** @deprecated Use {@link #setToSearch()} instead. */
        @Deprecated
        public static MenuItem createSearch(Context c, OnClickListener listener) {
            return MenuItem.builder(c)
                    .setToSearch()
                    .setOnClickListener(listener)
                    .build();
        }

        /** @deprecated Use {@link #setToSettings()} instead. */
        @Deprecated
        public static MenuItem createSettings(Context c, OnClickListener listener) {
            return MenuItem.builder(c)
                    .setToSettings()
                    .setOnClickListener(listener)
                    .build();
        }
    }

    /** Get a new {@link Builder}. */
    public static Builder builder(Context context) {
        return new Builder(context);
    }

    /**
     * OnClickListener for a MenuItem.
     */
    public interface OnClickListener {
        /** Called when the MenuItem is clicked */
        void onClick(MenuItem item);
    }

    /**
     * DisplayBehavior controls how the MenuItem is presented in the Toolbar
     */
    public enum DisplayBehavior {
        /** Always show the MenuItem on the toolbar instead of the overflow menu */
        ALWAYS,
        /** Never show the MenuItem in the toolbar, always put it in the overflow menu */
        NEVER
    }

    /** Listener for {@link Toolbar} to update when this MenuItem changes */
    interface Listener {
        /** Called when the MenuItem is changed. For use only by {@link Toolbar} */
        void onMenuItemChanged(MenuItem item);
    }

    /**
     * Sets a listener for changes to this MenuItem. Note that the MenuItem will only hold
     * weak references to the Listener, so that the listener is not held if the MenuItem
     * outlives the toolbar.
     */
    void setListener(Listener listener) {
        mListener = new WeakReference<>(listener);
    }
}
