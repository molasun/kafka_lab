package org.acme.song.app;

import java.io.Serializable;

public class Song implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer songId;
    private String songName;
    private Integer songPrice;

    public Integer getSongId() {
        return songId;
    }

    public String getSongName() {
        return songName;
    }

    public void setSongName(String songName) {
        this.songName = songName;
    }

    public Integer getSongPrice() {
        return songPrice;
    }

    public void setSongPrice(Integer songPrice) {
        this.songPrice = songPrice;
    }

}
