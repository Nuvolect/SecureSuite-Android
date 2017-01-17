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

    private static CharSequence[] importAccountList;
    private static int[] importCountList;
    private static boolean[] importSelectList;
    private static AlertDialog importDialog;

    /** progress dialog to show user that the import is processing. */
    private static ProgressDialog m_importProgressDialog = null;


    public static void openDialog(final Activity act) {

        String title = "Select accounts to import";

        AlertDialog.Builder builder = new AlertDialog.Builder( act );
        builder.setTitle(title);
        builder.setIcon(CConst.SMALL_ICON);

        Bundle bundle = ImportUtil.generateCloudSummary( act);

        importAccountList = bundle.getCharSequenceArray("accountList");
        importCountList = bundle.getIntArray("countList");

        importSelectList = new boolean[ importAccountList.length ];

        for( int i = 0; i< importAccountList.length; i++)
            importSelectList[ i ] = false;

        builder.setMultiChoiceItems( importAccountList, importSelectList,
                new DialogInterface.OnMultiChoiceClickListener(){

                    @Override
                    public void onClick(DialogInterface arg0, int which, boolean arg2) {

                    }});

        builder.setPositiveButton("Start import", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {

                LogUtil.log("Start cloud import... ");
                int totalImport = 0;

                for( int i=0; i< importAccountList.length; i++){

                    // Remove trailing ", ### contacts"
                    String[] parts = importAccountList[ i ].toString().split("\\n"); // String array, each element is text between dots
                    importAccountList[ i ] = parts[ 0 ];

                    // Get total contacts to import and save to Persist
                    // # > 0 will indicate import is in progress
                    if( importSelectList[i])
                        totalImport += importCountList[i];
                }
                act.setProgressBarIndeterminateVisibility( true );
                Persist.setProgressBarActive( act, true );
                Persist.setImportInProgress( act, totalImport );
                cloudImportProgressDialog( act );

                WorkerCommand.importCloudContacts(act, importAccountList, importSelectList);
            }});

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {
                act.setProgressBarIndeterminateVisibility( false );
                Persist.setProgressBarActive( act, false );
                importDialog.dismiss();
            }});

        importDialog = builder.create();
        importDialog.show();

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

    public static String getFirstImportedAccount() {

        if( importAccountList!= null && importAccountList.length>0)
            return importAccountList[0].toString();
        else
            return "";
    }
}
