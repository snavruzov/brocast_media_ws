package com.brocast.api.ws.config.protocol;

import com.brocast.api.ws.beans.InMessage;
import com.brocast.api.ws.beans.OutMessage;
import com.brocast.riak.api.beans.DcMediaEntity;
import com.brocast.riak.api.beans.DcUsersEntity;
import com.brocast.riak.api.dao.RiakAPI;
import com.brocast.riak.api.dao.RiakTP;
import com.brocast.riak.api.factory.RiakQueryFactory;
import com.dgtz.db.api.beans.MediaMappingStatInfo;
import com.dgtz.mcache.api.factory.Constants;
import com.dgtz.mcache.api.factory.RMemoryAPI;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.GsonBuilder;
import org.atmosphere.config.managed.Decoder;
import org.atmosphere.config.managed.Encoder;
import org.atmosphere.config.service.Disconnect;
import org.atmosphere.config.service.ManagedService;
import org.atmosphere.config.service.PathParam;
import org.atmosphere.config.service.Ready;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

/**
 * BroCast.
 * Copyright: Sardor Navruzov
 * 2013-2017.
 */

@ManagedService(path = "/media/{idmedia}")
public class ChatService {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ChatService.class);

    @PathParam("idmedia")
    private String idMedia;

    @Ready
    public void onReady(final AtmosphereResource resource) {
        logger.info("Connected IDMEDIA {}, UUID {}", idMedia, resource.uuid());
        RMemoryAPI.getInstance()
                .pushSetElemToMemory(Constants.MEDIA_KEY + "ws:sessions:" + idMedia, resource.uuid());
        OutMessage om = new OutMessage();
        liveIncrementing(idMedia, om);
        String js = new GsonBuilder().create().toJson(om);
        logger.info("Enter values {}", js);
        resource.getAtmosphereResourceEvent().broadcaster().broadcast(js);
    }

    @Disconnect
    public void onDisconnect(AtmosphereResourceEvent event) {
        logger.info("Client {} disconnected [{}]", event.getResource().uuid(),
                (event.isCancelled() ? "cancelled" : "closed"));
        RMemoryAPI.getInstance()
                .delFromSetElem(Constants.MEDIA_KEY + "ws:sessions:" + idMedia, event.getResource().uuid());

        OutMessage om = new OutMessage();
        liveIncrementing(idMedia, om);
        String js = new GsonBuilder().create().toJson(om);
        logger.info("Exit values {}", js);
        event.broadcaster().broadcast(js);
    }

    @org.atmosphere.config.service.Message(encoders = JacksonEncoderDecoder.class, decoders = JacksonEncoderDecoder.class)
    public OutMessage onMessage(InMessage m) throws IOException {
        OutMessage om = new OutMessage();

        try {
            logger.info("Hash value {}", m.idHash);
            DcUsersEntity e = getUserInfoByHash(m.idHash);
            if (e != null && e.getIdUser() != 0L) {

                String dateadded = System.currentTimeMillis() + "";
                switch (m.wsType) {
                    case 0: {
                        String mediaOwnerId = RMemoryAPI.getInstance().pullHashFromMemory(Constants.MEDIA_KEY + m.idMedia, "id_user");
                        boolean blocked = RMemoryAPI.getInstance().pullIfSetElem("dc_users:comment:blocked:users:" + mediaOwnerId, e.getIdUser() + "");

                        if (!blocked) {
                            String msg = new String(m.text.getBytes("UTF-8"));
                            String vurl = "";
                            if (m.commentType == 1) {
                                msg = "it's a voice message, please update BroCast!";
                                vurl = RMemoryAPI.getInstance().pullElemFromMemory(Constants.COMMENT_KEY + "voice-url:" + e.getIdUser() + m.idMedia);
                                vurl = Constants.STATIC_URL + vurl;
                            }
                            boolean vc = RMemoryAPI.getInstance().pullIfSetElem("dc_users:comment:voice:users:" + mediaOwnerId, e.getIdUser() + "");
                            om.avatar = e.getAvatar();
                            om.idMedia = m.idMedia;
                            om.dateadded = dateadded;
                            om.idUser = e.getIdUser();
                            om.username = e.getUsername();
                            om.text = msg;
                            om.tkey = m.time;// moment of live duration be sent comment
                            om.type = 0;
                            om.commentType = m.commentType;
                            om.vPermit = (vc || Objects.equals(Long.valueOf(mediaOwnerId), e.getIdUser())) ? 1 : 0;
                            om.url = vurl;
                            om.duration = m.duration;

                        } else {
                            om.blocked = true;
                            om.idUser = e.getIdUser();
                            om.idMedia = m.idMedia;
                            om.type = 5; //blocked user
                        }
                        break;
                    }
                    case 1: {

                        om.lcnt = 0l;
                        String likes = RMemoryAPI.getInstance().pullHashFromMemory("dc_media:" + m.idMedia, "liked");
                        if (likes != null && !likes.isEmpty()) {
                            om.lcnt = Long.valueOf(likes);
                        }
                        om.type = 1;
                        break;

                    }
                    case 2: {
                        liveIncrementing(m.idMedia+"", om);
                        break;
                    }
                    case 3: {
                        om.idMedia = m.idMedia;
                        om.liveEnded = true;
                        om.type = 3;
                        break;
                    }
                    case 5: {
                        om.blocked = true;
                        om.idUser = Long.valueOf(m.text); //text == blockedIdUser
                        om.idMedia = m.idMedia;
                        om.type = 5; //blocked user
                        break;
                    }
                    case 6: {
                        String mediaOwnerId = RMemoryAPI.getInstance().pullHashFromMemory(Constants.MEDIA_KEY + m.idMedia, "id_user");
                        boolean vc = RMemoryAPI.getInstance().pullIfSetElem("dc_users:comment:voice:users:" + mediaOwnerId, m.text + "");

                        om.blocked = !vc;
                        om.idUser = Long.valueOf(m.text); //text == blockedVoiceIdUser
                        om.idMedia = m.idMedia;
                        om.type = 6; //blocked voice user
                        break;
                    }
                    case 7: {
                        om.idUser = e.getIdUser();
                        om.idMedia = m.idMedia;
                        om.idComment = m.text; //text == idComment
                        om.type = 7; // removed comment
                        break;
                    }
                    case 8: {
                        om.idUser = e.getIdUser();
                        om.avatar = e.getAvatar();
                        om.username = e.getUsername();
                        om.type = 8; // joined user
                        break;
                    }
                    case 9: {
                        DcMediaEntity mediaEntity = RMemoryAPI.getInstance().pullHashFromMemory(Constants.MEDIA_KEY + m.idMedia, "detail", DcMediaEntity.class);
                        if (mediaEntity != null && mediaEntity.getProgress() == 0) {
                            MediaMappingStatInfo info = new MediaMappingStatInfo();
                            String username = RMemoryAPI.getInstance().pullHashFromMemory(Constants.USER_KEY + mediaEntity.getIdUser(), "username");
                            String thumb = Constants.encryptAmazonURL(mediaEntity.getIdUser(), mediaEntity.getIdMedia(), "jpg", "thumb", Constants.STATIC_URL);
                            String thumb_webp = Constants.encryptAmazonURL(mediaEntity.getIdUser(), mediaEntity.getIdMedia(), "webp", "thumb", Constants.STATIC_URL);
                            String avatar = RMemoryAPI.getInstance().pullHashFromMemory(Constants.USER_KEY + mediaEntity.getIdUser(), "avatar");
                            String vcnt = RMemoryAPI.getInstance()
                                    .pullHashFromMemory(Constants.MEDIA_KEY + mediaEntity.idMedia, "vcount");
                            String lcnt = RMemoryAPI.getInstance()
                                    .pullHashFromMemory(Constants.MEDIA_KEY + mediaEntity.idMedia, "liked");
                            String evnt_time = RMemoryAPI.getInstance()
                                    .pullHashFromMemory(Constants.MEDIA_KEY + mediaEntity.idMedia, "evnt_time");

                            info.setThumb(thumb);
                            info.setThumb_webp(thumb_webp);
                            info.setAvatar(Constants.STATIC_URL + mediaEntity.getIdUser() + "/image" + avatar + "M.jpg");
                            info.setUsername(username);
                            info.setLocation(mediaEntity.location);
                            info.setCurrentTime(String.valueOf(System.currentTimeMillis()));
                            info.setLatLng(mediaEntity.coordinate);
                            info.setIdMedia(mediaEntity.idMedia);
                            info.setTitle(mediaEntity.title);
                            info.setDateadded(mediaEntity.dateadded);
                            info.setDuration(mediaEntity.duration);
                            info.setMethod(mediaEntity.method);
                            info.setAmount(Long.valueOf(vcnt));
                            info.setLiked(Long.valueOf(lcnt));
                            info.setStart_time(evnt_time);

                            om.content = info.toString();
                            om.type = 9; // Live started
                        }
                        break;
                    }
                    case 10: {
                        om.liveDeleted = true;
                        om.type = 10;
                        break;
                    }
                    case 11: {
                        om.idMedia = m.idMedia;
                        om.rotate = m.rotate;
                        om.time = m.time;
                        om.type = 11; // user screen rotated
                        break;
                    }
                    case 12: {
                        om.idMedia = m.idMedia;
                        om.ratingType = m.ratingType;
                        om.colorType = m.colorType;
                        om.time = m.time;
                        om.type = 12; // rating hearts
                        break;
                    }
                    case 13: {
                        String username = RMemoryAPI.getInstance()
                                .pullHashFromMemory(Constants.USER_KEY + e.getIdUser(), "username");
                        String rtmp_url = RMemoryAPI.getInstance()
                                .pullHashFromMemory(Constants.LIVE_KEY + m.idMedia, "rtmp_liveurl");
                        String idHoster = RMemoryAPI.getInstance()
                                .pullHashFromMemory(Constants.MEDIA_KEY + m.idMedia, "debate.author");
                        String pose = RMemoryAPI.getInstance()
                                .pullHashFromMemory(Constants.MEDIA_KEY + idHoster, "debate.position");
                        logger.info("position {}", pose);

                        om.idUser = e.getIdUser();
                        om.username = username;
                        om.rtmp_url = rtmp_url;
                        om.avatar = e.getAvatar();
                        om.idMedia = m.idMedia;
                        om.position = pose;

                        om.type = 13; // debate started
                        break;
                    }
                    case 14: {
                        om.idUser = e.getIdUser();
                        om.type = 14; // debate canceled
                        break;
                    }
                    case 15: {
                        om.idMedia = m.idMedia;
                        om.idUser = e.getIdUser();
                        om.type = 15; // debate stoped
                        break;
                    }
                    case 16: {
                        om.idMedia = m.idMedia;
                        om.rotate = m.rotate;
                        om.time = m.time;
                        om.type = 16; // debater screen rotated
                        break;
                    }
                    case 17: {
                        om.idUser = e.getIdUser();
                        om.idMedia = m.idMedia; //id of live
                        om.type = 17; // live connection lost
                        break;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error in WS", e);
        }

        return om;
    }

    public static class JacksonEncoderDecoder
            implements Encoder<OutMessage, String>, Decoder<String, InMessage> {

        private final ObjectMapper mapper = new ObjectMapper();

        @Override
        public String encode(OutMessage m) {
            try {
                return this.mapper.writeValueAsString(m);
            }
            catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
        }

        @Override
        public InMessage decode(String s) {
            try {
                return this.mapper.readValue(s, InMessage.class);
            }
            catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
        }

    }

    protected void liveIncrementing(String idMedia, OutMessage json) {
        long vcnt = RMemoryAPI.getInstance().checkSetElemCount(Constants.MEDIA_KEY + "ws:sessions:" + idMedia);
        json.vcnt = vcnt>0?vcnt-1:0;
        json.total = RMemoryAPI.getInstance().pullHashFromMemory(Constants.MEDIA_KEY + idMedia, "vcount");
        json.type = 2;
    }

    protected DcUsersEntity getUserInfoByHash(String hash) {
        DcUsersEntity entity = null;

        try {
            String idUser = (String) RMemoryAPI.getInstance().pullElemFromMemory(Constants.USER_HASH + hash, String.class);

            if (idUser != null) {
                RiakTP transport = RiakAPI.getInstance();
                RiakQueryFactory queryFactory = new RiakQueryFactory(transport);
                entity = queryFactory.queryUserDataByIDUser(Long.valueOf(idUser));

                if (entity != null) {
                    String avatar = RMemoryAPI.getInstance().pullHashFromMemory(Constants.USER_KEY + idUser, "avatar");
                    String wallpic = RMemoryAPI.getInstance().pullHashFromMemory(Constants.USER_KEY + idUser, "wallpic");
                    entity.avatar = (Constants.STATIC_URL + entity.idUser + "/image" + avatar + "S.jpg");
                    entity.wallpic = (Constants.STATIC_URL + entity.idUser + "/image" + wallpic + ".jpg");
                }
            }

        } catch (Exception e) {
            logger.error("ERROR IN DB API ", e);
        }

        return entity;
    }

}
