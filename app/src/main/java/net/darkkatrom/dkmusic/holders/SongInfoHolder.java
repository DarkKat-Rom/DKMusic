/*
 * Copyright (C) 2018 DarkKat
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

package net.darkkatrom.dkmusic.holders;

import net.darkkatrom.dkmusic.models.Song;

public class SongInfoHolder {
    private Song mSong = null;
    private boolean mIsMediaPlayerReleased = true;
    private boolean mIsPlaying = false;
    private int mPosition = 0;

    public SongInfoHolder() {
    }

    public void setSong(Song song) {
        mSong = song;
    }

    public void setIsMediaPlayerReleased(boolean released) {
        mIsMediaPlayerReleased = released;
    }

    public void setIsPlaying(boolean isPlaying) {
        mIsPlaying = isPlaying;
    }

    public void setPosition(int position) {
        mPosition = position;
    }

    public Song getSong() {
        return mSong;
    }

    public boolean getIsMediaPlayerReleased() {
        return mIsMediaPlayerReleased;
    }

    public boolean getIsPlaying() {
        return mIsPlaying;
    }

    public int getPosition() {
        return mPosition;
    }
}
