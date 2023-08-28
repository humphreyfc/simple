# SimpleEmail

If you have a question, please check the frequently asked questions below first.
At the bottom you can find how to ask other questions, request features and report bugs.

## Frequently Asked Questions

#### What is the difference between SimpleEmail and FairEmail?

Right now there big differences, after the forked, the two apps have an
independent development and different paths and the focus of SimpleEmail is be a
simple, privacy-friendly app with a good UX/UI and minimalistic design.

* Many "pro" features on [FairEmail](https://github.com/M66B/open-source-email) are open in SimpleEmail
* The APK size is smaller, SimpleEmail is 3.x MB vs 15.x MB of FairEmail, even bigger than K-9 Mail
* SimpleEmail has a community focus, all contributions are welcome
* SimpleEmail will not have "pro" features
* F-Droid friendly, no redirect to play store (potentially violating your
  privacy) or background fetch updates
* Notifications group per account
* The UI was cleaned and simplified
* SimpleEmail will keep simple, without unnecessaries settings (FairEmail added
  a lot settings in the last versions)
* FairEmial require purchased basic things with no transparent way to purchase
  that features

please see the [CHANGELOG](https://framagit.org/dystopia-project/simple-email/blob/master/CHANGELOG.md) for more detail about the changes.

#### Which permissions are needed and why?

* have full network access (INTERNET): to send and receive email
* view network connections (ACCESS_NETWORK_STATE): to monitor internet
  connectivity changes
* allow show notifications (POST_NOTIFICATIONS): to show new message
  notifications on Android 13 and later
* run at startup (RECEIVE_BOOT_COMPLETED): to start monitoring on device start
* foreground service (FOREGROUND_SERVICE/_DATA_SYNC): to run a foreground service on
  Android 9 Pie and later, see also the next question
* prevent device from sleeping (WAKE_LOCK): to keep the device awake while
  synchronizing messages
* Optional: read your contacts (READ_CONTACTS): to autocomplete addresses and to
  show photos
* Optional: find accounts on the device (GET_ACCOUNTS): to use
  [OAuth](https://en.wikipedia.org/wiki/OAuth) instead of passwords

#### Why is there a permanent notification shown?

A permanent status bar notification with the number of accounts being
synchronized and the number of operations pending is shown to prevent Android
from killing the service that takes care of receiving and sending email.

Most, if not all, other email apps don't show a notification with the "side
effect" that new email is often not or late being reported.

Background: this is because of the introduction of [doze mode](https://developer.android.com/training/monitoring-device-state/doze-standby) in Android 6 Marshmallow.

#### What are operations and why are they pending?

The low priority status bar notification shows the number of pending operations, which can be:

* add: add message to remote folder
* move: move message to another remote folder
* delete: delete message from remote folder
* send: send message
* seen: mark message as seen/unseen in remote folder
* flag: star/unstar remote message
* headers: download message headers
* body: download message text
* attachment: download attachment

Operations are processed only when there is a connection to the email server or
when manually synchronizing.  See also [this FAQ](#FAQ16).

#### What is a valid security certificate?

Valid security certificates are officially signed (not self signed) and have matching a host name.

#### What does 'no IDLE support' mean?

Without [IMAP IDLE](https://en.wikipedia.org/wiki/IMAP_IDLE) emails need to be periodically fetched.

#### How can I login to Gmail / G suite?

Since January 2020 Gmail restricted the oAuth scopes, now requires authorization
to be able to authenticate, we are looking the best way to fix oAuth
authentication without privative libraries.

As a temporary solution you need to allow normal authentication (non-oAuth) or
how Google says "less secure apps".  See here for instructions:
[https://support.google.com/accounts/answer/6010255](https://support.google.com/accounts/answer/6010255)

#### Why are messages in the outbox not moved to the sent folder?

Messages in the outbox are moved to the sent folder as soon as your provider
adds the message to the sent folder.  If this doesn't happen, your provider
might not keep track of sent messages or you might be using an SMTP server not
related to the provider. In these cases you can enable the account option
*Store sent messages* to let the app move messages from the outbox to the sent
folder after sending. There is a menu to move sent messages to the sent folder
after enabling this option.

#### Can I use a Microsoft Exchange account?

You can use a Microsoft Exchange account if it is accessible via IMAP.
ActiveSync is not supported at this moment.
See here for more information: [https://support.office.com/en-us/article/what-is-a-microsoft-exchange-account-47f000aa-c2bf-48ac-9bc2-83e5c6036793](https://support.office.com/en-us/article/what-is-a-microsoft-exchange-account-47f000aa-c2bf-48ac-9bc2-83e5c6036793)

#### What are identities?

Identities represent email addresses you are sending *from*.

Some providers allow you to have multiple email aliases. You can configure
these by setting the email address field to the alias address and setting the
user name field to your main email address.

#### What does 'UIDPLUS not supported' mean?

The error message *UIDPLUS not supported* means that your email provider does
not provide the IMAP [UIDPLUS extension](https://tools.ietf.org/html/rfc4315).
This IMAP extension is required to implement two way synchronization, which is
not an optional feature.  So, unless your provider can enable this extension,
you cannot use SimpleEmail for this provider.

#### ~~Why is STARTTLS for IMAP not supported?~~

~~STARTTLS starts with a not encrypted connection and is therefore not secure.~~
~~All well known IMAP servers support IMAP with a plain SSL connection, so there is no need to support STARTTLS for IMAP.~~
~~If you encounter an IMAP server that requires STARTTLS, please let me know.~~

~~For more background information, please see [this article](https://www.eff.org/nl/deeplinks/2018/06/announcing-starttls-everywhere-securing-hop-hop-email-delivery).~~

~~tl;dr; "*Additionally, even if you configure STARTTLS perfectly and use a valid certificate, there’s still no guarantee your communication will be encrypted.*"~~

#### How does encryption/decryption work?

First of all you need to install and configure
[OpenKeychain](https://f-droid.org/en/packages/org.sufficientlysecure.keychain/).
To encrypt a message before sending, just select the menu *Encrypt*. Similarly,
to decrypt a received message, just select the menu *Decrypt*.

#### How does search on server work?

You can start searching for messages on sender, recipient, subject or message text by using the magnify glass in the action bar of a folder (not in the unified inbox because it could be a collection of folders).
The server executes the search. Scrolling down will fetch more messages from the server.
Searching by the server might be case sensitive or case insensitive and might be on partial text or whole words, depending on the provider.
Search on server is a pro feature.

#### How can I setup Outlook with 2FA?

To use Outlook with two factor authentication enabled, you need to create an app password.
See [here](https://support.microsoft.com/en-us/help/12409/microsoft-account-app-passwords-two-step-verification) for the details.

#### Can you add ... ?

* More themes/black theme: the goal is to keep the app as simple as possible, so
  no more themes will not be added.
* Previewing message text in notification/widget: this is not always possible
  because the message text is initially not downloaded for larger messages.
* Executing filter rules: filter rules should be executed on the server because
  a battery powered device with possibly an unstable internet connection is not
  suitable for this.

#### Why are messages not being synchronized?

Possible causes of messages not being synchronized (sent or received) are:

* The account or folder(s) are not set to synchronize
* The number of days to synchronize is set to low
* There is no usable internet connection
* The email server is temporarily not available
* Android stopped the synchronization service

So, check your account and folder settings and check if the accounts/folders are
connected (see the legend menu for the meaning of the icons).

On some devices, where there are lots of applications competing for memory,
Android may stop the synchronization service as a last resort. Some Android
versions, in particular of Huawei (see
[here](https://www.forbes.com/sites/bensin/2016/07/04/push-notifications-not-coming-through-to-your-huawei-phone-heres-how-to-fix-it/)
for a fix) or Xiaomi (see
[here](https://www.forbes.com/sites/bensin/2016/11/17/how-to-fix-push-notifications-on-xiaomis-miui-8-for-real/)
for a fix) stop apps and services too aggressively.

#### Why does manual synchronize not work?

If the *Synchronize now* menu is dimmed, there is no connection to the account.

See the previous question for more information.

#### How do I enable the notification light?

Before Android 8 Oreo: there is an advanced option in the setup for this.

Android 8 Oreo and later: see [here](https://developer.android.com/training/notify-user/channels) about how to configure notification channels.

#### Why do I get 'Couldn't connect to host' ?

The message *Couldn't connect to host ...* means that SimpleEmail was not able to connect to the email server.

Possible causes are:

* A firewall is blocking connections to the server
* The email server is refusing to accept the connection
* The host name or port number is invalid
* The are problems with the internet connection

If you are using a VPN, the VPN provider might block the connection because it is too aggressively trying to prevent spam.

#### Why do I get 'Too many simultaneous connections' ?

The message *Too many simultaneous connections* is sent by the email server when there are too many connections to the same email account at the same time.

Possible causes are:

* There are multiple email clients connected to the same account
* The same email client is connected multiple times to the same account
* The previous connection was terminated abruptly for example by losing internet connectivity

#### What is browse messages on the server?

Browse messages on the server will fetch messages from the email server in real
time when you reach the end of the list of synchronized messages, even when the
folder is set to not synchronize. You can disable this feature under *Setup* >
*Advanced options* > *Browse messages on the server*.

#### Why can't I select an image, attachment or a file to export/import?

If a menu item to select a file is disabled (dimmed),
likely the [storage access framework](https://developer.android.com/guide/topics/providers/document-provider),
a standard Android component, is not present,
for example because your custom ROM does not include it or because it was removed.
SimpleEmail does not request storage permissions, so this framework is required to select files and folders.
No app, except maybe file managers, targetting Android 4.4 KitKat or later should ask for storage permissions because it would allow access to *all* files.

#### Can I help to translate SimpleEmail in my own language?

Yes, you can translate the texts of SimpleEmail in your own language, please open a [pull requests][pull-requests] with the changes,
we are looking for web service to automated the process.

<br>

If you have another question, want to request a feature or report a bug, please open a [issue][], send a [email](distopico@riseup.net) or open [PR][pull-requests].

 [issue]: https://framagit.org/dystopia-project/simple-email/issues
 [pull-requests]: https://framagit.org/dystopia-project/simple-email/merge_requests
