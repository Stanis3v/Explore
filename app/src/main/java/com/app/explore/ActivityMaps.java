package com.app.explore;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.view.ViewCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.app.explore.data.Constant;
import com.app.explore.data.SharedPref;
import com.app.explore.model.Around;
import com.app.explore.model.PlaceModel;
import com.app.explore.model.UserLoc;
import com.app.explore.utils.Analytics;
import com.app.explore.utils.Callback;
import com.app.explore.utils.Network;
import com.app.explore.utils.PermissionUtil;
import com.app.explore.utils.Tools;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.GeoApiContext;
import com.google.maps.NearbySearchRequest;
import com.google.maps.PendingResult;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.google.maps.model.PlaceType;
import com.google.maps.model.PlacesSearchResponse;
import com.google.maps.model.PlacesSearchResult;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class ActivityMaps extends AppCompatActivity implements OnMapReadyCallback {

    public static final String EXTRA_OBJC = "key.EXTRA_OBJC";
    public static final int REQCODE_ON_RESUME = 200;
    public static final int REQCODE_ON_CLICK = 201;

    // give preparation animation activity transition
    public static void navigate(AppCompatActivity activity, View transitionView, Around obj) {
        Intent intent = new Intent(activity, ActivityMaps.class);
        intent.putExtra(EXTRA_OBJC, obj);
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity, transitionView, EXTRA_OBJC);
        ActivityCompat.startActivity(activity, intent, options.toBundle());
    }

    private SharedPref sharedPref;
    private GoogleMap mMap;
    private Toolbar toolbar;
    private ActionBar actionBar;
    private ProgressBar progressBar;
    private View parent_view;

    // view for custom marker
    private ImageView icon, marker_bg;
    private View marker_view;

    private Around around_extra;
    private UserLoc userLoc = null;
    private boolean is_map_ready = false;
    private boolean nearby_success = false;
    private ClusterManager<PlaceModel> mClusterManager;
    private HashMap<String, PlaceModel> mapPlaces = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        parent_view = findViewById(android.R.id.content);

        sharedPref = new SharedPref(this);

        // animation transition
        ViewCompat.setTransitionName(findViewById(R.id.toolbar), EXTRA_OBJC);

        around_extra = (Around) getIntent().getSerializableExtra(EXTRA_OBJC);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        mBottomSheet = findViewById(R.id.bottomSheet);
        mBehavior = BottomSheetBehavior.from(mBottomSheet);
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        marker_view = inflater.inflate(R.layout.maps_marker, null);
        icon = (ImageView) marker_view.findViewById(R.id.marker_icon);
        marker_bg = (ImageView) marker_view.findViewById(R.id.marker_bg);

        initMapFragment();
        initToolbar();
        Analytics.trackActivityScreen(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (nearby_success) return;
        if (PermissionUtil.isLocationGranted(this)) {
            getCurrentLocation();
        } else {
            if (sharedPref.getNeverAskAgain(PermissionUtil.LOCATION)) {
                PermissionUtil.showDialogLocation(this);
            } else {
                PermissionUtil.showSystemDialogPermission(this, PermissionUtil.LOCATION, REQCODE_ON_RESUME);
            }
        }
    }

    private void getCurrentLocation() {
        progressBar.setVisibility(View.VISIBLE);
        Tools.checkingGPS(this, new Callback<UserLoc>() {
            @Override
            public void onSuccess(UserLoc result) {
                userLoc = result;
                if (userLoc != null && is_map_ready) {
                    moveCameraToCurrentLocation();
                    loadNearbyPlaces();
                }
            }

            @Override
            public void onError(String msg) {
                showSnackBarRetry(msg);
            }

            @Override
            public void onReject(String msg) {
                ActivityMaps.this.finish();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = Tools.configBasicGoogleMap(googleMap);
        mMap.setMapType(sharedPref.getMapType());
        mClusterManager = new ClusterManager<>(this, mMap);
        is_map_ready = true;
        if (userLoc != null) {
            moveCameraToCurrentLocation();
            loadNearbyPlaces();
        }

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                PlaceModel place;
                if (mapPlaces.get(marker.getId()) != null) {
                    place = mapPlaces.get(marker.getId());
                    marker.showInfoWindow();
                    showBottomSheetDialog(place, marker);
                } else {
                    marker.showInfoWindow();
                }
                return true;
            }
        });
    }

    private void moveCameraToCurrentLocation() {
        com.google.android.gms.maps.model.LatLng cur = userLoc.getPosition();
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(cur, 12));
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mMap.setMyLocationEnabled(true);
        mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {
                try {
                    if (PermissionUtil.isLocationGranted(ActivityMaps.this)) {
                        Location loc = Tools.getLastKnownLocation(ActivityMaps.this);
                        CameraUpdate myCam = CameraUpdateFactory.newLatLngZoom(new LatLng(loc.getLatitude(), loc.getLongitude()), 12);
                        mMap.animateCamera(myCam);
                    } else {
                        if (sharedPref.getNeverAskAgain(PermissionUtil.LOCATION)) {
                            PermissionUtil.showDialogLocation(ActivityMaps.this);
                        } else {
                            PermissionUtil.showSystemDialogPermission(ActivityMaps.this, PermissionUtil.LOCATION, REQCODE_ON_CLICK);
                        }
                    }
                } catch (Exception e) {
                }
                return true;
            }
        });
    }

    private void initMapFragment() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void initToolbar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setTitle(around_extra.name);
        // for system bar in lollipop
        Tools.systemBarLolipop(this);
    }

    private BottomSheetBehavior mBehavior;
    private BottomSheetDialog mBottomSheetDialog;
    private View mBottomSheet;

    private void showBottomSheetDialog(final PlaceModel place, final Marker marker) {
        if (mBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            mBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }

        final View view = getLayoutInflater().inflate(R.layout.sheet_activity_map, null);
        ((TextView) view.findViewById(R.id.name)).setText(place.name);
        ((TextView) view.findViewById(R.id.address)).setText(place.vicinity);
        (view.findViewById(R.id.bt_close)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBottomSheetDialog.dismiss();
            }
        });

        (view.findViewById(R.id.bt_details)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                place.type = around_extra.name;
                ActivityPlaceDetail.navigate(ActivityMaps.this, view, place.getJsonString());
            }
        });

        mBottomSheetDialog = new BottomSheetDialog(this);
        mBottomSheetDialog.setContentView(view);

        mBottomSheetDialog.show();
        mBottomSheetDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                marker.hideInfoWindow();
                mBottomSheetDialog = null;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_maps, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            super.onBackPressed();
        } else if (id == R.id.mode_normal) {
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            sharedPref.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        } else if (id == R.id.mode_satellite) {
            mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
            sharedPref.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        } else if (id == R.id.mode_hybrid) {
            mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
            sharedPref.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        } else if (id == R.id.mode_terrain) {
            mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
            sharedPref.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
        }
        return true;
    }


    private void loadNearbyPlaces() {
        GeoApiContext context = new GeoApiContext().setApiKey(getString(R.string.google_maps_key));
        context.setConnectTimeout(Constant.TIMEOUT_AROUND_ME, TimeUnit.SECONDS);

        com.google.maps.model.LatLng location = new com.google.maps.model.LatLng(userLoc.lat, userLoc.lng);
        int radius = sharedPref.getRadius() * 1000;

        NearbySearchRequest n = new NearbySearchRequest(context);
        n.type(PlaceType.valueOf(PlaceType.class, around_extra.type.toUpperCase()));
        n.radius(radius);
        n.location(location);

        n.setCallback(new PendingResult.Callback<PlacesSearchResponse>() {
            @Override
            public void onResult(PlacesSearchResponse result) {
                final PlacesSearchResult[] places_result = result.results;
                runOnUiThread(new Runnable() {
                    public void run() {
                        displayMarker(places_result);
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

    private void showSnackBarRetry(String msg) {
        if (!Network.hasInternet(this)) msg = getString(R.string.no_internet);
        Snackbar snackbar = Snackbar.make(parent_view, msg, Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction(getString(R.string.RETRY), new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getCurrentLocation();
            }
        });
        snackbar.show();
    }

    private void displayMarker(final PlacesSearchResult[] places_result) {
        mMap.clear();
        moveCameraToCurrentLocation();
        mClusterManager.clearItems();
        for (PlacesSearchResult p : places_result) {
            PlaceModel place = PlaceModel.copy(p);
            mClusterManager.addItem(place);
        }

        PlaceMarkerRenderer placeMarkerRenderer = new PlaceMarkerRenderer(this, mMap, mClusterManager);
        mClusterManager.setRenderer(placeMarkerRenderer);
        mMap.setOnCameraChangeListener(mClusterManager);
        nearby_success = true;
    }

    private class PlaceMarkerRenderer extends DefaultClusterRenderer<PlaceModel> {

        public PlaceMarkerRenderer(Context context, GoogleMap map, ClusterManager<PlaceModel> clusterManager) {
            super(context, map, clusterManager);
        }

        @Override
        protected void onBeforeClusterItemRendered(PlaceModel item, MarkerOptions markerOpt) {
            markerOpt.title(item.name);
            if (around_extra.icon != -1) {
                icon.setImageResource(around_extra.icon);
            }
            marker_bg.setColorFilter(getResources().getColor(R.color.colorAccent));
            markerOpt.icon(BitmapDescriptorFactory.fromBitmap(Tools.createBitmapFromView(ActivityMaps.this, marker_view)));
        }

        @Override
        protected void onClusterItemRendered(PlaceModel item, Marker marker) {
            mapPlaces.put(marker.getId(), item);
            super.onClusterItemRendered(item, marker);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQCODE_ON_RESUME) {
            for (int i = 0; i < permissions.length; i++) {
                boolean rationale = shouldShowRequestPermissionRationale(permissions[i]);
                sharedPref.setNeverAskAgain(permissions[i], !rationale);
                if (permissions[i].equals(PermissionUtil.LOCATION) && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    getCurrentLocation();
                }
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
