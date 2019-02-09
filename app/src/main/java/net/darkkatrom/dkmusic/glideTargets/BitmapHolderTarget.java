/*
 * Copyright (C) 2019 DarkKat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.darkkatrom.dkmusic.glideTargets;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;

import com.bumptech.glide.request.transition.Transition;

import net.darkkatrom.dkmusic.interfaces.BitmapHolder;

public class BitmapHolderTarget extends CustomTarget<Bitmap> {

    private BitmapHolder mHolder;
    private boolean mAnimate;

    public BitmapHolderTarget(BitmapHolder holder, boolean animate) {
        super();
        mHolder = holder;
        mAnimate = animate;
    }

    public BitmapHolderTarget(BitmapHolder holder, int width, int height, boolean animate) {
        super(width, height);
        mHolder = holder;
        mAnimate = animate;
    }

    @Override
    public void onLoadCleared(@Nullable Drawable placeholder) {
        if (mHolder != null) {
            mHolder.setBitmap(null, mAnimate);
        }
    }

    @Override
    public void onLoadFailed(@Nullable Drawable placeholder) {
        if (mHolder != null) {
            mHolder.setBitmap(null, mAnimate);
        }
    }

    @Override
    public void onResourceReady(Bitmap resource, Transition transition) {
        if (mHolder != null) {
            mHolder.setBitmap(resource, mAnimate);
        }
    }
}
