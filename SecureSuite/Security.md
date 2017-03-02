SS Security  
-----------  

Security Dependencies  
---------------------  

1. Guardian Project SQLCipher  
2. Java's standard security packages  
3. Android Keystore  
4. YubiKey NEO  


Data in Place  
-------------  

1. The SS SQLCipher database uses a randomly generated 32 character password.
2. The database is encrypted and stored in the app private data area
3. The database passphrase is itself encrypted using an Android Keystore public key and saved 
in Android standard preferences. 
4. On startup a Keystore private key is used to decrypt the database passphrase into memory.
It is used to open the database then the memory is cleared.
5. The user can re-key the database using their own passphrase.


Data in Motion  
--------------  

1. SS uses a self signed security certificate for https communications.  
2. The certificate cannot be validated by the browser, hence it generates nasty warnings.  
3. The header utilized a security token to authenticate each https request, http requests are blocked.


Vulnerabilities  
---------------  

1. Math.random() can be more random, it is used to create the initial database password.  
2. Certain memory locations hold sensitive information.  
3. Brute force database passphrase, app entry passphrase, YubiKey, or network https encryption.
4. The password modal is stored in app private data.


Security Improvements  
---------------------  

1. Clear memory holding sensitive information after use.
2. Make failed password entry on Android app and web app more sophisticated. 
3. Replace Math.random().  
4. Use CORS or some method to leverage a confirmed security certificate.  

