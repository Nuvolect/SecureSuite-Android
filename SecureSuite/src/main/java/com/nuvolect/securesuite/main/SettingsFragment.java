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

package com.nuvolect.securesuite.main;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.nuvolect.securesuite.R;
import com.nuvolect.securesuite.data.MyGroups;
import com.nuvolect.securesuite.data.SqlCipher;
import com.nuvolect.securesuite.data.SqlIncSync;
import com.nuvolect.securesuite.license.AppSpecific;
import com.nuvolect.securesuite.license.LicensePersist;
import com.nuvolect.securesuite.util.ActionBarUtil;
import com.nuvolect.securesuite.util.AppTheme;
import com.nuvolect.securesuite.util.DbPassphrase;
import com.nuvolect.securesuite.util.DeviceInfo;
import com.nuvolect.securesuite.util.DialogUtil;
import com.nuvolect.securesuite.util.LogUtil;
import com.nuvolect.securesuite.util.PermissionManager;
import com.nuvolect.securesuite.util.PermissionUtil;
import com.nuvolect.securesuite.util.Util;
import com.nuvolect.securesuite.webserver.Comm;
import com.nuvolect.securesuite.webserver.CrypServer;
import com.nuvolect.securesuite.webserver.RestfulHtm;
import com.nuvolect.securesuite.webserver.WebUtil;

