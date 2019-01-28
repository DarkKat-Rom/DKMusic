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
package net.darkkatrom.dkmusic.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import net.darkkatrom.dkmusic.models.Song;

public class Config {

    public static final String PREF_KEY_SHOW_VISUALIZER = "show_visualizer";
    public static final String PREF_KEY_SHOW_ALBUM_ART_ON_LOCK_SCREEN = "show_album_art_on_lock_screen";
    public static final String PREF_KEY_THEME = "theme";

    public static final String KEY_SONG_DATA          = "song_data";
    public static final String KEY_SONG_TITLE         = "song_title";
    public static final String KEY_SONG_ARTIST        = "song_artist";
    public static final String KEY_SONG_ALBUM         = "song_album";
    public static final String KEY_SONG_ALBUM_ID      = "song_album_id";
    public static final String KEY_SONG_POSITION      = "song_position";

    public static final int THEME_MATERIAL_DARKKAT = 0;
    public static final int THEME_MATERIAL         = 1;

    public static boolean getShowVisualizer(Context context) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);

        return prefs.getBoolean(PREF_KEY_SHOW_VISUALIZER, false);
    }

    public static boolean getShowAlbumArtOnLockScreen(Context context) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);

        return prefs.getBoolean(PREF_KEY_SHOW_ALBUM_ART_ON_LOCK_SCREEN, true);
    }

    public static int getTheme(Context context) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);

        String valueString = prefs.getString(PREF_KEY_THEME, "0");
        return Integer.valueOf(valueString);
    }

    public static void saveSong(Context context, Song song) {
        if (song == null) {
            return;
        }
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);

        prefs.edit().putString(KEY_SONG_DATA, song.getData()).commit();
        prefs.edit().putString(KEY_SONG_TITLE, song.getTitle()).commit();
        prefs.edit().putString(KEY_SONG_ARTIST, song.getArtist()).commit();
        prefs.edit().putString(KEY_SONG_ALBUM, song.getAlbum()).commit();
        prefs.edit().putLong(KEY_SONG_ALBUM_ID, song.getAlbumId()).commit();
        prefs.edit().putInt(KEY_SONG_POSITION, 0).commit();
    }

    public static void savePlaybackPosition(Context context, int playbackPosition) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);

        prefs.edit().putInt(KEY_SONG_POSITION, playbackPosition).commit();
    }

    public static Song getSong(Context context) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);

        String data = prefs.getString(KEY_SONG_DATA, null);
        if (data == null) {
            return null;
        }
        String title = prefs.getString(KEY_SONG_TITLE, null);
        String artist = prefs.getString(KEY_SONG_ARTIST, null);
        String album = prefs.getString(KEY_SONG_ALBUM, null);
        long albumId = prefs.getLong(KEY_SONG_ALBUM_ID, 0L);
        return new Song(data, title, artist, album, albumId);
    }

    public static int getPlaybackPosition(Context context) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        return prefs.getInt(KEY_SONG_POSITION, 0);
    }
}
