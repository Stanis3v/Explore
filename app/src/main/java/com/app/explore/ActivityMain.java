package com.app.explore;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.IdRes;
import com.google.android.material.navigation.NavigationView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.app.explore.data.AppConfig;
import com.app.explore.data.Constant;
import com.app.explore.data.GDPR;
import com.app.explore.data.SharedPref;
import com.app.explore.fragment.FragmentAround;
import com.app.explore.fragment.FragmentDirection;
import com.app.explore.fragment.FragmentFavorites;
import com.app.explore.fragment.FragmentFind;
import com.app.explore.realm.RealmController;
import com.app.explore.utils.Analytics;
import com.app.explore.utils.Network;
import com.app.explore.utils.Tools;
import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;

public class ActivityMain extends AppCompatActivity {

    //for ads
    private InterstitialAd mInterstitialAd;

    private Toolbar toolbar;
    private ActionBar actionBar;
    private RelativeLayout nav_header_lyt;
    private SharedPref sharedPref;
    private Fragment fragment = null;
    private NavigationView navigationView;
    private View parent_view;

    static ActivityMain activityMain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        parent_view = findViewById(android.R.id.content);

        activityMain = this;
        sharedPref = new SharedPref(this);

        setupToolbar();
        setupDrawerMenu();

        // display first fragment
        onNavigationSelected(R.id.nav_find, getString(R.string.str_nav_find));

        prepareAds();
        Analytics.trackActivityScreen(this);
    }

    public static ActivityMain getInstance() {
        return activityMain;
    }

    private void setupToolbar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setTitle(getString(R.string.app_name));
        // for system bar in lollipop
        Tools.systemBarLolipop(this);
    }

    private int itemId = 0;
    private String itemTitle = "";

    private void setupDrawerMenu() {
        navigationView = (NavigationView) findViewById(R.id.nav_view);
        final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            @Override
            public void onDrawerClosed(View drawerView) {
                onNavigationSelected(itemId, itemTitle);
                super.onDrawerClosed(drawerView);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                showInterstitial();
                updateFavoritesCounter(navigationView, R.id.nav_favorites, RealmController.with(ActivityMain.this).getPlaceSize());
                super.onDrawerOpened(drawerView);
            }
        };
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(final MenuItem item) {
                drawer.closeDrawers();
                itemId = item.getItemId();
                itemTitle = item.getTitle().toString();
                return true;
            }
        });

        // navigation header
        View nav_header = navigationView.getHeaderView(0);
        nav_header_lyt = (RelativeLayout) nav_header.findViewById(R.id.nav_header_lyt);
    }

    private void prepareAds() {
        if (AppConfig.ENABLE_GDPR) GDPR.updateConsentStatus(this);
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getString(R.string.interstitial_ad_unit_id));
        AdRequest adRequest2 = new AdRequest.Builder()
                .addNetworkExtrasBundle(AdMobAdapter.class, GDPR.getBundleAd(this)).build();
        mInterstitialAd.loadAd(adRequest2);
    }

    /**
     * show ads
     */
    public void showInterstitial() {
        // Show the ad if it's ready
        if (AppConfig.INTERSTITIAL_MAIN && mInterstitialAd != null && mInterstitialAd.isLoaded()) {
            mInterstitialAd.show();
        }
    }

    @Override
    protected void onResume() {
        updateFavoritesCounter(navigationView, R.id.nav_favorites, RealmController.with(ActivityMain.this).getPlaceSize());
        if (!Network.hasInternet(this)) {
            Network.noConnectionSnackBar(this, parent_view);
        }
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (!drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.openDrawer(GravityCompat.START);
        } else {
            doExitApp();
        }
    }

    private long exitTime = 0;

    public void doExitApp() {
        if ((System.currentTimeMillis() - exitTime) > 2000) {
            Toast.makeText(this, R.string.press_again_exit_app, Toast.LENGTH_SHORT).show();
            exitTime = System.currentTimeMillis();
        } else {
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent i = new Intent(getApplicationContext(), ActivitySetting.class);
            startActivity(i);



        } else if (id == R.id.action_about) {
            Tools.aboutAction(ActivityMain.this);

        }

        return super.onOptionsItemSelected(item);
    }

    public void onNavigationSelected(int id, String title) {

        if (title.equals(actionBar.getTitle())) return;

        if (id == R.id.nav_find) {
            actionBar.setTitle(title);
            fragment = new FragmentFind();

        } else if (id == R.id.nav_direction) {
            actionBar.setTitle(title);
            fragment = new FragmentDirection();

        } else if (id == R.id.nav_around) {
            actionBar.setTitle(title);
            fragment = new FragmentAround();

        } else if (id == R.id.nav_favorites) {
            actionBar.setTitle(title);
            fragment = new FragmentFavorites();

        } else if (id == R.id.nav_setting) {
            Intent i = new Intent(getApplicationContext(), ActivitySetting.class);
            startActivity(i);



        } else if (id == R.id.nav_more_app) {
            Tools.directUrl(this, Constant.MORE_APP_URL + "?t=" + System.currentTimeMillis());

        } else if (id == R.id.nav_about) {
            Tools.aboutAction(ActivityMain.this);

        }

        if (fragment != null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.frame_content, fragment);
            fragmentTransaction.commit();
        }
    }

    private void updateFavoritesCounter(NavigationView nav, @IdRes int itemId, int count) {
        TextView view = (TextView) nav.getMenu().findItem(itemId).getActionView().findViewById(R.id.counter);
        view.setText(String.valueOf(count));
    }

}
