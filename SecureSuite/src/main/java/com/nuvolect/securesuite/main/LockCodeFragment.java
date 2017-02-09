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
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.nuvolect.securesuite.R;
import com.nuvolect.securesuite.nfc.NfcSession;
import com.nuvolect.securesuite.util.AppTheme;
import com.nuvolect.securesuite.util.Cryp;
import com.nuvolect.securesuite.util.LogUtil;

public class LockCodeFragment extends DialogFragment {

    private static Activity m_act;
    private static LockCodeFragment m_fragment;
    private boolean DEBUG = false;
    private View mRootView;
    private int scanningYubikeySlot = 0;

    public static LockCodeFragment newInstance(Activity act) {

        m_act = act;
        m_fragment = new LockCodeFragment();

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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mRootView = inflater.inflate(R.layout.lock_code_fragment, container, false);

        // Add the theme background outline and fill color behind fragment
        AppTheme.applyDrawableShape(m_act, mRootView);

        mRootView.findViewById(R.id.lockCodeButton).setOnClickListener(onClickLockCodeButton);
        mRootView.findViewById(R.id.confirmClearLockCodeButton).setOnClickListener(onClickConfirmClearLockCodeButton);
        mRootView.findViewById(R.id.showCodeCb).setOnClickListener(onClickShowCodeCb);
        mRootView.findViewById(R.id.yubiKey1Button).setOnClickListener(onClickYubiKey1Button);
        mRootView.findViewById(R.id.confirmClearYubiKey1Button).setOnClickListener(onClickConfirmClearYubiKey1Button);
        mRootView.findViewById(R.id.cancelScanYubiKey1Button).setOnClickListener(onClickCancelYubiKey1ScanButton);
        mRootView.findViewById(R.id.yubiKey2Button).setOnClickListener(onClickYubiKey2Button);
        mRootView.findViewById(R.id.confirmClearYubiKey2Button).setOnClickListener(onClickConfirmClearYubiKey2Button);
        mRootView.findViewById(R.id.cancelScanYubiKey2Button).setOnClickListener(onClickCancelYubiKey2ScanButton);
        mRootView.findViewById(R.id.pw_cancelFl).setOnClickListener(cancelButtonOnClick);

        /**
         * Set lock code button and details
         */
        manageLockCodeDisplay();

        /**
         * Set YubiKey button and details
         */
        manageYubiKey1Display();
        manageYubiKey2Display();

        return mRootView;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        // Request a window without the title
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        return dialog;
    }

    private void manageLockCodeDisplay(){

        String lockCode;
        if( (lockCode = Cryp.getLockCode(m_act)).isEmpty()){
            ((Button)mRootView.findViewById(R.id.lockCodeButton)).setText("Set code");
            ((TextView) mRootView.findViewById(R.id.lockCodeTv)).setText("Lock code not set");
        }
        else {
            ((Button) mRootView.findViewById(R.id.lockCodeButton)).setText("Clear");
            if( ((CheckBox) mRootView.findViewById(R.id.showCodeCb)).isChecked())
                ((TextView) mRootView.findViewById(R.id.lockCodeTv)).setText(lockCode);
            else
                ((TextView) mRootView.findViewById(R.id.lockCodeTv)).setText("Lock code is set");
        }
    }

    private void manageYubiKey1Display() {

        String serial;
        if( (serial = Cryp.get(CConst.YUBIKEY_SERIAL1)).isEmpty()){
            ((Button) mRootView.findViewById(R.id.yubiKey1Button)).setText("Scan key 1");
            ((TextView) mRootView.findViewById(R.id.yubiKey1Tv)).setText("YubiKey #1 not set");
        }
        else {
            ((Button) mRootView.findViewById(R.id.yubiKey1Button)).setText("Clear");
            ((TextView) mRootView.findViewById(R.id.yubiKey1Tv)).setText("YubiKey #1 Serial: "+serial);
        }
    }

