package com.example.kimyongjumap;
import android.os.AsyncTask;
import android.util.Log;

import com.naver.maps.geometry.LatLng;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class NaverSearchTask extends AsyncTask<String, Void, String> {

    private static final String NAVER_CLIENT_ID = "t2onmjtW9uRxLYOHFLlE";
    private static final String NAVER_CLIENT_SECRET = "cMqZPul6Mj";

    private SearchCallback callback;

    public interface SearchCallback {
        void onSearchResult(String result);
    }

    public NaverSearchTask(SearchCallback callback) {
        this.callback = callback;
    }

    @Override
    protected String doInBackground(String... strings) {
        String query = strings[0];
        try {
            String encodedQuery = URLEncoder.encode(query, "UTF-8");
            String apiUrl = "https://openapi.naver.com/v1/search/local.json?query=" + encodedQuery + "&display=500"; //한번의 검색으로 100개를 가져옴

            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("X-Naver-Client-Id", NAVER_CLIENT_ID);
            connection.setRequestProperty("X-Naver-Client-Secret", NAVER_CLIENT_SECRET);
            connection.setConnectTimeout(10000);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                return response.toString();
            } else {
                Log.e("NaverSearchTask", "HTTP 오류 코드: " + responseCode);
                return null;
            }
        } catch (IOException e) {
            Log.e("NaverSearchTask", "네트워크 오류", e);
            return null;
        }
    }
    // 아래 새로 추가
    public static List<MarkerInfo> parseSearchResult(String result) {
        List<MarkerInfo> markerInfoList = new ArrayList<>();
        try {
            JSONObject jsonObject = new JSONObject(result);
            JSONArray items = jsonObject.getJSONArray("items");
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                String title = item.getString("title");
                String link = item.getString("link");
                String address = item.getString("address");
                String category = item.getString("category");
                double latitude = item.getDouble("mapy");
                double longitude = item.getDouble("mapx");
                markerInfoList.add(new MarkerInfo(title, link, address, category, new LatLng(latitude, longitude)));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return markerInfoList;
    }
    @Override
    protected void onPostExecute(String result) {
        if (callback != null) {
            callback.onSearchResult(result);
        }
    }
}