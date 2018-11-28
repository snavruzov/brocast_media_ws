package com.brocast.api.ws.beans;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by Sardor Navruzov on 7/2/15.
 * Copyrights Digitizen Co.
 */
@XmlRootElement
public class InMessage {
    public String time;
    public String idHash;
    public String ratingType;
    public String colorType;
    public int rotate = 0;
    public String text;
    public Long idMedia;
    public int wsType;
    public int commentType=0; //0 - common, 1-voice
    public long duration=0;
    public long playerDuration=0;

    public InMessage() {
    }

    public InMessage(String time, String idHash, String ratingType, String colorType, int rotate, String text, Long idMedia, int wsType, int commentType, long duration, long playerDuration) {
        this.time = time;
        this.idHash = idHash;
        this.ratingType = ratingType;
        this.colorType = colorType;
        this.rotate = rotate;
        this.text = text;
        this.idMedia = idMedia;
        this.wsType = wsType;
        this.commentType = commentType;
        this.duration = duration;
        this.playerDuration = playerDuration;
    }
}

