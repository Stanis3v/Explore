package com.app.explore.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.android.gms.maps.GoogleMap;

public class SharedPref {

    private Context ctx;
    private SharedPreferences custom_preferences;

    public SharedPref(Context context) {
        this.ctx = context;
        custom_preferences = context.getSharedPreferences("MAIN_PREF", Context.MODE_PRIVATE);
    }

    private String str(int string_id) {
        return ctx.getString(string_id);
    }

    public void setRadius(int radius) {
        custom_preferences.edit().putInt("KEY_RADIUS", radius).apply();
    }

    public int getRadius() {
        return custom_preferences.getInt("KEY_RADIUS", 5);
    }


    public void setMapType(int type) {
        custom_preferences.edit().putInt("KEY_MAP_TYPE", type).apply();
    }

    public int getMapType() {
        return custom_preferences.getInt("KEY_MAP_TYPE", GoogleMap.MAP_TYPE_NORMAL);
    }

    public void setNeverAskAgain(String key, boolean value){
        custom_preferences.edit().putBoolean(key, value).apply();
    }

    public boolean getNeverAskAgain(String key){
        return custom_preferences.getBoolean(key, false);
    }

}
