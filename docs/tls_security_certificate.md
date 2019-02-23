# Transport Layer Security

Communications are protected with TLS https security. 
* The application self generates a self-signed certificate.  
* This certificate is used for encrypted communications between a web browser and the the Android app.  
* The certificate is also used to communicate between the primary and companion Android devices.  

_ About PEM, Base64 encoded DER file.  //TODO
_ How to export the pem file  //TODO
_ How to import the pem file into your keychain on Mac  //TODO

_ About warnings and Not Secure  //TODO


Command to extract a PEM file from a server  
~~~
echo | openssl s_client -connect ${MY_SERVER}:443 2>&1 | \
 sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > mycert.pem
~~~



[Web reference](https://serverfault.com/questions/9708/what-is-a-pem-file-and-how-does-it-differ-from-other-openssl-generated-key-file#9717)

## Security certificate debugging with Openssl

openssl s_client -connect 10.0.1.11:8002
~~~
CONNECTED(00000003)
depth=0 CN = Nuvolect LLC, O = Nuvolect LLC, OU = Development, C = US, L = Orlando, ST = FL, emailAddress = support@nuvolect.com, unstructuredName = securesuite.org, unstructuredAddress = FL
verify error:num=18:self signed certificate
verify return:1
depth=0 CN = Nuvolect LLC, O = Nuvolect LLC, OU = Development, C = US, L = Orlando, ST = FL, emailAddress = support@nuvolect.com, unstructuredName = securesuite.org, unstructuredAddress = FL
verify return:1
---
Certificate chain
 0 s:/CN=Nuvolect LLC/O=Nuvolect LLC/OU=Development/C=US/L=Orlando/ST=FL/emailAddress=support@nuvolect.com/unstructuredName=securesuite.org/unstructuredAddress=FL
   i:/CN=Nuvolect LLC/O=Nuvolect LLC/OU=Development/C=US/L=Orlando/ST=FL/emailAddress=support@nuvolect.com/unstructuredName=securesuite.org/unstructuredAddress=FL
---
Server certificate
-----BEGIN CERTIFICATE-----
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
kqUPEe4ORdlN4GPLLTZ6e44=
-----END CERTIFICATE-----
subject=/CN=Nuvolect LLC/O=Nuvolect LLC/OU=Development/C=US/L=Orlando/ST=FL/emailAddress=support@nuvolect.com/unstructuredName=securesuite.org/unstructuredAddress=FL
issuer=/CN=Nuvolect LLC/O=Nuvolect LLC/OU=Development/C=US/L=Orlando/ST=FL/emailAddress=support@nuvolect.com/unstructuredName=securesuite.org/unstructuredAddress=FL
---
No client certificate CA names sent
Server Temp Key: ECDH, P-256, 256 bits
---
SSL handshake has read 2498 bytes and written 318 bytes
---
New, TLSv1/SSLv3, Cipher is ECDHE-RSA-CHACHA20-POLY1305
Server public key is 4096 bit
Secure Renegotiation IS supported
Compression: NONE
Expansion: NONE
No ALPN negotiated
SSL-Session:
    Protocol  : TLSv1.2
    Cipher    : ECDHE-RSA-CHACHA20-POLY1305
    Session-ID: E8E549707F4B0BFBBE5400E9292FDA103E9214171ABAAF21ED14453C6DCAB624
    Session-ID-ctx: 
    Master-Key: 35CFC59A2554E0E1DF4A29BC4345204FBBF514B4DFC99A585B8A9E504C3F69EEE57F8C4BF1BF83CFE60D0FEE1B239E32
    Start Time: 1550694967
    Timeout   : 7200 (sec)
    Verify return code: 18 (self signed certificate)
---
closed
~~~

## Security certificate debugging with CURL

curl -iv https://10.0.1.11:8002/
~~~
*   Trying 10.0.1.11...
* TCP_NODELAY set
* Connected to 10.0.1.11 (10.0.1.11) port 8002 (#0)
* ALPN, offering h2
* ALPN, offering http/1.1
* Cipher selection: ALL:!EXPORT:!EXPORT40:!EXPORT56:!aNULL:!LOW:!RC4:@STRENGTH
* successfully set certificate verify locations:
*   CAfile: /etc/ssl/cert.pem
  CApath: none
* TLSv1.2 (OUT), TLS handshake, Client hello (1):
* TLSv1.2 (IN), TLS handshake, Server hello (2):
* TLSv1.2 (IN), TLS handshake, Certificate (11):
* TLSv1.2 (OUT), TLS alert, Server hello (2):
* error:0DFFF07B:asn1 encoding routines:CRYPTO_internal:header too long
* stopped the pause stream!
* Closing connection 0
curl: (35) error:0DFFF07B:asn1 encoding routines:CRYPTO_internal:header too long
~~~
