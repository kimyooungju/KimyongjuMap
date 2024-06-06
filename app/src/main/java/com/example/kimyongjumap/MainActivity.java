package com.example.kimyongjumap;


import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;

import android.location.Address;
import android.location.Geocoder;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;

import android.view.LayoutInflater;
import android.view.View;

import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import android.widget.ImageButton;
import android.widget.SearchView;


import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.CircleOverlay;
import com.naver.maps.map.overlay.InfoWindow;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.overlay.PathOverlay;
import com.naver.maps.map.util.FusedLocationSource;
import android.Manifest;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, NaverSearchTask.SearchCallback {
    PathOverlay path = new PathOverlay();
    private static final String TAG = "MainActivity";
    private static OverlayImage overlayImage;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };
    private FusedLocationSource mLocationSource;
    private NaverMap mNaverMap;
    private SearchView mSearchView;
    private NaverSearchTask mNaverSearchTask;
    private AutoCompleteTextView autoCompleteTextView;
    private List<String> autoCompleteList = new ArrayList<>();
    // 자동 완성 결과 리스트
    private boolean isInfoWindowOpen = false;
    private List<Marker> markerList = new ArrayList<>();
    private Marker currentLocationMarker; // 현재 위치 마커를 클래스 필드로 선언
    View.OnClickListener cl;
    private InfoWindow infoWindow = new InfoWindow();
    public CircleOverlay mCircleOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        mLocationSource =
                new FusedLocationSource(this, PERMISSION_REQUEST_CODE);
        setContentView(R.layout.activity_main);

        autoCompleteTextView = (AutoCompleteTextView) findViewById(R.id.searchAutoComplete);
        mSearchView = (SearchView) findViewById(R.id.searchView);

        ImageButton myGPSLocationResult = (ImageButton) findViewById(R.id.mygpslocation);
        infoWindow.setAdapter( new InfoWindow.DefaultTextAdapter(this) {
            @NonNull
            @Override
            public CharSequence getText(@NonNull InfoWindow infoWindow) {
                return "현재 위치";
            }
        });

        cl = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int id = v.getId();
                if ( id == R.id.mygpslocation){
                    mylocationMap();
                }
            }
        };
        myGPSLocationResult.setOnClickListener(cl);

        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(TAG, "onQueryTextSubmit");
                if(autoCompleteTextView != null){
                    autoCompleteTextView.setText(""); //매번 수행마다 텍스트초기화
                }
                autoCompleteTextView.setTextColor(Color.TRANSPARENT); //텍스트 투명화
                autoCompleteList.clear(); //자동완성 리스트초기화
                handleSearchQuery(query);
                // 검색 버튼이 클릭되었을 때 호출됨
                // 여기에 네이버 검색 API를 사용하여 검색 결과를 가져오고 표시하는 로직을 추가합니다.
                return true;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                // 검색어가 변경될 때마다 호출됨
                // 실시간 검색 기능을 구현할 수 있습니다.
                return false;
            }
        });

        autoCompleteTextView.addTextChangedListener(new TextWatcher() { //문장자동완성 기능을 수행하는 코드
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // 입력 전에 수행할 작업
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 입력이 변경될 때마다 호출되는 메서드
                // false는 검색어 제출을 막음
                mNaverSearchTask = new NaverSearchTask(MainActivity.this);
                mNaverSearchTask.execute(s.toString());
                mSearchView.setQuery(autoCompleteTextView.getText().toString(), false);
            }

            @Override
            public void afterTextChanged(Editable s) {
                // 입력 후에 수행할 작업 //포커스가 EditText를 벗어날때 수행
                // 입력이 변경될 때마다 NaverSearchTask 실행
            }
        });

        FragmentManager fm = getSupportFragmentManager();  //현제 액티비티의 fragmentManager을 가져옴
        MapFragment mapFragment = (MapFragment) fm.findFragmentById(R.id.map_fragment); //연결/ 변수mapFragment에 Naver에서 제공되는 지도api를 담음
        if (mapFragment == null) { //null이면 현제 액티비티에 추가되지 않은상태일때 실행
            mapFragment = MapFragment.newInstance(); //새로운 mapFragment인스턴스를 생성
            fm.beginTransaction().add(R.id.map_fragment, mapFragment).commit(); //트렌직션을 실행, 해당되는 레이아웃을 추가하고 커밋
        }
        mapFragment.getMapAsync(this);
    }

    private void mylocationMap(){ // 아이콘버튼을 클릭하면 수행되는 현재위치 초기화 메서드
        // GPS를 사용하는 사용자의 기기가 위치변동이 있으면 변동된 위치를 표시
        NaverMap.OnLocationChangeListener locationChangeListener = location -> {
            LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
            mNaverMap.moveCamera(CameraUpdate.scrollTo(currentLocation));
            infoWindow.close(); // 기존 마커에 연결된 정보창 닫기
            if (currentLocationMarker != null) {
                currentLocationMarker.setMap(null); // 기존 마커 제거
            }

            if (location != null) { // 내위치를 중심으로 반경 1km원 생성
                if(mCircleOverlay != null){
                    mCircleOverlay.setMap(null);
                }
                mCircleOverlay = new CircleOverlay();
                mCircleOverlay.setCenter(currentLocation);
                mCircleOverlay.setRadius(1000); // 반경 1km (미터 단위)
                mCircleOverlay.setColor(Color.argb(20, 0, 0, 255)); // 투명 파란색
                mCircleOverlay.setMap(mNaverMap);

                // API 호출을 위한 NaverMarkerCreateTask 실행
                new NaverMarkerCreateTask(this, currentLocation).execute();
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> addresses;
                try {
                    addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        String address = addresses.get(0).getAddressLine(0);
                        Log.d(TAG, "현재 위치의 주소: " + address);
                        String firstDelAdd = address.replaceFirst("^.*?\\s+", "");
                        String[] addressParts = firstDelAdd.split("\\s+", 4);
                        String modifiedAddress = addressParts[0] + " " + addressParts[1] + " " + addressParts[2];
                        Log.d(TAG,"내위치 주소: "+modifiedAddress);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            // 현재 위치에 마커 추가
            currentLocationMarker = new Marker();
            currentLocationMarker.setPosition(currentLocation);
            currentLocationMarker.setAlpha(0.7f);
            currentLocationMarker.setMap(mNaverMap);

            //현위치를 나타내는 마커구분을 위한 정보창 생성
            infoWindow.open(currentLocationMarker);
        };
        // 위치 추적 리스너 추가
        mNaverMap.addOnLocationChangeListener(locationChangeListener);
        // 위치를 받아온 후 위치 추적 리스너를 제거합니다. //초기화
        mNaverMap.removeOnLocationChangeListener(locationChangeListener);
    }

    private void handleSearchQuery(String query) {// 자신의 위치로 이동후 마커생성
        // MainActivity 인스턴스를 NaverSearchTask 생성자에 전달하지 않고,
        // MainActivity에서 구현한 SearchCallback 인터페이스를 사용하여 콜백을 처리합니다.
        try{
            Geocoder geocoder = new Geocoder(MainActivity.this);
            List<Address> addresses = geocoder.getFromLocationName(query, 1);
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
    }

    public void onSearchResult(String result) { //NaverSearchTask에서 응답받는쪽
        if (result != null) {
            try {
                // 검색 결과를 파싱하고 AutoCompleteTextView에 표시
                autoCompleteList.clear();
                JSONObject jsonObject = new JSONObject(result);  //검색문장 자동완성, 주소기반
                JSONArray items = jsonObject.getJSONArray("items");
                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.getJSONObject(i);
                    String title = item.getString("title").replace("<b>", " ").replace("</b>", " ");
                    String address = item.getString("address");
                    String combined = title + " 주소: " + address; // 식당 이름과 주소를 합
                    if (combined.contains(mSearchView.getQuery().toString())){
                        if (!autoCompleteList.contains(combined)) { // 중복되지 않은 주소만 추가
                            autoCompleteList.add(combined);
                        }
                    }
                }
                ArrayAdapter<String> autoCompleteAdapter = new ArrayAdapter<>(
                        MainActivity.this, R.layout.line2_dropdown, autoCompleteList);
                autoCompleteTextView.setAdapter(autoCompleteAdapter);
            } catch (Exception e) {
                Log.e(TAG, "검색 결과 파싱 오류", e);
                Toast.makeText(MainActivity.this, "검색 결과 파싱 오류", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e(TAG, "검색 결과가 null입니다.");
            Toast.makeText(MainActivity.this, "검색결과가 없습니다..", Toast.LENGTH_SHORT).show();
        }
    }

    public void showMarkers(List<MarkerInfo> markerInfoList) { //지도위에 정보가 담긴 다중마커 생성
        try {
            if( markerList != null) {
                for (Marker marker : markerList) {
                    marker.setMap(null);
                }
                markerList.clear();
            }
            for (MarkerInfo markerInfo : markerInfoList) {
                Marker marker = new Marker();
                marker.setPosition(markerInfo.getLatLng());
                marker.setMap(mNaverMap);
                markerList.add(marker); // 생성된 마커를 전역 리스트에 추가

                InfoWindow infoWindow = new InfoWindow();
                infoWindow.setAdapter(new InfoWindow.ViewAdapter() {
                    @NonNull
                    @Override
                    public View getView(@NonNull InfoWindow infoWindow) {
                        View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.marker_infomation, null);
                        TextView fTitle = (TextView) view.findViewById(R.id.getFoodTitle);
                        TextView fAddress = (TextView) view.findViewById(R.id.getFoodAddress);
                        TextView fCategory = (TextView) view.findViewById(R.id.getFoodCategory);
                        fTitle.setText(markerInfo.getTitle().replace("<b>", " ").replace("</b>", " "));
                        fAddress.setText(markerInfo.getAddress());
                        fCategory.setText(markerInfo.getCategory());

                        return view;
                    }
                });
                NaverMap.OnLocationChangeListener locationChangeListener = location -> {//현재 내위치와 markerinfo의 위치를 경로선 표현
                    LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    path.setCoords(Arrays.asList(
                            currentLocation,
                            markerInfo.getLatLng()
                    ));
                };
                // 마커에 클릭 이벤트 설정
                marker.setOnClickListener((overlay) -> {
                    if (isInfoWindowOpen == false) { // 정보창 상태를 닫힘에서 열림으로 변경합니다.
                        mNaverMap.addOnLocationChangeListener(locationChangeListener);
                        mNaverMap.removeOnLocationChangeListener(locationChangeListener);
                        path.setMap(mNaverMap);
                        marker.setCaptionOffset(20);
                        marker.setCaptionColor(Color.parseColor("#FFEA6E21"));
                        marker.setCaptionTextSize(15);
                        marker.setSubCaptionColor(Color.parseColor("#FF3982CE"));
                        marker.setSubCaptionTextSize(12);
                        marker.setSubCaptionText("음식점과 거리");
                        marker.setCaptionText(markerInfo.getComplatedDistance());
                        infoWindow.open(marker); // 정보창을 엽니다.
                        isInfoWindowOpen = true;
                    } else { // 정보창 상태를 열림에서 닫힘으로 변경합니다.
                        path.setMap(null);
                        marker.setCaptionText(null);
                        infoWindow.close(); // 정보창을 닫습니다.
                        isInfoWindowOpen = false;
                    }
                    return true; // 클릭 이벤트를 소비했음을 반환
                });
            }
        }catch(Exception e){
            Log.d(TAG, "showMarkers : 마커 메서드 에러", e);
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
        naverMap.setLocationSource(mLocationSource); //naverMap 객체에 위치 리소스를 지정
        // 권한 확인, onRequestPermissionsResult 콜백 메서드 호출
        ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_REQUEST_CODE);
        naverMap.setLocationTrackingMode(LocationTrackingMode.Follow); //지도에서 위치 추적모드 설정
        NaverMap.OnLocationChangeListener locationChangeListener = location -> {
            LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
            mNaverMap.moveCamera(CameraUpdate.scrollTo(currentLocation));

            // 현재 위치에 마커 추가
            currentLocationMarker = new Marker();
            currentLocationMarker.setPosition(currentLocation);
            currentLocationMarker.setAlpha(0.7f);
            currentLocationMarker.setMap(mNaverMap);

            //현위치를 나타내는 마커구분을 위한 정보창 생성
            infoWindow.open(currentLocationMarker);
        };
        // 위치 추적 리스너 추가
        naverMap.addOnLocationChangeListener(locationChangeListener);
        // 위치를 받아온 후 위치 추적 리스너를 제거합니다.
        naverMap.removeOnLocationChangeListener(locationChangeListener);

        path.setPatternImage(overlayImage.fromResource(R.drawable.baseline_arrow_drop_up_24));
        path.setPatternInterval(50); //화살표 사이 거리px
        path.setWidth(30); //경로두깨
        path.setColor(Color.WHITE); //경로색상
        path.setPassedColor(Color.GRAY); //지나간 경로 색상
        path.setOutlineColor(Color.parseColor("#FFEA6E21"));// 경로 테두리 색상
        path.setPassedOutlineColor(Color.parseColor("#FF732F05")); //경로 지나간 테두리 색상
        path.setHideCollidedMarkers(true); //경로에 겹치는 마커 숨기기
    }
}