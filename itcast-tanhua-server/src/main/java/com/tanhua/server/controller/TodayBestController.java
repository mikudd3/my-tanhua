package com.tanhua.server.controller;

import com.tanhua.server.service.TodayBestService;
import com.tanhua.server.vo.PageResult;
import com.tanhua.server.vo.RecommendUserQueryParam;
import com.tanhua.server.vo.TodayBest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("tanhua")
public class TodayBestController {

    @Autowired
    private TodayBestService todayBestService;


    /**
     * 查询今日佳人
     *
     * @return
     */
    @GetMapping("todayBest")
    public TodayBest queryTodayBest(){
        return this.todayBestService.queryTodayBest();
    }

    /**
     * 推荐列表
     *
     * @return
     */
    @GetMapping("recommendation")
    public PageResult queryRecommendUserList(RecommendUserQueryParam queryParam) {
        return this.todayBestService.queryRecommendUserList(queryParam);
    }

}
