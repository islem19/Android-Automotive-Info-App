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
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.android.car.ui.R;
import com.android.car.ui.utils.CarUiUtils;

/**
 * Class used to traverse through the view hierarchy of the activity and check if carUI components
 * are being used as expected.
 *
 * To check if the activity is using the CarUI components properly, navigate to the activity and
 * run: adb shell am broadcast -a com.android.car.ui.intent.CHECK_CAR_UI_COMPONENTS. Filter
 * the logs with "CheckCarUiComponents". This is ONLY available for debug and eng builds.
 *
 * Other than using the adb command you can also set a boolean resource
 * "car_ui_escrow_check_components_automatically" to true. This will generate the logs 2 seconds
 * after launching any activity.
 */
class CheckCarUiComponents implements Application.ActivityLifecycleCallbacks {
    private static final String TAG = CheckCarUiComponents.class.getSimpleName();
    private static final String INTENT_FILTER = "com.android.car.ui.intent.CHECK_CAR_UI_COMPONENTS";
    private static final String NO_CAR_UI_RV = "CarUiRecyclerView not used:";
    private static final String NO_CAR_UI_TOOLBAR = "CarUiToolbar is not used: ";
    private static final String NO_CAR_UI_TOOLBAR_BL = "CarUiBaseLayoutToolbar is not used: ";
    private static final String NO_CAR_UI_PREFERENCE = "CarUiPreference is not used: ";
    private static final String NO_LIST_ITEM =
            "CarUiListItem are not used within CarUiRecyclerView: ";
    private View mRootView;
    private boolean mIsScreenVisible;

