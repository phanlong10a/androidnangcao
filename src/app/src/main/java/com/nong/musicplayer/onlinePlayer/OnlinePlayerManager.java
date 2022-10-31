package com.nong.musicplayer.onlinePlayer;

import static com.nong.musicplayer.MPConstants.AUDIO_FOCUSED;
import static com.nong.musicplayer.MPConstants.AUDIO_NO_FOCUS_CAN_DUCK;
import static com.nong.musicplayer.MPConstants.AUDIO_NO_FOCUS_NO_DUCK;
import static com.nong.musicplayer.MPConstants.CLOSE_ACTION;
import static com.nong.musicplayer.MPConstants.NEXT_ACTION;
import static com.nong.musicplayer.MPConstants.NOTIFICATION_ID;
import static com.nong.musicplayer.MPConstants.PLAY_PAUSE_ACTION;
import static com.nong.musicplayer.MPConstants.PREV_ACTION;
import static com.nong.musicplayer.MPConstants.VOLUME_DUCK;
import static com.nong.musicplayer.MPConstants.VOLUME_NORMAL;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.PowerManager;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.nong.musicplayer.model.OnlineSong;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class OnlinePlayerManager implements MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener {

    private final Context context;
    private final OnlinePlayerService onlinePlayerService;
    private final AudioManager audioManager;
    private final List<OnlinePlayerListener> playerListeners = new ArrayList<>();
    private final OnlinePlayerQueue onlinePlayerQueue;
    private final MutableLiveData<Integer> progressPercent = new MutableLiveData<>();
    private int playerState;
    private MediaPlayer mediaPlayer;
    private NotificationReceiver notificationReceiver;
    private OnlinePlayerNotificationManager notificationManager;
    private int currentAudioFocus = AUDIO_NO_FOCUS_NO_DUCK;
    private boolean playOnFocusGain;
    private final AudioManager.OnAudioFocusChangeListener audioFocusChangeListener =
            new AudioManager.OnAudioFocusChangeListener() {

                @Override
                public void onAudioFocusChange(final int focusChange) {
                    switch (focusChange) {
                        case AudioManager.AUDIOFOCUS_GAIN:
                            currentAudioFocus = AUDIO_FOCUSED;
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                            // Audio focus was lost, but it's possible to duck (i.e.: play quietly)
                            currentAudioFocus = AUDIO_NO_FOCUS_CAN_DUCK;
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                            // Lost audio focus, but will gain it back (shortly), so note whether
                            // playback should resume
                            currentAudioFocus = AUDIO_NO_FOCUS_NO_DUCK;
                            playOnFocusGain = isMediaPlayer() && playerState == PlaybackStateCompat.STATE_PLAYING || playerState == PlaybackStateCompat.STATE_NONE;
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS:
                            // Lost audio focus, probably "permanently"
                            currentAudioFocus = AUDIO_NO_FOCUS_NO_DUCK;
                            break;
                    }

                    if (mediaPlayer != null) {
                        // Update the player state based on the change
                        configurePlayerState();
                    }

                }
            };
    private MediaObserver mediaObserver;

    public OnlinePlayerManager(@NonNull OnlinePlayerService onlinePlayerService) {
        this.onlinePlayerService = onlinePlayerService;
        this.context = onlinePlayerService.getApplicationContext();
        this.onlinePlayerQueue = new OnlinePlayerQueue();
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        Observer<Integer> progressObserver = percent -> {
            for (OnlinePlayerListener playerListener : playerListeners)
                playerListener.onPositionChanged(percent);
        };
        progressPercent.observeForever(progressObserver);
    }

    public void registerActionsReceiver() {
        notificationReceiver = new OnlinePlayerManager.NotificationReceiver();
        final IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(PREV_ACTION);
        intentFilter.addAction(PLAY_PAUSE_ACTION);
        intentFilter.addAction(NEXT_ACTION);
        intentFilter.addAction(CLOSE_ACTION);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        intentFilter.addAction(Intent.ACTION_HEADSET_PLUG);
        intentFilter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

        onlinePlayerService.registerReceiver(notificationReceiver, intentFilter);
    }

    public void unregisterActionsReceiver() {
        if (onlinePlayerService != null && notificationReceiver != null) {
            try {
                onlinePlayerService.unregisterReceiver(notificationReceiver);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
    }

    public void attachListener(OnlinePlayerListener playerListener) {
        playerListeners.add(playerListener);
    }

    public void detachListener(OnlinePlayerListener playerListener) {
        if (playerListeners.size() > 2) {
            playerListeners.remove(playerListener);
        }
    }

    private void setPlayerState(@OnlinePlayerListener.State int state) {
        playerState = state;
        for (OnlinePlayerListener listener : playerListeners) {
            listener.onStateChanged(state);
        }

        onlinePlayerService.getNotificationManager().updateNotification();

        int playbackState = isPlaying() ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED;

        onlinePlayerService.getMediaSessionCompat().setPlaybackState(
                new PlaybackStateCompat.Builder()
                        .setActions(
                                PlaybackStateCompat.ACTION_PLAY |
                                        PlaybackStateCompat.ACTION_PAUSE |
                                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                                        PlaybackStateCompat.ACTION_SEEK_TO |
                                        PlaybackStateCompat.ACTION_PLAY_PAUSE)
                        .setState(playbackState, mediaPlayer.getCurrentPosition(), 0)
                        .build());
    }

    public OnlineSong getCurrentMusic() {
        return onlinePlayerQueue.getCurrentMusic();
    }

    public int getCurrentPosition() {
        return mediaPlayer.getCurrentPosition();
    }

    public int getDuration() {
        return mediaPlayer.getDuration();
    }

    public OnlinePlayerQueue getPlayerQueue() {
        return onlinePlayerQueue;
    }

    public void setPlayerListener(OnlinePlayerListener listener) {
        playerListeners.add(listener);
        listener.onPrepared();
    }

    public void setMusicList(List<OnlineSong> musicList) {
        onlinePlayerQueue.setCurrentQueue(musicList);
        initMediaPlayer(); // play now
    }

    public void setMusic(OnlineSong music) {
        List<OnlineSong> musicList = new ArrayList<>();
        musicList.add(music);
        onlinePlayerQueue.setCurrentQueue(musicList);
        initMediaPlayer();
    }

    public void addMusicQueue(List<OnlineSong> musicList) {
        onlinePlayerQueue.addMusicListToQueue(musicList);

        if (!mediaPlayer.isPlaying())
            initMediaPlayer();  // play when ready
    }

    public int getAudioSessionId() {
        return (mediaPlayer != null) ? mediaPlayer.getAudioSessionId() : -1;
    }

    public void detachService() {
        onlinePlayerService.stopForeground(false);
    }

    public void attachService() {
        if (notificationManager != null) {
            onlinePlayerService.startForeground(NOTIFICATION_ID, notificationManager.createNotification());
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        playNext();
        for (OnlinePlayerListener listener : playerListeners)
            listener.onPlaybackCompleted();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
        onlinePlayerService.startForeground(NOTIFICATION_ID, notificationManager.createNotification());

        for (OnlinePlayerListener listener : playerListeners)
            listener.onMusicSet(onlinePlayerQueue.getCurrentMusic());

        if (mediaObserver == null) {
            mediaObserver = new MediaObserver();
            new Thread(mediaObserver).start();
        }
    }

    public boolean isPlaying() {
        return isMediaPlayer() && mediaPlayer.isPlaying();
    }

    public boolean isMediaPlayer() {
        return mediaPlayer != null;
    }

    public void pauseMediaPlayer() {
        setPlayerState(OnlinePlayerListener.State.PAUSED);
        mediaPlayer.pause();

        onlinePlayerService.stopForeground(false);
        notificationManager.getNotificationManager().notify(NOTIFICATION_ID, notificationManager.createNotification());
    }

    public void resumeMediaPlayer() {
        if (!isPlaying()) {
            mediaPlayer.start();
            setPlayerState(OnlinePlayerListener.State.RESUMED);
            onlinePlayerService.startForeground(NOTIFICATION_ID, notificationManager.createNotification());

            if (notificationManager != null) {
                notificationManager.updateNotification();
            }
        }
    }

    public void playPrev() {
        onlinePlayerQueue.prev();
        initMediaPlayer();
    }

    public void playNext() {
        onlinePlayerQueue.next();
        initMediaPlayer();
    }

    public void playPause() {
        if (isPlaying()) {
            mediaPlayer.pause();
            setPlayerState(OnlinePlayerListener.State.PAUSED);
        } else {
            mediaPlayer.start();
            setPlayerState(OnlinePlayerListener.State.PLAYING);
        }
    }

    public void release() {
        mediaObserver.stop();
        onlinePlayerService.stopForeground(true);
        onlinePlayerService.stopSelf();
        mediaPlayer.release();
        mediaPlayer = null;

        for (OnlinePlayerListener playerListener : playerListeners)
            playerListener.onRelease();

    }

    public void seekTo(int position) {
        mediaPlayer.seekTo(position);
    }

    private void configurePlayerState() {

        if (currentAudioFocus == AUDIO_NO_FOCUS_NO_DUCK) {
            // We don't have audio focus and can't duck, so we have to pause
            pauseMediaPlayer();
        } else {

            if (currentAudioFocus == AUDIO_NO_FOCUS_CAN_DUCK) {
                // We're permitted to play, but only if we 'duck', ie: play softly
                mediaPlayer.setVolume(VOLUME_DUCK, VOLUME_DUCK);
            } else {
                mediaPlayer.setVolume(VOLUME_NORMAL, VOLUME_NORMAL);
            }

            // If we were playing when we lost focus, we need to resume playing.
            if (playOnFocusGain) {
                resumeMediaPlayer();
                playOnFocusGain = false;
            }
        }
    }

    private void tryToGetAudioFocus() {
        final int result = audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            currentAudioFocus = AUDIO_FOCUSED;
        } else {
            currentAudioFocus = AUDIO_NO_FOCUS_NO_DUCK;
        }
    }

    private void initMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.reset();
        } else {
            mediaPlayer = new MediaPlayer();

            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());

            notificationManager = onlinePlayerService.getNotificationManager();
        }

        tryToGetAudioFocus();
        Uri trackUri = ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                onlinePlayerQueue.getCurrentMusic().albumId);

        try {
            mediaPlayer.setDataSource(context, trackUri);
            mediaPlayer.prepareAsync();
            setPlayerState(OnlinePlayerListener.State.PLAYING);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
    }

    public class NotificationReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(@NonNull final Context context, @NonNull final Intent intent) {
            final String action = intent.getAction();

            OnlineSong currentSong = null;
            if (onlinePlayerQueue.getCurrentQueue() != null)
                currentSong = onlinePlayerQueue.getCurrentMusic();


            if (action != null && currentSong != null) {

                switch (action) {
                    case PREV_ACTION:
                        playPrev();
                        break;

                    case PLAY_PAUSE_ACTION:
                        playPause();
                        break;

                    case NEXT_ACTION:
                        playNext();
                        break;

                    case CLOSE_ACTION:
                        release();
                        break;

                    case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                        pauseMediaPlayer();
                        break;
                    case BluetoothDevice.ACTION_ACL_CONNECTED:
                        if (!isPlaying()) {
                            resumeMediaPlayer();
                        }
                        break;

                    case Intent.ACTION_HEADSET_PLUG:
                        if (intent.hasExtra("state")) {
                            switch (intent.getIntExtra("state", -1)) {
                                //0 means disconnected
                                case 0:
                                    pauseMediaPlayer();
                                    break;
                                //1 means connected
                                case 1:
                                    if (!isPlaying()) {
                                        resumeMediaPlayer();
                                    }
                                    break;
                            }
                        }
                        break;

                    case AudioManager.ACTION_AUDIO_BECOMING_NOISY:
                        if (isPlaying()) {
                            pauseMediaPlayer();
                        }
                        break;
                }
            }
        }
    }

    private class MediaObserver implements Runnable {
        private final AtomicBoolean stop = new AtomicBoolean(false);

        public void stop() {
            stop.set(true);
        }

        @Override
        public void run() {
            while (!stop.get()) {
                try {

                    if (mediaPlayer != null && isPlaying()) {
                        int percent = mediaPlayer.getCurrentPosition() * 100 / mediaPlayer.getDuration();
                        progressPercent.postValue(percent);
                    }

                    Thread.sleep(100);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
