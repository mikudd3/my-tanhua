package com.tanhua.recommend.msg;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tanhua.dubbo.server.pojo.Publish;
import com.tanhua.recommend.pojo.RecommendQuanZi;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
@RocketMQMessageListener(topic = "tanhua-quanzi",
        consumerGroup = "tanhua-quanzi-consumer")
public class QuanZiMsgConsumer implements RocketMQListener<String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Logger LOGGER = LoggerFactory.getLogger(QuanZiMsgConsumer.class);

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public void onMessage(String msg) {
        try {
            JsonNode jsonNode = MAPPER.readTree(msg);

            Long userId = jsonNode.get("userId").asLong();
            Long pid = jsonNode.get("pid").asLong();
            String publishId = jsonNode.get("publishId").asText();
            Integer type = jsonNode.get("type").asInt();

            //1-发动态，2-浏览动态， 3-点赞， 4-喜欢， 5-评论，6-取消点赞，7-取消喜欢
            RecommendQuanZi recommendQuanZi = new RecommendQuanZi();
            recommendQuanZi.setUserId(userId);
            recommendQuanZi.setId(ObjectId.get());
            recommendQuanZi.setDate(System.currentTimeMillis());
            recommendQuanZi.setPublishId(pid);

            switch (type) {
                case 1: {
                    int score = 0;
                    Publish publish = this.mongoTemplate.findById(new ObjectId(publishId), Publish.class);
                    int count = StringUtils.length(publish.getText());
                    if (count >= 0 && count <= 50) {
                        score += 1;
                    } else if (count <= 100) {
                        score += 2;
                    } else {
                        score += 3;
                    }

                    if (!CollectionUtils.isEmpty(publish.getMedias())) {
                        score += publish.getMedias().size();
                    }

                    recommendQuanZi.setScore(Double.valueOf(score));

                    break;
                }
                case 2: {
                    recommendQuanZi.setScore(1d);
                    break;
                }
                case 3: {
                    recommendQuanZi.setScore(5d);
                    break;
                }
                case 4: {
                    recommendQuanZi.setScore(8d);
                    break;
                }
                case 5: {
                    recommendQuanZi.setScore(10d);
                    break;
                }
                case 6: {
                    recommendQuanZi.setScore(-5d);
                    break;
                }
                case 7: {
                    recommendQuanZi.setScore(-8d);
                    break;
                }
                default: {
                    recommendQuanZi.setScore(0d);
                    break;
                }
            }

//            String collectionName = "recommend_quanzi_" + new DateTime().toString("yyyyMMdd");
            //为了方便测试，将数据写到一张表
            String collectionName = "recommend_quanzi";
            this.mongoTemplate.save(recommendQuanZi, collectionName);

        } catch (Exception e) {
            LOGGER.error("处理消息失败~" + msg, e);
        }
    }
}
