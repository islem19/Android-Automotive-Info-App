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

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

import static com.android.car.ui.utils.CarUiUtils.findViewByRefId;
import static com.android.car.ui.utils.CarUiUtils.getBooleanSystemProperty;
import static com.android.car.ui.utils.CarUiUtils.requireViewByRefId;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.XmlRes;

import com.android.car.ui.AlertDialogBuilder;
import com.android.car.ui.R;
import com.android.car.ui.imewidescreen.CarUiImeSearchListItem;
import com.android.car.ui.recyclerview.CarUiContentListItem;
import com.android.car.ui.recyclerview.CarUiListItem;
import com.android.car.ui.recyclerview.CarUiListItemAdapter;
import com.android.car.ui.utils.CarUiUtils;
import com.android.car.ui.utils.CarUxRestrictionsUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The implementation of {@link ToolbarController}. This class takes a ViewGroup, and looks
 * in the ViewGroup to find all the toolbar-related views to control.
 */
public final class ToolbarControllerImpl implements ToolbarController {
    private static final String TAG = "CarUiToolbarController";

    @Nullable
    private View mBackground;
    private ImageView mNavIcon;
    private ImageView mLogoInNavIconSpace;
    private ViewGroup mNavIconContainer;
    private ViewGroup mTitleContainer;
    private TextView mTitle;
    private TextView mSubtitle;
    private ImageView mTitleLogo;
    private ViewGroup mTitleLogoContainer;
    private TabLayout mTabLayout;
    private ViewGroup mMenuItemsContainer;
    private FrameLayout mSearchViewContainer;
    private SearchView mSearchView;


    // Cached values that we will send to views when they are inflated
    private CharSequence mSearchHint;
    private Drawable mSearchIcon;
    private String mSearchQuery;
    private final Context mContext;
    private final Set<Toolbar.OnSearchListener> mOnSearchListeners = new HashSet<>();
    private final Set<Toolbar.OnSearchCompletedListener> mOnSearchCompletedListeners =
            new HashSet<>();

    private final Set<Toolbar.OnBackListener> mOnBackListeners = new HashSet<>();
    private final Set<Toolbar.OnTabSelectedListener> mOnTabSelectedListeners = new HashSet<>();
    private final Set<Toolbar.OnHeightChangedListener> mOnHeightChangedListeners = new HashSet<>();

    private final MenuItem mOverflowButton;
    private final boolean mIsTabsInSecondRow;
    private boolean mShowTabsInSubpage = false;
    private boolean mHasLogo = false;
    private boolean mShowMenuItemsWhileSearching;
    private Toolbar.State mState = Toolbar.State.HOME;
    private Toolbar.NavButtonMode mNavButtonMode = Toolbar.NavButtonMode.BACK;
    @NonNull
    private List<MenuItem> mMenuItems = Collections.emptyList();
    private List<MenuItem> mOverflowItems = new ArrayList<>();
    private final List<CarUiListItem> mUiOverflowItems = new ArrayList<>();
    private final CarUiListItemAdapter mOverflowAdapter;
    private final List<MenuItemRenderer> mMenuItemRenderers = new ArrayList<>();
    private View[] mMenuItemViews;
    private int mMenuItemsXmlId = 0;
    private AlertDialog mOverflowDialog;
    private boolean mNavIconSpaceReserved;
    private boolean mLogoFillsNavIconSpace;
    private View mViewForContentAreaInWideScreenMode;
    private boolean mShowLogo;
    private List<? extends CarUiImeSearchListItem> mSearchItems;
    private final ProgressBarController mProgressBar;
    private final MenuItem.Listener mOverflowItemListener = item -> {
        updateOverflowDialog(item);
        setState(getState());
    };

    // Despite the warning, this has to be a field so it's not garbage-collected.
    // The only other reference to it is a weak reference
    private final CarUxRestrictionsUtil.OnUxRestrictionsChangedListener
            mOnUxRestrictionsChangedListener = restrictions -> {
                for (MenuItemRenderer renderer : mMenuItemRenderers) {
                    renderer.setCarUxRestrictions(restrictions);
                }
            };


