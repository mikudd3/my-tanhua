package com.tanhua.sso.controller;

import com.tanhua.sso.service.SmsService;
import com.tanhua.sso.vo.ErrorResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("user")
public class SmsController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SmsController.class);

    @Autowired
    private SmsService smsService;

    /**
     * 发送验证码
     *
     * @return
     */
    @PostMapping("login")
    public ResponseEntity<Object> sendCheckCode(@RequestBody Map<String, Object> param) {
        ErrorResult.ErrorResultBuilder resultBuilder = ErrorResult.builder().errCode("000000").errMessage("发送短信验证码失败");
        try {
            String phone = String.valueOf(param.get("phone"));
            Map<String, Object> sendCheckCode = this.smsService.sendCheckCode(phone);
            int code = ((Integer) sendCheckCode.get("code")).intValue();
            if (code == 3) {
                //200
                return ResponseEntity.ok(null);
            }else if(code == 1){
                resultBuilder.errCode("000001").errMessage(sendCheckCode.get("msg").toString());
            }
        } catch (Exception e) {
            LOGGER.error("发送短信验证码失败", e);
        }

        //404
//       return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        // 500
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resultBuilder.build());
    }

}
