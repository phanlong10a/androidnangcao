package com.nong.musicplayer.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.nong.musicplayer.R;
import com.nong.musicplayer.adapter.OnlineSongAdapter;
import com.nong.musicplayer.adapter.SongsAdapter;
import com.nong.musicplayer.helper.ListHelper;
import com.nong.musicplayer.listener.MusicSelectListener;
import com.nong.musicplayer.listener.OnlineMusicSelectListener;
import com.nong.musicplayer.listener.PlayListListener;
import com.nong.musicplayer.model.Music;
import com.nong.musicplayer.model.OnlineSong;
import com.nong.musicplayer.viewmodel.MainViewModel;
import com.nong.musicplayer.viewmodel.MainViewModelFactory;

import java.util.ArrayList;
import java.util.List;

public class OnlineFragment extends Fragment implements SearchView.OnQueryTextListener, PlayListListener {
    private static OnlineMusicSelectListener listener;
    private final List<OnlineSong> musicList = new ArrayList<>();
    private MainViewModel viewModel;
    private OnlineSongAdapter songsAdapter;
    private List<OnlineSong> unChangedList = new ArrayList<>();

    private ExtendedFloatingActionButton shuffleControl;

    public OnlineFragment() {
    }

    public static OnlineFragment newInstance(OnlineMusicSelectListener selectListener) {
        OnlineFragment.listener = selectListener;
        return new OnlineFragment();
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    @Override
    public void option(Context context, Music music) {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity(),
                new MainViewModelFactory()).get(MainViewModel.class);
    }

    private void setUpUi(List<OnlineSong> songList) {
        unChangedList = songList;
        musicList.clear();
        musicList.addAll(unChangedList);
        shuffleControl.setText(String.valueOf(songList.size()));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_online, container, false);
        shuffleControl = view.findViewById(R.id.shuffle_button);
        RecyclerView recyclerView = view.findViewById(R.id.songs_layout);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        songsAdapter = new OnlineSongAdapter(listener, this, musicList);
        recyclerView.setAdapter(songsAdapter);

        shuffleControl.setOnClickListener(v -> listener.playQueue1(musicList, true));
        viewModel.getOnlineSongList().observe(requireActivity(), this::setUpUi);
        return view;
    }


    @Override
    public void onlineOption(Context context, OnlineSong music) {

    }
}
