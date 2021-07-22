package com.app.explore.utils;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.app.explore.R;
import com.app.explore.model.PlaceModel;
import com.app.explore.model.UserLoc;
import com.google.android.gms.maps.GoogleMap;
import com.google.gson.Gson;
import com.google.maps.model.OpeningHours;

import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class Tools {

    private static String URL_IMG_HEADER = "https://maps.googleapis.com/maps/api/place/photo?";

    public static boolean isLolipopOrHigher() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);
    }

    public static boolean needRequestPermission() {
        return (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1);
    }

    @SuppressWarnings({"MissingPermission"})
    public static void systemBarLolipop(Activity act) {
        if (isLolipopOrHigher()) {
            Window window = act.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(act.getResources().getColor(R.color.colorPrimaryDark));
        }
    }

    public String generateCurrentDate(int format_key) {
        Date curDate = new Date();
        String DateToStr = "";
        //default 11-5-2014 11:11:51
        SimpleDateFormat format = new SimpleDateFormat("dd-M-yyyy hh:mm:ss");

        switch (format_key) {
            case 1:

                format = new SimpleDateFormat("dd/MM/yyy");
                DateToStr = format.format(curDate);
                break;

            case 2:
                //May 11, 2014 11:37 PM
                DateToStr = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(curDate);
                break;
            case 3:
                //11-5-2014 11:11:51
                format = new SimpleDateFormat("dd-M-yyyy hh:mm:ss");
                DateToStr = format.format(curDate);
                break;
        }
        return DateToStr;
    }

    public static void rateAction(Activity activity) {
        Uri uri = Uri.parse("market://details?id=" + activity.getPackageName());
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
        try {
            activity.startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + activity.getPackageName())));
        }
    }

    public static void aboutAction(Activity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(activity.getString(R.string.dialog_about_title));
        builder.setMessage(Html.fromHtml(activity.getString(R.string.about_text)));
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    public static String[] getNameLocationFromType(String s[]) {
        String arr_result[] = new String[s.length];
        for (int j = 0; j < arr_result.length; j++) {
            char[] c = s[j].toCharArray();
            String result = "";
            result = String.valueOf(c[0]).toUpperCase();
            for (int i = 1; i < c.length; i++) {
                if (c[i] == '_') {
                    result = result + " ";
                } else if (c[i - 1] == '_') {
                    result = result + String.valueOf(c[i]).toUpperCase();
                } else {
                    result = result + c[i];
                }
            }
            arr_result[j] = result;
        }
        return arr_result;
    }

    public static int colorDarker(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 0.8f; // value component
        return Color.HSVToColor(hsv);
    }

    public static int colorBrighter(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] /= 0.8f; // value component
        return Color.HSVToColor(hsv);
    }

    public static String getVersionName(Context ctx) {
        try {
            PackageManager manager = ctx.getPackageManager();
            PackageInfo info = manager.getPackageInfo(ctx.getPackageName(), 0);
            return ctx.getString(R.string.version) + " " + info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return ctx.getString(R.string.unknown);
        }
    }

    @SuppressWarnings({"MissingPermission"})
    public static Location getLastKnownLocation(Context ctx) {
        LocationManager mLocationManager = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
        LocationListener locationListener = Tools.requestLocationUpdate(mLocationManager);
        List<String> providers = mLocationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            Location l = mLocationManager.getLastKnownLocation(provider);
            if (l == null) {
                continue;
            }
            if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                // Found best last known location: %s", l);
                bestLocation = l;
            }
        }
        mLocationManager.removeUpdates(locationListener);
        return bestLocation;
    }

    @SuppressWarnings({"MissingPermission"})
    private static LocationListener requestLocationUpdate(LocationManager manager) {
        // Define a listener that responds to location updates
        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };

        // Register the listener with the Location Manager to receive location updates
        manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        return locationListener;
    }

    public static GoogleMap configBasicGoogleMap(GoogleMap googleMap) {
        // set map type
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        // Enable / Disable zooming controls
        googleMap.getUiSettings().setZoomControlsEnabled(true);

        // Enable / Disable Compass icon
        googleMap.getUiSettings().setCompassEnabled(true);
        // Enable / Disable Rotate gesture
        googleMap.getUiSettings().setRotateGesturesEnabled(true);
        // Enable / Disable zooming functionality
        googleMap.getUiSettings().setZoomGesturesEnabled(true);

        googleMap.getUiSettings().setScrollGesturesEnabled(true);
        googleMap.getUiSettings().setMapToolbarEnabled(true);

        // enable traffic layer
        googleMap.setTrafficEnabled(false);

        return googleMap;
    }

    public static void checkingGPS(final Activity act, final Callback<UserLoc> callback) {
        final LocationManager manager = (LocationManager) act.getSystemService(Context.LOCATION_SERVICE);
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showAlertDialogGps(act, callback);
        } else {
            Location loc = Tools.getLastKnownLocation(act);
            if (loc != null) {
                callback.onSuccess(new UserLoc(loc.getLatitude(), loc.getLongitude()));
            } else {
                callback.onError(act.getString(R.string.error_get_location));
            }
        }
    }

    public static void showAlertDialogGps(final Activity act, final Callback<UserLoc> callback) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(act);
        builder.setMessage(R.string.dialog_content_gps);
        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                act.startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            }
        });
        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                callback.onReject("NO");
                dialog.cancel();
            }
        });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    public static Bitmap createBitmapFromView(Activity act, View view) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        act.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        view.measure(displayMetrics.widthPixels, displayMetrics.heightPixels);
        view.layout(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels);
        view.buildDrawingCache();
        Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);

        return bitmap;
    }

    public static String getGooglePhotoUrl(Context act, String ref) {
        String API_KEY = act.getResources().getString(R.string.google_maps_key);
        String url_img = URL_IMG_HEADER
                + "maxwidth=500"
                + "&photoreference=" + ref
                + "&sensor=false"
                + "&key=" + API_KEY;

        return url_img;
    }

    public static String getFormattedUrl(Context ctx, URL website) {
        String res_url = "";
        try {
            res_url = res_url + website.toString();
        } catch (Exception e) {
            res_url = ctx.getString(R.string.no_website);
        }
        return res_url;
    }

    public static void dialNumber(Context ctx, String phone) {
        Intent i = new Intent(Intent.ACTION_DIAL);
        i.setData(Uri.parse("tel:" + phone));
        ctx.startActivity(i);
    }

    public static void directUrl(Context ctx, String website) {
        String url = website;
        if (!url.startsWith("https://") && !url.startsWith("http://")) {
            url = "http://" + url;
        }
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        ctx.startActivity(i);
    }

    public static void displayLocationOnGoogleMap(Activity activity, PlaceModel place) {
        double latitude = place.getPosition().latitude;
        double longitude = place.getPosition().longitude;
        String label = place.name;
        String uriBegin = "geo:" + latitude + "," + longitude;
        String query = latitude + "," + longitude + "(" + label + ")";
        String encodedQuery = Uri.encode(query);
        String uriString = uriBegin + "?q=" + encodedQuery + "&z=16";
        Uri uri = Uri.parse(uriString);
        Intent intent = new Intent(android.content.Intent.ACTION_VIEW, uri);
        activity.startActivity(intent);
    }


    public static String getOpeningHour(OpeningHours opening) {
        try {
            Log.d("OpeningHours", new Gson().toJson(opening, OpeningHours.class));
            StringBuilder sb = new StringBuilder();
            sb.append("<html>");
            sb.append("<head><style>table { border-collapse: collapse;} body{ margin:0; padding:0; }</style></head>");
            sb.append("<body><table>");

            for (String s : opening.weekdayText) {
                String day = s.toUpperCase().substring(0, 3);
                String hour = s.split(": ")[1];

                sb.append("<tr>");
                sb.append("<td>" + day + "</td>");
                sb.append("<td>&nbsp;&nbsp;&nbsp;" + hour + "</td>");
                sb.append("</tr>");
            }

            sb.append("</table></body>");
            sb.append("</html>");

            return sb.toString();
        } catch (Exception e) {

            StringBuilder sb = new StringBuilder();
            sb.append("<html>");
            sb.append("<head><style>body{ margin:0; padding:0; }</style></head>");
            sb.append("<body>No work time</body>");
            sb.append("</html>");

            return sb.toString();
        }
    }
}
