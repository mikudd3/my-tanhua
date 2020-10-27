package com.tanhua.sso.service;

import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tanhua.sso.config.AliyunSMSConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class SmsService {

    @Autowired
    private RestTemplate restTemplate;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private AliyunSMSConfig aliyunSMSConfig;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 发送验证码
     *
     * @param mobile
     * @return
     */
    public Map<String, Object> sendCheckCode(String mobile) {
        Map<String, Object> result = new HashMap<>(2);
        try {
            String redisKey = "CHECK_CODE_" + mobile;
            String value = this.redisTemplate.opsForValue().get(redisKey);
            if (StringUtils.isNotEmpty(value)) {
                result.put("code", 1);
                result.put("msg", "上一次发送的验证码还未失效");
                return result;
            }
            String code = "123456"; //使用固定验证码
//            String code = this.sendSmsAliyun(mobile);
            if (null == code) {
                result.put("code", 2);
                result.put("msg", "发送短信验证码失败");
                return result;
            }

            //发送验证码成功
            result.put("code", 3);
            result.put("msg", "ok");

            //将验证码存储到Redis,2分钟后失效
            this.redisTemplate.opsForValue().set(redisKey, code, Duration.ofMinutes(2));

            return result;
        } catch (Exception e) {

            log.error("发送验证码出错！" + mobile, e);

            result.put("code", 4);
            result.put("msg", "发送验证码出现异常");
            return result;
        }

    }


    /**
     * 发送验证码短信
     *
     * @param mobile
     */
    public String sendSms(String mobile) {
        String url = "https://open.ucpaas.com/ol/sms/sendsms";
        Map<String, Object> params = new HashMap<>();
        params.put("sid", "*******");
        params.put("token", "*******");
        params.put("appid", "*******");
        params.put("templateid", "*****");
        params.put("mobile", mobile);
        // 生成4位数验证
        params.put("param", RandomUtils.nextInt(100000, 999999));
        ResponseEntity<String> responseEntity = this.restTemplate.postForEntity(url, params, String.class);

        String body = responseEntity.getBody();

        try {
            JsonNode jsonNode = MAPPER.readTree(body);
            //000000 表示发送成功
            if (StringUtils.equals(jsonNode.get("code").textValue(), "000000")) {
                return String.valueOf(params.get("param"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;

    }

    /**
     * 通过阿里云发送验证码短信
     *
     * @param mobile
     */
    public String sendSmsAliyun(String mobile) {
        DefaultProfile profile = DefaultProfile.getProfile(
                this.aliyunSMSConfig.getRegionId(),
                this.aliyunSMSConfig.getAccessKeyId(),
                this.aliyunSMSConfig.getAccessKeySecret());
        IAcsClient client = new DefaultAcsClient(profile);

        String code = RandomUtils.nextInt(100000, 999999) + "";

        CommonRequest request = new CommonRequest();
        request.setSysMethod(MethodType.POST);
        request.setSysDomain(this.aliyunSMSConfig.getDomain());
        request.setSysVersion("2017-05-25");
        request.setSysAction("SendSms");
        request.putQueryParameter("RegionId", this.aliyunSMSConfig.getRegionId());
        request.putQueryParameter("PhoneNumbers", mobile);
        request.putQueryParameter("SignName", this.aliyunSMSConfig.getSignName());
        request.putQueryParameter("TemplateCode", this.aliyunSMSConfig.getTemplateCode());
        request.putQueryParameter("TemplateParam", "{\"code\":\"" + code + "\"}");
        try {
            CommonResponse response = client.getCommonResponse(request);
            if (StringUtils.contains(response.getData(), "\"Code\":\"OK\"")) {
                return code;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
