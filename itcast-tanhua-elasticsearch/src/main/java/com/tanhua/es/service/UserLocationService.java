package com.tanhua.es.service;

import com.tanhua.es.pojo.UserLocationES;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.GeoDistanceQueryBuilder;
import org.elasticsearch.search.sort.GeoDistanceSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.*;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class UserLocationService {

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    /**
     * 更新用户的地理位置
     *
     * @param userId
     * @param longitude
     * @param latitude
     * @param address
     * @return
     */
    public boolean updateUserLocation(Long userId, Double longitude, Double latitude, String address) {
        try {
            if (!this.elasticsearchTemplate.indexExists("tanhua")) {
                // 创建索引
                this.elasticsearchTemplate.createIndex(UserLocationES.class);
            }

            if (!this.elasticsearchTemplate.typeExists("tanhua", "user_location")) {
                // 创建type
                this.elasticsearchTemplate.putMapping(UserLocationES.class);
            }


            GetQuery getQuery = new GetQuery();
            getQuery.setId(userId.toString());
            UserLocationES ul = this.elasticsearchTemplate.queryForObject(getQuery, UserLocationES.class);
            if (null == ul) {
                UserLocationES userLocationES = new UserLocationES();

                userLocationES.setLocation(new GeoPoint(latitude, longitude));
                userLocationES.setAddress(address);
                userLocationES.setUserId(userId);
                userLocationES.setCreated(System.currentTimeMillis());
                userLocationES.setUpdated(userLocationES.getCreated());
                userLocationES.setLastUpdated(userLocationES.getCreated());

                IndexQuery indexQuery = new IndexQueryBuilder().withObject(userLocationES).build();
                this.elasticsearchTemplate.index(indexQuery);
            } else {
                Map<String, Object> map = new HashMap<>();
                map.put("lastUpdated", ul.getUpdated());
                map.put("updated", System.currentTimeMillis());
                map.put("address", address);
                map.put("location", new GeoPoint(latitude, longitude));

                UpdateRequest updateRequest = new UpdateRequest();
                updateRequest.doc(map);

                UpdateQuery updateQuery = new UpdateQueryBuilder()
                        .withId(userId.toString())
                        .withClass(UserLocationES.class)
                        .withUpdateRequest(updateRequest).build();

                this.elasticsearchTemplate.update(updateQuery);
            }


            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 查询用户的位置信息
     *
     * @param userId
     * @return
     */
    public UserLocationES queryByUserId(Long userId) {
        GetQuery getQuery = new GetQuery();
        getQuery.setId(userId.toString());
        return this.elasticsearchTemplate.queryForObject(getQuery, UserLocationES.class);
    }

    /**
     * 根据位置搜索
     *
     * @param longitude 经度
     * @param latitude  纬度
     * @param distance  距离(米)
     * @param page      页数
     * @param pageSize  页面大小
     */
    public Page<UserLocationES> queryUserFromLocation(Double longitude, Double latitude, Double distance, Integer page, Integer pageSize) {
        String fieldName = "location";

        // 实现了SearchQuery接口，用于组装QueryBuilder和SortBuilder以及Pageable等
        NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder();

        // 分页
        PageRequest pageRequest = PageRequest.of(page - 1, pageSize);
        nativeSearchQueryBuilder.withPageable(pageRequest);

        // 定义bool查询
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();

        // 以某点为中心，搜索指定范围
        GeoDistanceQueryBuilder distanceQueryBuilder = new GeoDistanceQueryBuilder(fieldName);
        distanceQueryBuilder.point(latitude, longitude);

        // 定义查询单位：公里
        distanceQueryBuilder.distance(distance / 1000, DistanceUnit.KILOMETERS);

        boolQueryBuilder.must(distanceQueryBuilder);
        nativeSearchQueryBuilder.withQuery(boolQueryBuilder);

        // 按距离升序
        GeoDistanceSortBuilder distanceSortBuilder =
                new GeoDistanceSortBuilder(fieldName, latitude, longitude);
        distanceSortBuilder.unit(DistanceUnit.KILOMETERS); //设置单位
        distanceSortBuilder.order(SortOrder.ASC); //正序排序
        nativeSearchQueryBuilder.withSort(distanceSortBuilder);

        return this.elasticsearchTemplate.queryForPage(nativeSearchQueryBuilder.build(), UserLocationES.class);
    }

}
