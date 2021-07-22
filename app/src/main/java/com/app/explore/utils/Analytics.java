package com.app.explore.utils;

import android.app.Activity;
import android.os.Bundle;
import androidx.fragment.app.Fragment;

import com.app.explore.ThisApplication;
import com.google.firebase.analytics.FirebaseAnalytics;

public class Analytics {

    public static void trackActivityScreen(Activity activity) {
        Bundle params = new Bundle();
        params.putString(FirebaseAnalytics.Param.ITEM_NAME, activity.getClass().getSimpleName());
        ThisApplication.firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM, params);

        // custom user property
        ThisApplication.firebaseAnalytics.setUserProperty("ACTIVITY", activity.getClass().getSimpleName());
    }

    public static void trackFragmentScreen(Fragment fragment) {
        Bundle params = new Bundle();
        params.putString(FirebaseAnalytics.Param.ITEM_NAME, fragment.getClass().getSimpleName());
        ThisApplication.firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM, params);

        // custom user property
        ThisApplication.firebaseAnalytics.setUserProperty("FRAGMENT", fragment.getClass().getSimpleName());
    }

    public static void trackAroundDisplayType(String type) {
        // custom user property
        ThisApplication.firebaseAnalytics.setUserProperty("AROUND_TYPE", type);
    }

    public static void trackPlaceDetails(String placeId, String name) {
        Bundle params = new Bundle();
        params.putString(FirebaseAnalytics.Param.ITEM_ID, placeId);
        params.putString(FirebaseAnalytics.Param.ITEM_NAME, name);
        ThisApplication.firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM, params);

        // custom user property
        ThisApplication.firebaseAnalytics.setUserProperty("PLACE_DETAILS", name);
    }

}
