Building SecureSuite
====================

Basics
------

SecureSuite uses [Gradle](http://gradle.org) to build the project and to maintain
dependencies.  However, you needn't install it yourself; the
"gradle wrapper" `gradlew`, mentioned below, will do that for you.

Building SecureSuite
--------------------

The following steps should help you (re)build SecureSuite from the command line.

1. Checkout the SecureSuite-Android project source with the command:

        git clone https://github.com/Nuvolect/SecureSuite-Android.git

2. Make sure you have the [Android SDK](https://developer.android.com/sdk/index.html) installed.
3. Ensure that the following packages are installed from the Android SDK manager:
    * Android SDK Build Tools
    * SDK Platform (API level 28)
    * Android Support Repository
    * Google Repository
4. Create a local.properties file at the root of your source checkout and add an sdk.dir entry to it.  For example:

        sdk.dir=/Application/android-sdk-macosx

5. Execute Gradle:

        ./gradlew build

Setting up a development environment
------------------------------------

[Android Studio](https://developer.android.com/sdk/installing/studio.html) is the recommended development environment.

1. Install Android Studio.
1. Open Android Studio. On a new installation, the Quickstart panel will appear. If you have open projects, close them using "File > Close Project" to see the Quickstart panel.
1. From the Quickstart panel, choose "Configure" then "SDK Manager".
1. In the SDK Tools tab of the SDK Manager, make sure that the "Android Support Repository" is installed, and that the latest "Android SDK build-tools" are installed. Click "OK" to return to the Quickstart panel.
1. From the Quickstart panel, choose "Checkout from Version Control" then "git".
1. Paste the URL for the SecureSuite-Android project when prompted (https://github.com/Nuvolect/SecureSuite-Android.git).
1. Android studio should detect the presence of a project file and ask you whether to open it. Click "yes".
1. Default config options should be good enough.
1. Project initialisation and build should proceed.

Contributing code
-----------------

[Code contributions](/CONTRIBUTING.md) should be sent via github as pull requests, from feature branches [as explained here](https://help.github.com/articles/using-pull-requests).

Mailing list
------------

Development discussion happens on the Nuvolect mailing list.
[To join](https://lists.riseup.net/www/info/nuvolect)
Send emails to nuvolect@lists.riseup.net
