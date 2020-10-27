package com.tanhua.sso.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tanhua.common.pojo.User;
import com.tanhua.sso.mapper.UserMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Value("${jwt.secret}")
    private String secret;

    public String login(String mobile, String code) {

        Boolean isNew = false; //是否为新注册

        //校验验证码
        String redisKey = "CHECK_CODE_" + mobile;
        String value = this.redisTemplate.opsForValue().get(redisKey);
        //判断是否为空
        if (!StringUtils.equals(value, code)) {
            return null; //验证码错误
        }

        //验证码正确，进行登录操作
        this.redisTemplate.delete(redisKey);

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("mobile", mobile);
        User selectUser = this.userMapper.selectOne(queryWrapper);
        if (selectUser == null) {
            // 该手机号未注册，进行注册操作
            User user = new User();
            user.setMobile(mobile);
            user.setPassword(DigestUtils.md5Hex(secret + "_123456"));// 默认密码
            this.userMapper.insert(user); //插入数据完成，id已经有了值了
            selectUser = user;
            isNew = true;
        }

        Map<String, Object> claims = new HashMap<String, Object>();
        claims.put("mobile", mobile);
        claims.put("id", selectUser.getId());

        // 生成token
        String token = Jwts.builder()
                .setClaims(claims) //设置响应数据体
                .signWith(SignatureAlgorithm.HS256, secret) //设置加密方法和加密盐
                .compact();

        //将用户数据写入到redis中
        String redisTokenKey = "TOKEN_" + token;
        try {
            this.redisTemplate.opsForValue().set(redisTokenKey, MAPPER.writeValueAsString(selectUser), Duration.ofHours(1));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        try {
            // 发送登录成功的消息
            Map<String, Object> msg = new HashMap<>();
            msg.put("userId", selectUser.getId());
            msg.put("date", new Date());
            this.rocketMQTemplate.convertAndSend("tanhua-sso-login", msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isNew + "|" + token;
    }

    public User queryUserByToken(String token) {
        try {
            String redisTokenKey = "TOKEN_" + token;
            String cacheData = this.redisTemplate.opsForValue().get(redisTokenKey);
            if (StringUtils.isEmpty(cacheData)) {
                return null;
            }
            // 刷新时间
            this.redisTemplate.expire(redisTokenKey, 1, TimeUnit.HOURS);
            return MAPPER.readValue(cacheData, User.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
