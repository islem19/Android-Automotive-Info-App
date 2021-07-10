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

package com.android.car.ui.utils;

import static android.view.accessibility.AccessibilityNodeInfo.ACTION_FOCUS;

import static com.android.car.ui.utils.RotaryConstants.ROTARY_CONTAINER;
import static com.android.car.ui.utils.RotaryConstants.ROTARY_FOCUS_DELEGATING_CONTAINER;
import static com.android.car.ui.utils.RotaryConstants.ROTARY_HORIZONTALLY_SCROLLABLE;
import static com.android.car.ui.utils.RotaryConstants.ROTARY_VERTICALLY_SCROLLABLE;

import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.car.ui.FocusArea;
import com.android.car.ui.FocusParkingView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Utility class used by {@link com.android.car.ui.FocusArea} and {@link
 * com.android.car.ui.FocusParkingView}.
 *
 * @hide
 */
public final class ViewUtils {

    /**
     * No view is focused, the focused view is not shown, or the focused view is a FocusParkingView.
     */
    public static final int NO_FOCUS = 1;

    /** A scrollable container is focused. */
    public static final int SCROLLABLE_CONTAINER_FOCUS = 2;

    /**
     * A regular view is focused. A regular View is a View that is neither a FocusParkingView nor a
     * scrollable container.
     */
    public static final int REGULAR_FOCUS = 3;

    /**
     * An implicit default focus view (i.e., the first focusable item in a scrollable container) is
     * focused.
     */
    public static final int IMPLICIT_DEFAULT_FOCUS = 4;

    /** The {@code app:defaultFocus} view is focused. */
    public static final int DEFAULT_FOCUS = 5;

    /** The {@code android:focusedByDefault} view is focused. */
    public static final int FOCUSED_BY_DEFAULT = 6;

