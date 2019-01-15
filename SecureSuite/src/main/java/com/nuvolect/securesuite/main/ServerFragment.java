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

package com.nuvolect.securesuite.main;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.nuvolect.securesuite.R;
import com.nuvolect.securesuite.util.AppTheme;
import com.nuvolect.securesuite.util.Cryp;
import com.nuvolect.securesuite.webserver.CrypServer;
import com.nuvolect.securesuite.webserver.WebUtil;

/**
 * Support a dialog to see status and control the embedded server
 */
public class ServerFragment extends DialogFragment {

    private static Activity m_act;
    private static ServerFragment m_fragment;
    private String[] m_password_list;
    private boolean DEBUG = false;
    private boolean rogueFirstSelection;
    private static Callbacks m_listener;
    private ToggleButton m_serverStateTb;
    private View m_refreshIv;
    private TextView m_ipAddressTv;
    private TextView m_lockStatusTv;
    private TextView m_loginStatusTv;

    /**
     * Callback interface used when the generated password is applied to a field.
     */
    public interface Callbacks {

        public void passwordApply(String password);
    }

    public static ServerFragment newInstance(Activity act) {

        m_act = act;
        m_listener = null;
        m_fragment = new ServerFragment();

        // Supply no input as an argument, not required, left as example
        Bundle args = new Bundle();
        m_fragment.setArguments(args);

        return m_fragment;
    }

    public void start(){

        FragmentTransaction ft = m_act.getFragmentManager().beginTransaction();
        Fragment prev = m_act.getFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        this.show(ft, "dialog");
    }

    public void start(Callbacks listener){

        m_listener = listener;
        start();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.server_fragment, container, false);

        // Add the theme background outline and fill color behind fragment
        AppTheme.applyDrawableShape(m_act, rootView);

        m_ipAddressTv = (TextView) rootView.findViewById(R.id.ipAddressTv);
        m_lockStatusTv = (TextView) rootView.findViewById(R.id.lockStatusTv);
        m_loginStatusTv = (TextView) rootView.findViewById(R.id.loginStatusTv);

        rootView.findViewById(R.id.cancelFl).setOnClickListener(cancelButtonOnClick);

        m_serverStateTb = (ToggleButton) rootView.findViewById(R.id.serverStateTb);
        m_serverStateTb.setOnClickListener(onClickServerState);
        m_refreshIv = rootView.findViewById(R.id.refreshIv);
        m_refreshIv.setOnClickListener(onClickRefreshIP);

        m_ipAddressTv.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                copyIpToPasteBuffer();
            }
        });
        m_ipAddressTv.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                browserChooser();
                return false;
            }
        });

        setDialogData();

        return rootView;
    }

    private void copyIpToPasteBuffer() {

        // Gets a handle to the clipboard service.
        ClipboardManager clipboard = (ClipboardManager)
                getActivity().getSystemService(Context.CLIPBOARD_SERVICE);

        String ipAddress = WebUtil.getServerUrl(getActivity());

        // Creates a new text clip to put on the clipboard
        ClipData clip = ClipData.newPlainText("App IP address", ipAddress);

        // Set the clipboard's primary clip.
        clipboard.setPrimaryClip(clip);

        Toast.makeText(getActivity(), "IP address copied to paste buffer", Toast.LENGTH_SHORT).show();
    }

    private void browserChooser(){

        Uri uri = Uri.parse( WebUtil.getServerUrl( getActivity()) );
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);

        Intent chooser = Intent.createChooser( intent, "Choose browser");
        // Verify the intent will resolve to at least one activity
        if (chooser.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivity(chooser);
        }
    }

    private void setDialogData(){

        boolean serverEnabled = CrypServer.isServerEnabled();
        m_serverStateTb.setChecked( serverEnabled);

        if( serverEnabled)
            m_ipAddressTv.setText( WebUtil.getServerUrl(m_act) + " ONLINE");
        else
            m_ipAddressTv.setText( WebUtil.getServerUrl(m_act) + " OFFLINE");

        if ( ! Cryp.getLockCode(m_act).isEmpty())
            m_lockStatusTv.setText("Password secured");
        else
            m_lockStatusTv.setText("NOT password secured");

        String status = Cryp.get(CConst.LAST_LOGIN_STATUS, "No login attempts");
        m_loginStatusTv.setText(status);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        // Request a window without the title
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        return dialog;
    }

    Button.OnClickListener onClickServerState = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            CrypServer.enableServer(m_act, m_serverStateTb.isChecked());
            setDialogData();
        }
    };

    Button.OnClickListener onClickRefreshIP = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            setDialogData();
        }
    };

    Button.OnClickListener cancelButtonOnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if( m_fragment != null && m_fragment.isCancelable()) {

                m_fragment.dismiss();
            }
        }
    };
}
