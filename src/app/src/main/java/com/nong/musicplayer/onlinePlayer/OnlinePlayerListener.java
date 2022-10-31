package com.nong.musicplayer.onlinePlayer;

import androidx.annotation.IntDef;

import com.nong.musicplayer.model.Music;
import com.nong.musicplayer.model.OnlineSong;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public interface OnlinePlayerListener {

    void onPrepared();

    void onStateChanged(@State int state);

    void onPositionChanged(int position);

    void onMusicSet(OnlineSong music);

    void onPlaybackCompleted();

    void onRelease();

    @IntDef({OnlinePlayerListener.State.INVALID,
            OnlinePlayerListener.State.PLAYING,
            OnlinePlayerListener.State.PAUSED,
            OnlinePlayerListener.State.COMPLETED,
            OnlinePlayerListener.State.RESUMED})
    @Retention(RetentionPolicy.SOURCE)
    @interface State {
        int INVALID = -1;
        int PLAYING = 0;
        int PAUSED = 1;
        int COMPLETED = 2;
        int RESUMED = 3;
    }
}
