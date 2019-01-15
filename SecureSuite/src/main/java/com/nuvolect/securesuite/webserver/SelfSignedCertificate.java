/*
 * Copyright (c) 2018 Nuvolect LLC.
 * This software is offered for free under conditions of the GPLv3 open source software license.
 * Contact Nuvolect LLC for a less restrictive commercial license if you would like to use the software
 * without the GPLv3 restrictions.
 */

package com.nuvolect.securesuite.webserver;

import android.content.Context;

import com.nuvolect.securesuite.util.LogUtil;
import com.nuvolect.securesuite.util.Passphrase;
import com.nuvolect.securesuite.util.Persist;

import org.spongycastle.asn1.x500.X500Name;
import org.spongycastle.asn1.x500.X500NameBuilder;
import org.spongycastle.asn1.x500.style.BCStyle;
import org.spongycastle.asn1.x509.BasicConstraints;
import org.spongycastle.asn1.x509.ExtendedKeyUsage;
import org.spongycastle.asn1.x509.Extension;
import org.spongycastle.asn1.x509.KeyPurposeId;
import org.spongycastle.asn1.x509.KeyUsage;
import org.spongycastle.cert.X509CertificateHolder;
import org.spongycastle.cert.X509v3CertificateBuilder;
import org.spongycastle.cert.jcajce.JcaX509CertificateConverter;
import org.spongycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.operator.ContentSigner;
import org.spongycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * Generate a self-signed certificate and store it in a keystore.
 * The keystore password is secured in Android's keystore.
 *
 * Here's a complete self-signed ECDSA certificate generator that creates certificates
 * usable in TLS connections on both client and server side.
 * It was tested with BouncyCastle 1.57. Similar code can be used to create RSA certificates.
 *
 * https://stackoverflow.com/questions/29852290/self-signed-x509-certificate-with-bouncy-castle-in-java
 * Robert Vazan, https://stackoverflow.com/users/1981276/robert-va%C5%BEan
 */
public class SelfSignedCertificate {

    public static boolean makeKeystore(Context ctx, String keystoreFilepath, boolean recreate){

        boolean success = true;
        File keystoreFile = new File( keystoreFilepath );

        // If the keystore already exists and recreate not requested, nothing to do.
        boolean skipOut = keystoreFile.exists() && Persist.keyExists( ctx, Persist.SELFSIGNED_KS_KEY);
        if( skipOut && ! recreate){

            LogUtil.log(LogUtil.LogType.CERTIFICATE, "Certificate exists, recreate: "+recreate);
            return success;
        }

        try {

            // Generate a random password, encrypt it with Android keystore then persist encrypted value
            char[] storePassword = Passphrase.generateRandomPasswordChars( 32, Passphrase.SYSTEM_MODE);
            Persist.putSelfsignedKsKey( ctx, storePassword);

            SecureRandom random = new SecureRandom();
            Provider bcProvider = new BouncyCastleProvider();
            Security.addProvider(bcProvider);

            // create keypair
            KeyPairGenerator keypairGen = KeyPairGenerator.getInstance("RSA");// RSA fails
            keypairGen.initialize(4096);
            KeyPair keypair = keypairGen.generateKeyPair();

            // yesterday
            Date validityBeginDate = new Date(System.currentTimeMillis() - 24L * 60 * 60 * 1000);
            // in 25 years
            Date validityEndDate = new Date(System.currentTimeMillis() + 25L * 365 * 24 * 60 * 60 * 1000);

            // fill in certificate fields
            X500Name subject = new X500NameBuilder(BCStyle.INSTANCE)
                    .addRDN(BCStyle.CN, "Nuvolect LLC")
                    .addRDN(BCStyle.O, "Nuvolect LLC")
                    .addRDN(BCStyle.OU, "Development")
                    .addRDN(BCStyle.C, "US")
                    .addRDN(BCStyle.L, "Orlando")
                    .addRDN(BCStyle.ST, "FL")
                    .build();
            byte[] id = new byte[20];
            random.nextBytes(id);
            BigInteger serial = new BigInteger(160, random);
            X509v3CertificateBuilder certificate = new JcaX509v3CertificateBuilder(
                    subject,
                    serial,
                    validityBeginDate,
                    validityEndDate,
                    subject,
                    keypair.getPublic());
            certificate.addExtension(Extension.subjectKeyIdentifier, false, id);
            certificate.addExtension(Extension.authorityKeyIdentifier, false, id);

            BasicConstraints constraints = new BasicConstraints(false);// cannot sign other certificates
            certificate.addExtension(
                    Extension.basicConstraints,
                    true,
                    constraints.getEncoded());
            KeyUsage usage = new KeyUsage(
                    KeyUsage.digitalSignature |
                            KeyUsage.nonRepudiation |
                            KeyUsage.keyCertSign |
                            KeyUsage.keyEncipherment |
                            KeyUsage.dataEncipherment
            );
            certificate.addExtension(Extension.keyUsage, false, usage.getEncoded());
            ExtendedKeyUsage usageEx = new ExtendedKeyUsage(new KeyPurposeId[] {
                    KeyPurposeId.id_kp_serverAuth,
                    KeyPurposeId.id_kp_clientAuth
            });
            certificate.addExtension(
                    Extension.extendedKeyUsage,
                    false,
                    usageEx.getEncoded());


            // build BouncyCastle certificate
            ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                    .build(keypair.getPrivate());
            X509CertificateHolder holder = certificate.build(signer);

            // convert to JRE certificate
            JcaX509CertificateConverter converter = new JcaX509CertificateConverter();
            converter.setProvider(new BouncyCastleProvider());
            X509Certificate x509Certificate = converter.getCertificate(holder);

            KeyStore keyStore = KeyStore.getInstance("BKS");
            keyStore.load( null, storePassword);// Initialize it
            keyStore.setCertificateEntry("deepdive_cert", x509Certificate);

            X509Certificate[] chain = new X509Certificate[1];
            chain[0] = x509Certificate;
            KeyStore.PrivateKeyEntry privateKeyEntry = new KeyStore.PrivateKeyEntry(keypair.getPrivate(), chain);
            keyStore.setEntry("CA-Key", privateKeyEntry, new KeyStore.PasswordProtection(storePassword));

            FileOutputStream fos = new FileOutputStream( keystoreFile);
            keyStore.store( fos, storePassword);
            fos.close();
            LogUtil.log(LogUtil.LogType.CERTIFICATE, "Certificate created");

        } catch (Exception e) {
            LogUtil.logException(LogUtil.LogType.CERTIFICATE, e);
            success = false;
        }
        return success;
    }
}

