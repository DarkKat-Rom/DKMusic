/*
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

package net.darkkatrom.dkmusic;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Binder;
import android.os.IBinder;

import net.darkkatrom.dkmusic.activities.MainActivity;
import net.darkkatrom.dkmusic.listeners.PlaybackInfoListener;
import net.darkkatrom.dkmusic.models.Song;
import net.darkkatrom.dkmusic.utils.Config;
import net.darkkatrom.dkmusic.utils.NotificationUtil;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MusicPlaybackService extends Service {
    public static final int PLAYBACK_POSITION_REFRESH_INTERVAL_MS = 1000;
    public static final String KEY_ACTION_PLAY_PAUSE = "action_play_pause";

    private final IBinder mBinder = new LocalBinder();

    private MediaPlayer mMediaPlayer;
    private MediaSession mSession;
    private PlaybackInfoListener mPlaybackInfoListener;
    private ScheduledExecutorService mExecutor;
    private Runnable mSeekbarPositionUpdateTask;

    private NotificationUtil mNotificationUtil;

    private boolean mIsMediaPlayerPrepared = false;

    private Song mSong;
    private Bitmap mAlbumArt;

    private boolean mShowAlbumArtOnLockScreen;

    @Override
    public void onCreate() {
        super.onCreate();

        initializeMediaPlayer();
        mSession = new MediaSession(this, "DKMusic");
        mNotificationUtil = new NotificationUtil(this);
        mShowAlbumArtOnLockScreen = Config.getShowAlbumArtOnLockScreen(this);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        release();
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        if (intent != null) {
            if (intent.hasExtra(KEY_ACTION_PLAY_PAUSE)) {
                boolean play = intent.getBooleanExtra(KEY_ACTION_PLAY_PAUSE, false);
                if (play) {
                    play();
                } else {
                    pause();
                }
            }
        }
        return START_NOT_STICKY;
    }


    public class LocalBinder extends Binder {
        public MusicPlaybackService getService() {
            return MusicPlaybackService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private void initializeMediaPlayer() {
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                stopUpdatingCallbackWithPosition(true);
                if (mPlaybackInfoListener != null) {
                    mPlaybackInfoListener.onStateChanged(PlaybackInfoListener.State.COMPLETED);
                }
            }
        });
    }

    public void setPlaybackInfoListener(PlaybackInfoListener listener) {
        mPlaybackInfoListener = listener;
        if (mPlaybackInfoListener == null) {
            stopUpdatingCallbackWithPosition(false);
        } else {
            if (mMediaPlayer != null) {
                mPlaybackInfoListener.onStateChanged(PlaybackInfoListener.State.PREPARED);
                final int duration = mMediaPlayer.getDuration();
                int currentPosition = mMediaPlayer.getCurrentPosition();
                mPlaybackInfoListener.onDurationChanged(duration);
                mPlaybackInfoListener.onPositionChanged(currentPosition);
                if (mMediaPlayer.isPlaying()) {
                    mPlaybackInfoListener.onStateChanged(PlaybackInfoListener.State.PLAYING);
                    startUpdatingCallbackWithPosition();
                }
            }
        }
    }

    public void setDataSource(String data) {
        mMediaPlayer.reset();
        mIsMediaPlayerPrepared = false;
        try {
            mMediaPlayer.setDataSource(data);
            mMediaPlayer.prepare();
            mIsMediaPlayerPrepared = true;
        } catch (Exception e) {
        }

        if (mIsMediaPlayerPrepared) {
            if (mPlaybackInfoListener != null) {
                mPlaybackInfoListener.onStateChanged(PlaybackInfoListener.State.PREPARED);
                final int duration = mMediaPlayer.getDuration();
                mPlaybackInfoListener.onDurationChanged(duration);
                mPlaybackInfoListener.onPositionChanged(0);
            }
        }
    }

    public void setSong(Song song) {
        mSong = song;
        setDataSource(mSong.getData());
    }

    public Song getSong() {
        return mSong;
    }

    public void setAlbumArt(Bitmap albumArt) {
        mAlbumArt = albumArt;

        updateMediaMetadata();
        if (mSong != null) {
            mNotificationUtil.sendNotification(mSong.getTitle(), mSong.getArtist(), mAlbumArt, !isPlaying(),
                    mSession.getSessionToken());
        }
    }

    public Bitmap getAlbumArt() {
        return mAlbumArt;
    }

    public void updateShowAlbumArtOnLockScreen() {
        mShowAlbumArtOnLockScreen = Config.getShowAlbumArtOnLockScreen(this);
        setAlbumArt(mAlbumArt);
    }

    private void updateMediaMetadata() {
        Bitmap albumArtCopy = null;
        if (mAlbumArt != null && mShowAlbumArtOnLockScreen) {
            Bitmap.Config config = mAlbumArt.getConfig();
            if (config == null) {
                config = Bitmap.Config.ARGB_8888;
            }
            albumArtCopy = mAlbumArt.copy(config, false);
        }
        mSession.setMetadata(new MediaMetadata.Builder()
                .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, albumArtCopy)
                .build());
    }

    public void release() {
        if (mIsMediaPlayerPrepared) {
            if (mMediaPlayer.isPlaying()) {
                stopForeground(true);
            } else {
                NotificationUtil.removeNotification(this);
            }
        }
        mMediaPlayer.release();
        mMediaPlayer = null;
        mSession.setActive(false);
        mSession.release();
        mIsMediaPlayerPrepared = false;
        if (mPlaybackInfoListener != null) {
            mPlaybackInfoListener.onStateChanged(PlaybackInfoListener.State.PAUSED);
            mPlaybackInfoListener.onStateChanged(PlaybackInfoListener.State.RESET);
        }
        stopUpdatingCallbackWithPosition(true);
    }

    public boolean isPlaying() {
        if (mIsMediaPlayerPrepared) {
            return mMediaPlayer.isPlaying();
        }
        return false;
    }

    public void play() {
        if (mIsMediaPlayerPrepared && !mMediaPlayer.isPlaying()) {
            mMediaPlayer.start();
            mSession.setPlaybackState(new PlaybackState.Builder()
                    .setState(PlaybackState.STATE_PLAYING, 0, 1.0f)
                    .build());
            mSession.setActive(true);
            startForeground(NotificationUtil.MUSIC_PLAYBACK_NOTIFICATION_ID,
                    mNotificationUtil.createNotification(null, null, null, !isPlaying(),
                    mSession.getSessionToken()));
            if (mPlaybackInfoListener != null) {
                mPlaybackInfoListener.onStateChanged(PlaybackInfoListener.State.PLAYING);
                startUpdatingCallbackWithPosition();
            }
        }
    }

    public void reset() {
        if (mIsMediaPlayerPrepared) {
            if (mMediaPlayer.isPlaying()) {
                stopForeground(true);
            } else {
                NotificationUtil.removeNotification(this);
            }
            mMediaPlayer.reset();
            mIsMediaPlayerPrepared = false;
            mSession.setActive(false);
            if (mPlaybackInfoListener != null) {
                mPlaybackInfoListener.onStateChanged(PlaybackInfoListener.State.PAUSED);
                mPlaybackInfoListener.onStateChanged(PlaybackInfoListener.State.RESET);
            }
            stopUpdatingCallbackWithPosition(true);
        }
    }

    public void pause() {
        if (mIsMediaPlayerPrepared && mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            mSession.setPlaybackState(new PlaybackState.Builder()
                    .setState(PlaybackState.STATE_PAUSED, 0, 1.0f)
                    .build());
            stopForeground(false);
            mNotificationUtil.sendNotification(mSong.getTitle(), mSong.getArtist(), mAlbumArt, !isPlaying(),
                    mSession.getSessionToken());
            mNotificationUtil.sendNotification(null, null, null, !isPlaying(),
                    mSession.getSessionToken());
            if (mPlaybackInfoListener != null) {
                mPlaybackInfoListener.onStateChanged(PlaybackInfoListener.State.PAUSED);
            }
        }
    }

    public void seekTo(int position) {
        if (mIsMediaPlayerPrepared) {
            mMediaPlayer.seekTo(position);
        }
    }

    public int getCurrentPosition() {
        int position = 0;
        if (mIsMediaPlayerPrepared) {
            position = mMediaPlayer.getCurrentPosition();
        }
        return position;
    }

    public int getDuration() {
        int duration = 0;
        if (mIsMediaPlayerPrepared && mMediaPlayer.getDuration() != -1) {
            duration = mMediaPlayer.getDuration();
        }
        return duration;
    }

    /**
     * Syncs the mMediaPlayer position with mPlaybackProgressCallback via recurring task.
     */
    private void startUpdatingCallbackWithPosition() {
        if (mExecutor == null) {
            mExecutor = Executors.newSingleThreadScheduledExecutor();
        }
        if (mSeekbarPositionUpdateTask == null) {
            mSeekbarPositionUpdateTask = new Runnable() {
                @Override
                public void run() {
                    updateProgressCallbackTask();
                }
            };
        }
        mExecutor.scheduleAtFixedRate(
                mSeekbarPositionUpdateTask,
                0,
                PLAYBACK_POSITION_REFRESH_INTERVAL_MS,
                TimeUnit.MILLISECONDS
        );
    }

    // Reports media playback position to mPlaybackProgressCallback.
    private void stopUpdatingCallbackWithPosition(boolean resetUIPlaybackPosition) {
        if (mExecutor != null) {
            mExecutor.shutdownNow();
            mExecutor = null;
            mSeekbarPositionUpdateTask = null;
            if (resetUIPlaybackPosition && mPlaybackInfoListener != null) {
                mPlaybackInfoListener.onPositionChanged(0);
            }
        }
    }

    private void updateProgressCallbackTask() {
        if (mIsMediaPlayerPrepared && mMediaPlayer.isPlaying()) {
            int currentPosition = mMediaPlayer.getCurrentPosition();
            if (mPlaybackInfoListener != null) {
                mPlaybackInfoListener.onPositionChanged(currentPosition);
            }
        }
    }
}
