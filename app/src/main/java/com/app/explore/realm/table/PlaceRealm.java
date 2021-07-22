package com.app.explore.realm.table;

import com.app.explore.model.PlaceModel;
import com.google.maps.model.Geometry;
import com.google.maps.model.LatLng;
import com.google.maps.model.OpeningHours;
import com.google.maps.model.Photo;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class PlaceRealm extends RealmObject implements Serializable {

    @PrimaryKey
    public String placeId;
    public String type;
    public String formattedAddress;
    public String name;
    public String icon;
    public float rating;
    public double lat;
    public double lng;
    public RealmList<StringRealm> weekdayText;
    public RealmList<StringRealm> photoReference;
    public String vicinity;

    public long added_date = 0;

    public PlaceModel getOriginal() {
        PlaceModel p = new PlaceModel();
        p.placeId = placeId;
        p.type = type;
        p.formattedAddress = formattedAddress;
        p.name = name;
        try {
            p.icon = new URL(icon);
        } catch (MalformedURLException e) {
        }
        p.rating = rating;
        p.geometry = new Geometry();
        p.geometry.location = new LatLng(lat, lng);

        if (weekdayText != null) {
            p.openingHours = new OpeningHours();
            p.openingHours.weekdayText = new String[weekdayText.size()];
            for (int i = 0; i < weekdayText.size(); i++) {
                p.openingHours.weekdayText[i] = weekdayText.get(i).stringValue;
            }
        }

        if (photoReference != null) {
            p.photos = new Photo[photoReference.size()];
            for (int i = 0; i < p.photos.length; i++) {
                p.photos[i] = new Photo();
                p.photos[i].photoReference = photoReference.get(i).stringValue;
            }
        }

        p.vicinity = vicinity;
        return p;
    }

}
