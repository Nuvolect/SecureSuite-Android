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
import android.content.Context;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.nuvolect.securesuite.R;
import com.nuvolect.securesuite.nfc.NfcSession;
import com.nuvolect.securesuite.util.Cryp;
import com.nuvolect.securesuite.util.LogUtil;

/**
 * <pre>
 * Present a security lock to the user and either test user entry against an
 * existing passcode or enter and confirm a new passcode.
 *
 * RESPONSE_OK is returned when the user enters the proper passcode
 * or sets a valid passcode.  RESULT_CANCEL is returned when
 * the user hits the back button or too many incorrect attempts are made.
 *
 * intentExtra:
 *     CConst.VALIDATE_LOCK_CODE - validate code with user
 *          RESULT_OK/RESULT_CANCEL for pass/fail
 *          No returned value
 *
 *     CConst.CHANGE_LOCK_CODE - change the code
 *          If the code passed is not empty,
 *              validate the existing code first, before learning a new code.
 *          RESULT_OK - changed code is returned
 *          RESULT_CANCEL - empty string is returned
 * </pre>
 */
public class LockActivity extends Activity {

    enum STATE {NIL, LEARN_CODE1, LEARN_CODE2, CHANGE_CODE, CLEAR_CODE, VALIDATE, FINISH};
    enum MESSAGE_CMD {NIL, FINISH};

    STATE mCurrentState = STATE.NIL;

    Activity m_act;
    public static boolean lockDisabled = false;// Global flag
    private static final int MIN_LENGTH = 4;
    private static final long DISPLAY_FINISH_DELAY = 750; // Delay to finish in milliseconds
    private String mLockcode1;
    private String mLockcode2;
    private String mLockInputMode;
    private String mConfirmCode;
    private int mAttempts;
    private Vibrator myVib;
    private Handler mHandler = new Handler();
    private boolean mValidateYubiKey = false;

    private String mLine1;

    private String mLine2;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        m_act = this;
        mLockInputMode = Cryp.get(CConst.LOCK_INPUT_MODE, CConst.LOCK_INPUT_MODE_NUMERIC);
        mValidateYubiKey = false;

        if( mLockInputMode.contentEquals(CConst.LOCK_INPUT_MODE_NUMERIC))
            setContentView(R.layout.lock_screen);
        else
            setContentView(R.layout.lock_screen_kb);

