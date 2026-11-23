[//]: # (NOTE: Please put every sentence in its own line, Transifex puts every line in its own translation field!)

## 1.0.0
First release of EasyKeychain.

EasyKeychain is a fork of [OpenKeychain](https://www.openkeychain.org).
It was forked from OpenKeychain 6.0.4, at commit [8d0bd1f85](https://github.com/open-keychain/open-keychain/commit/8d0bd1f8537fb3a795fda8161f730a313ef7b01c) (2024-05-16).
This changelog only lists what changed in EasyKeychain since then.
For everything before that, see the [changelog of OpenKeychain](https://github.com/open-keychain/open-keychain/blob/8d0bd1f8537fb3a795fda8161f730a313ef7b01c/OpenKeychain/src/main/res/raw/help_changelog.md).

  * Renamed to EasyKeychain with its own application id, so it can be installed next to OpenKeychain
  * Version numbers start again at 1.0.0
  * Update Bouncy Castle to 1.86
  * New option to hide recipients when encrypting, and support for decrypting such messages
  * New option to obfuscate the PGP markers of encrypted text
  * New sign-only (clearsign) option for text
  * Keys can be given a local nickname
  * Experimental: offer to decrypt PGP messages seen in notifications
  * keyserver.ubuntu.com is the default keyserver, keys.openpgp.org stays as the second one
  * Requires Android 7.0 or newer
  * Updates for Android 16, including edge-to-edge layouts
  * No longer asks for access to photos, videos and audio
  * Asks for the notification permission once after setup
  * Fix theme switching to light when opening the experimental features
