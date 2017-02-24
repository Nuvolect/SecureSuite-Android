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

package com.nuvolect.securesuite.data;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

import com.nuvolect.securesuite.license.LicensePersist;
import com.nuvolect.securesuite.util.LogUtil;
import com.nuvolect.securesuite.util.Util;

import net.sqlcipher.Cursor;
import net.sqlcipher.DatabaseUtils;
import net.sqlcipher.database.SQLiteDatabase;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

public class BackupRestore {

    static SQLiteDatabase account_db = SqlCipher.account_db;
    static SQLiteDatabase detail_db = SqlCipher.detail_db;
    static String ACCOUNT_TABLE = SqlCipher.ACCOUNT_TABLE;
    static String GROUP_TITLE_TABLE = SqlCipher.GROUP_TITLE_TABLE;
    static String ACCOUNT_DATA_TABLE = SqlCipher.ACCOUNT_DATA_TABLE;
    static String[] ACCOUNT_DATA_COLUMNS = SqlCipher.ACCOUNT_DATA_COLUMNS;
    static String[] ACCOUNT_DB_COLUMNS = SqlCipher.ACCOUNT_DB_COLUMNS;
    static String[] GROUP_TITLE_COLUMNS = SqlCipher.GROUP_TITLE_COLUMNS;
    static String DETAIL_TABLE = SqlCipher.DETAIL_TABLE;


