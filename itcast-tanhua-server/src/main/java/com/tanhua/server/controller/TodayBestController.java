package com.tanhua.server.controller;

import com.tanhua.server.service.TodayBestService;
import com.tanhua.server.vo.PageResult;
import com.tanhua.server.vo.RecommendUserQueryParam;
import com.tanhua.server.vo.TodayBest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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

    /**
     * 查询今日佳人详情
     *
     * @param userId
     * @return
     */
    @GetMapping("{id}/personalInfo")
    public ResponseEntity<TodayBest> queryTodayBest(@PathVariable("id") Long userId) {
        try {
            TodayBest todayBest = this.todayBestService.queryTodayBest(userId);
            return ResponseEntity.ok(todayBest);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * 查询陌生人问题
     *
     * @param userId
     * @return
     */
    @GetMapping("strangerQuestions")
    public ResponseEntity<String> queryQuestion(@RequestParam("userId") Long userId) {
        try {
            String question = this.todayBestService.queryQuestion(userId);
            return ResponseEntity.ok(question);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * 回复陌生人问题
     *
     * @return
     */
    @PostMapping("strangerQuestions")
    public ResponseEntity<Void> replyQuestion(@RequestBody Map<String, Object> param) {
        try {
            Long userId = Long.valueOf(param.get("userId").toString());
            String reply = param.get("reply").toString();
            Boolean result = this.todayBestService.replyQuestion(userId, reply);
            if (result) {
                return ResponseEntity.ok(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

}
