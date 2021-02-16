/*
 * Copyright (c) 2018 Nuvolect LLC.
 * This software is offered for free under conditions of the GPLv3 open source software license.
 * Contact Nuvolect LLC for a less restrictive commercial license if you would like to use the software
 * without the GPLv3 restrictions.
 */

package com.nuvolect.securesuite.util;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

public class Passphrase {

    public static int ALPHA_UPPER = 1;
    public static int ALPHA_LOWER = 2;
    public static int NUMERIC     = 4;
    public static int SPECIAL     = 8;
    public static int HEX         = 16;
    public static int SYSTEM_MODE = ALPHA_UPPER | ALPHA_LOWER | NUMERIC;

    static final String ALPHA_UPPERS    = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    static final String ALPHA_LOWERS    = "abcdefghijklmnopqrstuvwxyz";
    static final String NUMERICS        = "0123456789";
    static final String SPECIALS        = "!$%@#";
    static final String HEXS            = "0123456789ABCDEF";

    /**
     * Generate a random password of the specific length using a variety of character types.
     * Validate and guarantee each variety of character types is used.
     * @param length
     * @param modeTarget
     * @return
     */
    public static char[] generateRandomPasswordChars(int length, int modeTarget) {

        boolean types_validated = false;
        char[] ranChars = new char[ length];
        int modeFound = 0;

        StringBuffer sourceBuffer = new StringBuffer( 0 );

        if( (modeTarget & ALPHA_UPPER) > 0)
            sourceBuffer.append( ALPHA_UPPERS);
        if( (modeTarget & ALPHA_LOWER) > 0)
            sourceBuffer.append( ALPHA_LOWERS);
        if( (modeTarget & NUMERIC) > 0)
            sourceBuffer.append( NUMERICS);
        if( (modeTarget & SPECIAL) > 0)
            sourceBuffer.append( SPECIALS);
        if( (modeTarget & HEX) > 0)
            sourceBuffer.append( HEXS);

        if( sourceBuffer.length() == 0)
            sourceBuffer.append("0123456789");

        int sourceLength = sourceBuffer.length();

        while (!types_validated) {

            for (int i = 0; i < length; i++) {
                double index = Math.random() * sourceLength;
                char randomChar = sourceBuffer.charAt((int) index);
                ranChars[i] = randomChar;

                if ( ALPHA_UPPERS.contains(String.valueOf(randomChar)))
                    modeFound |= ALPHA_UPPER;
                if ( ALPHA_LOWERS.contains(String.valueOf(randomChar)))
                    modeFound |= ALPHA_LOWER;
                if ( NUMERICS.contains(String.valueOf(randomChar)))
                    modeFound |= NUMERIC;
                if ( SPECIALS.contains(String.valueOf(randomChar)))
                    modeFound |= SPECIAL;
                if ( HEXS.contains(String.valueOf(randomChar)))
                    modeFound |= HEX;
            }
            /**
             * Support the case where extra conditions are met. Certain upper case letters
             * also qualify as HEX as do numbers 0-9. The conditional makes sure you get at
             * least the type want but sometimes you also get HEX.
             */
            if ( (modeFound & modeTarget) == modeTarget)
                types_validated = true;
            else
                modeFound = 0;
        }

        sourceBuffer.delete( 0, sourceLength);

        return ranChars;
    }

    /**
     * Generate a random password of the specific length using a variety of character types.
     *
     * @param length
     * @param mode
     * @return
     */
    public static byte[] generateRandomPasswordBytes(int length, int mode) {

        char[] chars = generateRandomPasswordChars( length, mode);
        byte[] bytes = toBytes( chars);
        chars = cleanArray( chars);
        return bytes;
    }

    /**
     * Generate a random password of the specific length using a variety of character types.
     *
     * @param length
     * @param mode
     * @return
     */
    public static String generateRandomString(int length, int mode) {

        char[] chars = generateRandomPasswordChars( length, mode);
        String string = new String(chars);

        // Dispose of the char array
        chars = CrypUtil.cleanArray( chars);

        return string;
    }

    /**
     * Convert a char array to a byte array.
     * @param chars
     * @return
     */
    public static byte[] toBytes(char[] chars) {//FIXME remove duplicate methods into new class
        CharBuffer charBuffer = CharBuffer.wrap(chars);
        ByteBuffer byteBuffer = Charset.forName("UTF-8").encode(charBuffer);
        byte[] bytes = Arrays.copyOfRange(byteBuffer.array(),
                byteBuffer.position(), byteBuffer.limit());
        Arrays.fill(byteBuffer.array(), (byte) 0); // clear sensitive data
        return bytes;
    }

    /**
     * Convert a byte array to a char array.
     * @param bytes
     * @return
     */
    public static char[] toChars(byte[] bytes){

        char chars[] = new char[0];

        try {
            chars = new String( bytes, "UTF-8").toCharArray();
        } catch (UnsupportedEncodingException e) {
            LogUtil.log(" Exception in toChars");
        }
        return chars;
    }

    /**
     * Clear an array changing the contents to zero then changing the
     * array size to zero.
     *
     * @param dirtyArray
     */
    public static char[] cleanArray(char[] dirtyArray) {

        for(int i = 0; i< dirtyArray.length; i++){

            dirtyArray[i] = 0;//Clear contents.
        }
        dirtyArray = new char[0];//Don't save the size

        return dirtyArray;
    }

    /**
     * Clear an array changing the contents to zero then changing the
     * array size to zero.
     *
     * @param dirtyArray
     * @return
     */
    public static byte[] cleanArray(byte[] dirtyArray) {

        for(int i = 0; i< dirtyArray.length; i++){

            dirtyArray[i] = 0;//Clear contents.
        }
        dirtyArray = new byte[0];//Don't save the size

        return dirtyArray;
    }
}
