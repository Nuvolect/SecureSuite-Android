# SecureSuite Android

SecureSuite is a secure app for your personal contacts, passwords and notes, and in the future, for your calendar, ToDo lists and files.  

SecureSuite has an embedded web server that serves the SecureSuite web app on your WiFi network. 
You can either access your personal information directly on Android or over https on your WiFi network.
The concept is to provide a similar workflow to cloud based apps, without the security risks of storing your private information
in an Internet cloud.
Your private information is always with you, encrypted in your pocket and nowhere else.

The current stable release is ~~on~~ NOT ON Google Play  
Google does not like the number monitoring feature. When receiving an incoming call, SecureSuite will find an associated contact and display the name of the caller. Seems like a useful thing to do, this is how Android works but apparently not for 3-party apps. If you are a curious type and kind of handy, your participation in finding a way is appreciated.  
<a href='https://play.google.com/store/apps/details?id=com.nuvolect.securesuite&pcampaignid=MKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1'>
<img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png' 
width="172" height="60" /></a>

[Sideload Android SecureSuite 1.5.6](https://securesuite.org/signed_apps/1.5.6/SecureSuite-release.apk)  

## Contributing Bug Reports
We use GitHub for bug tracking. Please search the existing issues for your bug and create a new one if the issue is not yet tracked.
<https://github.com/Nuvolect/SecureSuite-Android/issues>

## Discussions
Join the discussion, ask a question, and share what you have learned.  
<https://github.com/Nuvolect/SecureSuite-Android/discussions>

## Joining the Beta  

Want to live life on the bleeding edge and help out with testing?

You can subscribe to SecureSuite Android Beta releases here: <https://play.google.com/apps/testing/com.nuvolect.securesuite>.  

If you're interested in a life of peace and tranquility, stick with the standard releases.  

## Contributing Code  

Instructions on how to setup your development environment and build SecureSuite can be found in 
[BUILDING.md](/BUILDING.md).

If you're new to the SecureSuite codebase, we recommend going through our issues and picking out a simple bug to fix 
(check the "easy" label in our issues) in order to get yourself familiar. 
Also please have a look at the CONTRIBUTING.md, that might answer some of your questions.

For larger changes and feature ideas, we ask that you propose it on the unofficial Community Forum for a high-level 
discussion with the wider community before implementation.

Note the test doc: [TEST_README](SecureSuite/src/androidTest/java/com/nuvolect/securesuite/TEST_README.md#instrumented-tests).  
Example vCards can be found here: SecureSuite/src/androidTest/test_vcard

# Help  

## Support  
For troubleshooting and support information, please visit the wiki.
<https://github.com/teamnuvolect/securesuite/wiki>

For general questions and discussion about features, visit the forum.
<a href="http://nuvolect.freeforums.net/board/3/discussion-securesuite">
<img src="https://securesuite.org/img/forum_join_chat.png"  height="50" width="134"></a> 

# Legal things  

## Cryptography Notice  
This distribution includes cryptographic software. The country in which you currently reside may have restrictions on the import, 
possession, use, and/or re-export to another country, of encryption software. BEFORE using any encryption software, 
please check your country's laws, regulations and policies concerning the import, possession, or use, and re-export of 
encryption software, to see if this is permitted. See [http://www.wassenaar.org/](http://www.wassenaar.org/) for more information.

The U.S. Government Department of Commerce, Bureau of Industry and Security (BIS), has classified this software as 
Export Commodity Control Number (ECCN) 5D002.C.1, which includes information security software using or performing 
cryptographic functions with asymmetric algorithms. The form and manner of this distribution makes it eligible for 
export under the License Exception ENC Technology Software Unrestricted (TSU) exception 
(see the BIS Export Administration Regulations, Section 740.13) for both object code and source code.

## License 

Copyright 2016-2019 Nuvolect LLC

Licensed under the GPLv3: <http://www.gnu.org/licenses/gpl-3.0.html>
