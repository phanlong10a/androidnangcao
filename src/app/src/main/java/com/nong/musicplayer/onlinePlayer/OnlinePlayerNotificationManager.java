package com.nong.musicplayer.onlinePlayer;

import static com.nong.musicplayer.MPConstants.CHANNEL_ID;
import static com.nong.musicplayer.MPConstants.CLOSE_ACTION;
import static com.nong.musicplayer.MPConstants.NEXT_ACTION;
import static com.nong.musicplayer.MPConstants.NOTIFICATION_ID;
import static com.nong.musicplayer.MPConstants.PLAY_PAUSE_ACTION;
import static com.nong.musicplayer.MPConstants.PREV_ACTION;
import static com.nong.musicplayer.MPConstants.REQUEST_CODE;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.nong.musicplayer.MainActivity;
import com.nong.musicplayer.R;
import com.nong.musicplayer.helper.MusicLibraryHelper;
import com.nong.musicplayer.model.Music;
import com.nong.musicplayer.model.OnlineSong;

public class OnlinePlayerNotificationManager {

    private final NotificationManager notificationManager;
    private final OnlinePlayerService onlinePlayerService;
    private NotificationCompat.Builder notificationBuilder;

    OnlinePlayerNotificationManager(@NonNull final OnlinePlayerService onlinePlayerService) {
        this.onlinePlayerService = onlinePlayerService;
        notificationManager = (NotificationManager) onlinePlayerService.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public final NotificationManager getNotificationManager() {
        return notificationManager;
    }

    private PendingIntent playerAction(@NonNull final String action) {
        final Intent pauseIntent = new Intent();
        pauseIntent.setAction(action);

        return PendingIntent.getBroadcast(onlinePlayerService, REQUEST_CODE, pauseIntent, PendingIntent.FLAG_IMMUTABLE);
    }

    public Notification createNotification() {
        final OnlineSong song = onlinePlayerService.getPlayerManager().getCurrentMusic();
        notificationBuilder = new NotificationCompat.Builder(onlinePlayerService, CHANNEL_ID);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }

        final Intent openPlayerIntent = new Intent(onlinePlayerService, MainActivity.class);
        openPlayerIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        final PendingIntent contentIntent = PendingIntent.getActivity(onlinePlayerService, REQUEST_CODE,
                openPlayerIntent, PendingIntent.FLAG_IMMUTABLE);

        Bitmap albumArt = MusicLibraryHelper.getThumbnail(onlinePlayerService.getApplicationContext(), song.thumbnail);

        notificationBuilder
                .setShowWhen(false)
                .setSmallIcon(R.drawable.ic_notif_music_note)
                .setContentTitle(song.title)
                .setContentText(song.artists)
                .setProgress(100, onlinePlayerService.getPlayerManager().getCurrentPosition(), true)
                .setColor(MusicLibraryHelper.getDominantColorFromThumbnail(albumArt))
                .setColorized(false)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setLargeIcon(albumArt)
                .addAction(notificationAction(PREV_ACTION))
                .addAction(notificationAction(PLAY_PAUSE_ACTION))
                .addAction(notificationAction(NEXT_ACTION))
                .addAction(notificationAction(CLOSE_ACTION))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle().setShowActionsInCompactView(0, 1, 2));

        return notificationBuilder.build();
    }

    @SuppressLint("RestrictedApi")
    public void updateNotification() {
        if (notificationBuilder == null)
            return;

        notificationBuilder.setOngoing(onlinePlayerService.getPlayerManager().isPlaying());
        OnlinePlayerManager playerManager = onlinePlayerService.getPlayerManager();
        OnlineSong song = playerManager.getCurrentMusic();
        Bitmap albumArt = MusicLibraryHelper.getThumbnail(onlinePlayerService.getApplicationContext(),
                song.thumbnail);

        if (notificationBuilder.mActions.size() > 0)
            notificationBuilder.mActions.set(1, notificationAction(PLAY_PAUSE_ACTION));

        notificationBuilder
                .setLargeIcon(albumArt)
                .setColor(MusicLibraryHelper.getDominantColorFromThumbnail(albumArt))
                .setContentTitle(song.title)
                .setContentText(song.artists)
                .setColorized(false)
                .setAutoCancel(true)
                .setSubText(song.album);


        NotificationManagerCompat.from(onlinePlayerService).notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    @NonNull
    private NotificationCompat.Action notificationAction(@NonNull final String action) {
        int icon = R.drawable.ic_controls_pause;

        switch (action) {
            case PREV_ACTION:
                icon = R.drawable.ic_controls_prev;
                break;
            case PLAY_PAUSE_ACTION:
                icon = onlinePlayerService.getPlayerManager().isPlaying() ? R.drawable.ic_controls_pause : R.drawable.ic_controls_play;
                break;
            case NEXT_ACTION:
                icon = R.drawable.ic_controls_next;
                break;
            case CLOSE_ACTION:
                icon = R.drawable.ic_close;
                break;
        }
        return new NotificationCompat.Action.Builder(icon, action, playerAction(action)).build();
    }

    @RequiresApi(26)
    private void createNotificationChannel() {

        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            final NotificationChannel notificationChannel =
                    new NotificationChannel(CHANNEL_ID,
                            onlinePlayerService.getString(R.string.app_name),
                            NotificationManager.IMPORTANCE_LOW);

            notificationChannel.setDescription(onlinePlayerService.getString(R.string.app_name));
            notificationChannel.enableLights(false);
            notificationChannel.enableVibration(false);
            notificationChannel.setShowBadge(false);

            notificationManager.createNotificationChannel(notificationChannel);
        }
    }
}
