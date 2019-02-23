/*
 * Copyright (c) 2017. Nuvolect LLC
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * Contact legal@nuvolect.com for a less restrictive commercial license if you would like to use the
 * software without the GPLv3 restrictions.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.  If not,
 * see <http://www.gnu.org/licenses/>.
 *
 */

package com.nuvolect.securesuite.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.nuvolect.securesuite.main.CConst;
import com.nuvolect.securesuite.main.ContactListActivity;
import com.nuvolect.securesuite.util.LogUtil.LogType;

import org.apache.commons.io.IOUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class Util {


    private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);

    @SuppressLint("NewApi")
    public static int generateViewId(){

        if( Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1)

            return Util.preApi17_generateViewId();
        else
            return View.generateViewId();
    }

    /**
     * Generate a value suitable for use in setId(int)}.
     * This value will not collide with ID values generated at build time by aapt for R.id.
     *
     * @return a generated ID value
     */
    public static int preApi17_generateViewId() {
        for (;;) {
            final int result = sNextGeneratedId.get();
            // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
            int newValue = result + 1;
            if (newValue > 0x00FFFFFF) newValue = 1; // Roll over to 1, not 0.
            if (sNextGeneratedId.compareAndSet(result, newValue)) {
                return result;
            }
        }
    }

    /**
     * Return the passed array if item is in the array or
     * return a new array including custom item.
     *
     * @param array
     * @param item
     * @return
     */
    public static String[] extendArray(String[] array, String item) {

        // First check if array contains item
        for(int i = 0; i<array.length; i++)
            if( array[i].contentEquals(item))
                return array;

        String[] tmp = new String[ array.length + 1];
        for(int i = 0; i<array.length; i++)
            tmp[i] = array[i];

        tmp[array.length] = item;

        return tmp;
    }

    /**
     * Dump to log and return a string with the description of a cursor.
     * @param cursor
     * @return
     */
    public static String dumpCursorDescription(String tag, net.sqlcipher.Cursor cursor){

        String description = tag + " ";
        String newLine = "";

        if( cursor == null)
            description += "cursor is null";
        else
            if( cursor.isClosed())
                description += "cursor is closed";
            else{
                description += "count(): "+cursor.getCount()+"\n";
                for( int columnIndex=0; columnIndex< cursor.getColumnCount(); columnIndex++){

                    String columnDesc = columnIndex+": "+cursor.getColumnName(columnIndex);
                    description = description + newLine + columnDesc;
                    newLine = "\n";
                }
            }
        LogUtil.log(description);
        return description;
    }
    public static String dumpCursorDescription(String tag, android.database.Cursor cursor) {
        String description = tag + " ";
        String newLine = "";

        if( cursor == null)
            description += "cursor is null";
        else
            if( cursor.isClosed())
                description += "cursor is closed";
            else{
                description += "count(): "+cursor.getCount()+"\n";
                for( int columnIndex=0; columnIndex< cursor.getColumnCount(); columnIndex++){

                    String columnDesc = columnIndex+": "+cursor.getColumnName(columnIndex);
                    description = description + newLine + columnDesc;
                    newLine = "\n";
                }
            }
        LogUtil.log(description);
        return description;
    }
    /**
     * Convert dp units to pixels
     * @param ctx
     * @param dp
     * @return int pixels
     */
    public static int dpToPixels(Context ctx, int dp){

        DisplayMetrics displayMetrics = ctx.getResources().getDisplayMetrics();

        int pixels = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                displayMetrics);

        return pixels;
    }

    /**
     * Return the screen width in dp units
     * @param context
     * @return float
     */
    public static float screenWidthDp(Context context){

        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();

        //    float dpHeight = displayMetrics.heightPixels / displayMetrics.density;
        float dpWidth = displayMetrics.widthPixels / displayMetrics.density;

        return dpWidth;
    }

    /**
     * Return the screen width in pixels
     * @param context
     * @return
     */
    public static float screenWidth(Context context){

        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();

        //    float dpHeight = displayMetrics.heightPixels / displayMetrics.density;
        float dpWidth = displayMetrics.widthPixels ;

        return dpWidth;
    }

    /**
     * Create and return a string made from the date and time, to be used in filenames
     * @return
     */
    public static String makeDateTimeFilename(){

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HHmm.ss", Locale.US);
        String fName = sdf.format( System.currentTimeMillis());

        return fName;
    }

    /**
     * Generate a time date base folder name, create the folder and return the full path
     * Return an empty string on error.
     * @param m_ctx
     * @return
     */
    public static String createTimeStampedBackupFolder(Context m_ctx) {

        // Create the app folder if necessary
        String appPublicFolder = createAppPublicFolder();
        if( appPublicFolder.isEmpty())
            return ""; // error condition

        // Create the time specific folder
        String folderName = makeDateTimeFilename();
        String folderPathWithName = Environment.getExternalStorageDirectory()+CConst.FOLDER_NAME+folderName;

        File folder = new File( folderPathWithName );
        if(!folder.exists()) {

            if(folder.mkdir())  //directory is created;
                LogUtil.log( LogType.UTIL, "create success: "+folderPathWithName);
            else{
                folderPathWithName = "";
                LogUtil.log( LogType.UTIL, "create ERROR: "+folderPathWithName);
            }
        }
        else{
            LogUtil.log( LogType.UTIL, "create success: "+folderPathWithName);
        }
        return folderPathWithName;
    }

    /**
     * Create the application specific folder under /sdcard and return the
     * path to that folder.  Path includes a trailing slash.
     * @return
     */
    public static String createAppPublicFolder(){

        String appFolderPath = Environment.getExternalStorageDirectory()+CConst.FOLDER_NAME;
        File appFolder = new File( appFolderPath );
        if(!appFolder.exists()) {

            if(appFolder.mkdir())  //directory is created;
                LogUtil.log( LogType.UTIL, "create success: "+appFolderPath);
            else{
                LogUtil.log( LogType.UTIL, "create ERROR: "+appFolderPath);
                return "";
            }
        }
        return appFolderPath;
    }

    public static void writeFile(File file, String fileContents) {

        try {
            OutputStream out = null;

            out = new BufferedOutputStream( new FileOutputStream( file));

            out.write(fileContents.getBytes());

            if( out != null)
                out.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    public static void writeFile(byte[] bytes, File file) throws IOException {

        OutputStream out = new BufferedOutputStream( new FileOutputStream( file));
        out.write( bytes);
        out.close();
    }

    public static String readFile( Context ctx, File file){

        String fileContents = "";
        StringBuilder sb = new StringBuilder();

        try {
            InputStream is = new FileInputStream(file);

            byte[] buffer = new byte[4096];
            int len;
            while ((len = is.read(buffer)) > 0) {

                String s = new String( buffer, 0, len, "UTF-8");
                sb.append( s );
            }
            fileContents = sb.toString();

            if( is != null)
                is.close();
        } catch (FileNotFoundException e) {
            Log.e(LogUtil.TAG, "Exception while getting FileInputStream", e);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return fileContents;
    }

    public static File copyAssets(Context ctx, String assetFolder) {

        AssetManager assetManager = ctx.getAssets();
        String[] files = null;
        try {
            files = assetManager.list(assetFolder);
        } catch (IOException e) {
            Log.e("tag", "Failed to get asset file list.", e);
        }
        for(String filename : files) {
            InputStream in = null;
            try {
                in = assetManager.open(assetFolder + "/" + filename);
                FileOutputStream fos = ctx.openFileOutput( filename, Context.MODE_PRIVATE);

                copyFile(in, fos);

                in.close();
                fos.flush();
                fos.close();
            } catch(IOException e) {
                Log.e("tag", "Failed to copy asset file: " + filename, e);
            }
        }
        return ctx.getFilesDir();
    }

    public static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }

    public static void copyFile(File source, File dest) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(source);
            os = new FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } finally {
            is.close();
            os.close();
        }
    }
    
    public static String copyFile(InputStream in) {

        StringWriter writer = new StringWriter();
        try {
            IOUtils.copy( in, writer, "UTF-8");
        } catch (IOException e) {
            return "";
        }
        return writer.toString();
    }

    /**
     * Restart the entire application
     * @param ctx
     */
    public static void restartApplication(Context ctx) {

        Intent mStartActivity = new Intent(ctx, ContactListActivity.class);
        int mPendingIntentId = 123456;
        PendingIntent mPendingIntent = PendingIntent.getActivity(ctx,
                mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager mgr = (AlarmManager)ctx.getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
        System.exit(0);
    }

    public static void hideKeyboardFrom(Context ctx, View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) ctx.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    /**
     * Indicates whether the specified action can be used as an intent. This
     * method queries the package manager for installed packages that can
     * respond to an intent with the specified action. If no suitable package is
     * found, this method returns false.
     *
     * @param context The application's environment.
     * @param action The Intent action to check for availability.
     *
     * @return True if an Intent with the specified action can be sent and
     *         responded to, false otherwise.
     */
    public static boolean isIntentAvailable(Context context, String action) {
        final PackageManager packageManager = context.getPackageManager();
        final Intent intent = new Intent(action);
        List<ResolveInfo> list =
                packageManager.queryIntentActivities(intent,
                        PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    /**
     * Indicates whether the specified action can be used as an intent. This
     * method queries the package manager for installed packages that can
     * respond to an intent with the specified action. If no suitable package is
     * found, this method returns false.
     * @param context
     * @param intent
     * @return
     */
    public static boolean isIntentAvailable(Context context, Intent intent) {
        final PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> list =
                packageManager.queryIntentActivities(intent,
                        PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    /** Open another app.
     * @param context current Context, like Activity, App, or Service
     * @param packageName the full package name of the app to open
     * @return true if likely successful, false if unsuccessful
     */
    public static boolean openApp(Context context, String packageName) {

        PackageManager manager = context.getPackageManager();
        Intent i = manager.getLaunchIntentForPackage(packageName);
        if (i == null) {
            return false;
            //throw new PackageManager.NameNotFoundException();
        }
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        context.startActivity(i);

        return true;
    }


    /**
     * Test a string for a valid IP address and port of the
     * form: 0.0.0.0:0000
     * @param ipAndPort
     * @return
     */
    public static String validIpPort(String ipAndPort) {
        try {
            if (ipAndPort == null || ipAndPort.isEmpty()) {
                return "No address";
            }

            String[] parts = ipAndPort.split("\\:");
            if( parts.length != 2)
                return "Missing port number";

            int port = Integer.valueOf(parts[1]);
            if( port <= 1024 || port >= 10000)
                return "Port number out of range 1025 to 9999";

            String[] ipParts = parts[0].split( "\\." );
            if ( ipParts.length != 4 ) {
                return "IP requires 4 parts";
            }

            for ( String s : ipParts) {
                int i = Integer.parseInt( s );
                if ( (i < 0) || (i > 255) ) {
                    return "IP number not 0 to 255";
                }
            }
            if(ipAndPort.endsWith(".")) {
                return "Cannot end with a dot";
            }

            return CConst.OK;

        } catch (NumberFormatException nfe) {
            return "Number format error";
        }
    }

    public static String trimAt(String string, int max) {

        if( string.length() > max)
            string = string.substring(0,max-1)+"..("+string.length()+")";

        return string;
    }

    /**
     * Return true when the device can reach the Internet.
     * @param context
     * @return
     */
    public static boolean checkInternetConnection( Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        // test for connection
        if (cm.getActiveNetworkInfo() != null
                && cm.getActiveNetworkInfo().isAvailable()
                && cm.getActiveNetworkInfo().isConnected()) {
            return true;
        } else {
            LogUtil.log(LogUtil.LogType.UTIL, "Internet Connection Not Present");
            return false;
        }
    }

    /**
     * Return "1 Item" or "0 Items" or "23 Items".
     * @param n
     * @param item
     * @return
     */
    public static String plural(int n, String item) {

        if( n == 1)
            return "1 " + item;
        else
            return n + " " + item + "s";
    }

    /**
     * Set the visibility of a menu item.
     * @param menu
     * @param item
     */
    public static void hideMenu(Menu menu, int item) {

        MenuItem menuItem = menu.findItem(item);
        if( menuItem != null && menuItem.isVisible())
            menuItem.setVisible( false );
    }
    /**
     * Set the visibility of a menu item.
     * @param menu
     * @param item
     */
    public static void showMenu(Menu menu, int item) {

        MenuItem menuItem = menu.findItem(item);
        if( menuItem != null && ! menuItem.isVisible())
            menuItem.setVisible( true );
    }

    /**
     * Delete all files in the apps private TEMP directory.
     * @param act
     */
    public static void cleanupTempFolder(Activity act) {

        File dir = new File( act.getFilesDir() +CConst.SHARE_FOLDER);
        dir.mkdirs();

        File[] files = dir.listFiles();
        for( File f : files){

            f.deleteOnExit();
        }
    }
}
