package com.app.explore;

import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.app.explore.data.AppConfig;
import com.app.explore.data.GDPR;
import com.app.explore.data.SharedPref;
import com.app.explore.utils.Analytics;
import com.app.explore.utils.Tools;
import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

public class ActivitySetting extends AppCompatActivity {
    private Toolbar toolbar;
    private ActionBar actionBar;
    private SharedPref sharedPref;
    private TextView tv_radius, tv_build;
    private AdView mAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        sharedPref = new SharedPref(this);
        setupToolbar();

        tv_radius = (TextView) findViewById(R.id.tv_radius);
        tv_build = (TextView) findViewById(R.id.tv_build);
        tv_radius.setText(sharedPref.getRadius() + " " + getString(R.string.kilometer));
        tv_build.setText(Tools.getVersionName(this));

        initAds();
        Analytics.trackActivityScreen(this);
    }

    private void setupToolbar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        // for system bar in lollipop
        Tools.systemBarLolipop(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    private void initAds() {
        mAdView = (AdView) findViewById(R.id.ad_view);
        AdRequest adRequest = new AdRequest.Builder()
                .addNetworkExtrasBundle(AdMobAdapter.class, GDPR.getBundleAd(this))
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR).build();
        mAdView.loadAd(adRequest);
        if (!AppConfig.BANNER_SETTING) {
            mAdView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    public void actionMenu(View view) {
        switch (view.getId()) {
            case R.id.lyt_radius:
                dialogRadius();
                break;
            /**case R.id.lyt_rate:
                Uri uri = Uri.parse("" + getPackageName());
                Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
                try {
                    startActivity(goToMarket);
                } catch (ActivityNotFoundException e) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("" + getPackageName())));
                }
                break;
             */
            case R.id.lyt_about:
                Tools.aboutAction(ActivitySetting.this);
                break;

            case R.id.lyt_term:
                dialogTerm(ActivitySetting.this);
                break;
        }
    }

    protected void dialogRadius() {
        final Dialog dialog = new Dialog(ActivitySetting.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); // before
        dialog.setContentView(R.layout.dialog_radius);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        final Button button_ok = (Button) dialog.findViewById(R.id.button_ok);
        final Button button_no = (Button) dialog.findViewById(R.id.button_no);
        final EditText et_radius = (EditText) dialog.findViewById(R.id.et_radius);
        et_radius.setText(sharedPref.getRadius() + "");
        button_no.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        button_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int radius = Integer.parseInt(et_radius.getText().toString());
                if (radius > 50) {
                    Toast.makeText(getApplicationContext(), R.string.max_radius_message, Toast.LENGTH_SHORT).show();
                    return;
                }
                sharedPref.setRadius(radius);
                tv_radius.setText(sharedPref.getRadius() + " " + getString(R.string.kilometer));
                dialog.dismiss();
            }
        });
        dialog.show();
        dialog.getWindow().setAttributes(lp);
    }

    public void dialogTerm(Activity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(activity.getString(R.string.term_condition));
        builder.setMessage(activity.getString(R.string.content_term));
        builder.setPositiveButton(getString(R.string.OK), null);
        builder.show();
    }
}
