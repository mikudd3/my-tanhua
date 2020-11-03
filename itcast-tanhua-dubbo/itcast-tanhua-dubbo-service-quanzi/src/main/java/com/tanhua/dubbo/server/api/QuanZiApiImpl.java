package com.tanhua.dubbo.server.api;

import com.alibaba.dubbo.common.utils.CollectionUtils;
import com.alibaba.dubbo.config.annotation.Service;
import com.mongodb.client.result.DeleteResult;
import com.tanhua.dubbo.server.pojo.*;
import com.tanhua.dubbo.server.service.IdService;
import com.tanhua.dubbo.server.vo.PageInfo;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.List;

@Service(version = "1.0.0")
public class QuanZiApiImpl implements QuanZiApi {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private IdService idService;


    @Override
    public String savePublish(Publish publish) {

        // 校验
        if (publish.getUserId() == null) {
            return null;
        }

        try {
            publish.setCreated(System.currentTimeMillis()); //设置创建时间
            publish.setId(ObjectId.get()); //设置id
            publish.setPid(this.idService.createId("publish", publish.getId().toHexString()));

            this.mongoTemplate.save(publish); //保存发布

            Album album = new Album(); // 构建相册对象
            album.setPublishId(publish.getId());
            album.setCreated(System.currentTimeMillis());
            album.setId(ObjectId.get());
            this.mongoTemplate.save(album, "quanzi_album_" + publish.getUserId());

            //写入好友的时间线中
            Criteria criteria = Criteria.where("userId").is(publish.getUserId());
            List<Users> users = this.mongoTemplate.find(Query.query(criteria), Users.class);
            for (Users user : users) {
                TimeLine timeLine = new TimeLine();
                timeLine.setId(ObjectId.get());
                timeLine.setPublishId(publish.getId());
                timeLine.setUserId(user.getUserId());
                timeLine.setDate(System.currentTimeMillis());
                this.mongoTemplate.save(timeLine, "quanzi_time_line_" + user.getFriendId());
            }

            return publish.getId().toHexString();
        } catch (Exception e) {
            e.printStackTrace();
            //TODO 出错的事务回滚
        }

        return null;
    }

