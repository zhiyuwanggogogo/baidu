package com.example.utils;

import android.util.Log;

import com.baidu.mapapi.search.route.BikingRouteResult;
import com.baidu.mapapi.search.route.DrivingRoutePlanOption;
import com.baidu.mapapi.search.route.DrivingRouteResult;
import com.baidu.mapapi.search.route.IndoorRouteResult;
import com.baidu.mapapi.search.route.MassTransitRouteResult;
import com.baidu.mapapi.search.route.OnGetRoutePlanResultListener;
import com.baidu.mapapi.search.route.PlanNode;
import com.baidu.mapapi.search.route.RoutePlanSearch;
import com.baidu.mapapi.search.route.TransitRouteResult;
import com.baidu.mapapi.search.route.WalkingRouteResult;

public class RoutePlanUtil {
    private RoutePlanSearch mRoutePlanSearch = RoutePlanSearch.newInstance();
    private OnGetDrivingResultListener getDrivingResultListener;
    private OnGetRoutePlanResultListener getRoutePlanResultListener = new OnGetRoutePlanResultListener() {
        @Override
        public void onGetWalkingRouteResult(WalkingRouteResult walkingRouteResult) {

        }

        @Override
        public void onGetTransitRouteResult(TransitRouteResult transitRouteResult) {

        }

        @Override
        public void onGetMassTransitRouteResult(MassTransitRouteResult massTransitRouteResult) {

        }

        @Override
        public void onGetDrivingRouteResult(DrivingRouteResult drivingRouteResult) {
            Log.e("测试", drivingRouteResult.error + ":" + drivingRouteResult.status);
            getDrivingResultListener.onSuccess(drivingRouteResult);
        }

        @Override
        public void onGetIndoorRouteResult(IndoorRouteResult indoorRouteResult) {

        }

        @Override
        public void onGetBikingRouteResult(BikingRouteResult bikingRouteResult) {

        }
    };

    public RoutePlanUtil(OnGetDrivingResultListener getDrivingResultListener) {
        this.getDrivingResultListener = getDrivingResultListener;
        this.mRoutePlanSearch.setOnGetRoutePlanResultListener(this.getRoutePlanResultListener);
    }

    public void routePlan(PlanNode startNode, PlanNode endNode){
        mRoutePlanSearch.drivingSearch((new DrivingRoutePlanOption())
                .from(startNode).to(endNode)
                .policy(DrivingRoutePlanOption.DrivingPolicy.ECAR_TIME_FIRST)
                .trafficPolicy(DrivingRoutePlanOption.DrivingTrafficPolicy.ROUTE_PATH_AND_TRAFFIC));
    }
}
