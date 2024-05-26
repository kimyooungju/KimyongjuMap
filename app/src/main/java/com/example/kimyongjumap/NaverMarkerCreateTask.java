package com.example.kimyongjumap;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.naver.maps.geometry.LatLng;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class NaverMarkerCreateTask extends AsyncTask<Void, Void, List<MarkerInfo>> {
    private static  final String TAG = "NaverMarkerCreateTask";
    private static final String NAVER_CLIENT_ID = "t2onmjtW9uRxLYOHFLlE";
    private static final String NAVER_CLIENT_SECRET = "cMqZPul6Mj";
    private Context context;
    private LatLng currentLocation;

    public NaverMarkerCreateTask(Context context, LatLng currentLocation) {
        this.context = context;
        this.currentLocation = currentLocation;
    }

    @Override
    protected List<MarkerInfo> doInBackground(Void... voids) {
        Log.d(TAG, "doInBackground");
        List<MarkerInfo> markerInfoList = new ArrayList<>();
        try {
            String query = URLEncoder.encode("맘스터치", "UTF-8");
            String apiUrl = "https://openapi.naver.com/v1/search/local.json?query="+query+"&display=100"; //한번의 검색으로 1000개를 가져옴

            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("X-Naver-Client-Id", NAVER_CLIENT_ID);
            connection.setRequestProperty("X-Naver-Client-Secret", NAVER_CLIENT_SECRET);
            connection.setConnectTimeout(10000);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONObject jsonObject = new JSONObject(response.toString());
                JSONArray items = jsonObject.getJSONArray("items");

                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.getJSONObject(i);
                    String title = item.getString("title");
                    String link = item.getString("link");
                    String address = item.getString("address");
                    String category = item.getString("category");
                    String JSONlatitude = item.getString("mapy");
                    String JSONlongitude = item.getString("mapx");
                    double longitude = Double.parseDouble(JSONlongitude.substring(0, 3) + "." + JSONlongitude.substring(3));
                    double latitude = Double.parseDouble(JSONlatitude.substring(0, 2) + "." + JSONlatitude.substring(2));
                    System.out.println("위도" +latitude+ "경도"+longitude );
                    LatLng latLng = new LatLng(latitude, longitude);

                    if (isWithin1Km(currentLocation.latitude, currentLocation.longitude, latitude, longitude)) {
                        markerInfoList.add(new MarkerInfo(title, link, address, category, latLng));
                    }
                }
            }
        } catch (Exception e) {
            Log.e("NaverMarkerCreateTask", "백그라운드 에러발생", e);
        }
        return markerInfoList;
    }

    @Override
    protected void onPostExecute(List<MarkerInfo> markerInfoList) {
        if (context instanceof MainActivity) {
            ((MainActivity) context).showMarkers(markerInfoList);
        }
    }
    private boolean isWithin1Km(double lat1, double lon1, double lat2, double lon2) {
        // 위도 1km에 해당하는 변화량
        double latDelta = 0.0091;
        // 경도 1km에 해당하는 변화량
        double lonDelta = 0.0113;

        // 두 지점 간의 직선 거리 계산
        double latDistance = Math.abs(lat2 - lat1);
        double lonDistance = Math.abs(lon2 - lon1);

        // 1km 이내에 있는지 확인
        return latDistance <= latDelta && lonDistance <= lonDelta;
    }
}