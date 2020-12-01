package com.example.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import java.util.List;

/**
 * 获取位置本机经纬度
 */
 
public class LocationUtils {
    @SuppressLint("MissingPermission")
    private Location location;
 
    private LocationManager locationManager;
    private static LocationUtils locationUtils;
    public static LocationUtils getInstance(){
        if (locationUtils == null){
            locationUtils = new LocationUtils();
        }
        return locationUtils;
    }
 
    public Location getLocations(Context context){
        if (!checkPermission(context, permission.ACCESS_COARSE_LOCATION)){
            return null;
        }
        try {
            //获取系统的服务，
            locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            //创建一个criteria对象
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_COARSE);
            //设置不需要获取海拔方向数据
            criteria.setAltitudeRequired(false);
            criteria.setBearingRequired(false);
            //设置允许产生资费
            criteria.setCostAllowed(true);
            //要求低耗电
            criteria.setPowerRequirement(Criteria.POWER_LOW);
            String provider = locationManager.getBestProvider(criteria, true);
            if (provider == null) {
                List<String> providers = locationManager.getProviders(true);
                if (providers != null && providers.size() > 0) {
                    provider = providers.get(0);
                }
            }
 
            location= locationManager.getLastKnownLocation(provider);
 
            if(location==null){
                locationManager.requestLocationUpdates(provider, 1000 * 60 * 60, 1000, new LocationListener() {
                    @Override
                    public void onLocationChanged(Location loc) {
                        location=loc;
                    }
 
                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {
 
                    }
 
                    @Override
                    public void onProviderEnabled(String provider) {
 
                    }
 
                    @Override
                    public void onProviderDisabled(String provider) {
 
                    }
                });
            }
 
            return location;
 
        }catch (SecurityException e){
            e.printStackTrace();
        }catch (Exception e){
            e.printStackTrace();
        }
 
        return null;
 
    }
 
 
    private boolean checkPermission(Context context, permission permName) {
        int perm = context.checkCallingOrSelfPermission("android.permission."+permName.toString());
        return perm == PackageManager.PERMISSION_GRANTED;
    }
 
    private enum permission{
        ACCESS_COARSE_LOCATION,
        ACCESS_FINE_LOCATION
    }
 
}