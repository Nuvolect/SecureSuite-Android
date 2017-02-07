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

package com.nuvolect.securesuite.nfc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
/**
 * Utility class for converting hex data to bytes and back again.
 */
public class Hex {
    private static final Encoder encoder = new HexEncoder();

    public static String toHexString(
            byte[] data)
    {
        return toHexString(data, 0, data.length);
    }

    public static String toHexString(
            byte[] data,
            int    off,
            int    length)
    {
        byte[] encoded = encode(data, off, length);
        return com.nuvolect.securesuite.nfc.Strings.fromByteArray(encoded);
    }

    /**
     * encode the input data producing a Hex encoded byte array.
     *
     * @return a byte array containing the Hex encoded data.
     */
    public static byte[] encode(
            byte[]    data)
    {
        return encode(data, 0, data.length);
    }

    /**
     * encode the input data producing a Hex encoded byte array.
     *
     * @return a byte array containing the Hex encoded data.
     */
    public static byte[] encode(
            byte[]    data,
            int       off,
            int       length)
    {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();

        try
        {
            encoder.encode(data, off, length, bOut);
        }
        catch (Exception e)
        {
            throw new EncoderException("exception encoding Hex string: " + e.getMessage(), e);
        }

        return bOut.toByteArray();
    }

    /**
     * Hex encode the byte data writing it to the given output stream.
     *
     * @return the number of bytes produced.
     */
    public static int encode(
            byte[]         data,
            OutputStream out)
            throws IOException
    {
        return encoder.encode(data, 0, data.length, out);
    }

    /**
     * Hex encode the byte data writing it to the given output stream.
     *
     * @return the number of bytes produced.
     */
    public static int encode(
            byte[]         data,
            int            off,
            int            length,
            OutputStream   out)
            throws IOException
    {
        return encoder.encode(data, off, length, out);
    }

    /**
     * decode the Hex encoded input data. It is assumed the input data is valid.
     *
     * @return a byte array representing the decoded data.
     */
    public static byte[] decode(
            byte[]    data)
    {
        ByteArrayOutputStream    bOut = new ByteArrayOutputStream();

        try
        {
            encoder.decode(data, 0, data.length, bOut);
        }
        catch (Exception e)
        {
            throw new DecoderException("exception decoding Hex data: " + e.getMessage(), e);
        }

        return bOut.toByteArray();
    }

    /**
     * decode the Hex encoded String data - whitespace will be ignored.
     *
     * @return a byte array representing the decoded data.
     */
    public static byte[] decode(
            String    data)
    {
        ByteArrayOutputStream    bOut = new ByteArrayOutputStream();

        try
        {
            encoder.decode(data, bOut);
        }
        catch (Exception e)
        {
            throw new DecoderException("exception decoding Hex string: " + e.getMessage(), e);
        }

        return bOut.toByteArray();
    }

    /**
     * decode the Hex encoded String data writing it to the given output stream,
     * whitespace characters will be ignored.
     *
     * @return the number of bytes produced.
     */
    public static int decode(
            String          data,
            OutputStream    out)
            throws IOException
    {
        return encoder.decode(data, out);
    }

    /**
     * Convert a hex string to a Modhex string
     * @param hex
     * @return
     */
    public static String hexToModhex(String hex){

        String modhex = "";
        char[] char_hex = hex.toCharArray();

        for( char c : char_hex){

            switch (c){
                case '0': modhex = modhex + 'c'; break;
                case '1': modhex = modhex + 'b'; break;
                case '2': modhex = modhex + 'd'; break;
                case '3': modhex = modhex + 'e'; break;
                case '4': modhex = modhex + 'f'; break;
                case '5': modhex = modhex + 'g'; break;
                case '6': modhex = modhex + 'h'; break;
                case '7': modhex = modhex + 'i'; break;
                case '8': modhex = modhex + 'j'; break;
                case '9': modhex = modhex + 'k'; break;
                case 'a': modhex = modhex + 'l'; break;
                case 'b': modhex = modhex + 'n'; break;
                case 'c': modhex = modhex + 'r'; break;
                case 'd': modhex = modhex + 't'; break;
                case 'e': modhex = modhex + 'u'; break;
                case 'f': modhex = modhex + 'v'; break;
            }
        }

        return modhex;
    }

    /**
     * Convert a Modhex string to a hex string
     * @param modhex
     * @return
     */
    public static String modHextoHex(String modhex){

        String hex = "";
        char[] char_modhex = modhex.toCharArray();

        for( char c : char_modhex){

            switch (c){
                case 'c': hex = hex + '0'; break;
                case 'b': hex = hex + '1'; break;
                case 'd': hex = hex + '2'; break;
                case 'e': hex = hex + '3'; break;
                case 'f': hex = hex + '4'; break;
                case 'g': hex = hex + '5'; break;
                case 'h': hex = hex + '6'; break;
                case 'i': hex = hex + '7'; break;
                case 'j': hex = hex + '8'; break;
                case 'k': hex = hex + '9'; break;
                case 'l': hex = hex + 'a'; break;
                case 'n': hex = hex + 'b'; break;
                case 'r': hex = hex + 'c'; break;
                case 't': hex = hex + 'd'; break;
                case 'u': hex = hex + 'e'; break;
                case 'v': hex = hex + 'f'; break;
            }
        }

        return hex;
    }
}
