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

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.XmlRes;

import com.android.car.ui.R;
import com.android.car.ui.imewidescreen.CarUiImeSearchListItem;
import com.android.car.ui.recyclerview.CarUiListItem;

import java.util.List;

/**
 * A toolbar for Android Automotive OS apps.
 *
 * <p>This isn't a toolbar in the android framework sense, it's merely a custom view that can be
 * added to a layout. (You can't call
 * {@link android.app.Activity#setActionBar(android.widget.Toolbar)} with it)
 *
 * <p>The toolbar supports a navigation button, title, tabs, search, and {@link MenuItem MenuItems}
 *
 * @deprecated Instead of creating this class, use Theme.CarUi.WithToolbar, and get access to it
 *             via {@link com.android.car.ui.core.CarUi#requireToolbar(android.app.Activity)}
 */
@Deprecated
public final class Toolbar extends FrameLayout implements ToolbarController {

    /** Callback that will be issued whenever the height of toolbar is changed. */
    public interface OnHeightChangedListener {
        /**
         * Will be called when the height of the toolbar is changed.
         *
         * @param height new height of the toolbar
         */
        void onHeightChanged(int height);
    }

    /** Back button listener */
    public interface OnBackListener {
        /**
         * Invoked when the user clicks on the back button. By default, the toolbar will call
         * the Activity's {@link android.app.Activity#onBackPressed()}. Returning true from
         * this method will absorb the back press and prevent that behavior.
         */
        boolean onBack();
    }

    /** Tab selection listener */
    public interface OnTabSelectedListener {
        /** Called when a {@link TabLayout.Tab} is selected */
        void onTabSelected(TabLayout.Tab tab);
    }

    /** Search listener */
    public interface OnSearchListener {
        /**
         * Invoked when the user edits a search query.
         *
         * <p>This is called for every letter the user types, and also empty strings if the user
         * erases everything.
         */
        void onSearch(String query);
    }

    /** Search completed listener */
    public interface OnSearchCompletedListener {
        /**
         * Invoked when the user submits a search query by clicking the keyboard's search / done
         * button.
         */
        void onSearchCompleted();
    }

    private static final String TAG = "CarUiToolbar";

    /** Enum of states the toolbar can be in. Controls what elements of the toolbar are displayed */
    public enum State {
        /**
         * In the HOME state, the logo will be displayed if there is one, and no navigation icon
         * will be displayed. The tab bar will be visible. The title will be displayed if there
         * is space. MenuItems will be displayed.
         */
        HOME,
        /**
         * In the SUBPAGE state, the logo will be replaced with a back button, the tab bar won't
         * be visible. The title and MenuItems will be displayed.
         */
        SUBPAGE,
        /**
         * In the SEARCH state, only the back button and the search bar will be visible.
         */
        SEARCH,
        /**
         * In the EDIT state, the search bar will look like a regular text box, but will be
         * functionally identical to the SEARCH state.
         */
        EDIT,
    }

    private ToolbarControllerImpl mController;
    private boolean mEatingTouch = false;
    private boolean mEatingHover = false;

    public Toolbar(Context context) {
        this(context, null);
    }

