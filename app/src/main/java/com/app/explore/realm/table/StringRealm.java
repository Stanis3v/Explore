package com.app.explore.realm.table;

import io.realm.RealmObject;

public class StringRealm extends RealmObject {
    public String stringValue;

    public StringRealm() {
    }

    public StringRealm(String stringValue) {
        this.stringValue = stringValue;
    }
}