    private void manageYubiKey2Display() {

        String serial;
        if( (serial = Cryp.get(CConst.YUBIKEY_SERIAL2)).isEmpty()){
            ((Button) mRootView.findViewById(R.id.yubiKey2Button)).setText("Scan key 2");
            ((TextView) mRootView.findViewById(R.id.yubiKey2Tv)).setText("YubiKey #2 not set");
        }
        else {
            ((Button) mRootView.findViewById(R.id.yubiKey2Button)).setText("Clear");
            ((TextView) mRootView.findViewById(R.id.yubiKey2Tv)).setText("YubiKey #2 Serial: "+serial);
        }
    }

    NfcSession.NfcCallbacks nfcCallbacks = new NfcSession.NfcCallbacks() {
        @Override
        public void yubiKeySerial(String serial) {

            switch (scanningYubikeySlot){

                case 1:
                    Cryp.put(CConst.YUBIKEY_SERIAL1, serial);
                    manageYubiKey1Display();
                    mRootView.findViewById(R.id.yubiKey1ScanningTv).setVisibility(View.GONE);
                    mRootView.findViewById(R.id.cancelScanYubiKey1Button).setVisibility(View.GONE);
                    mRootView.findViewById(R.id.yubiKey2Button).setClickable(true);
                    // Cancel scanning
                    NfcSession.getInstance().disableNfcForegroundDispatch(m_act);
                    break;
                case 2:
                    Cryp.put(CConst.YUBIKEY_SERIAL2, serial);
                    manageYubiKey2Display();
                    mRootView.findViewById(R.id.yubiKey2ScanningTv).setVisibility(View.GONE);
                    mRootView.findViewById(R.id.cancelScanYubiKey2Button).setVisibility(View.GONE);
                    mRootView.findViewById(R.id.yubiKey1Button).setClickable(true);
                    // Cancel scanning
                    NfcSession.getInstance().disableNfcForegroundDispatch(m_act);
                    break;
                default:
                    LogUtil.log(LogUtil.LogType.NFC_SESSION, "ERROR, bad YubiKey slot");
                    // Cancel scanning
                    NfcSession.getInstance().disableNfcForegroundDispatch(m_act);
            }
            scanningYubikeySlot = 0;
        }

        @Override
        public void yubiKeyError(String error) {

        }
    };

    Button.OnClickListener onClickLockCodeButton = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            // If no code is set, enable keypad to set it, otherwise the user wants to change it
            String lockCode = Cryp.getLockCode(getActivity());

