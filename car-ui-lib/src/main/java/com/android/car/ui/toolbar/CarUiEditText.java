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

import static com.android.car.ui.imewidescreen.CarUiImeWideScreenController.CONTENT_AREA_SURFACE_DISPLAY_ID;
import static com.android.car.ui.imewidescreen.CarUiImeWideScreenController.CONTENT_AREA_SURFACE_HEIGHT;
import static com.android.car.ui.imewidescreen.CarUiImeWideScreenController.CONTENT_AREA_SURFACE_HOST_TOKEN;
import static com.android.car.ui.imewidescreen.CarUiImeWideScreenController.CONTENT_AREA_SURFACE_WIDTH;
import static com.android.car.ui.imewidescreen.CarUiImeWideScreenController.SEARCH_RESULT_ITEM_ID_LIST;
import static com.android.car.ui.imewidescreen.CarUiImeWideScreenController.SEARCH_RESULT_SUPPLEMENTAL_ICON_ID_LIST;
import static com.android.car.ui.imewidescreen.CarUiImeWideScreenController.WIDE_SCREEN_CLEAR_DATA_ACTION;

import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;
import android.util.AttributeSet;
import android.widget.EditText;

import java.util.HashSet;
import java.util.Set;

/**
 * Edit text supporting the callbacks from the IMS. This will be useful in widescreen IME mode to
 * allow car-ui-lib to receive responses (like onClick events) from the IMS
 */
class CarUiEditText extends EditText {

    private final Set<PrivateImeCommandCallback> mPrivateImeCommandCallback = new HashSet<>();

    // These need to be public for the layout inflater to inflate them, but
    // checkstyle complains about a public constructor on a package-private class
    //CHECKSTYLE:OFF Generated code
    public CarUiEditText(Context context) {
        super(context);
    }

    public CarUiEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CarUiEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CarUiEditText(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }
    //CHECKSTYLE:ON Generated code

    @Override
    public boolean onPrivateIMECommand(String action, Bundle data) {

        if (WIDE_SCREEN_CLEAR_DATA_ACTION.equals(action)) {
            // clear the text.
            setText("");
        }

        if (data == null || mPrivateImeCommandCallback == null) {
            return false;
        }

        if (data.getString(SEARCH_RESULT_ITEM_ID_LIST) != null) {
            for (PrivateImeCommandCallback listener : mPrivateImeCommandCallback) {
                listener.onItemClicked(data.getString(SEARCH_RESULT_ITEM_ID_LIST));
            }
        }

        if (data.getString(SEARCH_RESULT_SUPPLEMENTAL_ICON_ID_LIST) != null) {
            for (PrivateImeCommandCallback listener : mPrivateImeCommandCallback) {
                listener.onSecondaryImageClicked(
                        data.getString(SEARCH_RESULT_SUPPLEMENTAL_ICON_ID_LIST));
            }
        }

        int displayId = data.getInt(CONTENT_AREA_SURFACE_DISPLAY_ID);
        int height = data.getInt(CONTENT_AREA_SURFACE_HEIGHT);
        int width = data.getInt(CONTENT_AREA_SURFACE_WIDTH);
        IBinder binder = data.getBinder(CONTENT_AREA_SURFACE_HOST_TOKEN);

        if (binder != null) {
            for (PrivateImeCommandCallback listener : mPrivateImeCommandCallback) {
                listener.onSurfaceInfo(displayId, binder, height, width);
            }
        }

        return false;
    }

    /**
     * Registers a new {@link PrivateImeCommandCallback} to the list of
     * listeners.
     */
    public void registerOnPrivateImeCommandListener(PrivateImeCommandCallback listener) {
        mPrivateImeCommandCallback.add(listener);
    }

    /**
     * Unregisters an existing {@link PrivateImeCommandCallback} from the list
     * of listeners.
     */
    public boolean unregisterOnPrivateImeCommandListener(PrivateImeCommandCallback listener) {
        return mPrivateImeCommandCallback.remove(listener);
    }
}
