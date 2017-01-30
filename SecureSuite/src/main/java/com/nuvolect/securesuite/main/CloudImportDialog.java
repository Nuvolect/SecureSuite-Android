package com.nuvolect.securesuite.main;
//
//TODO create class description
//

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;

import com.nuvolect.securesuite.data.ImportUtil;
import com.nuvolect.securesuite.util.LogUtil;
import com.nuvolect.securesuite.util.Persist;
import com.nuvolect.securesuite.util.WorkerCommand;

public class CloudImportDialog {

    private static CharSequence[] m_importAccountList;
    private static int[] m_importCountList;
    private static boolean[] m_importSelectList;
    private static String m_mainAccountImported;
    private static AlertDialog m_importDialog;

    /** progress dialog to show user that the import is processing. */
    private static ProgressDialog m_importProgressDialog = null;


    public static void openDialog(final Activity act) {

        String title = "Select accounts to import";

        AlertDialog.Builder builder = new AlertDialog.Builder( act );
        builder.setTitle(title);
        builder.setIcon(CConst.SMALL_ICON);

        Bundle bundle = ImportUtil.generateCloudSummary( act);

        m_importAccountList = bundle.getCharSequenceArray("accountList");
        m_importCountList = bundle.getIntArray("countList");
        m_mainAccountImported = "";

        m_importSelectList = new boolean[ m_importAccountList.length ];

        for(int i = 0; i< m_importAccountList.length; i++)
            m_importSelectList[ i ] = false;

        builder.setMultiChoiceItems(m_importAccountList, m_importSelectList,
                new DialogInterface.OnMultiChoiceClickListener(){

                    @Override
                    public void onClick(DialogInterface arg0, int which, boolean arg2) {

                    }});

        builder.setPositiveButton("Start import", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {

                LogUtil.log("Start cloud import... ");
                int totalImport = 0;
                int mainAccountCount = 0;

                for(int i = 0; i< m_importAccountList.length; i++){

                    // Remove trailing ", ### contacts"
                    String[] parts = m_importAccountList[ i ].toString().split("\\n"); // String array, each element is text between dots
                    m_importAccountList[ i ] = parts[ 0 ];

                    // Get total contacts to import and save to Persist
                    // # > 0 will indicate import is in progress
                    if( m_importSelectList[i]) {
                        totalImport += m_importCountList[i];

                        /**
                         * Save the main account, as determined by the account with the most
                         * contacts imported. This will be the account displayed to the user
                         * when import is complete.
                         */
                        if( m_importCountList[i] > mainAccountCount){

                            mainAccountCount = m_importCountList[i];
                            m_mainAccountImported = m_importAccountList[i].toString();
                        }
                    }
                }
                act.setProgressBarIndeterminateVisibility( true );
                Persist.setProgressBarActive( act, true );
                Persist.setImportInProgress( act, totalImport );
                cloudImportProgressDialog( act );

                WorkerCommand.importCloudContacts(act, m_importAccountList, m_importSelectList);
            }});

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {
                act.setProgressBarIndeterminateVisibility( false );
                Persist.setProgressBarActive( act, false );
                m_importDialog.dismiss();
            }});

        m_importDialog = builder.create();
        m_importDialog.show();

    }
    public static ProgressDialog m_cloudImportProgressDialog;
    public static boolean m_userCanceledCloudImport;
    public static int m_import_count;

    /**
     * Present a progress dialog with cloud import progress. This method may be called multiple times
     * depending on lifecycle updates and it will either create or restore the dialog.
     */
    public static void cloudImportProgressDialog(final Activity act){//import_cloud

        String message =
                "Encrypting contact data. You may migrate away from this app and encryption will continue in the background.";
        act.setProgressBarIndeterminateVisibility( true );
        Persist.setProgressBarActive( act, true );

        m_import_count = Persist.getImportInProgress( act );

        m_cloudImportProgressDialog = new ProgressDialog( act);
        m_cloudImportProgressDialog.setProgressStyle( ProgressDialog.STYLE_HORIZONTAL);
        m_cloudImportProgressDialog.setTitle("Please wait...");
        m_cloudImportProgressDialog.setIcon(CConst.SMALL_ICON);
        m_cloudImportProgressDialog.setMessage(message);
        m_cloudImportProgressDialog.setIndeterminate(false);
        m_cloudImportProgressDialog.setCancelable(false);
        m_cloudImportProgressDialog.setCanceledOnTouchOutside(false);
        m_cloudImportProgressDialog.setMax( m_import_count + 2);// 2 extra for cleanup progress
        m_cloudImportProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
                "Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                /* User clicked Cancel so do some stuff */
                        act.setProgressBarIndeterminateVisibility( false );
                        Persist.setProgressBarActive( act, false );
                        Persist.setImportInProgress(act, 0);

                        WorkerCommand.interruptProcessingAndStop(act);

                        m_userCanceledCloudImport = true;
                        m_cloudImportProgressDialog.dismiss();
                        m_cloudImportProgressDialog = null;
                    }
                });
        m_cloudImportProgressDialog.setProgress( 0);
        m_cloudImportProgressDialog.show();

        m_userCanceledCloudImport = false;
    }

    public static void dismissProgressDialog(Activity act) {

        if( m_cloudImportProgressDialog != null && m_cloudImportProgressDialog.isShowing()){

            m_cloudImportProgressDialog.dismiss();
        }

        if( m_importProgressDialog != null && m_importProgressDialog.isShowing()){

            m_importProgressDialog.dismiss();
        }
        m_importProgressDialog = null;
    }

    public static void complete(Activity act) {

        Persist.setImportInProgress(act, 0);
        Persist.setProgressBarActive(act, false );
        act.setProgressBarIndeterminateVisibility( false );

        if( m_cloudImportProgressDialog != null && m_cloudImportProgressDialog.isShowing()){

            m_cloudImportProgressDialog.dismiss();
            m_cloudImportProgressDialog = null;
        }
        if( act != null)
            act.recreate();
    }

    public static void updateProgress(Activity act, Bundle bundle) {

        int progress = bundle.getInt(CConst.IMPORT_PROGRESS);

        if( m_cloudImportProgressDialog != null &&
                m_cloudImportProgressDialog.isShowing())
            m_cloudImportProgressDialog.setProgress(progress);
    }

    /**
     * Return the account that had the most contacts imported.
     * @return
     */
    public static String getMainAccountImported() {

        return m_mainAccountImported;
    }
}
