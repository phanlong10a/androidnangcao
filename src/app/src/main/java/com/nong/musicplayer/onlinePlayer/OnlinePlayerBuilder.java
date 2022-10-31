package com.nong.musicplayer.onlinePlayer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import androidx.annotation.NonNull;

public class OnlinePlayerBuilder {
    private final Context context;
    private final OnlinePlayerListener playerListener;
    private OnlinePlayerService onlinePlayerService;
    private OnlinePlayerManager playerManager;
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(@NonNull final ComponentName componentName, @NonNull final IBinder iBinder) {
            onlinePlayerService = ((OnlinePlayerService.LocalBinder) iBinder).getInstance();
            playerManager = onlinePlayerService.getPlayerManager();

            if (playerListener != null) {
                playerManager.setPlayerListener(playerListener);
            }
        }

        @Override
        public void onServiceDisconnected(@NonNull final ComponentName componentName) {
            onlinePlayerService = null;
        }
    };

    public OnlinePlayerBuilder(Context context, OnlinePlayerListener listener) {
        this.context = context;
        this.playerListener = listener;

        bindService();
    }

    public OnlinePlayerManager getPlayerManager() {
        return playerManager;
    }

    public void bindService() {
        context.bindService(new Intent(context, OnlinePlayerService.class), serviceConnection, Context.BIND_AUTO_CREATE);
        context.startService(new Intent(context, OnlinePlayerService.class));
    }

    public void unBindService() {
        if (playerManager != null) {
            playerManager.detachService();
            context.unbindService(serviceConnection);
        }
    }
}
