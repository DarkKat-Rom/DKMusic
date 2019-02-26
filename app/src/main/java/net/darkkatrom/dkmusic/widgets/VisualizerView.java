/*
* Copyright (C) 2014 The CyanogenMod Project
*
* Copyright (C) 2018 DarkKat
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package net.darkkatrom.dkmusic.widgets;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.View;

import net.darkkatrom.dkmusic.R;
import net.darkkatrom.dkmusic.holders.VisualizerHolder;
import net.darkkatrom.dkmusic.utils.ThemeUtil;

public class VisualizerView extends View implements VisualizerHolder.Listener {
    private VisualizerHolder mVisualizerHolder;
    private Paint mPaint;
    private ObjectAnimator mVisualizerColorAnimator;

    private ValueAnimator[] mValueAnimators = new ValueAnimator[32];
    private float[] mFFTPoints = new float[128];

    private boolean mVisible = true;
    private boolean mPlaying = false;
    private boolean mPowerSaveMode = false;
    private int mColor;

    private static final float MAX_DB_VALUE = (float) (10 * Math.log10(256 * 256 + 256 * 256));

    @Override
    public void onFftDataCapture(byte[] fft) {
        byte rfk, ifk;
        int dbValue;
        float magnitude;

        for (int i = 0; i < 32; i++) {
            mValueAnimators[i].cancel();

            rfk = fft[i * 2 + 2];
            ifk = fft[i * 2 + 3];
            magnitude = !mPlaying ? 0 : rfk * rfk + ifk * ifk;
            dbValue = magnitude > 0 ? (int) (10 * Math.log10(magnitude)) : 0;

            mValueAnimators[i].setFloatValues(mFFTPoints[i * 4 + 1],
                    mFFTPoints[3] - (dbValue * getHeight() / MAX_DB_VALUE));
            mValueAnimators[i].start();
        }
    }

    public VisualizerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public VisualizerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VisualizerView(Context context) {
        this(context, null, 0);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        final int size = Math.min(getMeasuredWidth(), getMeasuredHeight());
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        float barUnit = w / 32f;
        float barWidth = barUnit * 8f / 9f;
        barUnit = barWidth + (barUnit - barWidth) * 32f / 31f;
        mPaint.setStrokeWidth(barWidth);

        for (int i = 0; i < 32; i++) {
            mFFTPoints[i * 4] = mFFTPoints[i * 4 + 2] = i * barUnit + (barWidth / 2);
            mFFTPoints[i * 4 + 3] = h;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawLines(mFFTPoints, mPaint);
    }

    public void initialize(Context context, VisualizerHolder holder) {
        initialize(context, holder, true);
    }

    public void initialize(Context context, VisualizerHolder holder, boolean useDefaultFillColor) {
        mVisualizerHolder = holder;
        if (useDefaultFillColor) {
            mColor = context.getColor(R.color.visualizer_fill_color_default);
        } else {
            mColor = ThemeUtil.getColorFromThemeAttribute(context, R.attr.visualizerFillColor);
        }

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(mColor);

        for (int i = 0; i < 32; i++) {
            final int j = i * 4 + 1;
            mValueAnimators[i] = new ValueAnimator();
            mValueAnimators[i].setDuration(128);
            mValueAnimators[i].addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mFFTPoints[j] = (float) animation.getAnimatedValue();
                }
            });
        }

        mValueAnimators[31].addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                postInvalidate();
            }
        });
    }

    public void setVisible(boolean visible) {
        if (mVisible != visible) {
            mVisible = visible;
            checkStateChanged();
        }
    }

    public void setPlaying(boolean playing) {
        setPlaying(playing, true);
    }

    public void setPlaying(boolean playing, boolean applyVisibility) {
        if (mPlaying != playing) {
            mPlaying = playing;
            checkStateChanged(applyVisibility);
        }
    }

    public boolean isPlaying() {
        return mPlaying;
    }

    public void setPowerSaveMode(boolean powerSaveMode) {
        if (mPowerSaveMode != powerSaveMode) {
            mPowerSaveMode = powerSaveMode;
            checkStateChanged();
        }
    }

    public void setColor(int color) {
        setColor(color, true);
    }

    public void setColor(int color, boolean animate) {
        color = Color.argb(191, Color.red(color), Color.green(color), Color.blue(color));

        if (mColor != color) {
            mColor = color;

            if (mVisualizerColorAnimator != null) {
                mVisualizerColorAnimator.cancel();
            }

            if (mPlaying && animate) {
                mVisualizerColorAnimator = ObjectAnimator.ofArgb(mPaint, "color",
                        mPaint.getColor(), mColor);
                mVisualizerColorAnimator.setStartDelay(600);
                mVisualizerColorAnimator.setDuration(1200);
                mVisualizerColorAnimator.start();
            } else {
                mPaint.setColor(mColor);
            }
        }
    }

    public void unListen() {
        mVisualizerHolder.removeListener(this);
    }

    private void checkStateChanged() {
        checkStateChanged(true);
    }

    private void checkStateChanged(boolean applyVisibility) {
        boolean show = mVisible && mPlaying && !mPowerSaveMode;

        if (show) {
            mVisualizerHolder.addListener(this);
        }
        if (applyVisibility) {
            float end = show ? 1f : 0f;
            ValueAnimator animator = ValueAnimator.ofFloat(getAlpha(), end);
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    setAlpha((float) animation.getAnimatedValue());
                }
            });
            animator.setDuration(300);
            animator.start();

            if (!show) {
                mVisualizerHolder.removeListener(this);
            }
        }
    }
}
