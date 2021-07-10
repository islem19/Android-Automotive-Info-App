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

package com.android.car.ui.matchers;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

/* package */ class DrawableMatcher extends TypeSafeMatcher<View> {

    private final int mDrawableId;

    DrawableMatcher(int drawableId) {
        mDrawableId = drawableId;
    }

    @Override
    protected boolean matchesSafely(View item) {
        if (!(item instanceof ImageView) || !item.isShown()) {
            return false;
        }

        ImageView imageView = (ImageView) item;

        Bitmap bitmap = drawableToBitmap(imageView.getDrawable());
        Bitmap otherBitmap = drawableToBitmap(imageView.getContext().getDrawable(mDrawableId));

        if (bitmap == null && otherBitmap == null) {
            return true;
        } else if ((bitmap == null) != (otherBitmap == null)) {
            return false;
        }

        return bitmap.sameAs(otherBitmap);
    }

    private Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable == null) {
            return null;
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("has drawable with id " + mDrawableId);
    }
}
