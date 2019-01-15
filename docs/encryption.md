Notes on app encryption
-----------------------

Note that CipherVfsPassword and DbSqlPassword need to be separate methods.
While it is tempting to combine these methods, only the SQL DB can be
rekeyed at this time. If the SQL DB is rekeyed then the CIPHER VFS will
have an invalid password. Revisit this subject when the CIPHER VFS can
be rekeyed.

//TODO document approach to app encryption
