package app.services;

import app.models.*;
import me.chanjar.weixin.common.api.WxConsts;
import me.chanjar.weixin.common.exception.WxErrorException;
import me.chanjar.weixin.common.session.WxSession;
import me.chanjar.weixin.common.session.WxSessionManager;
import me.chanjar.weixin.mp.api.*;
import me.chanjar.weixin.mp.bean.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.WxMpXmlOutMessage;
import me.chanjar.weixin.mp.bean.WxMpXmlOutTextMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.Map;

/**
 * Created by cjl20 on 2016/4/1.
 */
@Service
public class MessageService {

    private WxMpInMemoryConfigStorage wxMpConfigStorage;
    private WxMpService wxMpService;
    private WxMpMessageRouter wxMpMessageRouter;

    @Autowired
    private WeiXinConfig weiXinConfig;

    @PostConstruct
    public void init() {
        System.out.println("init");
        wxMpConfigStorage = new WxMpInMemoryConfigStorage();
        wxMpConfigStorage.setAppId(weiXinConfig.getAppid()); // 设置微信公众号的appid
        wxMpConfigStorage.setSecret(weiXinConfig.getAppsecret()); // 设置微信公众号的app corpSecret
        wxMpConfigStorage.setToken(weiXinConfig.getToken()); // 设置微信公众号的token
        wxMpConfigStorage.setAesKey(weiXinConfig.getEncodingaeskey()); // 设置微信公众号的EncodingAESKey

        wxMpService = new WxMpServiceImpl();
        wxMpService.setWxMpConfigStorage(wxMpConfigStorage);


        WxMpMessageHandler voiceHandle = new WxMpMessageHandler() {
            @Override
            public WxMpXmlOutMessage handle(WxMpXmlMessage wxMpXmlMessage, Map<String, Object> map, WxMpService wxMpService, WxSessionManager wxSessionManager) throws WxErrorException {
                WxMpXmlOutTextMessage message = message = WxMpXmlOutMessage.TEXT().content(wxMpXmlMessage.getRecognition())
                        .fromUser(wxMpXmlMessage.getToUserName())
                        .toUser(wxMpXmlMessage.getFromUserName())
                        .build();
                if (wxMpXmlMessage.getRecognition().contains("菜单")) {
                    message = WxMpXmlOutMessage.TEXT().content("欢迎你关注本公众号！\n1、管理员认证\n2、经销商认证")
                            .fromUser(wxMpXmlMessage.getToUserName())
                            .toUser(wxMpXmlMessage.getFromUserName())
                            .build();
                }
                return message;
            }
        };

        WxMpMessageHandler subscribeHandler = new WxMpMessageHandler() {
            @Override
            public WxMpXmlOutMessage handle(WxMpXmlMessage wxMpXmlMessage, Map<String, Object> map, WxMpService wxMpService, WxSessionManager wxSessionManager) throws WxErrorException {
                WxMpXmlOutTextMessage message
                        = WxMpXmlOutMessage.TEXT().content("欢迎你关注本公众号！\n1、管理员认证\n2、经销商认证")
                        .fromUser(wxMpXmlMessage.getToUserName())
                        .toUser(wxMpXmlMessage.getFromUserName())
                        .build();
                return message;
            }
        };

        WxMpMessageHandler restHandler = new WxMpMessageHandler() {
            @Override
            public WxMpXmlOutMessage handle(WxMpXmlMessage wxMpXmlMessage, Map<String, Object> map, WxMpService wxMpService, WxSessionManager wxSessionManager) throws WxErrorException {
                WxMpXmlOutTextMessage message
                        = WxMpXmlOutMessage.TEXT().content("未支持的消息类型")
                        .fromUser(wxMpXmlMessage.getToUserName())
                        .toUser(wxMpXmlMessage.getFromUserName())
                        .build();
//                RestTemplate restTemplate = new RestTemplate();
//
//                ManageUser manageUser = new ManageUser("陈靖磊","13004165668");
//
//                UpdateUserStatus updateUserStatus = restTemplate.postForObject("http://localhost:8090/ManageUser/addManageUser", manageUser, UpdateUserStatus.class);
//                System.out.println(updateUserStatus.toString());

                return message;
            }
        };


        wxMpMessageRouter = new WxMpMessageRouter(wxMpService);
        wxMpMessageRouter
                .rule()
                .async(false)
                .msgType(WxConsts.XML_MSG_EVENT)
                .event(WxConsts.EVT_SUBSCRIBE)
                .handler(enterhandler)
                .end()

                .rule()
                .async(false)
                .content("菜单")
                .handler(enterhandler)
                .end()

                .rule()
                .async(false)
                .content("1")
                .handler(choice1handeler)
                .end()

                .rule()
                .async(false)
                .content("2")
                .handler(choice2handeler)
                .end()
                //当用户输入姓名，电话时
                .rule()
                .async(false)
                .msgType(WxConsts.XML_MSG_TEXT)
                .rContent("^[\\u4e00-\\u9fa5A-Za-z0-9]+[，|,][0-9]+$")
                .handler(registerhandeler)
                .end()


                .rule()
                .async(false)
                .msgType(WxConsts.XML_MSG_TEXT)
                .handler(texthandeler)
                .end()
                .rule()

                .async(false)
                .msgType(WxConsts.XML_MSG_VOICE)
                .handler(voiceHandle)
                .end()

                .rule()
                .async(false)
                .handler(restHandler)
                .end();

    }

