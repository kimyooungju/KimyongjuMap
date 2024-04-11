package com.example.kimyongjumap;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

public interface NaverApiService {
    @GET("/v1/search/blog")
    Call<NaverSearchResponse> getBlogSearchResults(
            @Header("X-Naver-Client-Id") String clientId,
            @Header("X-Naver-Client-Secret") String clientSecret,
            @Query("query") String query
    );
}