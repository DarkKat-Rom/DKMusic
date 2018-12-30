/*
 * Copyright 2017 Google Inc. All rights reserved.
 *
 * Copyright (C) Copyright (C) 2018 DarkKat
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

import android.Manifest.permission;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetBehavior.BottomSheetCallback;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bumptech.glide.request.FutureTarget;

import net.darkkatrom.dkmusic.R;
import net.darkkatrom.dkmusic.GlideApp;
import net.darkkatrom.dkmusic.adapters.PlayerAdapter;
import net.darkkatrom.dkmusic.adapters.SongAdapter;
import net.darkkatrom.dkmusic.holders.MediaPlayerHolder;
import net.darkkatrom.dkmusic.listeners.PlaybackInfoListener;
import net.darkkatrom.dkmusic.models.Song;
import net.darkkatrom.dkmusic.utils.Config;
import net.darkkatrom.dkmusic.widgets.LockableBottomSheetBehavior;
import net.darkkatrom.dkmusic.widgets.VisualizerView;

import java.lang.InterruptedException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.List;

/**
 * Allows playback of a single MP3 file via the UI. It contains a {@link MediaPlayerHolder}
 * which implements the {@link PlayerAdapter} interface that the activity uses to control
 * audio playback.
 */
public final class MainActivity extends AppCompatActivity implements
        SongAdapter.OnSongClickedListener {

    public static final String TAG = "DKMusic/MainActivity";
    public static boolean DEBUG = false;

    private View mRoot;
    private TextView mLoadingMediaText;
    private ProgressBar mLoadingMediaProgress;
    private RecyclerView mList;
    private ImageView mAlbumArt;
    private VisualizerView mVisualizerView;
    private SeekBar mSeekbarAudio;
    private TextView mSongPlayTime;
    private TextView mSongTimeRemaining;
    private TextView mTitle;
    private TextView mArtist;
    private ImageView mPlayPauseButtonBig;
    private View mBottomSheetBar;
    private ImageView mAlbumArtSmall;
    private ImageView mPlayPauseButton;
    private PlayerAdapter mPlayerAdapter;
    private boolean mUserIsSeeking = false;
    private int mDuration;
    private boolean mShowVisualizer;
    private int mDefaultVisualizerColor = 0xbffafafa;
    private int mVisualizerColor = 0;

    private boolean mCanReadExternalStorage = false;

    private LockableBottomSheetBehavior mBottomSheetBehavior;

    private List<Song> mSongs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mCanReadExternalStorage = checkSelfPermission(permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
        initializeUI();
        initializeSeekbar();
        initializePlaybackController();
        log("onCreate: finished");

        mDefaultVisualizerColor = getColor(R.color.visualizer_fill_color);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mCanReadExternalStorage != (checkSelfPermission(permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED)) {
            checkPermission();
        }
        updateVisualizer();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.item_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void initializeUI() {
        mRoot = findViewById(R.id.root);
        mLoadingMediaText = (TextView) findViewById(R.id.loading_media_text);
        mLoadingMediaProgress = (ProgressBar) findViewById(R.id.loading_media_progress);
        mList = (RecyclerView) findViewById(R.id.list);
        mAlbumArt = (ImageView) findViewById(R.id.album_art);
        mVisualizerView = (VisualizerView) findViewById(R.id.visualizerView);
        mSeekbarAudio = (SeekBar) findViewById(R.id.seekbar_audio);
        mSongPlayTime = (TextView) findViewById(R.id.song_play_time);
        mSongTimeRemaining = (TextView) findViewById(R.id.song_time_remaining);
        mTitle = (TextView) findViewById(R.id.song_title);
        mArtist = (TextView) findViewById(R.id.artist_title);
        mPlayPauseButtonBig = (ImageView) findViewById(R.id.button_play_pause_big);
        mBottomSheetBar = findViewById(R.id.bottom_sheet_bar);
        mAlbumArtSmall = (ImageView) findViewById(R.id.album_art_small);
        mPlayPauseButton = (ImageView) findViewById(R.id.button_play_pause);

        checkPermission();

        mVisualizerView.initialize(this);

        mShowVisualizer = Config.getShowVisualizer(this);

        mBottomSheetBehavior = (LockableBottomSheetBehavior) LockableBottomSheetBehavior.from(
                findViewById(R.id.bottom_sheet));
        mBottomSheetBehavior.setBottomSheetCallback(new Callback());
        mBottomSheetBehavior.setLocked(true);

        mSeekbarAudio.setEnabled(false);

        mPlayPauseButtonBig.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mPlayerAdapter.isPlaying()) {
                            mPlayerAdapter.pause();
                            mPlayPauseButtonBig.setImageResource(
                                    R.drawable.ic_action_play_circle_outline);
                            mPlayPauseButton.setImageResource(R.drawable.ic_action_play);
                            mVisualizerView.setPlaying(false);
                        } else {
                            mPlayerAdapter.play();
                            mPlayPauseButtonBig.setImageResource(
                                    R.drawable.ic_action_pause_circle_outline);
                            mPlayPauseButton.setImageResource(R.drawable.ic_action_pause);
                            if (mShowVisualizer) {
                                mVisualizerView.setPlaying(true);
                            }
                        }
                    }
                });

        mPlayPauseButton.setEnabled(false);
        mPlayPauseButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mPlayerAdapter.isPlaying()) {
                            mPlayerAdapter.pause();
                            mPlayPauseButtonBig.setImageResource(
                                    R.drawable.ic_action_play_circle_outline);
                            mPlayPauseButton.setImageResource(R.drawable.ic_action_play);
                            mVisualizerView.setPlaying(false);
                        } else {
                            mPlayerAdapter.play();
                            mPlayPauseButtonBig.setImageResource(
                                    R.drawable.ic_action_pause_circle_outline);
                            mPlayPauseButton.setImageResource(R.drawable.ic_action_pause);
                            if (mShowVisualizer) {
                                mVisualizerView.setPlaying(true);
                            }
                        }
                    }
                });
    }

    private void setupList() {
        mSongs = new ArrayList<Song>();
        LoadSongsTask task = new LoadSongsTask(this, mList, mSongs, this, mLoadingMediaText,
                mLoadingMediaProgress);
        task.execute();
    }

    private void applyMediaMetadata(final Song song) {
        GlideApp.with(this)
            .asBitmap()
            .load(song.getAlbumArtUri())
            .placeholder(R.drawable.default_artwork)
            .fitCenter()
            .into(mAlbumArtSmall);
        mTitle.setText(song.getTitle());
        mArtist.setText(song.getArtist());

        GlideApp.with(this)
            .asBitmap()
            .load(song.getAlbumArtUri())
            .placeholder(R.drawable.default_artwork)
            .fitCenter()
            .into(mAlbumArt);
     
        AsyncTask.execute(new Runnable() {
           @Override
           public void run() {
                FutureTarget<Bitmap> futureTarget = GlideApp
                    .with(MainActivity.this)
                    .asBitmap()
                    .load(song.getAlbumArtUri())
                    .submit();

                try {
                    Bitmap bitmap = futureTarget.get();
                    Palette p = Palette.from(bitmap).generate();
                    mVisualizerColor = p.getDarkVibrantColor(mDefaultVisualizerColor);
                } catch (ExecutionException | InterruptedException e) {
                    mVisualizerColor = mDefaultVisualizerColor;
                }
                mVisualizerView.setColor(mVisualizerColor);
                futureTarget.cancel(false);
           }
        });
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

    private void updateVisualizer() {
        boolean show = Config.getShowVisualizer(this);
        if (mShowVisualizer != show) {
            mShowVisualizer = show;
            if (mPlayerAdapter.isPlaying()) {
                if (mShowVisualizer) {
                    mVisualizerView.setPlaying(true);
                } else {
                    mVisualizerView.setPlaying(false);
                }
            } else {
                mVisualizerView.setPlaying(false);
            }
        }
    }

    private void log(String message) {
        if (DEBUG) {
            Log.d(TAG, message);
        }
    }

    @Override
    public void onSongClicked(Song song, int position) {
        applyMediaMetadata(song);
        mSeekbarAudio.setEnabled(true);
        mPlayPauseButton.setEnabled(true);
        updateTimes(0);
        mBottomSheetBehavior.setLocked(false);
        mPlayerAdapter.setDataSource(song.getData());
    }

    private void checkPermission() {
        if (checkSelfPermission(permission.READ_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] { permission.READ_EXTERNAL_STORAGE }, 1);
        } else {
            mCanReadExternalStorage = true;
            setupList();
        }
    }

    private int getLoadingTextColor(boolean error) {
        TypedValue tv = new TypedValue();
        int textColor = 0;
        int resId = error
                ? R.attr.colorError : android.R.attr.textColorPrimary;
        getTheme().resolveAttribute(resId, tv, true);
        if (tv.type >= TypedValue.TYPE_FIRST_COLOR_INT
                && tv.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            textColor = tv.data;
        } else {
            textColor = getColor(tv.resourceId);
        }
        return textColor;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mCanReadExternalStorage = true;
                setupList();
            } else {
                mCanReadExternalStorage = false;
                mLoadingMediaText.setTextColor(getLoadingTextColor(true));
                mLoadingMediaText.setText(R.string.loading_media_missing_permission_title);
                mLoadingMediaProgress.setVisibility(View.GONE);
            }
            return;
        }
    }

    public class Callback extends BottomSheetCallback {

        @Override
        public void onStateChanged(View bottomSheet, int newState) {
            switch (newState) {
                case BottomSheetBehavior.STATE_HIDDEN:
                    break;
                case BottomSheetBehavior.STATE_EXPANDED:
                    break;
                case BottomSheetBehavior.STATE_COLLAPSED:
                    break;
                case BottomSheetBehavior.STATE_DRAGGING:
                    break;
                case BottomSheetBehavior.STATE_SETTLING:
                    break;
            }
        }

        @Override
        public void onSlide(View bottomSheet, float slideOffset) {
            if (slideOffset >= 0) {
                mBottomSheetBar.setAlpha(1f - slideOffset);
                if (slideOffset > 0) {
                    if (mPlayPauseButton.isEnabled()) {
                        mPlayPauseButton.setEnabled(false);
                    }
                } else {
                    mPlayPauseButton.setEnabled(true);
                }
            }
        }
    }

    public class LoadSongsTask extends AsyncTask<Void, Integer, String> {
        Context context;
        RecyclerView rv;
        SongAdapter adapter;
        List<Song> songs;
        SongAdapter.OnSongClickedListener listener;
        TextView text;
        ProgressBar progressBar;

        public LoadSongsTask(Context context, RecyclerView rv, List<Song> songs,
                SongAdapter.OnSongClickedListener listener, TextView text, ProgressBar progressBar) {
            this.context = context;
            this.rv = rv;
            this.songs = songs;
            this.listener = listener;
            this.text = text;
            this.progressBar = progressBar;
        }

        @Override
        protected String doInBackground(Void... params) {
            ContentResolver contentResolver = getContentResolver();

            Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
            String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
            Cursor cursor = contentResolver.query(uri, null, selection, null, sortOrder);
            int currentCount = 1;
            if (cursor != null && cursor.getCount() > 0) {
                int progressMax = cursor.getCount();
                while (cursor.moveToNext()) {
                    String data = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                    String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                    String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                    String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                    long albumId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ID));
                    songs.add(new Song(data, title, artist, album, albumId));
                    publishProgress(cursor.getCount(), currentCount);
                    currentCount ++;
                }
            }
            cursor.close();
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            if (progress[1] == 1) {
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setMax(progress[0]);
                text.setTextColor(getLoadingTextColor(false));
                text.setText(R.string.loading_media_title);
            }
            progressBar.setProgress(progress[1]);
        }

        @Override
        protected void onPostExecute(String result) {
            SongAdapter adapter = new SongAdapter(context, songs);
            adapter.setOnSongClickedListener(listener);
            text.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
            rv.setVisibility(View.VISIBLE);
            rv.setAdapter(adapter);
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
