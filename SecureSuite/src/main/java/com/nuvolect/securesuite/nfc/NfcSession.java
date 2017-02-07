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

package com.nuvolect.securesuite.nfc;//

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.IsoDep;
import android.nfc.tech.Ndef;
import android.os.AsyncTask;
import android.widget.Toast;

import com.nuvolect.securesuite.R;
import com.nuvolect.securesuite.util.LogUtil;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Manage an Near Field Communication YubiKey Session.
 * The YubiKey serial number is returned as a decimal string.
 * The serial number is extracted using two methods, with the idea
 * that it is nearly impossible to spoof the combination of both methods:
 * 1) using the NfcAdapter
 * 2) from the intent data OTP URL string
 */
public class NfcSession {

    private static final int TIMEOUT = 100000;
    private static NfcSession instance;
    private static Activity m_act;
    protected boolean mPw1ValidForMultipleSignatures;
    protected boolean mPw1ValidatedForSignature;
    protected boolean mPw1ValidatedForDecrypt; // Mode 82 does other things; consider renaming?
    protected boolean mPw3Validated;
    private IsoDep mIsoDep;
    private NfcAdapter mNfcAdapter;
    private byte[] mNfcFingerprints;
    private String mNfcUserId;
    private byte[] mNfcAid;
    private NfcCallbacks mListener;
    private String mYubiKeySerialFromTag;
    private String mYubiKeySerialFromUri;

    final String[] URI_PREFIX = new String[] {
    /* 0x00 */ "",
    /* 0x01 */ "http://www.",
    /* 0x02 */ "https://www.",
    /* 0x03 */ "http://",
    /* 0x04 */ "https://",
    /* 0x05 */ "tel:",
    /* 0x06 */ "mailto:",
    /* 0x07 */ "ftp://anonymous:anonymous@",
    /* 0x08 */ "ftp://ftp.",
    /* 0x09 */ "ftps://",
    /* 0x0A */ "sftp://",
    /* 0x0B */ "smb://",
    /* 0x0C */ "nfs://",
    /* 0x0D */ "ftp://",
    /* 0x0E */ "dav://",
    /* 0x0F */ "news:",
    /* 0x10 */ "telnet://",
    /* 0x11 */ "imap:",
    /* 0x12 */ "rtsp://",
    /* 0x13 */ "urn:",
    /* 0x14 */ "pop:",
    /* 0x15 */ "sip:",
    /* 0x16 */ "sips:",
    /* 0x17 */ "tftp:",
    /* 0x18 */ "btspp://",
    /* 0x19 */ "btl2cap://",
    /* 0x1A */ "btgoep://",
    /* 0x1B */ "tcpobex://",
    /* 0x1C */ "irdaobex://",
    /* 0x1D */ "file://",
    /* 0x1E */ "urn:epc:id:",
    /* 0x1F */ "urn:epc:tag:",
    /* 0x20 */ "urn:epc:pat:",
    /* 0x21 */ "urn:epc:raw:",
    /* 0x22 */ "urn:epc:",
    /* 0x23 */ "urn:nfc:"
    };

    /**
     * Callback interface to share NFC results
     */
    public interface NfcCallbacks {

        public void yubiKeySerial(String serial);
        public void yubiKeyError(String error);
    }

    public NfcSession(){

        mNfcAdapter = null;
    }

    /**
     * Manage a NFC YubiKey session.
     * You must Override onNewIntent to receive the YubuKey NFC intent:
     * Singletons are wonder things, because their the only one.
     * @return
     */
    public static synchronized NfcSession getInstance() {

        if(instance == null) {
            instance = new NfcSession();
        }
        return instance;
    }

    /**
     * Receive new NFC Intents to this activity only by enabling foreground dispatch.
     * This can only be done in onResume!
     */
    public void enableNfcForegroundDispatch(Activity act, NfcCallbacks nfcCallbacks) {

        m_act = act;
        mListener = nfcCallbacks;
        mNfcAdapter = NfcAdapter.getDefaultAdapter(m_act);
        if (mNfcAdapter == null) {
            return;
        }
        Intent nfcI = new Intent(m_act, m_act.getClass())
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent nfcPendingIntent = PendingIntent.getActivity(m_act, 0, nfcI, PendingIntent.FLAG_CANCEL_CURRENT);
        IntentFilter[] writeTagFilters = new IntentFilter[]{
                new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
        };

        // https://code.google.com/p/android/issues/detail?id=62918
        // maybe mNfcAdapter.enableReaderMode(); ?
        try {
            mNfcAdapter.enableForegroundDispatch(m_act, nfcPendingIntent, writeTagFilters, null);
        } catch (IllegalStateException e) {
            LogUtil.logException(LogUtil.LogType.NFC_SESSION, "NfcForegroundDispatch Error!", e);
        }
        LogUtil.log(LogUtil.LogType.NFC_SESSION, "NfcForegroundDispatch has been enabled!");
    }

