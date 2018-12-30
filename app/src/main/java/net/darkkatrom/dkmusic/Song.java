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

package net.darkkatrom.dkmusic;

import android.content.ContentUris;
import android.net.Uri;

public class Song {
    public static final Uri ALBUM_ART_URI = Uri.parse("content://media/external/audio/albumart");

    private String mData;
    private String mTitle;
    private String mArtist;
    private String mAlbum;
    private long mAlbumId;
    private Uri mAlbumArtUri;

    public Song(String data, String title, String artist, String album, long albumId) {
        mData = data;
        mTitle = title;
        mArtist = artist;
        mAlbum = album;
        mAlbumId = albumId;
    }

    public String getData() {
        return mData;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getArtist() {
        return mArtist;
    }

    public String getAlbum() {
        return mAlbum;
    }

    public long getAlbumId() {
        return mAlbumId;
    }

    public Uri getAlbumArtUri() {
        return ContentUris.withAppendedId(ALBUM_ART_URI, getAlbumId());
    }
}
