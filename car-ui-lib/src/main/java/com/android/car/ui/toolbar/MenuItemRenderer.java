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

import android.app.Activity;
import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.XmlRes;
import androidx.asynclayoutinflater.view.AsyncLayoutInflater;
import androidx.core.util.Consumer;

import com.android.car.ui.R;
import com.android.car.ui.utils.CarUiUtils;
import com.android.car.ui.uxr.DrawableStateView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

class MenuItemRenderer implements MenuItem.Listener {

    private static final int[] RESTRICTED_STATE = new int[] {R.attr.state_ux_restricted};

    private final int mMenuItemIconSize;

    private Toolbar.State mToolbarState;

    private final MenuItem mMenuItem;
    private final ViewGroup mParentView;
    private View mView;
    private View mIconContainer;
    private ImageView mIconView;
    private Switch mSwitch;
    private TextView mTextView;
    private TextView mTextWithIconView;
    private boolean mIndividualClickListeners;

    MenuItemRenderer(MenuItem item, ViewGroup parentView) {
        mMenuItem = item;
        mParentView = parentView;
        mMenuItem.setListener(this);

        mMenuItemIconSize = parentView.getContext().getResources()
                .getDimensionPixelSize(R.dimen.car_ui_toolbar_menu_item_icon_size);
    }

    void setToolbarState(Toolbar.State state) {
        mToolbarState = state;

        if (mMenuItem.isSearch()) {
            updateView();
        }
    }

    void setCarUxRestrictions(CarUxRestrictions restrictions) {
        mMenuItem.setCarUxRestrictions(restrictions);
    }

    @Override
    public void onMenuItemChanged(MenuItem changedItem) {
        updateView();
    }

    void createView(Consumer<View> callback) {
        AsyncLayoutInflater inflater = new AsyncLayoutInflater(mParentView.getContext());
        @LayoutRes int layout = mMenuItem.isPrimary()
                ? R.layout.car_ui_toolbar_menu_item_primary
                : R.layout.car_ui_toolbar_menu_item;
        inflater.inflate(layout, mParentView, (View view, int resid,
                ViewGroup parent) -> {
            mView = view;

            mIconContainer =
                    requireViewByRefId(mView, R.id.car_ui_toolbar_menu_item_icon_container);
            mIconView = requireViewByRefId(mView, R.id.car_ui_toolbar_menu_item_icon);
            mSwitch = requireViewByRefId(mView, R.id.car_ui_toolbar_menu_item_switch);
            mTextView = requireViewByRefId(mView, R.id.car_ui_toolbar_menu_item_text);
            mTextWithIconView =
                    requireViewByRefId(mView, R.id.car_ui_toolbar_menu_item_text_with_icon);
            mIndividualClickListeners = mView.getContext().getResources()
                    .getBoolean(R.bool.car_ui_toolbar_menuitem_individual_click_listeners);

            updateView();
            callback.accept(mView);
        });
    }

    private void updateView() {
        if (mView == null) {
            return;
        }

        mView.setId(mMenuItem.getId());

        boolean hasIcon = mMenuItem.getIcon() != null;
        boolean hasText = !TextUtils.isEmpty(mMenuItem.getTitle());
        boolean textAndIcon = mMenuItem.isShowingIconAndTitle();
        boolean checkable = mMenuItem.isCheckable();

        if (!mMenuItem.isVisible()
                || (mMenuItem.isSearch() && mToolbarState == Toolbar.State.SEARCH)
                || (!checkable && !hasIcon && !hasText)) {
            mView.setVisibility(View.GONE);
            return;
        }
        mView.setVisibility(View.VISIBLE);
        mView.setContentDescription(mMenuItem.getTitle());

        View clickTarget;
        if (checkable) {
            mSwitch.setChecked(mMenuItem.isChecked());
            clickTarget = mSwitch;
        } else if (hasText && hasIcon && textAndIcon) {
            mMenuItem.getIcon().setBounds(0, 0, mMenuItemIconSize, mMenuItemIconSize);
            mTextWithIconView.setCompoundDrawables(mMenuItem.getIcon(), null, null, null);
            mTextWithIconView.setText(mMenuItem.getTitle());
            clickTarget = mTextWithIconView;
        } else if (hasIcon) {
            mIconView.setImageDrawable(mMenuItem.getIcon());
            clickTarget = mIconContainer;
        } else { // hasText will be true
            mTextView.setText(mMenuItem.getTitle());
            clickTarget = mTextView;
        }

        mIconContainer.setVisibility(clickTarget == mIconContainer ? View.VISIBLE : View.GONE);
        mTextView.setVisibility(clickTarget == mTextView ? View.VISIBLE : View.GONE);
        mTextWithIconView.setVisibility(clickTarget == mTextWithIconView
                ? View.VISIBLE : View.GONE);
        mSwitch.setVisibility(clickTarget == mSwitch ? View.VISIBLE : View.GONE);

        if (!mIndividualClickListeners) {
            clickTarget = mView;
        }

        if (!mMenuItem.isTinted() && hasIcon) {
            mMenuItem.getIcon().setTintList(null);
        }

        recursiveSetEnabledAndDrawableState(mView);
        mView.setActivated(mMenuItem.isActivated());

        if (mMenuItem.getOnClickListener() != null
                || mMenuItem.isCheckable()
                || mMenuItem.isActivatable()) {
            clickTarget.setOnClickListener(v -> mMenuItem.performClick());
        } else if (clickTarget == mView) {
            mView.setOnClickListener(null);
            mView.setClickable(false);
        }
    }