    @Override
    public PageInfo<Publish> queryPublishList(Long userId, Integer page, Integer pageSize) {

        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Order.desc("date")));
        Query query = new Query().with(pageable);

        String tableName = "quanzi_time_line_";
        if (null == userId) {
            //查询推荐动态
            tableName += "recommend";
        } else {
            //查询好友动态
            tableName += userId;
        }

        // 查询自己的时间线表
        List<TimeLine> timeLines = this.mongoTemplate.find(query, TimeLine.class, tableName);

        List<ObjectId> ids = new ArrayList<>();
        for (TimeLine timeLine : timeLines) {
            ids.add(timeLine.getPublishId());
        }

        Query queryPublish = Query.query(Criteria.where("id").in(ids)).with(Sort.by(Sort.Order.desc("created")));
        //查询动态信息
        List<Publish> publishList = this.mongoTemplate.find(queryPublish, Publish.class);

        // 封装分页对象
        PageInfo<Publish> pageInfo = new PageInfo<>();
        pageInfo.setPageNum(page);
        pageInfo.setPageSize(pageSize);
        pageInfo.setTotal(0); //不提供总数
        pageInfo.setRecords(publishList);

        return pageInfo;
    }

    /**
     * 点赞
     *
     * @param userId
     * @param publishId
     */
    public boolean saveLikeComment(Long userId, String publishId) {
        Query query = Query.query(Criteria
                .where("publishId").is(new ObjectId(publishId))
                .and("userId").is(userId)
                .and("commentType").is(1));
        long count = this.mongoTemplate.count(query, Comment.class);
        if (count > 0) {
            return false;
        }
        return this.saveComment(userId, publishId, 1, null);
    }

    /**
     * 取消点赞、喜欢等
     *
     * @return
     */
    public boolean removeComment(Long userId, String publishId, Integer commentType) {
        Query query = Query.query(Criteria
                .where("publishId").is(new ObjectId(publishId))
                .and("userId").is(userId)
                .and("commentType").is(commentType));
        DeleteResult remove = this.mongoTemplate.remove(query, Comment.class);
        return remove.getDeletedCount() > 0;
    }

    /**
     * 喜欢
     *
     * @param userId
     * @param publishId
     */
    public boolean saveLoveComment(Long userId, String publishId) {
        Query query = Query.query(Criteria
                .where("publishId").is(new ObjectId(publishId))
                .and("userId").is(userId)
                .and("commentType").is(3));
        long count = this.mongoTemplate.count(query, Comment.class);
        if (count > 0) {
            return false;
        }
        return this.saveComment(userId, publishId, 3, null);
    }

    /**
     * 保存评论
     *
     * @param userId
     * @param publishId
     * @param type
     * @return
     */
    public boolean saveComment(Long userId, String publishId, Integer type, String content) {
        try {
            Comment comment = new Comment();
            comment.setId(ObjectId.get());
            comment.setUserId(userId);
            comment.setContent(content);
            comment.setPublishId(new ObjectId(publishId));
            comment.setCommentType(type);
            comment.setCreated(System.currentTimeMillis());

            // 设置发布人的id
            Publish publish = this.mongoTemplate.findById(comment.getPublishId(), Publish.class);
            if (null != publish) {
                comment.setPublishUserId(publish.getUserId());
            } else {
                Video video = this.mongoTemplate.findById(comment.getPublishId(), Video.class);
                if (null != video) {
                    comment.setPublishUserId(video.getUserId());
                }
            }

            this.mongoTemplate.save(comment);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public Long queryCommentCount(String publishId, Integer type) {
        Query query = Query.query(Criteria.where("publishId").is(new ObjectId(publishId)).and("commentType").is(type));
        return this.mongoTemplate.count(query, Comment.class);
    }

    @Override
    public Publish queryPublishById(String id) {
        return this.mongoTemplate.findById(new ObjectId(id), Publish.class);
    }

    @Override
    public PageInfo<Comment> queryCommentList(String publishId, Integer page, Integer pageSize) {
        PageRequest pageRequest = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Order.asc("created")));
        Query query = new Query(Criteria
                .where("publishId").is(new ObjectId(publishId))
                .and("commentType").is(2)).with(pageRequest);

        //查询时间线表
        List<Comment> timeLineList = this.mongoTemplate.find(query, Comment.class);

        PageInfo<Comment> pageInfo = new PageInfo<>();
        pageInfo.setPageNum(page);
        pageInfo.setPageSize(pageSize);
        pageInfo.setRecords(timeLineList);
        pageInfo.setTotal(0); //不提供总数
        return pageInfo;
    }

    @Override
    public PageInfo<Comment> queryCommentListByUser(Long userId, Integer type, Integer page, Integer pageSize) {

        PageRequest pageRequest = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Order.desc("created")));
        Query query = new Query(Criteria
                .where("publishUserId").is(userId)
                .and("commentType").is(type)).with(pageRequest);

        List<Comment> commentList = this.mongoTemplate.find(query, Comment.class);

        PageInfo<Comment> pageInfo = new PageInfo<>();
        pageInfo.setPageNum(page);
        pageInfo.setPageSize(pageSize);
        pageInfo.setRecords(commentList);
        pageInfo.setTotal(0); //不提供总数
        return pageInfo;
    }

    @Override
    public List<Publish> queryPublishByPids(List<Long> pids){
        Query query = new Query(Criteria.where("pid").in(pids));
        return this.mongoTemplate.find(query, Publish.class);
    }

    @Override
    public PageInfo<Publish> queryAlbumList(Long userId, Integer page, Integer pageSize) {
        PageInfo<Publish> pageInfo = new PageInfo<>();
        pageInfo.setPageNum(page);
        pageInfo.setPageSize(pageSize);
        pageInfo.setTotal(0); //不提供总数

        PageRequest pageRequest = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Order.desc("created")));
        Query query = new Query().with(pageRequest);
        List<Album> albumList = this.mongoTemplate.find(query, Album.class, "quanzi_album_" + userId);

        if(CollectionUtils.isEmpty(albumList)){
            return pageInfo;
        }


        List<ObjectId> publishIds = new ArrayList<>();
        for (Album album : albumList) {
            publishIds.add(album.getPublishId());
        }

        //查询发布信息
        Query queryPublish = Query.query(Criteria.where("id").in(publishIds)).with(Sort.by(Sort.Order.desc("created")));
        List<Publish> publishList = this.mongoTemplate.find(queryPublish, Publish.class);

        pageInfo.setRecords(publishList);

        return pageInfo;
    }




}
