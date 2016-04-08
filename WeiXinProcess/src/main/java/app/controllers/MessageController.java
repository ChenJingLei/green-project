package app.controllers;

import app.models.WeiXinConfig;
import app.services.MessageService;
import me.chanjar.weixin.common.bean.WxJsapiSignature;
import me.chanjar.weixin.common.exception.WxErrorException;
import me.chanjar.weixin.common.util.StringUtils;
import me.chanjar.weixin.mp.api.*;
import me.chanjar.weixin.mp.bean.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.WxMpXmlOutMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by cjl20 on 2016/4/1.
 */

@RestController
public class MessageController {

    @Autowired
    private MessageService service;

    @RequestMapping(value = "/getWeiXinConfig", method = RequestMethod.POST)
    public WxJsapiSignature getWeiXinConfig(@RequestBody String url) {
        try {
            return service.getWxMpService().createJsapiSignature(url);
        } catch (Exception e) {
            return null;
        }
    }


    @RequestMapping(value = "/WeiXin")
    public void WeiXinGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, WxErrorException {

        String signature = request.getParameter("signature");
        String nonce = request.getParameter("nonce");
        String timestamp = request.getParameter("timestamp");

        System.out.println(signature + "//" + nonce + "///" + timestamp);

        if (!service.getWxMpService().checkSignature(timestamp, nonce, signature)) {
            // 消息签名不正确，说明不是公众平台发过来的消息
            response.getWriter().println("非法请求");
            return;
        }

        String echostr = request.getParameter("echostr");
        if (StringUtils.isNotBlank(echostr)) {
            // 说明是一个仅仅用来验证的请求，回显echostr
            response.getWriter().println(echostr);
            return;
        }

        String encryptType = StringUtils.isBlank(request.getParameter("encrypt_type")) ?
                "raw" :
                request.getParameter("encrypt_type");

        if ("raw".equals(encryptType)) {
            // 明文传输的消息
            WxMpXmlMessage inMessage = WxMpXmlMessage.fromXml(request.getInputStream());
            System.out.println(inMessage.toString());
            WxMpXmlOutMessage outMessage = service.getWxMpMessageRouter().route(inMessage);
            response.getWriter().write(outMessage.toXml());
            return;
        }

        if ("aes".equals(encryptType)) {
            // 是aes加密的消息
            String msgSignature = request.getParameter("msg_signature");
            WxMpXmlMessage inMessage = WxMpXmlMessage.fromEncryptedXml(request.getInputStream(), service.getWxMpConfigStorage(), timestamp, nonce, msgSignature);
            WxMpXmlOutMessage outMessage = service.getWxMpMessageRouter().route(inMessage);
            response.getWriter().write(outMessage.toEncryptedXml(service.getWxMpConfigStorage()));
            return;
        }
        response.getWriter().println("不可识别的加密类型");


    }
}

