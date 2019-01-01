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
import android.content.res.Resources;
import android.content.Intent;
import android.media.session.MediaSession;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.text.TextPaint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.TextView;

import net.darkkatrom.dkmusic.R;
import net.darkkatrom.dkmusic.activities.MainActivity;

public class NotificationUtil {
    public static final int MUSIC_PLAYBACK_NOTIFICATION_ID = 1;

    public static final String MUSIC_PLAYBACK_NOTIFICATION_CHANNEL_ID =
            "music_playback_notification_chanel";

    private final Context mContext;
    private final Resources mResources;

    private String mTitle;
    private String mText;
    private Bitmap mAlbumArt;

    public NotificationUtil(Context context) {
        mContext = context;
        mResources = context.getResources();
    }

    public void sendNotification(String title, String text, Bitmap artwork, boolean play) {
        if ((mTitle == null && title == null)
                || (mText == null && text == null)
                || (mAlbumArt == null && artwork == null)) {
            return;
        }
        ((NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(MUSIC_PLAYBACK_NOTIFICATION_ID,
                createMusicPlaybackNotification(title, text, artwork, play));
    }

    private Notification createMusicPlaybackNotification(String title, String text, Bitmap albumArt, boolean play) {
        if (title != null) {
            mTitle = title;
        }
        if (text != null) {
            mText = text;
        }
        if (albumArt != null) {
            mAlbumArt = albumArt;
        }

        MediaSession session = new MediaSession(mContext, "Test");
        Notification.MediaStyle style = new Notification.MediaStyle()
                .setMediaSession(session.getSessionToken())
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
        b.putBoolean(MainActivity.KEY_ACTION_PLAY_PAUSE, play);
        Intent intent = new Intent(mContext, MainActivity.class);
        intent.putExtras(b);
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent,
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
