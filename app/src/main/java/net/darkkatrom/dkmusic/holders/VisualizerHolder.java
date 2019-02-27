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

package net.darkkatrom.dkmusic.holders;

import android.media.audiofx.Visualizer;
import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VisualizerHolder implements Visualizer.OnDataCaptureListener {

    private Visualizer mVisualizer;

    private boolean mPowerSaveMode = false;

    private List<Listener> mListeners;

    public interface Listener {

        public void onFftDataCapture(byte[] fft);
    }

    public VisualizerHolder() {
        mListeners = new ArrayList<Listener>();
    }

    public void addListener(Listener listener) {
        boolean linkVisualizer = !mPowerSaveMode && mListeners.isEmpty();
        mListeners.add(listener);
        if (linkVisualizer) {
            AsyncTask.execute(mLinkVisualizer);
        }
    }

    public void removeListener(Listener listener) {
        mListeners.remove(listener);
        if (!mPowerSaveMode && mListeners.isEmpty()) {
            AsyncTask.execute(mUnlinkVisualizer);
        }
    }

    @Override
    public void onWaveFormDataCapture(Visualizer visualizer, byte[] bytes, int samplingRate) {
    }

    @Override
    public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
        if (mListeners.isEmpty()) {
            return;
        }
        byte[] fftCopy = Arrays.copyOf(fft, fft.length);
        for (int i = 0; i < mListeners.size(); i ++ ) {
            mListeners.get(i).onFftDataCapture(fftCopy);
        }
    }

    public void unlinkVisualizer() {
        if (mVisualizer == null) {
            return;
        }
        for (int i = 0; i < mListeners.size(); i ++ ) {
            removeListener(mListeners.get(i));
        }
    }

    public void setPowerSaveMode(boolean powerSaveMode) {
        if (mPowerSaveMode != powerSaveMode) {
            mPowerSaveMode = powerSaveMode;
            if (mPowerSaveMode) {
                if (mVisualizer != null) {
                    AsyncTask.execute(mUnlinkVisualizer);
                }
            } else {
                if (mVisualizer == null && !mListeners.isEmpty()) {
                    AsyncTask.execute(mLinkVisualizer);
                }
            }
        }
    }

    private final Runnable mLinkVisualizer = new Runnable() {
        @Override
        public void run() {
            try {
                mVisualizer = new Visualizer(0);
            } catch (Exception e) {
                return;
            }

            mVisualizer.setEnabled(false);
            mVisualizer.setCaptureSize(66);
            mVisualizer.setDataCaptureListener(VisualizerHolder.this, Visualizer.getMaxCaptureRate(),
                    false, true);
            mVisualizer.setEnabled(true);
        }
    };

    private final Runnable mUnlinkVisualizer = new Runnable() {
        @Override
        public void run() {
            try {
                mVisualizer.setEnabled(false);
                mVisualizer.release();
                mVisualizer = null;
            } catch (Exception e) {
            }
        }
    };
}
