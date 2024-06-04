package com.example.kimyongjumap;

import com.naver.maps.geometry.LatLng;

public class MarkerInfo {
    private String title;
    private String link;
    private String address;
    private LatLng latLng;
    private String category;
    private String complatedDistance;

    public MarkerInfo(String title, String link, String address, String category, LatLng latLng, String complatedDistance) {
        this.title = title;
        this.link = link;
        this.address = address;
        this.category = category;
        this.latLng = latLng;
        this.complatedDistance = complatedDistance;
    }

    public MarkerInfo(String title, String link, String address, String category, LatLng latLng) {
        this.title = title;
        this.link = link;
        this.address = address;
        this.category = category;
        this.latLng = latLng;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public LatLng getLatLng() {
        return latLng;
    }

    public void setLatLng(LatLng latLng) {
        this.latLng = latLng;
    }

    public String getComplatedDistance() {
        return complatedDistance;
    }

    public void setComplatedDistance(String complatedDistance) {
        this.complatedDistance = complatedDistance;
    }
}
