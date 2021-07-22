package com.app.explore.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import com.google.android.material.snackbar.Snackbar;
import android.view.View;

import com.app.explore.R;

public class Network {

    /**
     * Checking for all possible internet providers
     **/
    public static boolean hasInternet(Context context) {
        ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null)
                for (int i = 0; i < info.length; i++)
                    if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }

        }
        return false;
    }


    public static void noConnectionSnackBar(Context ctx, View view) {
        final Snackbar snackbar = Snackbar.make(view, ctx.getString(R.string.no_internet), Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction(ctx.getString(R.string.OK), new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                snackbar.dismiss();
            }
        });
        snackbar.show();
    }
}
