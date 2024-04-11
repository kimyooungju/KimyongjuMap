package com.example.kimyongjumap;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class NaverSearchTask extends AsyncTask<String, Void, String> {

    private static final String NAVER_CLIENT_ID = "l0dlx2ru50";
    private static final String NAVER_CLIENT_SECRET = "cAwEIt8O2kuQ3F9Z3133f2qkxAg29wwddwL9uOBO";

    private SearchCallback callback;

    public interface SearchCallback {
        void onSearchResult(String result);
    }

    public NaverSearchTask(SearchCallback callback) {
        this.callback = callback;
    }

    @Override
    protected String doInBackground(String... params) {
        String query = params[0];
        try {
            String encodedQuery = URLEncoder.encode(query, "UTF-8");
            String apiUrl = "https://openapi.naver.com/v1/search/blog?query=" + encodedQuery;

            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("X-Naver-Client-Id", NAVER_CLIENT_ID);
            connection.setRequestProperty("X-Naver-Client-Secret", NAVER_CLIENT_SECRET);

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

    @Override
    protected void onPostExecute(String result) {
        if (callback != null) {
            callback.onSearchResult(result);
        }
    }
}