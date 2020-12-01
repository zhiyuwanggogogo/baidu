package com.example.utils;

import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.route.DrivingRouteLine;

import java.util.ArrayList;
import java.util.List;

/**
 *  规划路线后，取出路线经过的点
 */
public class WayPointUtil {

    public static ArrayList<LatLng> getWayPointLatLng(DrivingRouteLine selectedRouteLine) {
        ArrayList<LatLng> trackLatLng = new ArrayList<>();
        List<DrivingRouteLine.DrivingStep> allStep = selectedRouteLine.getAllStep();
        for (int j = 0; j < allStep.size(); j++) {
            if (j == allStep.size() - 1) {
                trackLatLng.addAll(allStep.get(j).getWayPoints());
            } else {
                trackLatLng.addAll(allStep.get(j).getWayPoints().subList(0, allStep.get(j).getWayPoints().size() - 1));
            }
        }

        return trackLatLng;
    }
}
