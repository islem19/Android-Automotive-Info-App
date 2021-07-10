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
package com.android.car.ui.core;

import android.app.Activity;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import com.android.car.ui.baselayout.Insets;
import com.android.car.ui.baselayout.InsetsChangedListener;
import com.android.car.ui.core.BaseLayoutController.InsetsUpdater;
import com.android.car.ui.toolbar.ToolbarController;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Public interface for general CarUi static functions.
 */
public class CarUi {

    /** Prevent instantiating this class */
    private CarUi() {}

    /**
     * Gets the {@link ToolbarController} for an activity. Requires that the Activity uses
     * Theme.CarUi.WithToolbar, or otherwise sets carUiBaseLayout and carUiToolbar to true.
     *
     * See also: {@link #requireToolbar(Activity)}
     */
    @Nullable
    public static ToolbarController getToolbar(Activity activity) {
        BaseLayoutController controller = getBaseLayoutController(activity);
        if (controller != null) {
            return controller.getToolbarController();
        }
        return null;
    }

    /**
     * Gets the {@link ToolbarController} for an activity. Requires that the Activity uses
     * Theme.CarUi.WithToolbar, or otherwise sets carUiBaseLayout and carUiToolbar to true.
     *
     * <p>See also: {@link #getToolbar(Activity)}
     *
     * @throws IllegalArgumentException When the CarUi Toolbar cannot be found.
     */
    @NonNull
    public static ToolbarController requireToolbar(Activity activity) {
        ToolbarController result = getToolbar(activity);
        if (result == null) {
            throw new IllegalArgumentException("Activity " + activity
                    + " does not have a CarUi Toolbar!"
                    + " Are you using Theme.CarUi.WithToolbar?");
        }

        return result;
    }

    /**
     * Registering a listener to receive the InsetsChanged updates instead of the Activity.
     */
    public static void replaceInsetsChangedListenerWith(Activity activity,
            InsetsChangedListener listener) {
        BaseLayoutController controller = getBaseLayoutController(activity);
        if (controller != null) {
            controller.replaceInsetsChangedListenerWith(listener);
        }
    }

    /**
     * Gets the current {@link Insets} of the given {@link Activity}. Only applies to Activities
     * using the base layout, ie have the theme attribute "carUiBaseLayout" set to true.
     *
     * <p>Note that you likely don't want to use this without also using
     * {@link com.android.car.ui.baselayout.InsetsChangedListener}, as without it the Insets
     * will automatically be applied to your Activity's content view.
     */
    @Nullable
    public static Insets getInsets(Activity activity) {
        BaseLayoutController controller = getBaseLayoutController(activity);
        if (controller != null) {
            return controller.getInsets();
        }
        return null;
    }

    /**
     * Gets the current {@link Insets} of the given {@link Activity}. Only applies to Activities
     * using the base layout, ie have the theme attribute "carUiBaseLayout" set to true.
     *
     * <p>Note that you likely don't want to use this without also using
     * {@link com.android.car.ui.baselayout.InsetsChangedListener}, as without it the Insets
     * will automatically be applied to your Activity's content view.
     *
     * @throws IllegalArgumentException When the activity is not using base layouts.
     */
    @NonNull
    public static Insets requireInsets(Activity activity) {
        Insets result = getInsets(activity);
        if (result == null) {
            throw new IllegalArgumentException("Activity " + activity
                    + " does not have a base layout!"
                    + " Are you using Theme.CarUi.WithToolbar or Theme.CarUi.NoToolbar?");
        }

        return result;
    }

    /**
     * Most apps should not use this method, but instead rely on CarUi automatically
     * installing the base layout into their activities. See {@link #requireToolbar(Activity)}.
     *
     * This method installs the base layout *around* the provided view. As a result, this view
     * must have a parent ViewGroup.
     *
     * When using this method, you can't use the other activity-based methods.
     * ({@link #requireToolbar(Activity)}, {@link #requireInsets(Activity)}, ect.)
     *
     * @param view The view to wrap inside a base layout.
     * @param hasToolbar if there should be a toolbar in the base layout.
     * @return The {@link ToolbarController}, which will be null if hasToolbar is false.
     */
    @Nullable
    public static ToolbarController installBaseLayoutAround(
            View view,
            InsetsChangedListener insetsChangedListener,
            boolean hasToolbar) {
        Pair<ToolbarController, InsetsUpdater> results =
                BaseLayoutController.installBaseLayoutAround(null, view, hasToolbar);

        Objects.requireNonNull(results.second)
                .replaceInsetsChangedListenerWith(insetsChangedListener);

        return results.first;
    }

    /* package */ static BaseLayoutController getBaseLayoutController(Activity activity) {
        if (activity.getClassLoader().equals(CarUi.class.getClassLoader())) {
            return BaseLayoutController.getBaseLayout(activity);
        } else {
            // Note: (b/156532465)
            // The usage of the alternate classloader is to accommodate GMSCore.
            // Some activities are loaded dynamically from external modules.
            try {
                Class<?> baseLayoutControllerClass = activity.getClassLoader()
                        .loadClass(BaseLayoutController.class.getName());
                Method method = baseLayoutControllerClass
                        .getDeclaredMethod("getBaseLayout", Activity.class);
                return (BaseLayoutController) method.invoke(null, activity);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
