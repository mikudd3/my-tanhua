package com.tanhua.server.controller;

import com.tanhua.server.service.UsersService;
import com.tanhua.server.vo.UserInfoVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("users")
public class UsersController {

    @Autowired
    private UsersService usersService;

    /**
     * 用户资料 - 读取
     *
     * @param userID
     * @param huanxinID
     * @return
     */
    @GetMapping
    public ResponseEntity<UserInfoVo> queryUserInfo(@RequestParam(value = "userID", required = false) String userID,
                                                    @RequestParam(value = "huanxinID", required = false) String huanxinID) {
        try {
            //用户第一次 接收到 环信推来的信息 ，不带用户信息
            UserInfoVo userInfoVo = this.usersService.queryUserInfo(userID, huanxinID);
            if (null != userInfoVo) {
                return ResponseEntity.ok(userInfoVo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

}
