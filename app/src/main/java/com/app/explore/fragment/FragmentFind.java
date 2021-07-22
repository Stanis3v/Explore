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

import com.app.explore.ActivityMain;
import com.app.explore.ActivityPlaceDetail;
import com.app.explore.R;
import com.app.explore.data.Constant;
import com.app.explore.data.SharedPref;
import com.app.explore.model.PlaceModel;
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
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.maps.model.Geometry;

import java.util.Arrays;

public class FragmentFind extends Fragment {

    private GoogleMap mMap;
    private SharedPref sharedPref;
    private SupportMapFragment mapFragment;
    private AutocompleteSupportFragment place_search;

    private View root_view, marker_view;

    private ImageView marker_bg;
    private CardView search_field;
    private ImageButton bt_location;
    private ProgressBar progressBar;
    private TextView tv_place_search;

    private Place obj_search = null;
    private Marker marker = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        root_view = inflater.inflate(R.layout.fragment_find, container, false);
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
        inflater.inflate(R.menu.menu_fragment_find, menu);
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
                if (snackbar_info != null) snackbar_info.dismiss();
            } else {
                if (snackbar_info != null) snackbar_info.show();
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

    private void iniComponent() {
        place_search = (AutocompleteSupportFragment) getChildFragmentManager().findFragmentById(R.id.place_search);
        place_search.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));
        place_search.setHint(getString(R.string.hint_search));

        tv_place_search = (EditText) place_search.getView().findViewById(R.id.places_autocomplete_search_input);
        tv_place_search.setTextAppearance(getActivity(), android.R.style.TextAppearance_Material_Body1);
        tv_place_search.setTextColor(getActivity().getResources().getColor(R.color.orig_color));
        tv_place_search.setHintTextColor(getActivity().getResources().getColor(R.color.grey_medium));

        (place_search.getView().findViewById(R.id.places_autocomplete_search_button)).setVisibility(View.GONE);

        search_field = (CardView) root_view.findViewById(R.id.search_field);
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        marker_view = inflater.inflate(R.layout.maps_marker, null);
        marker_bg = (ImageView) marker_view.findViewById(R.id.marker_bg);

        bt_location = (ImageButton) root_view.findViewById(R.id.bt_location);
        progressBar = (ProgressBar) root_view.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);
    }

    private void initAction() {
        place_search.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                obj_search = place;
                displayMarker(place.getLatLng().latitude, place.getLatLng().longitude, place.getName());
                displaySnackbarDetails(obj_search);
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
        ImageButton clear_button = (ImageButton) place_search.getView().findViewById(R.id.places_autocomplete_clear_button);
        clear_button.setLayoutParams(new AppBarLayout.LayoutParams(search_bar_size, search_bar_size));
        clear_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.setVisibility(View.GONE);
                tv_place_search.setText("");
                obj_search = null;
                marker.remove();
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
                        PermissionUtil.showSystemDialogPermission(FragmentFind.this, PermissionUtil.LOCATION);
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

    private void hideSnackbarDirection() {
        if (snackbar_info != null && snackbar_info.isShown()) {
            snackbar_info.dismiss();
        }
    }

    @SuppressWarnings("MissingPermission")
    private void toggleLocation() {
        Tools.checkingGPS(getActivity(), new Callback<UserLoc>() {
            @Override
            public void onSuccess(UserLoc result) {
                bt_location.setColorFilter(getActivity().getResources().getColor(R.color.colorAccent));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(result.getPosition(), 12));
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
                mMap.setMyLocationEnabled(true);
            }

            @Override
            public void onError(String msg) {
            }

            @Override
            public void onReject(String msg) {
            }
        });
    }

    private void displayMarker(double lat, double lng, String title) {
        // make current location marker
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(new LatLng(lat, lng)).title(title);
        if (mMap != null) {
            marker_bg.setColorFilter(getResources().getColor(R.color.dest_color));
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(Tools.createBitmapFromView(getActivity(), marker_view)));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lng), 10));
            marker = mMap.addMarker(markerOptions);
        }
    }

    private Snackbar snackbar_info = null;

    private void displaySnackbarDetails(final Place p) {
        snackbar_info = Snackbar.make(root_view, p.getName(), Snackbar.LENGTH_INDEFINITE);
        // Changing action button text color
        View sb = snackbar_info.getView();
        TextView message = (TextView) sb.findViewById(com.google.android.material.R.id.snackbar_text);
        message.setTextColor(getContext().getResources().getColor(R.color.grey_dark));
        sb.setBackgroundColor(Color.WHITE);
        snackbar_info.setAction(getString(R.string.DETAILS), new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PlaceModel pm = new PlaceModel();
                pm.formattedAddress = p.getAddress();
                pm.name = p.getName();
                pm.geometry = new Geometry();
                pm.geometry.location = new com.google.maps.model.LatLng(p.getLatLng().latitude, p.getLatLng().longitude);
                //pm.icon = p.icon;
                pm.placeId = p.getId();
                pm.type = getString(R.string.TYPE_SEARCH);

                ActivityPlaceDetail.navigate((ActivityMain) getActivity(), view, pm.getJsonString());
            }
        });
        snackbar_info.show();
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
