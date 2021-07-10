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

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import com.android.car.ui.test.R;

/**
 * An {@link Activity} that mimics a wide screen IME and displays the template for testing.
 */
public class CarUiImeWideScreenTestActivity extends Activity {
    public static CarUiImeWideScreenController sCarUiImeWideScreenController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.car_ui_ime_wide_screen_test_activity);

        FrameLayout root = findViewById(R.id.test_activity);

        sCarUiImeWideScreenController = new CarUiImeWideScreenController(this, null) {
            @Override
            public boolean isWideScreenMode() {
                return true;
            }
        };

        View imeInputView = LayoutInflater.from(this)
                .inflate(R.layout.test_ime_input_view, null, false);

        View templateView = sCarUiImeWideScreenController.createWideScreenImeView(imeInputView);

        root.addView(templateView);
    }
}
