package com.tanhua.server.service;

import com.alibaba.dubbo.config.annotation.Reference;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.tobato.fastdfs.domain.conn.FdfsWebServer;
import com.github.tobato.fastdfs.domain.fdfs.StorePath;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import com.tanhua.common.pojo.User;
import com.tanhua.common.pojo.UserInfo;
import com.tanhua.common.service.PicUploadService;
import com.tanhua.common.vo.PicUploadResult;
import com.tanhua.dubbo.server.api.QuanZiApi;
import com.tanhua.dubbo.server.api.VideoApi;
import com.tanhua.dubbo.server.pojo.Video;
import com.tanhua.dubbo.server.vo.PageInfo;
import com.tanhua.server.utils.UserThreadLocal;
import com.tanhua.server.vo.PageResult;
import com.tanhua.server.vo.VideoVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
public class VideoService {

    @Autowired
    private PicUploadService picUploadService;

    @Autowired
    protected FastFileStorageClient storageClient;

    @Autowired
    private FdfsWebServer fdfsWebServer;

    @Reference(version = "1.0.0")
    private VideoApi videoApi;

    @Reference(version = "1.0.0")
    private QuanZiApi quanZiApi;

    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    @Autowired
    private UserInfoService userInfoService;

    public String saveVideo(MultipartFile picFile, MultipartFile videoFile) {
        User user = UserThreadLocal.get();
        Video video = new Video();
        video.setUserId(user.getId());
        video.setSeeType(1);
        try {
            //上传封面图片
            PicUploadResult picUploadResult = this.picUploadService.upload(picFile);
            video.setPicUrl(picUploadResult.getName()); //图片路径

            //上传视频
            StorePath storePath = storageClient.uploadFile(videoFile.getInputStream(),
                    videoFile.getSize(),
                    StringUtils.substringAfter(videoFile.getOriginalFilename(), "."),
                    null);
            video.setVideoUrl(fdfsWebServer.getWebServerUrl() + "/" + storePath.getFullPath());

            return this.videoApi.saveVideo(video);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public PageResult queryVideoList(Integer page, Integer pageSize) {

        User user = UserThreadLocal.get();

        PageResult pageResult = new PageResult();
        pageResult.setPage(page);
        pageResult.setPagesize(pageSize);
        pageResult.setPages(0);
        pageResult.setCounts(0);

        PageInfo<Video> pageInfo = null;

        //先从Redis进行命中，如果命中则返回推荐列表，如果未命中查询默认列表
        String redisValue = this.redisTemplate.opsForValue().get("QUANZI_VIDEO_RECOMMEND_" + user.getId());
        if (StringUtils.isNotEmpty(redisValue)) {
            String[] pids = StringUtils.split(redisValue, ',');
            int startIndex = (page - 1) * pageSize;
            if (startIndex < pids.length) {
                int endIndex = startIndex + pageSize - 1;
                if (endIndex >= pids.length) {
                    endIndex = pids.length - 1;
                }

                List<Long> vidList = new ArrayList<>();
                for (int i = startIndex; i <= endIndex; i++) {
                    vidList.add(Long.valueOf(pids[i]));
                }

                List<Video> videoList = this.videoApi.queryVideoListByPids(vidList);
                pageInfo = new PageInfo<>();
                pageInfo.setRecords(videoList);
            }
        }

        if(null == pageInfo){
            pageInfo = this.videoApi.queryVideoList(page, pageSize);
        }

        List<Video> records = pageInfo.getRecords();
        List<VideoVo> videoVoList = new ArrayList<>();
        List<Long> userIds = new ArrayList<>();
        for (Video record : records) {
            VideoVo videoVo = new VideoVo();

            videoVo.setUserId(record.getUserId());
            videoVo.setCover(record.getPicUrl());
            videoVo.setVideoUrl(record.getVideoUrl());
            videoVo.setId(record.getId().toHexString());
            videoVo.setSignature("我就是我~");

            Long commentCount = this.quanZiApi.queryCommentCount(videoVo.getId(), 2);
            videoVo.setCommentCount(commentCount == null ? 0 : commentCount.intValue()); // 评论数

            String followUserKey = "VIDEO_FOLLOW_USER_" + user.getId() + "_" + videoVo.getUserId();
            videoVo.setHasFocus(this.redisTemplate.hasKey(followUserKey) ? 1 : 0); //是否关注

            String userKey = "QUANZI_COMMENT_LIKE_USER_" + user.getId() + "_" + videoVo.getId();
            videoVo.setHasLiked(this.redisTemplate.hasKey(userKey) ? 1 : 0); //是否点赞（1是，0否）

            String key = "QUANZI_COMMENT_LIKE_" + videoVo.getId();
            String value = this.redisTemplate.opsForValue().get(key);
            if (StringUtils.isNotEmpty(value)) {
                videoVo.setLikeCount(Integer.valueOf(value)); //点赞数
            } else {
                videoVo.setLikeCount(0);
            }

            if (!userIds.contains(record.getUserId())) {
                userIds.add(record.getUserId());
            }

            videoVoList.add(videoVo);
        }

        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper();
        queryWrapper.in("user_id", userIds);
        List<UserInfo> userInfos = this.userInfoService.queryUserInfoList(queryWrapper);
        for (VideoVo videoVo : videoVoList) {
            for (UserInfo userInfo : userInfos) {
                if (videoVo.getUserId().longValue() == userInfo.getUserId().longValue()) {

                    videoVo.setNickname(userInfo.getNickName());
                    videoVo.setAvatar(userInfo.getLogo());

                    break;
                }
            }

        }

        pageResult.setItems(videoVoList);
        return pageResult;
    }

    /**
     * 关注用户
     *
     * @param userId
     * @return
     */
    public Boolean followUser(Long userId) {
        User user = UserThreadLocal.get();
        this.videoApi.followUser(user.getId(), userId);

        //记录已关注
        String followUserKey = "VIDEO_FOLLOW_USER_" + user.getId() + "_" + userId;
        this.redisTemplate.opsForValue().set(followUserKey, "1");

        return true;
    }

    /**
     * 取消关注
     *
     * @param userId
     * @return
     */
    public Boolean disFollowUser(Long userId) {
        User user = UserThreadLocal.get();
        this.videoApi.disFollowUser(user.getId(), userId);

        String followUserKey = "VIDEO_FOLLOW_USER_" + user.getId() + "_" + userId;
        this.redisTemplate.delete(followUserKey);

        return true;
    }
}
