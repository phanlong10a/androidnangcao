package com.nong.musicplayer.listener;

import com.nong.musicplayer.model.OnlineSong;

import java.util.List;

public interface OnlineMusicSelectListener {
    void playQueue1(List<OnlineSong> musicList, boolean shuffle);

    void addToQueue1(List<OnlineSong> musicList);

    void refreshMediaLibrary1();
}
