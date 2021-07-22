package com.app.explore.utils;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;

import com.app.explore.R;
import com.app.explore.data.Constant;

import java.util.ArrayList;
import java.util.List;

public abstract class PermissionUtil {

    public static final String LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    public static final String STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;

    /* Permission required for application */
    public static final String[] PERMISSION_ALL = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    public static void goToPermissionSettingScreen(Activity activity) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", activity.getPackageName(), null));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
    }

    public static String[] getDeniedPermission(Activity act) {
        List<String> permissions = new ArrayList<>();
        for (int i = 0; i < PERMISSION_ALL.length; i++) {
            int status = act.checkSelfPermission(PERMISSION_ALL[i]);
            if (status != PackageManager.PERMISSION_GRANTED) {
                permissions.add(PERMISSION_ALL[i]);
            }
        }

        return permissions.toArray(new String[permissions.size()]);
    }


    public static boolean isGranted(Activity act, String permission) {
        if (!Tools.needRequestPermission()) return true;
        return (act.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED);
    }

    public static boolean isLocationGranted(Activity act) {
        return isGranted(act, Manifest.permission.ACCESS_FINE_LOCATION);
    }

    public static void showSystemDialogPermission(Fragment fragment, String perm) {
        fragment.requestPermissions(new String[]{perm}, Constant.DEFAULT_PERMISSION_CODE);
    }

    public static void showSystemDialogPermission(Activity act, String perm) {
        act.requestPermissions(new String[]{perm}, Constant.DEFAULT_PERMISSION_CODE);
    }

    public static void showSystemDialogPermission(Activity act, String perm, int code) {
        act.requestPermissions(new String[]{perm}, code);
    }


    public static void showDialogInfo(final Activity act) {
        AlertDialog.Builder builder = new AlertDialog.Builder(act);
        builder.setTitle(act.getString(R.string.dialog_title_permission));
        builder.setMessage(act.getString(R.string.dialog_content_permission));
        builder.setPositiveButton(act.getString(R.string.SETTING), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                goToPermissionSettingScreen(act);
            }
        });
        builder.setNegativeButton(act.getString(R.string.DISMISS), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                act.finish();
            }
        });
        builder.show();
    }

    public static void showDialogLocation(final Activity act) {
        AlertDialog.Builder builder = new AlertDialog.Builder(act);
        builder.setTitle(act.getString(R.string.dialog_title_denied));
        builder.setMessage(act.getString(R.string.dialog_content_permission_location));
        builder.setPositiveButton(act.getString(R.string.SETTING), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                goToPermissionSettingScreen(act);
            }
        });
        builder.setNegativeButton(act.getString(R.string.DISMISS), null);
        builder.show();
    }

}
