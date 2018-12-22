/*
 * Copyright 2017 Google Inc. All rights reserved.
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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Allows playback of a single MP3 file via the UI. It contains a {@link MediaPlayerHolder}
 * which implements the {@link PlayerAdapter} interface that the activity uses to control
 * audio playback.
 */
public final class MainActivity extends AppCompatActivity {

    public static final String TAG = "DKMusic/MainActivity";
    public static final int MEDIA_RES_ID = R.raw.jazz_in_paris;
    public static boolean DEBUG = false;

    private View mRoot;
    private View mListItem;
    private ImageView mListItemAlbumArtSmall;
    private TextView mListItemTitle;
    private TextView mListItemArtist;
    private ImageView mAlbumArt;
    private SeekBar mSeekbarAudio;
    private TextView mSongPlayTime;
    private TextView mSongTimeRemaining;
    private TextView mTitle;
    private TextView mArtist;
    private ImageView mAlbumArtSmall;
    private ImageView mPlayPauseButton;
    private ImageView mResetButton;
    private PlayerAdapter mPlayerAdapter;
    private boolean mUserIsSeeking = false;
    private int mDuration;

    private MediaMetadataRetriever mRetriever;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeUI();
        initializeSeekbar();
        initializePlaybackController();
        log("onCreate: finished");
    }

    @Override
    protected void onStart() {
        super.onStart();
        mPlayerAdapter.loadMedia(MEDIA_RES_ID);
        log("onStart: create MediaPlayer");
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isChangingConfigurations() && mPlayerAdapter.isPlaying()) {
            log("onStop: don't release MediaPlayer as screen is rotating & playing");
        } else {
            mPlayerAdapter.release();
            log("onStop: release MediaPlayer");
        }
    }

    private void initializeUI() {
        mRoot = findViewById(R.id.root);
        mListItem = findViewById(R.id.list_item_root);
        mListItemAlbumArtSmall = (ImageView) findViewById(R.id.list_item_album_art_small);
        mListItemTitle = (TextView) findViewById(R.id.list_item_song_title);
        mListItemArtist = (TextView) findViewById(R.id.list_item_artist_title);
        mAlbumArt = (ImageView) findViewById(R.id.album_art);
        mSeekbarAudio = (SeekBar) findViewById(R.id.seekbar_audio);
        mSongPlayTime = (TextView) findViewById(R.id.song_play_time);
        mSongTimeRemaining = (TextView) findViewById(R.id.song_time_remaining);
        mTitle = (TextView) findViewById(R.id.song_title);
        mArtist = (TextView) findViewById(R.id.artist_title);
        mAlbumArtSmall = (ImageView) findViewById(R.id.album_art_small);
        mPlayPauseButton = (ImageView) findViewById(R.id.button_play_pause);
        mResetButton = (ImageView) findViewById(R.id.button_reset);

        mListItem.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        applyMediaMetadata();
                        mSeekbarAudio.setEnabled(true);
                        mPlayPauseButton.setEnabled(true);
                        mResetButton.setEnabled(true);
                        view.setEnabled(false);
                        updateTimes(0);
                    }
                });

        setupRetriever();

        mSeekbarAudio.setEnabled(false);

        mPlayPauseButton.setEnabled(false);
        mPlayPauseButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mPlayerAdapter.isPlaying()) {
                            mPlayerAdapter.pause();
                            mPlayPauseButton.setImageResource(R.drawable.ic_action_play);
                        } else {
                            mPlayerAdapter.play();
                            mPlayPauseButton.setImageResource(R.drawable.ic_action_pause);
                        }
                    }
                });
        mResetButton.setEnabled(false);
        mResetButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mPlayerAdapter.reset();
                        mPlayPauseButton.setImageResource(R.drawable.ic_action_play);
                        removeMediaMetadata();
                        mListItem.setEnabled(true);
                        mSeekbarAudio.setEnabled(false);
                        mPlayPauseButton.setEnabled(false);
                        view.setEnabled(false);
                        updateTimes(0);
                    }
                });
    }

    private void setupRetriever() {
        mRetriever = new MediaMetadataRetriever();
        Uri path = Uri.parse("android.resource://" + getPackageName()  + "/" + MEDIA_RES_ID);

        mRetriever.setDataSource(this, path);
        byte [] data = mRetriever.getEmbeddedPicture();
        if (data != null) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            mListItemAlbumArtSmall.setImageBitmap(bitmap);
        }

        String defaultSongTitle = getString(R.string.default_song_title);
        String defaultArtistTitle = getString(R.string.default_artist_title);
        String songTitle = mRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        String artistTitle = mRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);

        if (songTitle == null) {
            mListItemTitle.setText(defaultSongTitle + " -");
        } else {
            mListItemTitle.setText(defaultSongTitle + " " + songTitle);
        }
        if (artistTitle == null) {
            mListItemArtist.setText(defaultArtistTitle + " -");
        } else {
            mListItemArtist.setText(defaultArtistTitle + " " + artistTitle);
        }

        mTitle.setText(defaultSongTitle + " -");
        mArtist.setText(defaultArtistTitle + " -");
    }

    private void applyMediaMetadata() {
        byte [] data = mRetriever.getEmbeddedPicture();
        if (data != null) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            mAlbumArt.setImageBitmap(bitmap);
            mAlbumArtSmall.setImageBitmap(bitmap);
        }

        String defaultSongTitle = getString(R.string.default_song_title);
        String defaultArtistTitle = getString(R.string.default_artist_title);
        String songTitle = mRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        String artistTitle = mRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);

        if (songTitle == null) {
            mTitle.setText(defaultSongTitle + " -");
        } else {
            mTitle.setText(defaultSongTitle + " " + songTitle);
        }
        if (artistTitle == null) {
            mArtist.setText(defaultArtistTitle + " -");
        } else {
            mArtist.setText(defaultArtistTitle + " " + artistTitle);
        }
    }

    private void removeMediaMetadata() {
        mAlbumArt.setImageResource(R.drawable.default_artwork);
        mAlbumArtSmall.setImageResource(R.drawable.default_artwork);

        String defaultSongTitle = getString(R.string.default_song_title);
        String defaultArtistTitle = getString(R.string.default_artist_title);
        mTitle.setText(defaultSongTitle + " -");
        mArtist.setText(defaultArtistTitle + " -");
    }

    private void initializePlaybackController() {
        MediaPlayerHolder mMediaPlayerHolder = new MediaPlayerHolder(this);
        log("initializePlaybackController: created MediaPlayerHolder");
        mMediaPlayerHolder.setPlaybackInfoListener(new PlaybackListener());
        mPlayerAdapter = mMediaPlayerHolder;
        log("initializePlaybackController: MediaPlayerHolder progress callback set");
    }

    private void initializeSeekbar() {
        mSeekbarAudio.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    int userSelectedPosition = 0;

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        mUserIsSeeking = true;
                    }

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (fromUser) {
                            userSelectedPosition = progress;
                            updateTimes(userSelectedPosition);
                        }
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        mUserIsSeeking = false;
                        mPlayerAdapter.seekTo(userSelectedPosition);
                    }
                });
    }

    private void updateTimes(final int position) {
        mRoot.post(new Runnable() {
            @Override
            public void run() {
                int secs = 0;
                int mins = 0;

                secs = position / 1000;
                secs %= 3600;
                mins = secs / 60;
                secs %= 60;
                String playTime = mins + ":" + (secs < 10 ? "0" + secs : secs);

                secs = mDuration / 1000 - position / 1000;
                secs %= 3600;
                mins = secs / 60;
                secs %= 60;
                String timeRemaining = "-" + mins + ":" + (secs < 10 ? "0" + secs : secs);

                TypedValue tv = new TypedValue();
                int textColor = 0;
                int resId = mUserIsSeeking
                        ? R.attr.colorAccent : android.R.attr.textColorTertiary;
                getTheme().resolveAttribute(resId, tv, true);
                if (tv.type >= TypedValue.TYPE_FIRST_COLOR_INT
                        && tv.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                    textColor = tv.data;
                } else {
                    textColor = getColor(tv.resourceId);
                }

                mSongPlayTime.setTextColor(textColor);
                mSongTimeRemaining.setTextColor(textColor);
                mSongPlayTime.setText(mSeekbarAudio.isEnabled() ? playTime : "0:00");
                mSongTimeRemaining.setText(mSeekbarAudio.isEnabled() ? timeRemaining : "-0:00");
            }
        });
    }

    private void log(String message) {
        if (DEBUG) {
            Log.d(TAG, message);
        }
    }

    public class PlaybackListener extends PlaybackInfoListener {

        @Override
        public void onDurationChanged(int duration) {
            mSeekbarAudio.setMax(duration);
            mDuration = duration;
            updateTimes(0);
            log(String.format("setPlaybackDuration: setMax(%d)", duration));
        }

        @Override
        public void onPositionChanged(int position) {
            if (!mUserIsSeeking) {
                mSeekbarAudio.setProgress(position, true);
                log(String.format("setPlaybackPosition: setProgress(%d)", position));
                updateTimes(position);
            }
        }

        @Override
        public void onStateChanged(@State int state) {
            String stateToString = PlaybackInfoListener.convertStateToString(state);
            onLogUpdated(String.format("onStateChanged(%s)", stateToString));
        }

        @Override
        public void onPlaybackCompleted() {
        }

        @Override
        public void onLogUpdated(String message) {
            log(message);
        }
    }
}
