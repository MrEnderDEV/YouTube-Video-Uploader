package io.github.stekeblad.videouploader.youtube;

import org.jetbrains.annotations.NotNull;

/**
 * A class for managing the details of a playlist that has been retrieved from YouTube.
 * visible is for filtering what playlist to show in the program, useful if the user has a lot of playlist and only
 * need to use a few of them with the program.
 * id is the playlist id on YouTube.
 * name is the name of the playlist.
 */
public class LocalPlaylist implements Comparable<LocalPlaylist> {
    private boolean visible;
    private String id;
    private String name;

    public static final String MAGIC_PLAYLIST_ID = "0";

    public LocalPlaylist(boolean visible, String playlistId, String playlistName) {
        this.visible = visible;
        this.id = playlistId;
        this.name = playlistName;
    }

    public boolean isVisible() {
        return visible;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String playlistUrl() {
        return "https://www.youtube.com/playlist?list=" + id;
    }

    @Override
    public int compareTo(@NotNull LocalPlaylist other) {
        // A playlist with the magic id is the placeholder for not selecting any real playlist and should be sorted first
        if (this.id.equals(MAGIC_PLAYLIST_ID))
            return -1;
        else if (other.getId().equals(MAGIC_PLAYLIST_ID))
            return 1;
        else
            return this.name.compareTo(other.getName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LocalPlaylist that = (LocalPlaylist) o;
        return id.equals(that.id);
    }
}
