package com.example.kimyongjumap;

import android.Manifest;
import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String CLIENT_ID = "pouSA3peBq1eTtbkQJkF"; // 네이버 API 클라이언트 ID
    private static final String CLIENT_SECRET = "KX1cNwBUSQ"; // 네이버 API 클라이언트 시크릿

    private FusedLocationProviderClient mFusedLocationClient;
    private NaverMap mNaverMap;
    private FusedLocationSource mLocationSource;

    private RecyclerView mRecyclerView;
    private RestaurantAdapter mAdapter;
    private DatabaseReference mDatabase;
    private NaverApiService mNaverApiService;

    private boolean isFirstLoad = true;
    private EditText editTextQuery;
    private Button buttonSearch;

    private Marker currentLocationMarker; //currentLocationMarker 현재 위치 마커 초기화와 1km 내에 표시된 마커와의 정보연결을 위해 전역변수로 초기화
    private String myMarkerInformation = "";
    public CircleOverlay mCircleOverlay; //반경 1km 원 생성
    private InfoWindow infoWindow = new InfoWindow(); //아이콘 버튼 클릭시 초기화를 위해 정보창
    private static final String TAG = "MainActivity";
    PathOverlay path = new PathOverlay(); // 경로 정보 초기화
    private List<Marker> markerList = new ArrayList<>(); //다중마커 초기화
    private boolean isInfoWindowOpen = false;
    ImageButton myGPSLocationResult;
    private static OverlayImage overlayImage;
    View.OnClickListener cl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Firebase 초기화
        FirebaseApp.initializeApp(this);

        // 위치 권한 요청
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);

        // Toolbar 설정
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // RecyclerView 초기화
        mRecyclerView = findViewById(R.id.recyclerView);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new RestaurantAdapter();
        mRecyclerView.setAdapter(mAdapter);

        // Firebase Database 초기화
        mDatabase = FirebaseDatabase.getInstance().getReference("restaurants");

        // 네이버 검색 API 서비스 초기화
        Gson gson = new GsonBuilder().create();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(NaverApi.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        mNaverApiService = retrofit.create(NaverApiService.class);

        // 현재 위치 마커 정보 초기화
        infoWindow.setAdapter( new InfoWindow.DefaultTextAdapter(this) {
            @NonNull
            @Override
            public CharSequence getText(@NonNull InfoWindow infoWindow) {
                return "현재 위치";
            }
        });

        // FusedLocationProviderClient 초기화
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // 지도 초기화
        MapFragment mapFragment = (MapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            getSupportFragmentManager().beginTransaction().add(R.id.map, mapFragment).commit();
        }
        mapFragment.getMapAsync(this);

        // 검색창 및 버튼 초기화
        editTextQuery = findViewById(R.id.editTextQuery);
        buttonSearch = findViewById(R.id.buttonSearch);

        // FusedLocationSource 초기화
        mLocationSource = new FusedLocationSource(this, PERMISSION_REQUEST_CODE);

        // 검색 버튼 클릭 리스너 설정
        buttonSearch.setOnClickListener(v -> {
            String query = editTextQuery.getText().toString().trim();
            if (!query.isEmpty()) {
                // 검색 실행
                searchRestaurants(query);
            } else {
                Toast.makeText(MainActivity.this, "검색어를 입력하세요", Toast.LENGTH_SHORT).show();
            }
        });
        myGPSLocationResult = findViewById(R.id.mygpslocation);
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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_category) {
            startActivity(new Intent(this, CategoryActivity.class));
            return true;
        } else if (id == R.id.action_bookmark) {
            startActivity(new Intent(this, BookmarkActivity.class));
            return true;
        } else if (id == R.id.action_mypage) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                startActivity(new Intent(this, MyPageActivity.class));
            } else {
                startActivity(new Intent(this, LoginActivity.class));
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        mNaverMap = naverMap;

        // 위치 소스 및 추적 모드 설정
        mNaverMap.setLocationSource(mLocationSource);
        mNaverMap.setLocationTrackingMode(LocationTrackingMode.Follow);

        NaverMap.OnLocationChangeListener locationChangeListener = location -> {
            LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
            mNaverMap.moveCamera(CameraUpdate.scrollTo(currentLocation));
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() -> {
                currentLocationMarker = new Marker();
                currentLocationMarker.setAlpha(0.7f);
                currentLocationMarker.setIconTintColor(Color.parseColor("#FFD59618"));
                currentLocationMarker.setCaptionText(myMarkerInformation);
                currentLocationMarker.setCaptionOffset(20);
                currentLocationMarker.setCaptionColor(Color.parseColor("#FF4B4B4B"));
                currentLocationMarker.setCaptionTextSize(15);
                currentLocationMarker.setPosition(currentLocation);
                currentLocationMarker.setMap(mNaverMap);
            });
        };

        path.setPatternImage(overlayImage.fromResource(R.drawable.baseline_arrow_drop_up_24));
        path.setPatternInterval(50); //화살표 사이 거리px
        path.setWidth(30); //경로두깨
        path.setColor(Color.WHITE); //경로색상
        path.setPassedColor(Color.GRAY); //지나간 경로 색상
        path.setOutlineColor(Color.parseColor("#FFEA6E21"));// 경로 테두리 색상
        path.setPassedOutlineColor(Color.parseColor("#FF732F05")); //경로 지나간 테두리 색상
        path.setHideCollidedMarkers(true); //경로에 겹치는 마커 숨기기

        // Firebase에서 데이터를 불러와 마커 추가
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                mNaverMap.addOnLocationChangeListener(locationChangeListener);
                // 위치를 받아온 후 위치 추적 리스너를 제거합니다. //초기화
                mNaverMap.removeOnLocationChangeListener(locationChangeListener);
                mylocationMap();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Restaurant restaurant = dataSnapshot.getValue(Restaurant.class);
                    if (restaurant != null) {
                        Log.d("RestaurantData", "Name: " + restaurant.getTitle() + " Lat: " + restaurant.getLatitude() + " Lng: " + restaurant.getLongitude());
                        // 위도와 경도로 변환
                        double latitude = restaurant.getLatitude() / 10.0;
                        double longitude = restaurant.getLongitude() / 10.0;
                        LatLng latLng = new LatLng(latitude, longitude);

                        // 마커 설정
                        Marker marker = new Marker();
                        marker.setPosition(latLng);
                        marker.setMap(mNaverMap);

                        // 마커 클릭 리스너 설정
                        marker.setOnClickListener(overlay -> {
                            showRestaurantInfo(restaurant);
                            return true;
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Failed to load restaurants", error.toException());
            }
        });


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

                //아이콘버튼 클릭으로 다중마커 MarkerCreateTask 생성
                List<MarkerInformationInfo> markerInformation = MarkerCreateTask("맘스터치", currentLocation);
                showMarkers(markerInformation);

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
                        myMarkerInformation = modifiedAddress;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            // 현재 위치에 마커 추가
            currentLocationMarker = new Marker();
            currentLocationMarker.setPosition(currentLocation);
            currentLocationMarker.setAlpha(0.7f);
            currentLocationMarker.setIconTintColor(Color.parseColor("#FFD59618"));
            currentLocationMarker.setCaptionText(myMarkerInformation);
            currentLocationMarker.setCaptionOffset(20);
            currentLocationMarker.setCaptionColor(Color.parseColor("#FF4B4B4B"));
            currentLocationMarker.setCaptionTextSize(15);
            currentLocationMarker.setMap(mNaverMap);

            //현위치를 나타내는 마커구분을 위한 정보창 생성
            infoWindow.open(currentLocationMarker);
        };
        // 위치 추적 리스너 추가
        mNaverMap.addOnLocationChangeListener(locationChangeListener);
        // 위치를 받아온 후 위치 추적 리스너를 제거합니다. //초기화
        mNaverMap.removeOnLocationChangeListener(locationChangeListener);
    }

    public void showMarkers(List<MarkerInformationInfo> markerInfoList) { //지도위에 정보가 담긴 다중마커 생성
        try {
            if( markerList != null) {
                for (Marker marker : markerList) {
                    marker.setMap(null);
                }
                markerList.clear();
            }
            for (MarkerInformationInfo markerInfo : markerInfoList) {
                Marker marker = new Marker();
                marker.setPosition(markerInfo.getLatLng());
                marker.setMap(mNaverMap);
                markerList.add(marker); // 생성된 마커를 전역 리스트에 추가

                InfoWindow infoMarkerWindow = new InfoWindow();
                infoMarkerWindow.setAdapter(new InfoWindow.ViewAdapter() {
                    @NonNull
                    @Override
                    public View getView(@NonNull InfoWindow infoWindow) {
                        View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.marker_infomation, null);
                        TextView fTitle = (TextView) view.findViewById(R.id.getFoodTitle);
                        TextView fAddress = (TextView) view.findViewById(R.id.getFoodAddress);
                        //TextView fCategory = (TextView) view.findViewById(R.id.getFoodCategory);
                        fTitle.setText(markerInfo.getTitle().replace("<b>", " ").replace("</b>", " "));
                        fAddress.setText(markerInfo.getAddress());
                        //fCategory.setText(markerInfo.getCategory());

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

    private void searchRestaurants(String query) {
        Call<SearchResponse> call = mNaverApiService.searchRestaurants(query, 10, 1, "random", CLIENT_ID, CLIENT_SECRET);
        call.enqueue(new Callback<SearchResponse>() {
            @Override
            public void onResponse(@NonNull Call<SearchResponse> call, @NonNull Response<SearchResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d("API Response", response.body().toString());
                    List<Restaurant> restaurants = response.body().getItems();

                    for (Restaurant restaurant : restaurants) {
                        String name = restaurant.getTitle();
                        if (name != null) {
                            name = name.replaceAll("<b>", "").replaceAll("</b>", "");
                            restaurant.setTitle(name);
                        } else {
                            restaurant.setTitle("Unknown Restaurant");
                        }

                        // TM128 좌표를 사용하여 위도와 경도로 변환하지 않고 그대로 저장
                        restaurant.setMapx(restaurant.getMapx());
                        restaurant.setMapy(restaurant.getMapy());

                        // Firebase에 데이터 저장
                        String key = mDatabase.push().getKey();
                        if (key != null) {
                            restaurant.setId(key); // ID 설정
                            mDatabase.child(key).setValue(restaurant).addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Log.d("Firebase", "Restaurant saved: " + restaurant.getTitle());
                                } else {
                                    Log.e("Firebase", "Failed to save restaurant", task.getException());
                                }
                            });
                        }
                    }

                    // RecyclerView에 검색 결과 표시
                    mAdapter.setRestaurants(restaurants);
                } else {
                    Log.e("API Error", response.errorBody().toString());
                    Toast.makeText(MainActivity.this, "검색에 실패했습니다", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<SearchResponse> call, @NonNull Throwable t) {
                Log.e("API Error", "Error fetching data", t);
                Toast.makeText(MainActivity.this, "검색 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showRestaurantInfo(Restaurant restaurant) {
        // 식당 정보 표시를 위한 다이얼로그 또는 액티비티를 구현
        Toast.makeText(this, "식당 이름: " + restaurant.getTitle(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (mLocationSource.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
            if (!mLocationSource.isActivated()) { // 권한 거부됨
                mNaverMap.setLocationTrackingMode(LocationTrackingMode.None);
            }
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private List<MarkerInformationInfo> MarkerCreateTask(String query, LatLng currentLocation) { //백그라운드 (쓰레드) 수행부분
        Log.d(TAG, "MarkerCreateTask: ");
        List<MarkerInformationInfo> markerInfoList = new ArrayList<>();
        try {
            String encodingQuery = URLEncoder.encode(query, "UTF-8");
            Call<SearchResponse> call = mNaverApiService.searchRestaurants(encodingQuery, 10, 1, "random", CLIENT_ID, CLIENT_SECRET);
            call.enqueue(new Callback<SearchResponse>() {
                @Override
                public void onResponse(@NonNull Call<SearchResponse> call, @NonNull Response<SearchResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Log.d("API Response", response.body().toString());
                        List<Restaurant> restaurants = response.body().getItems();

                        for (Restaurant restaurant : restaurants) {
                            String name = restaurant.getTitle();
                            if (name != null) {
                                String title = restaurant.getTitle().replaceAll("<b>", "").replaceAll("</b>", "");
                                String address = restaurant.getAddress();
                                //String category = item.getString("category");  추후 작업 예정
                                double latitude = restaurant.getMapy() / 1000000.0;
                                double longitude = restaurant.getMapx() / 1000000.0;
                                System.out.println("위도" + latitude + "경도" + longitude);
                                LatLng latLng = new LatLng(latitude, longitude);
                                String complatedDistance = placeDistance(calculateDistance(currentLocation, latLng));
                                if (!markerInfoList.contains(address)) {
                                    if (calculateDistance(currentLocation, latLng) <= 1) {
                                        markerInfoList.add(new MarkerInformationInfo(title, address, latLng, complatedDistance));
                                    }
                                }
                            } else {
                                restaurant.setTitle("Unknown Restaurant");
                            }
                        }

                    } else {
                        Log.e(TAG, response.errorBody().toString());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<SearchResponse> call, @NonNull Throwable t) {
                    Log.e(TAG, "NaverMarkerCreateActivity 쓰레드 에러 fetching data", t);
                }
            });
        }catch (Exception e){
            Log.d(TAG, "doInBackground: " + e);
        }
        return markerInfoList;
    }
    public static double calculateDistance(LatLng point1, LatLng point2) { // 현재 내위치와 해당마커 거리 계산
        double earthRadius = 6371.0; // km
        double dLat = Math.toRadians(point2.latitude - point1.latitude);
        double dLng = Math.toRadians(point2.longitude - point1.longitude);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(point1.latitude)) * Math.cos(Math.toRadians(point2.latitude))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        System.out.println("거리계산 결과: " +earthRadius * c);
        return  earthRadius * c;
    }

    public String placeDistance(double distance){ //calculateDistance을 포멧처리해서 사용자가 보기쉬운 형태로 표시
        String roundedDistance = String.format("%.3f", distance);
        double complateDistance = 0;

        if (roundedDistance.startsWith("0.")) {
            complateDistance  = Double.parseDouble(roundedDistance.substring(2));
        } else if(roundedDistance.startsWith("0.0")){
            complateDistance  = Double.parseDouble(roundedDistance.substring(3));
        } else if(roundedDistance.startsWith("0.00")){
            complateDistance  = Double.parseDouble(roundedDistance.substring(4));
        } else {
            complateDistance  = Double.parseDouble(roundedDistance.replace(".", ""));
        }
        if (complateDistance >= 1000) {
            complateDistance = complateDistance / 1000; // m를 km로 변환
            String distanceFormat = String.format("%.1f", complateDistance) + "km"; // 소수점 이하 한 자리까지 표시
            System.out.println("complateDistance 결과: " +complateDistance);
            return distanceFormat;
        }else{
            String distanceFormat = String.format(String.valueOf(complateDistance)).replace(".0","") + "m";
            return distanceFormat;
        }
    }
}
