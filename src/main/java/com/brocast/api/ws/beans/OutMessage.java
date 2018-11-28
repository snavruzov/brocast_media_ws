package com.brocast.api.ws.beans;

import com.fasterxml.jackson.annotation.JsonInclude;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * BroCast.
 * Copyright: Sardor Navruzov
 * 2013-2017.
 */
@XmlRootElement
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OutMessage {

    public String avatar;
    public Long idMedia;
    public String dateadded;
    public Long idUser;
    public String username;
    public String text;
    public String tkey;
    public Integer type;
    public Integer commentType;
    public Integer vPermit;
    public String url;
    public Long duration;
    public Boolean blocked;
    public Long lcnt;
    public Long vcnt;
    public String total;
    public Boolean liveEnded;
    public String idComment;
    public String content;
    public Boolean liveDeleted;
    public Integer rotate;
    public String ratingType;
    public String colorType;
    public String time;
    public String rtmp_url;
    public String position;

    public OutMessage() {
    }

    public OutMessage(String avatar, Long idMedia, String dateadded, Long idUser, String username, String text, String tkey, Integer type, Integer commentType, Integer vPermit, String url, Long duration, Boolean blocked, Long lcnt, Long vcnt, String total, Boolean liveEnded, String idComment, String content, Boolean liveDeleted, Integer rotate, String ratingType, String colorType, String time, String rtmp_url, String position) {
        this.avatar = avatar;
        this.idMedia = idMedia;
        this.dateadded = dateadded;
        this.idUser = idUser;
        this.username = username;
        this.text = text;
        this.tkey = tkey;
        this.type = type;
        this.commentType = commentType;
        this.vPermit = vPermit;
        this.url = url;
        this.duration = duration;
        this.blocked = blocked;
        this.lcnt = lcnt;
        this.vcnt = vcnt;
        this.total = total;
        this.liveEnded = liveEnded;
        this.idComment = idComment;
        this.content = content;
        this.liveDeleted = liveDeleted;
        this.rotate = rotate;
        this.ratingType = ratingType;
        this.colorType = colorType;
        this.time = time;
        this.rtmp_url = rtmp_url;
        this.position = position;
    }
}
