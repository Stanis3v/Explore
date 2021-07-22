package com.app.explore;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.app.explore.data.Constant;
import com.app.explore.data.SharedPref;
import com.app.explore.utils.Analytics;
import com.app.explore.utils.PermissionUtil;
import com.app.explore.utils.Tools;

import java.util.Timer;
import java.util.TimerTask;

public class ActivitySplash extends AppCompatActivity {

    private SharedPref sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        sharedPref = new SharedPref(this);
        Analytics.trackActivityScreen(this);

        if (Tools.needRequestPermission()) {
            String[] permission = PermissionUtil.getDeniedPermission(this);
            if (permission.length != 0) {
                requestPermissions(permission, Constant.DEFAULT_PERMISSION_CODE);
            } else {
                startActivityMain();
            }
        } else {
            startActivityMain();
        }
    }

    private void startActivityMain() {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                Intent i = new Intent(ActivitySplash.this, ActivityMain.class);
                startActivity(i);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
            }
        };
        new Timer().schedule(task, Constant.SPLASH_SCREEN_DELAY);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == Constant.DEFAULT_PERMISSION_CODE) {
            for (int i = 0; i < permissions.length; i++) {
                boolean rationale = shouldShowRequestPermissionRationale(permissions[i]);
                sharedPref.setNeverAskAgain(permissions[i], !rationale);
            }
            startActivityMain();
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
