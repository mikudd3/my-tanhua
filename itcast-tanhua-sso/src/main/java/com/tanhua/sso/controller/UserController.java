package com.tanhua.sso.controller;

import com.tanhua.common.pojo.User;
import com.tanhua.sso.service.UserService;
import com.tanhua.sso.vo.ErrorResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("user")
public class UserController {

    @Autowired
    private UserService userService;


    /**
     * 登录
     *
     * @return
     */
    @PostMapping("loginVerification")
    public ResponseEntity<Object> login(@RequestBody Map<String, Object> param) {
        try {
            String mobile = param.get("phone").toString();
            String code = param.get("verificationCode").toString();
            String token = this.userService.login(mobile, code);

            Map<String, Object> result = new HashMap<>(2);

            if (StringUtils.isNotEmpty(token)) {
                String[] ss = StringUtils.split(token, '|');
                String isNew = ss[0];
                String tokenStr = ss[1];

                result.put("isNew", Boolean.valueOf(isNew));//这里必须转成boolean类型
                result.put("token", tokenStr);
                return ResponseEntity.ok(result);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 登录失败，验证码错误
        ErrorResult errorResult = ErrorResult.builder().errCode("000000").errMessage("验证码错误").build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
    }

    /**
     * 根据token查询用户数据
     *
     * @param token
     * @return
     */
    @GetMapping("{token}")
    public User queryUserByToken(@PathVariable("token") String token) {
        return this.userService.queryUserByToken(token);
    }
    
}
