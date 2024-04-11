package com.example.kimyongjumap;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class NaverSearchResponse {
    @SerializedName("items")
    private List<BlogItem> items;

    public List<BlogItem> getItems() {
        return items;
    }

    public class BlogItem {
        @SerializedName("title")
        private String title;

        @SerializedName("link")
        private String link;

        public String getTitle() {
            return title;
        }

        public String getLink() {
            return link;
        }
    }
}