    /**
     * Focus level of a view. When adjusting the focus, the view with the highest focus level will
     * be focused.
     */
    @IntDef(flag = true, value = {NO_FOCUS, SCROLLABLE_CONTAINER_FOCUS, REGULAR_FOCUS,
            IMPLICIT_DEFAULT_FOCUS, DEFAULT_FOCUS, FOCUSED_BY_DEFAULT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface FocusLevel {
    }

    /** This is a utility class. */
    private ViewUtils() {
    }

    /**
     * This is a functional interface and can therefore be used as the assignment target for a
     * lambda expression or method reference.
     *
     * @param <T> the type of the input to the predicate
     */
    private interface Predicate<T> {
        /** Evaluates this predicate on the given argument. */
        boolean test(@NonNull T t);
    }

    /** Gets the ancestor FocusArea of the {@code view}, if any. Returns null if not found. */
    @Nullable
    public static FocusArea getAncestorFocusArea(@NonNull View view) {
        ViewParent parent = view.getParent();
        while (parent != null) {
            if (parent instanceof FocusArea) {
                return (FocusArea) parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

    /**
     * Gets the ancestor scrollable container of the {@code view}, if any. Returns null if not
     * found.
     */
    @Nullable
    public static ViewGroup getAncestorScrollableContainer(@Nullable View view) {
        if (view == null) {
            return null;
        }
        ViewParent parent = view.getParent();
        // A scrollable container can't contain a FocusArea, so let's return earlier if we found
        // a FocusArea.
        while (parent != null && parent instanceof ViewGroup && !(parent instanceof FocusArea)) {
            ViewGroup viewGroup = (ViewGroup) parent;
            if (isScrollableContainer(viewGroup)) {
                return viewGroup;
            }
            parent = parent.getParent();
        }
        return null;
    }

    /**
     * Focuses on the {@code view} if it can be focused.
     *
     * @return whether it was successfully focused or already focused
     */
    public static boolean requestFocus(@Nullable View view) {
        if (view == null || !canTakeFocus(view)) {
            return false;
        }
        if (view.isFocused()) {
            return true;
        }
        // Exit touch mode and focus the view. The view may not be focusable in touch mode, so we
        // need to exit touch mode before focusing it.
        return view.performAccessibilityAction(ACTION_FOCUS, /* arguments= */ null);
    }

    /**
     * Searches the {@code root}'s descendants for a view with the highest {@link FocusLevel}. If
     * the view's FocusLevel is higher than the {@code currentFocus}'s FocusLevel, focuses on the
     * view.
     *
     * @return whether the view is focused
     */
    public static boolean adjustFocus(@NonNull View root, @Nullable View currentFocus) {
        @FocusLevel int level = getFocusLevel(currentFocus);
        return adjustFocus(root, level);
    }

    /**
     * Searches the {@code root}'s descendants for a view with the highest {@link FocusLevel}. If
     * the view's FocusLevel is higher than {@code currentLevel}, focuses on the view.
     *
     * @return whether the view is focused
     */
    public static boolean adjustFocus(@NonNull View root, @FocusLevel int currentLevel) {
        if (currentLevel < FOCUSED_BY_DEFAULT && focusOnFocusedByDefaultView(root)) {
            return true;
        }
        if (currentLevel < DEFAULT_FOCUS && focusOnDefaultFocusView(root)) {
            return true;
        }
        if (currentLevel < IMPLICIT_DEFAULT_FOCUS && focusOnImplicitDefaultFocusView(root)) {
            return true;
        }
        if (currentLevel < REGULAR_FOCUS && focusOnFirstRegularView(root)) {
            return true;
        }
        if (currentLevel < SCROLLABLE_CONTAINER_FOCUS) {
            return focusOnScrollableContainer(root);
        }
        return false;
    }

    @VisibleForTesting
    @FocusLevel
    static int getFocusLevel(@Nullable View view) {
        if (view == null || view instanceof FocusParkingView || !view.isShown()) {
            return NO_FOCUS;
        }
        if (view.isFocusedByDefault()) {
            return FOCUSED_BY_DEFAULT;
        }
        if (isDefaultFocus(view)) {
            return DEFAULT_FOCUS;
        }
        if (isImplicitDefaultFocusView(view)) {
            return IMPLICIT_DEFAULT_FOCUS;
        }
        if (isScrollableContainer(view)) {
            return SCROLLABLE_CONTAINER_FOCUS;
        }
        return REGULAR_FOCUS;
    }

    /** Returns whether the {@code view} is a {@code app:defaultFocus} view. */
    private static boolean isDefaultFocus(@NonNull View view) {
        FocusArea parent = getAncestorFocusArea(view);
        return parent != null && view == parent.getDefaultFocusView();
    }

    /**
     * Returns whether the {@code view} is an implicit default focus view, i.e., the first focusable
     * item in a rotary container.
     */
    @VisibleForTesting
    static boolean isImplicitDefaultFocusView(@NonNull View view) {
        ViewGroup rotaryContainer = null;
        ViewParent parent = view.getParent();
        while (parent != null && parent instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) parent;
            if (isRotaryContainer(viewGroup)) {
                rotaryContainer = viewGroup;
                break;
            }
            parent = parent.getParent();
        }
        if (rotaryContainer == null) {
            return false;
        }
        return findFirstFocusableDescendant(rotaryContainer) == view;
    }

    private static boolean isRotaryContainer(@NonNull View view) {
        CharSequence contentDescription = view.getContentDescription();
        return TextUtils.equals(contentDescription, ROTARY_CONTAINER)
                || TextUtils.equals(contentDescription, ROTARY_VERTICALLY_SCROLLABLE)
                || TextUtils.equals(contentDescription, ROTARY_HORIZONTALLY_SCROLLABLE);
    }

    private static boolean isScrollableContainer(@NonNull View view) {
        CharSequence contentDescription = view.getContentDescription();
        return TextUtils.equals(contentDescription, ROTARY_VERTICALLY_SCROLLABLE)
                || TextUtils.equals(contentDescription, ROTARY_HORIZONTALLY_SCROLLABLE);
    }

    private static boolean isFocusDelegatingContainer(@NonNull View view) {
        CharSequence contentDescription = view.getContentDescription();
        return TextUtils.equals(contentDescription, ROTARY_FOCUS_DELEGATING_CONTAINER);
    }

    /**
     * Focuses on the first {@code app:defaultFocus} view in the view tree, if any.
     *
     * @param root the root of the view tree
     * @return whether succeeded
     */
    private static boolean focusOnDefaultFocusView(@NonNull View root) {
        View defaultFocus = findDefaultFocusView(root);
        return requestFocus(defaultFocus);
    }

    /**
     * Focuses on the first {@code android:focusedByDefault} view in the view tree, if any.
     *
     * @param root the root of the view tree
     * @return whether succeeded
     */
    private static boolean focusOnFocusedByDefaultView(@NonNull View root) {
        View focusedByDefault = findFocusedByDefaultView(root);
        return requestFocus(focusedByDefault);
    }

    /**
     * Focuses on the first implicit default focus view in the view tree, if any.
     *
     * @param root the root of the view tree
     * @return whether succeeded
     */
    private static boolean focusOnImplicitDefaultFocusView(@NonNull View root) {
        View implicitDefaultFocus = findImplicitDefaultFocusView(root);
        return requestFocus(implicitDefaultFocus);
    }

    /**
     * Tries to focus on the first focusable view in the view tree in depth first order, excluding
     * the FocusParkingView and scrollable containers. If focusing on the first such view fails,
     * keeps trying other views in depth first order until succeeds or there are no more such views.
     *
     * @param root the root of the view tree
     * @return whether succeeded
     */
    private static boolean focusOnFirstRegularView(@NonNull View root) {
        View focusedView = ViewUtils.depthFirstSearch(root,
                /* targetPredicate= */
                v -> !isScrollableContainer(v) && canTakeFocus(v) && requestFocus(v),
                /* skipPredicate= */ v -> !v.isShown());
        return focusedView != null;
    }

    /**
     * Focuses on the first scrollable container in the view tree, if any.
     *
     * @param root the root of the view tree
     * @return whether succeeded
     */
    private static boolean focusOnScrollableContainer(@NonNull View root) {
        View focusedView = ViewUtils.depthFirstSearch(root,
                /* targetPredicate= */ v -> isScrollableContainer(v) && canTakeFocus(v),
                /* skipPredicate= */ v -> !v.isShown());
        return requestFocus(focusedView);
    }

    /**
     * Searches the {@code root}'s descendants in depth first order, and returns the first
     * {@code app:defaultFocus} view that can take focus. Returns null if not found.
     */
    @Nullable
    private static View findDefaultFocusView(@NonNull View view) {
        if (!view.isShown()) {
            return null;
        }
        if (view instanceof FocusArea) {
            FocusArea focusArea = (FocusArea) view;
            View defaultFocus = focusArea.getDefaultFocusView();
            if (defaultFocus != null && canTakeFocus(defaultFocus)) {
                return defaultFocus;
            }
        } else if (view instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) view;
            for (int i = 0; i < parent.getChildCount(); i++) {
                View child = parent.getChildAt(i);
                View defaultFocus = findDefaultFocusView(child);
                if (defaultFocus != null) {
                    return defaultFocus;
                }
            }
        }
        return null;
    }

    /**
     * Searches the {@code view} and its descendants in depth first order, and returns the first
     * {@code android:focusedByDefault} view that can take focus. Returns null if not found.
     */
    @VisibleForTesting
    @Nullable
    static View findFocusedByDefaultView(@NonNull View view) {
        return depthFirstSearch(view,
                /* targetPredicate= */ v -> v.isFocusedByDefault() && canTakeFocus(v),
                /* skipPredicate= */ v -> !v.isShown());
    }

    /**
     * Searches the {@code view} and its descendants in depth first order, and returns the first
     * implicit default focus view, i.e., the first focusable item in the first rotary container.
     * Returns null if not found.
     */
    @VisibleForTesting
    @Nullable
    static View findImplicitDefaultFocusView(@NonNull View view) {
        View rotaryContainer = findRotaryContainer(view);
        return rotaryContainer == null
                ? null
                : findFirstFocusableDescendant(rotaryContainer);
    }

    /**
     * Searches the {@code view}'s descendants in depth first order, and returns the first view
     * that can take focus, or null if not found.
     */
    @VisibleForTesting
    @Nullable
    static View findFirstFocusableDescendant(@NonNull View view) {
        return depthFirstSearch(view,
                /* targetPredicate= */ v -> v != view && canTakeFocus(v),
                /* skipPredicate= */ v -> !v.isShown());
    }

    /**
     * Searches the {@code view} and its descendants in depth first order, and returns the first
     * rotary container shown on the screen. Returns null if not found.
     */
    @Nullable
    private static View findRotaryContainer(@NonNull View view) {
        return depthFirstSearch(view,
                /* targetPredicate= */ v -> isRotaryContainer(v),
                /* skipPredicate= */ v -> !v.isShown());
    }

    /**
     * Searches the {@code view} and its descendants in depth first order, skips the views that
     * match {@code skipPredicate} and their descendants, and returns the first view that matches
     * {@code targetPredicate}. Returns null if not found.
     */
    @Nullable
    private static View depthFirstSearch(@NonNull View view,
            @NonNull Predicate<View> targetPredicate,
            @Nullable Predicate<View> skipPredicate) {
        if (skipPredicate != null && skipPredicate.test(view)) {
            return null;
        }
        if (targetPredicate.test(view)) {
            return view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) view;
            for (int i = 0; i < parent.getChildCount(); i++) {
                View child = parent.getChildAt(i);
                View target = depthFirstSearch(child, targetPredicate, skipPredicate);
                if (target != null) {
                    return target;
                }
            }
        }
        return null;
    }

    /** Returns whether {@code view} can be focused. */
    private static boolean canTakeFocus(@NonNull View view) {
        boolean focusable = view.isFocusable() || isFocusDelegatingContainer(view);
        return focusable && view.isEnabled() && view.isShown()
                && view.getWidth() > 0 && view.getHeight() > 0 && view.isAttachedToWindow()
                && !(view instanceof FocusParkingView)
                // If it's a scrollable container, it can be focused only when it has no focusable
                // descendants. We focus on it so that the rotary controller can scroll it.
                && (!isScrollableContainer(view) || findFirstFocusableDescendant(view) == null);
    }
}