    WxMpMessageHandler registerhandeler = new WxMpMessageHandler() {
        @Override
        public WxMpXmlOutMessage handle(WxMpXmlMessage wxMpXmlMessage, Map<String, Object> map, WxMpService wxMpService, WxSessionManager wxSessionManager) throws WxErrorException {

            WxMpXmlOutTextMessage message
                    = WxMpXmlOutMessage.TEXT()
                    .fromUser(wxMpXmlMessage.getToUserName())
                    .toUser(wxMpXmlMessage.getFromUserName())
                    .build();
            WxSession session = wxSessionManager.getSession(wxMpXmlMessage.getFromUserName());
            try {
                String content = wxMpXmlMessage.getContent();
                String[] contents = null;
                if (content.contains(",")) {
                    contents = wxMpXmlMessage.getContent().split(",");
                } else {
                    contents = wxMpXmlMessage.getContent().split("，");
                }
                RestTemplate restTemplate = new RestTemplate();
                if (session.getAttribute("status").equals("choice1")) {

                    Dealer dealer = new Dealer(contents[0], contents[1]);
                    UpdateUserStatus updateUserStatus = restTemplate.postForObject("http://localhost:8080/UserManage/addDealer", dealer, UpdateUserStatus.class);
                    System.out.println(updateUserStatus.toString());
                    if (updateUserStatus.getMsgCode().equals("1")) {
                        message.setContent("登记成功\n分销商认证码：" + updateUserStatus.getResult());
                        session.invalidate();
                    } else {
                        message.setContent(updateUserStatus.getResult());
                    }
                } else if (session.getAttribute("status").equals("choice2")) {
                    ManageUser manageUser = new ManageUser(contents[0], contents[1]);
                    UpdateUserStatus updateUserStatus = restTemplate.postForObject("http://localhost:8080/UserManage/addManager", manageUser, UpdateUserStatus.class);
                    System.out.println(updateUserStatus.toString());
                    if (updateUserStatus.getMsgCode().equals("1")) {
                        message.setContent("登记成功\n管理员认证码：" + updateUserStatus.getResult());
                        session.invalidate();
                    } else {
                        message.setContent(updateUserStatus.getResult());
                    }
                }
            } catch (Exception e) {
                return menuhandle(wxMpXmlMessage, wxSessionManager);
            }
            return message;
        }
    };

