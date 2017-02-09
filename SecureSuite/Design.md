# SecureSuite High Level Design

The app is the combination of a fairly standard Android app and a web app that shares an encrypted 
database and common set of utilities. Long running tasks are executed in either the WorkerService 
or in the WebService.  The application database and WebService is started each time the Android
device boots.

## Activity and fragment design

The Android app is built on fragments. At anyone point in time there is a single activity running
with at least 1 fragment. A second fragment will be running if the device has a large enough screen.
Fragments are also used for dialogs and other UI elements.

The original design borrows the design from an early version of Google Contacts. There is a
contacts view and a groups view, each has its own activity and one or the other is running
but never both. The contacts list view is the default startup activity.

## Execution flow for menu commands with permissions support

## Web app design

## Data synchronization and RESTFul services

## Password generation and management

## User authentication and YubiKey Neo

## SQLCipher Database

## WorkerService and WebService

## Long running tasks and internal messaging

_handleMessage( Message msg )
-> 