        myVib = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);
        Intent intent = getIntent();

        mLockcode1 = "";
        mLockcode2 = "";
        mAttempts = 0;


        if( intent.hasExtra(CConst.VALIDATE_LOCK_CODE)){

            if( Cryp.get(CConst.YUBIKEY_SERIAL1).isEmpty()  && Cryp.get(CConst.YUBIKEY_SERIAL2).isEmpty())
                mValidateYubiKey = false;
            else
                mValidateYubiKey = true;

            mConfirmCode = intent.getStringExtra(CConst.VALIDATE_LOCK_CODE);
            if( !mValidateYubiKey && mConfirmCode.isEmpty()){

                message("No lock code", "Nothing to validate", MESSAGE_CMD.FINISH);
            }else{
                mCurrentState = STATE.VALIDATE;
                message("System Secure", "", MESSAGE_CMD.NIL);
            }
        }
        else
        if( intent.hasExtra(CConst.CHANGE_LOCK_CODE)){

            mConfirmCode = intent.getStringExtra(CConst.CHANGE_LOCK_CODE);
            if( mConfirmCode.isEmpty()){

                mCurrentState = STATE.LEARN_CODE1;
                if( mLockInputMode.contentEquals(CConst.LOCK_INPUT_MODE_NUMERIC))
                    message("Enter new code", "End with #", MESSAGE_CMD.NIL);
                else
                    message("Enter new code", "", MESSAGE_CMD.NIL);
            }else{

                mCurrentState = STATE.CHANGE_CODE;
                message("Enter current code", "", MESSAGE_CMD.NIL);
            }
        }

        else
        if( NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())){

            LogUtil.log(LogUtil.LogType.LOCK_ACTIVITY, "ACTION_NDEF_DISCOVERED");

        }else
            throw new RuntimeException("Expecting extra "+CConst.VALIDATE_LOCK_CODE+" or "+CConst.CHANGE_LOCK_CODE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if( mValidateYubiKey )
            NfcSession.getInstance().enableNfcForegroundDispatch(m_act, nfcCallbacks);
    }

    @Override
    public void onPause() {
        super.onPause();
        mHandler.removeCallbacks(displayFinishTimer);

        if( mValidateYubiKey )
            NfcSession.getInstance().disableNfcForegroundDispatch(m_act);
    }

    NfcSession.NfcCallbacks nfcCallbacks = new NfcSession.NfcCallbacks() {
        @Override
        public void yubiKeySerial(String serial) {

            LogUtil.log(LogUtil.LogType.LOCK_ACTIVITY, "nfcCallback: "+serial);

            if( serial.contentEquals( Cryp.get(CConst.YUBIKEY_SERIAL1))
                    || serial.contentEquals( Cryp.get(CConst.YUBIKEY_SERIAL2))){

                // Key match

                setResult(RESULT_OK);
                lockDisabled = true;
                mCurrentState = STATE.FINISH;
                message("Lock code confirmed", "", MESSAGE_CMD.FINISH);
            }else{

                // Key presented but it does not match
                message("Invalid NFC key", "", MESSAGE_CMD.NIL);
            }
        }

        @Override
        public void yubiKeyError(String error) {

            LogUtil.log(LogUtil.LogType.LOCK_ACTIVITY, "nfcCallback ERROR: "+error);

            message("NFC key error", "", MESSAGE_CMD.NIL);
        }
    };

    /**
     * Receive key presses from the user and operate a finite state machine.
     * Depending on conditions, the activity may end when the pass code is
     * given.  If the user hits the back button the application will exit.
     *
     * @param c
     */
    private void keypadInput(char c) {

        myVib.vibrate(10);

        String msg = "";

        switch(mCurrentState){

            case VALIDATE:{

                if( c == '#'){

                    ++mAttempts;
                    if( mAttempts == 1)
                        msg = "First attempt";
                    else
                        msg = mAttempts + " attempts";

                    //FUTURE delay the user when too many attempts

                    if( mAttempts > 4){

                        setResult(RESULT_CANCELED);
                        mCurrentState = STATE.FINISH;
                        message("Too many attempts", "Come back later", MESSAGE_CMD.FINISH);
                    }
                    mLockcode1 = "";
                    message("Enter lock code", msg, MESSAGE_CMD.NIL);
                }
                else{
                    mLockcode1 += c;

                    if( mLockcode1.contentEquals(mConfirmCode)){

                        setResult(RESULT_OK);
                        lockDisabled = true;
                        mCurrentState = STATE.FINISH;
                        message("Lock code confirmed", "", MESSAGE_CMD.FINISH);
                    }
                    addMessageStars( mLockcode1.length());
                }
                break;
            }
            case LEARN_CODE1:{

                if( c == '#' && mLockcode1.length() == 0){

                    message("Enter # one more time to clear code", "", MESSAGE_CMD.NIL);
                    mCurrentState = STATE.CLEAR_CODE;
                    break;
                }
                if( c == '#' && mLockcode1.length() >= MIN_LENGTH){

                    mCurrentState = STATE.LEARN_CODE2;
                    mLockcode2 = "";
                    message("New code entered", "Enter code again", MESSAGE_CMD.NIL);
                }else{

                    if( c == '#' && mLockcode1.length() < MIN_LENGTH){

                        mLockcode1 = "";
                        message("Code too short", "Try again", MESSAGE_CMD.NIL);
                    }else{
                        // Not the end, save to end of passphrase
                        mLockcode1 += c;
                        addMessageStars( mLockcode1.length());
                    }
                }
                break;
            }
            case LEARN_CODE2:{

                if( c == '#'){

                    mLockcode1 = "";
                    mLockcode2 = "";
                    message("No match", "Try again", MESSAGE_CMD.NIL);
                    mCurrentState = STATE.LEARN_CODE1;
                }else{

                    // Not the end, save to end of passphrase
                    mLockcode2 += c;

                    if( mLockcode2.contentEquals(mLockcode1)){

                        Intent returnCode = new Intent();
                        returnCode.putExtra(CConst.CHANGE_LOCK_CODE, mLockcode1);
                        setResult(RESULT_OK, returnCode);
                        mCurrentState = STATE.FINISH;
                        message("Lock code confirmed", "", MESSAGE_CMD.FINISH);
                    }
                    addMessageStars( mLockcode2.length());
                }
                break;
            }
            case CHANGE_CODE:{ // user must validate code first before it can be changed

                if( c == '#'){

                    mLockcode1 = "";
                    ++mAttempts;

                    if( mAttempts == 1 )
                        msg = "Validation error";
                    else
                        msg = mAttempts + " attempts";

                    if( mAttempts > 4){

                        mCurrentState = STATE.FINISH;
                        setResult(RESULT_CANCELED);
                        message( "Too many tries", "Come back later", MESSAGE_CMD.FINISH);
                    }
                    else
                        message( msg, "Try again", MESSAGE_CMD.NIL);

                }else{

                    // Not the end, save to end of passphrase
                    mLockcode1 += c;

                    if( mLockcode1.contentEquals(mConfirmCode)){

                        mLockcode1 = "";
                        mCurrentState = STATE.LEARN_CODE1;
                        message("Lock code confirmed", "Enter new code", MESSAGE_CMD.NIL);
                    }
                    addMessageStars( mLockcode1.length());
                }
                break;
            }
            case CLEAR_CODE:{
                if( c == '#'){

                    mCurrentState = STATE.FINISH;
                    Intent returnCode = new Intent();
                    returnCode.putExtra(CConst.CHANGE_LOCK_CODE, "");
                    setResult(RESULT_OK, returnCode);
                    message( "Lock code cleared", "", MESSAGE_CMD.FINISH);
                }else{

                    mCurrentState = STATE.LEARN_CODE1;
                    message( "Starting over", "Enter new lock code", MESSAGE_CMD.NIL);
                }
                break;
            }
            case FINISH:// ignore key press when finished
            case NIL:
            default:
        }
    }
    /**
     * Receive lock code from the user and operate a finite state machine.
     * Depending on conditions, the activity may end when the pass code is
     * given.  If the user hits the back button the application will exit.
     *
     * @param lockCode
     */
    private void keyboardInput(String lockCode) {

        myVib.vibrate(10);

        String msg = "";

        switch(mCurrentState){

            case VALIDATE:{

                ++mAttempts;
                if( mAttempts == 1)
                    msg = "First attempt";
                else
                    msg = mAttempts + " attempts";

                //FUTURE delay the user when too many attempts

                if( mAttempts > 4){

                    setResult(RESULT_CANCELED);
                    mCurrentState = STATE.FINISH;
                    message("Too many attempts", "Come back later", MESSAGE_CMD.FINISH);
                }
                mLockcode1 = "";
                message("Enter lock code", msg, MESSAGE_CMD.NIL);

                if( lockCode.contentEquals(mConfirmCode)){

                    setResult(RESULT_OK);
                    lockDisabled = true;
                    mCurrentState = STATE.FINISH;
                    message("Lock code confirmed", "", MESSAGE_CMD.FINISH);
                }
                break;
            }
            case LEARN_CODE1:{

                if( lockCode.isEmpty()){

                    message("Enter empty code more time to clear code", "", MESSAGE_CMD.NIL);
                    mCurrentState = STATE.CLEAR_CODE;
                    break;
                }
                if( lockCode.length() >= MIN_LENGTH){

                    mCurrentState = STATE.LEARN_CODE2;
                    mLockcode1 = lockCode;
                    ((EditText) findViewById(R.id.passwordEt)).setText("");
                    mLockcode2 = "";
                    message("New code entered", "Enter code again", MESSAGE_CMD.NIL);
                }else{

                    if( lockCode.length() < MIN_LENGTH){

                        mLockcode1 = "";
                        message("Code too short", "Try again", MESSAGE_CMD.NIL);
                    }
                }
                break;
            }
            case LEARN_CODE2:{

                if( ! lockCode.contentEquals(mLockcode1)){

                    mLockcode1 = "";
                    mLockcode2 = "";
                    message("No match", "Try again", MESSAGE_CMD.NIL);
                    mCurrentState = STATE.LEARN_CODE1;
                }else{

                    Intent returnCode = new Intent();
                    returnCode.putExtra(CConst.CHANGE_LOCK_CODE, mLockcode1);
                    setResult(RESULT_OK, returnCode);
                    mCurrentState = STATE.FINISH;
                    message("Lock code confirmed", "", MESSAGE_CMD.FINISH);
                }
                break;
            }
            case CHANGE_CODE:{ // user must validate code first before it can be changed

                mLockcode1 = "";
                ++mAttempts;


                if( mLockcode1.contentEquals(mConfirmCode)){

                    mLockcode1 = "";
                    mCurrentState = STATE.LEARN_CODE1;
                    message("Lock code confirmed", "Enter new code", MESSAGE_CMD.NIL);
                    break;
                }
                else
                if( mAttempts > 4){

                    mCurrentState = STATE.FINISH;
                    setResult(RESULT_CANCELED);
                    message( "Too many tries", "Come back later", MESSAGE_CMD.FINISH);
                }
                else
                    message( msg, "Try again", MESSAGE_CMD.NIL);

                break;
            }
            case CLEAR_CODE:{

                mCurrentState = STATE.FINISH;
                Intent returnCode = new Intent();
                returnCode.putExtra(CConst.CHANGE_LOCK_CODE, "");
                setResult(RESULT_OK, returnCode);
                message( "Lock code cleared", "", MESSAGE_CMD.FINISH);

                break;
            }
            case FINISH:// ignore key press when finished
            case NIL:
            default:
        }
    }

    /**
     * Set the display message for short amount of time. A timer is set and
     * when it expires the message is restored to "System Secure".
     * @param line1
     * @param line2
     * @param commandWhenDone
     */
    private void message(String line1, String line2, MESSAGE_CMD commandWhenDone) {

        mLine1 = line1;
        mLine2 = line2;

        String msg = line1 + "\n" + line2 + "\n";// extra newline to old *** as keys are pressed

        TextView lockBanner = (TextView) findViewById(R.id.lockBanner);
        lockBanner.setText(msg);

        if( commandWhenDone == MESSAGE_CMD.FINISH)
            mHandler.postDelayed(displayFinishTimer, DISPLAY_FINISH_DELAY);
    }

    private void addMessageStars( int codeLength){

        String msg = mLine1 + "\n" + mLine2 + "\n";

        for( int i=0; i<codeLength; i++)
            msg += "*";

        TextView lockBanner = (TextView) findViewById(R.id.lockBanner);
        lockBanner.setText(msg);
    }

    public void onClickButton0( View v ){ keypadInput('0'); }
    public void onClickButton1( View v ){ keypadInput('1'); }
    public void onClickButton2( View v ){ keypadInput('2'); }
    public void onClickButton3( View v ){ keypadInput('3'); }
    public void onClickButton4( View v ){ keypadInput('4'); }
    public void onClickButton5( View v ){ keypadInput('5'); }
    public void onClickButton6( View v ){ keypadInput('6'); }
    public void onClickButton7( View v ){ keypadInput('7'); }
    public void onClickButton8( View v ){ keypadInput('8'); }
    public void onClickButton9( View v ){ keypadInput('9'); }
    public void onClickButtonStar( View v ){ keypadInput('*'); }
    public void onClickButtonPound( View v ){ keypadInput('#'); }

    public void onClickModeToggle(View view) {

        if( mLockInputMode.contentEquals(CConst.LOCK_INPUT_MODE_NUMERIC))
            Cryp.put(CConst.LOCK_INPUT_MODE, CConst.LOCK_INPUT_MODE_KEYBOARD);
        else
            Cryp.put(CConst.LOCK_INPUT_MODE, CConst.LOCK_INPUT_MODE_NUMERIC);

        m_act.recreate();
    }

    private Runnable displayFinishTimer = new Runnable() {
        public void run() {

            finish();

            mHandler.removeCallbacks(displayFinishTimer);
        }
    };

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        setResult(RESULT_CANCELED);
        finish();
    }

    public static boolean lockCodePresent(Context ctx){

        if( ! Cryp.getLockCode(ctx).isEmpty() ||
            ! Cryp.get(CConst.YUBIKEY_SERIAL1).isEmpty() ||
            ! Cryp.get(CConst.YUBIKEY_SERIAL2).isEmpty()
        )
            return true;
        else
            return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case android.R.id.home:
                m_act.finish();
                return true;
            default:
        }
        return super.onOptionsItemSelected(item);
    }

    public void onClickPassword(View view) {

        EditText et = (EditText) view;
        String lockCode = et.getText().toString();

        keyboardInput(lockCode);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        LogUtil.log(LogUtil.LogType.LOCK_ACTIVITY, "onNewIntent");

        NfcSession.getInstance().handleIntentInBackground(intent);
    }
}