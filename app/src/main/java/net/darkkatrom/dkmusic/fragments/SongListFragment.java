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
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.ServiceConnection;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetBehavior.BottomSheetCallback;
import android.support.v4.app.Fragment;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.animation.Animation;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.request.FutureTarget;

import net.darkkatrom.dkmusic.MusicPlaybackService;
import net.darkkatrom.dkmusic.MusicPlaybackService.LocalBinder;
import net.darkkatrom.dkmusic.R;
import net.darkkatrom.dkmusic.GlideApp;
import net.darkkatrom.dkmusic.activities.MainActivity;
import net.darkkatrom.dkmusic.activities.SettingsActivity;
import net.darkkatrom.dkmusic.adapters.SongAdapter;
import net.darkkatrom.dkmusic.holders.SongInfoHolder;
import net.darkkatrom.dkmusic.listeners.PlaybackInfoListener;
import net.darkkatrom.dkmusic.models.Song;
import net.darkkatrom.dkmusic.utils.BitmapPaletteUtil;
import net.darkkatrom.dkmusic.utils.ColorHelper;
import net.darkkatrom.dkmusic.utils.Config;
import net.darkkatrom.dkmusic.utils.ThemeUtil;
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
    private View mBottomBar;
    private ImageView mBottomBarDragHandle;
    private View mBottomBarContent;
    private ImageView mBottomBarAlbumArt;
    private TextView mBottomBarSongTitle;
    private TextView mBottomBarSongArtist;
    private PlayPauseProgressButton mBottomBarPlayPauseButton;
    private PlayPauseProgressButton mBottomSheetPlayPauseButton;
    private ViewGroup mBottomSheet;
    private TextView mBottomSheetSongTitle;
    private TextView mBottomSheetSongArtist;
    private ImageView mBottomSheetAlbumArt;
    private VisualizerView mBottomSheetVisualizerView;
    private TextView mBottomSheetSongPlayTime;
    private TextView mBottomSheetSongTimeRemaining;

    private MenuItem mMenuItemMore;
    private PopupWindow mPopup;
    private LockableBottomSheetBehavior mBottomSheetBehavior;
    private List<Song> mSongs;
    private MusicPlaybackService mService;
    private PlaybackListener mPlaybackListener;

    private boolean mCanReadExternalStorage = false;

    private boolean mUserIsDragging = false;
    private int mDuration;
    private boolean mShowVisualizer;
    private boolean mBottomSheetExpanded = false;

    private boolean mUseColorsForExpandedState = true;
    private boolean mUpdateColorsToExpand = true;
    private boolean mUpdateColorsToCollapse = false;
    private boolean mLightToolbar = false;
    private boolean mLightStatusBar = false;

    private int mDefaultVisualizerColor = 0;
    private int mVisualizerColor = 0;
    private int mDefaultToolbarColor;
    private int mOldToolbarColor;
    private int mNewToolbarColor;
    private int mPrimaryTextColorLight;
    private int mSecondaryTextColorLight;
    private int mPrimaryTextColorDark;
    private int mSecondaryTextColorDark;
    private int mOldToolbarTextColor;
    private int mToolbarTextColor;
    private int mDefaultStatusBarColor;
    private int mStatusBarColor;
    private int mDefaultBottomBarBgColor;
    private int mBottomBarBgColor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDefaultToolbarColor =
                ThemeUtil.getColorFromThemeAttribute(getActivity(), R.attr.colorPrimary);
        mOldToolbarColor = mDefaultToolbarColor;
        mPrimaryTextColorLight = getActivity().getColor(R.color.primary_text_default_material_light);
        mSecondaryTextColorLight = getActivity().getColor(R.color.secondary_text_default_material_light);
        mPrimaryTextColorDark = getActivity().getColor(R.color.primary_text_default_material_dark);
        mSecondaryTextColorDark = getActivity().getColor(R.color.secondary_text_default_material_dark);
        mOldToolbarTextColor = mPrimaryTextColorDark;
        mToolbarTextColor = mOldToolbarTextColor;
        mDefaultStatusBarColor =
                ThemeUtil.getColorFromThemeAttribute(getActivity(), R.attr.colorPrimaryDark);
        mDefaultBottomBarBgColor =
                ThemeUtil.getColorFromThemeAttribute(getActivity(), R.attr.colorBackgroundFloating);

        mPlaybackListener = new PlaybackListener();

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
        initializePopupWindow();

        return mRoot;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mCanReadExternalStorage
                != (getActivity().checkSelfPermission(permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED)) {
            checkPermission();
        }

        Intent musicIntent = new Intent(getActivity(), MusicPlaybackService.class);
        getActivity().startService(musicIntent);
        getActivity().bindService(musicIntent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onPause() {
        super.onPause();

        mService.setPlaybackInfoListener(null);
        getActivity().unbindService(mConnection);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_main, menu);

        mMenuItemMore = menu.findItem(R.id.item_more);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.item_more) {
            mPopup.showAtLocation(mRoot, (Gravity.TOP | Gravity.END), 0 , 0);
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
        mBottomBar = mRoot.findViewById(R.id.bottom_bar);
        mBottomBarDragHandle = (ImageView) mRoot.findViewById(R.id.bottom_bar_drag_handle);
        mBottomBarContent = mRoot.findViewById(R.id.bottom_bar_content);
        mBottomBarAlbumArt = (ImageView) mRoot.findViewById(R.id.bottom_bar_album_art);
        mBottomBarSongTitle = (TextView) mRoot.findViewById(R.id.bottom_bar_song_title);
        mBottomBarSongArtist = (TextView) mRoot.findViewById(R.id.bottom_bar_artist_title);
        mBottomBarPlayPauseButton = (PlayPauseProgressButton) mRoot.findViewById(R.id.bottom_bar_button_play_pause);
        mBottomSheetPlayPauseButton = (PlayPauseProgressButton) mRoot.findViewById(R.id.bottom_sheet_button_play_pause);
        mBottomSheet = (ViewGroup) mRoot.findViewById(R.id.bottom_sheet);
        mBottomSheetSongTitle = (TextView) mRoot.findViewById(R.id.bottom_sheet_song_title);
        mBottomSheetSongArtist = (TextView) mRoot.findViewById(R.id.bottom_sheet_artist_title);
        mBottomSheetAlbumArt = (ImageView) mRoot.findViewById(R.id.bottom_sheet_album_art);
        mBottomSheetVisualizerView = (VisualizerView) mRoot.findViewById(R.id.bottom_sheet_visualizer_view);
        mBottomSheetSongPlayTime = (TextView) mRoot.findViewById(R.id.bottom_sheet_song_play_time);
        mBottomSheetSongTimeRemaining = (TextView) mRoot.findViewById(R.id.bottom_sheet_song_time_remaining);
        mBottomSheetBehavior = (LockableBottomSheetBehavior) LockableBottomSheetBehavior.from(mBottomSheet);

        checkPermission();

        mBottomSheetVisualizerView.initialize(getActivity());
        mShowVisualizer = Config.getShowVisualizer(getActivity());

        mBottomSheetBehavior.setBottomSheetCallback(new Callback());
        mBottomSheetBehavior.setLocked(true);
    }

    private void initializePopupWindow() {
        mPopup = new PopupWindow(getThemedPopupContent(), ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        mPopup.setBackgroundDrawable(new ColorDrawable(0));
        mPopup.setOutsideTouchable(true);
        Transition popupEnter = TransitionInflater.from(getActivity()).
                inflateTransition(R.transition.popup_window_enter);
        Transition popupExit = TransitionInflater.from(getActivity()).
                inflateTransition(R.transition.popup_window_exit);
        float popupElevation = getActivity().getResources().getDimension(R.dimen.popup_window_elevation);
        mPopup.setEnterTransition(popupEnter);
        mPopup.setExitTransition(popupExit);
        mPopup.setElevation(popupElevation);
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

    private View getThemedPopupContent() {
        int themeResId = 0;

        if (mLightToolbar) {
            themeResId = R.style.AppThemeLight;
        } else {
            if (Config.getTheme(getActivity()) == Config.THEME_MATERIAL_DARKKAT) {
                themeResId = R.style.AppThemeDarkKat;
            } else {
                themeResId = R.style.AppThemeDark;
            }
        }     

        ContextThemeWrapper themedContext = new ContextThemeWrapper(getActivity(), themeResId);
        View content = LayoutInflater.from(themedContext).inflate(R.layout.popup_content, null);
        content.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), SettingsActivity.class);
                startActivity(intent);
                mPopup.dismiss();
            }
        });

        return content;
    }
         
    private int getLoadingTextColor(boolean error) {
        int resId = error
                ? R.attr.colorError : android.R.attr.textColorPrimary;
        return ThemeUtil.getColorFromThemeAttribute(getActivity(), resId);
    }

    private void initializePlayPauseProgressButtons() {
        mBottomBarPlayPauseButton.setMusicPlaybackService(mService);
        mBottomBarPlayPauseButton.getPlayPauseButton().setFragment(this);
        mBottomBarPlayPauseButton.setDragEnabled(false);
        mBottomBarPlayPauseButton.setEnabled(false);
        mBottomSheetPlayPauseButton.setMusicPlaybackService(mService);
        mBottomSheetPlayPauseButton.getPlayPauseButton().setFragment(this);
        mBottomSheetPlayPauseButton.setEnabled(false);
        mBottomSheetPlayPauseButton.setOnDraggingListener(this);
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

    private void applyMediaMetadata(Song song) {
        applyMediaMetadata(song, false);
    }

    private void applyMediaMetadata(final Song song, final boolean animate) {
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
                    mBottomBarBgColor = mVisualizerColor;
                    mService.setAlbumArt(bitmap);
                    colors = null;
                } catch (ExecutionException | InterruptedException e) {
                    mVisualizerColor = mDefaultVisualizerColor;
                    mBottomBarBgColor = mDefaultBottomBarBgColor;
                }
                mNewToolbarColor = mVisualizerColor;
                mStatusBarColor = ThemeUtil.getStatusBarBackgroundColor(mNewToolbarColor);
                mBottomSheetVisualizerView.setColor(mVisualizerColor);
                updateContent(song, animate);
                futureTarget.cancel(false);
            }
        });
    }

    private void updateContent(final Song song, boolean animate) {
        if (animate) {
            final int centerX = mBottomBar.getWidth();
            final int centerY = mBottomBar.getHeight();
            final int startRadius = 0;
            final int endRadius =
                    (int) Math.hypot(mBottomBar.getWidth(), mBottomBar.getHeight());

            Animator animHide = ViewAnimationUtils.createCircularReveal(
                    mBottomBar, centerX, centerY, endRadius, startRadius);
            final Animator animShow = ViewAnimationUtils.createCircularReveal(
                    mBottomBar, centerX, centerY, startRadius, endRadius);

            animHide.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    mBottomBar.setVisibility(View.INVISIBLE);
                    updateBottomBar(song);
                    updateBottomBarColors();
                    mBottomBar.setVisibility(View.VISIBLE);
                    animShow.start();
                }
            });

            animHide.setDuration(195);
            animHide.setInterpolator(new ValueAnimator().getInterpolator());
            animShow.setDuration(225);
            animShow.setInterpolator(new ValueAnimator().getInterpolator());
            animHide.start();
        } else {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateBottomBar(song);
                    updateBottomBarColors();
                }
            });
        }
        updateBottomSheet(song);
    }

    private void updateBottomBar(Song song) {
        applyBitmap(song, mBottomBarAlbumArt);
        mBottomBarSongTitle.setText(song.getTitle());
        mBottomBarSongArtist.setText(song.getArtist());
    }

    private void updateBottomBarColors() {
        int draghandleBgColor = ThemeUtil.getDragHandleBgColor(mBottomBarBgColor);
        boolean lightDragHandleColor = !ColorHelper.isColorDark(draghandleBgColor);
        boolean lightTextColor = !ColorHelper.isColorDark(mBottomBarBgColor);
        int dragHandleColor =  lightDragHandleColor ? mSecondaryTextColorLight : mSecondaryTextColorDark;
        int primaryTextColor = lightTextColor ? mPrimaryTextColorLight : mPrimaryTextColorDark;
        int secondaryTextColor = lightTextColor ? mSecondaryTextColorLight : mSecondaryTextColorDark;

        mBottomBar.setBackgroundColor(mBottomBarBgColor);
        mBottomBarDragHandle.setImageTintList(ColorStateList.valueOf(dragHandleColor));
        mBottomBarSongTitle.setTextColor(primaryTextColor);
        mBottomBarSongArtist.setTextColor(secondaryTextColor);
        mBottomBarPlayPauseButton.setColor(lightTextColor);
    }

    private void updateBottomSheet(final Song song) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mBottomSheetSongTitle.setText(song.getTitle());
                mBottomSheetSongArtist.setText(song.getArtist());
                applyBitmap(song, mBottomSheetAlbumArt);
            }
        });
    }

    private void applyBitmap(final Song song, ImageView iv) {
        GlideApp.with(SongListFragment.this)
            .asBitmap()
            .load(song.getAlbumArtUri())
            .placeholder(R.drawable.default_artwork)
            .fitCenter()
            .into(iv);
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

    private void updateToolbarAndStatusBarColors() {
        MainActivity activity = (MainActivity) getActivity();
        final Window window = activity.getWindow();
        final Toolbar toolbar = activity.getToolbar();
        AnimatorSet animators = new AnimatorSet();
        animators.setDuration(300);
        ArrayList<Animator> animatorlist = new ArrayList<Animator>();

        final int toolbarColor = mUseColorsForExpandedState
                ? mNewToolbarColor : mDefaultToolbarColor;
        int statusBarColor = mUseColorsForExpandedState
                ? mStatusBarColor
                : mDefaultStatusBarColor;
        boolean lightToolbar = !ColorHelper.isColorDark(toolbarColor);
        boolean lightStatusBar = !ColorHelper.isColorDark(statusBarColor);
        mToolbarTextColor = lightToolbar ? mPrimaryTextColorLight : mPrimaryTextColorDark;

        ObjectAnimator toolbarBgAnimator = ObjectAnimator.ofInt(toolbar,
                "backgroundColor", mOldToolbarColor, toolbarColor);
        toolbarBgAnimator.setEvaluator(new ArgbEvaluator());
        toolbarBgAnimator.setDuration(300);

        toolbarBgAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(final Animator animator) {
                mOldToolbarColor = toolbarColor;
                animator.addListener(null);
            }
        });
        animatorlist.add(toolbarBgAnimator);

        if (mLightToolbar != lightToolbar) {
            mLightToolbar = lightToolbar;

            ObjectAnimator toolbarTextAnimator = ObjectAnimator.ofInt(toolbar,
                    "titleTextColor", mOldToolbarTextColor, mToolbarTextColor);
            toolbarTextAnimator.setEvaluator(new ArgbEvaluator());
            toolbarTextAnimator.setDuration(300);
            toolbarTextAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(final Animator animator) {
                    mOldToolbarTextColor = mToolbarTextColor;
                    animator.addListener(null);
                    mPopup.setContentView(getThemedPopupContent());
                }
            });
            toolbarTextAnimator.addUpdateListener(new ObjectAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    int color = (int) animation.getAnimatedValue();
                    mMenuItemMore.setIconTintList(ColorStateList.valueOf(color));
                }
            });
            animatorlist.add(toolbarTextAnimator);
        }


        if (window.getStatusBarColor() != statusBarColor) {
            ObjectAnimator statusBarAnimator = ObjectAnimator.ofInt(window,
                    "statusBarColor", window.getStatusBarColor(), statusBarColor);
            statusBarAnimator.setEvaluator(new ArgbEvaluator());
            statusBarAnimator.setDuration(300);
            statusBarAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(final Animator animator) {
                    animator.addListener(null);
                }
            });
            animatorlist.add(statusBarAnimator);

            if (mLightStatusBar != lightStatusBar) {
                mLightStatusBar = lightStatusBar;

                int oldFlags = window.getDecorView().getSystemUiVisibility();
                int newFlags = oldFlags;
                if (mLightStatusBar) {
                    newFlags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                } else {
                    newFlags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                }
                final int flags = newFlags;

                toolbar.postOnAnimationDelayed(new Runnable() {
                    @Override
                    public void run() {
                        window.getDecorView().setSystemUiVisibility(flags);
                    }
                }, 150);
            }
        }

        animators.playTogether(animatorlist);
        animators.start();
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

                int resId = mUserIsDragging
                        ? R.attr.colorAccent : android.R.attr.textColorTertiary;
                int textColor = ThemeUtil.getColorFromThemeAttribute(getActivity(), resId);

                mBottomSheetSongPlayTime.setTextColor(textColor);
                mBottomSheetSongTimeRemaining.setTextColor(textColor);
                mBottomSheetSongPlayTime.setText(playTime);
                mBottomSheetSongTimeRemaining.setText(timeRemaining);
            }
        });
    }

    private void updateVisualizer() {
        boolean show = Config.getShowVisualizer(getActivity());
        if (mShowVisualizer != show) {
            mShowVisualizer = show;
            if (mShowVisualizer && mBottomSheetExpanded && mService.isPlaying()) {
                mBottomSheetVisualizerView.setPlaying(true);
            } else {
                mBottomSheetVisualizerView.setPlaying(false);
            }
        }
    }

    @Override
    public void onSongClicked(Song song, int position) {
        boolean playNewSong = false;
        if (mService.isPlaying()) {
            pause();
            playNewSong = true;
        }
        mService.setSong(song);
        applyMediaMetadata(song, true);
        if (playNewSong) {
            play();
        }
    }

    public void play() {
        mService.play();
    }

    public void pause() {
        mService.pause();
    }

    private void log(String message) {
        if (DEBUG) {
            Log.d(TAG, message);
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            LocalBinder binder = (LocalBinder) service;
            mService = binder.getService();
            initializePlayPauseProgressButtons();
            mService.setPlaybackInfoListener(mPlaybackListener);

            Song song = mService.getSong();
            if (song != null) {
                applyMediaMetadata(song);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };

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
                    mBottomSheet.setBackgroundColor(ThemeUtil.getColorFromThemeAttribute(
                            getActivity(), android.R.attr.colorBackground));
                    mRoot.findViewById(R.id.bottom_sheet_drag_handle_frame).setVisibility(View.VISIBLE);
                    mBottomSheetSongTitle.setVisibility(View.VISIBLE);
                    mBottomSheetSongArtist.setVisibility(View.VISIBLE);
                    mRoot.findViewById(R.id.bottom_sheet_album_art_frame).setVisibility(View.VISIBLE);
                    if (mBottomBarPlayPauseButton.isEnabled()) {
                        mBottomBarPlayPauseButton.setEnabled(false);
                    }
                    if (!mBottomSheetPlayPauseButton.isEnabled()) {
                        mBottomSheetPlayPauseButton.setEnabled(true);
                    }
                    updateBottomSheetSongInfoLayout(false);
                } else {
                    mBottomSheet.setClickable(false);
                    mBottomSheet.setBackground(null);
                    mRoot.findViewById(R.id.bottom_sheet_drag_handle_frame).setVisibility(View.INVISIBLE);
                    mBottomSheetSongTitle.setVisibility(View.INVISIBLE);
                    mBottomSheetSongArtist.setVisibility(View.INVISIBLE);
                    mRoot.findViewById(R.id.bottom_sheet_album_art_frame).setVisibility(View.INVISIBLE);
                    if (!mBottomBarPlayPauseButton.isEnabled()) {
                        mBottomBarPlayPauseButton.setEnabled(true);
                    }
                    if (mBottomSheetPlayPauseButton.isEnabled()) {
                        mBottomSheetPlayPauseButton.setEnabled(false);
                    }
                    updateBottomSheetSongInfoLayout(true);
                }
                if (slideOffset > 0.7 && mUpdateColorsToExpand) {
                    mUpdateColorsToExpand = false;
                    mUpdateColorsToCollapse = true;
                    mUseColorsForExpandedState = true;
                    updateToolbarAndStatusBarColors();
                }
                if (slideOffset < 0.7 && mUpdateColorsToCollapse) {
                    mUpdateColorsToExpand = true;
                    mUpdateColorsToCollapse = false;
                    mUseColorsForExpandedState = false;
                    updateToolbarAndStatusBarColors();
                }
                if (slideOffset == 1) {
                    if (mShowVisualizer && mService.isPlaying()) {
                        mBottomSheetVisualizerView.setPlaying(true);
                    }
                } else {
                    mBottomSheetVisualizerView.setPlaying(false);
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
            mBottomSheetPlayPauseButton.updateState();
            mBottomBarPlayPauseButton.updateState();
            mDuration = duration;
            updateTimes(0);
        }

        @Override
        public void onPositionChanged(int position) {
            if (!mUserIsDragging) {
                updateTimes(position);
            }
        }

        @Override
        public void onStateChanged(@State int state) {
            switch (state) {
                case PlaybackInfoListener.State.INVALID:
                    break;
                case PlaybackInfoListener.State.PREPARED:
                    mBottomBarDragHandle.setImageAlpha(255);
                    mBottomSheetPlayPauseButton.setEnabled(true);
                    mBottomBarPlayPauseButton.setEnabled(true);
                    mBottomSheetBehavior.setLocked(false);
                    break;
                case PlaybackInfoListener.State.PLAYING:
                    mBottomSheetPlayPauseButton.resume();
                    mBottomBarPlayPauseButton.resume();
                    if (mShowVisualizer && mBottomSheetExpanded) {
                        mBottomSheetVisualizerView.setPlaying(true);
                    }
                    break;
                case PlaybackInfoListener.State.PAUSED:
                    mBottomSheetPlayPauseButton.pause();
                    mBottomBarPlayPauseButton.pause();
                    mBottomSheetVisualizerView.setPlaying(false);
                    break;
                case PlaybackInfoListener.State.COMPLETED:
                    mService.seekTo(0);
                    mBottomSheetPlayPauseButton.onPlaybackCompleted();
                    mBottomBarPlayPauseButton.onPlaybackCompleted();
                    mService.pause();
                    updateTimes(0);
                    break;
                case PlaybackInfoListener.State.RESET:
                    updateTimes(0);
                    mBottomBarDragHandle.setImageAlpha(77);
                    mBottomSheetPlayPauseButton.setEnabled(false);
                    mBottomBarPlayPauseButton.setEnabled(false);
                    mBottomSheetBehavior.setLocked(true);
                    break;
            }
        }
    }
}