            if( lockCode.isEmpty()) {
                Intent i = new Intent(getActivity(), LockActivity.class);
                i.putExtra(CConst.CHANGE_LOCK_CODE, lockCode);
                startActivityForResult(i, CConst.CHANGE_LOCK_CODE_ACTION);
            }else{

                mRootView.findViewById(R.id.confirmClearLockCodeButton).setVisibility(View.VISIBLE);
            }
        }
    };

    Button.OnClickListener onClickConfirmClearLockCodeButton = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            Cryp.put(CConst.LOCK_CODE, "");
            mRootView.findViewById(R.id.confirmClearLockCodeButton).setVisibility(View.GONE);
            manageLockCodeDisplay();
        }
    };

    CheckBox.OnClickListener onClickShowCodeCb = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            manageLockCodeDisplay();
        }
    };

    Button.OnClickListener onClickYubiKey1Button = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            String yubiKey1Serial = Cryp.get(CConst.YUBIKEY_SERIAL1);

            if( yubiKey1Serial.isEmpty()) {
                // Scan for key
                mRootView.findViewById(R.id.yubiKey1ScanningTv).setVisibility(View.VISIBLE);
                mRootView.findViewById(R.id.cancelScanYubiKey1Button).setVisibility(View.VISIBLE);
                // Disable key 2 scan button
                mRootView.findViewById(R.id.yubiKey2Button).setClickable(false);
                // Call scanning code
                NfcSession.getInstance().enableNfcForegroundDispatch(m_act, nfcCallbacks);
                scanningYubikeySlot = 1;
            }else{
                mRootView.findViewById(R.id.confirmClearYubiKey1Button).setVisibility(View.VISIBLE);
            }
        }
    };

    Button.OnClickListener onClickCancelYubiKey1ScanButton = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            mRootView.findViewById(R.id.yubiKey1ScanningTv).setVisibility(View.GONE);
            mRootView.findViewById(R.id.cancelScanYubiKey1Button).setVisibility(View.GONE);
            mRootView.findViewById(R.id.yubiKey2Button).setClickable(true);
            // Cancel scanning
            NfcSession.getInstance().disableNfcForegroundDispatch(m_act);
        }
    };

    Button.OnClickListener onClickConfirmClearYubiKey1Button = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            Cryp.put(CConst.YUBIKEY_SERIAL1, "");
            mRootView.findViewById(R.id.confirmClearYubiKey1Button).setVisibility(View.GONE);
            manageYubiKey1Display();
        }
    };

    Button.OnClickListener onClickYubiKey2Button = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            String yubiKey2Serial = Cryp.get(CConst.YUBIKEY_SERIAL2);

            if( yubiKey2Serial.isEmpty()) {
                // Scan for key
                mRootView.findViewById(R.id.yubiKey2ScanningTv).setVisibility(View.VISIBLE);
                mRootView.findViewById(R.id.cancelScanYubiKey2Button).setVisibility(View.VISIBLE);
                // Disable key 1 scan button
                mRootView.findViewById(R.id.yubiKey1Button).setClickable(false);
                // Call scanning code
                NfcSession.getInstance().enableNfcForegroundDispatch(m_act, nfcCallbacks);
                scanningYubikeySlot = 2;
            }else{
                mRootView.findViewById(R.id.confirmClearYubiKey2Button).setVisibility(View.VISIBLE);
            }
        }
    };

    Button.OnClickListener onClickCancelYubiKey2ScanButton = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            mRootView.findViewById(R.id.yubiKey2ScanningTv).setVisibility(View.GONE);
            mRootView.findViewById(R.id.cancelScanYubiKey2Button).setVisibility(View.GONE);
            mRootView.findViewById(R.id.yubiKey1Button).setClickable(true);
            // Cancel scanning
            NfcSession.getInstance().disableNfcForegroundDispatch(m_act);
        }
    };

    Button.OnClickListener onClickConfirmClearYubiKey2Button = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            Cryp.put(CConst.YUBIKEY_SERIAL2, "");
            mRootView.findViewById(R.id.confirmClearYubiKey2Button).setVisibility(View.GONE);
            manageYubiKey2Display();
        }
    };

    Button.OnClickListener cancelButtonOnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            m_fragment.dismiss();
            NfcSession.getInstance().disableNfcForegroundDispatch(m_act);
        }
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case CConst.CHANGE_LOCK_CODE_ACTION: {

                if (resultCode == Activity.RESULT_OK) {

                    Bundle activityResultBundle = data.getExtras();
                    String lockCode = activityResultBundle.getString(CConst.CHANGE_LOCK_CODE);
                    Cryp.setLockCode(m_act, lockCode);
                    if (lockCode.isEmpty())
                        Toast.makeText(m_act, "Lock code cleared", Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(m_act, "Lock code changed", Toast.LENGTH_SHORT).show();
                    SettingsFragment.startSettingsFragment(m_act, R.id.settings_fragmment_container);

                    manageLockCodeDisplay();

                } else
                    Toast.makeText(m_act, "Lock code unchanged", Toast.LENGTH_SHORT).show();
                break;
            }
            default:
                if (DEBUG) LogUtil.log(LogUtil.LogType.SETTINGS, "onActivityResult: default action");
        }
    }
}
