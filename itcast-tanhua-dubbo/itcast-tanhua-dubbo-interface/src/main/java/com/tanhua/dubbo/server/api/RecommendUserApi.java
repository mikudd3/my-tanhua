package com.tanhua.dubbo.server.api;

import com.tanhua.dubbo.server.vo.PageInfo;
import com.tanhua.dubbo.server.pojo.RecommendUser;

public interface RecommendUserApi {

    /**
     * 查询一位得分最高的推荐用户
     *
     * @param userId
     * @return
     */
    RecommendUser queryWithMaxScore(Long userId);

    /**
     * 按照得分倒序
     *
     * @param userId
     * @param pageNum
     * @param pageSize
     * @return
     */
    PageInfo<RecommendUser> queryPageInfo(Long userId, Integer pageNum, Integer pageSize);

    /**
     * 查询推荐好友的缘分值
     *
     * @param userId
     * @param toUserId
     * @return
     */
    double queryScore(Long userId, Long toUserId);
}