    public Toolbar(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.CarUiToolbarStyle);
    }

    public Toolbar(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public Toolbar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(getToolbarLayout(), this, true);

        mController = new ToolbarControllerImpl(this);

        TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.CarUiToolbar, defStyleAttr, defStyleRes);

        try {
            setShowTabsInSubpage(a.getBoolean(R.styleable.CarUiToolbar_showTabsInSubpage, false));
            setTitle(a.getString(R.styleable.CarUiToolbar_title));
            setLogo(a.getResourceId(R.styleable.CarUiToolbar_logo, 0));
            setBackgroundShown(a.getBoolean(R.styleable.CarUiToolbar_showBackground, true));
            setMenuItems(a.getResourceId(R.styleable.CarUiToolbar_menuItems, 0));
            String searchHint = a.getString(R.styleable.CarUiToolbar_searchHint);
            if (searchHint != null) {
                setSearchHint(searchHint);
            }

            switch (a.getInt(R.styleable.CarUiToolbar_car_ui_state, 0)) {
                case 0:
                    setState(State.HOME);
                    break;
                case 1:
                    setState(State.SUBPAGE);
                    break;
                case 2:
                    setState(State.SEARCH);
                    break;
                default:
                    if (Log.isLoggable(TAG, Log.WARN)) {
                        Log.w(TAG, "Unknown initial state");
                    }
                    break;
            }

            switch (a.getInt(R.styleable.CarUiToolbar_car_ui_navButtonMode, 0)) {
                case 0:
                    setNavButtonMode(NavButtonMode.BACK);
                    break;
                case 1:
                    setNavButtonMode(NavButtonMode.CLOSE);
                    break;
                case 2:
                    setNavButtonMode(NavButtonMode.DOWN);
                    break;
                default:
                    if (Log.isLoggable(TAG, Log.WARN)) {
                        Log.w(TAG, "Unknown navigation button style");
                    }
                    break;
            }
        } finally {
            a.recycle();
        }
    }

    /**
     * Override this in a subclass to allow for different toolbar layouts within a single app.
     *
     * <p>Non-system apps should not use this, as customising the layout isn't possible with RROs
     */
    protected int getToolbarLayout() {
        if (getContext().getResources().getBoolean(
                R.bool.car_ui_toolbar_tabs_on_second_row)) {
            return R.layout.car_ui_toolbar_two_row;
        }

        return R.layout.car_ui_toolbar;
    }

    /**
     * Returns {@code true} if a two row layout in enabled for the toolbar.
     */
    @Override
    public boolean isTabsInSecondRow() {
        return mController.isTabsInSecondRow();
    }

    /**
     * Sets the title of the toolbar to a string resource.
     *
     * <p>The title may not always be shown, for example with one row layout with tabs.
     */
    @Override
    public void setTitle(@StringRes int title) {
        mController.setTitle(title);
    }

    /**
     * Sets the title of the toolbar to a CharSequence.
     *
     * <p>The title may not always be shown, for example with one row layout with tabs.
     */
    @Override
    public void setTitle(CharSequence title) {
        mController.setTitle(title);
    }

    @Override
    public CharSequence getTitle() {
        return mController.getTitle();
    }

    /**
     * Sets the subtitle of the toolbar to a string resource.
     *
     * <p>The title may not always be shown, for example with one row layout with tabs.
     */
    @Override
    public void setSubtitle(@StringRes int title) {
        mController.setSubtitle(title);
    }

    /**
     * Sets the subtitle of the toolbar to a CharSequence.
     *
     * <p>The title may not always be shown, for example with one row layout with tabs.
     */
    @Override
    public void setSubtitle(CharSequence title) {
        mController.setSubtitle(title);
    }

    @Override
    public CharSequence getSubtitle() {
        return mController.getSubtitle();
    }

    /**
     * Gets the {@link TabLayout} for this toolbar.
     *
     * @deprecated Use other tab-related functions in the ToolbarController interface.
     */
    @Deprecated
    @Override
    public TabLayout getTabLayout() {
        return mController.getTabLayout();
    }

    /**
     * Gets the number of tabs in the toolbar. The tabs can be retrieved using
     * {@link #getTab(int)}.
     */
    @Override
    public int getTabCount() {
        return mController.getTabCount();
    }

    /**
     * Gets the index of the tab.
     */
    @Override
    public int getTabPosition(TabLayout.Tab tab) {
        return mController.getTabPosition(tab);
    }

    /**
     * Adds a tab to this toolbar. You can listen for when it is selected via
     * {@link #registerOnTabSelectedListener(OnTabSelectedListener)}.
     */
    @Override
    public void addTab(TabLayout.Tab tab) {
        mController.addTab(tab);
    }

    /** Removes all the tabs. */
    @Override
    public void clearAllTabs() {
        mController.clearAllTabs();
    }

    /**
     * Gets a tab added to this toolbar. See
     * {@link #addTab(TabLayout.Tab)}.
     */
    @Override
    public TabLayout.Tab getTab(int position) {
        return mController.getTab(position);
    }

    /**
     * Selects a tab added to this toolbar. See
     * {@link #addTab(TabLayout.Tab)}.
     */
    @Override
    public void selectTab(int position) {
        mController.selectTab(position);
    }

    /**
     * Sets whether or not tabs should also be shown in the SUBPAGE {@link State}.
     */
    @Override
    public void setShowTabsInSubpage(boolean showTabs) {
        mController.setShowTabsInSubpage(showTabs);
    }

    /**
     * Gets whether or not tabs should also be shown in the SUBPAGE {@link State}.
     */
    @Override
    public boolean getShowTabsInSubpage() {
        return mController.getShowTabsInSubpage();
    }

    /**
     * Sets the logo to display in this toolbar. If navigation icon is being displayed, this logo
     * will be displayed next to the title.
     */
    @Override
    public void setLogo(@DrawableRes int resId) {
        mController.setLogo(resId);
    }

    /**
     * Sets the logo to display in this toolbar. If navigation icon is being displayed, this logo
     * will be displayed next to the title.
     */
    @Override
    public void setLogo(Drawable drawable) {
        mController.setLogo(drawable);
    }

    /** Sets the hint for the search bar. */
    @Override
    public void setSearchHint(@StringRes int resId) {
        mController.setSearchHint(resId);
    }

    /** Sets the hint for the search bar. */
    @Override
    public void setSearchHint(CharSequence hint) {
        mController.setSearchHint(hint);
    }

    /** Gets the search hint */
    @Override
    public CharSequence getSearchHint() {
        return mController.getSearchHint();
    }

    /**
     * Sets the icon to display in the search box.
     *
     * <p>The icon will be lost on configuration change, make sure to set it in onCreate() or
     * a similar place.
     */
    @Override
    public void setSearchIcon(@DrawableRes int resId) {
        mController.setSearchIcon(resId);
    }

    /**
     * Sets the icon to display in the search box.
     *
     * <p>The icon will be lost on configuration change, make sure to set it in onCreate() or
     * a similar place.
     */
    @Override
    public void setSearchIcon(Drawable d) {
        mController.setSearchIcon(d);
    }

    /**
     * An enum of possible styles the nav button could be in. All styles will still call
     * {@link OnBackListener#onBack()}.
     */
    public enum NavButtonMode {
        /** A back button */
        BACK,
        /** A close button */
        CLOSE,
        /** A down button, used to indicate that the page will animate down when navigating away */
        DOWN
    }

    /** Sets the {@link NavButtonMode} */
    @Override
    public void setNavButtonMode(NavButtonMode style) {
        mController.setNavButtonMode(style);
    }

    /** Gets the {@link NavButtonMode} */
    @Override
    public NavButtonMode getNavButtonMode() {
        return mController.getNavButtonMode();
    }

    /**
     * setBackground is disallowed, to prevent apps from deviating from the intended style too much.
     */
    @Override
    public void setBackground(Drawable d) {
        throw new UnsupportedOperationException(
                "You can not change the background of a CarUi toolbar, use "
                        + "setBackgroundShown(boolean) or an RRO instead.");
    }

    /** Show/hide the background. When hidden, the toolbar is completely transparent. */
    @Override
    public void setBackgroundShown(boolean shown) {
        mController.setBackgroundShown(shown);
    }

    /** Returns true is the toolbar background is shown */
    @Override
    public boolean getBackgroundShown() {
        return mController.getBackgroundShown();
    }

    /**
     * Sets the {@link MenuItem Menuitems} to display.
     */
    @Override
    public void setMenuItems(@Nullable List<MenuItem> items) {
        mController.setMenuItems(items);
    }

    /**
     * Sets the {@link MenuItem Menuitems} to display to a list defined in XML.
     *
     * <p>If this method is called twice with the same argument (and {@link #setMenuItems(List)}
     * wasn't called), nothing will happen the second time, even if the MenuItems were changed.
     *
     * <p>The XML file must have one <MenuItems> tag, with a variable number of <MenuItem>
     * child tags. See CarUiToolbarMenuItem in CarUi's attrs.xml for a list of available attributes.
     *
     * Example:
     * <pre>
     * <MenuItems>
     *     <MenuItem
     *         app:title="Foo"/>
     *     <MenuItem
     *         app:title="Bar"
     *         app:icon="@drawable/ic_tracklist"
     *         app:onClick="xmlMenuItemClicked"/>
     *     <MenuItem
     *         app:title="Bar"
     *         app:checkable="true"
     *         app:uxRestrictions="FULLY_RESTRICTED"
     *         app:onClick="xmlMenuItemClicked"/>
     * </MenuItems>
     * </pre>
     *
     * @return The MenuItems that were loaded from XML.
     * @see #setMenuItems(List)
     */
    @Override
    public List<MenuItem> setMenuItems(@XmlRes int resId) {
        return mController.setMenuItems(resId);
    }

    /** Gets the {@link MenuItem MenuItems} currently displayed */
    @Override
    @NonNull
    public List<MenuItem> getMenuItems() {
        return mController.getMenuItems();
    }

    /** Gets a {@link MenuItem} by id. */
    @Override
    @Nullable
    public MenuItem findMenuItemById(int id) {
        return mController.findMenuItemById(id);
    }

    /** Gets a {@link MenuItem} by id. Will throw an exception if not found. */
    @Override
    @NonNull
    public MenuItem requireMenuItemById(int id) {
        return mController.requireMenuItemById(id);
    }

    /**
     * Set whether or not to show the {@link MenuItem MenuItems} while searching. Default false.
     * Even if this is set to true, the {@link MenuItem} created by
     * {@link MenuItem.Builder#setToSearch()} will still be hidden.
     */
    @Override
    public void setShowMenuItemsWhileSearching(boolean showMenuItems) {
        mController.setShowMenuItemsWhileSearching(showMenuItems);
    }

    /** Returns if {@link MenuItem MenuItems} are shown while searching */
    @Override
    public boolean getShowMenuItemsWhileSearching() {
        return mController.getShowMenuItemsWhileSearching();
    }

    /**
     * Sets the search query.
     */
    @Override
    public void setSearchQuery(String query) {
        mController.setSearchQuery(query);
    }

    /**
     * Sets the state of the toolbar. This will show/hide the appropriate elements of the toolbar
     * for the desired state.
     */
    @Override
    public void setState(State state) {
        mController.setState(state);
    }

    /** Gets the current {@link State} of the toolbar. */
    @Override
    public State getState() {
        return mController.getState();
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // Copied from androidx.appcompat.widget.Toolbar

        // Toolbars always eat touch events, but should still respect the touch event dispatch
        // contract. If the normal View implementation doesn't want the events, we'll just silently
        // eat the rest of the gesture without reporting the events to the default implementation
        // since that's what it expects.

        final int action = ev.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            mEatingTouch = false;
        }

        if (!mEatingTouch) {
            final boolean handled = super.onTouchEvent(ev);
            if (action == MotionEvent.ACTION_DOWN && !handled) {
                mEatingTouch = true;
            }
        }

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            mEatingTouch = false;
        }

        return true;
    }

    @Override
    public boolean onHoverEvent(MotionEvent ev) {
        // Copied from androidx.appcompat.widget.Toolbar

        // Same deal as onTouchEvent() above. Eat all hover events, but still
        // respect the touch event dispatch contract.

        final int action = ev.getActionMasked();
        if (action == MotionEvent.ACTION_HOVER_ENTER) {
            mEatingHover = false;
        }

        if (!mEatingHover) {
            final boolean handled = super.onHoverEvent(ev);
            if (action == MotionEvent.ACTION_HOVER_ENTER && !handled) {
                mEatingHover = true;
            }
        }

        if (action == MotionEvent.ACTION_HOVER_EXIT || action == MotionEvent.ACTION_CANCEL) {
            mEatingHover = false;
        }

        return true;
    }

    /**
     * Registers a new {@link OnHeightChangedListener} to the list of listeners. Register a
     * {@link com.android.car.ui.recyclerview.CarUiRecyclerView} only if there is a toolbar at
     * the top and a {@link com.android.car.ui.recyclerview.CarUiRecyclerView} in the view and
     * nothing else. {@link com.android.car.ui.recyclerview.CarUiRecyclerView} will
     * automatically adjust its height according to the height of the Toolbar.
     */
    @Override
    public void registerToolbarHeightChangeListener(
            OnHeightChangedListener listener) {
        mController.registerToolbarHeightChangeListener(listener);
    }

    /** Unregisters an existing {@link OnHeightChangedListener} from the list of listeners. */
    @Override
    public boolean unregisterToolbarHeightChangeListener(
            OnHeightChangedListener listener) {
        return mController.unregisterToolbarHeightChangeListener(listener);
    }

    /** Registers a new {@link OnTabSelectedListener} to the list of listeners. */
    @Override
    public void registerOnTabSelectedListener(OnTabSelectedListener listener) {
        mController.registerOnTabSelectedListener(listener);
    }

    /** Unregisters an existing {@link OnTabSelectedListener} from the list of listeners. */
    @Override
    public boolean unregisterOnTabSelectedListener(OnTabSelectedListener listener) {
        return mController.unregisterOnTabSelectedListener(listener);
    }

    /** Registers a new {@link OnSearchListener} to the list of listeners. */
    @Override
    public void registerOnSearchListener(OnSearchListener listener) {
        mController.registerOnSearchListener(listener);
    }

    /** Unregisters an existing {@link OnSearchListener} from the list of listeners. */
    @Override
    public boolean unregisterOnSearchListener(OnSearchListener listener) {
        return mController.unregisterOnSearchListener(listener);
    }

    /**
     * Returns true if the toolbar can display search result items. One example of this is when the
     * system is configured to display search items in the IME instead of in the app.
     */
    @Override
    public boolean canShowSearchResultItems() {
        return mController.canShowSearchResultItems();
    }

    /**
     * Returns true if the app is allowed to set search results view.
     */
    @Override
    public boolean canShowSearchResultsView() {
        return mController.canShowSearchResultsView();
    }

    /**
     * Add a view within a container that will animate with the wide screen IME to display search
     * results.
     *
     * <p>Note: Apps can only call this method if the package name is allowed via OEM to render
     * their view.  To check if the application have the permission to do so or not first call
     * {@link #canShowSearchResultsView()}. If the app is not allowed this method will throw an
     * {@link IllegalStateException}
     *
     * @param view to be added in the container.
     */
    @Override
    public void setSearchResultsView(View view) {
        mController.setSearchResultsView(view);
    }

    /**
     * Sets list of search item {@link CarUiListItem} to be displayed in the IMS
     * template. This method should be called when system is running in a wide screen mode. Apps
     * can check that by using {@link #canShowSearchResultItems()}
     * Else, this method will throw an {@link IllegalStateException}
     */
    @Override
    public void setSearchResultItems(List<? extends CarUiImeSearchListItem> searchItems) {
        mController.setSearchResultItems(searchItems);
    }

    /** Registers a new {@link OnSearchCompletedListener} to the list of listeners. */
    @Override
    public void registerOnSearchCompletedListener(OnSearchCompletedListener listener) {
        mController.registerOnSearchCompletedListener(listener);
    }

    /** Unregisters an existing {@link OnSearchCompletedListener} from the list of listeners. */
    @Override
    public boolean unregisterOnSearchCompletedListener(OnSearchCompletedListener listener) {
        return mController.unregisterOnSearchCompletedListener(listener);
    }

    /** Registers a new {@link OnBackListener} to the list of listeners. */
    @Override
    public void registerOnBackListener(OnBackListener listener) {
        mController.registerOnBackListener(listener);
    }

    /** Unregisters an existing {@link OnBackListener} from the list of listeners. */
    @Override
    public boolean unregisterOnBackListener(OnBackListener listener) {
        return mController.unregisterOnBackListener(listener);
    }

    /** Returns the progress bar */
    @Override
    public ProgressBarController getProgressBar() {
        return mController.getProgressBar();
    }
}
