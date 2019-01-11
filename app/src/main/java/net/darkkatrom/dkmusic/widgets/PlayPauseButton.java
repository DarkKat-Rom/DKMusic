/*
 * Copyright (C) 2012 Andrew Neal
 * Copyright (C) 2014 The CyanogenMod Project
 * Copyright (C) 2018 DarkKat
 * Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package net.darkkatrom.dkmusic.widgets;

import android.animation.Animator;
import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;

import net.darkkatrom.dkmusic.R;
import net.darkkatrom.dkmusic.adapters.PlayerAdapter;
import net.darkkatrom.dkmusic.fragments.SongListFragment;

/**
 * A custom {@link ImageButton} that represents the "play and pause" button.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class PlayPauseButton extends ImageButton implements OnClickListener {

    private PlayerAdapter mPlayerAdapter;
    private SongListFragment mFragment;
    /**
     * @param context The {@link Context} to use
     * @param attrs The attributes of the XML tag that is inflating the view.
     */
    @SuppressWarnings("deprecation")
    public PlayPauseButton(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        setBackgroundResource(R.drawable.selectable_background);
        // Control playback (play/pause)
        setOnClickListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onClick(final View v) {
        if (mPlayerAdapter != null) {
            if (mPlayerAdapter.isPlaying()) {
                mFragment.pause();
            } else {
                mFragment.play();
            }
        }

        int centerX = (v.getLeft() + v.getRight())  / 2;
        int centerY = (v.getTop()  + v.getBottom()) / 2;
        int startRadius = 0;
        int endRadius = (int) Math.hypot(v.getWidth(), v.getHeight());

        Animator anim = ViewAnimationUtils.createCircularReveal(
                v, centerX, centerY, startRadius, endRadius);

        anim.setDuration(800);
        anim.start();

        updateState();
    }

    /**
     * Sets the correct drawable for playback.
     */
    public void updateState() {
        if (mPlayerAdapter.isPlaying()) {
            setImageResource(R.drawable.ic_action_pause);
        } else {
            setImageResource(R.drawable.ic_action_play);
        }
    }

    /**
     * Sets the PlayerAdapter.
     */
    void setPlayerAdapter(PlayerAdapter adapter) {
        mPlayerAdapter = adapter;
    }

    /**
     * Sets the SongListFragment, which handles the internal play/pause function.
     */
    public void setFragment(SongListFragment fragment) {
        mFragment = fragment;
    }
}
