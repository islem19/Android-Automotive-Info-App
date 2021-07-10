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

package com.android.car.ui.preference;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;

public class PreferenceTestActivity extends AppCompatActivity {

    private PreferenceTestFragment mPreferenceFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        if (savedInstanceState == null) {
            mPreferenceFragment = new PreferenceTestFragment();

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.content, mPreferenceFragment)
                    .commitNow();
        }
    }

    public void setOnPreferenceChangeListener(
            String key, Preference.OnPreferenceChangeListener listener) {
        mPreferenceFragment.setOnPreferenceChangeListener(key, listener);
    }

    public void scrollToPreference(String key) {
        mPreferenceFragment.scrollToPreference(key);
    }

    public void addPreference(Preference preference) {
        mPreferenceFragment.addPreference(preference);
    }
}
