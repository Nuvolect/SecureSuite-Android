/*
 * Copyright (c) 2017. Nuvolect LLC
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.nuvolect.securesuite.util;//

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

/**
 * Permission management utilities
 */
public class PermissionUtil {

    public final static int READ_PHONE_STATE       = 6;

    public static void requestReadContacts(Activity act, int responseId){

        ActivityCompat.requestPermissions( act,
                new String[]{Manifest.permission.READ_CONTACTS}, responseId);
    }
    public static boolean canReadExternalStorage(Context ctx) {

        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }
    public static void requestReadExternalStorage(Activity act, int responseId){

        ActivityCompat.requestPermissions(act,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, responseId);
    }
    public static boolean canWriteExternalStorage(Context ctx) {

        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }
    public static void requestWriteExternalStorage(Activity act, int responseId){

        ActivityCompat.requestPermissions(act,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, responseId);
    }

    public static boolean canReadWriteExternalStorage(Activity act) {

        return canReadExternalStorage(act) && canWriteExternalStorage(act);
    }
    public static void requestReadWriteExternalStorage(Activity act, int responseId){

        ActivityCompat.requestPermissions(act,
                new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                }, responseId);
    }


    public static boolean canAccessPhoneState(Context ctx) {

        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestReadPhoneState(Activity act, int responseId){

        ActivityCompat.requestPermissions(act,
                new String[]{Manifest.permission.READ_PHONE_STATE}, responseId);
    }

    public static void requestGetAccounts(Activity act, int responseId){

        ActivityCompat.requestPermissions(act,
                new String[]{Manifest.permission.GET_ACCOUNTS}, responseId);
    }

    public static void showInstalledAppDetails(Context context) {

        if (context == null) {
            return;
        }
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", context.getPackageName(), null);
        intent.setData(uri);
        context.startActivity(intent);
    }
}
