package com.example.utils;

import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;

/**
 * 添加地图标志
 */
public class MarkerUtil {
    private BitmapDescriptor bitmapDescriptor;
    private LatLng point;
    private BaiduMap baiduMap;
    private boolean isCenterZoom; // 是否将标志居中显示
    private OverlayOptions options;

    public MarkerUtil(BitmapDescriptor bitmapDescriptor, LatLng latLng, BaiduMap baiduMap, boolean isCenterZoom) {
        this.bitmapDescriptor = bitmapDescriptor;
        this.point = latLng;
        this.baiduMap = baiduMap;
        this.isCenterZoom = isCenterZoom;
        this.options = new MarkerOptions().icon(this.bitmapDescriptor)
                .position(this.point).zIndex(99);
    }

    public void mark() {
        baiduMap.addOverlay(this.options);
        if (this.isCenterZoom) {
            this.baiduMap
                    .setMapStatus(MapStatusUpdateFactory.newMapStatus(
                            new MapStatus.Builder()
                                    .target(this.point)
                                    .zoom(18)
                                    .build()
                    ));
        }
    }
}