    private void recursiveSetEnabledAndDrawableState(View view) {
        view.setEnabled(mMenuItem.isEnabled());

        int[] drawableState = mMenuItem.isRestricted() ? RESTRICTED_STATE : null;
        if (view instanceof ImageView) {
            ((ImageView) view).setImageState(drawableState, true);
        } else if (view instanceof DrawableStateView) {
            ((DrawableStateView) view).setDrawableState(drawableState);
        }

        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = ((ViewGroup) view);
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                recursiveSetEnabledAndDrawableState(viewGroup.getChildAt(i));
            }
        }
    }

    static List<MenuItem> readMenuItemList(Context c, @XmlRes int resId) {
        if (resId == 0) {
            return new ArrayList<>();
        }

        try (XmlResourceParser parser = c.getResources().getXml(resId)) {
            AttributeSet attrs = Xml.asAttributeSet(parser);
            List<MenuItem> menuItems = new ArrayList<>();

            parser.next();
            parser.next();
            parser.require(XmlPullParser.START_TAG, null, "MenuItems");
            while (parser.next() != XmlPullParser.END_TAG) {
                menuItems.add(readMenuItem(c, parser, attrs));
            }

            return menuItems;
        } catch (XmlPullParserException | IOException e) {
            throw new RuntimeException("Unable to parse Menu Items", e);
        }
    }

    private static MenuItem readMenuItem(Context c, XmlResourceParser parser, AttributeSet attrs)
            throws XmlPullParserException, IOException {

        parser.require(XmlPullParser.START_TAG, null, "MenuItem");

        TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.CarUiToolbarMenuItem);
        try {
            int id = a.getResourceId(R.styleable.CarUiToolbarMenuItem_id, View.NO_ID);
            String title = a.getString(R.styleable.CarUiToolbarMenuItem_title);
            Drawable icon = a.getDrawable(R.styleable.CarUiToolbarMenuItem_icon);
            boolean isSearch = a.getBoolean(R.styleable.CarUiToolbarMenuItem_search, false);
            boolean isSettings = a.getBoolean(R.styleable.CarUiToolbarMenuItem_settings, false);
            boolean tinted = a.getBoolean(R.styleable.CarUiToolbarMenuItem_tinted, true);
            boolean visible = a.getBoolean(R.styleable.CarUiToolbarMenuItem_visible, true);
            boolean showIconAndTitle = a.getBoolean(
                    R.styleable.CarUiToolbarMenuItem_showIconAndTitle, false);
            boolean checkable = a.getBoolean(R.styleable.CarUiToolbarMenuItem_checkable, false);
            boolean checked = a.getBoolean(R.styleable.CarUiToolbarMenuItem_checked, false);
            boolean checkedExists = a.hasValue(R.styleable.CarUiToolbarMenuItem_checked);
            boolean activatable = a.getBoolean(R.styleable.CarUiToolbarMenuItem_activatable, false);
            boolean activated = a.getBoolean(R.styleable.CarUiToolbarMenuItem_activated, false);
            boolean activatedExists = a.hasValue(R.styleable.CarUiToolbarMenuItem_activated);
            int displayBehaviorInt = a.getInt(R.styleable.CarUiToolbarMenuItem_displayBehavior, 0);
            int uxRestrictions = a.getInt(R.styleable.CarUiToolbarMenuItem_uxRestrictions, 0);
            String onClickMethod = a.getString(R.styleable.CarUiToolbarMenuItem_onClick);
            MenuItem.OnClickListener onClickListener = null;

            if (onClickMethod != null) {
                Activity activity = CarUiUtils.getActivity(c);
                if (activity == null) {
                    throw new RuntimeException("Couldn't find an activity for the MenuItem");
                }

                try {
                    Method m = activity.getClass().getMethod(onClickMethod, MenuItem.class);
                    onClickListener = i -> {
                        try {
                            m.invoke(activity, i);
                        } catch (InvocationTargetException | IllegalAccessException e) {
                            throw new RuntimeException("Couldn't call the MenuItem's listener", e);
                        }
                    };
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException("OnClick method "
                            + onClickMethod + "(MenuItem) not found in your activity", e);
                }
            }

            MenuItem.DisplayBehavior displayBehavior = displayBehaviorInt == 0
                    ? MenuItem.DisplayBehavior.ALWAYS
                    : MenuItem.DisplayBehavior.NEVER;

            parser.next();
            parser.require(XmlPullParser.END_TAG, null, "MenuItem");

            MenuItem.Builder builder = MenuItem.builder(c)
                    .setId(id)
                    .setTitle(title)
                    .setIcon(icon)
                    .setOnClickListener(onClickListener)
                    .setUxRestrictions(uxRestrictions)
                    .setTinted(tinted)
                    .setVisible(visible)
                    .setShowIconAndTitle(showIconAndTitle)
                    .setDisplayBehavior(displayBehavior);

            if (isSearch) {
                builder.setToSearch();
            }

            if (isSettings) {
                builder.setToSettings();
            }

            if (checkable || checkedExists) {
                builder.setChecked(checked);
            }

            if (activatable || activatedExists) {
                builder.setActivated(activated);
            }

            return builder.build();
        } finally {
            a.recycle();
        }
    }
}
