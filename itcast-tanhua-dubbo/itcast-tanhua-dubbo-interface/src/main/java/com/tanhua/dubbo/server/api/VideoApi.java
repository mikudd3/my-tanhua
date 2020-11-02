package com.tanhua.dubbo.server.api;

import com.tanhua.dubbo.server.pojo.Video;
import com.tanhua.dubbo.server.vo.PageInfo;

import java.util.List;

public interface VideoApi {

    /**
     * 保存小视频
     *
     * @param video
     * @return
     */
    String saveVideo(Video video);

    /**
     * 分页查询小视频列表，按照时间倒序排序
     *
     * @param page
     * @param pageSize
     * @return
     */
    PageInfo<Video> queryVideoList(Integer page, Integer pageSize);

    /**
     * 关注用户
     *
     * @param userId
     * @param followUserId
     * @return
     */
    Boolean followUser(Long userId, Long followUserId);

    /**
     * 取消关注用户
     *
     * @param userId
     * @param followUserId
     * @return
     */
    Boolean disFollowUser(Long userId, Long followUserId);

    /**
     * 根据id查询
     *
     * @param videoId
     * @return
     */
    Video queryVideoById(String videoId);

    /**
     * 根据vid批量查询
     *
     * @param vids
     * @return
     */
    List<Video> queryVideoListByPids(List<Long> vids);
}
