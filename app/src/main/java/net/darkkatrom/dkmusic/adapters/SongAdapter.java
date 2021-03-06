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

package net.darkkatrom.dkmusic.adapters;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import net.darkkatrom.dkmusic.R;
import net.darkkatrom.dkmusic.GlideApp;
import net.darkkatrom.dkmusic.holders.VisualizerHolder;
import net.darkkatrom.dkmusic.models.Song;
import net.darkkatrom.dkmusic.widgets.VisualizerView;

import java.util.ArrayList;
import java.util.List;

public class SongAdapter extends
        RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    private Context mContext;
    private List<Song> mSongs;
    private VisualizerHolder mVisualizerHolder;
    private boolean mPowerSaveMode = false;

    private OnSongClickedListener mOnSongClickedListener;

    public interface OnSongClickedListener {
        public void onSongClicked(Song song, int position);
    }

    public SongAdapter(Context context, List<Song> songs, VisualizerHolder holder) {
        super();

        mContext = context;
        if (songs != null) {
            mSongs = songs;
        } else {
            mSongs = new ArrayList<Song>();
        }
        mVisualizerHolder = holder;
    }

    @Override
    public SongAdapter.SongViewHolder onCreateViewHolder(ViewGroup parent,
            int viewType) {

        View v = LayoutInflater.from(mContext).inflate(
                R.layout.list_item, parent, false);
        return new SongViewHolder(mContext, v, mVisualizerHolder);
    }

    @Override
    public void onBindViewHolder(SongViewHolder holder, final int position) {
        final Song song = mSongs.get(position);

        holder.mRootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnSongClickedListener != null) {
                    mOnSongClickedListener.onSongClicked(song, position);
                }
            }
        });

        GlideApp.with(mContext)
            .asBitmap()
            .load(song.getAlbumArtUri())
            .placeholder(R.drawable.default_artwork)
            .fitCenter()
            .into(holder.mAlbumArt);
        holder.mTitle.setText(song.getTitle());
        holder.mArtist.setText(song.getArtist());

        updateVisualizerVisibility(song, holder);
    }

    @Override
    public int getItemCount() {
        return mSongs.size();
    }

    public void setOnSongClickedListener(OnSongClickedListener onItemClickedListener) {
        mOnSongClickedListener = onItemClickedListener;
    }

    public void setPowerSaveMode(boolean powerSaveMode) {
        mPowerSaveMode = powerSaveMode;
    }

    private void updateVisualizerVisibility(Song song, final SongViewHolder holder) {
        boolean showVisualizerInList = song.getShowVisualizerInList() && !mPowerSaveMode;
        if (showVisualizerInList != holder.mVisualizer.isPlaying()) {
            final RelativeLayout.LayoutParams params =
                (RelativeLayout.LayoutParams) holder.mTextLayout.getLayoutParams();
            final int ruleToRemove = showVisualizerInList ? RelativeLayout.ALIGN_PARENT_RIGHT : RelativeLayout.LEFT_OF;
            final int ruleToAdd = showVisualizerInList ? RelativeLayout.LEFT_OF : RelativeLayout.ALIGN_PARENT_RIGHT;
            final int subject = showVisualizerInList ? R.id.list_item_visualizer_view : RelativeLayout.TRUE;

            if (showVisualizerInList) {
                params.removeRule(ruleToRemove);
                params.addRule(ruleToAdd, subject);
                holder.mTextLayout.setLayoutParams(params);
                holder.mVisualizer.setPlaying(true);
            } else {
                final ViewPropertyAnimator alphaAnimator = holder.mVisualizer.animate().alpha(0f);
                alphaAnimator.setDuration(300);
                alphaAnimator.setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animator) {
                        holder.mVisualizer.setPlaying(false, false);
                    }

                    @Override
                    public void onAnimationEnd(Animator animator) {
                        alphaAnimator.setListener(null);
                        holder.mVisualizer.unListen();
                        params.removeRule(ruleToRemove);
                        params.addRule(ruleToAdd, subject);
                        holder.mTextLayout.setLayoutParams(params);
                    }
                });
                alphaAnimator.start();
            }
        }
    }

    public static class SongViewHolder extends RecyclerView.ViewHolder {
        public View mRootView;
        public ImageView mAlbumArt;
        public View mTextLayout;
        public TextView mTitle;
        public TextView mArtist;
        public VisualizerView mVisualizer;

        public SongViewHolder(Context context, View v, VisualizerHolder holder) {
            super(v);

            mRootView = v;
            mAlbumArt = (ImageView) v.findViewById(R.id.list_item_album_art_small);
            mTextLayout = v.findViewById(R.id.list_item_song_text_layout);
            mTitle = (TextView) v.findViewById(R.id.list_item_song_title);
            mArtist = (TextView) v.findViewById(R.id.list_item_artist_title);
            mVisualizer = (VisualizerView) v.findViewById(R.id.list_item_visualizer_view);
            mVisualizer.initialize(context, holder, false);
        }
    }
}
