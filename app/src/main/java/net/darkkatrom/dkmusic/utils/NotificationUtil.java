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

import android.app.Notification;
import android.app.Notification.Action;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.session.MediaSession.Token;
import android.graphics.Bitmap;
import android.os.Bundle;

import net.darkkatrom.dkmusic.MusicPlaybackService;
import net.darkkatrom.dkmusic.R;
import net.darkkatrom.dkmusic.activities.MainActivity;

public class NotificationUtil {
    public static final int MUSIC_PLAYBACK_NOTIFICATION_ID = 1;

    public static final String MUSIC_PLAYBACK_NOTIFICATION_CHANNEL_ID =
            "music_playback_notification_chanel";

    private final Context mContext;

    private String mTitle;
    private String mText;
    private Bitmap mAlbumArt;

    public NotificationUtil(Context context) {
        mContext = context;
    }

    public void sendNotification(String title, String text, Bitmap albumArt, boolean play,
            Token token) {
        if ((mTitle == null && title == null)
                || (mText == null && text == null)
                || (mAlbumArt == null && albumArt == null)) {
            return;
        }
        ((NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(MUSIC_PLAYBACK_NOTIFICATION_ID,
                createNotification(title, text, albumArt, play, token));
    }

    public Notification createNotification(String title, String text, Bitmap albumArt, boolean play,
            Token token) {
        if (title != null) {
            mTitle = title;
        }
        if (text != null) {
            mText = text;
        }
        if (albumArt != null) {
            mAlbumArt = albumArt;
        }

        Notification.MediaStyle style = new Notification.MediaStyle()
                .setMediaSession(token)
                .setShowActionsInCompactView(0);

        Notification.Builder builder = new Notification.Builder(mContext, MUSIC_PLAYBACK_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(mAlbumArt)
            .setShowWhen(false)
            .setStyle(style)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setContentTitle(mTitle)
            .setContentText(mText)
            .addAction(getPlayPauseAction(play));
        return builder.build();
    }

    private Action getPlayPauseAction(boolean play) {
        Bundle b = new Bundle();
        b.putBoolean(MusicPlaybackService.KEY_ACTION_PLAY_PAUSE, play);
        Intent intent = new Intent(mContext, MusicPlaybackService.class);
        intent.putExtras(b);
        PendingIntent pendingIntent = PendingIntent.getService(mContext, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Action.Builder builder = new Action.Builder(play ? R.drawable.ic_action_play : R.drawable.ic_action_pause,
                "playpause", pendingIntent);
        return builder.build();
    }

    public void setNotificationChannels() {
        NotificationManager notificationManager =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = getMusicPlaybackNotificationChannel();

        channel.setSound(null, null);
        channel.enableLights(false);
        channel.enableVibration(false);

        notificationManager.createNotificationChannel(channel);
    }

    private NotificationChannel getMusicPlaybackNotificationChannel() {
        String id = MUSIC_PLAYBACK_NOTIFICATION_CHANNEL_ID;
        CharSequence name = mContext.getString(R.string.music_playback_notification_chanel_title);
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        String description = mContext.getString(R.string.music_playback_notification_chanel_description);

        NotificationChannel channel = new NotificationChannel(id, name, importance);
        channel.setDescription(description);

        return channel;
    }

    public static void removeNotification(Context context) {
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                .cancel(MUSIC_PLAYBACK_NOTIFICATION_ID);
    }
}
