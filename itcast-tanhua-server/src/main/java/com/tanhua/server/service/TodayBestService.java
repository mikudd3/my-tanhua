package com.tanhua.server.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tanhua.common.pojo.Question;
import com.tanhua.common.pojo.User;
import com.tanhua.common.pojo.UserInfo;
import com.tanhua.dubbo.server.pojo.RecommendUser;
import com.tanhua.dubbo.server.vo.PageInfo;
import com.tanhua.server.utils.UserThreadLocal;
import com.tanhua.server.vo.PageResult;
import com.tanhua.server.vo.RecommendUserQueryParam;
import com.tanhua.server.vo.TodayBest;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class TodayBestService {

    @Autowired
    private RecommendUserService recommendUserService;

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private UserService userService;

    @Value("${tanhua.sso.default.user}")
    private Long defaultUser;

    @Value("${tanhua.sso.default.recommend.users}")
    private String defaultRecommendUsers;

    @Value("${tanhua.sso.url}")
    private String ssoUrl;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private RestTemplate restTemplate;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public TodayBest queryTodayBest() {
              //查询当前的登录信息
        User user = UserThreadLocal.get();

        //查询今日佳人
        TodayBest todayBest = this.recommendUserService.queryTodayBest(user.getId());
        if (null == todayBest) {
            // 默认推荐用户
            todayBest = new TodayBest();
            todayBest.setId(defaultUser);
        }

        //补全信息
        UserInfo userInfo = this.userInfoService.queryById(todayBest.getId());
        todayBest.setAge(userInfo.getAge());
        todayBest.setAvatar(userInfo.getLogo());
        todayBest.setGender(userInfo.getSex().toString());
        todayBest.setNickname(userInfo.getNickName());
        todayBest.setTags(StringUtils.split(userInfo.getTags(), ','));

        return todayBest;
    }

    public TodayBest queryTodayBest(Long userId) {

        User user = UserThreadLocal.get();

        TodayBest todayBest = new TodayBest();
        //补全信息
        UserInfo userInfo = this.userInfoService.queryById(userId);
        todayBest.setId(userId);
        todayBest.setAge(userInfo.getAge());
        todayBest.setAvatar(userInfo.getLogo());
        todayBest.setGender(userInfo.getSex().name().toLowerCase());
        todayBest.setNickname(userInfo.getNickName());
        todayBest.setTags(StringUtils.split(userInfo.getTags(), ','));

        double score = this.recommendUserService.queryScore(userId, user.getId());
        if(score == 0){
            score = 98; //默认分值
        }

        todayBest.setFateValue(Double.valueOf(score).longValue());

        return todayBest;
    }

    /**
     * 查询推荐用户列表
     *
     * @param queryParam
     * @return
     */
    public PageResult queryRecommendUserList(RecommendUserQueryParam queryParam) {
        //查询当前的登录信息
        User user = UserThreadLocal.get();

        PageResult pageResult = new PageResult();
        PageInfo<RecommendUser> pageInfo = this.recommendUserService.queryRecommendUserList(user.getId(), queryParam.getPage(), queryParam.getPagesize());

        pageResult.setCounts(0); //前端不参与计算，仅需要返回字段
        pageResult.setPage(queryParam.getPage());
        pageResult.setPagesize(queryParam.getPagesize());


        List<RecommendUser> records = pageInfo.getRecords();

        if(CollectionUtils.isEmpty(records)){
            //默认推荐列表
            String[] ss = StringUtils.split(defaultRecommendUsers, ',');
            for (String s : ss) {
                RecommendUser recommendUser = new RecommendUser();

                recommendUser.setUserId(Long.valueOf(s));
                recommendUser.setToUserId(user.getId());
                recommendUser.setScore(RandomUtils.nextDouble(70, 99));

                records.add(recommendUser);
            }
        }

        List<Long> userIds = new ArrayList<>();
        for (RecommendUser record : records) {
            userIds.add(record.getUserId());
        }

        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("user_id", userIds);

        //暂时不添加条件，默认返回所有推荐的数据
//        if (StringUtils.isNotEmpty(queryParam.getGender())) { //性别条件
//            if (queryParam.getGender().equals("man")) {
//                queryWrapper.eq("sex", 1);
//            } else {
//                queryWrapper.eq("sex", 2);
//            }
//        }

//        if (StringUtils.isNotEmpty(queryParam.getCity())) { //居住城市
//            queryWrapper.eq("city", queryParam.getCity());
//        }

//        if (queryParam.getAge() != null) { //年龄
//            queryWrapper.lt("age", queryParam.getAge());
//        }

        List<UserInfo> userInfos = this.userInfoService.queryUserInfoList(queryWrapper);

        List<TodayBest> todayBests = new ArrayList<>();
        for (UserInfo userInfo : userInfos) {
            TodayBest todayBest = new TodayBest();
            todayBest.setId(userInfo.getUserId());
            todayBest.setAge(userInfo.getAge());
            todayBest.setAvatar(userInfo.getLogo());
            todayBest.setGender(userInfo.getSex().name().toLowerCase());
            todayBest.setNickname(userInfo.getNickName());
            todayBest.setTags(StringUtils.split(userInfo.getTags(), ','));

            //设置缘分值
            for (RecommendUser record : records) {
                if(userInfo.getUserId().longValue() == record.getUserId().longValue()){
                    double score = Math.floor(record.getScore());
                    todayBest.setFateValue(Double.valueOf(score).longValue());
                    break;
                }
            }

            todayBests.add(todayBest);
        }

        //排序集合，按照score倒序排序
        Collections.sort(todayBests, (o1, o2) -> new Long(o2.getFateValue() - o1.getFateValue()).intValue());

        pageResult.setItems(todayBests);

        return pageResult;
    }

    public String queryQuestion(Long userId) {
        Question question = this.questionService.queryQuestion(userId);
        if (null != question) {
            return question.getTxt();
        }
        return "";
    }

    /**
     * 回复陌生人问题，发送消息给对方
     *
     * @param userId
     * @param reply
     * @return
     */
    public Boolean replyQuestion(Long userId, String reply) {
        User user = UserThreadLocal.get();
        UserInfo userInfo = this.userInfoService.queryById(user.getId());

        //构建消息内容
        Map<String, Object> msg = new HashMap<>();
        msg.put("userId", user.getId().toString());
        msg.put("nickname", this.queryQuestion(userId));
        msg.put("strangerQuestion", userInfo.getNickName());
        msg.put("reply", reply);

        try {
            String msgStr = MAPPER.writeValueAsString(msg);

            String targetUrl = this.ssoUrl + "/user/huanxin/messages";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("target", userId.toString());
            params.add("msg", msgStr);

            HttpEntity<MultiValueMap<String, String>> httpEntity = new HttpEntity<>(params, headers);

            ResponseEntity<Void> responseEntity = this.restTemplate.postForEntity(targetUrl, httpEntity, Void.class);

            return responseEntity.getStatusCodeValue() == 200;
        } catch (Exception e) {
            e.printStackTrace();
        }


        return false;
    }
}
