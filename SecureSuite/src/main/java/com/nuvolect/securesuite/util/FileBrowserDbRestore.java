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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.nuvolect.securesuite.R;
import com.nuvolect.securesuite.data.SqlCipher;
import com.nuvolect.securesuite.main.CConst;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import static com.nuvolect.securesuite.data.SqlCipher.DETAIL_DB_NAME;

@SuppressWarnings("deprecation")
public class FileBrowserDbRestore extends Activity {

    private static final boolean DEBUG = false;
    // Stores names of traversed directories
    ArrayList<String> str = new ArrayList<String>();

    // Check if the first level of the directory structure is the one showing
    private Boolean firstLvl = true;

    private Item[] fileList;
    private File path = new File(Environment.getExternalStorageDirectory() + "");
    private String basePath = path.getPath();
    private String chosenFile;
    private static final int DIALOG_LOAD_FILE = 2200;
    private static final int REQUEST_ID_READ_CONTACTS = 123;

    ListAdapter adapter;

    private String TAG = "SecureSuite.FileBrowser";

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if( PermissionUtil.canReadExternalStorage(this)) {
            loadFileList();
            showDialog(DIALOG_LOAD_FILE);
        }
        else
            PermissionUtil.requestReadExternalStorage(this, REQUEST_ID_READ_CONTACTS);

