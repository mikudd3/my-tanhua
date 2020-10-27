package com.tanhua.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tanhua.common.pojo.User;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class UserService {

    @Autowired
    private RestTemplate restTemplate;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Value("${tanhua.sso.url}")
    private String url;

    public User queryUserByToken(String token) {
        String data = this.restTemplate.getForObject(url + "/user/{token}", String.class, token);
        if (StringUtils.isNotEmpty(data)) {
            try {
                return MAPPER.readValue(data, User.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
