# Contributing to SecureSuite  

Thank you, we appreciate your help. Please note the conventions used in this app,
they might be a bit different than what you are used to.
Reading this document will save you and other contributors time.


## Development Ideology

Truths which we believe to be self-evident:

1. **The answer is not more options.**  If you feel compelled to add a preference that's exposed to 
the user, it's very possible you've made a wrong turn somewhere.
1. **The user doesn't know what a key is.**  We need to minimize the points at which a user is 
exposed to this sort of terminology.
1. **There are no power users.** The idea that some users "understand" concepts better than others 
has proven to be, for the most part, false. If anything, "power users" are more dangerous than the rest, 
and we should avoid exposing dangerous functionality to them.
1. **It's an asynchronous world.**  Be wary of anything that is anti-asynchronous: ACKs, protocol 
confirmations, or any protocol-level "advisory" message.
1. **There is no such thing as time.** Protocol ideas that require synchronized clocks are doomed to failure.


## Issues

### Useful bug reports
1. Please search both open and closed issues first to make sure your issue is not a duplicate.
1. Read the [Submitting useful bug reports guide](https://github.com/Nuvolect/SecureSuite-Android/wiki/Submitting-useful-bug-reports)<!-- @IGNORE PREVIOUS: link --> before posting a bug.

### Issue tracker is for bugs
The main purpose of this issue tracker is to track bugs for the Android client. 
Relevant, concise and to the point comments that help to solve the issue are very welcome.

##### Send support questions
Please do **not** ask support questions at the issue tracker. We want to help you using SecureSuite 
and we have created our support system just for that. You can reach support by sending email to 
support@nuvolect.com. You can also search for existing troubleshooting articles on the 
[Wiki](https://github.com/Nuvolect/SecureSuite-Android/wiki)<!-- @IGNORE PREVIOUS: link -->.

##### Not a discussion forum
Please do **not** use this issue tracker as a discussion forum. Discussion related to the bug in 
question should of course go to the issue itself. However other discussion should take place at 
the [community forum](https://nuvolect.freeforums.net/). You can use that forum to 
discuss any SecureSuite related topics or to just hang out with your fellow users.

### Don't bump issues
Every time someone comments on an issue, GitHub sends email to [everyone who is watching](https://github.com/Nuvolect/SecureSuite-Android/watchers). 
Thus bumping issues with :+1:s, _me toos_ or asking for updates generates a lot of unnecessary email 
notifications. Moreover bumping an issue does not help solve it. Please be respectful of everyone's 
time and only comment if you have relevant new information to add.

### Open issues

#### If it's open it's tracked
Have you followed all the points in the [Submitting useful bug reports guide](https://github.com/Nuvolect/SecureSuite-Android/wiki/Submitting-useful-bug-reports)<!-- @IGNORE PREVIOUS: link -->
but nobody has commented on your issue? Is there no milestone or person assigned to it? Don't worry, 
the developers read every issue and if it's open it means it's tracked and taken into account. 
It might just take time as other issues have higher priority. And remember that this is an open 
source project: Everyone is encouraged to take an active role in fixing open issues.

### Closed issues

#### "My issue was closed without giving a reason!"
Please understand that writing detailed explanations every time for every issue someone comes up 
with takes time. Sometimes a reason has been posted earlier to another related issue which you can 
search for. It's also possible that your issue was not in line with the guidelines of the project 
(see especially the [Development Ideology](https://github.com/Nuvolect/SecureSuite-Android/blob/master/CONTRIBUTING.md#development-ideology)), 
or it was decided that the issue is not something that SecureSuite should do at this time.


## Pull requests

### Sign the Contributor Licence Agreement (CLA)
You need to sign our CLA before your pull request can be merged. You can find it at: 
[https://nuvolect.com/cla.pdf](https://nuvolect.com/cla.pdf)

### Follow the Code Style Guidelines
Before submitting a pull request please check that your code adheres to the [Code Style Guidelines](https://github.com/Nuvolect/SecureSuite-Android/wiki/Code-Style-Guidelines)<!-- @IGNORE PREVIOUS: link -->.

### Submit only complete PRs and test them
Please do not submit pull requests that are still a work in progress. Pull requests should be ready 
for a merge when you submit them. Also please do not submit pull requests that you have not tested.

### Smaller is better
Please do not try to change too much at once. Big changes are less likely to be merged. If you are a 
first time contributor start with small and simple PRs to get to know the codebase.

### Merging can sometimes take a while
If your pull request follows all the advice above but still has not been merged it usually means the 
developers haven't simply had the time to review it yet. We understand that this might feel frustrating. 
We are sorry!


## How can I contribute?
Anyone can help by
- advising new people about the guidelines of this project
   - redirecting support questions to support@nuvolect.com
   - redirecting non-bug related discussions to the [community forum](https://nuvolect.freeforums.net)
- improving documentation in the [wiki](https://github.com/Nuvolect/SecureSuite-Android/wiki)<!-- @IGNORE PREVIOUS: link -->
- help write tests,[TEST_README](SecureSuite/src/androidTest/java/com/nuvolect/securesuite/TEST_README.md)
- finding and marking duplicate issues
- trying to reproduce issues
- finding solutions to open issues and posting relevant findings as comments
- submitting pull requests
- testing other people's pull requests
- spreading the joy of SecureSuite to your friends and family
- become a [Nuvolect patron](https://nuvolect.com/donate/) and donate money to help offset development costs