        if(DEBUG)LogUtil.log( path.getAbsolutePath());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch( requestCode){

            case REQUEST_ID_READ_CONTACTS:{

                if( grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    loadFileList();
                    showDialog(DIALOG_LOAD_FILE);

                    if (DEBUG) LogUtil.log(path.getAbsolutePath());
                }
                else
                    finish();
                break;
            }
            default:
        }
    }

    class MyComparator implements Comparator<String> {
        public int compare(String strA, String strB) {
          return strA.compareToIgnoreCase(strB);
        }
      }
    MyComparator icc = new MyComparator();


    private void loadFileList() {
        try {
            path.mkdirs();
        } catch (SecurityException e) {
            LogUtil.e( TAG, "unable to write on the sd card ");
        }

        // Checks whether path exists
        if (path.exists()) {
            FilenameFilter filter = new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    File sel = new File(dir, filename);
                    // Filters based on whether the file is hidden or not
                    return (sel.isFile() || sel.isDirectory()) && !sel.isHidden();
                }
            };

            String[] fList = path.list(filter);
            Arrays.sort(fList, icc);
            fileList = new Item[fList.length];
            for (int i = 0; i < fList.length; i++) {
                fileList[i] = new Item(fList[i], R.drawable.file_icon);

                // Convert into file path
                File sel = new File(path, fList[i]);

                // Set drawables
                if (sel.isDirectory()) {
                    fileList[i].icon = R.drawable.directory_icon;
                    if(DEBUG)LogUtil.log("DIRECTORY: "+ fileList[i].file);
                } else {
                    if(DEBUG)LogUtil.log("FILE: "+ fileList[i].file);
                }
            }

            if (!firstLvl) {
                Item temp[] = new Item[fileList.length + 1];
                for (int i = 0; i < fileList.length; i++) {
                    temp[i + 1] = fileList[i];
                }
                temp[0] = new Item("Up", R.drawable.directory_up);
                fileList = temp;
            }
        } else {
            LogUtil.e( TAG, "path does not exist");
        }

        adapter = new ArrayAdapter<Item>(this,
                android.R.layout.select_dialog_item, android.R.id.text1, fileList) {

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                // creates view
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view
                        .findViewById(android.R.id.text1);

                // put the image on the text view
                textView.setCompoundDrawablesWithIntrinsicBounds(
                        fileList[position].icon, 0, 0, 0);

                // add margin between image and text (support various screen densities)
                int dp5 = (int) (5 * getResources().getDisplayMetrics().density + 0.5f);
                textView.setCompoundDrawablePadding(dp5);

                return view;
            }
        };
    }

    private class Item {
        public String file;
        public int icon;

        public Item(String file, Integer icon) {
            this.file = file;
            this.icon = icon;
        }

        @Override
        public String toString() {
            return file;
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        AlertDialog.Builder builder = new Builder(this);

        if (fileList == null) {
            LogUtil.e( TAG, "No files loaded");
            dialog = builder.create();
            return dialog;
        }

        String title = path.toString().replace( basePath, "");
        if( title.contentEquals(""))
            title = "/";

        switch (id) {
        case DIALOG_LOAD_FILE:
            builder.setTitle( title );
            builder.setCancelable(true);
            builder.setPositiveButton("Restore backup", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(DEBUG)LogUtil.log("Path selected: "+path);

                    File f1 = new File( path + "/" + SqlCipher.ACCOUNT_DB_NAME);
                    File f2 = new File( path + "/" + DETAIL_DB_NAME);

                    boolean validBackup = f1.exists() && f1.canRead() && f2.exists() && f2.canRead();

                    if( ! validBackup) {
                        f1 = new File(path + "/" + "crypsafe1_db");// TODO Remove after upgrade period
                        f2 = new File(path + "/" + "crypsafe2_db");

                        validBackup = f1.exists() && f1.canRead() && f2.exists() && f2.canRead();
                    }

                    if( validBackup ){

                        // Send action performed back to calling activity
                        Intent data = new Intent();
                        data.putExtra( CConst.RESTORE_BACKUP_PATH, path.toString());
                        setResult(Activity.RESULT_OK, data);
                        finish();
                        return;
                    }
                    else {
                        Toast.makeText(getApplicationContext(),
                                "Error, invalid folder", Toast.LENGTH_LONG).show();
                        Toast.makeText(getApplicationContext(),
                                "Two database files are required", Toast.LENGTH_LONG).show();
                        Toast.makeText(getApplicationContext(),
                                SqlCipher.ACCOUNT_DB_NAME+" and "+ DETAIL_DB_NAME,
                                Toast.LENGTH_LONG).show();

                        removeDialog(DIALOG_LOAD_FILE);
                        showDialog(DIALOG_LOAD_FILE);
                    }
                }
            });

            // Handle the back button
            builder.setOnKeyListener( new Dialog.OnKeyListener() {

                @Override
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {

                    Intent data = new Intent();
                    data.putExtra(CConst.RESTORE_BACKUP_PATH, "");// empty path
                    setResult(Activity.RESULT_CANCELED, data);
                    finish();
                    return true;
                }
            });

            builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    chosenFile = fileList[which].file;
                    File sel = new File(path + "/" + chosenFile);

                    if (sel.isDirectory()) {
                        firstLvl = false;

                        // Adds chosen directory to list
                        str.add(chosenFile);
                        fileList = null;
                        path = new File(sel + "");

                        loadFileList();

                        removeDialog(DIALOG_LOAD_FILE);
                        showDialog(DIALOG_LOAD_FILE);
                        if(DEBUG)LogUtil.log( path.getAbsolutePath());

                    } else if (chosenFile.equalsIgnoreCase("up") && !sel.exists()) {
                    // Checks if 'up' was clicked

                        // present directory removed from list
                        String s = str.remove(str.size() - 1);

                        // path modified to exclude present directory
                        path = new File(path.toString().substring(0,
                                path.toString().lastIndexOf(s)));
                        fileList = null;

                        // if there are no more directories in the list, then
                        // its the first level
                        if (str.isEmpty()) {
                            firstLvl = true;
                        }
                        loadFileList();

                        removeDialog(DIALOG_LOAD_FILE);
                        showDialog(DIALOG_LOAD_FILE);
                        if(DEBUG)LogUtil.log( path.getAbsolutePath());
                    }else{
                        removeDialog(DIALOG_LOAD_FILE);
                        showDialog(DIALOG_LOAD_FILE);
                    }
                }
            });
            break;
        }
        dialog = builder.show();
        return dialog;
    }
    protected void onActivityResult(int startType, int resultCode, Intent data) {

        loadFileList();

        removeDialog(DIALOG_LOAD_FILE);
        showDialog(DIALOG_LOAD_FILE);
        if(DEBUG)LogUtil.log( "FileBrowser.onActivityResult: "+path.getAbsolutePath());
//        Intent intent = getIntent();
//        finish();
//        startActivity(intent);
    }
}