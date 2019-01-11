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

package net.darkkatrom.dkmusic.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import net.darkkatrom.dkmusic.R;
import net.darkkatrom.dkmusic.fragments.SongListFragment;
import net.darkkatrom.dkmusic.utils.NotificationUtil;

public final class MainActivity extends AppCompatActivity {
    private static final String FRAGMENT_TAG = "song_list_fragment";
    public static final String KEY_ACTION_PLAY_PAUSE = "action_play_pause";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        NotificationUtil notificationUtil = new NotificationUtil(this);
        notificationUtil.setNotificationChannels();

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_content, new SongListFragment(), FRAGMENT_TAG)
                    .commit();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        super.onNewIntent(intent);
    }
}
