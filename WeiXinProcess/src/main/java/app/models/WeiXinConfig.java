package app.models;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Created by cjl20 on 2016/4/1.
 */

@Configuration
@ConfigurationProperties(prefix = "weixin")
public class WeiXinConfig {

    private String appid;
    private String appsecret;
    private String token;
    private String encodingaeskey;

    public String getAppid() {
        return appid;
    }

    public void setAppid(String appid) {
        this.appid = appid;
    }

    public String getAppsecret() {
        return appsecret;
    }

    public void setAppsecret(String appsecret) {
        this.appsecret = appsecret;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getEncodingaeskey() {
        return encodingaeskey;
    }

    public void setEncodingaeskey(String encodingaeskey) {
        this.encodingaeskey = encodingaeskey;
    }

    @Override
    public String toString() {
        return "WeiXinConfig{" +
                "appid='" + appid + '\'' +
                ", appsecret='" + appsecret + '\'' +
                ", token='" + token + '\'' +
                ", encodingaeskey='" + encodingaeskey + '\'' +
                '}';
    }
}

