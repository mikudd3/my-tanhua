package com.tanhua.dubbo.server.api;

import com.tanhua.dubbo.server.pojo.Comment;
import com.tanhua.dubbo.server.pojo.Publish;
import com.tanhua.dubbo.server.vo.PageInfo;

import java.util.List;

public interface QuanZiApi {

    /**
     * 发布动态
     *
     * @param publish
     * @return
     */
    String savePublish(Publish publish);

    /**
     * 查询动态
     *
     * @return
     */
    PageInfo<Publish> queryPublishList(Long userId, Integer page, Integer pageSize);

    /**
     * 点赞
     *
     * @param userId
     * @param publishId
     * @return
     */
    boolean saveLikeComment(Long userId, String publishId);

    /**
     * 取消点赞、喜欢等
     *
     * @param userId
     * @param publishId
     * @return
     */
    boolean removeComment(Long userId, String publishId, Integer commentType);

    /**
     * 喜欢
     *
     * @param userId
     * @param publishId
     * @return
     */
    boolean saveLoveComment(Long userId, String publishId);

    /**
     * 保存评论
     *
     * @param userId
     * @param publishId
     * @param type
     * @param content
     * @return
     */
    boolean saveComment(Long userId, String publishId, Integer type, String content);

    /**
     * 查询评论数
     *
     * @param publishId
     * @param type
     * @return
     */
    Long queryCommentCount(String publishId, Integer type);

    /**
     * 根据id查询动态
     *
     * @param id
     * @return
     */
    Publish queryPublishById(String id);

    /**
     * 查询评论
     *
     * @return
     */
    PageInfo<Comment> queryCommentList(String publishId, Integer page, Integer pageSize);

    /**
     * 查询用户的评论数据
     *
     * @return
     */
    PageInfo<Comment> queryCommentListByUser(Long userId, Integer type, Integer page, Integer pageSize);

    /**
     * 根据pid批量查询
     *
     * @param pids
     * @return
     */
    List<Publish> queryPublishByPids(List<Long> pids);

    /**
     * 查询相册表
     *
     * @param userId
     * @param page
     * @param pageSize
     * @return
     */
    PageInfo<Publish> queryAlbumList(Long userId, Integer page, Integer pageSize);
}