    private Handler mHandler = new Handler();

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            checkForComponents(context, true);
        }
    };

    CheckCarUiComponents(Context context) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(INTENT_FILTER);
        context.registerReceiver(mReceiver, filter);
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
    }

    @Override
    public void onActivityResumed(Activity activity) {
    }

    @Override
    public void onActivityPostResumed(Activity activity) {
        mRootView = activity.getWindow().getDecorView().getRootView();
        mIsScreenVisible = true;
        if (checkComponentsForAllActivities(activity)) {
            // post a message after 2 seconds delay. This is an arbitrary number so that activity
            // is ready with all its views rendered by this time.
            mHandler.postDelayed(() -> checkForComponents(activity, false), 2000);
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        mIsScreenVisible = false;
    }

    @Override
    public void onActivityStopped(Activity activity) {
        mHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        if (mRootView != null
                && CarUiUtils.getActivity(mRootView.getContext()) == activity) {
            mRootView = null;
        }
    }

    private boolean checkComponentsForAllActivities(Context context) {
        return context.getResources().getBoolean(
                R.bool.car_ui_escrow_check_components_automatically);
    }

    private void checkForComponents(Context context, boolean showToast) {
        if (!mIsScreenVisible) {
            return;
        }

        CarUiComponents carUiComponents = new CarUiComponents();
        checkForCarUiComponents(mRootView, carUiComponents);
        if (carUiComponents.mIsUsingCarUiRecyclerView
                && !carUiComponents.mIsCarUiRecyclerViewUsingListItem) {
            Log.d(TAG, NO_LIST_ITEM);
            mayShowToast(context, NO_LIST_ITEM, showToast);
        }
        if (carUiComponents.mIsUsingAndroidXRecyclerView) {
            Log.d(TAG, NO_CAR_UI_RV);
            mayShowToast(context, NO_CAR_UI_RV, showToast);
        }
        if (!carUiComponents.mIsUsingCarUiToolbar) {
            Log.d(TAG, NO_CAR_UI_TOOLBAR);
            mayShowToast(context, NO_CAR_UI_TOOLBAR, showToast);
        }
        if (!carUiComponents.mIsUsingCarUiBaseLayoutToolbar
                && carUiComponents.mIsUsingCarUiToolbar) {
            Log.d(TAG, NO_CAR_UI_TOOLBAR_BL);
            mayShowToast(context, NO_CAR_UI_TOOLBAR_BL, showToast);
        }
        if (carUiComponents.mIsUsingCarUiRecyclerViewForPreference
                && !carUiComponents.mIsUsingCarUiPreference) {
            Log.d(TAG, NO_CAR_UI_PREFERENCE);
            mayShowToast(context, NO_CAR_UI_PREFERENCE, showToast);
        }
    }

    private void checkForCarUiComponents(View v, CarUiComponents carUiComponents) {
        viewHasChildMatching(v, view -> {
            if (isCarUiRecyclerView(view)) {
                carUiComponents.mIsUsingCarUiRecyclerView = true;

                if (viewHasChildMatching(view, CheckCarUiComponents::isCarUiPreference)) {
                    carUiComponents.mIsUsingCarUiPreference = true;
                    return false;
                }

                carUiComponents.mIsCarUiRecyclerViewUsingListItem = viewHasChildMatching(view,
                        CheckCarUiComponents::isCarUiListItem);
                return false;
            }

            if (isAndroidXRecyclerView(view)) {
                carUiComponents.mIsUsingAndroidXRecyclerView = true;
            }

            if (isCarUiToolbar(view)) {
                carUiComponents.mIsUsingCarUiToolbar = true;
            }

            if (isCarUiBaseLayoutToolbar(view)) {
                carUiComponents.mIsUsingCarUiBaseLayoutToolbar = true;
            }
            return false;
        });
    }

    private static boolean viewHasChildMatching(View view, Predicate<View> p) {
        if (view == null) {
            return false;
        }
        if (p.test(view)) {
            return true;
        }
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                if (viewHasChildMatching(((ViewGroup) view).getChildAt(i), p)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isCarUiRecyclerView(View view) {
        return view.getTag() != null && view.getTag().toString().equals("carUiRecyclerView");
    }

    private static boolean isCarUiListItem(View view) {
        return view.getTag() != null && view.getTag().toString().equals("carUiListItem");
    }

    private static boolean isCarUiPreference(View view) {
        return view.getTag() != null && view.getTag().toString().equals("carUiPreference");
    }

    private static boolean isCarUiToolbar(View view) {
        return view.getTag() != null && (view.getTag().toString().equals("carUiToolbar")
                || view.getTag().toString().equals("CarUiBaseLayoutToolbar"));
    }

    private static boolean isCarUiBaseLayoutToolbar(View view) {
        return view.getTag() != null && view.getTag().toString().equals("CarUiBaseLayoutToolbar");
    }

    private static boolean isAndroidXRecyclerView(View view) {
        return view.getClass() == RecyclerView.class;
    }

    private static void mayShowToast(Context context, String message, boolean show) {
        if (!show) {
            return;
        }
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    private static class CarUiComponents {
        boolean mIsUsingCarUiRecyclerView;
        boolean mIsUsingCarUiRecyclerViewForPreference;
        boolean mIsCarUiRecyclerViewUsingListItem;
        boolean mIsUsingCarUiToolbar;
        boolean mIsUsingCarUiBaseLayoutToolbar;
        boolean mIsUsingCarUiPreference;
        boolean mIsUsingAndroidXRecyclerView;
    }

    /**
     * Dump's the view hierarchy.
     */
    private static void printViewHierarchy(String indent, View view) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n ");
        sb.append(indent);
        sb.append('{');

        if (view == null) {
            sb.append("viewNode= NULL, ");
            sb.append('}');
            return;
        }

        sb.append("viewNode= ").append(view.toString()).append(", ");
        sb.append("id= ").append(view.getId()).append(", ");
        sb.append("name= ").append(view.getAccessibilityClassName()).append(", ");

        sb.append('}');
        System.out.println(sb.toString());

        indent += "  ";
        if (!(view instanceof ViewGroup)) {
            return;
        }
        for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
            printViewHierarchy(indent, ((ViewGroup) view).getChildAt(i));
        }
    }

    private interface Predicate<T> {
        boolean test(T input);
    }
}
