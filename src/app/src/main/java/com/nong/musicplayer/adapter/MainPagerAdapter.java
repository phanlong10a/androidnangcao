package com.nong.musicplayer.adapter;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.nong.musicplayer.fragments.AlbumsFragment;
import com.nong.musicplayer.fragments.ArtistsFragment;
import com.nong.musicplayer.fragments.OnlineFragment;
import com.nong.musicplayer.fragments.PlaylistFragment;
import com.nong.musicplayer.fragments.SettingsFragment;
import com.nong.musicplayer.fragments.SongsFragment;
import com.nong.musicplayer.listener.MusicSelectListener;
import com.nong.musicplayer.listener.OnlineMusicSelectListener;
import com.nong.musicplayer.model.OnlineSong;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MainPagerAdapter extends FragmentPagerAdapter {

    private final MusicSelectListener selectListener;
    private final OnlineMusicSelectListener selectListener1;
    List<Fragment> fragments = new ArrayList<>();

    public MainPagerAdapter(FragmentManager fm, MusicSelectListener selectListener, OnlineMusicSelectListener selectListener1) {
        super(fm, FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        this.selectListener = selectListener;
        this.selectListener1 = selectListener1;

        setFragments();
    }

    public void setFragments() {
        fragments.add(SongsFragment.newInstance(selectListener));
        fragments.add(ArtistsFragment.newInstance());
        fragments.add(AlbumsFragment.newInstance());
        fragments.add(PlaylistFragment.newInstance());
        fragments.add(SettingsFragment.newInstance());
        fragments.add(OnlineFragment.newInstance(selectListener1));
    }

    @NotNull
    @Override
    public Fragment getItem(int position) {
        return fragments.get(position);
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return null;
    }

    @Override
    public int getCount() {
        return fragments.size();
    }
}