package com.app.explore.model;

import com.google.android.gms.maps.model.LatLng;

public class UserLoc {

    public double lat = 0;
    public double lng = 0;

    public UserLoc() {
    }

    public UserLoc(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
    }

    public LatLng getPosition() {
        return new LatLng(lat, lng);
    }
}
