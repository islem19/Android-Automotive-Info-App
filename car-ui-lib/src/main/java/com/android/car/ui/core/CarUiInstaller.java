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

import static com.android.car.ui.core.CarUi.getBaseLayoutController;

import android.app.Activity;
import android.app.Application;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.car.ui.baselayout.Insets;

import java.lang.reflect.Method;
import java.util.Locale;

/**
 * {@link ContentProvider ContentProvider's} onCreate() methods are "called for all registered
 * content providers on the application main thread at application launch time." This means we
 * can use a content provider to register for Activity lifecycle callbacks before any activities
 * have started, for installing the CarUi base layout into all activities.
 */
public class CarUiInstaller extends ContentProvider {

    private static final String TAG = "CarUiInstaller";
    private static final String CAR_UI_INSET_LEFT = "CAR_UI_INSET_LEFT";
    private static final String CAR_UI_INSET_RIGHT = "CAR_UI_INSET_RIGHT";
    private static final String CAR_UI_INSET_TOP = "CAR_UI_INSET_TOP";
    private static final String CAR_UI_INSET_BOTTOM = "CAR_UI_INSET_BOTTOM";

    private static final boolean IS_DEBUG_DEVICE =
            Build.TYPE.toLowerCase(Locale.ROOT).contains("debug")
                    || Build.TYPE.toLowerCase(Locale.ROOT).equals("eng");

    @Override
    public boolean onCreate() {
        Context context = getContext();
        if (context == null) {
            Log.e(TAG, "CarUiInstaller had a null context!");
            return false;
        }
        Log.i(TAG, "CarUiInstaller started for " + context.getPackageName());

        Application application = (Application) context.getApplicationContext();
        application.registerActivityLifecycleCallbacks(
                new Application.ActivityLifecycleCallbacks() {
                    private Insets mInsets = null;
                    private boolean mIsActivityStartedForFirstTime = false;

                    @Override
                    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                        if (activity.getClassLoader()
                                .equals(CarUiInstaller.class.getClassLoader())) {
                            BaseLayoutController.build(activity);
                        } else {
                            callBaseLayoutControllerMethod("build", activity);
                        }

                        if (savedInstanceState != null) {
                            int inset_left = savedInstanceState.getInt(CAR_UI_INSET_LEFT);
                            int inset_top = savedInstanceState.getInt(CAR_UI_INSET_TOP);
                            int inset_right = savedInstanceState.getInt(CAR_UI_INSET_RIGHT);
                            int inset_bottom = savedInstanceState.getInt(CAR_UI_INSET_BOTTOM);
                            mInsets = new Insets(inset_left, inset_top, inset_right, inset_bottom);
                        }

                        mIsActivityStartedForFirstTime = true;
                    }

                    @Override
                    public void onActivityPostStarted(Activity activity) {
                        BaseLayoutController controller = getBaseLayoutController(activity);
                        if (mInsets != null && controller != null
                                && mIsActivityStartedForFirstTime) {
                            controller.dispatchNewInsets(mInsets);
                            mIsActivityStartedForFirstTime = false;
                        }
                    }

                    @Override
                    public void onActivityStarted(Activity activity) {
                    }

                    @Override
                    public void onActivityResumed(Activity activity) {
                    }

                    @Override
                    public void onActivityPaused(Activity activity) {
                    }

                    @Override
                    public void onActivityStopped(Activity activity) {
                    }

                    @Override
                    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
                        BaseLayoutController controller = getBaseLayoutController(activity);
                        if (controller != null) {
                            Insets insets = controller.getInsets();
                            outState.putInt(CAR_UI_INSET_LEFT, insets.getLeft());
                            outState.putInt(CAR_UI_INSET_TOP, insets.getTop());
                            outState.putInt(CAR_UI_INSET_RIGHT, insets.getRight());
                            outState.putInt(CAR_UI_INSET_BOTTOM, insets.getBottom());
                        }
                    }

                    @Override
                    public void onActivityDestroyed(Activity activity) {
                        if (activity.getClassLoader()
                                .equals(CarUiInstaller.class.getClassLoader())) {
                            BaseLayoutController.destroy(activity);
                        } else {
                            callBaseLayoutControllerMethod("destroy", activity);
                        }
                    }
                });

        // Check only if we are in debug mode.
        if (IS_DEBUG_DEVICE) {
            CheckCarUiComponents checkCarUiComponents = new CheckCarUiComponents(application);
            application.registerActivityLifecycleCallbacks(checkCarUiComponents);
        }

        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
            @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        return null;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection,
            @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection,
            @Nullable String[] selectionArgs) {
        return 0;
    }

    private static void callBaseLayoutControllerMethod(String methodName, Activity activity) {
        // Note: (b/156532465)
        // The usage of the alternate classloader is to accommodate GMSCore.
        // Some activities are loaded dynamically from external modules.
        try {
            Class<?> baseLayoutControllerClass =
                    activity.getClassLoader()
                            .loadClass(BaseLayoutController.class.getName());
            Method method = baseLayoutControllerClass
                    .getDeclaredMethod(methodName, Activity.class);
            method.invoke(null, activity);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
