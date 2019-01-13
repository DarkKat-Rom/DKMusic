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
import android.transition.TransitionManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.request.FutureTarget;

import net.darkkatrom.dkmusic.R;
import net.darkkatrom.dkmusic.GlideApp;
import net.darkkatrom.dkmusic.activities.MainActivity;
import net.darkkatrom.dkmusic.activities.SettingsActivity;
import net.darkkatrom.dkmusic.adapters.PlayerAdapter;
import net.darkkatrom.dkmusic.adapters.SongAdapter;
import net.darkkatrom.dkmusic.holders.MediaPlayerHolder;
import net.darkkatrom.dkmusic.holders.SongInfoHolder;
import net.darkkatrom.dkmusic.listeners.PlaybackInfoListener;
import net.darkkatrom.dkmusic.models.Song;
import net.darkkatrom.dkmusic.utils.BitmapPaletteUtil;
import net.darkkatrom.dkmusic.utils.Config;
import net.darkkatrom.dkmusic.utils.NotificationUtil;
import net.darkkatrom.dkmusic.widgets.LockableBottomSheetBehavior;
import net.darkkatrom.dkmusic.widgets.PlayPauseProgressButton;
import net.darkkatrom.dkmusic.widgets.VisualizerView;

import java.lang.InterruptedException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.List;

