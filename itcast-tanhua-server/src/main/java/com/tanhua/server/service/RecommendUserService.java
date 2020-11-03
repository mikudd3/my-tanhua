package com.tanhua.server.service;

import com.alibaba.dubbo.config.annotation.Reference;
import com.tanhua.dubbo.server.api.RecommendUserApi;
import com.tanhua.dubbo.server.pojo.RecommendUser;
import com.tanhua.dubbo.server.vo.PageInfo;
import com.tanhua.server.vo.TodayBest;
import org.springframework.stereotype.Service;

@Service
public class RecommendUserService {

    @Reference(version = "1.0.0")
    private RecommendUserApi recommendUserApi;

    /**
     * 根据用户id查询今日佳人
     *
     * @param id
     * @return
     */
    public TodayBest queryTodayBest(Long id) {
        RecommendUser recommendUser = this.recommendUserApi.queryWithMaxScore(id);
        if (null != recommendUser) {
            TodayBest todayBest = new TodayBest();
            todayBest.setId(recommendUser.getUserId()); //推荐结果的用户id

            double score = Math.floor(recommendUser.getScore());//取整,98.2 -> 98
            todayBest.setFateValue(Double.valueOf(score).longValue());
            return todayBest;
        }

        return null;
    }

    public PageInfo<RecommendUser> queryRecommendUserList(Long id, Integer page, Integer pagesize) {
        return this.recommendUserApi.queryPageInfo(id, page, pagesize);
    }

    /**
     * 查询推荐好友的缘分值
     *
     * @param userId
     * @param toUserId
     * @return
     */
    double queryScore(Long userId, Long toUserId) {
        return this.recommendUserApi.queryScore(userId, toUserId);
    }
}
