package com.tanhua.sso.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tanhua.common.vo.HuanXinUser;
import com.tanhua.sso.config.HuanXinConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class HuanXinService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private HuanXinTokenService huanXinTokenService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private HuanXinConfig huanXinConfig;

    private int reTryCount = 0;

    /**
     * 注册环信用户
     *
     * @param userId
     * @return
     */
    public boolean register(Long userId) {
        String targetUrl = this.huanXinConfig.getUrl()
                + this.huanXinConfig.getOrgName() + "/"
                + this.huanXinConfig.getAppName() + "/users";

        try {
            // 请求体
            HuanXinUser huanXinUser = new HuanXinUser(String.valueOf(userId), DigestUtils.md5Hex(userId + "_itcast_tanhua"));
            String body = MAPPER.writeValueAsString(Arrays.asList(huanXinUser));
            return this.execute(targetUrl, body);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 注册失败
        return false;

    }

    /**
     * 添加好友
     *
     * @param userId
     * @param friendId
     * @return
     */
    public boolean contactUsers(Long userId, Long friendId) {
        String targetUrl = this.huanXinConfig.getUrl()
                + this.huanXinConfig.getOrgName() + "/"
                + this.huanXinConfig.getAppName() + "/users/" +
                userId + "/contacts/users/" + friendId;
        try {
            // 404 -> 对方未在环信注册
            return this.execute(targetUrl, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 添加失败
        return false;
    }

    private boolean execute(String url, String body) {
        try {
            String token = this.huanXinTokenService.getToken();
            // 请求头
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Type", "application/json");
            headers.add("Authorization", "Bearer " + token);

            HttpEntity<String> httpEntity = new HttpEntity<>(body, headers);

            ResponseEntity<String> responseEntity = null;
            try {
                responseEntity = this.restTemplate.postForEntity(url, httpEntity, String.class);
            } catch (HttpClientErrorException.Unauthorized e) {
                //401 token失效，重新刷新token，重试3次
                if(reTryCount >= 3){
                    throw new RuntimeException("重试3次获取token依然无效，请检查！！！", e);
                }

                //休息一下再试
                Thread.sleep(reTryCount * 100);

                this.huanXinTokenService.refreshToken();
                reTryCount++;

                //递归调用
                this.execute(url, body);

                log.error("token失效，重新刷新", e);
            } catch (HttpClientErrorException.NotFound e){
                log.error("用户数据在环信不存在~ url = " + url, e);
                return false;
            }

            reTryCount = 0;
            return responseEntity.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("请求环信出错~", e);
        }
        reTryCount = 0;
        return false;
    }

    public boolean sendMsg(String target, String type, String msg) {
        String targetUrl = this.huanXinConfig.getUrl()
                + this.huanXinConfig.getOrgName() + "/"
                + this.huanXinConfig.getAppName() + "/messages";
        try {
            String token = this.huanXinTokenService.getToken();
            // 请求头
            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", "Bearer " + token);

            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put("target_type", "users");
            paramMap.put("target", Arrays.asList(target));

            Map<String, Object> msgMap = new HashMap<>();
            msgMap.put("type", type);
            msgMap.put("msg", msg);

            paramMap.put("msg", msgMap);

            //表示消息发送者;无此字段Server会默认设置为“from”:“admin”，有from字段但值为空串(“”)时请求失败
//            msgMap.put("from", type);

            String body = MAPPER.writeValueAsString(paramMap);
            return this.execute(targetUrl, body);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

}
