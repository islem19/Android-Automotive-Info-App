/*
 * Copyright 2019 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import android.car.drivingstate.CarUxRestrictions;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class CarUxRestrictionsUtilTest {
    private int[] mRestrictionsArray;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mRestrictionsArray = new int[]{
                CarUxRestrictions.UX_RESTRICTIONS_NO_DIALPAD,
                CarUxRestrictions.UX_RESTRICTIONS_NO_KEYBOARD,
                CarUxRestrictions.UX_RESTRICTIONS_NO_DIALPAD
                        | CarUxRestrictions.UX_RESTRICTIONS_NO_KEYBOARD,
                CarUxRestrictions.UX_RESTRICTIONS_FULLY_RESTRICTED
        };
    }

    @Test
    public void testNullActiveRestriction() {
        CarUxRestrictions activeRestrictions = null;
        boolean[] expectedResults = {true, true, true, true};
        for (int i = 0; i < mRestrictionsArray.length; i++) {
            boolean actualResult = CarUxRestrictionsUtil.isRestricted(mRestrictionsArray[i],
                    activeRestrictions);
            assertThat(actualResult == expectedResults[i]).isTrue();
        }
    }

    @Test
    public void testOneActiveRestriction() {
        CarUxRestrictions activeRestrictions = new CarUxRestrictions.Builder(/* reqOpt= */true,
                CarUxRestrictions.UX_RESTRICTIONS_NO_DIALPAD, /* timestamp= */0).build();
        boolean[] expectedResults = {true, false, true, true};
        for (int i = 0; i < mRestrictionsArray.length; i++) {
            boolean actualResult = CarUxRestrictionsUtil.isRestricted(mRestrictionsArray[i],
                    activeRestrictions);
            assertThat(actualResult == expectedResults[i]).isTrue();
        }
    }

    @Test
    public void testMultipleActiveRestrictions() {
        CarUxRestrictions activeRestrictions = new CarUxRestrictions.Builder(/* reqOpt= */true,
                CarUxRestrictions.UX_RESTRICTIONS_NO_DIALPAD
                        | CarUxRestrictions.UX_RESTRICTIONS_NO_TEXT_MESSAGE, /* timestamp= */
                0).build();
        boolean[] expectedResults = {true, false, true, true};
        for (int i = 0; i < mRestrictionsArray.length; i++) {
            boolean actualResult = CarUxRestrictionsUtil.isRestricted(mRestrictionsArray[i],
                    activeRestrictions);
            assertThat(actualResult == expectedResults[i]).isTrue();
        }
    }
}
