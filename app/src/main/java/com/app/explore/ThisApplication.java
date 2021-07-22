package com.app.explore;

import android.app.Application;

import com.app.explore.data.AppConfig;
import com.google.android.gms.ads.MobileAds;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.firebase.analytics.FirebaseAnalytics;

import io.realm.Realm;
import io.realm.RealmConfiguration;

public class ThisApplication extends Application {

    private static ThisApplication mInstance;
    public static FirebaseAnalytics firebaseAnalytics;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;

        MobileAds.initialize(this, getResources().getString(R.string.admob_app_id));

        // Initialize Places.
        Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));

        // Create a new Places client instance.
        PlacesClient placesClient = Places.createClient(this);

        // init realm database
        initRealmDatabase();

        // init property firebase analytics
        initFirebaseAnalytics();

    }

    private void initRealmDatabase() {
        Realm.init(this);
        RealmConfiguration realmConfiguration = new RealmConfiguration.Builder()
                .name("my.places.realm")
                .schemaVersion(0)
                .deleteRealmIfMigrationNeeded()
                .build();
        Realm.setDefaultConfiguration(realmConfiguration);
    }

    private void initFirebaseAnalytics() {
        firebaseAnalytics = FirebaseAnalytics.getInstance(this);
        //Sets whether analytics collection is enabled for this app on this device.
        firebaseAnalytics.setAnalyticsCollectionEnabled(AppConfig.FIREBASE_ANALYTICS);
    }

    public static synchronized ThisApplication getInstance() {
        return mInstance;
    }
}
