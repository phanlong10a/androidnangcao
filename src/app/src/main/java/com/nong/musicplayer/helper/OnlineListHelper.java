package com.nong.musicplayer.helper;

import com.nong.musicplayer.MPConstants;
import com.nong.musicplayer.model.Album;
import com.nong.musicplayer.model.Artist;
import com.nong.musicplayer.model.Music;
import com.nong.musicplayer.model.OnlineSong;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import kotlin.collections.CollectionsKt;

public class OnlineListHelper {

    public static List<OnlineSong> searchMusicByName(List<OnlineSong> list, String query) {
        List<OnlineSong> newList = new ArrayList<>(list);
        return CollectionsKt.filter(newList, music ->
                (music.title.toLowerCase().contains(query) || music.name_song.toLowerCase().contains(query)) ||
                        (music.artists.toLowerCase().contains(query) || music.album.toLowerCase().contains(query))
        );
    }

    public static List<OnlineSong> sortMusicByDateAdded(List<OnlineSong> list, boolean reverse) {
        List<OnlineSong> newList = new ArrayList<>(list);
        Collections.sort(newList, new OnlineListHelper.SongComparator(MPConstants.SORT_MUSIC_BY_DATE_ADDED));

        if (reverse)
            Collections.reverse(newList);

        return newList;
    }

    public static List<OnlineSong> sortMusic(List<OnlineSong> list, boolean reverse) {
        List<OnlineSong> newList = new ArrayList<>(list);
        Collections.sort(newList, new OnlineListHelper.SongComparator(MPConstants.SORT_MUSIC_BY_TITLE));

        if (reverse)
            Collections.reverse(newList);

        return newList;
    }
    public static String ifNull(String val) {
        return val == null ? "" : val;
    }

    public static class SongComparator implements Comparator<OnlineSong> {
        private final int mode;

        public SongComparator(int mode) {
            this.mode = mode;
        }

        @Override
        public int compare(OnlineSong m1, OnlineSong m2) {
            if (mode == MPConstants.SORT_MUSIC_BY_TITLE)
                return m1.title.compareTo(m2.title);

            else if (mode == MPConstants.SORT_MUSIC_BY_DATE_ADDED)
                return Long.compare(m2.dateAdded, m1.dateAdded);

            return 0;
        }
    }
}


