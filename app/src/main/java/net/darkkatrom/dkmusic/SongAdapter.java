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

package net.darkkatrom.dkmusic;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class SongAdapter extends
        RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    private Context mContext;
    private List<Song> mSongs;

    private OnSongClickedListener mOnSongClickedListener;

    public interface OnSongClickedListener {
        public void onSongClicked(Song song, int position);
    }

    public SongAdapter(Context context, List<Song> songs) {
        super();

        mContext = context;
        if (songs != null) {
            mSongs = songs;
        } else {
            mSongs = new ArrayList<Song>();
        }
    }

    @Override
    public SongAdapter.SongViewHolder onCreateViewHolder(ViewGroup parent,
            int viewType) {

        View v = LayoutInflater.from(mContext).inflate(
                R.layout.list_item, parent, false);
        return new SongViewHolder(v);
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
    }

    @Override
    public int getItemCount() {
        return mSongs.size();
    }

    public void setOnSongClickedListener(OnSongClickedListener onItemClickedListener) {
        mOnSongClickedListener = onItemClickedListener;
    }

    public static class SongViewHolder extends RecyclerView.ViewHolder {
        public View mRootView;
        public ImageView mAlbumArt;
        public TextView mTitle;
        public TextView mArtist;

        public SongViewHolder(View v) {
            super(v);

            mRootView = v;
            mAlbumArt = (ImageView) v.findViewById(R.id.list_item_album_art_small);
            mTitle = (TextView) v.findViewById(R.id.list_item_song_title);
            mArtist = (TextView) v.findViewById(R.id.list_item_artist_title);
        }
    }
}
