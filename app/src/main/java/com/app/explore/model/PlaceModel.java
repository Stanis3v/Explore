package com.app.explore.model;

import com.app.explore.realm.table.PlaceRealm;
import com.app.explore.realm.table.StringRealm;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.model.PlaceDetails;
import com.google.maps.model.PlacesSearchResult;

import java.io.Serializable;

import io.realm.RealmList;

public class PlaceModel extends PlacesSearchResult implements Serializable, ClusterItem {

    public String type;

    @Override
    public LatLng getPosition() {
        if (geometry != null) {
            return new LatLng(geometry.location.lat, geometry.location.lng);
        }
        return null;
    }

    public static PlaceModel copy(PlacesSearchResult p) {
        PlaceModel place = new PlaceModel();
        place.formattedAddress = p.formattedAddress;
        place.name = p.name;
        place.geometry = p.geometry;
        place.icon = p.icon;
        place.placeId = p.placeId;
        place.rating = p.rating;
        place.openingHours = p.openingHours;
        place.photos = p.photos;
        place.vicinity = p.vicinity;
        return place;
    }

    public static PlaceModel copyDetails(PlaceDetails p) {
        PlaceModel place = new PlaceModel();
        place.formattedAddress = p.formattedAddress;
        place.name = p.name;
        place.geometry = p.geometry;
        place.icon = p.icon;
        place.placeId = p.placeId;
        place.rating = p.rating;
        place.openingHours = p.openingHours;
        place.photos = p.photos;
        place.vicinity = p.vicinity;
        return place;
    }

    public String getJsonString() {
        Gson gson = new Gson();
        return gson.toJson(this, PlaceModel.class);
    }

    public static PlaceModel getObjectJson(String str) {
        Gson gson = new Gson();
        return gson.fromJson(str, PlaceModel.class);
    }

    public PlaceRealm getObjectRealm() {
        PlaceRealm obj = new PlaceRealm();
        obj.formattedAddress = formattedAddress;
        obj.name = name;
        obj.icon = icon.toString();
        obj.placeId = placeId;
        obj.rating = rating;
        obj.type = type;
        obj.lat = geometry.location.lat;
        obj.lng = geometry.location.lng;

        if (openingHours != null && openingHours.weekdayText != null) {
            obj.weekdayText = new RealmList<>();
            for (int i = 0; i < openingHours.weekdayText.length; i++) {
                obj.weekdayText.add(new StringRealm(openingHours.weekdayText[i]));
            }
        }

        if (photos != null) {
            obj.photoReference = new RealmList<>();
            for (int i = 0; i < photos.length; i++) {
                obj.photoReference.add(new StringRealm(photos[i].photoReference));
            }
        }

        obj.vicinity = vicinity;
        return obj;
    }

}
