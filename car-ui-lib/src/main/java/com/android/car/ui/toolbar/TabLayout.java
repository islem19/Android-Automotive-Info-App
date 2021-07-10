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

import static com.android.car.ui.utils.CarUiUtils.requireViewByRefId;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.car.ui.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Custom tab layout which supports adding tabs dynamically
 *
 * <p>It supports two layout modes:
 * <ul><li>Flexible layout which will fill the width
 * <li>Non-flexible layout which wraps content with a minimum tab width. By setting tab gravity,
 * it can left aligned, right aligned or center aligned.
 *
 * <p>Scrolling function is not supported. If a tab item runs out of the tab layout bound, there
 * is no way to access it. It's better to set the layout mode to flexible in this case.
 *
 * <p>Default tab item inflates from R.layout.car_ui_tab_item, but it also supports custom layout
 * id, by overlaying R.layout.car_ui_tab_item_layout. By doing this, appearance of tab item view
 * can be customized.
 *
 * <p>Touch feedback is using @android:attr/selectableItemBackground.
 */
public class TabLayout extends LinearLayout {

    /**
     * Listener that listens the tab selection change.
     */
    public interface Listener {
        /** Callback triggered when a tab is selected. */
        default void onTabSelected(Tab tab) {
        }

        /** Callback triggered when a tab is unselected. */
        default void onTabUnselected(Tab tab) {
        }

        /** Callback triggered when a tab is reselected. */
        default void onTabReselected(Tab tab) {
        }
    }

    private final Set<Listener> mListeners = new ArraySet<>();

    private final TabAdapter mTabAdapter;

    public TabLayout(@NonNull Context context) {
        this(context, null);
    }

    public TabLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TabLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        Resources resources = context.getResources();

