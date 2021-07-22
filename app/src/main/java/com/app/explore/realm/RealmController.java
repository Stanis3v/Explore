package com.app.explore.realm;

import android.app.Activity;
import android.app.Application;
import androidx.fragment.app.Fragment;

import com.app.explore.model.PlaceModel;
import com.app.explore.realm.table.PlaceRealm;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

public class RealmController {

    private static RealmController instance;
    private final Realm realm;

    /**
     * Variation RealmController Constructor -----------------------------------------------------
     */
    public RealmController(Application application) {
        realm = Realm.getDefaultInstance();
    }

    public static RealmController with(Fragment fragment) {
        if (instance == null)
            instance = new RealmController(fragment.getActivity().getApplication());
        return instance;
    }

    public static RealmController with(Activity activity) {
        if (instance == null) instance = new RealmController(activity.getApplication());
        return instance;
    }

    public static RealmController with(Application application) {
        if (instance == null) instance = new RealmController(application);
        return instance;
    }

    public static RealmController getInstance() {
        return instance;
    }

    public Realm getRealm() {
        return realm;
    }

    /**
     * Object Post Transaction -----------------------------------------------------------------
     */
    //find all objects
    public List<PlaceModel> getPlace() {
        RealmResults<PlaceRealm> realmResults = realm.where(PlaceRealm.class).findAll();
        realmResults = realmResults.sort("added_date", Sort.DESCENDING);
        List<PlaceModel> newList = new ArrayList<>();
        for (PlaceRealm c : realmResults) {
            newList.add(c.getOriginal());
        }
        return newList;
    }

    //save single object
    public PlaceModel savePlace(PlaceModel obj) {
        realm.beginTransaction();
        PlaceRealm newObj = obj.getObjectRealm();
        newObj.added_date = System.currentTimeMillis(); // set added time now
        newObj = realm.copyToRealmOrUpdate(newObj);
        realm.commitTransaction();
        return newObj != null ? newObj.getOriginal() : null;
    }

    //query get single object by id
    public PlaceModel getPlace(String placeId) {
        PlaceRealm realmObj = realm.where(PlaceRealm.class).equalTo("placeId", placeId).findFirst();
        return realmObj != null ? realmObj.getOriginal() : null;
    }

    //delete object by id
    public void deletePlace(String placeId) {
        realm.beginTransaction();
        realm.where(PlaceRealm.class).equalTo("placeId", placeId).findFirst().deleteFromRealm();
        realm.commitTransaction();
    }

    //check if table is empty
    public boolean hasPlaces() {
        return (realm.where(PlaceRealm.class).findAll().size() > 0);
    }

    //get table size
    public int getPlaceSize() {
        return realm.where(PlaceRealm.class).findAll().size();
    }


}
