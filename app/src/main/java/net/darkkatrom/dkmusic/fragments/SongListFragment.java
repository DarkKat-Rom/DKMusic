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
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.IntentFilter;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.ServiceConnection;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetBehavior.BottomSheetCallback;
import android.support.v4.app.Fragment;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.CardView;
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

import com.bumptech.glide.request.RequestOptions;

import jp.wasabeef.glide.transformations.BlurTransformation;
import jp.wasabeef.glide.transformations.ColorFilterTransformation;
import jp.wasabeef.glide.transformations.CropTransformation;
import jp.wasabeef.glide.transformations.RoundedCornersTransformation;

import net.darkkatrom.dkmusic.MusicPlaybackService;
import net.darkkatrom.dkmusic.MusicPlaybackService.LocalBinder;
import net.darkkatrom.dkmusic.GlideApp;
import net.darkkatrom.dkmusic.R;
import net.darkkatrom.dkmusic.activities.MainActivity;
import net.darkkatrom.dkmusic.activities.SettingsActivity;
import net.darkkatrom.dkmusic.adapters.SongAdapter;
import net.darkkatrom.dkmusic.glideTargets.BitmapHolderTarget;
import net.darkkatrom.dkmusic.glideTargets.DrawableViewBackgroundTarget;
import net.darkkatrom.dkmusic.holders.VisualizerHolder;
import net.darkkatrom.dkmusic.interfaces.BitmapHolder;
import net.darkkatrom.dkmusic.listeners.PlaybackInfoListener;
import net.darkkatrom.dkmusic.models.Song;
import net.darkkatrom.dkmusic.utils.BitmapPaletteUtil;
import net.darkkatrom.dkmusic.utils.ColorHelper;
import net.darkkatrom.dkmusic.utils.Config;
import net.darkkatrom.dkmusic.utils.ThemeUtil;
import net.darkkatrom.dkmusic.widgets.LockableBottomSheetBehavior;
import net.darkkatrom.dkmusic.widgets.PlayPauseProgressButton;
import net.darkkatrom.dkmusic.widgets.VisualizerView;

import java.util.ArrayList;
import java.util.List;

