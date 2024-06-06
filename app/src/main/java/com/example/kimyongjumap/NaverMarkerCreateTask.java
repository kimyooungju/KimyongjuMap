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
                String query = URLEncoder.encode("기찬닭발", "UTF-8");
                String apiUrl = "https://openapi.naver.com/v1/search/local.json?query="+query+"&display=5"; //한번의 검색으로 1000개를 가져옴

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
                        System.out.println("위도" + latitude + "경도" + longitude);
                        LatLng latLng = new LatLng(latitude, longitude);
                        String complatedDistance = placeDistance(calculateDistance(currentLocation, latLng));
                        if(!markerInfoList.contains(address)) {
                            if (calculateDistance(currentLocation, latLng) <= 1) {
                                markerInfoList.add(new MarkerInfo(title, link, address, category, latLng, complatedDistance));
                            }
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
        try {
            if (context instanceof MainActivity) {
                ((MainActivity) context).showMarkers(markerInfoList);
            }
        }catch(Exception e){
            Log.e(" onPostExecute", "매인 스레드 에러발생", e);
        }
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
        int typeConversion = 0;

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