import org.json.JSONException;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SettingsFragment extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final boolean DEBUG = false;
    static Activity m_act;
    private View m_rootView;
    private String mLicenseSummary;
    private static Preference m_backupIpPref;
    private static boolean m_cancelTimer = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        if (DEBUG) LogUtil.log("SettingsFragment onCreate");

        m_act = getActivity();
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.settings);

        // Show the Up button in the action bar.
        ActionBarUtil.setDisplayHomeAsUpEnabled(m_act, true);

        ListPreference listPreference = (ListPreference) findPreference(CConst.THEME_SETTINGS);
        Preference themePref = findPreference(CConst.THEME_SETTINGS);

        CharSequence currText = listPreference.getEntry();
        if (currText == null)
            currText = "Select a theme";

        // Set summary to be the user-description for the selected value
        themePref.setSummary(currText.toString());

        Preference lockPref = findPreference(CConst.LOCK_CODE);
        String lockMsg = "Lock disabled\nSystem disarmed";

        if (LockActivity.lockCodePresent(m_act))
            lockMsg = "Lock enabled\nSystem armed";
        else
            lockMsg = "Lock disabled";

        lockPref.setSummary(lockMsg);

        // Display current port number
        final EditTextPreference portPref = (EditTextPreference)findPreference("port_number");
        portPref.setSummary(String.valueOf(WebUtil.getPort(m_act)));
        portPref.setDefaultValue(String.valueOf(WebUtil.getPort(m_act)));
        portPref.setText(String.valueOf(WebUtil.getPort(m_act)));

        portPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                int portNumber = 0;
                try {
                    portNumber = Integer.valueOf( (String)newValue);
                } catch (NumberFormatException e) {
                    Toast.makeText(m_act, "Invalid, use range 1000:65535",Toast.LENGTH_SHORT).show();
                    return false;
                }
                if( portNumber < 1000 | portNumber > 65535){

                    Toast.makeText(m_act, "Invalid, use range 1000:65535",Toast.LENGTH_SHORT).show();
                    return false;
                }
                WebUtil.setPort(m_act, portNumber);
                preference.setDefaultValue(String.valueOf(portNumber));
                Toast.makeText(m_act, "Restart app to update embedded web server",Toast.LENGTH_SHORT).show();

                preference.setSummary( String.valueOf(portNumber));
                return true;
            }
        });

        // Display current IP address
        final Preference ipPref = findPreference("ip_address");
        ipPref.setSummary(WebUtil.getServerUrl(m_act) + " OFFLINE");
        Map<String, String> parameters = new HashMap<String, String>();

        if (!WebUtil.getServerIpPort(m_act).contentEquals(App.DEFAULT_IP_PORT)) {

            final String thisDeviceUrl = WebUtil.getServerUrl(m_act, CConst.RESTFUL_HTM);
            parameters.put(RestfulHtm.COMM_KEYS.self_ip_test.toString(), WebUtil.getServerIpPort(m_act));

            Comm.sendPostUi(m_act, thisDeviceUrl, parameters, new Comm.CommPostCallbacks() {
                @Override
                public void success(String jsonString) {

                    if (WebUtil.responseMatch(jsonString, CConst.RESPONSE_CODE_SUCCESS_100)) {

                        String summary = WebUtil.getServerUrl(m_act) + " ONLINE";
                        ipPref.setSummary(summary);
                    }
                }

                @Override
                public void fail(String error) {

                    String summary = WebUtil.getServerUrl(m_act) + " OFFLINE";
                    ipPref.setSummary(summary);
                }
            });
        }
        // Display companion IP address
        m_backupIpPref = findPreference(CConst.COMPANION_IP_PORT);
        String companionSummary = WebUtil.getCompanionServerUrl() + " OFFLINE"
                +"\n"+ SqlIncSync.getInstance().getIncomingUpdate()
                +"\n"+ SqlIncSync.getInstance().getOutgoingUpdate();
        m_backupIpPref.setSummary( companionSummary );
        final String companionDeviceIpPort = WebUtil.getCompanionServerIpPort(); // has port #

        if ( Util.validIpPort( companionDeviceIpPort ).contentEquals( CConst.OK )) {

            final String companionDeviceIp = WebUtil.getCompanionServerIp(); // no port #

            Comm.testConnectionOnUi(companionDeviceIp, new Comm.TestConnectionCallbacks() {
                @Override
                public void result(boolean reachable) {

                    if (reachable) {

                        final String companionUrl = WebUtil.getCompanionServerUrl(CConst.RESTFUL_HTM);
                        LogUtil.log("Companion device reachable: " + companionUrl);

                        Map<String, String> parameters = new HashMap<String, String>();
                        parameters.put(RestfulHtm.COMM_KEYS.companion_ip_test.toString(), WebUtil.getServerIpPort(m_act));

                        Comm.sendPostUi(m_act, companionUrl, parameters, new Comm.CommPostCallbacks() {
                            @Override
                            public void success(String jsonString) {

                                String summary;
                                if( WebUtil.responseMatch(jsonString, CConst.RESPONSE_CODE_SUCCESS_100))
                                    summary = WebUtil.getCompanionServerUrl() + " ONLINE"
                                            +"\n"+SqlIncSync.getInstance().getIncomingUpdate()
                                            +"\n"+SqlIncSync.getInstance().getOutgoingUpdate();
                                else
                                    summary = WebUtil.getCompanionServerUrl() + " OFFLINE"
                                            +"\n"+SqlIncSync.getInstance().getIncomingUpdate()
                                            +"\n"+SqlIncSync.getInstance().getOutgoingUpdate();
                                LogUtil.log(LogUtil.LogType.SETTINGS, "Companion device summary: " + summary);
                                m_backupIpPref.setSummary(summary);
                            }

                            @Override
                            public void fail(String error) {

                                String summary = WebUtil.getCompanionServerUrl() + " OFFLINE"
                                        +"\n"+SqlIncSync.getInstance().getIncomingUpdate()
                                        +"\n"+SqlIncSync.getInstance().getOutgoingUpdate();
                                LogUtil.log(LogUtil.LogType.SETTINGS, "Companion device summary: " + summary);
                                m_backupIpPref.setSummary(summary);
                            }
                        });
                    } else {

                        LogUtil.log(LogUtil.LogType.SETTINGS, "Companion device UNREACHABLE: " + companionDeviceIp);
                        String summary = WebUtil.getCompanionServerUrl() + " UNREACHABLE"
                                +"\n"+SqlIncSync.getInstance().getIncomingUpdate()
                                +"\n"+SqlIncSync.getInstance().getOutgoingUpdate();
                        m_backupIpPref.setSummary(summary);
                    }
                }
            });
        }

        // Display country code and national phone number example
        String nationalNumber = "Invalid country code";
        String cc = SettingsActivity.getCountryCode(m_act).toUpperCase(Locale.US);
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        Phonenumber.PhoneNumber localPrototype = phoneUtil.getExampleNumber(cc);

        try {
            if( localPrototype != null ){

                if( SettingsActivity.getInternationalNotation(m_act))
                    nationalNumber = phoneUtil.format(
                            localPrototype, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL);
                else
                    nationalNumber = phoneUtil.format(
                            localPrototype, PhoneNumberUtil.PhoneNumberFormat.NATIONAL);
            }
        } catch (Exception e) {
        }

        Preference countryCodePref = findPreference(CConst.COUNTRY_CODE);
        countryCodePref.setSummary(
                "Country code: "+cc+"\nPhone number standard: "+nationalNumber);

        // Set license summary
        mLicenseSummary = LicensePersist.getLicenseSummary(m_act);
        Preference licensePref = findPreference(LicensePersist.APP_LICENSE);
        licensePref.setSummary(mLicenseSummary);

        // Display current permissions
        updatePermissionsSummary();
    }

    private void updatePermissionsSummary(){

        // Display current permissions
        String permissions = PermissionManager.getInstance(m_act).getSummary();
        Preference permissionManagerPref = findPreference(CConst.PERMISSION_MANAGER);
        permissionManagerPref.setSummary("Enabled: "+permissions);
    }

    int clickCount = 0;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        m_rootView = inflater.inflate(R.layout.settings_preference, container, false);

        // Add the theme background outline and fill color behind fragment
        AppTheme.applyDrawableShape(m_act, m_rootView);

        String version = "";
        try {
            PackageInfo pInfo = m_act.getPackageManager().getPackageInfo(m_act.getPackageName(), 0);
            version = pInfo.versionName;
        } catch (NameNotFoundException e1) {
        }

        TextView appVersionTv = (TextView) m_rootView.findViewById(R.id.settings_app_version);
        appVersionTv.setText(AppSpecific.APP_NAME + " version " + version);
        int m_h2_color = AppTheme.getThemeColor(m_act, R.attr.h2_color);
        appVersionTv.setTextColor(m_h2_color);

        int m_rule_color = AppTheme.getThemeColor(m_act, R.attr.rule_color);
        View v = (View) m_rootView.findViewById(R.id.settings_rule);
        v.setBackgroundColor(m_rule_color);

        appVersionTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (++clickCount > 6) {
                    clickCount = 0;

                    String udi = null;
                    try {
                        udi = DeviceInfo.getDeviceInfo(m_act).toString(2);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    // Gets a handle to the clipboard service.
                    ClipboardManager clipboard = (ClipboardManager)
                            m_act.getSystemService(Context.CLIPBOARD_SERVICE);

                    // Creates a new text clip to put on the clipboard
                    ClipData clip = ClipData.newPlainText("Unique Device ID", udi);

                    // Set the clipboard's primary clip.
                    clipboard.setPrimaryClip(clip);

                    Toast.makeText(m_act, "Device info copied to paste buffer", Toast.LENGTH_SHORT).show();
                }
            }
        });
        return m_rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (DEBUG) LogUtil.log(LogUtil.LogType.SETTINGS, "SettingsFragment onResume");
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (DEBUG) LogUtil.log(LogUtil.LogType.SETTINGS, "SettingsFragment onPause");

        if (DEBUG) LogUtil.log("SettingsFragment onDismiss pairing mode dialog");

        /**
         * Set a flag to break out of the countdown loop.  This solves problems
         * on ICS level devices with async tasks
         */
        m_cancelTimer = true;

        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (DEBUG) LogUtil.log(LogUtil.LogType.SETTINGS, "SettingsFragment onDestroy");
        /**
         * Update in-memory group data.  The user may have synced data from the companion device.
         * This a workaround to solve a crash when exiting settings
         */
        MyGroups.loadGroupMemory();
    }

    /**
     * Display the fragment in the provided container.
     *
     * @param act
     * @param containerViewId
     */
    public static SettingsFragment startSettingsFragment(Activity act, int containerViewId) {

        FragmentTransaction ft = act.getFragmentManager().beginTransaction();
        SettingsFragment frag = new SettingsFragment();
        ft.replace(containerViewId, frag);
        ft.commit();

        return frag;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {

        if (preference.getKey().contentEquals("database_passphrase")) {

            /* Create a dialog showing the clear text passcode
             * Buttons:
             * "Copy" - copy to paste buffer, show Toast
             * "Save" - save the new passcode, update DB passcode, show Toast
             * Never save a clear passcode in preferences.
             */
            ManagePassphraseDialogFragment frag = new ManagePassphraseDialogFragment();
            frag.show(getFragmentManager(), "manage_passphrase");
        }

        if (preference.getKey().contentEquals(LicensePersist.APP_LICENSE)) {

//            String url = "http://www.nuvolect.com/donate/#securesuite";
//            Intent i = new Intent(Intent.ACTION_VIEW);
//            i.setData(Uri.parse(url));
//            startActivity(i);
        }
        if( preference.getKey().contentEquals(CConst.PERMISSION_MANAGER)){

            PermissionManager.getInstance(m_act).showDialog(
                    new PermissionManager.PermissionMgrCallbacks() {
                        @Override
                        public void dialogOnCancel() {

                            updatePermissionsSummary();
                        }
                    });
        }
        if (preference.getKey().contentEquals("notify_incoming_call")) {

            if( preference.isEnabled() && ! PermissionUtil.canAccessPhoneState(m_act)){

                PermissionUtil.requestReadPhoneState(m_act, PermissionUtil.READ_PHONE_STATE);
            }
        }

        if (preference.getKey().contentEquals("ip_address")) {

            String ip = WebUtil.getServerUrl(m_act);

            // Gets a handle to the clipboard service.
            ClipboardManager clipboard = (ClipboardManager)
                    m_act.getSystemService(Context.CLIPBOARD_SERVICE);

            // Creates a new text clip to put on the clipboard
            ClipData clip = ClipData.newPlainText("SecureSuite Device address", ip);

            // Set the clipboard's primary clip.
            clipboard.setPrimaryClip(clip);

            Toast.makeText(m_act, "Copied", Toast.LENGTH_SHORT).show();
        }

        if (preference.getKey().contentEquals(CConst.COMPANION_IP_PORT)) {

            /* Create a dialog showing the backup IP address Buttons:
             * "Copy" - copy to paste buffer, show Toast
             * "Save" - save the new passcode, update DB passcode, show Toast
             */
            CompanionIpPortDialogFragment frag = new CompanionIpPortDialogFragment();
            frag.show(getFragmentManager(), "manage_companion_ip_port");
        }

        if (preference.getKey().contentEquals("open_source_license")) {

            DisplayOpenSourceInfoFragment frag = new DisplayOpenSourceInfoFragment();
            frag.show(getFragmentManager(), "display_open_source_info");
        }

        if (preference.getKey().contentEquals(CConst.LOCK_CODE)) {

            manageLockCode();
        }

        if (preference.getKey().contains("rate_app_google_play")) {

            String url = AppSpecific.APP_GOOGLE_PLAY_URL;
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void manageLockCode() {

        LockCodeFragment f = LockCodeFragment.newInstance(m_act);
        f.start();
    }

    public static class DisplayOpenSourceInfoFragment extends DialogFragment {
        static DisplayOpenSourceInfoFragment newInstance() {
            return new DisplayOpenSourceInfoFragment();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

            View v = inflater.inflate(R.layout.open_source_license, container, false);

            setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Holo_Light);
            getDialog().setTitle("Software Licenses");

            return v;
        }
    }

    public static class ManagePassphraseDialogFragment extends DialogFragment {

        private EditText m_passphraseEt;

        public ManagePassphraseDialogFragment() {
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Manage database passphrase");
            builder.setMessage("Copy your current passphrase, or enter a new passphrase");

            m_passphraseEt = new EditText(m_act);
            String cleartextPassphrase = DbPassphrase.getDbPassphrase(m_act);
            m_passphraseEt.setText(cleartextPassphrase);
            builder.setView(m_passphraseEt);

            builder.setPositiveButton("Change", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {

                    String newPassphrase = m_passphraseEt.getText().toString();

                    if (newPassphrase.length() < 4) {

                        Toast.makeText(m_act, "Passphrase minimum length is 4 characters", Toast.LENGTH_LONG).show();
                        Toast.makeText(m_act, "No changes made", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Boolean success = SqlCipher.rekey(m_act, newPassphrase);

                    if (success)
                        Toast.makeText(m_act, "Passphrase changed", Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(m_act, "Passphrase change failed", Toast.LENGTH_SHORT).show();
                }
            })
                    .setNeutralButton("Copy", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {

                            String passphrase = m_passphraseEt.getText().toString();

                            // Gets a handle to the clipboard service.
                            ClipboardManager clipboard = (ClipboardManager)
                                    m_act.getSystemService(Context.CLIPBOARD_SERVICE);

                            // Creates a new text clip to put on the clipboard
                            ClipData clip = ClipData.newPlainText("SecureSuite database passphrase", passphrase);

                            // Set the clipboard's primary clip.
                            clipboard.setPrimaryClip(clip);

                            Toast.makeText(m_act, "Copied to paste buffer", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Email", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {

                            String passphrase = m_passphraseEt.getText().toString();

                            String version = "";
                            try {
                                PackageInfo pInfo = m_act.getPackageManager().getPackageInfo(m_act.getPackageName(), 0);
                                version = pInfo.versionName;
                            } catch (NameNotFoundException e1) {
                            }
                            String s1 = "Your SecureSuite passphrase is attached.\n\n"
                                    + "Your passphrase is necessary in the event you need to restore a database or you replace your phone.\n\n";
                            String s2 = "Visit http://nuvolect.com/securesuite_help for additional support information.\n\n"
                                    + "SecureSuite version: " + version + "\n\n";

                            String userMessage = s1 + "UserPassphrase: " + passphrase + "\n\n" + s2;

                            Intent intent = new Intent(Intent.ACTION_SEND);
                            intent.setType("text/plain");
                            intent.putExtra(Intent.EXTRA_SUBJECT, "SecureSuite Passphrase");
                            intent.putExtra(Intent.EXTRA_TEXT, userMessage);
                            m_act.startActivity(Intent.createChooser(intent, "Send email..."));
                        }
                    });
            // Create the AlertDialog object and return it
            return builder.create();
        }
    }

    /**
     * Manage pairing mode.  Host verification is disabled for the duration of a countdown timer.
     * Every few seconds the UI is updated with the companion device configuration.
     */
    public static class PairingModeDialogFragment extends DialogFragment {


        private AlertDialog m_dialog;
        private static TextView countdown_tv = null;
        private static TextView connection_status_tv = null;
        private static AsyncTask<Void, Integer, Void> countdownTask;

        public PairingModeDialogFragment(){
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            View view = m_act.getLayoutInflater().inflate(R.layout.pairing_mode_alert_dialog, null);
            builder.setView(view);

            TextView ip_port_tv = (TextView) view.findViewById(R.id.ip_port_tv);
            countdown_tv = (TextView) view.findViewById(R.id.countdown_tv);
            connection_status_tv = (TextView) view.findViewById(R.id.connection_status_tv);
            ip_port_tv.setText(WebUtil.getServerIpPort(m_act));

            view.findViewById(R.id.pm_cancelFl).setOnClickListener(onClickListener);
            view.findViewById(R.id.pm_cancel_button).setOnClickListener(onClickListener);

            // Disable host verifier, any host can connect during this period
            WebUtil.NullHostNameVerifier.getInstance().setHostVerifierEnabled(false);
            /**
             * Start a repeating task to count down and test access to the companion device
             */
            m_cancelTimer = false;
            countdownTask = new CountdownTask().execute();

            builder.setCancelable(false);
            m_dialog = builder.create();
            return m_dialog;
        }

        OnClickListener onClickListener = new OnClickListener(){
            @Override
            public void onClick(View v) {

                switch ( v.getId() ){

                    case R.id.pm_cancelFl:
                    case R.id.pm_cancel_button:{

                        // User canceled, re-enable host verifier
                        WebUtil.NullHostNameVerifier.getInstance().setHostVerifierEnabled( true );
                        /**
                         * Set a flag to break out of the countdown loop.  This solves problems
                         * on ICS level devices with async tasks
                         */
                        m_cancelTimer = true;
                        countdownTask.cancel( true );
                        m_dialog.dismiss();
                        break;
                    }
                }
            }
        };

        private static class CountdownTask extends AsyncTask<Void, Integer, Void>{

            @Override
            protected Void doInBackground(Void... params) {

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            for( int i = 300; i >= 0; i--){

                                publishProgress(i);
                                Thread.sleep( 1000L);

                                if( m_cancelTimer)
                                    break;
                            }
                            // Timer expired, re-enable host verifier
                            WebUtil.NullHostNameVerifier.getInstance().setHostVerifierEnabled( true );

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
                return null;
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                super.onProgressUpdate(values);

                int secondsRemaining = values[ 0 ];

                int minutes = secondsRemaining / 60;
                int seconds = secondsRemaining % 60;

                countdown_tv.setText(minutes + ":" + (seconds < 10 ? "0" + seconds : seconds));

                if( secondsRemaining % 2 == 0){// test every 2 seconds
                    Map<String, String> parameters = new HashMap<String, String>();
                    parameters.put(RestfulHtm.COMM_KEYS.companion_ip_test.toString(), WebUtil.getServerIpPort(m_act));

                    String companionServerIpPort = WebUtil.getCompanionServerIpPort();
                    String validationReport = Util.validIpPort(companionServerIpPort);

                    if( validationReport.contentEquals(CConst.OK)) {

                        String companionServerUrl = WebUtil.getCompanionServerUrl( CConst.RESTFUL_HTM);
                        Comm.sendPostUi(m_act, companionServerUrl, parameters, new Comm.CommPostCallbacks() {
                            @Override
                            public void success(String jsonString) {

                                if (! jsonString.isEmpty() && WebUtil.responseMatch(jsonString, CConst.RESPONSE_CODE_SUCCESS_100)) {

                                    connection_status_tv.setText("Companion SecureSuite ONLINE");
                                    connection_status_tv.setTextColor(m_act.getResources().getColor(R.color.green));
                                } else {

                                    connection_status_tv.setText("Companion SecureSuite OFFLINE");
                                    connection_status_tv.setTextColor(m_act.getResources().getColor(R.color.bluedark));
                                }
                            }

                            @Override
                            public void fail(String error) {

                                connection_status_tv.setText("Companion SecureSuite OFFLINE");
                                connection_status_tv.setTextColor(m_act.getResources().getColor(R.color.bluedark));
                            }
                        });
                    }
                    else {

                        connection_status_tv.setText("Companion SecureSuite OFFLINE");
                        connection_status_tv.setTextColor(m_act.getResources().getColor(R.color.bluedark));
                    }
                }
            }
        }
    }

    public static class CompanionIpPortDialogFragment extends DialogFragment {

        private static String m_ip_port = "";
        private AlertDialog m_dialog;
        private static TextView m_ip_port_tv;
        private static TextView m_error_message_tv;

        public CompanionIpPortDialogFragment() {
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            View view = m_act.getLayoutInflater().inflate(R.layout.ip_port_alert_dialog, null);
            builder.setView(view);
            builder.setCancelable(false);
            m_error_message_tv = (TextView) view.findViewById(R.id.ip_error_message_tv);
            m_ip_port_tv = (TextView) view.findViewById(R.id.ip_port_tv);

            m_ip_port = WebUtil.getCompanionServerIpPort();
            if( m_ip_port.contentEquals(App.DEFAULT_IP_PORT))
                m_ip_port = "";// display hint text
            else
                m_ip_port_tv.setText(m_ip_port);// display current ip port

            OnClickListener buttonOnClick = new OnClickListener() {
                @Override
                public void onClick(View v) {

                    switch (v.getId()) {

                        case R.id.b_0:
                            m_ip_port += "0";
                            break;
                        case R.id.b_1:
                            m_ip_port += "1";
                            break;
                        case R.id.b_2:
                            m_ip_port += "2";
                            break;
                        case R.id.b_3:
                            m_ip_port += "3";
                            break;
                        case R.id.b_4:
                            m_ip_port += "4";
                            break;
                        case R.id.b_5:
                            m_ip_port += "5";
                            break;
                        case R.id.b_6:
                            m_ip_port += "6";
                            break;
                        case R.id.b_7:
                            m_ip_port += "7";
                            break;
                        case R.id.b_8:
                            m_ip_port += "8";
                            break;
                        case R.id.b_9:
                            m_ip_port += "9";
                            break;
                        case R.id.b_colon:
                            m_ip_port += ":";
                            break;
                        case R.id.b_dot:
                            m_ip_port += ".";
                            break;
                        case R.id.set_button: {

                            String testResult = validateIpPort(m_ip_port, m_ip_port_tv, m_error_message_tv);
                            WebUtil.setCompanionServerIpPort(m_ip_port);

                            if( testResult.contentEquals(CConst.OK)){

                                /**
                                 * Register companion server.
                                 * UI will be updated in the async task
                                 */
                                new RegisterCompanionServerTask().execute();
                            }
                            break;
                        }
                        case R.id.copy_button: {
                            // Gets a handle to the clipboard service.
                            ClipboardManager clipboard = (ClipboardManager)
                                    m_act.getSystemService(Context.CLIPBOARD_SERVICE);

                            // Creates a new text clip to put on the clipboard
                            ClipData clip = ClipData.newPlainText("SecureSuite backup IP address", m_ip_port);

                            // Set the clipboard's primary clip.
                            clipboard.setPrimaryClip(clip);

                            Toast.makeText(m_act, "Copied to paste buffer", Toast.LENGTH_SHORT).show();
                            break;
                        }
                        case R.id.backspace_button: {
                            if (m_ip_port.length() == 0)
                                break;
                            if (m_ip_port.length() == 1) {
                                m_ip_port = "";
                                break;
                            }
                            m_ip_port = m_ip_port.substring(0, m_ip_port.length() - 1);
                            break;
                        }
                        case R.id.done_button:
                        case R.id.ip_cancelFl:
                            m_dialog.dismiss();
                            break;
                    }
                    m_ip_port_tv.setText(m_ip_port);
                }
            };

            View.OnLongClickListener setButtonLongClick = new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {

                    DialogUtil.confirmDialog(
                            m_act,
                            "Reset Sync Server",
                            "Synchronization will be disabled, please confirm.",
                            new DialogUtil.DialogUtilCallbacks() {
                                @Override
                                public void confirmed( boolean confirmed) {

                                    // Clear history
                                    SqlCipher.clearCryp(CConst.LAST_INCOMING_UPDATE);
                                    SqlCipher.clearCryp(CConst.LAST_OUTGOING_UPDATE);

                                    // Clear sync database entries
                                    SqlCipher.dropIncSyncTable();

                                    // Reset sync flag
                                    SqlIncSync.getInstance().setSyncEnabled(false);

                                    // Reset IP to default
                                    WebUtil.setCompanionServerIpPort(App.DEFAULT_IP_PORT);

                                    // Toast confirmation
                                    Toast.makeText(m_act, "Sync has been disabled",Toast.LENGTH_SHORT).show();
                                }
                            });
                    return true;// click is consumed
                }
            };

            view.findViewById(R.id.b_0).setOnClickListener(buttonOnClick);
            view.findViewById(R.id.b_1).setOnClickListener(buttonOnClick);
            view.findViewById(R.id.b_2).setOnClickListener(buttonOnClick);
            view.findViewById(R.id.b_3).setOnClickListener(buttonOnClick);
            view.findViewById(R.id.b_4).setOnClickListener(buttonOnClick);
            view.findViewById(R.id.b_5).setOnClickListener(buttonOnClick);
            view.findViewById(R.id.b_6).setOnClickListener(buttonOnClick);
            view.findViewById(R.id.b_7).setOnClickListener(buttonOnClick);
            view.findViewById(R.id.b_8).setOnClickListener(buttonOnClick);
            view.findViewById(R.id.b_9).setOnClickListener(buttonOnClick);
            view.findViewById(R.id.b_colon).setOnClickListener(buttonOnClick);
            view.findViewById(R.id.b_dot).setOnClickListener(buttonOnClick);
            view.findViewById(R.id.set_button).setOnClickListener(buttonOnClick);
            view.findViewById(R.id.set_button).setOnLongClickListener(setButtonLongClick);
            view.findViewById(R.id.copy_button).setOnClickListener(buttonOnClick);
            view.findViewById(R.id.backspace_button).setOnClickListener(buttonOnClick);
            view.findViewById(R.id.ip_cancelFl).setOnClickListener(buttonOnClick);
            view.findViewById(R.id.done_button).setOnClickListener(buttonOnClick);

            m_dialog = builder.create();
            return m_dialog;
        }

        /**
         * Validate the ip and port number and update the dialog.
         * @param ip_port
         * @param ip_port_tv
         * @param error_message_tv
         */
        private String validateIpPort(final String ip_port, final TextView ip_port_tv, final TextView error_message_tv) {

            String inputTestReport = Util.validIpPort(ip_port);
            error_message_tv.setText("");// clear out existing message
            error_message_tv.setVisibility(View.GONE);

            if ( inputTestReport.contentEquals(CConst.OK)) {

                if (ip_port.contentEquals(WebUtil.getServerIp(m_act))) {

                    String message = "Companion IP can't be this device IP.";
                    error_message_tv.setText( message );
                    error_message_tv.setVisibility(View.VISIBLE);
                    return message;
                }else
                    return CConst.OK;

            } else {

                ip_port_tv.setText( "https://"+ip_port);
                error_message_tv.setText(inputTestReport);
                error_message_tv.setVisibility(View.VISIBLE);
                return inputTestReport;
            }
        }

        /**
         * Register the companion server.  The arrangement is bi-laterial meaning that each is
         * the companion server of the other.  Define a security token to be passed with all
         * future communications.
         */
        private static class RegisterCompanionServerTask extends AsyncTask<Void, Void, Boolean>{

            @Override
            protected Boolean doInBackground(Void... params) {

                String ip = WebUtil.getCompanionServerIp();
                boolean reachable = false;
                try {
                    reachable = InetAddress.getByName(ip).isReachable(CConst.IP_TEST_TIMEOUT_MS);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return reachable;
            }

            @Override
            protected void onPostExecute(Boolean reachable){

                if (reachable) {

                    /**
                     * Save the IP, update the settings post a registration request.
                     */
                    WebUtil.setCompanionServerIpPort(m_ip_port);
                    String backupIpSummary = WebUtil.getCompanionServerUrl() + " OFFLINE";
                    m_backupIpPref.setSummary(backupIpSummary);
                    m_error_message_tv.setVisibility(View.GONE);

                    String ipTest = Util.validIpPort( m_ip_port );

                    if ( ipTest.contentEquals( CConst.OK )) {

                        final String companionUrl = WebUtil.getCompanionServerUrl(CConst.RESTFUL_HTM);
                        final String myIpPort = WebUtil.getServerIpPort(m_act);

                        Map<String, String> parameters = new HashMap<String, String>();
                        parameters = new HashMap<String, String>();
                        parameters.put(RestfulHtm.COMM_KEYS.register_companion_device.toString(), myIpPort);
                        parameters.put(CConst.SEC_TOK, CrypServer.getSecTok());

                        Comm.sendPostUi(m_act, companionUrl, parameters, new Comm.CommPostCallbacks() {
                            @Override
                            public void success(String jsonObject) {

                                if( WebUtil.responseMatch(jsonObject, CConst.RESPONSE_CODE_SUCCESS_100)){

                                    m_ip_port_tv.setText(m_ip_port + " ONLINE");
                                    m_backupIpPref.setSummary(WebUtil.getCompanionServerUrl() + " ONLINE");
                                }else{
                                    m_ip_port_tv.setText(m_ip_port + " OFFLINE");
                                    m_backupIpPref.setSummary(WebUtil.getCompanionServerUrl() + " OFFLINE");
                                }
                            }

                            @Override
                            public void fail(String error) {

                                m_ip_port_tv.setText(m_ip_port + " OFFLINE");
                                m_backupIpPref.setSummary(WebUtil.getCompanionServerUrl() + " OFFLINE");
                            }
                        });
                    }else{

                        m_ip_port_tv.setText(m_ip_port + " Error: "+ipTest);
                        m_backupIpPref.setSummary(WebUtil.getCompanionServerUrl() + " OFFLINE");
                    }
                } else {

                    m_error_message_tv.setText("Companion device us unreachable");
                    m_error_message_tv.setVisibility(View.VISIBLE);
                    return;
                }
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences pref, String key) {

        if (key.equals(CConst.THEME_SETTINGS)) {

            /*
             * The user selected a new theme.  Restart the activity to show it off.
             */
            Intent intent = m_act.getIntent();
            m_act.finish();
            m_act.startActivity(intent);
        } else if (key.equals(CConst.DATABASE_PASSPHRASE)) {

            //TODO finish or remove
        }
    }

}
