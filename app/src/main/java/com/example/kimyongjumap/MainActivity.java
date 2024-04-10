package com.example.kimyongjumap;

import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;

import com.naver.maps.geometry.Coord;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.Overlay;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.overlay.PathOverlay;
import com.naver.maps.map.util.FusedLocationSource;
import android.Manifest;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    PathOverlay path = new PathOverlay();
    List<LatLng> coords = new ArrayList<>();
    private static final String TAG = "MainActivity";
    private static OverlayImage overlayImage;
    private static final int PERMISSION_REQUEST_CODE = 100;

    private static final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };
    private FusedLocationSource mLocationSource;
    private NaverMap mNaverMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLocationSource =
                new FusedLocationSource(this, PERMISSION_REQUEST_CODE);
        setContentView(R.layout.activity_main);

        FragmentManager fm = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment) fm.findFragmentById(R.id.map_fragment);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.map_fragment, mapFragment).commit();
        }

        mapFragment.getMapAsync(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // 권한 획득 여부 확인
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mNaverMap.setLocationTrackingMode(LocationTrackingMode.Follow);
            }
        }
    }
    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        Log.d(TAG, "onMapReady");

        // NaverMap 객체에 위치 소스 지정
        mNaverMap = naverMap;
        mNaverMap.setLocationSource(mLocationSource);

        // 권한 확인, onRequestPermissionsResult 콜백 메서드 호출
        ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_REQUEST_CODE);

        // 지도에 마커 표시
        Marker marker = new Marker();
        marker.setPosition(new LatLng(37.5670135, 126.9783740));
        marker.setMap(naverMap);
        naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);

        naverMap.addOnLocationChangeListener(location ->
                Toast.makeText(this,
                        location.getLatitude() + ", " + location.getLongitude(),
                        Toast.LENGTH_SHORT).show());

        path.setCoords(Arrays.asList(
                new LatLng(37.4487, 127.1680583),
                new LatLng(37.4487, 127.1681),
                new LatLng(37.4499, 127.1696),
                new LatLng(37.4509, 127.1724)
        ));
        path.setPatternImage(overlayImage.fromResource(R.drawable.baseline_arrow_drop_up_24));
        path.setPatternInterval(40); //화살표 사이 거리px
        path.setWidth(30); //경로두깨
        path.setColor(Color.WHITE); //경로색상
        path.setPassedColor(Color.GRAY); //지나간 경로 색상
        path.setOutlineColor(Color.RED);// 경로 테두리 색상
        path.setPassedOutlineColor(Color.rgb(233,20,50)); //경로 지나간 테두리 색상
       // path.setHideCollidedSymbols(true); //경로에 겹치는 심볼 숨기기
        //path.setHideCollidedMarkers(true); //경로에 겹치는 마커 숨기기
        path.setMap(naverMap);


    }

}