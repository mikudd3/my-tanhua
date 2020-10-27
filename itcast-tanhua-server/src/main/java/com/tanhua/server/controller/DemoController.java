package com.tanhua.server.controller;

import com.tanhua.server.utils.NoAuthorization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("demo")
public class DemoController {

    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    @GetMapping("QA")
    @NoAuthorization
    public Object queryA(int type){

        //从缓存中命中
        String key = "KEY_" + type +"_{UID}";
        String data = this.redisTemplate.opsForValue().get(key);
        if(null != data){
            return data;
        }

        //查询Service，获取数据
        Map<String,Object> map = new HashMap<>();
        map.put("type", type);

        //写入到缓存中
        this.redisTemplate.opsForValue().set(key, map.toString() );

        return map;
    }

    @GetMapping("QB")
    public Object queryB(int type, int a){

        //从缓存中命中
        String key = "KEY_" + type +"_{UID}_" + a;
        String data = this.redisTemplate.opsForValue().get(key);
        if(null != data){
            return data;
        }

        //查询Service，获取数据
        Map<String,Object> map = new HashMap<>();
        map.put("type", type);
        map.put("a", a);

        //写入到缓存中
        this.redisTemplate.opsForValue().set(key, map.toString() );

        return map;
    }

    @GetMapping("QC")
    public Object queryC(int type, int a, String b){

        //从缓存中命中
        String key = "KEY_" + type +"_{UID}_"+a+"_" + b;
        String data = this.redisTemplate.opsForValue().get(key);
        if(null != data){
            return data;
        }

        //查询Service，获取数据
        Map<String,Object> map = new HashMap<>();
        map.put("type", type);
        map.put("a", a);
        map.put("b", b);

        //写入到缓存中
        this.redisTemplate.opsForValue().set(key, map.toString() );

        return map;
    }

    @PostMapping("S1")
    public void save1(Object obj){
        //save(obj)
    }


}
