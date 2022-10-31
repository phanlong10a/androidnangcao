package com.nong.musicplayer.listener;

import android.content.Context;

import com.nong.musicplayer.model.Music;
import com.nong.musicplayer.model.OnlineSong;

public interface PlayListListener {
    void option(Context context, Music music);

    void onlineOption(Context context, OnlineSong music);
}
