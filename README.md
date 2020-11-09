![logo](/assets/icon_small.png)
# SimpleEmail

Check [main](https://framagit.org/dystopia-project/simple-email/-/tree/main) branch for the last changes.

A [Free Software][free-software], minimalistic and privacy friendly email app for Android.

<a href="https://f-droid.org/packages/org.dystopia.email"><img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="75"></a>

Alternatively, you can download directly from GitLab [Releases][gitlab-releases].

## Highlights

* Easy navigation
* No unnecessary settings
* No bells and whistles
* Privacy friendly
* Simple design

## Why?

The focus of SimpleEmail is be a privacy-friendly email app with a good UX/UI and with a community development model.

SimpleEmail is minimalistic by design, so you can concentrate on reading and writing messages, it starts a foreground
service with a low priority status bar notification to make sure you'll never miss a new email.

*This project has been forked from [FairEmail](https://framagit.org/dystopia-project/simple-email/blob/master/docs/FAQ.md#what-is-the-difference-between-simpleemail-and-fairemail).*

## Features

* 100% [Free Software][free-software]
* Multiple accounts (inboxes)
* Multiple identities (outboxes)
* Unified inbox
* Conversation view
* Two way synchronization
* Offline storage and operations
* Battery friendly
* Low data usage
* Folder management
* Signatures
* Dark theme
* Account colors
* Search on server
* Encryption/decryption
* Export settings

## Screenshots

[<img src="metadata/en-US/images/phoneScreenshots/05_screenshot.png" width=160>](metadata/en-US/images/phoneScreenshots/05_screenshot.png)
[<img src="metadata/en-US/images/phoneScreenshots/08_screenshot.png" width=160>](metadata/en-US/images/phoneScreenshots/08_screenshot.png)

Please see more [here](/metadata/en-US/images/phoneScreenshots/) or in [FDroid store][fdroid]

## Secure

* Allow encrypted connections only
* Accept valid security certificates only
* Authentication required
* Safe message view (styling, scripting and unsafe HTML removed)
* No special permissions required
* No advertisements
* No analytics and no tracking

## Efficient

* [IMAP IDLE](https://en.wikipedia.org/wiki/IMAP_IDLE) (push messages) supported
* Built with latest development tools and libraries
* Android 6 Marshmallow or later required

## Downloads

* [FDroid store][fdroid]
* [GitLab][gitlab-releases]

Direct download certificate fingerprints:
```
MD5:  85:B2:A9:90:3D:21:25:9E:4F:43:D1:71:8C:29:6C:70
SHA1: 7F:AB:59:CD:12:A1:11:E7:6B:12:9D:71:70:5E:21:76:D9:0E:59:C0
SHA256: A7:A9:0A:5F:14:ED:00:57:56:49:9A:53:4A:13:1A:F0:64:0A:C4:DF:62:2F:76:35:F6:51:69:D8:C9:E9:19:F2
```

## Compatibility

SimpleEmail requires at least Android 6 Marshmallow.

## Frequently asked questions

See [here][faqs] for a list of often asked questions.

## Support

* For support on SimpleEmail, please open a [issue][]
* For support on authorizing accounts you should contact your provider.

## Contributing

Whether you have ideas, translations, features you think are missing, design or code changes, help is always welcome!.
You can submit [pull requests][pull-requests] to this repository, or submit translations using [Weblate](https://hosted.weblate.org/projects/simple-email/).

To get started, take a look at [CONTRIBUTING.md](https://framagit.org/dystopia-project/simple-email/blob/develop/CONTRIBUTING.md)

## Attribution

SimpleEmail uses:

* [JavaMail](https://javaee.github.io/javamail/). under [GPLv2+CE license](https://javaee.github.io/javamail/JavaMail-License).
* [jsoup](https://jsoup.org/). under [MIT license](https://jsoup.org/license).
* [JCharset](http://www.freeutils.net/source/jcharset/). under [GNU General Public License](http://www.freeutils.net/source/jcharset/#license)
* [Android Support Library](https://developer.android.com/tools/support-library/). under [Apache license](https://android.googlesource.com/platform/frameworks/support/+/master/LICENSE.txt).
* [Android Architecture Components](https://developer.android.com/topic/libraries/architecture/). under [Apache license](https://github.com/googlesamples/android-architecture-components/blob/master/LICENSE).
* [colorpicker](https://android.googlesource.com/platform/frameworks/opt/colorpicker). under [Apache license](https://android.googlesource.com/platform/frameworks/opt/colorpicker/+/master/src/com/android/colorpicker/ColorPickerDialog.java).
* [dnsjava](http://www.xbill.org/dnsjava/). under [BSD License](https://sourceforge.net/p/dnsjava/code/HEAD/tree/trunk/LICENSE).
* [OpenPGP API library](https://github.com/open-keychain/openpgp-api). under [Apache License 2.0](https://github.com/open-keychain/openpgp-api/blob/master/LICENSE).

## License

[![GNU General Public License version 3](https://www.gnu.org/graphics/gplv3-127x51.png)](https://framagit.org/dystopia-project/simple-email/blob/master/LICENSE)

    Copyright 2018, Marcel Bokhorst (M66B)
    Copyright 2018, Distopico (dystopia project) and contributors

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program. If not, see <http://www.gnu.org/licenses/>.

* All assets and docs are under [CC BY-SA 4.0](http://creativecommons.org/licenses/by-sa/4.0/),
for the license of the image used in the [assets](/assets) please see the metadata under the SVG file.

 [free-software]: https://www.gnu.org/philosophy/free-sw.html
 [issue]: https://framagit.org/dystopia-project/simple-email/issues
 [pull-requests]: https://framagit.org/dystopia-project/simple-email/merge_requests
 [faqs]: https://framagit.org/dystopia-project/simple-email/blob/master/docs/FAQ.md
 [gitlab-releases]: https://framagit.org/dystopia-project/simple-email/tags
 [fdroid]: https://f-droid.org/packages/org.dystopia.email
