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

package com.nuvolect.securesuite.util;


public class Safe {

    private static final boolean DEBUG = false;

    //FUTURE reduce four single quotes to two single quotes
    /**
     * Make a safe string by making pairs of single quotes, removing non-printing characters
     * and replacing the null string with an empty string.
     * @param unsafeString
     * @return safeString
     */
    public static String safeString(String unsafeString){

        if(unsafeString == null){
            return "";
        }

        char[] oldChars = new char[unsafeString.length()];
        int extraSpace = Math.max( unsafeString.length() / 10, 20);
        unsafeString.getChars(0, unsafeString.length(), oldChars, 0);
        int bufferSize = unsafeString.length()+extraSpace;// to handle extra quote characters
        char[] newChars = new char[ bufferSize];
        int newLen = 0;
        for (int j = 0; j < unsafeString.length(); j++) {
            char ch = oldChars[j];
            if( ch == 39){
                // Found a single quote, put in two
                newChars[newLen] = ch;
                newLen++;
                newChars[newLen] = ch;
                newLen++;
            }else
                if( newLen == bufferSize -1){
                    LogUtil.log("Input note string too long");
                    break;
                }
                if (ch == 9 || ch == 10 || ch >= ' ') { // accept newline, tab and all printing chars
                    newChars[newLen] = ch;
                    newLen++;
                }
        }
        String safeString = new String(newChars, 0, newLen);

        if(DEBUG){
            if( unsafeString.contains("'")){
                LogUtil.log("found quote: "+unsafeString );
                LogUtil.log("quoted: "+safeString);
            }
        }
        return safeString.trim();
    }
}
