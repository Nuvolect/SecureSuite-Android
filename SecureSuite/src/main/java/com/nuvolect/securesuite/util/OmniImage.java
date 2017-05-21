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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ThumbnailUtils;

import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

/**
 * Image manipulation methods for OmniFile.  Works on all volume types.
 */
public class OmniImage {

    static int tmbSize = 48;
    static final boolean DEBUG = LogUtil.DEBUG;

    public static OmniFile makeThumbnail(OmniFile file) {

        OmniFile thumbnailFile = getThumbnailFile( file );
        if( thumbnailFile.exists()){
            if( DEBUG )
                LogUtil.log(LogUtil.LogType.OMNI_IMAGE,"Thumb exists: "+thumbnailFile.getPath());
            return thumbnailFile;
        }

        InputStream is = file.getFileInputStream();

        Bitmap resized = ThumbnailUtils.extractThumbnail(
                BitmapFactory.decodeStream(is), tmbSize, tmbSize);


        OutputStream out = null;
        try {
            out = thumbnailFile.getOutputStream();
            resized.compress(Bitmap.CompressFormat.PNG, 100, out);
            // tmb is your Bitmap instance
            // PNG is a lossless format, the compression factor (100) is ignored
        } catch (Exception e) {
            if( DEBUG )
                LogUtil.logException(LogUtil.LogType.OMNI_IMAGE,e);
        } finally {
            try {

                if( is != null)
                    is.close();

                if (out != null)
                    out.close();

            } catch (IOException e) {
                if( DEBUG )
                    LogUtil.logException(LogUtil.LogType.OMNI_IMAGE,e);
                return null;// error condition, tells client to request thumbnail later
            }
        }
        if( DEBUG )
            LogUtil.log(LogUtil.LogType.OMNI_IMAGE,"Thumb created: "+thumbnailFile.getPath());
        return thumbnailFile;
    }

    public static Bitmap getThumbnailBitmap(OmniFile file) {

        OmniFile thumbnailFile = getThumbnailFile( file );
        if( thumbnailFile.exists()){
            if( DEBUG )
                LogUtil.log(LogUtil.LogType.OMNI_IMAGE,"Thumb exists: "+thumbnailFile.getPath());

            Bitmap b = null;
            try {
                long startTime = System.currentTimeMillis();

                InputStream fis = thumbnailFile.getFileInputStream();
                b = BitmapFactory.decodeStream(fis, null, null);
                fis.close();

                if( DEBUG )
                    LogUtil.log(LogUtil.LogType.OMNI_IMAGE,"Thumb load time: "
                            +String.valueOf(System.currentTimeMillis() - startTime));

            } catch (Exception e) {
                LogUtil.logException(LogUtil.LogType.OMNI_IMAGE,"error decoding bitmap preview",e);
            }
            return b;
        }

        InputStream is = file.getFileInputStream();

        BitmapFactory.Options bitmapFactoryOptions = new BitmapFactory.Options();
        bitmapFactoryOptions.inSampleSize = 8;

        Bitmap resized = ThumbnailUtils.extractThumbnail(
                BitmapFactory.decodeStream(is), tmbSize, tmbSize, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);

        OutputStream out = null;
        try {
            out = thumbnailFile.getOutputStream();
            resized.compress(Bitmap.CompressFormat.PNG, 100, out);
            // tmb is your Bitmap instance
            // PNG is a lossless format, the compression factor (100) is ignored
        } catch (Exception e) {
            if( DEBUG )
                LogUtil.logException(LogUtil.LogType.OMNI_IMAGE,e);
        } finally {
            try {

                if( is != null)
                    is.close();

                if (out != null)
                    out.close();

            } catch (IOException e) {
                if( DEBUG )
                    LogUtil.logException(LogUtil.LogType.OMNI_IMAGE,e);
                return null;// error condition, tells client to request thumbnail later
            }
        }
        if( DEBUG )
            LogUtil.log(LogUtil.LogType.OMNI_IMAGE,"Thumb created: "+thumbnailFile.getPath());

        return resized;
    }

