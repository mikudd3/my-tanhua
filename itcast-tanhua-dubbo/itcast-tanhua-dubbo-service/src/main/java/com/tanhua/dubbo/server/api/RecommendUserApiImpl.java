package com.tanhua.dubbo.server.api;

import com.alibaba.dubbo.config.annotation.Service;
import com.tanhua.dubbo.server.pojo.RecommendUser;
import com.tanhua.dubbo.server.vo.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

@Service(version = "1.0.0")
public class RecommendUserApiImpl implements RecommendUserApi {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public RecommendUser queryWithMaxScore(Long userId) {
        Query query = Query.query(Criteria.where("toUserId").is(userId))
                .with(Sort.by(Sort.Order.desc("score"))).limit(1);
        return this.mongoTemplate.findOne(query, RecommendUser.class);
    }

    @Override
    public PageInfo<RecommendUser> queryPageInfo(Long userId, Integer pageNum, Integer pageSize) {
        PageRequest pageRequest = PageRequest.of(pageNum - 1, pageSize, Sort.by(Sort.Order.desc("score")));
        Query query = Query.query(Criteria.where("toUserId").is(userId)).with(pageRequest);
        List<RecommendUser> recommendUserList = this.mongoTemplate.find(query, RecommendUser.class);
        // 数据总数暂不提供，如前端需要再实现
        return new PageInfo<>(0, pageNum, pageSize, recommendUserList);
    }

    @Override
    public double queryScore(Long userId, Long toUserId) {
        Query query = Query.query(Criteria
                .where("toUserId").is(toUserId)
                .and("userId").is(userId));
        RecommendUser recommendUser = this.mongoTemplate.findOne(query, RecommendUser.class);
        if (null == recommendUser) {
            return 0;
        }
        return recommendUser.getScore();
    }
}
