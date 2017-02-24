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

package com.nuvolect.securesuite.util;//

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TableRow;
import android.widget.TextView;

import com.nuvolect.securesuite.R;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.READ_CONTACTS;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.READ_PHONE_STATE;


/**
 * Show user a dialog allowing them to enable and disable their permissions.
 */
public class PermissionManager {

    private static PermissionManager singleton = null;
    private static Activity m_act;
    private Dialog m_dialog = null;
    private PermissionMgrCallbacks m_callbacks;

    public static PermissionManager getInstance(Activity act){

        m_act = act;

        if( singleton == null){
            singleton = new PermissionManager();
        }
        return singleton;
    }
    private PermissionManager() {
    }

    public String getSummary(){
        String summary = "";
        String separator = "";

        if( hasPermission( READ_CONTACTS )){
            summary = "Contacts";
            separator = ", ";
        }
        if( hasPermission( ACCESS_COARSE_LOCATION)){
            summary += separator + "Location";
            separator = ", ";
        }
        if( hasPermission( READ_PHONE_STATE)){
            summary += separator + "Phone";
            separator = ", ";
        }
        if( hasPermission( READ_EXTERNAL_STORAGE)){
            summary += separator + "Storage";
            separator = ", ";
        }

        if( summary.isEmpty())
            summary = "None";

        return summary;
    }

    private boolean hasPermission(String perm) {
        return(ContextCompat.checkSelfPermission(m_act, perm)==
                PackageManager.PERMISSION_GRANTED);
    }

    public interface PermissionMgrCallbacks {
        public void dialogOnCancel();
    }

    public void showDialog(final PermissionMgrCallbacks callbacks){

        m_dialog = new Dialog(m_act);
        m_callbacks = callbacks;

        LayoutInflater myInflater = (LayoutInflater) m_act.getSystemService(m_act.LAYOUT_INFLATER_SERVICE);
        View view = myInflater.inflate(R.layout.permissions_manager, null);

        m_dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        m_dialog.setContentView(view);
        m_dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        m_dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if( callbacks != null)
                    callbacks.dialogOnCancel();
            }
        });

        setOnClicks(view); // Configure onClick callbacks for each button

        m_dialog.show();
    }

    private void setOnClicks(View view){

        TextView tv = (TextView) view.findViewById(R.id.permissionContactsTv);
        TableRow tr = (TableRow) view.findViewById(R.id.permissionContactsTr);
        if( hasPermission( READ_CONTACTS)){
            tv.setText("Enabled");
            setClickListener( tr );
        }else{
            tv.setText("Disabled");
            tr.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    PermissionUtil.requestReadContacts(m_act,0);
                }
            });
        }

        tv = (TextView) view.findViewById(R.id.permissionStorageTv);
        tr = (TableRow) view.findViewById(R.id.permissionStorageTr);
        if( PermissionUtil.canReadWriteExternalStorage(m_act)){
            tv.setText("Enabled");
            setClickListener( tr );
        }else{
            tv.setText("Disabled");
            tr.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    PermissionUtil.requestReadWriteExternalStorage(m_act,0);
                }
            });
        }

        tv = (TextView) view.findViewById(R.id.permissionPhoneTv);
        tr = (TableRow) view.findViewById(R.id.permissionPhoneTr);
        if( PermissionUtil.canAccessPhoneState(m_act)){
            tv.setText("Enabled");
            setClickListener( tr );
        }else{
            tv.setText("Disabled");
            tr.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    PermissionUtil.requestReadPhoneState(m_act,0);
                }
            });
        }

        ((ImageView) view.findViewById(R.id.refreshPermissionDialogIv)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                m_dialog.cancel();
                showDialog(m_callbacks);
            }
        });
    }

    private void setClickListener(TableRow tr) {

        tr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PermissionUtil.showInstalledAppDetails(m_act);
                m_dialog.cancel();
            }
        });
    }

    /**
     * Called from onRequestPermissionsResult
     */
    public void refresh() {

        if( m_dialog == null)
                return ;

        if( m_dialog.isShowing())
            m_dialog.cancel();
        showDialog(m_callbacks);
    }
}
