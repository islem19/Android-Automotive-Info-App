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

package com.android.car.ui.imewidescreen;

import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.Nullable;

import com.android.car.ui.recyclerview.CarUiContentListItem;
import com.android.car.ui.recyclerview.CarUiListItemAdapter;

/**
 * Definition of list items that can be inserted into {@link CarUiListItemAdapter}. This class is
 * used to display the search items in the template for wide screen mode.
 *
 * The class is used to pass application icon resources ids to the IME for rendering in its
 * process. Applications can also pass a unique id for each item and supplemental icon that will be
 * used by the IME to notify the application when a click action is taken on them.
 */
public class CarUiImeSearchListItem extends CarUiContentListItem {

    private int mIconResId;
    private int mSupplementalIconResId;

    public CarUiImeSearchListItem(Action action) {
        super(action);
    }

    /**
     * Sets the icon of the item. Icon must be a BitmapDrawable.
     *
     * @param icon the icon to display.
     */
    @Override
    public void setIcon(@Nullable Drawable icon) {
        if (icon instanceof BitmapDrawable || icon == null) {
            super.setIcon(icon);
            return;
        }
        throw new RuntimeException("icon should be of type BitmapDrawable");
    }

    /**
     * Sets supplemental icon to be displayed in a list item. Icon must be a BitmapDrawable.
     *
     * @param icon     the Drawable to set as the icon, or null to clear the content.
     * @param listener the callback that is invoked when the icon is clicked.
     */
    @Override
    public void setSupplementalIcon(@Nullable Drawable icon,
            @Nullable OnClickListener listener) {
        if (icon instanceof BitmapDrawable || icon == null) {
            super.setSupplementalIcon(icon, listener);
            return;
        }
        throw new RuntimeException("icon should be of type BitmapDrawable");
    }


    /**
     * Returns the icons resource of the item.
     */
    public int getIconResId() {
        return mIconResId;
    }

    /**
     * Sets the icons resource of the item.
     */
    public void setIconResId(int iconResId) {
        mIconResId = iconResId;
    }

    /**
     * Returns the supplemental icon resource id of the item.
     */
    public int getSupplementalIconResId() {
        return mSupplementalIconResId;
    }

    /**
     * Sets supplemental icon resource id.
     */
    public void setSupplementalIconResId(int supplementalIconResId) {
        mSupplementalIconResId = supplementalIconResId;
    }
}