public final class SongListFragment extends Fragment implements BitmapHolder,
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
    private PowerSaveModeReceiver mPowerSaveModeReceiver;
    private Song mCurrentSong;

    private VisualizerHolder mVisualizerHolder;

    private boolean mCanReadExternalStorage = false;

    private boolean mUserIsDragging = false;
    private int mDuration;
    private boolean mSongListShowVisualizer;
    private boolean mBottomSheetShowVisualizer;
    private boolean mBottomSheetExpanded = false;
    private boolean mPowerSaveMode = false;

    private int mCurrentSongPosition = -1;
    private int mLastSongPosition = mCurrentSongPosition;

    private boolean mUpdateBottomSheetToExpand = true;
    private boolean mUseColorsForExpandedState = true;
    private boolean mUpdateToExpand = true;
    private boolean mUpdateToCollapse = false;
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
    private int mOldToolbarTitleTextColor;
    private int mToolbarTitleTextColor;
    private int mOldToolbarSubtitleTextColor;
    private int mToolbarSubtitleTextColor;

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
        mOldToolbarTitleTextColor = mPrimaryTextColorDark;
        mToolbarTitleTextColor = mOldToolbarTitleTextColor;
        mOldToolbarSubtitleTextColor = mPrimaryTextColorDark;
        mToolbarSubtitleTextColor = mOldToolbarSubtitleTextColor;
        mDefaultStatusBarColor =
                ThemeUtil.getColorFromThemeAttribute(getActivity(), R.attr.colorPrimaryDark);
        mDefaultBottomBarBgColor =
                ThemeUtil.getColorFromThemeAttribute(getActivity(), R.attr.colorBackgroundFloating);
        mPlaybackListener = new PlaybackListener();
        mVisualizerHolder = new VisualizerHolder();
        mPowerSaveModeReceiver = new PowerSaveModeReceiver();
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        mCanReadExternalStorage = getActivity().checkSelfPermission(permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
        mDefaultVisualizerColor = getActivity().getColor(R.color.visualizer_fill_color_default);

        mRoot = inflater.inflate(R.layout.fragment_song_list, container, false);
        initializeUI();
        initializePopupWindow();

        final IntentFilter filter = new IntentFilter();
        filter.addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED);
        getActivity().registerReceiver(mPowerSaveModeReceiver, filter);

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
        updateVisualizer();
    }

    @Override
    public void onPause() {
        super.onPause();

        mService.setPlaybackInfoListener(null);
        getActivity().unbindService(mConnection);
        mVisualizerHolder.unlinkVisualizer();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        try {
            getActivity().unregisterReceiver(mPowerSaveModeReceiver);
        } catch (final Throwable e) {
        }
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

        mList.setItemAnimator(null);

        mSongListShowVisualizer = Config.getShowVisualizerInSongList(getActivity());

        mBottomSheetVisualizerView.initialize(getActivity(), mVisualizerHolder);
        mBottomSheetShowVisualizer = Config.getShowVisualizerInPlaybackControl(getActivity());

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

    private void applyMediaMetadata() {
        applyMediaMetadata(false);
    }

    private void applyMediaMetadata(final boolean animate) {
        // First load the original sized album art image,
        // Once it's done or failed, 'setBitmap' will be called
        GlideApp.with(SongListFragment.this)
            .asBitmap()
            .load(mCurrentSong.getAlbumArtUri())
            .into(new BitmapHolderTarget(this, animate));
    }

    @Override
    public void setBitmap(Bitmap bitmap, boolean animate) {
        mService.setAlbumArt(bitmap);
        boolean useContrastingColor = false;
        int contrastingColor = 0;
        if (bitmap != null) {
            BitmapPaletteUtil colors = new BitmapPaletteUtil(bitmap);
            contrastingColor = colors.getContrastingColor();
            useContrastingColor = true;
            colors = null;
        }
        mVisualizerColor = useContrastingColor ? contrastingColor : mDefaultVisualizerColor;
        mBottomBarBgColor = useContrastingColor ? contrastingColor : mDefaultBottomBarBgColor;
        mNewToolbarColor = mVisualizerColor;
        mStatusBarColor = ThemeUtil.getStatusBarBackgroundColor(mNewToolbarColor);
        updateBottomBar(animate);
        updateBottomSheet(bitmap);
    }

    private void updateBottomBar(boolean animate) {
        if (!mBottomBar.isAttachedToWindow()) {
            return;
        }
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
                    setBottomBarContent();
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
            setBottomBarContent();
            updateBottomBarColors();
        }
    }

    private void setBottomBarContent() {
        applyBitmap(mBottomBarAlbumArt);
        mBottomBarSongTitle.setText(mCurrentSong.getTitle());
        mBottomBarSongArtist.setText(mCurrentSong.getArtist());
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

    private void updateBottomSheet(Bitmap bitmap) {
        if (mBottomSheetSongTitle != null) {
            mBottomSheetSongTitle.setText(mCurrentSong.getTitle());
        }
        if (mBottomSheetSongArtist != null) {
            mBottomSheetSongArtist.setText(mCurrentSong.getArtist());
        }
        if (bitmap != null) {
            if (mBottomSheetAlbumArt.getWidth() > bitmap.getWidth()) {
                mBottomSheetAlbumArt.setScaleType(ImageView.ScaleType.CENTER_CROP);
            } else {
                mBottomSheetAlbumArt.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            }
            applyBitmap(mBottomSheetAlbumArt);
        } else {
            mBottomSheetAlbumArt.setScaleType(ImageView.ScaleType.CENTER_CROP);
            mBottomSheetAlbumArt.setImageResource(R.drawable.default_artwork);
        }
        mBottomSheetVisualizerView.setColor(mVisualizerColor);

        final View cardLayout = mRoot.findViewById(R.id.bottom_sheet_music_control_card_layout);
        final int cornerRadius =
                (int) ((CardView) mRoot.findViewById(R.id.bottom_sheet_music_control_card)).getRadius();

        GlideApp.with(SongListFragment.this)
            .load(mCurrentSong.getAlbumArtUri())
            .placeholder(R.drawable.default_artwork)
            .apply(new RequestOptions().transforms(
                new BlurTransformation(25, 5),
                new CropTransformation(cardLayout.getWidth(), cardLayout.getHeight(),
                        CropTransformation.CropType.CENTER),
                new RoundedCornersTransformation(cornerRadius, 0,
                        RoundedCornersTransformation.CornerType.ALL),
                new ColorFilterTransformation(ThemeUtil.getCardBgFilterColor(getActivity()))))
            .into(new DrawableViewBackgroundTarget(cardLayout));
    }

    private void applyBitmap(ImageView iv) {
        GlideApp.with(SongListFragment.this)
            .asBitmap()
            .load(mCurrentSong.getAlbumArtUri())
            .placeholder(R.drawable.default_artwork)
            .into(iv);
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
        mToolbarTitleTextColor = lightToolbar ? mPrimaryTextColorLight : mPrimaryTextColorDark;
        mToolbarSubtitleTextColor = lightToolbar ? mSecondaryTextColorLight : mSecondaryTextColorDark;

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

            ObjectAnimator toolbarTitleTextAnimator = ObjectAnimator.ofInt(toolbar,
                    "titleTextColor", mOldToolbarTitleTextColor, mToolbarTitleTextColor);
            toolbarTitleTextAnimator.setEvaluator(new ArgbEvaluator());
            toolbarTitleTextAnimator.setDuration(300);
            toolbarTitleTextAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(final Animator animator) {
                    mOldToolbarTitleTextColor = mToolbarTitleTextColor;
                    animator.addListener(null);
                    mPopup.setContentView(getThemedPopupContent());
                }
            });
            toolbarTitleTextAnimator.addUpdateListener(new ObjectAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    int color = (int) animation.getAnimatedValue();
                    mMenuItemMore.setIconTintList(ColorStateList.valueOf(color));
                }
            });
            ObjectAnimator toolbarSubtitleTextAnimator = ObjectAnimator.ofInt(toolbar,
                    "subtitleTextColor", mOldToolbarSubtitleTextColor, mToolbarSubtitleTextColor);
            toolbarSubtitleTextAnimator.setEvaluator(new ArgbEvaluator());
            toolbarSubtitleTextAnimator.setDuration(300);
            toolbarSubtitleTextAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(final Animator animator) {
                    mOldToolbarSubtitleTextColor = mToolbarSubtitleTextColor;
                    animator.addListener(null);
                }
            });
            animatorlist.add(toolbarTitleTextAnimator);
            animatorlist.add(toolbarSubtitleTextAnimator);
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
        PowerManager pm = (PowerManager) getActivity().getSystemService(Context.POWER_SERVICE);
        boolean forceItemChange = pm.isPowerSaveMode() != mPowerSaveMode;
        mPowerSaveMode = pm.isPowerSaveMode();
        if (mPowerSaveMode) {
            mVisualizerHolder.setPowerSaveMode(true);
            mBottomSheetVisualizerView.setPowerSaveMode(true);
        } else {
            mVisualizerHolder.setPowerSaveMode(false);
            mBottomSheetVisualizerView.setPowerSaveMode(false);
        }
        if (mList.getAdapter() != null) {
            ((SongAdapter) mList.getAdapter()).setPowerSaveMode(mPowerSaveMode);
        }

        boolean showVisualizerInSongList = Config.getShowVisualizerInSongList(getActivity());
        boolean showInPlaybackControl = Config.getShowVisualizerInPlaybackControl(getActivity());

        if (mSongListShowVisualizer != showVisualizerInSongList || forceItemChange) {
            mSongListShowVisualizer = showVisualizerInSongList;
            if (mSongListShowVisualizer && mService.isPlaying()) {
                mSongs.get(mCurrentSongPosition).setShowVisualizerInList(true);
                mList.getAdapter().notifyItemChanged(mCurrentSongPosition);
            } else {
                mSongs.get(mCurrentSongPosition).setShowVisualizerInList(false);
                mList.getAdapter().notifyItemChanged(mCurrentSongPosition);
            }
        }
        if (mBottomSheetShowVisualizer != showInPlaybackControl) {
            mBottomSheetShowVisualizer = showInPlaybackControl;
            if (mBottomSheetShowVisualizer && mBottomSheetExpanded && mService.isPlaying()) {
                mBottomSheetVisualizerView.setPlaying(true);
            } else {
                mBottomSheetVisualizerView.setPlaying(false);
            }
        }
    }

    @Override
    public void onSongClicked(Song song, int position) {
        mCurrentSong = song;
        mCurrentSongPosition = position;
        boolean playNewSong = false;
        if (mService.isPlaying()) {
            pause();
            playNewSong = true;
        }
        mService.setSong(mCurrentSong);
        applyMediaMetadata(true);
        if (playNewSong) {
            play();
        }
    }

    public void play() {
        mService.play();
        if (mSongListShowVisualizer && !mBottomSheetExpanded) {
            if (mCurrentSongPosition != -1) {
                mSongs.get(mCurrentSongPosition).setShowVisualizerInList(true);
                mList.getAdapter().notifyItemChanged(mCurrentSongPosition);
                if (mCurrentSongPosition != mLastSongPosition) {
                    mSongs.get(mLastSongPosition).setShowVisualizerInList(false);
                    mList.getAdapter().notifyItemChanged(mLastSongPosition);
                }

            }
        }
        if (mLastSongPosition != mCurrentSongPosition) {
            mLastSongPosition = mCurrentSongPosition;
        }
    }

    public void setVisualizerInListPlayingIfNeeded() {
        if (mService == null || mList.getAdapter() == null || mCurrentSongPosition == -1
            || !mSongListShowVisualizer) {
            return;
        }
        if (mService.isPlaying()) {
            boolean isPlaying = mSongs.get(mCurrentSongPosition).getShowVisualizerInList();
            if (!isPlaying) {
                mSongs.get(mCurrentSongPosition).setShowVisualizerInList(true);
                mList.getAdapter().notifyItemChanged(mCurrentSongPosition);
            }
        }
    }

    public void pause() {
        if (mLastSongPosition != -1 && mSongListShowVisualizer && !mBottomSheetExpanded) {
            if (mLastSongPosition == mCurrentSongPosition) {
                mSongs.get(mLastSongPosition).setShowVisualizerInList(false);
                mList.getAdapter().notifyItemChanged(mLastSongPosition);
            }
        }
        mService.pause();
    }

    public void setVisualizerInListPausingIfNeeded() {
        if (mService == null || mList.getAdapter() == null || mCurrentSongPosition == -1
            || !mSongListShowVisualizer) {
            return;
        }
        if (mService.isPlaying()) {
            boolean isPlaying = mSongs.get(mCurrentSongPosition).getShowVisualizerInList();
            if (isPlaying) {
                mSongs.get(mCurrentSongPosition).setShowVisualizerInList(false);
                mList.getAdapter().notifyItemChanged(mCurrentSongPosition);
            }
        }
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

            mCurrentSong = mService.getSong();
            if (mCurrentSong != null) {
                applyMediaMetadata();
                mCurrentSongPosition = mCurrentSong.getPositionInList();
                mLastSongPosition = mCurrentSongPosition;
                if (mCurrentSongPosition != -1) {
                    mList.getLayoutManager().scrollToPosition(mCurrentSongPosition);
                    setVisualizerInListPlayingIfNeeded();
                }
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
                    if (mUpdateBottomSheetToExpand) {
                        mUpdateBottomSheetToExpand = false;
                        mBottomSheet.setClickable(true);
                        mBottomSheet.setBackgroundColor(ThemeUtil.getColorFromThemeAttribute(
                                getActivity(), android.R.attr.colorBackground));
                        mRoot.findViewById(R.id.bottom_sheet_drag_handle_frame).setVisibility(View.VISIBLE);
                        if (mBottomSheetSongTitle != null) {
                            mBottomSheetSongTitle.setVisibility(View.VISIBLE);
                            mRoot.findViewById(R.id.bottom_sheet_music_control_card)
                                    .setVisibility(View.VISIBLE);
                        } else {
                            MainActivity activity = (MainActivity) getActivity();
                            Toolbar toolbar = activity.getToolbar();
                            toolbar.setTitle(mService.getSong().getTitle());
                            toolbar.setSubtitle(mService.getSong().getArtist());
                        }
                        if (mBottomSheetSongArtist != null) {
                            mBottomSheetSongArtist.setVisibility(View.VISIBLE);
                        }
                        mRoot.findViewById(R.id.bottom_sheet_album_art_frame).setVisibility(View.VISIBLE);
                        if (mBottomSheetShowVisualizer && mService.isPlaying()) {
                            mBottomSheetVisualizerView.setPlaying(true);
                        }
                    }
                    if (mBottomBarPlayPauseButton.isEnabled()) {
                        mBottomBarPlayPauseButton.setEnabled(false);
                    }
                    if (!mBottomSheetPlayPauseButton.isEnabled()) {
                        mBottomSheetPlayPauseButton.setEnabled(true);
                    }
                } else {
                    mUpdateBottomSheetToExpand = true;
                    mBottomSheet.setClickable(false);
                    mBottomSheet.setBackground(null);
                    mRoot.findViewById(R.id.bottom_sheet_drag_handle_frame).setVisibility(View.INVISIBLE);
                    if (mBottomSheetSongTitle != null) {
                        mBottomSheetSongTitle.setVisibility(View.INVISIBLE);
                        mRoot.findViewById(R.id.bottom_sheet_music_control_card)
                                .setVisibility(View.INVISIBLE);
                    } else {
                        MainActivity activity = (MainActivity) getActivity();
                        Toolbar toolbar = activity.getToolbar();
                        toolbar.setTitle(R.string.app_name);
                        toolbar.setSubtitle(null);
                    }
                    if (mBottomSheetSongArtist != null) {
                        mBottomSheetSongArtist.setVisibility(View.INVISIBLE);
                    }
                    mRoot.findViewById(R.id.bottom_sheet_album_art_frame).setVisibility(View.INVISIBLE);
                    if (!mBottomBarPlayPauseButton.isEnabled()) {
                        mBottomBarPlayPauseButton.setEnabled(true);
                    }
                    if (mBottomSheetPlayPauseButton.isEnabled()) {
                        mBottomSheetPlayPauseButton.setEnabled(false);
                    }
                    mBottomSheetVisualizerView.setPlaying(false);
                }
                if (slideOffset > 0.7 && mUpdateToExpand) {
                    setVisualizerInListPausingIfNeeded();
                    mUpdateToExpand = false;
                    mUpdateToCollapse = true;
                    mUseColorsForExpandedState = true;
                    updateToolbarAndStatusBarColors();
                }
                if (slideOffset < 0.7 && mUpdateToCollapse) {
                    setVisualizerInListPlayingIfNeeded();
                    mUpdateToExpand = true;
                    mUpdateToCollapse = false;
                    mUseColorsForExpandedState = false;
                    updateToolbarAndStatusBarColors();
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
                    songs.add(new Song(currentCount - 1, data, title, artist, album, albumId));
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
            SongAdapter adapter = new SongAdapter(context, songs, mVisualizerHolder);
            adapter.setOnSongClickedListener(listener);
            text.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
            rv.setVisibility(View.VISIBLE);
            rv.setAdapter(adapter);
            setVisualizerInListPlayingIfNeeded();
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
                    if (mBottomSheetShowVisualizer && mBottomSheetExpanded) {
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

    public class PowerSaveModeReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();
            if (action.equals(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)) {
                updateVisualizer();
            }
        }
    }
}
