package com.example.myapp;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiCitySearchOption;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiDetailSearchResult;
import com.baidu.mapapi.search.poi.PoiIndoorResult;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;

public class RouteActivity extends AppCompatActivity implements View.OnClickListener {

    private MapView mMapView;
    /**
     * poi检索
     */
    private Button mBtnPoi;
    /**
     * 路线规划
     */
    private Button mBtnRoute;
    private PoiSearch poiSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route);
        initView();
    }

    private void initView() {
        mMapView = (MapView) findViewById(R.id.mapView);
        mMapView.setOnClickListener(this);
        mBtnPoi = (Button) findViewById(R.id.btn_poi);
        mBtnPoi.setOnClickListener(this);
        mBtnRoute = (Button) findViewById(R.id.btn_route);
        mBtnRoute.setOnClickListener(this);


    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            default:
                break;
            case R.id.mapView:
                break;
            case R.id.btn_poi:
                initPoi();
                break;
            case R.id.btn_route:
                initRoute();
                break;
        }
    }

    private void initRoute() {

    }

    private void initPoi() {
        poiSearch = PoiSearch.newInstance();
        poiSearch.setOnGetPoiSearchResultListener(listener);

        poiSearch.searchInCity(new PoiCitySearchOption()
                .city("北京") //必填
                .keyword("美食") //必填
                .pageNum(10));

    }

    private static final String TAG = "RouteActivity";
    OnGetPoiSearchResultListener listener = new OnGetPoiSearchResultListener() {
        @Override
        public void onGetPoiResult(PoiResult poiResult) {

            SearchResult.ERRORNO error = poiResult.error;
            Log.d(TAG, "onGetPoiResult: "+error);

        }
        @Override
        public void onGetPoiDetailResult(PoiDetailSearchResult poiDetailSearchResult) {

        }
        @Override
        public void onGetPoiIndoorResult(PoiIndoorResult poiIndoorResult) {

        }
        //废弃
        @Override
        public void onGetPoiDetailResult(PoiDetailResult poiDetailResult) {

        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        poiSearch.destroy();
    }
}