    WxMpMessageHandler choice1handeler = new WxMpMessageHandler() {
        @Override
        public WxMpXmlOutMessage handle(WxMpXmlMessage wxMpXmlMessage, Map<String, Object> map, WxMpService wxMpService, WxSessionManager wxSessionManager) throws WxErrorException {
            WxMpXmlOutTextMessage tmessage
                    = WxMpXmlOutMessage.TEXT()
                    .fromUser(wxMpXmlMessage.getToUserName())
                    .toUser(wxMpXmlMessage.getFromUserName())
                    .build();
            WxSession session = wxSessionManager.getSession(wxMpXmlMessage.getFromUserName());
            try {
                if (session.getAttribute("status").equals("choice")) {
                    session.setAttribute("status", "choice1");
                    switch (Integer.valueOf(session.getAttribute("Identification").toString())) {
                        case 0:
                            tmessage.setContent("请输入认证码");
                            break;
                        case 1:
                            tmessage.setContent("请输入查询商品名称");
                            break;
                        case 2:
                            tmessage.setContent("请输入分销商姓名和手机号，中间用逗号隔开");
                            break;
                    }
                } else {
                    return menuhandle(wxMpXmlMessage, wxSessionManager);
                }
            } catch (Exception e) {
                return menuhandle(wxMpXmlMessage, wxSessionManager);
            }
            return tmessage;
        }
    };

    WxMpMessageHandler choice2handeler = new WxMpMessageHandler() {
        @Override
        public WxMpXmlOutMessage handle(WxMpXmlMessage wxMpXmlMessage, Map<String, Object> map, WxMpService wxMpService, WxSessionManager wxSessionManager) throws WxErrorException {
            WxMpXmlOutTextMessage tmessage
                    = WxMpXmlOutMessage.TEXT()
                    .fromUser(wxMpXmlMessage.getToUserName())
                    .toUser(wxMpXmlMessage.getFromUserName())
                    .build();
            WxSession session = wxSessionManager.getSession(wxMpXmlMessage.getFromUserName());
            try {
                if (session.getAttribute("status").equals("choice")) {
                    session.setAttribute("status", "choice2");
                    switch (Integer.valueOf(session.getAttribute("Identification").toString())) {
                        case 0:
                            tmessage.setContent("请输入认证码");
                            break;
                        case 1:
                            tmessage.setContent("进货管理");
                            break;
                        case 2:
                            tmessage.setContent("请输入管理员姓名和手机号，中间用逗号隔开");
                            break;
                    }
                } else {
                    return menuhandle(wxMpXmlMessage, wxSessionManager);
                }
            } catch (Exception e) {
                return menuhandle(wxMpXmlMessage, wxSessionManager);
            }
            return tmessage;
        }
    };


    WxMpMessageHandler texthandeler = new WxMpMessageHandler() {
        @Override
        public WxMpXmlOutMessage handle(WxMpXmlMessage wxMpXmlMessage, Map<String, Object> map, WxMpService wxMpService, WxSessionManager wxSessionManager) throws WxErrorException {
            System.out.println("text");
            WxMpXmlOutTextMessage tmessage
                    = WxMpXmlOutMessage.TEXT()
                    .fromUser(wxMpXmlMessage.getToUserName())
                    .toUser(wxMpXmlMessage.getFromUserName())
                    .build();
            WxSession session = wxSessionManager.getSession(wxMpXmlMessage.getFromUserName());
            try {
                if (session.getAttribute("status").equals("choice1")) {
                    RestTemplate restTemplate = new RestTemplate();
                    ManageUser manageUser = new ManageUser();
                    manageUser.setId(wxMpXmlMessage.getContent());
                    manageUser.setOpenid(wxMpXmlMessage.getFromUserName());

                    System.out.println(manageUser.toString());

                    UpdateUserStatus updateUserStatus = restTemplate.postForObject("http://localhost:8080/UserManage/addOpenIdToManager", manageUser, UpdateUserStatus.class);

                    if (updateUserStatus.getMsgCode().equals("1")) {
                        IdentifyUserStatus identifyUserStatus = IdentifyUser(wxMpXmlMessage.getFromUserName());
                        tmessage.setContent("欢迎您，" + identifyUserStatus.getResult().getName() + "，登记成功\n输入菜单选择需要的功能");
                        session.invalidate();
                    } else {
                        tmessage.setContent(updateUserStatus.getResult());
                    }

                } else if (session.getAttribute("status").equals("choice2")) {
                    RestTemplate restTemplate = new RestTemplate();
                    Dealer dealer = new Dealer();
                    dealer.setId(wxMpXmlMessage.getContent());
                    dealer.setOpenid(wxMpXmlMessage.getFromUserName());

                    UpdateUserStatus updateUserStatus = restTemplate.postForObject("http://localhost:8080/UserManage/addOpenIdToDealer", dealer, UpdateUserStatus.class);

                    if (updateUserStatus.getMsgCode().equals("1")) {
                        IdentifyUserStatus identifyUserStatus = IdentifyUser(wxMpXmlMessage.getFromUserName());
                        tmessage.setContent("欢迎您，" + identifyUserStatus.getResult().getName() + "，登记成功\n" +
                                "输入菜单选择需要的功能");
                        session.invalidate();
                    } else {
                        tmessage.setContent(updateUserStatus.getResult());
                    }
                }
            } catch (Exception e) {
                return menuhandle(wxMpXmlMessage, wxSessionManager);
            }
            return tmessage;
        }
    };

