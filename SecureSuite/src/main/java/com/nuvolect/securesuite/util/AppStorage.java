/*
 * Copyright (c) 2018 Nuvolect LLC.
 * This software is offered for free under conditions of the GPLv3 open source software license.
 * Contact Nuvolect LLC for a less restrictive commercial license if you would like to use the software
 * without the GPLv3 restrictions.
 */

package com.nuvolect.securesuite.util;

import android.content.Context;
import android.os.Environment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import androidx.core.content.ContextCompat;

/**
 * Survey a device's app storage, to include removable and non-removable storage.
 *
 * Universal way to write to external SD card on Android
 * http://stackoverflow.com/questions/40068984/universal-way-to-write-to-external-sd-card-on-android
 */
public class AppStorage {


    public static JSONArray getAppStorage(Context ctx) {

        JSONArray jsonArray = new JSONArray();

        File[] filesDirs = ContextCompat.getExternalFilesDirs(ctx, null);// null == all files?
        try {

            int number = 0;

            for( File drive : filesDirs){

                boolean external = Environment.isExternalStorageRemovable( drive );

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("name", Omni.APP_STORAGE_NAME_PREFIX+number++);
                jsonObject.put("path", drive.getAbsolutePath());
                jsonObject.put("can_read", drive.canRead());
                jsonObject.put("can_write", drive.canWrite());
                jsonObject.put("total_space", drive.getTotalSpace());
                jsonObject.put("free_space", drive.getFreeSpace());
                jsonObject.put("removable", external);

                jsonArray.put( jsonObject);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonArray;
    }
}
