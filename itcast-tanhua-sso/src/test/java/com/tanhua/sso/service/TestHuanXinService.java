package com.tanhua.sso.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TestHuanXinService {

    @Autowired
    protected HuanXinService huanXinService;

    @Test
    public void testUpload(){
        this.huanXinService.register(200L);
    }
}