    WxMpMessageHandler enterhandler = new WxMpMessageHandler() {
        @Override
        public WxMpXmlOutMessage handle(WxMpXmlMessage wxMpXmlMessage, Map<String, Object> map, WxMpService wxMpService, WxSessionManager wxSessionManager) throws WxErrorException {

            return menuhandle(wxMpXmlMessage, wxSessionManager);
        }
    };

    public WxMpXmlOutMessage menuhandle(WxMpXmlMessage wxMpXmlMessage, WxSessionManager wxSessionManager) throws WxErrorException {

        //识别用户身份
        RestTemplate restTemplate = new RestTemplate();
        IdentifyUserStatus identifyUserStatus = restTemplate.getForObject("http://localhost:8090/User/IdentifyUser?OpenId=" + wxMpXmlMessage.getFromUserName(), IdentifyUserStatus.class);

        WxMpXmlOutTextMessage emessage
                = WxMpXmlOutMessage.TEXT()
                .fromUser(wxMpXmlMessage.getToUserName())
                .toUser(wxMpXmlMessage.getFromUserName())
                .build();
        if (identifyUserStatus.getMsgCode().equals("1")) {

            if (identifyUserStatus.getResult().getIdentification() == 1) {
                emessage.setContent("1、查询商品信息\n2、进货管理");
            } else if (identifyUserStatus.getResult().getIdentification() == 2) {
                emessage.setContent("1、增加分销商\n2、增加管理员\n3、货物入库\n4、仓库信息修改\n5、查询订单\n6、确认订单\n7、查询商品信息");
            }
        } else if (identifyUserStatus.getMsgCode().equals("0")) {
            emessage.setContent("欢迎你关注本公众号！\n" +
                    "1、管理员认证\n" +
                    "2、经销商认证");
        } else {
            emessage.setContent("未知错误请稍后重试！！！");
        }
        WxSession session = wxSessionManager.getSession(wxMpXmlMessage.getFromUserName());
        session.setAttribute("status", "choice");
        session.setAttribute("Identification", identifyUserStatus.getResult().getIdentification());
        System.out.println(identifyUserStatus.getResult().toString());
        return emessage;
    }

    public IdentifyUserStatus IdentifyUser(String OpenId) {
        //识别用户身份
        RestTemplate restTemplate = new RestTemplate();
        IdentifyUserStatus identifyUserStatus = restTemplate.getForObject("http://localhost:8090/User/IdentifyUser?OpenId=" + OpenId, IdentifyUserStatus.class);
        return identifyUserStatus;
    }


    public WxMpInMemoryConfigStorage getWxMpConfigStorage() {
        return wxMpConfigStorage;
    }

    public WxMpService getWxMpService() {
        return wxMpService;
    }

    public WxMpMessageRouter getWxMpMessageRouter() {
        return wxMpMessageRouter;
    }

}

