package com.app.explore;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.viewpager.widget.PagerAdapter;
import androidx.core.view.ViewCompat;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatRatingBar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.app.explore.adapter.AdapterReviews;
import com.app.explore.data.AppConfig;
import com.app.explore.data.Constant;
import com.app.explore.data.GDPR;
import com.app.explore.model.PlaceModel;
import com.app.explore.realm.RealmController;
import com.app.explore.utils.Analytics;
import com.app.explore.utils.Network;
import com.app.explore.utils.Tools;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.gson.Gson;
import com.google.maps.GeoApiContext;
import com.google.maps.PendingResult;
import com.google.maps.PlaceDetailsRequest;
import com.google.maps.PlacesApi;
import com.google.maps.model.Photo;
import com.google.maps.model.PlaceDetails;

import java.util.concurrent.TimeUnit;

public class ActivityPlaceDetail extends AppCompatActivity {

    private static final String EXTRA_OBJ = "EXTRA_OBJ";

    // give preparation animation activity transition
    public static void navigate(AppCompatActivity activity, View view, String str) {
        Intent intent = new Intent(activity, ActivityPlaceDetail.class);
        intent.putExtra(EXTRA_OBJ, str);
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity, view, EXTRA_OBJ);
        ActivityCompat.startActivity(activity, intent, options.toBundle());
    }

    private PlaceModel place = null;
    private PlaceDetails place_details = null;
    private View parent_view = null;
    private ProgressBar progressBar;
    private FloatingActionButton fab;

    private LinearLayout dotsLayout;
    private TextView[] dots;
    private boolean flag_favorite;
    private MenuItem favorite_menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_detail);
        parent_view = findViewById(android.R.id.content);

        // animation transition
        ViewCompat.setTransitionName(findViewById(R.id.nested_content), EXTRA_OBJ);
        place = new Gson().fromJson(getIntent().getStringExtra(EXTRA_OBJ), PlaceModel.class);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        fab = (FloatingActionButton) findViewById(R.id.fab);

        setupToolbar();
        loadPlaceDetail();
        initAds();
        Analytics.trackActivityScreen(this);
        Analytics.trackPlaceDetails(place.placeId, place.name);
    }

    private void loadPlaceDetail() {
        progressBar.setVisibility(View.VISIBLE);
        GeoApiContext context = new GeoApiContext().setApiKey(getString(R.string.google_maps_key));
        context.setConnectTimeout(Constant.TIMEOUT_PLACE_DETAIL, TimeUnit.SECONDS);
        PlaceDetailsRequest p = PlacesApi.placeDetails(context, place.placeId);
        p.setCallback(new PendingResult.Callback<PlaceDetails>() {
            @Override
            public void onResult(final PlaceDetails result) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        displayData(result);
                        progressBar.setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void onFailure(final Throwable e) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                        showSnackBarRetry(e.getMessage());
                    }
                });
            }
        });
    }

    private void refreshFavoriteMenu() {
        flag_favorite = RealmController.with(this).getPlace(place.placeId) != null;
        if (flag_favorite) {
            favorite_menu.setIcon(R.drawable.ic_menu_favorite);
        } else {
            favorite_menu.setIcon(R.drawable.ic_menu_favorite_border);
        }
    }

    private void showSnackBarRetry(String msg) {
        if (!Network.hasInternet(this)) msg = getString(R.string.no_internet);
        Snackbar snackbar = Snackbar.make(parent_view, msg, Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction(getString(R.string.RETRY), new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadPlaceDetail();
            }
        });
        snackbar.show();
    }

    private void displayData(final PlaceDetails p) {
        place_details = p;
        String cur_type = place.type;
        place = PlaceModel.copyDetails(p);
        place.type = cur_type;
        ((TextView) findViewById(R.id.name)).setText(p.name);
        ((TextView) findViewById(R.id.type)).setText(cur_type);
        ((TextView) findViewById(R.id.address)).setText(p.formattedAddress);
        ((TextView) findViewById(R.id.phone)).setText(p.internationalPhoneNumber != null ? p.internationalPhoneNumber : getString(R.string.no_phone));
        ((TextView) findViewById(R.id.website)).setText(Tools.getFormattedUrl(this, p.website));
        ((TextView) findViewById(R.id.rating)).setText("( " + p.rating + " )");
        ((AppCompatRatingBar) findViewById(R.id.rating_star)).setRating(p.rating);

        WebView open = (WebView) findViewById(R.id.open);
        open.loadData(Tools.getOpeningHour(p.openingHours), "text/html; charset=UTF-8", null);

        ViewPager viewPager = (ViewPager) findViewById(R.id.view_pager);
        dotsLayout = (LinearLayout) findViewById(R.id.layoutDots);

        View lyt_no_image = findViewById(R.id.lyt_no_image);
        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

        if (p.photos != null && p.photos.length > 0) {

            lyt_no_image.setVisibility(View.GONE);
            viewPager.setVisibility(View.VISIBLE);

            PhotosPageAdapter adapter = new PhotosPageAdapter(p.photos);
            viewPager.setAdapter(adapter);
            addBottomDots(0, p.photos.length);

            viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

                @Override
                public void onPageSelected(int position) {
                    addBottomDots(position, p.photos.length);
                }

                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                }

                @Override
                public void onPageScrollStateChanged(int state) {
                }
            });


            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent navigation = new Intent(Intent.ACTION_VIEW, Uri.parse("http://maps.google.com/maps?daddr=" + place.geometry.location.lat + "," + place.geometry.location.lng));
                    startActivity(navigation);
                }
            });

        } else {
            lyt_no_image.setVisibility(View.VISIBLE);
            viewPager.setVisibility(View.GONE);
        }


        if (p.reviews != null && p.reviews.length > 0) {
            RecyclerView recycler = (RecyclerView) findViewById(R.id.recyclerView);
            recycler.setLayoutManager(new LinearLayoutManager(this));
            recycler.setAdapter(new AdapterReviews(this, p.reviews));
        } else {
            ((View) findViewById(R.id.lyt_reviews)).setVisibility(View.GONE);
        }

    }

    public void onLayoutClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.lyt_address:
                Tools.displayLocationOnGoogleMap(this, place);
                break;
            case R.id.lyt_phone:
                if (place_details.formattedPhoneNumber == null) return;
                Tools.dialNumber(this, place_details.formattedPhoneNumber);
                break;
            case R.id.lyt_website:
                if (place_details.website == null) return;
                Tools.directUrl(this, place_details.website.toString());
                break;
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle("");

        // for system bar in lollipop
        Tools.systemBarLolipop(this);

        final CollapsingToolbarLayout collapsing_toolbar = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        ((AppBarLayout) findViewById(R.id.app_bar_layout)).addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (collapsing_toolbar.getHeight() + verticalOffset < 2 * ViewCompat.getMinimumHeight(collapsing_toolbar)) {
                    fab.hide();
                } else {
                    fab.show();
                }
            }
        });
    }

    private void initAds() {
        AdView mAdView = (AdView) findViewById(R.id.ad_view);
        AdRequest adRequest = new AdRequest.Builder()
                .addNetworkExtrasBundle(AdMobAdapter.class, GDPR.getBundleAd(this))
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR).build();
        mAdView.loadAd(adRequest);
        if (!AppConfig.BANNER_DETAIL) {
            mAdView.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_place_details, menu);
        favorite_menu = menu.findItem(R.id.action_favorite);
        refreshFavoriteMenu();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            super.onBackPressed();
        } else if (id == R.id.action_favorite) {
            if (place_details == null) return true;
            String msg;
            if (flag_favorite) {
                RealmController.with(this).deletePlace(place.placeId);
                msg = getString(R.string.remove_from_favorite);
            } else {
                RealmController.with(this).savePlace(place);
                msg = getString(R.string.add_to_favorite);
            }
            Snackbar.make(parent_view, msg, Snackbar.LENGTH_LONG).show();
            refreshFavoriteMenu();
        }
        return true;
    }


    private void addBottomDots(int currentPage, int length) {
        dots = new TextView[length];

        dotsLayout.removeAllViews();
        for (int i = 0; i < dots.length; i++) {
            dots[i] = new TextView(this);
            dots[i].setText(Html.fromHtml("&#8226;"));
            dots[i].setTextSize(35);
            dots[i].setTextColor(Color.BLACK);
            dotsLayout.addView(dots[i]);
        }

        if (dots.length > 0) {
            dots[currentPage].setTextColor(Color.WHITE);
        }
    }

    /**
     * View pager adapter
     */
    public class PhotosPageAdapter extends PagerAdapter {
        private LayoutInflater layoutInflater;

        private Photo[] photos;
        private Context context;

        public PhotosPageAdapter(Photo[] photos) {
            this.photos = photos;
            context = getApplicationContext();
        }

        @Override
        public Object instantiateItem(ViewGroup container, int pos) {
            layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View view = layoutInflater.inflate(R.layout.item_view_pager, container, false);
            ImageView image = (ImageView) view.findViewById(R.id.image);
            String url = Tools.getGooglePhotoUrl(context, photos[pos].photoReference);
            Glide.with(context).load(url).placeholder(R.drawable.loading_placeholder)
                    .skipMemoryCache(false).centerCrop().diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .into(image);
            container.addView(view);

            return view;
        }

        @Override
        public int getCount() {
            return photos.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object obj) {
            return view == obj;
        }


        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            View view = (View) object;
            container.removeView(view);
        }
    }

}
