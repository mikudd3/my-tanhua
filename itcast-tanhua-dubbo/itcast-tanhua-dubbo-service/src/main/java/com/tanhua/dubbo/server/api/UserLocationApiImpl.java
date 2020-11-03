package com.tanhua.dubbo.server.api;

import com.alibaba.dubbo.config.annotation.Service;
import com.tanhua.dubbo.server.pojo.UserLocation;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@Service(version = "1.0.0")
public class UserLocationApiImpl implements UserLocationApi {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public String updateUserLocation(Long userId, Double longitude, Double latitude, String address) {

        UserLocation userLocation = new UserLocation();
        userLocation.setAddress(address);
        userLocation.setLocation(new GeoJsonPoint(longitude, latitude));
        userLocation.setUserId(userId);

        Query query = Query.query(Criteria.where("userId").is(userLocation.getUserId()));
        UserLocation ul = this.mongoTemplate.findOne(query, UserLocation.class);
        if (ul == null) {
            //新增
            userLocation.setId(ObjectId.get());
            userLocation.setCreated(System.currentTimeMillis());
            userLocation.setUpdated(userLocation.getCreated());
            userLocation.setLastUpdated(userLocation.getCreated());

            this.mongoTemplate.save(userLocation);

            return userLocation.getId().toHexString();
        } else {
            //更新
            Update update = Update
                    .update("location", userLocation.getLocation())
                    .set("updated", System.currentTimeMillis())
                    .set("address", userLocation.getAddress())
                    .set("lastUpdated", ul.getUpdated());
            this.mongoTemplate.updateFirst(query, update, UserLocation.class);
        }

        return ul.getId().toHexString();
    }
    
}
