package com.nuvolect.securesuite.util;

import android.app.ActionBar;
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

import com.nuvolect.securesuite.R;
import com.nuvolect.securesuite.main.CConst;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

@SuppressWarnings("deprecation")
public class FileBrowserImportVcf extends Activity {

    private static final boolean DEBUG = false;
    // Stores names of traversed directories
    ArrayList<String> str = new ArrayList<String>();

    // Check if the first level of the directory structure is the one showing
    private Boolean firstLvl = true;

    private Item[] fileList;
    private File path = new File(Environment.getExternalStorageDirectory() + "");
    private String basePath = path.getPath();
    private String chosenFile;
    private static final int DIALOG_LOAD_FILE = 1000;
    private static final int REQUEST_ID_READ_CONTACTS = 123;

    ListAdapter adapter;

    private String TAG = "SecureSuite.FileBrowser";

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);


        ActionBar actionBar = getActionBar();
        ActionBarUtil.setDisplayShowTitleEnabled(actionBar, false);
        ActionBarUtil.setDisplayHomeAsUpEnabled(actionBar, true);

        if( PermissionUtil.canReadExternalStorage(this)) {
            loadFileList();
            showDialog(DIALOG_LOAD_FILE);
        }
        else
            PermissionUtil.requestReadExternalStorage(this, REQUEST_ID_READ_CONTACTS);
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
//            builder.setPositiveButton("Select file", new DialogInterface.OnClickListener() {
//
//                @Override
//                public void onClick(DialogInterface dialog, int which) {
//                    if(DEBUG)LogUtil.log("Path selected: "+path);
//
//                    File f1 = new File( path + "");
//
//                    if( f1.exists() && f1.canRead()){
//
//                        // Send action performed back to calling activity
//                        Intent data = new Intent();
//                        data.putExtra( CConst.IMPORT_VCF_PATH, path.toString());
//                        setResult(Activity.RESULT_OK, data);
//                        finish();
//                        return;
//                    }else{
//                        Toast.makeText(getApplicationContext(),
//                                "Error, invalid file", Toast.LENGTH_LONG).show();
//
//                        removeDialog(DIALOG_LOAD_FILE);
//                        showDialog(DIALOG_LOAD_FILE);
//                    }
//                }
//            });

            // Handle the back button
            builder.setOnKeyListener( new Dialog.OnKeyListener() {

                @Override
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {

                    Intent data = new Intent();
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
                        // File picked
                    }else{

                        // Send action performed back to calling activity
                        Intent data = new Intent();
                        data.putExtra( CConst.IMPORT_VCF_PATH, path.toString()+"/"+chosenFile);
                        setResult(Activity.RESULT_OK, data);
                        finish();

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
    }
}