public final class SongListFragment extends Fragment implements
        SongAdapter.OnSongClickedListener, PlayPauseProgressButton.OnDraggingListener {

    public static final String TAG = "DKMusic/SongListFragment";
    public static boolean DEBUG = false;

    private View mRoot;
    private TextView mLoadingMediaText;
    private ProgressBar mLoadingMediaProgress;
    private RecyclerView mList;
    private ViewGroup mBottomSheet;
    private View mBottomBar;
    private ImageView mAlbumArtSmall;
    private PlayPauseProgressButton mPlayPauseProgressButtonSmall;
    private ImageView mAlbumArtBig;
    private VisualizerView mVisualizerView;
    private TextView mSongPlayTime;
    private TextView mSongTimeRemaining;
    private TextView mTitle;
    private TextView mArtist;
    private PlayPauseProgressButton mPlayPauseProgressButtonBig;

    private LockableBottomSheetBehavior mBottomSheetBehavior;
    private boolean mBottomSheetExpanded = false;
    private PlayerAdapter mPlayerAdapter;
    private SongInfoHolder mCurrentSongInfo;
    private NotificationUtil mNotificationUtil;

    private boolean mCanReadExternalStorage = false;
    private boolean mUserIsDragging = false;
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
        mNotificationUtil = new NotificationUtil(getActivity());

        mCanReadExternalStorage = getActivity().checkSelfPermission(permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
        mDefaultVisualizerColor = getActivity().getColor(R.color.visualizer_fill_color);

        mRoot = inflater.inflate(R.layout.fragment_song_list, container, false);
        initializeUI();
        initializePlaybackController();

        log("onCreateView: finished");
        return mRoot;
    }

    @Override
    public void onStart() {
        super.onStart();

        if (mCurrentSongInfo.getSong() != null) {
            if (mCurrentSongInfo.getIsMediaPlayerReleased()) {
                mPlayerAdapter.setDataSource(mCurrentSongInfo.getSong().getData());
                if (mCurrentSongInfo.getPosition() > 0) {
                    mPlayerAdapter.seekTo(mCurrentSongInfo.getPosition());
                }
                if (mCurrentSongInfo.getIsPlaying()) {
                    mPlayerAdapter.play();
                } else {
                    if (mCurrentSongInfo.getPosition() > 0) {
                        mPlayPauseProgressButtonBig.resume();
                        mPlayPauseProgressButtonSmall.resume();
                        updateTimes(mCurrentSongInfo.getPosition());
                    }
                }
                Bundle extras = getActivity().getIntent().getExtras();
                if (extras != null) {
                    extras.putBoolean(MainActivity.KEY_ACTION_PLAY_PAUSE, mCurrentSongInfo.getIsPlaying());
                    Intent intent = getActivity().getIntent();
                    intent.putExtras(extras);
                    getActivity().setIntent(intent);
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mCanReadExternalStorage
                != (getActivity().checkSelfPermission(permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED)) {
            checkPermission();
        }

        Bundle extras = getActivity().getIntent().getExtras();
        if (extras != null) {
            if (extras.getBoolean(MainActivity.KEY_ACTION_PLAY_PAUSE)) {
                play();
            } else {
                pause();
            }
        }
        updateVisualizer();
    }

    @Override
    public void onStop() {
        super.onStop();
//        if (getActivity().isChangingConfigurations() && mPlayerAdapter.isPlaying()) {
//            log("onStop: don't release MediaPlayer as screen is rotating & playing");
//        } else {
            mCurrentSongInfo.setIsMediaPlayerReleased(true);
            mCurrentSongInfo.setPosition(mPlayerAdapter.getCurrentPosition());
            mPlayerAdapter.release();
            log("onStop: release MediaPlayer");
//        }
        NotificationUtil.removeNotification(getActivity());
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
        mBottomSheet = (ViewGroup) mRoot.findViewById(R.id.bottom_sheet);
        mBottomBar = mRoot.findViewById(R.id.bottom_bar);
        mAlbumArtSmall = (ImageView) mRoot.findViewById(R.id.album_art_small);
        mPlayPauseProgressButtonSmall = (PlayPauseProgressButton) mRoot.findViewById(R.id.button_play_pause_small);
        mAlbumArtBig = (ImageView) mRoot.findViewById(R.id.album_art_big);
        mVisualizerView = (VisualizerView) mRoot.findViewById(R.id.visualizerView);
        mSongPlayTime = (TextView) mRoot.findViewById(R.id.song_play_time);
        mSongTimeRemaining = (TextView) mRoot.findViewById(R.id.song_time_remaining);
        mTitle = (TextView) mRoot.findViewById(R.id.song_title);
        mArtist = (TextView) mRoot.findViewById(R.id.artist_title);
        mPlayPauseProgressButtonBig = (PlayPauseProgressButton) mRoot.findViewById(R.id.button_play_pause_big);
        mBottomSheetBehavior = (LockableBottomSheetBehavior) LockableBottomSheetBehavior.from(mBottomSheet);

        mCurrentSongInfo = new SongInfoHolder();

        checkPermission();

        mVisualizerView.initialize(getActivity());
        mShowVisualizer = Config.getShowVisualizer(getActivity());

        mBottomSheetBehavior.setBottomSheetCallback(new Callback());
        mBottomSheetBehavior.setLocked(true);
    }

    private void initializePlaybackController() {
        MediaPlayerHolder mMediaPlayerHolder = new MediaPlayerHolder(getActivity());
        log("initializePlaybackController: created MediaPlayerHolder");
        mMediaPlayerHolder.setPlaybackInfoListener(new PlaybackListener());
        mPlayerAdapter = mMediaPlayerHolder;
        log("initializePlaybackController: MediaPlayerHolder progress callback set");
        mPlayPauseProgressButtonBig.setPlayerAdapter(mPlayerAdapter);
        mPlayPauseProgressButtonBig.getPlayPauseButton().setFragment(this);
        mPlayPauseProgressButtonBig.setEnabled(false);
        mPlayPauseProgressButtonBig.setOnDraggingListener(this);
        mPlayPauseProgressButtonSmall.setPlayerAdapter(mPlayerAdapter);
        mPlayPauseProgressButtonSmall.getPlayPauseButton().setFragment(this);
        mPlayPauseProgressButtonSmall.setDragEnabled(false);
        mPlayPauseProgressButtonSmall.setEnabled(false);
    }

    @Override
    public void onStartDragging() {
        mUserIsDragging = true;
    }

    @Override
    public void onDragging(int progress) {
        updateTimes(progress);
    }

    @Override
    public void onStopDragging() {
        mUserIsDragging = false;
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
                    BitmapPaletteUtil colors = new BitmapPaletteUtil(bitmap);
                    mVisualizerColor = colors.getContrastingColor();
                    mNotificationUtil.sendNotification(song.getTitle(), song.getArtist(), bitmap, true);
                    colors = null;
                } catch (ExecutionException | InterruptedException e) {
                    mVisualizerColor = mDefaultVisualizerColor;
                }
                mVisualizerView.setColor(mVisualizerColor);
                futureTarget.cancel(false);
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
                int resId = mUserIsDragging
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
                mSongPlayTime.setText(playTime);
                mSongTimeRemaining.setText(timeRemaining);
            }
        });
    }

    private void updateVisualizer() {
        boolean show = Config.getShowVisualizer(getActivity());
        if (mShowVisualizer != show) {
            mShowVisualizer = show;
            if (mShowVisualizer && mBottomSheetExpanded && mPlayerAdapter.isPlaying()) {
                mVisualizerView.setPlaying(true);
            } else {
                mVisualizerView.setPlaying(false);
            }
        }
    }

    @Override
    public void onSongClicked(Song song, int position) {
        boolean playNewSong = false;
        if (mPlayerAdapter.isPlaying()) {
            pause();
            playNewSong = true;
        }
        mRoot.findViewById(R.id.bottom_sheet_drag_handle).setAlpha(1);
        applyMediaMetadata(song);
        mPlayPauseProgressButtonBig.setEnabled(true);
        mPlayPauseProgressButtonSmall.setEnabled(true);
        updateTimes(0);
        mBottomSheetBehavior.setLocked(false);
        mPlayerAdapter.setDataSource(song.getData());
        mCurrentSongInfo.setSong(song);
        mCurrentSongInfo.setPosition(0);
        mCurrentSongInfo.setIsMediaPlayerReleased(false);
        if (playNewSong) {
            play();
        }
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

    public void play() {
        mPlayerAdapter.play();
        mPlayPauseProgressButtonBig.resume();
        mPlayPauseProgressButtonSmall.resume();
        if (mShowVisualizer && mBottomSheetExpanded) {
            mVisualizerView.setPlaying(true);
        }
        mNotificationUtil.sendNotification(null, null, null, false);
        mCurrentSongInfo.setIsPlaying(true);
    }

    public void pause() {
        mPlayerAdapter.pause();
        mPlayPauseProgressButtonBig.pause();
        mPlayPauseProgressButtonSmall.pause();
        mVisualizerView.setPlaying(false);
        mNotificationUtil.sendNotification(null, null, null, true);
        mCurrentSongInfo.setIsPlaying(false);
    }

    private void log(String message) {
        if (DEBUG) {
            Log.d(TAG, message);
        }
    }

    private int getColorFromThemeAttribute(int resId) {
        TypedValue tv = new TypedValue();
        int color = 0;
        getActivity().getTheme().resolveAttribute(resId, tv, true);
        if (tv.type >= TypedValue.TYPE_FIRST_COLOR_INT && tv.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            color = tv.data;
        } else {
            color = getActivity().getColor(tv.resourceId);
        }
        return color;
    }

    public class Callback extends BottomSheetCallback {

        @Override
        public void onStateChanged(View bottomSheet, int newState) {
            switch (newState) {
                case BottomSheetBehavior.STATE_HIDDEN:
                    break;
                case BottomSheetBehavior.STATE_EXPANDED:
                    mBottomSheetExpanded = true;
                    break;
                case BottomSheetBehavior.STATE_COLLAPSED:
                    mBottomSheetExpanded = false;
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
                if (slideOffset > 0) {
                    mBottomSheet.setClickable(true);
                    int bgColor = getColorFromThemeAttribute(android.R.attr.colorBackground);
                    mBottomSheet.setBackgroundColor(bgColor);
                    mRoot.findViewById(R.id.album_art_big_frame).setVisibility(View.VISIBLE);
                    if (mPlayPauseProgressButtonSmall.isEnabled()) {
                        mPlayPauseProgressButtonSmall.setEnabled(false);
                    }
                    if (!mPlayPauseProgressButtonBig.isEnabled()) {
                        mPlayPauseProgressButtonBig.setEnabled(true);
                    }
                    updateBottomSheetSongInfoLayout(false);
                } else {
                    mBottomSheet.setClickable(false);
                    mBottomSheet.setBackground(null);
                    mRoot.findViewById(R.id.album_art_big_frame).setVisibility(View.GONE);
                    if (mPlayPauseProgressButtonSmall.isEnabled()) {
                        mPlayPauseProgressButtonSmall.setEnabled(true);
                    }
                    if (mPlayPauseProgressButtonBig.isEnabled()) {
                        mPlayPauseProgressButtonBig.setEnabled(false);
                    }
                    updateBottomSheetSongInfoLayout(true);
                }
                if (slideOffset == 1) {
                    if (mShowVisualizer && mPlayerAdapter.isPlaying()) {
                        mVisualizerView.setPlaying(true);
                    }
                } else {
                    mVisualizerView.setPlaying(false);
                }
            }
        }
    }

    private void updateBottomSheetSongInfoLayout(boolean collapsed) {
        View v = mRoot.findViewById(R.id.bottom_sheet_song_info_layout);
        if (v == null) {
            return;
        }

        int paddingStart = getActivity().getResources().getDimensionPixelOffset(collapsed
            ? R.dimen.bottom_sheet_collapsed_song_info_padding_start
            : R.dimen.bottom_sheet_expanded_song_info_padding_start);
        int paddingEnd = getActivity().getResources().getDimensionPixelOffset(collapsed
            ? R.dimen.bottom_sheet_collapsed_song_info_padding_end
            : R.dimen.bottom_sheet_expanded_song_info_padding_end);
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) v.getLayoutParams();
        params.gravity = collapsed ? Gravity.START : Gravity.CENTER_HORIZONTAL;
        if (mBottomSheet.getTag().equals("landscape")) {
            int marginTop = collapsed ? 0 : getActivity().getResources().getDimensionPixelOffset(
                    R.dimen.bottom_sheet_expanded_song_info_margin_top);
            params.topMargin = marginTop;
        }

        v.setPaddingRelative(paddingStart, v.getPaddingTop(),
                paddingEnd, v.getPaddingBottom());
        v.setLayoutParams(params);
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
            mPlayPauseProgressButtonBig.updateState();
            mPlayPauseProgressButtonSmall.updateState();
            mDuration = duration;
            updateTimes(0);
            log(String.format("setPlaybackDuration: setMax(%d)", duration));
        }

        @Override
        public void onPositionChanged(int position) {
            log(String.format("setPlaybackPosition: setProgress(%d)", position));
            if (!mUserIsDragging) {
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
            mPlayerAdapter.pause();
            updateTimes(0);
            mPlayerAdapter.seekTo(0);
            mPlayPauseProgressButtonBig.onPlaybackCompleted();
            mPlayPauseProgressButtonSmall.onPlaybackCompleted();
            mVisualizerView.setPlaying(false);
            mNotificationUtil.sendNotification(null, null, null, true);
            mCurrentSongInfo.setPosition(0);
            mCurrentSongInfo.setIsPlaying(false);
        }

        @Override
        public void onLogUpdated(String message) {
            log(message);
        }
    }
}
