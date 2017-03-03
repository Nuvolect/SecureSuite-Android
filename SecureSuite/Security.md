SS Security  
-----------  

Security Dependencies  
---------------------  

1. Guardian Project SQLCipher  
1. Java's standard security packages  
1. Android Keystore  
1. YubiKey NEO  


Data in Place  
-------------  

1. The SS SQLCipher database uses a randomly generated 32 character password.
1. The database is encrypted and stored in the app private data area
1. The database passphrase is itself encrypted using an Android Keystore public key and saved 
in Android standard preferences. 
1. On startup a Keystore private key is used to decrypt the database passphrase into memory.
It is used to open the database then the memory is cleared.
1. The user can re-key the database using their own passphrase.


Data in Motion  
--------------  

1. SS uses a self signed security certificate for https communications.  
1. The certificate cannot be validated by the browser, hence it generates nasty warnings.  
1. The header utilized a security token to authenticate each https request, http requests are blocked.


Vulnerabilities  
---------------  

1. Math.random() can be more random, it is used to create the initial database password.  
1. Certain memory locations hold sensitive information.  
1. Brute force database passphrase, app entry passphrase, YubiKey, or network https encryption.
1. The password modal is stored in app private data.
1. Private share/ folder can contain .vcard files shared with other apps.


Security Improvements  
---------------------  

1. Clear memory holding sensitive information after use.
1. Clear share/ folder after use or when user exits app.
1. Make failed password entry on Android app and web app more sophisticated. 
1. Replace Math.random() with a more random method.  
1. Use CORS or some method to leverage a confirmed security certificate.  

