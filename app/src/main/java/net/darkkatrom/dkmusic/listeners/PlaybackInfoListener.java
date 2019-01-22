/*
 * Copyright 2017 Google Inc. All rights reserved.
 *
 * Copyright (C) 2018 DarkKat
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

package net.darkkatrom.dkmusic.listeners;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Allows {@link MusicPlaybackService} to report media playback duration and progress updates to
 * the {@link SongListFragment}.
 */
public abstract class PlaybackInfoListener {

    @IntDef({State.INVALID, State.PREPARED, State.PLAYING, State.PAUSED, State.RESET, State.COMPLETED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface State {

        int INVALID = -1;
        int PREPARED = 0;
        int PLAYING = 1;
        int PAUSED = 2;
        int COMPLETED = 3;
        int RESET = 4;
    }

    public static String convertStateToString(@State int state) {
        String stateString;
        switch (state) {
            case State.INVALID:
                stateString = "INVALID";
                break;
            case State.PREPARED:
                stateString = "PREPARED";
                break;
            case State.PLAYING:
                stateString = "PLAYING";
                break;
            case State.PAUSED:
                stateString = "PAUSED";
                break;
            case State.COMPLETED:
                stateString = "COMPLETED";
                break;
            case State.RESET:
                stateString = "RESET";
                break;
            default:
                stateString = "N/A";
        }
        return stateString;
    }

    public void onDurationChanged(int duration) {
    }

    public void onPositionChanged(int position) {
    }

    public void onStateChanged(@State int state) {
    }
}
