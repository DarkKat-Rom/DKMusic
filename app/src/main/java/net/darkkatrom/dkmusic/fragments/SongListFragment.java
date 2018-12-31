/*
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

package net.darkkatrom.dkmusic.fragments;

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
import android.support.v4.app.Fragment;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bumptech.glide.request.FutureTarget;

import net.darkkatrom.dkmusic.R;
import net.darkkatrom.dkmusic.GlideApp;
import net.darkkatrom.dkmusic.activities.SettingsActivity;
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

public final class SongListFragment extends Fragment implements
        SongAdapter.OnSongClickedListener {

    public static final String TAG = "DKMusic/SongListFragment";
    public static boolean DEBUG = false;

    private View mRoot;
    private TextView mLoadingMediaText;
    private ProgressBar mLoadingMediaProgress;
    private RecyclerView mList;
    private View mBottomSheetBar;
    private ImageView mAlbumArtSmall;
    private ImageView mPlayPauseButtonSmall;
    private ImageView mAlbumArtBig;
    private VisualizerView mVisualizerView;
    private SeekBar mSeekbarAudio;
    private TextView mSongPlayTime;
    private TextView mSongTimeRemaining;
    private TextView mTitle;
    private TextView mArtist;
    private ImageView mPlayPauseButtonBig;

    private LockableBottomSheetBehavior mBottomSheetBehavior;
    private PlayerAdapter mPlayerAdapter;

    private boolean mCanReadExternalStorage = false;
    private boolean mUserIsSeeking = false;
    private int mDuration;
    private boolean mShowVisualizer;
    private int mDefaultVisualizerColor = 0;
    private int mVisualizerColor = 0;

    private List<Song> mSongs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mCanReadExternalStorage = getActivity().checkSelfPermission(permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
        mDefaultVisualizerColor = getActivity().getColor(R.color.visualizer_fill_color);

        mRoot = inflater.inflate(R.layout.fragment_song_list, container, false);
        initializeUI();
        initializeSeekbar();
        initializePlaybackController();

        log("onCreateView: finished");
        return mRoot;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mCanReadExternalStorage
                != (getActivity().checkSelfPermission(permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED)) {
            checkPermission();
        }
        updateVisualizer();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (getActivity().isChangingConfigurations() && mPlayerAdapter.isPlaying()) {
            log("onStop: don't release MediaPlayer as screen is rotating & playing");
        } else {
            mPlayerAdapter.release();
            log("onStop: release MediaPlayer");
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_main, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.item_settings) {
            Intent intent = new Intent(getActivity(), SettingsActivity.class);
            startActivity(intent);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
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

    private void initializeUI() {
        mLoadingMediaText = (TextView) mRoot.findViewById(R.id.loading_media_text);
        mLoadingMediaProgress = (ProgressBar) mRoot.findViewById(R.id.loading_media_progress);
        mList = (RecyclerView) mRoot.findViewById(R.id.list);
        mBottomSheetBar = mRoot.findViewById(R.id.bottom_sheet_bar);
        mAlbumArtSmall = (ImageView) mRoot.findViewById(R.id.album_art_small);
        mPlayPauseButtonSmall = (ImageView) mRoot.findViewById(R.id.button_play_pause_small);
        mAlbumArtBig = (ImageView) mRoot.findViewById(R.id.album_art_big);
        mVisualizerView = (VisualizerView) mRoot.findViewById(R.id.visualizerView);
        mSeekbarAudio = (SeekBar) mRoot.findViewById(R.id.seekbar_audio);
        mSongPlayTime = (TextView) mRoot.findViewById(R.id.song_play_time);
        mSongTimeRemaining = (TextView) mRoot.findViewById(R.id.song_time_remaining);
        mTitle = (TextView) mRoot.findViewById(R.id.song_title);
        mArtist = (TextView) mRoot.findViewById(R.id.artist_title);
        mPlayPauseButtonBig = (ImageView) mRoot.findViewById(R.id.button_play_pause_big);
        mBottomSheetBehavior = (LockableBottomSheetBehavior) LockableBottomSheetBehavior.from(
                mRoot.findViewById(R.id.bottom_sheet));

        checkPermission();

        mVisualizerView.initialize(getActivity());
        mShowVisualizer = Config.getShowVisualizer(getActivity());

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
                            mPlayPauseButtonSmall.setImageResource(R.drawable.ic_action_play);
                            mVisualizerView.setPlaying(false);
                        } else {
                            mPlayerAdapter.play();
                            mPlayPauseButtonBig.setImageResource(
                                    R.drawable.ic_action_pause_circle_outline);
                            mPlayPauseButtonSmall.setImageResource(R.drawable.ic_action_pause);
                            if (mShowVisualizer) {
                                mVisualizerView.setPlaying(true);
                            }
                        }
                    }
                });

        mPlayPauseButtonSmall.setEnabled(false);
        mPlayPauseButtonSmall.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mPlayerAdapter.isPlaying()) {
                            mPlayerAdapter.pause();
                            mPlayPauseButtonBig.setImageResource(
                                    R.drawable.ic_action_play_circle_outline);
                            mPlayPauseButtonSmall.setImageResource(R.drawable.ic_action_play);
                            mVisualizerView.setPlaying(false);
                        } else {
                            mPlayerAdapter.play();
                            mPlayPauseButtonBig.setImageResource(
                                    R.drawable.ic_action_pause_circle_outline);
                            mPlayPauseButtonSmall.setImageResource(R.drawable.ic_action_pause);
                            if (mShowVisualizer) {
                                mVisualizerView.setPlaying(true);
                            }
                        }
                    }
                });
    }

    private void initializePlaybackController() {
        MediaPlayerHolder mMediaPlayerHolder = new MediaPlayerHolder(getActivity());
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

    private void setupList() {
        mSongs = new ArrayList<Song>();
        LoadSongsTask task = new LoadSongsTask(getActivity(), mList, mSongs, this, mLoadingMediaText,
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
            .into(mAlbumArtBig);
     
        AsyncTask.execute(new Runnable() {
           @Override
           public void run() {
                FutureTarget<Bitmap> futureTarget = GlideApp
                    .with(SongListFragment.this)
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
        mAlbumArtBig.setImageResource(R.drawable.default_artwork);
        mAlbumArtSmall.setImageResource(R.drawable.default_artwork);

        String defaultSongTitle = getString(R.string.default_song_title);
        String defaultArtistTitle = getString(R.string.default_artist_title);
        mTitle.setText(defaultSongTitle + " -");
        mArtist.setText(defaultArtistTitle + " -");
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
                getActivity().getTheme().resolveAttribute(resId, tv, true);
                if (tv.type >= TypedValue.TYPE_FIRST_COLOR_INT
                        && tv.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                    textColor = tv.data;
                } else {
                    textColor = getActivity().getColor(tv.resourceId);
                }

                mSongPlayTime.setTextColor(textColor);
                mSongTimeRemaining.setTextColor(textColor);
                mSongPlayTime.setText(mSeekbarAudio.isEnabled() ? playTime : "0:00");
                mSongTimeRemaining.setText(mSeekbarAudio.isEnabled() ? timeRemaining : "-0:00");
            }
        });
    }

    private void updateVisualizer() {
        boolean show = Config.getShowVisualizer(getActivity());
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

    @Override
    public void onSongClicked(Song song, int position) {
        applyMediaMetadata(song);
        mSeekbarAudio.setEnabled(true);
        mPlayPauseButtonSmall.setEnabled(true);
        updateTimes(0);
        mBottomSheetBehavior.setLocked(false);
        mPlayerAdapter.setDataSource(song.getData());
    }

    private void checkPermission() {
        if (getActivity().checkSelfPermission(permission.READ_EXTERNAL_STORAGE) !=
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
        getActivity().getTheme().resolveAttribute(resId, tv, true);
        if (tv.type >= TypedValue.TYPE_FIRST_COLOR_INT
                && tv.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            textColor = tv.data;
        } else {
            textColor = getActivity().getColor(tv.resourceId);
        }
        return textColor;
    }

    private void log(String message) {
        if (DEBUG) {
            Log.d(TAG, message);
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
                    if (mPlayPauseButtonSmall.isEnabled()) {
                        mPlayPauseButtonSmall.setEnabled(false);
                    }
                } else {
                    mPlayPauseButtonSmall.setEnabled(true);
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
            ContentResolver contentResolver = getActivity().getContentResolver();

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
