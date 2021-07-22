package com.app.explore.fragment;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.snackbar.Snackbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.FragmentTransaction;


import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.app.explore.R;
import com.app.explore.data.Constant;
import com.app.explore.data.SharedPref;
import com.app.explore.model.UserLoc;
import com.app.explore.utils.Analytics;
import com.app.explore.utils.Callback;
import com.app.explore.utils.Network;
import com.app.explore.utils.PermissionUtil;
import com.app.explore.utils.Tools;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.maps.DirectionsApi;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.PendingResult;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.TravelMode;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class FragmentDirection extends Fragment {

    private GoogleMap mMap;
    private SharedPref sharedPref;
    private Polyline polyline;
    private SupportMapFragment mapFragment;
    private AutocompleteSupportFragment place_orig, place_dest;

    private View root_view, marker_view;

    private ImageView icon, marker_bg;
    private CardView search_field;
    private ImageButton bt_location;
    private ProgressBar progressBar;
    private TextView tv_place_orig, tv_place_dest, tv_my_location;

    private Place obj_orig = null, obj_dest = null;
    private UserLoc obj_user = null;
    private Marker marker_orig = null, marker_dest = null;

    boolean is_location_active = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        root_view = inflater.inflate(R.layout.fragment_direction, container, false);
        // activate fragment menu
        setHasOptionsMenu(true);
        sharedPref = new SharedPref(getActivity());

        iniComponent();
        initAction();
        Analytics.trackFragmentScreen(this);
        return root_view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        FragmentManager fm = getChildFragmentManager();
        mapFragment = (SupportMapFragment) fm.findFragmentById(R.id.map_container);
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            fm.beginTransaction().replace(R.id.map_container, mapFragment).commit();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_fragment_direction, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.mode_normal) {
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
        } else if (id == R.id.action_field) {
            item.setIcon(R.drawable.ic_menu_less);
            if (!is_search_hide) {
                item.setIcon(R.drawable.ic_menu_more);
                if (snackbar_direction != null) snackbar_direction.dismiss();
            } else {
                if (snackbar_direction != null) snackbar_direction.show();
            }
            animateSearchField();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mMap == null && mapFragment != null) {
            mapFragment.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap googleMap) {
                    mMap = Tools.configBasicGoogleMap(googleMap);
                    mMap.setMapType(sharedPref.getMapType());
                }
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        try {
            // remove auto map fragment
            if (mapFragment != null) {
                FragmentTransaction ft2 = getActivity().getSupportFragmentManager().beginTransaction();
                ft2.remove(mapFragment);
                ft2.commit();
            }
        } catch (Exception e) {

        }
    }

    private String summary = "";

    private void processDirection() {
        if ((obj_orig != null && obj_dest != null) || (obj_dest != null && obj_user != null)) {
            progressBar.setVisibility(View.VISIBLE);
            GeoApiContext context = new GeoApiContext().setApiKey(getString(R.string.google_maps_key));
            context.setConnectTimeout(Constant.TIMEOUT_DIRECTION, TimeUnit.SECONDS);

            DirectionsApiRequest d = DirectionsApi.newRequest(context);
            double orig_lat = obj_user != null ? obj_user.lat : obj_orig.getLatLng().latitude;
            double orig_lng = obj_user != null ? obj_user.lng : obj_orig.getLatLng().longitude;

            com.google.maps.model.LatLng origin = new com.google.maps.model.LatLng(orig_lat, orig_lng);
            com.google.maps.model.LatLng destination = new com.google.maps.model.LatLng(obj_dest.getLatLng().latitude, obj_dest.getLatLng().longitude);

            d.origin(origin).destination(destination).mode(TravelMode.DRIVING).alternatives(false);
            d.setCallback(new PendingResult.Callback<DirectionsResult>() {
                @Override
                public void onResult(DirectionsResult result) {
                    final PolylineOptions polylineOptions = new PolylineOptions().width(10).color(getContext().getResources().getColor(R.color.colorAccent)).geodesic(true);
                    int counter = 0;
                    summary = "";
                    for (DirectionsRoute d : result.routes) {
                        for (com.google.maps.model.LatLng l : d.overviewPolyline.decodePath()) {
                            polylineOptions.add(new LatLng(l.lat, l.lng));
                            counter++;
                        }
                        summary = d.summary;
                    }
                    // draw polyline
                    getActivity().runOnUiThread(new Runnable() {
                        public void run() {
                            polyline = mMap.addPolyline(polylineOptions);
                            progressBar.setVisibility(View.GONE);
                            displaySnackbarDirection(summary);
                        }
                    });
                }

                @Override
                public void onFailure(Throwable e) {
                    progressBar.setVisibility(View.GONE);
                    final Snackbar snackbar = Snackbar.make(root_view, getString(R.string.FAILED) + " : " + e.getMessage(), Snackbar.LENGTH_LONG);
                    snackbar.setAction(getString(R.string.RETRY), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            processDirection();
                        }
                    }).show();
                }
            });
        }
    }

    private void iniComponent() {
        place_orig = (AutocompleteSupportFragment) getChildFragmentManager().findFragmentById(R.id.place_orig);
        place_dest = (AutocompleteSupportFragment) getChildFragmentManager().findFragmentById(R.id.place_dest);
        place_orig.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));
        place_dest.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));
        // Retrieve the PlaceAutocompleteFragment.
        place_orig.setHint(getString(R.string.hint_origin));
        place_dest.setHint(getString(R.string.hint_destination));

        tv_my_location = (TextView) root_view.findViewById(R.id.tv_my_location);
        tv_my_location.setVisibility(View.GONE);

        tv_place_orig = (EditText) place_orig.getView().findViewById(R.id.places_autocomplete_search_input);
        tv_place_dest = (EditText) place_dest.getView().findViewById(R.id.places_autocomplete_search_input);
        tv_place_orig.setTextAppearance(getActivity(), android.R.style.TextAppearance_Material_Body1);
        tv_place_dest.setTextAppearance(getActivity(), android.R.style.TextAppearance_Material_Body1);
        tv_place_orig.setTextColor(getActivity().getResources().getColor(R.color.orig_color));
        tv_place_dest.setTextColor(getActivity().getResources().getColor(R.color.dest_color));
        tv_place_orig.setHintTextColor(getActivity().getResources().getColor(R.color.grey_medium));
        tv_place_dest.setHintTextColor(getActivity().getResources().getColor(R.color.grey_medium));

        (place_orig.getView().findViewById(R.id.places_autocomplete_search_button)).setVisibility(View.GONE);
        (place_dest.getView().findViewById(R.id.places_autocomplete_search_button)).setVisibility(View.GONE);

        search_field = (CardView) root_view.findViewById(R.id.search_field);
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        marker_view = inflater.inflate(R.layout.maps_marker, null);
        icon = (ImageView) marker_view.findViewById(R.id.marker_icon);
        marker_bg = (ImageView) marker_view.findViewById(R.id.marker_bg);

        bt_location = (ImageButton) root_view.findViewById(R.id.bt_location);
        progressBar = (ProgressBar) root_view.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);
    }

    private void initAction() {
        place_orig.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                obj_orig = place;
                obj_user = null;
                displayMarker(place.getLatLng().latitude, place.getLatLng().longitude, getString(R.string.window_origin), true);
                processDirection();
            }

            @Override
            public void onError(Status status) {
                if (!Network.hasInternet(getContext())) {
                    Network.noConnectionSnackBar(getContext(), root_view);
                } else {
                    Toast.makeText(getActivity(), status.getStatusMessage(), Toast.LENGTH_SHORT).show();
                }
                Log.e("onError", status.getStatusMessage());
            }
        });
        place_dest.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                obj_dest = place;
                displayMarker(place.getLatLng().latitude, place.getLatLng().longitude, getString(R.string.window_destination), false);
                processDirection();
            }

            @Override
            public void onError(Status status) {
                if (!Network.hasInternet(getContext())) {
                    Network.noConnectionSnackBar(getContext(), root_view);
                } else {
                    Toast.makeText(getActivity(), status.getStatusMessage(), Toast.LENGTH_SHORT).show();
                }
                Log.e("onError", status.getStatusMessage());
            }
        });
        int search_bar_size = (int) getResources().getDimension(R.dimen.search_bar_size);
        ImageButton orig_clear_button = (ImageButton) place_orig.getView().findViewById(R.id.places_autocomplete_clear_button);
        orig_clear_button.setLayoutParams(new AppBarLayout.LayoutParams(search_bar_size, search_bar_size));
        orig_clear_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.setVisibility(View.GONE);
                tv_place_orig.setText("");
                obj_orig = null;
                marker_orig.remove();
                clearPolyLine();
                hideSnackbarDirection();
            }
        });
        ImageButton dest_clear_button = (ImageButton) place_dest.getView().findViewById(R.id.places_autocomplete_clear_button);
        dest_clear_button.setLayoutParams(new AppBarLayout.LayoutParams(search_bar_size, search_bar_size));
        dest_clear_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.setVisibility(View.GONE);
                tv_place_dest.setText("");
                obj_dest = null;
                marker_dest.remove();
                clearPolyLine();
                hideSnackbarDirection();
            }
        });

        bt_location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (PermissionUtil.isLocationGranted(getActivity())) {
                    toggleLocation();
                } else {
                    if (sharedPref.getNeverAskAgain(PermissionUtil.LOCATION)) {
                        PermissionUtil.showDialogLocation(getActivity());
                    } else {
                        PermissionUtil.showSystemDialogPermission(FragmentDirection.this, PermissionUtil.LOCATION);
                    }
                }
            }
        });
    }

    boolean is_search_hide = false;

    private void animateSearchField() {
        float translationY = -(3 * getResources().getDimensionPixelOffset(R.dimen.search_bar_size));
        if (is_search_hide) translationY = 0;
        search_field.animate().translationY(translationY).setInterpolator(new OvershootInterpolator(1.f)).setDuration(400).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }
        }).start();
        is_search_hide = !is_search_hide;
    }

    private void clearPolyLine() {
        if (mMap == null) return;
        if (polyline != null) polyline.remove();
    }

    private void hideSnackbarDirection() {
        if (snackbar_direction != null && snackbar_direction.isShown())
            snackbar_direction.dismiss();
    }

    private void toggleLocation() {
        if (is_location_active) {
            bt_location.setColorFilter(getActivity().getResources().getColor(R.color.grey_medium));
            tv_place_orig.setText("");
            tv_my_location.setVisibility(View.GONE);
            place_orig.getView().setVisibility(View.VISIBLE);
            obj_orig = null;
            obj_user = null;
            marker_orig.remove();
            clearPolyLine();
            hideSnackbarDirection();
            is_location_active = false;
        } else {
            Tools.checkingGPS(getActivity(), new Callback<UserLoc>() {
                @Override
                public void onSuccess(UserLoc result) {
                    obj_user = result;
                    if (marker_orig != null) {
                        marker_orig.remove();
                    }
                    clearPolyLine();
                    hideSnackbarDirection();
                    is_location_active = true;
                    tv_place_orig.setText("");
                    tv_my_location.setVisibility(View.VISIBLE);
                    place_orig.getView().setVisibility(View.GONE);
                    displayMarker(obj_user.lat, obj_user.lng, getString(R.string.current_location), true);
                    processDirection();
                    bt_location.setColorFilter(getActivity().getResources().getColor(R.color.colorAccent));
                }

                @Override
                public void onError(String msg) {
                }

                @Override
                public void onReject(String msg) {
                }

            });
        }
    }

    private void displayMarker(double lat, double lng, String title, boolean isOrigin) {
        // make current location marker
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(new LatLng(lat, lng)).title(title);
        if (mMap != null) {
            marker_bg.setColorFilter(getResources().getColor(R.color.dest_color));
            if (isOrigin) marker_bg.setColorFilter(getResources().getColor(R.color.orig_color));
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(Tools.createBitmapFromView(getActivity(), marker_view)));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lng), 10));

            Marker marker = mMap.addMarker(markerOptions);
            if (isOrigin) {
                marker_orig = marker;
            } else {
                marker_dest = marker;
            }
        }
    }

    private Snackbar snackbar_direction = null;

    private void displaySnackbarDirection(String summary) {
        if (summary.trim().equals("")) {
            summary = getString(R.string.msg_no_route);
        } else {
            summary = getString(R.string.via) + " : " + summary;
        }
        snackbar_direction = Snackbar.make(root_view, summary, Snackbar.LENGTH_INDEFINITE);
        // Changing action button text color
        View sb = snackbar_direction.getView();
        TextView message = (TextView) sb.findViewById(com.google.android.material.R.id.snackbar_text);
        message.setTextColor(getContext().getResources().getColor(R.color.grey_dark));
        sb.setBackgroundColor(Color.WHITE);
        snackbar_direction.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == Constant.DEFAULT_PERMISSION_CODE) {
            for (int i = 0; i < permissions.length; i++) {
                boolean rationale = shouldShowRequestPermissionRationale(permissions[i]);
                sharedPref.setNeverAskAgain(permissions[i], !rationale);
                if (permissions[i].equals(PermissionUtil.LOCATION) && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    toggleLocation();
                }
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