    public ToolbarControllerImpl(View view) {
        mContext = view.getContext();
        mOverflowButton = MenuItem.builder(getContext())
                .setIcon(R.drawable.car_ui_icon_overflow_menu)
                .setTitle(R.string.car_ui_toolbar_menu_item_overflow_title)
                .setOnClickListener(v -> {
                    if (mOverflowDialog == null) {
                        if (Log.isLoggable(TAG, Log.ERROR)) {
                            Log.e(TAG, "Overflow dialog was null when trying to show it!");
                        }
                    } else {
                        mOverflowDialog.show();
                    }
                })
                .build();

        mIsTabsInSecondRow = getContext().getResources().getBoolean(
                R.bool.car_ui_toolbar_tabs_on_second_row);
        mNavIconSpaceReserved = getContext().getResources().getBoolean(
                R.bool.car_ui_toolbar_nav_icon_reserve_space);
        mLogoFillsNavIconSpace = getContext().getResources().getBoolean(
                R.bool.car_ui_toolbar_logo_fills_nav_icon_space);
        mShowLogo = getContext().getResources().getBoolean(
                R.bool.car_ui_toolbar_show_logo);
        mSearchHint = getContext().getString(R.string.car_ui_toolbar_default_search_hint);

        mBackground = findViewByRefId(view, R.id.car_ui_toolbar_background);
        mTabLayout = requireViewByRefId(view, R.id.car_ui_toolbar_tabs);
        mNavIcon = requireViewByRefId(view, R.id.car_ui_toolbar_nav_icon);
        mLogoInNavIconSpace = requireViewByRefId(view, R.id.car_ui_toolbar_logo);
        mNavIconContainer = requireViewByRefId(view, R.id.car_ui_toolbar_nav_icon_container);
        mMenuItemsContainer = requireViewByRefId(view, R.id.car_ui_toolbar_menu_items_container);
        mTitleContainer = requireViewByRefId(view, R.id.car_ui_toolbar_title_container);
        mSubtitle = requireViewByRefId(view, R.id.car_ui_toolbar_subtitle);
        mTitle = requireViewByRefId(view, R.id.car_ui_toolbar_title);
        mTitleLogoContainer = requireViewByRefId(view, R.id.car_ui_toolbar_title_logo_container);
        mTitleLogo = requireViewByRefId(view, R.id.car_ui_toolbar_title_logo);
        mSearchViewContainer = requireViewByRefId(view, R.id.car_ui_toolbar_search_view_container);
        mProgressBar = new ProgressBarControllerImpl(
                requireViewByRefId(view, R.id.car_ui_toolbar_progress_bar));

        mTabLayout.addListener(new TabLayout.Listener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                for (Toolbar.OnTabSelectedListener listener : mOnTabSelectedListeners) {
                    listener.onTabSelected(tab);
                }
            }
        });

        if (mBackground != null) {
            mBackground.addOnLayoutChangeListener((v, left, top, right, bottom,
                    oldLeft, oldTop, oldRight, oldBottom) -> {
                if (oldBottom - oldTop != bottom - top) {
                    for (Toolbar.OnHeightChangedListener listener : mOnHeightChangedListeners) {
                        listener.onHeightChanged(mBackground.getHeight());
                    }
                }
            });
        }

        setBackgroundShown(true);

        mOverflowAdapter = new CarUiListItemAdapter(mUiOverflowItems);

        // This holds weak references so we don't need to unregister later
        CarUxRestrictionsUtil.getInstance(getContext())
                .register(mOnUxRestrictionsChangedListener);
    }

    private Context getContext() {
        return mContext;
    }

    /**
     * Returns {@code true} if a two row layout in enabled for the toolbar.
     */
    @Override
    public boolean isTabsInSecondRow() {
        return mIsTabsInSecondRow;
    }

    /**
     * Sets the title of the toolbar to a string resource.
     *
     * <p>The title may not always be shown, for example with one row layout with tabs.
     */
    @Override
    public void setTitle(@StringRes int title) {
        mTitle.setText(title);
        setState(getState());
    }

    /**
     * Sets the title of the toolbar to a CharSequence.
     *
     * <p>The title may not always be shown, for example with one row layout with tabs.
     */
    @Override
    public void setTitle(CharSequence title) {
        mTitle.setText(title);
        setState(getState());
    }

    @Override
    public CharSequence getTitle() {
        return mTitle.getText();
    }

    /**
     * Sets the subtitle of the toolbar to a string resource.
     *
     * <p>The title may not always be shown, for example with one row layout with tabs.
     */
    @Override
    public void setSubtitle(@StringRes int title) {
        mSubtitle.setText(title);
        setState(getState());
    }

    /**
     * Sets the subtitle of the toolbar to a CharSequence.
     *
     * <p>The title may not always be shown, for example with one row layout with tabs.
     */
    @Override
    public void setSubtitle(CharSequence title) {
        mSubtitle.setText(title);
        setState(getState());
    }

    @Override
    public CharSequence getSubtitle() {
        return mSubtitle.getText();
    }

    /**
     * Gets the {@link TabLayout} for this toolbar.
     *
     * @deprecated Use other tab-related functions in the ToolbarController interface.
     */
    @Deprecated
    @Override
    public TabLayout getTabLayout() {
        return mTabLayout;
    }

    /**
     * Gets the number of tabs in the toolbar. The tabs can be retrieved using
     * {@link #getTab(int)}.
     */
    @Override
    public int getTabCount() {
        return mTabLayout.getTabCount();
    }

    /**
     * Gets the index of the tab.
     */
    @Override
    public int getTabPosition(TabLayout.Tab tab) {
        return mTabLayout.getTabPosition(tab);
    }

    /**
     * Adds a tab to this toolbar. You can listen for when it is selected via
     * {@link #registerOnTabSelectedListener(Toolbar.OnTabSelectedListener)}.
     */
    @Override
    public void addTab(TabLayout.Tab tab) {
        mTabLayout.addTab(tab);
        setState(getState());
    }

    /** Removes all the tabs. */
    @Override
    public void clearAllTabs() {
        mTabLayout.clearAllTabs();
        setState(getState());
    }

    /**
     * Gets a tab added to this toolbar. See
     * {@link #addTab(TabLayout.Tab)}.
     */
    @Override
    public TabLayout.Tab getTab(int position) {
        return mTabLayout.get(position);
    }

    /**
     * Selects a tab added to this toolbar. See
     * {@link #addTab(TabLayout.Tab)}.
     */
    @Override
    public void selectTab(int position) {
        mTabLayout.selectTab(position);
    }

    /**
     * Sets whether or not tabs should also be shown in the SUBPAGE {@link Toolbar.State}.
     */
    @Override
    public void setShowTabsInSubpage(boolean showTabs) {
        if (showTabs != mShowTabsInSubpage) {
            mShowTabsInSubpage = showTabs;
            setState(getState());
        }
    }

    /**
     * Gets whether or not tabs should also be shown in the SUBPAGE {@link Toolbar.State}.
     */
    @Override
    public boolean getShowTabsInSubpage() {
        return mShowTabsInSubpage;
    }

    /**
     * Sets the logo to display in this toolbar. If navigation icon is being displayed, this logo
     * will be displayed next to the title.
     */
    @Override
    public void setLogo(@DrawableRes int resId) {
        setLogo(resId != 0 ? getContext().getDrawable(resId) : null);
    }

    /**
     * Sets the logo to display in this toolbar. If navigation icon is being displayed, this logo
     * will be displayed next to the title.
     */
    @Override
    public void setLogo(Drawable drawable) {
        if (!mShowLogo) {
            // If no logo should be shown then we act as if we never received one.
            return;
        }
        if (drawable != null) {
            mLogoInNavIconSpace.setImageDrawable(drawable);
            mTitleLogo.setImageDrawable(drawable);
            mHasLogo = true;
        } else {
            mHasLogo = false;
        }
        setState(mState);
    }

    /** Sets the hint for the search bar. */
    @Override
    public void setSearchHint(@StringRes int resId) {
        setSearchHint(getContext().getString(resId));
    }

    /** Sets the hint for the search bar. */
    public void setSearchHint(CharSequence hint) {
        mSearchHint = hint;
        if (mSearchView != null) {
            mSearchView.setHint(mSearchHint);
        }
    }

    /** Gets the search hint */
    @Override
    public CharSequence getSearchHint() {
        return mSearchHint;
    }

    /**
     * Sets the icon to display in the search box.
     *
     * <p>The icon will be lost on configuration change, make sure to set it in onCreate() or
     * a similar place.
     */
    @Override
    public void setSearchIcon(@DrawableRes int resId) {
        setSearchIcon(getContext().getDrawable(resId));
    }

    /**
     * Sets the icon to display in the search box.
     *
     * <p>The icon will be lost on configuration change, make sure to set it in onCreate() or
     * a similar place.
     */
    @Override
    public void setSearchIcon(Drawable d) {
        if (!Objects.equals(d, mSearchIcon)) {
            mSearchIcon = d;
            if (mSearchView != null) {
                mSearchView.setIcon(mSearchIcon);
            }
        }
    }


    /** Sets the {@link Toolbar.NavButtonMode} */
    @Override
    public void setNavButtonMode(Toolbar.NavButtonMode style) {
        if (style != mNavButtonMode) {
            mNavButtonMode = style;
            setState(mState);
        }
    }

    /** Gets the {@link Toolbar.NavButtonMode} */
    @Override
    public Toolbar.NavButtonMode getNavButtonMode() {
        return mNavButtonMode;
    }

    /** Show/hide the background. When hidden, the toolbar is completely transparent. */
    @Override
    public void setBackgroundShown(boolean shown) {
        if (mBackground == null) {
            return;
        }

        if (shown) {
            mBackground.setBackground(
                    getContext().getDrawable(R.drawable.car_ui_toolbar_background));
        } else {
            mBackground.setBackground(null);
        }
    }

    /** Returns true is the toolbar background is shown */
    @Override
    public boolean getBackgroundShown() {
        if (mBackground == null) {
            return true;
        }

        return mBackground.getBackground() != null;
    }

    private void setMenuItemsInternal(@Nullable List<MenuItem> items) {
        if (items == null) {
            items = Collections.emptyList();
        }

        List<MenuItem> visibleMenuItems = new ArrayList<>();
        List<MenuItem> overflowItems = new ArrayList<>();
        AtomicInteger loadedMenuItems = new AtomicInteger(0);

        synchronized (this) {
            if (items.equals(mMenuItems)) {
                return;
            }

            for (MenuItem item : items) {
                if (item.getDisplayBehavior() == MenuItem.DisplayBehavior.NEVER) {
                    overflowItems.add(item);
                    item.setListener(mOverflowItemListener);
                } else {
                    visibleMenuItems.add(item);
                }
            }

            // Copy the list so that if the list is modified and setMenuItems is called again,
            // the equals() check will fail. Note that the MenuItems are not copied here.
            mMenuItems = new ArrayList<>(items);
            mOverflowItems = overflowItems;
            mMenuItemRenderers.clear();
            mMenuItemsContainer.removeAllViews();

            if (!overflowItems.isEmpty()) {
                visibleMenuItems.add(mOverflowButton);
                createOverflowDialog();
            }

            View[] menuItemViews = new View[visibleMenuItems.size()];
            mMenuItemViews = menuItemViews;

            for (int i = 0; i < visibleMenuItems.size(); ++i) {
                int index = i;
                MenuItem item = visibleMenuItems.get(i);
                MenuItemRenderer renderer = new MenuItemRenderer(item, mMenuItemsContainer);
                mMenuItemRenderers.add(renderer);
                renderer.createView(view -> {
                    synchronized (ToolbarControllerImpl.this) {
                        if (menuItemViews != mMenuItemViews) {
                            return;
                        }

                        menuItemViews[index] = view;
                        if (loadedMenuItems.addAndGet(1) == menuItemViews.length) {
                            for (View v : menuItemViews) {
                                mMenuItemsContainer.addView(v);
                            }
                        }
                    }
                });
            }
        }

        setState(mState);
    }

    /**
     * Sets the {@link MenuItem Menuitems} to display.
     */
    @Override
    public void setMenuItems(@Nullable List<MenuItem> items) {
        mMenuItemsXmlId = 0;
        setMenuItemsInternal(items);
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
        if (mMenuItemsXmlId != 0 && mMenuItemsXmlId == resId) {
            return mMenuItems;
        }

        mMenuItemsXmlId = resId;
        List<MenuItem> menuItems = MenuItemRenderer.readMenuItemList(getContext(), resId);
        setMenuItemsInternal(menuItems);
        return menuItems;
    }

    /** Gets the {@link MenuItem MenuItems} currently displayed */
    @Override
    @NonNull
    public List<MenuItem> getMenuItems() {
        return Collections.unmodifiableList(mMenuItems);
    }

    /** Gets a {@link MenuItem} by id. */
    @Override
    @Nullable
    public MenuItem findMenuItemById(int id) {
        for (MenuItem item : mMenuItems) {
            if (item.getId() == id) {
                return item;
            }
        }
        return null;
    }

    /** Gets a {@link MenuItem} by id. Will throw an IllegalArgumentException if not found. */
    @Override
    @NonNull
    public MenuItem requireMenuItemById(int id) {
        MenuItem result = findMenuItemById(id);

        if (result == null) {
            throw new IllegalArgumentException("ID does not reference a MenuItem on this Toolbar");
        }

        return result;
    }

    private int countVisibleOverflowItems() {
        int numVisibleItems = 0;
        for (MenuItem item : mOverflowItems) {
            if (item.isVisible()) {
                numVisibleItems++;
            }
        }
        return numVisibleItems;
    }

    private void createOverflowDialog() {
        mUiOverflowItems.clear();
        for (MenuItem menuItem : mOverflowItems) {
            if (menuItem.isVisible()) {
                mUiOverflowItems.add(toCarUiContentListItem(menuItem));
            }
        }

        mOverflowDialog = new AlertDialogBuilder(getContext())
                .setAdapter(mOverflowAdapter)
                .create();
    }

    private void updateOverflowDialog(MenuItem changedItem) {
        int itemIndex = mOverflowItems.indexOf(changedItem);
        if (itemIndex >= 0) {
            mUiOverflowItems.set(itemIndex, toCarUiContentListItem(changedItem));
            mOverflowAdapter.notifyItemChanged(itemIndex);
        } else {
            createOverflowDialog();
        }
    }

    private CarUiContentListItem toCarUiContentListItem(MenuItem menuItem) {
        CarUiContentListItem carUiItem;
        if (menuItem.isCheckable()) {
            carUiItem = new CarUiContentListItem(CarUiContentListItem.Action.SWITCH);
        } else {
            carUiItem = new CarUiContentListItem(CarUiContentListItem.Action.NONE);
        }
        carUiItem.setIcon(menuItem.getIcon());
        carUiItem.setActivated(menuItem.isActivated());
        carUiItem.setChecked(menuItem.isChecked());
        carUiItem.setEnabled(menuItem.isEnabled());
        carUiItem.setTitle(menuItem.getTitle());
        carUiItem.setOnItemClickedListener(item -> {
            menuItem.performClick();
            mOverflowDialog.hide();
        });
        return carUiItem;
    }

    /**
     * Set whether or not to show the {@link MenuItem MenuItems} while searching. Default false.
     * Even if this is set to true, the {@link MenuItem} created by
     * {@link MenuItem.Builder#setToSearch()} will still be hidden.
     */
    @Override
    public void setShowMenuItemsWhileSearching(boolean showMenuItems) {
        mShowMenuItemsWhileSearching = showMenuItems;
        setState(mState);
    }

    /** Returns if {@link MenuItem MenuItems} are shown while searching */
    @Override
    public boolean getShowMenuItemsWhileSearching() {
        return mShowMenuItemsWhileSearching;
    }

    /**
     * Sets the search query.
     */
    @Override
    public void setSearchQuery(String query) {
        if (mSearchView != null) {
            mSearchView.setSearchQuery(query);
        } else {
            mSearchQuery = query;
            for (Toolbar.OnSearchListener listener : mOnSearchListeners) {
                listener.onSearch(query);
            }
        }
    }

    /**
     * Sets the state of the toolbar. This will show/hide the appropriate elements of the toolbar
     * for the desired state.
     */
    @Override
    public void setState(Toolbar.State state) {
        mState = state;

        if (mSearchView == null && (state == Toolbar.State.SEARCH || state == Toolbar.State.EDIT)) {
            SearchView searchView = new SearchView(getContext());
            searchView.setHint(mSearchHint);
            searchView.setIcon(mSearchIcon);
            searchView.setSearchQuery(mSearchQuery);
            searchView.setSearchListeners(mOnSearchListeners);
            searchView.setSearchCompletedListeners(mOnSearchCompletedListeners);
            searchView.setVisibility(GONE);

            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            mSearchViewContainer.addView(searchView, layoutParams);
            if (canShowSearchResultsView()) {
                searchView.setViewToImeWideScreenSurface(mViewForContentAreaInWideScreenMode);
            }

            searchView.installWindowInsetsListener(mSearchViewContainer);

            if (mSearchItems != null) {
                searchView.setSearchItemsForWideScreen(mSearchItems);
            }

            mSearchView = searchView;
        }

        for (MenuItemRenderer renderer : mMenuItemRenderers) {
            renderer.setToolbarState(mState);
        }

        View.OnClickListener backClickListener = (v) -> {
            boolean absorbed = false;
            List<Toolbar.OnBackListener> listenersCopy = new ArrayList<>(mOnBackListeners);
            for (Toolbar.OnBackListener listener : listenersCopy) {
                absorbed = absorbed || listener.onBack();
            }

            if (!absorbed) {
                Activity activity = CarUiUtils.getActivity(getContext());
                if (activity != null) {
                    activity.onBackPressed();
                }
            }
        };

        if (state == Toolbar.State.SEARCH) {
            mNavIcon.setImageResource(R.drawable.car_ui_icon_search_nav_icon);
        } else {
            switch (mNavButtonMode) {
                case CLOSE:
                    mNavIcon.setImageResource(R.drawable.car_ui_icon_close);
                    break;
                case DOWN:
                    mNavIcon.setImageResource(R.drawable.car_ui_icon_down);
                    break;
                default:
                    mNavIcon.setImageResource(R.drawable.car_ui_icon_arrow_back);
                    break;
            }
        }

        mNavIcon.setVisibility(state != Toolbar.State.HOME ? VISIBLE : GONE);

        // Show the logo in the nav space if that's enabled, we have a logo,
        // and we're in the Home state.
        mLogoInNavIconSpace.setVisibility(mHasLogo
                && state == Toolbar.State.HOME
                && mLogoFillsNavIconSpace
                ? VISIBLE : INVISIBLE);

        // Show logo next to the title if we're in the subpage state or we're configured to not show
        // the logo in the nav icon space.
        mTitleLogoContainer.setVisibility(mHasLogo
                && (state == Toolbar.State.SUBPAGE
                || (state == Toolbar.State.HOME && !mLogoFillsNavIconSpace))
                ? VISIBLE : GONE);

        // Show the nav icon container if we're not in the home space or the logo fills the nav icon
        // container. If car_ui_toolbar_nav_icon_reserve_space is true, hiding it will still reserve
        // its space
        mNavIconContainer.setVisibility(
                state != Toolbar.State.HOME || (mHasLogo && mLogoFillsNavIconSpace)
                        ? VISIBLE : (mNavIconSpaceReserved ? INVISIBLE : GONE));
        mNavIconContainer.setOnClickListener(
                state != Toolbar.State.HOME ? backClickListener : null);
        mNavIconContainer.setClickable(state != Toolbar.State.HOME);
        mNavIconContainer.setContentDescription(state != Toolbar.State.HOME
                ? mContext.getString(R.string.car_ui_toolbar_nav_icon_content_description)
                : null);

        boolean hasTabs = mTabLayout.getTabCount() > 0
                && (state == Toolbar.State.HOME
                || (state == Toolbar.State.SUBPAGE && mShowTabsInSubpage));
        // Show the title if we're in the subpage state, or in the home state with no tabs or tabs
        // on the second row
        int visibility = (state == Toolbar.State.SUBPAGE || state == Toolbar.State.HOME)
                && (!hasTabs || mIsTabsInSecondRow)
                ? VISIBLE : GONE;
        mTitleContainer.setVisibility(visibility);
        mSubtitle.setVisibility(
                TextUtils.isEmpty(mSubtitle.getText()) ? GONE : VISIBLE);

        mTabLayout.setVisibility(hasTabs ? VISIBLE : GONE);

        if (mSearchView != null) {
            if (state == Toolbar.State.SEARCH || state == Toolbar.State.EDIT) {
                mSearchView.setPlainText(state == Toolbar.State.EDIT);
                mSearchView.setVisibility(VISIBLE);
            } else {
                mSearchView.setVisibility(GONE);
            }
        }

        boolean showButtons = (state != Toolbar.State.SEARCH && state != Toolbar.State.EDIT)
                || mShowMenuItemsWhileSearching;
        mMenuItemsContainer.setVisibility(showButtons ? VISIBLE : GONE);
        mOverflowButton.setVisible(showButtons && countVisibleOverflowItems() > 0);
    }

    /**
     * Return the list of package names allowed to hide the content area in wide screen IME.
     */
    private List<String> allowPackageList(Context context) {
        String[] packages = context.getResources()
                .getStringArray(R.array.car_ui_ime_wide_screen_allowed_package_list);
        return Arrays.asList(packages);
    }

    /**
     * Returns true if the toolbar can display search result items. One example of this is when the
     * system is configured to display search items in the IME instead of in the app.
     */
    @Override
    public boolean canShowSearchResultItems() {
        return isWideScreenMode(mContext);
    }

    /**
     * Returns whether or not system is running in a wide screen mode.
     */
    private static boolean isWideScreenMode(Context context) {
        return getBooleanSystemProperty(context.getResources(),
                R.string.car_ui_ime_wide_screen_system_property_name, false);
    }

    /**
     * Returns true if the app is allowed to set search results view.
     */
    @Override
    public boolean canShowSearchResultsView() {
        boolean allowAppsToHideContentArea = mContext.getResources().getBoolean(
                R.bool.car_ui_ime_wide_screen_allow_app_hide_content_area);
        return isWideScreenMode(mContext) && (allowPackageList(mContext).contains(
                mContext.getPackageName()) || allowAppsToHideContentArea);
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
        if (!canShowSearchResultsView()) {
            throw new IllegalStateException(
                    "not allowed to add view to wide screen IME, package name: "
                            + mContext.getPackageName());
        }

        if (mSearchView != null) {
            mSearchView.setViewToImeWideScreenSurface(view);
        }

        mViewForContentAreaInWideScreenMode = view;
    }

    /**
     * Sets list of search item {@link CarUiListItem} to be displayed in the IMS
     * template. This method should be called when system is running in a wide screen mode. Apps
     * can check that by using {@link #canShowSearchResultItems()}
     * Else, this method will throw an {@link IllegalStateException}
     */
    @Override
    public void setSearchResultItems(List<? extends CarUiImeSearchListItem> searchItems) {
        if (!canShowSearchResultItems()) {
            throw new IllegalStateException(
                    "system not in wide screen mode, not allowed to set search result items ");
        }
        mSearchItems = searchItems;
        if (mSearchView != null) {
            mSearchView.setSearchItemsForWideScreen(searchItems);
        }
    }


    /** Gets the current {@link Toolbar.State} of the toolbar. */
    @Override
    public Toolbar.State getState() {
        return mState;
    }

    /**
     * Registers a new {@link Toolbar.OnHeightChangedListener} to the list of listeners. Register a
     * {@link com.android.car.ui.recyclerview.CarUiRecyclerView} only if there is a toolbar at
     * the top and a {@link com.android.car.ui.recyclerview.CarUiRecyclerView} in the view and
     * nothing else. {@link com.android.car.ui.recyclerview.CarUiRecyclerView} will
     * automatically adjust its height according to the height of the Toolbar.
     */
    @Override
    public void registerToolbarHeightChangeListener(
            Toolbar.OnHeightChangedListener listener) {
        mOnHeightChangedListeners.add(listener);
    }

    /**
     * Unregisters an existing {@link Toolbar.OnHeightChangedListener} from the list of
     * listeners.
     */
    @Override
    public boolean unregisterToolbarHeightChangeListener(
            Toolbar.OnHeightChangedListener listener) {
        return mOnHeightChangedListeners.remove(listener);
    }

    /** Registers a new {@link Toolbar.OnTabSelectedListener} to the list of listeners. */
    @Override
    public void registerOnTabSelectedListener(Toolbar.OnTabSelectedListener listener) {
        mOnTabSelectedListeners.add(listener);
    }

    /** Unregisters an existing {@link Toolbar.OnTabSelectedListener} from the list of listeners. */
    @Override
    public boolean unregisterOnTabSelectedListener(Toolbar.OnTabSelectedListener listener) {
        return mOnTabSelectedListeners.remove(listener);
    }

    /** Registers a new {@link Toolbar.OnSearchListener} to the list of listeners. */
    @Override
    public void registerOnSearchListener(Toolbar.OnSearchListener listener) {
        mOnSearchListeners.add(listener);
    }

    /** Unregisters an existing {@link Toolbar.OnSearchListener} from the list of listeners. */
    @Override
    public boolean unregisterOnSearchListener(Toolbar.OnSearchListener listener) {
        return mOnSearchListeners.remove(listener);
    }

    /** Registers a new {@link Toolbar.OnSearchCompletedListener} to the list of listeners. */
    @Override
    public void registerOnSearchCompletedListener(Toolbar.OnSearchCompletedListener listener) {
        mOnSearchCompletedListeners.add(listener);
    }

    /**
     * Unregisters an existing {@link Toolbar.OnSearchCompletedListener} from the list of
     * listeners.
     */
    @Override
    public boolean unregisterOnSearchCompletedListener(Toolbar.OnSearchCompletedListener listener) {
        return mOnSearchCompletedListeners.remove(listener);
    }

    /** Registers a new {@link Toolbar.OnBackListener} to the list of listeners. */
    @Override
    public void registerOnBackListener(Toolbar.OnBackListener listener) {
        mOnBackListeners.add(listener);
    }

    /** Unregisters an existing {@link Toolbar.OnBackListener} from the list of listeners. */
    @Override
    public boolean unregisterOnBackListener(Toolbar.OnBackListener listener) {
        return mOnBackListeners.remove(listener);
    }

    /** Returns the progress bar */
    @Override
    public ProgressBarController getProgressBar() {
        return mProgressBar;
    }
}
