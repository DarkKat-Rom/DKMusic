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

public class Config {

    public static final String PREF_KEY_SHOW_VISUALIZER = "show_visualizer";
    public static final String PREF_KEY_SHOW_ALBUM_ART_ON_LOCK_SCREEN = "show_album_art_on_lock_screen";


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
}
