package com.nong.musicplayer.model;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.nong.musicplayer.helper.ListHelper;
import com.nong.musicplayer.helper.OnlineListHelper;

import org.jetbrains.annotations.NotNull;

public class OnlineSong implements Parcelable {
    long leftLimit = 1L;
    long rightLimit = 10L;
    public static final Creator<OnlineSong> CREATOR = new Creator<OnlineSong>() {
        @Override
        public OnlineSong createFromParcel(Parcel in) {
            return new OnlineSong(in);
        }

        @Override
        public OnlineSong[] newArray(int size) {
            return new OnlineSong[size];
        }
    };
    public String artists;
    public String title;
    public String name_song;
    public String album;
    public String relativePath;
    public String absolutePath;
    public String thumbnail;
    public String link;
    public int year;
    public int track;
    public int startFrom;
    public long dateAdded;
    public String _id;
    public long duration;
    public long albumId;

    public OnlineSong(String artists, String title, String name_song, String album, String link,
                      Uri thumbnail, String _id) {
        this.artists = OnlineListHelper.ifNull(artists);
        this.title = OnlineListHelper.ifNull(title);
        this.name_song = OnlineListHelper.ifNull(name_song);
        this.album = OnlineListHelper.ifNull(album);
        this.relativePath = OnlineListHelper.ifNull(link);
        this.absolutePath = OnlineListHelper.ifNull(link);
        this.link = OnlineListHelper.ifNull(link);
        this.year = 1111;
        this.track = 1;
        this.startFrom = 1;
        this.dateAdded = 11;
        this._id = _id;
        this.duration = 1;
        this.albumId = leftLimit + (long) (Math.random() * (rightLimit - leftLimit));;
        this.thumbnail = thumbnail.toString();
    }

    protected OnlineSong(Parcel in) {
        artists = in.readString();
        title = in.readString();
        name_song = in.readString();
        album = in.readString();
        relativePath = in.readString();
        absolutePath = in.readString();
        link = in.readString();
        thumbnail = in.readString();
        year = in.readInt();
        track = in.readInt();
        startFrom = in.readInt();
        dateAdded = in.readLong();
        _id = in.readString();
        duration = in.readLong();
        albumId = in.readLong();
    }

    @NotNull
    @Override
    public String toString() {
        return "Music{" +
                "artist='" + artists + '\'' +
                ", title='" + title + '\'' +
                ", displayName='" + name_song + '\'' +
                ", album='" + album + '\'' +
                ", relativePath='" + relativePath + '\'' +
                ", absolutePath='" + absolutePath + '\'' +
                ", year=" + year +
                ", track=" + track +
                ", startFrom=" + startFrom +
                ", dateAdded=" + dateAdded +
                ", id=" + _id +
                ", duration=" + duration +
                ", albumId=" + albumId +
                ", albumArt=" + thumbnail +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(artists);
        dest.writeString(title);
        dest.writeString(name_song);
        dest.writeString(album);
        dest.writeString(relativePath);
        dest.writeString(absolutePath);
        dest.writeString(link);
        dest.writeString(thumbnail);
        dest.writeInt(year);
        dest.writeInt(track);
        dest.writeInt(startFrom);
        dest.writeLong(dateAdded);
        dest.writeString(_id);
        dest.writeLong(duration);
        dest.writeLong(albumId);
    }
}