        boolean tabFlexibleLayout = resources.getBoolean(R.bool.car_ui_toolbar_tab_flexible_layout);
        @LayoutRes int tabLayoutRes = tabFlexibleLayout
                ? R.layout.car_ui_toolbar_tab_item_layout_flexible
                : R.layout.car_ui_toolbar_tab_item_layout;
        mTabAdapter = new TabAdapter(context, tabLayoutRes, this);
    }

    /**
     * Add a tab to this layout. The tab will be added at the end of the list. If this is the first
     * tab to be added it will become the selected tab.
     */
    public void addTab(Tab tab) {
        mTabAdapter.add(tab);
        // If there is only one tab in the group, set it to be selected.
        if (mTabAdapter.getCount() == 1) {
            mTabAdapter.selectTab(0);
        }
    }

    /** Set the tab as the current selected tab. */
    public void selectTab(Tab tab) {
        mTabAdapter.selectTab(tab);
    }

    /** Set the tab at given position as the current selected tab. */
    public void selectTab(int position) {
        mTabAdapter.selectTab(position);
    }

    /** Returns how tab items it has. */
    public int getTabCount() {
        return mTabAdapter.getCount();
    }

    /** Returns the position of the given tab. */
    public int getTabPosition(Tab tab) {
        return mTabAdapter.getPosition(tab);
    }

    /** Return the tab at the given position. */
    public Tab get(int position) {
        return mTabAdapter.getItem(position);
    }

    /** Clear all tabs. */
    public void clearAllTabs() {
        mTabAdapter.clear();
    }

    /** Register a {@link Listener}. Same listener will only be registered once. */
    public void addListener(@NonNull Listener listener) {
        mListeners.add(listener);
    }

    /** Unregister a {@link Listener} */
    public void removeListener(@NonNull Listener listener) {
        mListeners.remove(listener);
    }

    private void dispatchOnTabSelected(Tab tab) {
        for (Listener listener : mListeners) {
            listener.onTabSelected(tab);
        }
    }

    private void dispatchOnTabUnselected(Tab tab) {
        for (Listener listener : mListeners) {
            listener.onTabUnselected(tab);
        }
    }

    private void dispatchOnTabReselected(Tab tab) {
        for (Listener listener : mListeners) {
            listener.onTabReselected(tab);
        }
    }

    private void addTabView(View tabView, int position) {
        addView(tabView, position);
    }

    private static class TabAdapter extends BaseAdapter {
        private final Context mContext;
        private final TabLayout mTabLayout;
        @LayoutRes
        private final int mTabItemLayoutRes;
        private final List<Tab> mTabList;

        private TabAdapter(Context context, @LayoutRes int res, TabLayout tabLayout) {
            mTabList = new ArrayList<>();
            mContext = context;
            mTabItemLayoutRes = res;
            mTabLayout = tabLayout;
        }

        private void add(@NonNull Tab tab) {
            mTabList.add(tab);
            notifyItemInserted(mTabList.size() - 1);
        }

        private void clear() {
            mTabList.clear();
            mTabLayout.removeAllViews();
        }

        private int getPosition(Tab tab) {
            return mTabList.indexOf(tab);
        }

        @Override
        public int getCount() {
            return mTabList.size();
        }

        @Override
        public Tab getItem(int position) {
            return mTabList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        @NonNull
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View tabItemView = LayoutInflater.from(mContext)
                    .inflate(mTabItemLayoutRes, parent, false);

            presentTabItemView(position, tabItemView);
            return tabItemView;
        }

        private void selectTab(Tab tab) {
            selectTab(getPosition(tab));
        }

        private void selectTab(int position) {
            if (position < 0 || position >= getCount()) {
                throw new IndexOutOfBoundsException("Invalid position");
            }

            for (int i = 0; i < getCount(); i++) {
                Tab tab = mTabList.get(i);
                boolean isTabSelected = position == i;
                if (tab.mIsSelected != isTabSelected) {
                    tab.mIsSelected = isTabSelected;
                    notifyItemChanged(i);
                    if (tab.mIsSelected) {
                        mTabLayout.dispatchOnTabSelected(tab);
                    } else {
                        mTabLayout.dispatchOnTabUnselected(tab);
                    }
                } else if (tab.mIsSelected) {
                    mTabLayout.dispatchOnTabReselected(tab);
                }
            }
        }

        /** Represent the tab item at given position without destroying and recreating UI. */
        private void notifyItemChanged(int position) {
            View tabItemView = mTabLayout.getChildAt(position);
            presentTabItemView(position, tabItemView);
        }

        private void notifyItemInserted(int position) {
            View insertedView = getView(position, null, mTabLayout);
            mTabLayout.addTabView(insertedView, position);
        }

        private void presentTabItemView(int position, @NonNull View tabItemView) {
            Tab tab = mTabList.get(position);

            ImageView iconView = requireViewByRefId(tabItemView, R.id.car_ui_toolbar_tab_item_icon);
            TextView textView = requireViewByRefId(tabItemView, R.id.car_ui_toolbar_tab_item_text);

            tabItemView.setOnClickListener(view -> selectTab(tab));
            tab.bindText(textView);
            tab.bindIcon(iconView);
            tabItemView.setActivated(tab.mIsSelected);
            textView.setTextAppearance(tab.mIsSelected
                    ? R.style.TextAppearance_CarUi_Widget_Toolbar_Tab_Selected
                    : R.style.TextAppearance_CarUi_Widget_Toolbar_Tab);
        }
    }

    /** Tab entity. */
    public static class Tab {
        private final Drawable mIcon;
        private final CharSequence mText;
        private boolean mIsSelected;

        public Tab(@Nullable Drawable icon, @Nullable CharSequence text) {
            mIcon = icon;
            mText = text;
        }

        /** Set tab text. */
        protected void bindText(TextView textView) {
            textView.setText(mText);
        }

        /** Set icon drawable. TODO(b/139444064): revise this api.*/
        protected void bindIcon(ImageView imageView) {
            imageView.setImageDrawable(mIcon);
        }
    }
}
