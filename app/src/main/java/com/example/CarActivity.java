package com.example;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.Polyline;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.model.LatLngBounds;
import com.baidu.mapapi.search.route.DrivingRouteLine;
import com.baidu.mapapi.search.route.DrivingRouteResult;
import com.baidu.mapapi.search.route.PlanNode;
import com.baidu.mapapi.utils.DistanceUtil;
import com.example.myapp.MainActivity;
import com.example.myapp.R;
import com.example.utils.OnGetDrivingResultListener;
import com.example.utils.RoutePlanUtil;
import com.example.utils.WayPointUtil;

import java.util.ArrayList;
import java.util.List;

public class CarActivity extends AppCompatActivity {
    // 百度地图和百度地图操作对象
    private MapView mMapView;
    private BaiduMap mBaiduMap;

    // 百度地图划线对象（画路线）
    private Polyline mPolyline;
    // 百度地图标志对象（画小车）
    private Marker mMoveMarker;
    // 更新小车和路线的状态
    private Handler mHandler;
    // 更新界面的时间间隔
    private static final int TIME_INTERVAL = 5000;

    // 小车要走的路线上面的点
    private ArrayList<LatLng> latLngs = new ArrayList<>();

    // 画路线实时路况的填充纹理列表
    private List<BitmapDescriptor> mTrafficTextureList = new ArrayList<>();
    // 路线规划后的实时路况索引列表
    private List<Integer> mTrafficTextureIndexList = new ArrayList<>();
    // 细分路段后重新划分的实时路况索引列表
    private List<Integer> mNewTrafficTextureIndexList = new ArrayList<>();

    // 小车当前所在索引
    private int mIndex = 0;
    // 小车移动线程
    private Thread moveThread = null;
    // 退出移动线程的标志
    private volatile boolean exit = false;

