package com.tanhua.dubbo.server.api;

import com.tanhua.dubbo.server.pojo.Publish;
import com.tanhua.dubbo.server.pojo.TimeLine;
import com.tanhua.dubbo.server.vo.PageInfo;
import org.bson.types.ObjectId;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TestQuanziApi {

    @Autowired
    private QuanZiApi quanZiApi;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Test
    public void testSavePublish(){
        Publish publish = new Publish();
        publish.setUserId(1L);
        publish.setLocationName("上海市");
        publish.setSeeType(1);
        publish.setText("今天天气不错~");
        publish.setMedias(Arrays.asList("https://itcast-tanhua.oss-cn-shanghai.aliyuncs.com/images/quanzi/1.jpg"));
        String result = this.quanZiApi.savePublish(publish);
        System.out.println(result);
    }

    @Test
    public void testQuery(){
        PageInfo<Publish> pageInfo = this.quanZiApi.queryPublishList(1L, 1, 10);
        System.out.println(pageInfo);
    }

    @Test
    public void testRecommendPublish(){
        //查询用户id为1的动态作为推荐动态的数据
        PageInfo<Publish> pageInfo = this.quanZiApi.queryPublishList(2L, 1, 10);
        for (Publish record : pageInfo.getRecords()) {

            TimeLine timeLine = new TimeLine();
            timeLine.setId(ObjectId.get());
            timeLine.setPublishId(record.getId());
            timeLine.setUserId(record.getUserId());
            timeLine.setDate(System.currentTimeMillis());

            this.mongoTemplate.save(timeLine, "quanzi_time_line_recommend");
        }
    }

    @Test
    public void queryPublishList(){
        //查询用户id为1的推荐数据
        PageInfo<Publish> pageInfo = this.quanZiApi.queryPublishList(null, 1, 10);
        System.out.println(pageInfo);
    }
}