    /**
     * Gets the file, does not create the thumbnail.
     * Use updateThumbnail createThumbnail, or getThumbnailBitmap for that.
     * @param file
     * @return
     */
    public static OmniFile getThumbnailFile(OmniFile file) {

        String volumeId = file.getVolumeId();
        String thumbnailFileName = file.getHash();// hash is the name
        String thumbPath = Omni.THUMBNAIL_FOLDER_PATH + thumbnailFileName;
        OmniFile thumbnailFile = new OmniFile( volumeId, thumbPath);

        return thumbnailFile;
    }


    public static void updateThumbnail(OmniFile file) {

        deleteThumbnail( file );
        makeThumbnail( file );
    }

    public static boolean deleteThumbnail(OmniFile file){

        OmniFile thumbnailFile = getThumbnailFile( file );
        if( thumbnailFile.exists())
            return thumbnailFile.delete();
        else
            return false;
    }


    /**
     * Rotate an image by an arbitrary number of degrees.
     * @param file
     * @param degrees
     * @param quality
     * @return
     * @throws IOException
     */
    public static OmniFile rotateImage(OmniFile file, float degrees, int quality) throws IOException {

        InputStream is = file.getFileInputStream();
        BufferedInputStream bufferedInputStream = new BufferedInputStream(is);
        Bitmap bitmap = BitmapFactory.decodeStream(bufferedInputStream);
        is.close();

        Matrix matrix = new Matrix();
        matrix.postRotate( degrees );

        Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0,
                bitmap.getWidth(),  bitmap.getHeight(), matrix, true);

        Bitmap.CompressFormat compressFormat;
        String fileName = file.getName().toLowerCase(Locale.US);
        if( fileName.endsWith(".jpg"))
            compressFormat = Bitmap.CompressFormat.JPEG;
        else
        if( fileName.endsWith(".png"))
            compressFormat = Bitmap.CompressFormat.PNG;
        else
        if( fileName.endsWith(".jpeg"))
            compressFormat = Bitmap.CompressFormat.JPEG;
        else{

            compressFormat = Bitmap.CompressFormat.JPEG;
            String volumeId = file.getVolumeId();
            String path = file.getPath()+".jpg";
            file = new OmniFile(volumeId, path);
        }

