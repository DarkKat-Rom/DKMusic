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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;

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
    public static final String KEY_ACTION_SHUTDOWN   = "shutdown";

    private static final int IDLE_DELAY = 5 * 60 * 1000;

    private final IBinder mBinder = new LocalBinder();

    private boolean mServiceInUse = false;

    private MediaPlayer mMediaPlayer;
    private MediaSession mSession;
    private PlaybackInfoListener mPlaybackInfoListener;
    private ScheduledExecutorService mExecutor;
    private Runnable mSeekbarPositionUpdateTask;

    private NotificationUtil mNotificationUtil;

    private boolean mIsMediaPlayerPrepared = false;
    private boolean mIsMediaPlayerReleased = true;

    private Song mSong;
    private Bitmap mAlbumArt;

    private boolean mShowAlbumArtOnLockScreen;

    private AlarmManager mAlarmManager;
    private PendingIntent mShutdownIntent;
    private boolean mShutdownScheduled;

    @Override
    public void onCreate() {
        super.onCreate();

        initializeMediaPlayer();
        mSession = new MediaSession(this, "DKMusic");
        mNotificationUtil = new NotificationUtil(this);
        mShowAlbumArtOnLockScreen = Config.getShowAlbumArtOnLockScreen(this);

        final Intent shutdownIntent = new Intent(this, MusicPlaybackService.class);
        shutdownIntent.setAction(KEY_ACTION_SHUTDOWN);

        mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        mShutdownIntent = PendingIntent.getService(this, 0, shutdownIntent, 0);

        mSong = Config.getSong(this);
        if (mSong != null) {
            setDataSource(mSong.getData());
            int position = Config.getPlaybackPosition(this);
            if (position > 0) {
                savePlaybackPosition();
                seekTo(position);

            }
        }

        scheduleDelayedShutdown();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (!mIsMediaPlayerReleased) {
            savePlaybackPosition();
            release();
        }
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        if (intent != null) {
            final String action = intent.getAction();

            if (KEY_ACTION_SHUTDOWN.equals(action)) {
                mShutdownScheduled = false;
                releaseAndStop();
                return START_NOT_STICKY;
            }

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
        cancelShutdown();
        mServiceInUse = true;
        return mBinder;
    }

    @Override
    public boolean onUnbind(final Intent intent) {
        mServiceInUse = false;
        scheduleDelayedShutdown();
        return true;
    }

    @Override
    public void onRebind(final Intent intent) {
        cancelShutdown();
        mServiceInUse = true;
    }

    private void scheduleDelayedShutdown() {
        mAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + IDLE_DELAY, mShutdownIntent);
        mShutdownScheduled = true;
    }

    private void cancelShutdown() {
        if (mShutdownScheduled) {
            mAlarmManager.cancel(mShutdownIntent);
            mShutdownScheduled = false;
        }
    }

    private void releaseAndStop() {
        if (isPlaying()) {
            return;
        }

        if (!mServiceInUse) {
            if (!mIsMediaPlayerReleased) {
                savePlaybackPosition();
                release();
            }
            stopSelf();
        }
    }

    private void initializeMediaPlayer() {
        mMediaPlayer = new MediaPlayer();
        mIsMediaPlayerReleased = false;
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
            if (mIsMediaPlayerPrepared) {
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
        saveSong();
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
                .putString(MediaMetadata.METADATA_KEY_TITLE, mSong.getTitle())
                .putString(MediaMetadata.METADATA_KEY_ARTIST, mSong.getArtist())
                .putString(MediaMetadata.METADATA_KEY_ALBUM, mSong.getAlbum())
                .putLong(MediaMetadata.METADATA_KEY_DURATION, getDuration())
                .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, albumArtCopy)
                .build());
    }

    private void saveSong() {
        Song song = getSong();
        if (song == null) {
            return;
        }

        Config.saveSong(this, song);
    }

    private void savePlaybackPosition() {
        Config.savePlaybackPosition(this, getCurrentPosition());
    }

    public void release() {
        if (mIsMediaPlayerPrepared) {
            if (mMediaPlayer.isPlaying()) {
                stopForeground(true);
            } else {
                NotificationUtil.removeNotification(this);
            }
        } else {
            NotificationUtil.removeNotification(this);
        }
        mMediaPlayer.release();
        mMediaPlayer = null;
        mIsMediaPlayerPrepared = false;
        mIsMediaPlayerReleased = true;
        mSession.setActive(false);
        mSession.release();
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
                    .setState(PlaybackState.STATE_PLAYING, getCurrentPosition(), 1.0f)
                    .build());
            mSession.setActive(true);
            startForeground(NotificationUtil.MUSIC_PLAYBACK_NOTIFICATION_ID,
                    mNotificationUtil.createNotification(null, null, null, !isPlaying(),
                    mSession.getSessionToken()));
            if (mPlaybackInfoListener != null) {
                mPlaybackInfoListener.onStateChanged(PlaybackInfoListener.State.PLAYING);
                startUpdatingCallbackWithPosition();
            }
            cancelShutdown();
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
                    .setState(PlaybackState.STATE_PAUSED, getCurrentPosition(), 1.0f)
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
            int playbackState = mMediaPlayer.isPlaying()
                    ? PlaybackState.STATE_PLAYING
                    : PlaybackState.STATE_PAUSED;
            mMediaPlayer.seekTo(position);
            mSession.setPlaybackState(new PlaybackState.Builder()
                    .setState(playbackState, getCurrentPosition(), 1.0f)
                    .build());
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
