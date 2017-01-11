package com.nuvolect.securesuite.main;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
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
import com.nuvolect.securesuite.util.LogUtil;
import com.nuvolect.securesuite.webserver.Comm;
import com.nuvolect.securesuite.webserver.CrypServer;
import com.nuvolect.securesuite.webserver.RestfulHtm;
import com.nuvolect.securesuite.webserver.WebUtil;

import java.util.HashMap;
import java.util.Map;

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

        setDialogData();

        return rootView;
    }

    private void setDialogData(){

        boolean serverEnabled = CrypServer.isServerEnabled();
        m_serverStateTb.setChecked( serverEnabled);
        m_ipAddressTv.setText( WebUtil.getServerUrl(m_act) + " OFFLINE");

        if (serverEnabled && !WebUtil.getServerIpPort(m_act).contentEquals(
                App.DEFAULT_IP_PORT)) {

            final String thisDeviceUrl = WebUtil.getServerUrl(m_act, CConst.RESTFUL_HTM);
            Map<String, String> parameters = new HashMap<String, String>();
            parameters.put(RestfulHtm.COMM_KEYS.self_ip_test.toString(), WebUtil.getServerIpPort(m_act));

            LogUtil.log("thisDeviceUrl: " + thisDeviceUrl);

            Comm.sendPostUi(m_act, thisDeviceUrl, parameters, new Comm.CommPostCallbacks() {
                @Override
                public void success(String jsonString) {

                    if (WebUtil.responseMatch(jsonString, CConst.RESPONSE_CODE_SUCCESS_100)) {

                        m_ipAddressTv.setText( WebUtil.getServerUrl(m_act) + " ONLINE");
                    }
                }

                @Override
                public void fail(String error) {

                    m_ipAddressTv.setText( WebUtil.getServerUrl(m_act) + " OFFLINE");
                }
            });
        }

        if (LockActivity.lockCodePresent(m_act))
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
            Toast.makeText(m_act,"Please refresh",Toast.LENGTH_SHORT).show();
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

            m_fragment.dismiss();
        }
    };
}
