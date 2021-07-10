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

import android.os.IBinder;

/**
 * Interface for {@link CarUiEditText} to support different actions and callbacks from IME
 * when running in wide screen mode.
 */
public interface PrivateImeCommandCallback {
    /**
     * Called when user clicks on an item in the search results.
     *
     * @param itemId the id of the item clicked. This will be the same id that was passed by the
     *               application to the IMS template to display search results.
     */
    void onItemClicked(String itemId);

    /**
     * Called when user clicks on a secondary image within item in the search results.
     *
     * @param secondaryImageId the id of the secondary image clicked. This will be the same id
     *                         that was passed by the application to the IMS template to display
     *                         search results.
     */
    void onSecondaryImageClicked(String secondaryImageId);

    /**
     * Called when the edit text is clicked and IME is about to launch. IME provides the surface
     * view information through this call that applications can use to display a view on the
     * IME surface.
     *
     * This method will NOT be called if an OEM has not allowed an application to hide the
     * content area.
     */
    void onSurfaceInfo(int displayId, IBinder binder, int height, int width);
}