    /**
     * Disable foreground dispatch in onPause!
     */
    public void disableNfcForegroundDispatch(Activity act) {

        m_act = act;

        if (mNfcAdapter == null) {
            return;
        }
        mNfcAdapter.disableForegroundDispatch(m_act);
        LogUtil.log(LogUtil.LogType.NFC_SESSION, "NfcForegroundDispatch has been disabled!");
        mNfcAdapter = null;
    }

    public void handleIntentInBackground(final Intent intent) {
        // Actual NFC operations are executed in doInBackground to not block the UI thread
        new AsyncTask<Void, Void, Exception>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                onNfcPreExecute();
            }

            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                    /**
                     * Get YubiKey serial from the tag using the nfc adapter
                     */
                    extractYubiKeySerialFromTag(tag);
                    /**
                     * Get YubiKey serial by decoding the URI returned from the intent
                     */
                    extractYubiKeySerialFromUri(tag);

                } catch (CardException e) {
                    return e;
                } catch (IOException e) {
                    return e;
                }

                return null;
            }

            @Override
            protected void onPostExecute(Exception exception) {
                super.onPostExecute(exception);

                if (exception != null) {
                    handleNfcError(exception);
                    return;
                }

                try {
                    onNfcPostExecute();
                } catch (IOException e) {
                    handleNfcError(e);
                }
            }
        }.execute();
    }

    private void handleNfcError(Exception e) {
        LogUtil.logException(LogUtil.LogType.NFC_SESSION, e);

        if (e instanceof TagLostException) {
            onNfcError(m_act.getString(R.string.error_nfc_tag_lost));
            return;
        }

        short status;
        if (e instanceof CardException) {
            status = ((CardException) e).getResponseCode();
        } else {
            status = -1;
        }
        // When entering a PIN, a status of 63CX indicates X attempts remaining.
        if ((status & (short)0xFFF0) == 0x63C0) {
            int tries = status & 0x000F;
            onNfcError(m_act.getResources().getQuantityString(R.plurals.error_pin, tries, tries));
            return;
        }

        // Otherwise, all status codes are fixed values.
        switch (status) {
            // These errors should not occur in everyday use; if they are returned, it means we
            // made a mistake sending data to the card, or the card is misbehaving.
            case 0x6A80: {
                onNfcError(m_act.getString(R.string.error_nfc_bad_data));
                break;
            }
            case 0x6883: {
                onNfcError(m_act.getString(R.string.error_nfc_chaining_error));
                break;
            }
            case 0x6B00: {
                onNfcError(m_act.getString(R.string.error_nfc_header, "P1/P2"));
                break;
            }
            case 0x6D00: {
                onNfcError(m_act.getString(R.string.error_nfc_header, "INS"));
                break;
            }
            case 0x6E00: {
                onNfcError(m_act.getString(R.string.error_nfc_header, "CLA"));
                break;
            }
            // These error conditions are more likely to be experienced by an end user.
            case 0x6285: {
                onNfcError(m_act.getString(R.string.error_nfc_terminated));
                break;
            }
            case 0x6700: {
                onNfcError(m_act.getString(R.string.error_nfc_wrong_length));
                break;
            }
            case 0x6982: {
                onNfcError(m_act.getString(R.string.error_nfc_security_not_satisfied));
                break;
            }
            case 0x6983: {
                onNfcError(m_act.getString(R.string.error_nfc_authentication_blocked));
                break;
            }
            case 0x6985: {
                onNfcError(m_act.getString(R.string.error_nfc_conditions_not_satisfied));
                break;
            }
            // 6A88 is "Not Found" in the spec, but Yubikey also returns 6A83 for this in some cases.
            case 0x6A88:
            case 0x6A83: {
                onNfcError(m_act.getString(R.string.error_nfc_data_not_found));
                break;
            }
            // 6F00 is a JavaCard proprietary status code, SW_UNKNOWN, and usually represents an
            // unhandled exception on the smart card.
            case 0x6F00: {
                onNfcError(m_act.getString(R.string.error_nfc_unknown));
                break;
            }
            default: {
                onNfcError(m_act.getString(R.string.error_nfc, e.getMessage()));
                break;
            }
        }

    }

    /**
     * Override to use something different than Notify (UI thread)
     */
    protected void onNfcError(String error) {
//        Notify.create(this, error, Style.WARN).show();

        Toast.makeText(m_act, error, Toast.LENGTH_LONG).show();
        LogUtil.log(LogUtil.LogType.NFC_SESSION, error);
    }

    /**
     * Override to change UI before NFC handling (UI thread)
     */
    protected void onNfcPreExecute() {

    }

    /** Handle NFC communication and return a result.
     *
     * This method is called by onNewIntent above upon discovery of an NFC tag.
     * It handles initialization and login to the application, subsequently
     * calls either nfcCalculateSignature() or nfcDecryptSessionKey(), then
     * finishes the activity with an appropriate result.
     *
     * On general communication, see also
     * http://www.cardwerk.com/smartcards/smartcard_standard_ISO7816-4_annex-a.aspx
     *
     * References to pages are generally related to the OpenPGP Application
     * on ISO SmartCard Systems specification.
     *
     */
    protected void extractYubiKeySerialFromTag(Tag detectedTag) throws IOException {

        mYubiKeySerialFromTag = "";
        // Connect to the detected tag, setting a couple of settings
        mIsoDep = IsoDep.get(detectedTag);
        mIsoDep.setTimeout(TIMEOUT); // timeout is set to 100 seconds to avoid cancellation during calculation
        mIsoDep.connect();

        // SW1/2 0x9000 is the generic "ok" response, which we expect most of the time.
        // See specification, page 51
        String accepted = "9000";

        // Command APDU (page 51) for SELECT FILE command (page 29)
        String opening = ""
                + "00" // CLA
                + "A4" // INS
                + "04" // P1
                + "00" // P2
                + "06" // Lc (number of bytes)
                + "D27600012401" // Data (6 bytes)
                + "00"; // Le
        String response = nfcCommunicate(opening);  // activate connection
        if ( ! response.endsWith(accepted) ) {
            throw new CardException("Initialization failed!", parseCardStatus(response));
        }

        byte[] pwStatusBytes = nfcGetPwStatusBytes();
        mPw1ValidForMultipleSignatures = (pwStatusBytes[0] == 1);
        mPw1ValidatedForSignature = false;
        mPw1ValidatedForDecrypt = false;
        mPw3Validated = false;

        doNfcInBackground();
    }

    /**
     * Override to implement NFC operations (background thread)
     */
    protected void doNfcInBackground() throws IOException {
        mNfcFingerprints = nfcGetFingerprints();
        mNfcUserId = nfcGetUserId();
        mNfcAid = nfcGetAid();

        String identity = Hex.toHexString(mNfcAid, 10, 4);
        long id_long = Long.parseLong( identity );
        if( id_long > 0)
            mYubiKeySerialFromTag = String.valueOf( id_long);
    }

    /**
     * Override to handle result of NFC operations (UI thread)
     */
    protected void onNfcPostExecute() throws IOException {

        final long subKeyId = KeyFormattingUtils.getKeyIdFromFingerprint(mNfcFingerprints);

        String data = ""
                +"\nmNfcFingerprints: "+Hex.toHexString(mNfcFingerprints)
                +"\nsubKeyId \"\"   : "+subKeyId
                +"\nmNfcAid:          "+Hex.toHexString(mNfcAid, 10, 4)
                +"\nmNfcUserId:       "+mNfcUserId
                ;
        LogUtil.log(LogUtil.LogType.NFC_SESSION, data);

        if( ! mYubiKeySerialFromTag.isEmpty() &&
            mYubiKeySerialFromTag.contentEquals(mYubiKeySerialFromUri)){

            mListener.yubiKeySerial(mYubiKeySerialFromTag);
        }else {
            if( mYubiKeySerialFromTag.isEmpty())
                mListener.yubiKeyError("Error reading key");
            else
                mListener.yubiKeyError("Error, invalid key");
        }
    }

    /** Return fingerprints of all keys from application specific data stored
     * on tag, or null if data not available.
     *
     * @return The fingerprints of all subkeys in a contiguous byte array.
     */
    public byte[] nfcGetFingerprints() throws IOException {
        String data = "00CA006E00";
        byte[] buf = mIsoDep.transceive(Hex.decode(data));

        Iso7816TLV tlv = Iso7816TLV.readSingle(buf, true);
        LogUtil.log(LogUtil.LogType.NFC_SESSION, "nfc tlv data:\n" + tlv.prettyPrint());

        Iso7816TLV fptlv = Iso7816TLV.findRecursive(tlv, 0xc5);
        if (fptlv == null) {
            return null;
        }

        return fptlv.mV;
    }

    public byte[] nfcGetAid() throws IOException {

        String info = "00CA004F00";
        return mIsoDep.transceive(Hex.decode(info));

    }

    public String nfcGetUserId() throws IOException {

        String info = "00CA006500";
        return nfcGetHolderName(nfcCommunicate(info));
    }

    /** Return the PW Status Bytes from the card. This is a simple DO; no TLV decoding needed.
     *
     * @return Seven bytes in fixed format, plus 0x9000 status word at the end.
     */
    public byte[] nfcGetPwStatusBytes() throws IOException {
        String data = "00CA00C400";
        return mIsoDep.transceive(Hex.decode(data));
    }

    /**
     * Parses out the status word from a JavaCard response string.
     *
     * @param response A hex string with the response from the card
     * @return A short indicating the SW1/SW2, or 0 if a status could not be determined.
     */
    short parseCardStatus(String response) {
        if (response.length() < 4) {
            return 0; // invalid input
        }

        try {
            return Short.parseShort(response.substring(response.length() - 4), 16);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public String nfcGetHolderName(String name) {
        String slength;
        int ilength;
        name = name.substring(6);
        slength = name.substring(0, 2);
        ilength = Integer.parseInt(slength, 16) * 2;
        name = name.substring(2, ilength + 2);
        name = (new String(Hex.decode(name))).replace('<', ' ');
        return (name);
    }

    public String nfcCommunicate(String apdu) throws IOException {
        return getHex(mIsoDep.transceive(Hex.decode(apdu)));
    }

    public static String getHex(byte[] raw) {
        return new String(Hex.encode(raw));
    }

    private String extractYubiKeySerialFromUri(Tag tag){

        long id_long = 0;
        mYubiKeySerialFromUri = "";

        Ndef ndef = Ndef.get(tag);
        if (ndef == null) {
            // NDEF is not supported by this Tag.
            return "";
        }

        NdefMessage ndefMessage = ndef.getCachedNdefMessage();

        NdefRecord[] records = ndefMessage.getRecords();

        for (NdefRecord ndefRecord : records) {

            LogUtil.log(LogUtil.LogType.NFC_SESSION, "getTnf(): " + ndefRecord.getTnf());

            if (ndefRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN) {

                //  is empty                   byte[] id = ndefRecord.getId();
                // https://my.yubico.com/neo/cccccceiijtkgtukkijdctjtgghggghbkfkenerllngk
                //                                       niljijfcnfdbjeduvuthuugnvuuvgrnh

                byte[] payload = ndefRecord.getPayload();

                int prefixCode = payload[0] & 0x0FF;
                if (prefixCode >= URI_PREFIX.length) prefixCode = 0;

                String reducedUri = new String(payload, 1, payload.length - 1, Charset.forName("UTF-8"));
                String key = new String(payload, payload.length - 32, 32, Charset.forName("UTF-8"));
                String identity = new String(payload, payload.length - 44, 12, Charset.forName("UTF-8"));
                id_long = Long.parseLong(Hex.modHextoHex(identity), 16);
                if( id_long > 0)
                    mYubiKeySerialFromUri = String.valueOf( id_long);

                String uri = URI_PREFIX[prefixCode] + reducedUri;

                LogUtil.log(LogUtil.LogType.NFC_SESSION, "uri: " + uri);
                LogUtil.log(LogUtil.LogType.NFC_SESSION, "key: " + key);
                LogUtil.log(LogUtil.LogType.NFC_SESSION, "identity: " + identity + ", " + mYubiKeySerialFromUri);
            }
        }
        return String.valueOf(id_long);
    }

    public class CardException extends IOException {
        private short mResponseCode;

        public CardException(String detailMessage, short responseCode) {
            super(detailMessage);
            mResponseCode = responseCode;
        }

        public short getResponseCode() {
            return mResponseCode;
        }

    }
}