        OutputStream out = file.getOutputStream();
        try {

            rotated.compress( compressFormat, quality, out);

        } catch (Exception e) {
            e.printStackTrace();
            if( DEBUG )
                LogUtil.logException(LogUtil.LogType.OMNI_IMAGE,e);
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                if( DEBUG )
                    LogUtil.logException(LogUtil.LogType.OMNI_IMAGE,e);
                return null;// error condition, tells client to request thumbnail later
            }
        }

        // Update the modified time and thumbnail
        file.setLastModified();
        OmniImage.deleteThumbnail( file );

        return file;
    }

    public static OmniFile resizeImage(OmniFile file, int width, int height, int quality) throws IOException {

        InputStream is = file.getFileInputStream();

        Bitmap resized = ThumbnailUtils.extractThumbnail(
                BitmapFactory.decodeStream(is), width, height);

        Bitmap.CompressFormat compressFormat;
        String fileName = file.getName().toLowerCase(Locale.US);
        if( fileName.endsWith(".jpg"))
            compressFormat = Bitmap.CompressFormat.JPEG;
        else
        if( fileName.endsWith(".png"))
            compressFormat = Bitmap.CompressFormat.PNG;
        else
        if( fileName.endsWith(".jpeg"))
            compressFormat = Bitmap.CompressFormat.JPEG;
        else{

            compressFormat = Bitmap.CompressFormat.JPEG;
            String volumeId = file.getVolumeId();
            String path = file.getPath()+".jpg";
            file = new OmniFile(volumeId, path);
        }

        OutputStream out = file.getOutputStream();
        try {

            resized.compress( compressFormat, quality, out);

        } catch (Exception e) {
            e.printStackTrace();
            if( DEBUG )
                LogUtil.logException(LogUtil.LogType.OMNI_IMAGE,e);
        } finally {
            try {

                if( is != null)
                    is.close();
                if (out != null)
                    out.close();

            } catch (IOException e) {
                if( DEBUG )
                    LogUtil.logException(LogUtil.LogType.OMNI_IMAGE,e);
                return null;// error condition, tells client to request thumbnail later
            }
        }

        // Update the modified time and thumbnail
        file.setLastModified();
        OmniImage.updateThumbnail( file );

        return file;
    }

    public static OmniFile cropImage(OmniFile file, int x, int y, int width, int height, int quality) throws IOException {

        InputStream is = file.getFileInputStream();
        BufferedInputStream bufferedInputStream = new BufferedInputStream(is);
        Bitmap bitmap = BitmapFactory.decodeStream(bufferedInputStream);
        is.close();

        /**
         * Work around for elFinder crop bug
         */
        if( y + height > bitmap.getHeight())
            height = bitmap.getHeight() - y;

        if( x + width > bitmap.getWidth())
            width = bitmap.getWidth() - x;

        if( quality == 0)
            quality = 100;

        Bitmap resized = Bitmap.createBitmap(bitmap, x, y, width,  height);

        Bitmap.CompressFormat compressFormat;
        String fileName = file.getName().toLowerCase(Locale.US);
        if( fileName.endsWith(".jpg"))
            compressFormat = Bitmap.CompressFormat.JPEG;
        else
        if( fileName.endsWith(".png"))
            compressFormat = Bitmap.CompressFormat.PNG;
        else
        if( fileName.endsWith(".jpeg"))
            compressFormat = Bitmap.CompressFormat.JPEG;
        else{

            compressFormat = Bitmap.CompressFormat.JPEG;
            String volumeId = file.getVolumeId();
            String path = file.getPath()+".jpg";
            file = new OmniFile(volumeId, path);
        }

        OutputStream out = file.getOutputStream();
        try {

            resized.compress( compressFormat, quality, out);

        } catch (Exception e) {
            e.printStackTrace();
            if( DEBUG )
                LogUtil.logException(LogUtil.LogType.OMNI_IMAGE,e);
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                if( DEBUG )
                    LogUtil.logException(LogUtil.LogType.OMNI_IMAGE,e);
                return null;// error condition, tells client to request thumbnail later
            }
        }

        // Update the modified time and thumbnail
        file.setLastModified();

        boolean del =  OmniImage.deleteThumbnail(file);
        LogUtil.log(LogUtil.LogType.OMNI_IMAGE, "delete thumbnail: "+del);

        return file;
    }

    private static BitmapFactory.Options options = new BitmapFactory.Options();

    public static String getDim(OmniFile file) {

        options.inJustDecodeBounds = true;

        //Returns null, sizes are in the options variable
        BitmapFactory.decodeStream( file.getFileInputStream(), null, options);
        int width = options.outWidth;
        int height = options.outHeight;

        return width+"x"+height;
    }

    public static void addPsImageSize(OmniFile omniFile, JsonObject psObject) {

        options.inJustDecodeBounds = true;

        //Returns null, sizes are in the options variable
        BitmapFactory.decodeStream(omniFile.getFileInputStream(), null, options);
        psObject.addProperty("h", options.outHeight);
        psObject.addProperty("w", options.outWidth);
    }

    public static boolean isImage(OmniFile f) {

        String fileName = f.getName().toLowerCase(Locale.US);
        if( fileName.endsWith(".jpg"))
            return true;
        else
        if( fileName.endsWith(".png"))
            return true;
        else
        if( fileName.endsWith(".jpeg"))
            return true;

        return false;
    }
}