    public static void dumpDbToFile(Context ctx){

        // Create necessary folder, return path
        String appFolderPath = Util.createAppPublicFolder();

        // Create a unique name
        String filename = Util.makeDateTimeFilename();

        // Create the output file with extension
        File outFile = new File( appFolderPath + filename + ".txt");
        FileWriter fw = null;

        try {
            fw = new FileWriter( outFile, true);

        Cursor c;

            c = account_db.query(ACCOUNT_TABLE, null, null, null, null, null, null);
            fw.append(ACCOUNT_TABLE);

            while( c.moveToNext()){

               String row = DatabaseUtils.dumpCurrentRowToString( c );
               fw.append( row );
            }
            c.close();

            c = account_db.query(ACCOUNT_DATA_TABLE, null, null, null, null, null, null);
            fw.append(ACCOUNT_DATA_TABLE);

            while( c.moveToNext()){

               String row = DatabaseUtils.dumpCurrentRowToString( c );
               fw.append( row );
            }
            c.close();

            c = detail_db.query(DETAIL_TABLE, null, null, null, null, null, null);
            fw.append(DETAIL_TABLE);

            while( c.moveToNext()){

               String row = DatabaseUtils.dumpCurrentRowToString( c );
               fw.append( row );
            }
            c.close();

            c = account_db.query(GROUP_TITLE_TABLE, null, null, null, null, null, null);
            fw.append(GROUP_TITLE_TABLE);

            while( c.moveToNext()){

               String row = DatabaseUtils.dumpCurrentRowToString( c );
               fw.append( row );
            }
            c.close();
            fw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Copy a database from the application into public storage
     * @param basePath - Path to store file, without trailing slash
     * @param db
     * @throws IOException
     */
    public static void copyDbToStorage( String basePath, SQLiteDatabase db)
            throws IOException {

        //Open local db as the input stream
        String inPathWithName = db.getPath();
        File dbFile = new File(inPathWithName);
        FileInputStream fis = new FileInputStream(dbFile);

        String fileName = inPathWithName.substring( inPathWithName.lastIndexOf("/")+1);
        String outPathWithName = basePath+"/"+fileName;
        //Open the empty db as the output stream
        OutputStream outputStream = new FileOutputStream(outPathWithName);
        //transfer bytes from the inputfile to the outputfile
        byte[] buffer = new byte[1024];
        int length;
        while ((length = fis.read(buffer))>0){
            outputStream.write(buffer, 0, length);
        }
        //Close the streams
        outputStream.flush();
        outputStream.close();
        fis.close();
    }

    /**
     * Copy both databases to storage.
     * @param act
     */
    public static void backupToStorage(Activity act) {

        try {
            String basePath = Util.createTimeStampedBackupFolder( act);
            BackupRestore.copyDbToStorage( basePath, SqlCipher.account_db);
            BackupRestore.copyDbToStorage( basePath, SqlCipher.detail_db);
            Toast.makeText( act, "Backup complete", Toast.LENGTH_SHORT).show();

            // Remove non-essential internal path, just show from /sdcard...
            String internalPath = Environment.getExternalStorageDirectory().getPath();
            basePath = basePath.replaceFirst( internalPath, "");
            Toast.makeText( act, basePath, Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText( act, "Backup FAILED", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Rename the target database to a temporary name.  This way it can either be
     * restored or deleted
     * @param ctx
     * @param dbFileName
     * @return
     */
    public static File renameDbTemp( Context ctx, String dbFileName){

        File targetFile = ctx.getDatabasePath( dbFileName);
        File targetFileTemp = ctx.getDatabasePath( dbFileName+".temp");

        targetFile.renameTo(targetFileTemp);

        return targetFileTemp;
    }

    /**
     * Rename the temporary file back to the database filename.
     * @param ctx
     * @param dbFileTemp
     */
    public static void restoreDbTemp( Context ctx, File dbFileTemp){

        String dbFileNameTemp = dbFileTemp.getName();
        String dbFileName = dbFileNameTemp.replace(".temp", "");
        File targetFile = ctx.getDatabasePath( dbFileName);
        targetFile.delete();
        dbFileTemp.renameTo(targetFile);
    }
    /**
     * Delete the temporary database file.
     * @param ctx
     * @param dbFileTemp
     */
    public static void deleteDbTemp( Context ctx, File dbFileTemp){

        dbFileTemp.delete();
    }

    /**
     * Copy a database from the public storage to the application database folder
     * @throws IOException
     */
    public static void copyDbToApp(Context ctx, String sourceDbPath, String targetDbFilename)
            throws IOException {

        // Get the file and path to the database
        File targetFile = ctx.getDatabasePath(targetDbFilename);
        String outPathWithName = targetFile.getPath();

        //Open storage db as the input stream
        String inPathWithName = sourceDbPath +"/"+ targetDbFilename;
        File dbFile = new File(inPathWithName);
        FileInputStream fis = new FileInputStream(dbFile);

        //Open the target db as the output stream and delete it
        if( targetFile.delete())
            LogUtil.log("file deleted: "+ targetDbFilename);

        OutputStream outputStream = new FileOutputStream(outPathWithName);

        //transfer bytes from the inputfile to the outputfile
        byte[] buffer = new byte[1024];
        int length;
        while ((length = fis.read(buffer))>0){
            outputStream.write(buffer, 0, length);
        }
        //Close the streams
        outputStream.flush();
        outputStream.close();
        fis.close();
        LogUtil.log("File restored: "+ targetDbFilename);
    }

    /**
     * Restore a database that was saved in CrypSafe
     * TODO purge this method when upgrade period is over
     * @param ctx
     * @param sourceDbPath
     * @param sourceDbFilename
     * @throws IOException
     */
    public static boolean copyCrypSafeDbToApp(Context ctx, String sourceDbPath, String sourceDbFilename)
            throws IOException {

        // Get the file and path to the database
        String targetDbFilename="";
        if( sourceDbFilename.contentEquals("crypsafe1_db"))
            targetDbFilename = "detail_db";
        if( sourceDbFilename.contentEquals("crypsafe2_db"))
            targetDbFilename = "account_db";
        if( targetDbFilename.isEmpty())
            return false;

        File targetFile = ctx.getDatabasePath(targetDbFilename);
        String outPathWithName = targetFile.getPath();

        //Open storage db as the input stream
        String inPathWithName = sourceDbPath +"/"+ sourceDbFilename;
        File dbFile = new File(inPathWithName);
        FileInputStream fis = new FileInputStream(dbFile);

        //Open the target db as the output stream and delete it
        if( targetFile.delete())
            LogUtil.log("file deleted: "+ targetDbFilename);
        else
            LogUtil.log("ERROR, file NOT deleted: "+ targetDbFilename);

        OutputStream outputStream = new FileOutputStream(outPathWithName);

        //transfer bytes from the inputfile to the outputfile
        byte[] buffer = new byte[1024];
        int length;
        while ((length = fis.read(buffer))>0){
            outputStream.write(buffer, 0, length);
        }
        //Close the streams
        outputStream.flush();
        outputStream.close();
        fis.close();
        LogUtil.log("File restored: "+ targetDbFilename);

        return true;
    }

    public static void backupToEmail(Activity act){

            String version = "";
            try {
                PackageInfo pInfo = act.getPackageManager().getPackageInfo(act.getPackageName(), 0);
                version = pInfo.versionName;
            } catch (NameNotFoundException e1) { }
            String userMessage = "Two encrypted files are attached.\n\n"
                    +"In the event you need to restore a backup:\n"
                    +"1) Create a new folder Downloads/securesuite and Save both files to it\n"
                    +"2) Start SecureSuite and select menu, Backup/restore, Restore from storage\n"
                    +"3) Select the folder from step 1 and select Restore\n"
                    +"4) Enter the passphrase for this backup\n\n"
                    +"Visit http://securesuite.org for additional support information.\n\n"
                    +"SecureSuite version: "+version+"\n\n";

            try {
                String basePath = Util.createTimeStampedBackupFolder( act);
                BackupRestore.copyDbToStorage( basePath, SqlCipher.account_db);
                BackupRestore.copyDbToStorage( basePath, SqlCipher.detail_db);

                Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_EMAIL, new String[] {LicensePersist.getLicenseAccount(act)});
                intent.putExtra(Intent.EXTRA_SUBJECT, "SecureSuite Backup");
                intent.putExtra(Intent.EXTRA_TEXT, userMessage);

                File f1 = new File( basePath + "/" + SqlCipher.ACCOUNT_DB_NAME);
                File f2 = new File( basePath + "/" + SqlCipher.DETAIL_DB_NAME);

                //convert from paths to Android friendly Parcelable Uri's
                ArrayList<Uri> uris = new ArrayList<Uri>();
                uris.add( Uri.fromFile( f1 ));
                uris.add( Uri.fromFile( f2 ));

                intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
                act.startActivity(Intent.createChooser(intent, "Send email..."));
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    /**
     * Simple test for identifying a CrypSafe named database
     * @param ctx
     * @param mNewDbPath
     * @return
     */
    public static boolean testCrypSafeRestore(Context ctx, String mNewDbPath) {

        boolean found1 = false, found2=false;

        File[] files = new File(mNewDbPath).listFiles();

        for( File f : files){

            if( f.getName().contentEquals("crypsafe1_db"))
                found1 = true;
            if( f.getName().contentEquals("crypsafe2_db"))
                found2 = true;
        }

        return found1 && found2;
    }
}
