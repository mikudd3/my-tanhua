package com.tanhua.dubbo.server.api;

public interface UserLocationApi {

    /**
     * 更新用户地理位置
     *
     * @return
     */
    String updateUserLocation(Long userId, Double longitude, Double latitude, String address);

}
