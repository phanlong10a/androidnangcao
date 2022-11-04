package com.nong.musicplayer;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager.widget.ViewPager;

import com.google.gson.JsonObject;
import com.nong.musicplayer.activities.PlayerDialog;
import com.nong.musicplayer.activities.QueueDialog;
import com.nong.musicplayer.adapter.MainPagerAdapter;
import com.nong.musicplayer.dialogs.SleepTimerDialog;
import com.nong.musicplayer.dialogs.SleepTimerDisplayDialog;
import com.nong.musicplayer.helper.MusicLibraryHelper;
import com.nong.musicplayer.helper.ThemeHelper;
import com.nong.musicplayer.listener.MusicSelectListener;
import com.nong.musicplayer.listener.OnlineMusicSelectListener;
import com.nong.musicplayer.listener.PlayerDialogListener;
import com.nong.musicplayer.listener.SleepTimerSetListener;
import com.nong.musicplayer.model.Music;
import com.nong.musicplayer.model.OnlineSong;
import com.nong.musicplayer.onlinePlayer.OnlinePlayerBuilder;
import com.nong.musicplayer.onlinePlayer.OnlinePlayerListener;
import com.nong.musicplayer.onlinePlayer.OnlinePlayerManager;
import com.nong.musicplayer.player.PlayerBuilder;
import com.nong.musicplayer.player.PlayerListener;
import com.nong.musicplayer.player.PlayerManager;
import com.nong.musicplayer.viewmodel.MainViewModel;
import com.nong.musicplayer.viewmodel.MainViewModelFactory;
import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.tabs.TabLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public class MainActivity extends AppCompatActivity
        implements OnlinePlayerListener, MusicSelectListener, PlayerListener, View.OnClickListener, SleepTimerSetListener, PlayerDialogListener, OnlineMusicSelectListener {

    public static boolean isSleepTimerRunning;
    public static MutableLiveData<Long> sleepTimerTick;
    private static CountDownTimer sleepTimer;
    private RelativeLayout playerView;
    private ImageView albumArt;
    private TextView songName;
    private TextView songDetails;
    private ImageButton play_pause;
    private LinearProgressIndicator progressIndicator;
    private PlayerDialog playerDialog;
    private QueueDialog queueDialog;
    private PlayerBuilder playerBuilder;
    private OnlinePlayerBuilder playerBuilder1;
    private PlayerManager playerManager;
    private OnlinePlayerManager playerManager1;
    private boolean albumState;
    private MainViewModel viewModel;
    List<OnlineSong> listMusicOnline = new ArrayList<>();
    List<Music> songList = new ArrayList<>();
    fetchData firstFetch = new fetchData();
    Handler mainHandler = new Handler();
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(ThemeHelper.getTheme(MPPreferences.getTheme(getApplicationContext())));
        MPPreferences.storeAlbumRequest(getApplicationContext(), true);
        AppCompatDelegate.setDefaultNightMode(MPPreferences.getThemeMode(getApplicationContext()));
        setContentView(R.layout.activity_main);
        MPConstants.musicSelectListener = this;

        viewModel = new ViewModelProvider(this, new MainViewModelFactory()).get(MainViewModel.class);
        fetchMusicList();
        getListMusicOnline();
        if (hasReadStoragePermission(MainActivity.this))
            setUpUiElements();
        else
            manageStoragePermission(MainActivity.this);

        albumState = MPPreferences.getAlbumRequest(this);
        MPConstants.musicSelectListener = this;

        MaterialCardView playerLayout = findViewById(R.id.player_layout);
        albumArt = findViewById(R.id.albumArt);
        progressIndicator = findViewById(R.id.song_progress);
        playerView = findViewById(R.id.player_view);
        songName = findViewById(R.id.song_title);
        songDetails = findViewById(R.id.song_details);
        play_pause = findViewById(R.id.control_play_pause);
        ImageButton queue = findViewById(R.id.control_queue);

        play_pause.setOnClickListener(this);
        playerLayout.setOnClickListener(this);
        queue.setOnClickListener(this);
    }

    private void setPlayerView() {
        if (playerManager != null && playerManager.isPlaying()) {
            playerView.setVisibility(View.VISIBLE);
            onMusicSet(playerManager.getCurrentMusic());
        }
    }

    private void fetchMusicList() {
        new Handler().post(() -> {
            List<Music> musicList = MusicLibraryHelper.fetchMusicLibrary(MainActivity.this);
            songList = musicList;
        });
    }

    public void getListMusicOnline() {
        firstFetch.start();
        System.out.println("a");
    }

    @Override
    public void playQueue1(List<OnlineSong> musicList, boolean shuffle) {
        if (shuffle) {
            Collections.shuffle(musicList);
        }

        if (musicList.size() > 0) {
            playerManager1.setMusicList(musicList);
            setPlayerView();
        }
    }

    @Override
    public void addToQueue1(List<OnlineSong> musicList) {
        if (musicList.size() > 0) {
            if (playerManager1 != null && playerManager1.isPlaying())
                playerManager1.addMusicQueue(musicList);
            else if (playerManager1 != null)
                playerManager1.setMusicList(musicList);
            setPlayerView();
        }
    }

    @Override
    public void refreshMediaLibrary1() {

    }

    class fetchData extends Thread {
        String data = "";

        @Override
        public void run() {

            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    progressDialog = new ProgressDialog(MainActivity.this);
                    progressDialog.setMessage("loading.......");
                    progressDialog.setCancelable(false);
                    progressDialog.show();
                }
            });


            try {
                URL url = new URL("http://13.212.58.90:5000/api/getAllSong");
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                InputStream inputStream = httpURLConnection.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String line;

                while ((line = bufferedReader.readLine()) != null) {
                    data = data + line;
                }

                if (!data.isEmpty()) {
                    JSONArray jsonArray = new JSONArray(data);
                    listMusicOnline.clear();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject onlineSong = jsonArray.getJSONObject(i);
                        String artists = onlineSong.getString("artists");
                        String title = onlineSong.getString("title");
                        String name_song = onlineSong.getString("name_song");
                        String link = onlineSong.getString("link");
                        String thumbnail = onlineSong.getString("thumbnail");
                        String _id = onlineSong.getString("_id");

                        OnlineSong onlineSong1 = new OnlineSong(artists, title, name_song, null, link, Uri.parse(thumbnail), _id);
                        Music music = new Music(artists, title, name_song, null, link, link, 2022, 0, 0, 2022, ThreadLocalRandom.current().nextLong(100), 1, 1,Uri.parse(thumbnail));
                        System.out.println(music);
                        listMusicOnline.add(onlineSong1);
                        songList.add(music);
                    }
                }
            } catch (JSONException | IOException e) {
                e.printStackTrace();
            }


            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();

                        viewModel.setOnlineSongList(listMusicOnline);
                        viewModel.setSongsList(songList);
                        viewModel.parseFolderList(songList);
                    }
                }
            });

        }
    }


    public boolean checkConnection() {
        return true;
    }

    public void setUpUiElements() {
        playerBuilder = new PlayerBuilder(MainActivity.this, this);
        playerBuilder1 = new OnlinePlayerBuilder(MainActivity.this, this);
        MainPagerAdapter sectionsPagerAdapter = new MainPagerAdapter(
                getSupportFragmentManager(), this, this);

        ViewPager viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(sectionsPagerAdapter);
        viewPager.setOffscreenPageLimit(3);

        TabLayout tabs = findViewById(R.id.tabs);
        tabs.setupWithViewPager(viewPager);

        int count = tabs.getTabCount();
        for (int i = 0; i < count; i++) {
            TabLayout.Tab tab = tabs.getTabAt(i);
            if (tab != null) {
                tab.setIcon(MPConstants.TAB_ICONS[i]);
            }
        }
    }

    public void manageStoragePermission(Activity context) {
        if (!hasReadStoragePermission(context)) {
            // required a dialog?
            if (ActivityCompat.shouldShowRequestPermissionRationale(context, Manifest.permission.READ_EXTERNAL_STORAGE) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                new MaterialAlertDialogBuilder(context)
                        .setTitle("Requesting permission")
                        .setMessage("Enable storage permission for accessing the media files.")
                        .setPositiveButton("Accept", (dialog, which) -> askReadStoragePermission(context)).show();
            } else
                askReadStoragePermission(context);
        }
    }

    public boolean hasReadStoragePermission(Activity context) {
        return (
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        );
    }

    public void askReadStoragePermission(Activity context) {
        ActivityCompat.requestPermissions(
                context,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                MPConstants.PERMISSION_READ_STORAGE
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            ThemeHelper.applySettings(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (playerBuilder != null)
            playerBuilder.unBindService();

        if (playerDialog != null)
            playerDialog.dismiss();

        if (queueDialog != null)
            queueDialog.dismiss();
    }

    @Override
    public void playQueue(List<Music> musicList, boolean shuffle) {
        if (shuffle) {
            Collections.shuffle(musicList);
        }

        if (musicList.size() > 0) {
            playerManager.setMusicList(musicList);
            setPlayerView();
        }
    }

    @Override
    public void addToQueue(List<Music> musicList) {
        if (musicList.size() > 0) {
            if (playerManager != null && playerManager.isPlaying())
                playerManager.addMusicQueue(musicList);
            else if (playerManager != null)
                playerManager.setMusicList(musicList);

            setPlayerView();
        }
    }

    @Override
    public void refreshMediaLibrary() {
        fetchMusicList();
    }

    @Override
    public void onPrepared() {
        playerManager = playerBuilder.getPlayerManager();
        playerManager1 = playerBuilder1.getPlayerManager();
        setPlayerView();
    }

    @Override
    public void onStateChanged(int state) {
        if (state == PlayerListener.State.PLAYING)
            play_pause.setImageResource(R.drawable.ic_controls_pause);
        else
            play_pause.setImageResource(R.drawable.ic_controls_play);
    }

    @Override
    public void onPositionChanged(int position) {
        progressIndicator.setProgress(position);
    }

    @Override
    public void onMusicSet(OnlineSong music) {

    }

    @Override
    public void onMusicSet(Music music) {
        songName.setText(music.title);
        songDetails.setText(
                String.format(Locale.getDefault(), "%s • %s",
                        music.artist, music.album));
        playerView.setVisibility(View.VISIBLE);

        if (albumState)
            Glide.with(getApplicationContext())
                    .load(music.albumArt)
                    .centerCrop()
                    .into(albumArt);

        if (playerManager != null && playerManager.isPlaying())
            play_pause.setImageResource(R.drawable.ic_controls_pause);
        else
            play_pause.setImageResource(R.drawable.ic_controls_play);
    }

    @Override
    public void onPlaybackCompleted() {
    }

    @Override
    public void onRelease() {
        playerView.setVisibility(View.GONE);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.control_play_pause)
            playerManager.playPause();

        else if (id == R.id.control_queue)
            setUpQueueDialogHeadless();

        else if (id == R.id.player_layout)
            setUpPlayerDialog();
    }

    private void setUpPlayerDialog() {
        playerDialog = new PlayerDialog(this, playerManager, this);

        playerDialog.show();
    }

    @Override
    public void setTimer(int minutes) {
        if (!isSleepTimerRunning) {
            isSleepTimerRunning = true;
            sleepTimer = new CountDownTimer(minutes * 60 * 1000L, 1000) {
                @Override
                public void onTick(long l) {
                    if (sleepTimerTick == null) sleepTimerTick = new MutableLiveData<>();
                    sleepTimerTick.postValue(l);
                }

                @Override
                public void onFinish() {
                    isSleepTimerRunning = false;
                    playerManager.pauseMediaPlayer();
                }
            }.start();
        }
    }

    @Override
    public void cancelTimer() {
        if (isSleepTimerRunning && sleepTimer != null) {
            isSleepTimerRunning = false;
            sleepTimer.cancel();
        }
    }

    @Override
    public MutableLiveData<Long> getTick() {
        return sleepTimerTick;
    }


    @Override
    public void queueOptionSelect() {
        setUpQueueDialog();
    }

    @Override
    public void sleepTimerOptionSelect() {
        setUpSleepTimerDialog();
    }

    private void setUpQueueDialog() {
        queueDialog = new QueueDialog(MainActivity.this, playerManager.getPlayerQueue());
        queueDialog.setOnDismissListener(v -> playerDialog.show());

        playerDialog.dismiss();
        queueDialog.show();
    }

    private void setUpQueueDialogHeadless() {
        queueDialog = new QueueDialog(MainActivity.this, playerManager.getPlayerQueue());
        queueDialog.show();
    }

    private void setUpSleepTimerDialog() {
        if (MainActivity.isSleepTimerRunning) {
            setUpSleepTimerDisplayDialog();
            return;
        }
        SleepTimerDialog sleepTimerDialog = new SleepTimerDialog(MainActivity.this, this);
        sleepTimerDialog.setOnDismissListener(v -> playerDialog.show());

        playerDialog.dismiss();
        sleepTimerDialog.show();
    }

    private void setUpSleepTimerDisplayDialog() {
        SleepTimerDisplayDialog sleepTimerDisplayDialog = new SleepTimerDisplayDialog(MainActivity.this, this);
        sleepTimerDisplayDialog.setOnDismissListener(v -> playerDialog.show());

        playerDialog.dismiss();
        sleepTimerDisplayDialog.show();
    }
}