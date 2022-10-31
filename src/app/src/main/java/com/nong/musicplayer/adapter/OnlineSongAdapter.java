package com.nong.musicplayer.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.nong.musicplayer.MPPreferences;
import com.nong.musicplayer.R;
import com.nong.musicplayer.helper.MusicLibraryHelper;
import com.nong.musicplayer.listener.MusicSelectListener;
import com.nong.musicplayer.listener.OnlineMusicSelectListener;
import com.nong.musicplayer.listener.PlayListListener;
import com.nong.musicplayer.model.Music;
import com.nong.musicplayer.model.OnlineSong;

import java.util.List;
import java.util.Locale;

public class OnlineSongAdapter extends RecyclerView.Adapter<OnlineSongAdapter.MyViewHolder> {

    private final List<OnlineSong> musicList;
    private final PlayListListener playListListener;
    public OnlineMusicSelectListener listener;

    public OnlineSongAdapter(OnlineMusicSelectListener listener, PlayListListener playListListener, List<OnlineSong> musics) {
        this.listener = listener;
        this.musicList = musics;
        this.playListListener = playListListener;
    }

    @NonNull
    @Override
    public OnlineSongAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_songs, parent, false);
        return new OnlineSongAdapter.MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OnlineSongAdapter.MyViewHolder holder, int position) {
        OnlineSong music = musicList.get(position);

        holder.songName.setText(music.title);
        holder.albumName.setText(
                String.format(Locale.getDefault(), "%s • %s",
                        music.artists,
                        music.album)
        );

        if (music.dateAdded == -1)
            holder.songHistory.setVisibility(View.GONE);
        else
            holder.songHistory.setText(
                    String.format(Locale.getDefault(), "%s • %s",
                            MusicLibraryHelper.formatDuration(music.duration),
                            MusicLibraryHelper.formatDate(music.dateAdded))
            );

        if (holder.state && !music.thumbnail.equals("")) {
            Glide.with(holder.albumArt.getContext())
                    .load(music.thumbnail)
                    .placeholder(R.drawable.ic_album_art)
                    .into(holder.albumArt);
        } else if (music.thumbnail.equals("")) {
            holder.albumArt.setImageResource(R.drawable.ic_album_art);
        }
    }

    @Override
    public int getItemCount() {
        return musicList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        private final TextView songName;
        private final TextView albumName;
        private final TextView songHistory;
        private final ImageView albumArt;
        private final boolean state;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            state = MPPreferences.getAlbumRequest(itemView.getContext());
            albumArt = itemView.findViewById(R.id.album_art);
            songHistory = itemView.findViewById(R.id.song_history);
            songName = itemView.findViewById(R.id.song_name);
            albumName = itemView.findViewById(R.id.song_album);

            itemView.findViewById(R.id.root_layout).setOnClickListener(v ->
                    listener.playQueue1(musicList.subList(getAdapterPosition(), musicList.size()), false));

            itemView.findViewById(R.id.root_layout).setOnLongClickListener(v -> {
                playListListener.onlineOption(itemView.getContext(), musicList.get(getAdapterPosition()));
                return true;
            });
        }
    }
}

