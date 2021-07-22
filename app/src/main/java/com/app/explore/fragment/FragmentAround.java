package com.app.explore.fragment;

import android.content.res.TypedArray;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import com.app.explore.ActivityMain;
import com.app.explore.ActivityMaps;
import com.app.explore.R;
import com.app.explore.adapter.AdapterAroundMe;
import com.app.explore.data.AppConfig;
import com.app.explore.data.GDPR;
import com.app.explore.model.Around;
import com.app.explore.utils.Analytics;
import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.util.ArrayList;
import java.util.List;

public class FragmentAround extends Fragment {

    private AppCompatRadioButton radio_simple, radio_all;
    private RecyclerView recyclerView;
    private AdapterAroundMe mAdapter;
    private View root_view;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        root_view = inflater.inflate(R.layout.fragment_around, container, false);
        radio_simple = (AppCompatRadioButton) root_view.findViewById(R.id.radio_simple);
        radio_all = (AppCompatRadioButton) root_view.findViewById(R.id.radio_all);

        recyclerView = (RecyclerView) root_view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setHasFixedSize(true);

        displayListData(getDataAround(true));
        radio_simple.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton bt, boolean b) {
                radio_all.setChecked(!b);
                displayListData(getDataAround(b));
            }
        });
        radio_all.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton bt, boolean b) {
                radio_simple.setChecked(!b);
                displayListData(getDataAround(!b));
            }
        });
        Analytics.trackFragmentScreen(this);
        return root_view;
    }

    public List<Around> getDataAround(boolean simple_mode) {
        List<Around> around_items = new ArrayList<>();
        String places_type[];
        String places_name[];
        around_items.clear();
        if (simple_mode) {
            places_type = getResources().getStringArray(R.array.place_type);
            places_name = getResources().getStringArray(R.array.place_title);
            TypedArray navMenuIcons = getResources().obtainTypedArray(R.array.place_icon_items);
            for (int i = 0; i < places_name.length; i++) {
                Around a = new Around();
                a.type = places_type[i];
                a.icon = navMenuIcons.getResourceId(i, -1);
                a.name = places_name[i];
                around_items.add(a);
            }
        } else {
            places_type = getResources().getStringArray(R.array.all_place_type);
            places_name = getResources().getStringArray(R.array.all_place_title);
            for (int i = 0; i < places_name.length; i++) {
                Around a = new Around();
                a.name = places_name[i];
                a.type = places_type[i];
                around_items.add(a);
            }
        }
        return around_items;
    }

    private void displayListData(List<Around> items) {
        //set data and list adapter
        mAdapter = new AdapterAroundMe(getActivity(), items);
        recyclerView.setAdapter(mAdapter);

        // on item list clicked
        mAdapter.setOnItemClickListener(new AdapterAroundMe.OnItemClickListener() {
            @Override
            public void onItemClick(View view, Around obj, int position) {
                Analytics.trackAroundDisplayType(obj.type);
                ActivityMaps.navigate((ActivityMain) getActivity(), view, obj);
            }
        });
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initAds();
    }

    private void initAds() {
        AdView mAdView = (AdView) root_view.findViewById(R.id.ad_view);
        AdRequest adRequest = new AdRequest.Builder()
                .addNetworkExtrasBundle(AdMobAdapter.class, GDPR.getBundleAd(getActivity()))
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR).build();
        mAdView.loadAd(adRequest);
        if (!AppConfig.BANNER_AROUND) {
            mAdView.setVisibility(View.GONE);
        }
    }

}
