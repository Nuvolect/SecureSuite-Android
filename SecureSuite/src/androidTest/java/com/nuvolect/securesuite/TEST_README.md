# Instrumented Tests

Note that MyGroupsTest.java deletes and recreates the database.
All contacts in the database will be lost.

Add new tests to [UnitTestSuite.java](testsuite/UnitTestSuite.java).

Current tests
-------------
1. [CryptoDbTest.java](data/CryptoDbTest.java)
1. [CryptoFilesystemTest.java](data/CryptoFilesystemTest.java)
1. [MyGroupsTest.java](data/MyGroupsTest.java) warning, deletes database
1. [CrypUtilTest.java](util/CrypUtilTest.java)
1. [InputStreamAsJsonTest.java](util/InputStreamAsJsonTest.java)
1. [KeystoreUtilTest.java](util/KeystoreUtilTest.java)
1. [OmniTest.java](util/OmniTest.java)
1. [PersistTest.java](util/PersistTest.java)
1. [SelfSignedKeystoreTest.java](util/SelfSignedKeystoreTest.java)
1. [TestFilesHelper.java](util/TestFilesHelper.java)
1. [CmdArchiveTest.java](webserver/connector/CmdArchiveTest.java)
1. [CmdExtractTest.java](webserver/connector/CmdExtractTest.java)
1. [CmdMkfileTest.java](webserver/connector/CmdMkfileTest.java)
1. [CmdRmFileTest.java](webserver/connector/CmdRmFileTest.java)
1. [CommTest.java](webserver/CommTest.java)

## Backlog of tests to create

1. Server REST interface tests
1. elFinder connector API tests
1. SqlCipher tests
1. Symmetric and asymettric encryption tests
1. UI tests