    // 起点信息
    private LatLng startLatLng;
    private double startLat;
    private double startLng;
    // 终点信息
    private LatLng endLatLng;
    private double endLat;
    private double endLng;
    private double mLatitude;
    private double mLongtitude;
    private MyLocationListener myListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car);
        initView();
    }

    private void initView() {
        mMapView = (MapView) findViewById(R.id.mapView);
        mBaiduMap = mMapView.getMap();
        mMapView.showZoomControls(false);
        mHandler = new Handler(Looper.getMainLooper());

        startLat = 23.581031;
        startLng = 116.36554;
        endLat = 23.56719;
        endLng = 116.375703;
        startLatLng = new LatLng(startLat, startLng);
        endLatLng = new LatLng(endLat, endLng);

        // 规划路线
        addRouteLine(startLatLng, endLatLng);
    }

    private static final String TAG = "CarActivity";

    private void addRouteLine(final LatLng startLatLng, final LatLng endLatLng) {

        // 调用路线规划
        PlanNode startNode = PlanNode.withLocation(startLatLng);
        PlanNode endNode = PlanNode.withLocation(endLatLng);

        // 路线规划回调
        RoutePlanUtil routePlanUtil = new RoutePlanUtil(new OnGetDrivingResultListener() {
            @Override
            public void onSuccess(DrivingRouteResult drivingRouteResult) {
                List<DrivingRouteLine> lines = drivingRouteResult.getRouteLines();
                if (lines == null) {
                    Toast.makeText(CarActivity.this, "搜索不到路线", Toast.LENGTH_LONG).show();
                    return;
                }
                DrivingRouteLine selectedRouteLine = lines.get(0);
                // 选出拥堵最少的路线
                for (int i = 1; i < lines.size(); i++) {
                    if (selectedRouteLine.getCongestionDistance() > lines.get(i).getCongestionDistance()) {
                        selectedRouteLine = lines.get(i);
                    }
                }

                // 设置路段实时路况索引
                List<DrivingRouteLine.DrivingStep> allStep = selectedRouteLine.getAllStep();
                mTrafficTextureIndexList.clear();
                for (int j = 0; j < allStep.size(); j++) {
                    if (allStep.get(j).getTrafficList() != null && allStep.get(j).getTrafficList().length > 0) {
                        for (int k = 0; k < allStep.get(j).getTrafficList().length; k++) {
                            mTrafficTextureIndexList.add(allStep.get(j).getTrafficList()[k]);
                        }
                    }
                }

                // 获取路线规划上的点
                latLngs = WayPointUtil.getWayPointLatLng(selectedRouteLine);
                // 将路段进行细分（测试大概0.00009就是9米，0.00008就是8米, 以此类推）
                ArrayList<LatLng> temp = divideRouteLine(latLngs, 0.00009);
                latLngs = temp;


                drawPolyLine();
                moveLooper();
                // 调整地图绘制缩放等级
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                builder.include(startLatLng).include(endLatLng);
                mBaiduMap.setMapStatus(MapStatusUpdateFactory.newLatLngBounds(builder.build(), 100, 100, 100, 100));
            }
        });

        routePlanUtil.routePlan(startNode, endNode);
    }

    /**
     * 将规划好的路线点进行截取
     * 参考百度给的小车平滑轨迹移动demo实现。（循环的算法不太懂）
     *
     * @param routeLine
     * @param distance
     * @return
     */
    private ArrayList<LatLng> divideRouteLine(ArrayList<LatLng> routeLine, double distance) {
        // 截取后的路线点的结果集
        ArrayList<LatLng> result = new ArrayList<>();

        mNewTrafficTextureIndexList.clear();
        for (int i = 0; i < routeLine.size() - 1; i++) {
            final LatLng startPoint = routeLine.get(i);
            final LatLng endPoint = routeLine.get(i + 1);

            double slope = getSlope(startPoint, endPoint);
            // 是不是正向的标示
            boolean isYReverse = (startPoint.latitude > endPoint.latitude);
            boolean isXReverse = (startPoint.longitude > endPoint.longitude);

            double intercept = getInterception(slope, startPoint);

            double xMoveDistance = isXReverse ? getXMoveDistance(slope, distance) :
                    -1 * getXMoveDistance(slope, distance);

            double yMoveDistance = isYReverse ? getYMoveDistance(slope, distance) :
                    -1 * getYMoveDistance(slope, distance);

            ArrayList<LatLng> temp1 = new ArrayList<>();

            for (double j = startPoint.latitude, k = startPoint.longitude;
                 !((j > endPoint.latitude) ^ isYReverse) && !((k > endPoint.longitude) ^ isXReverse); ) {
                LatLng latLng = null;

                if (slope == Double.MAX_VALUE) {
                    latLng = new LatLng(j, k);
                    j = j - yMoveDistance;
                } else if (slope == 0.0) {
                    latLng = new LatLng(j, k - xMoveDistance);
                    k = k - xMoveDistance;
                } else {
                    latLng = new LatLng(j, (j - intercept) / slope);
                    j = j - yMoveDistance;
                }


                final LatLng finalLatLng = latLng;
                if (finalLatLng.latitude == 0 && finalLatLng.longitude == 0) {
                    continue;
                }

                mNewTrafficTextureIndexList.add(mTrafficTextureIndexList.get(i));
                temp1.add(finalLatLng);
            }
            result.addAll(temp1);
            if (i == routeLine.size() - 2) {
                result.add(endPoint); // 终点
            }
        }
        return result;
    }

    /**
     * 获取路况填充纹理列表
     * <p>
     * 路况类型：0：未知 1：畅通 2：缓行 3：拥堵 4：非常拥堵
     *
     * @return 填充纹理
     */
    public List<BitmapDescriptor> getTrafficTextureList() {
        ArrayList<BitmapDescriptor> list = new ArrayList<BitmapDescriptor>();
        list.add(BitmapDescriptorFactory.fromAsset("Icon_road_blue_arrow.png"));
        list.add(BitmapDescriptorFactory.fromAsset("Icon_road_green_arrow.png"));
        list.add(BitmapDescriptorFactory.fromAsset("Icon_road_yellow_arrow.png"));
        list.add(BitmapDescriptorFactory.fromAsset("Icon_road_red_arrow.png"));
        list.add(BitmapDescriptorFactory.fromAsset("Icon_road_nofocus.png"));
        return list;
    }

    private void drawPolyLine() {
        if (mTrafficTextureList.isEmpty()) {
            mTrafficTextureList.addAll(getTrafficTextureList());
        }

        // 画路线
        PolylineOptions polylineOptions = new PolylineOptions()
                .points(latLngs)
                .dottedLine(true)
                .width(15)
                .focus(true)
                .textureIndex(mNewTrafficTextureIndexList)
                .customTextureList(mTrafficTextureList)
                .zIndex(0);

        mPolyline = (Polyline) mBaiduMap.addOverlay(polylineOptions);


        // 画小车
        OverlayOptions markerOptions;
        markerOptions = new MarkerOptions().flat(true).anchor(0.5f, 0.5f)
                .icon(BitmapDescriptorFactory.fromAsset("arrow.png")).position(latLngs.get(0))
                .rotate((float) getAngle(0));
        mMoveMarker = (Marker) mBaiduMap.addOverlay(markerOptions);
    }

    /**
     * 根据点获取图标转的角度
     */
    private double getAngle(int startIndex) {
        if ((startIndex + 1) >= mPolyline.getPoints().size()) {
            throw new RuntimeException("index out of bonds");
        }
        LatLng startPoint = mPolyline.getPoints().get(startIndex);
        LatLng endPoint = mPolyline.getPoints().get(startIndex + 1);
        return getAngle(startPoint, endPoint);
    }

    /**
     * 根据两点算取图标转的角度
     */
    private double getAngle(LatLng fromPoint, LatLng toPoint) {
        double slope = getSlope(fromPoint, toPoint);
        if (slope == Double.MAX_VALUE) {
            if (toPoint.latitude > fromPoint.latitude) {
                return 0;
            } else {
                return 180;
            }
        } else if (slope == 0.0) {
            if (toPoint.longitude > fromPoint.longitude) {
                return -90;
            } else {
                return 90;
            }
        }
        float deltAngle = 0;
        if ((toPoint.latitude - fromPoint.latitude) * slope < 0) {
            deltAngle = 180;
        }
        double radio = Math.atan(slope);
        double angle = 180 * (radio / Math.PI) + deltAngle - 90;
        return angle;
    }

    // 退出移动线程
    private void stopMoveThread() {
        exit = true;
    }

    /**
     * 循环进行移动逻辑
     */
    public void moveLooper() {
        moveThread = new Thread() {
            public void run() {
                Thread thisThread = Thread.currentThread();
                while (!exit) {
                    for (int i = 0; i < latLngs.size() - 1; ) {
                        if (exit) {
                            break;
                        }
                        for (int p = 0; p < latLngs.size() - 1; p++) {
                            // 这是更新索引的条件，这里总是为true
                            // 实际情况可以是：当前误差小于5米 DistanceUtil.getDistance(mCurrentLatLng, latLngs.get(p)) <= 5）
                            // mCurrentLatLng 这个小车的当前位置得自行获取得到
//                            double distance = DistanceUtil.getDistance(mCurrentLatLng, latLngs.get(p));

                            if (true) {
//                              实际情况的索引更新 mIndex = p;
                                mIndex++; // 模拟就是每次加1
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(CarActivity.this, "当前索引：" + mIndex, Toast.LENGTH_SHORT).show();
                                    }
                                });
                                break;
                            }
                        }

                        // 改变循环条件
                        i = mIndex + 1;

                        if (mIndex >= latLngs.size() - 1) {
                            exit = true;
                            break;
                        }

                        // 擦除走过的路线
                        int len = mNewTrafficTextureIndexList.subList(mIndex, mNewTrafficTextureIndexList.size()).size();
                        Integer[] integers = mNewTrafficTextureIndexList.subList(mIndex, mNewTrafficTextureIndexList.size()).toArray(new Integer[len]);
                        int[] index = new int[integers.length];
                        for (int x = 0; x < integers.length; x++) {
                            index[x] = integers[x];
                        }
                        if (index.length > 0) {
                            mPolyline.setIndexs(index);
                            mPolyline.setPoints(latLngs.subList(mIndex, latLngs.size()));
                        }

                        // 这里是小车的当前点和下一个点，用于确定车头方向
                        final LatLng startPoint = latLngs.get(mIndex);
                        final LatLng endPoint = latLngs.get(mIndex + 1);

                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                // 更新小车的位置和车头的角度
                                if (mMapView == null) {
                                    return;
                                }
                                mMoveMarker.setPosition(startPoint);
                                mMoveMarker.setRotate((float) getAngle(startPoint,
                                        endPoint));
                            }
                        });

                        try {
                            // 控制线程更新时间间隔
                            thisThread.sleep(TIME_INTERVAL);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        };
        // 启动线程
        moveThread.start();
    }

    //初始化定位
    private void initMyLocation() {
        //缩放地图
        MapStatusUpdate msu = MapStatusUpdateFactory.zoomTo(15.0f);
        mBaiduMap.setMapStatus(msu);
        //开启定位
        mBaiduMap.setMyLocationEnabled(true);
        //声明LocationClient类
        LocationClient mLocationClient = new LocationClient(this);
        //通过LocationClientOption设置LocationClient相关参数
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true); // 打开gps
        option.setCoorType("bd09ll"); // 设置坐标类型
        option.setIsNeedAddress(true);//设置是否需要地址信息
        option.setScanSpan(1000);
        //设置locationClientOption
        mLocationClient.setLocOption(option);
        myListener = new MyLocationListener();
        //注册监听函数
        mLocationClient.registerLocationListener(myListener);
        //定位动态跟随
        mBaiduMap.setMyLocationConfiguration(new MyLocationConfiguration(MyLocationConfiguration.LocationMode.FOLLOWING, false, null));

        //开始定位
        mLocationClient.start();
    }

    //定位
    private class MyLocationListener extends BDAbstractLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            //mapView 销毁后不在处理新接收的位置
            if (location == null || mMapView == null) {
                return;
            }
            //更新经纬度
            mLatitude = location.getLatitude();
            mLongtitude = location.getLongitude();
            //设置起点
            LatLng mCurrentLatLng = new LatLng(mLatitude, mLongtitude);
        }
    }

    /**
     * 计算x方向每次移动的距离
     */
    private double getXMoveDistance(double slope, double distance) {
        if (slope == Double.MAX_VALUE || slope == 0.0) {
            return distance;
        }
        return Math.abs((distance * 1 / slope) / Math.sqrt(1 + 1 / (slope * slope)));
    }

    /**
     * 计算y方向每次移动的距离
     */
    private double getYMoveDistance(double slope, double distance) {
        if (slope == Double.MAX_VALUE || slope == 0.0) {
            return distance;
        }
        return Math.abs((distance * slope) / Math.sqrt(1 + slope * slope));
    }

    /**
     * 根据点和斜率算取截距
     */
    private double getInterception(double slope, LatLng point) {
        double interception = point.latitude - slope * point.longitude;
        return interception;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);
    }

    /**
     * 斜率有两个作用——计算小车的旋转角度（车头方向）和计算截距。斜率计算方法如下
     * 计算两个坐标点之间的斜率
     * 根据斜率来计算小车旋转角度和方向
     * 以经度为X轴方向，纬度为Y轴方向
     */
    private double getSlope(LatLng startPoint, LatLng endPoint) {
        /**
         * 起点终点的经度相同，则认为斜率为Double.MAX_VALUE
         */
        if (endPoint.longitude == startPoint.longitude) {
            return Double.MAX_VALUE;
        }
        return (endPoint.latitude - startPoint.latitude) / (endPoint.longitude - startPoint.longitude);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
        mBaiduMap.clear();
        // 关闭线程
        stopMoveThread();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Log.e("测试", "结束");
        finish();
    }
}
