package com.example.kimyongjumap;
import com.example.kimyongjumap.NaverSearchTask;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import android.location.Address;
import android.location.Geocoder;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;

import android.widget.SearchView;
import com.naver.maps.geometry.Coord;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraUpdate;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, NaverSearchTask.SearchCallback {
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

        Log.d(TAG, "onCreate");
        SearchView sv = (SearchView) findViewById(R.id.searchView);
        final NaverSearchTask naverSearchTask = new NaverSearchTask(this);
        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // MainActivity 인스턴스를 NaverSearchTask 생성자에 전달하지 않고,
                // MainActivity에서 구현한 SearchCallback 인터페이스를 사용하여 콜백을 처리합니다.
                if (naverSearchTask.getStatus() == AsyncTask.Status.RUNNING) {
                    // 실행 중인 경우 취소하고 새로운 객체 생성하여 실행
                    naverSearchTask.cancel(true);
                }
                NaverSearchTask naverSearchTask = new NaverSearchTask(MainActivity.this); // 새로운 객체 생성
                naverSearchTask.execute(query);
                String address = query;
                try{
                    Geocoder geocoder = new Geocoder(MainActivity.this);
                    List<Address> addresses = geocoder.getFromLocationName(address, 1);
                    if (!addresses.isEmpty()) {
                        Address addressResult = addresses.get(0);
                        LatLng latLng = new LatLng(addressResult.getLatitude(), addressResult.getLongitude());
                        mNaverMap.moveCamera(CameraUpdate.scrollTo(latLng)); // 해당 좌표로 지도 이동
                        Marker marker = new Marker();
                        marker.setPosition(latLng); // 마커 위치 설정
                        marker.setMap(mNaverMap); // 마커 지도에 추가
                    } else {
                        Toast.makeText(MainActivity.this, "주소를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException e) { //Unhandled exception: java.io.IOException 에러 해결을 위해 작성
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "주소 변환 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                }
                // 검색 버튼이 클릭되었을 때 호출됨
                // 여기에 네이버 검색 API를 사용하여 검색 결과를 가져오고 표시하는 로직을 추가합니다.
                return true;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                //naverSearchTask.cancel(true); // 기존에 실행 중인 검색 작업을 취소
               // if (!newText.isEmpty()) { // 검색어가 비어있지 않은 경우에만 검색 수행
                //    NaverSearchTask naverSearchTask = new NaverSearchTask(MainActivity.this); // 새로운 검색 작업 생성
               //     naverSearchTask.execute(newText); // 새로운 검색 실행
               // }
                // 검색어가 변경될 때마다 호출됨
                // 실시간 검색 기능을 구현할 수 있습니다.
                return false;
            }
        });

        FragmentManager fm = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment) fm.findFragmentById(R.id.map_fragment);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.map_fragment, mapFragment).commit();
        }

        mapFragment.getMapAsync(this);
    }

    public void onSearchResult(String result) {
        if (result != null) {
            Toast.makeText(MainActivity.this, "검색 결과: " + result, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(MainActivity.this, "검색 결과를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show();
        }
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

        //naverMap.addOnLocationChangeListener(location ->
        //        Toast.makeText(this,
        //                location.getLatitude() + ", " + location.getLongitude(),
        //                Toast.LENGTH_SHORT).show());

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