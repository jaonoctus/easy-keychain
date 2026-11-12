# EasyKeychain privacy policy

Last updated: 12 November 2026

EasyKeychain is an OpenPGP app for Android. It is open source; this policy
describes what the app in this repository does with your data.

## In short

- EasyKeychain has no accounts, no advertising, no analytics and no trackers.
- The developer runs no servers for it and receives no data from the app.
- Your keys and everything else the app stores stay on your device.
- The app contacts third-party servers only for the key lookup, key update and
  key publishing features described below.

## Data stored on your device

- Your OpenPGP keys, public and secret, and the public keys of other people
  that you import.
- Information derived from those keys, such as names, email addresses and
  fingerprints, and the nicknames you give to keys.
- Your settings, and the list of apps you allowed to use your keys.
- Passphrases you enter, kept in memory for the time you chose and then
  discarded.
- Temporary files created while encrypting or decrypting.

This data is kept in the app's private storage. It is excluded from Android's
cloud backup. Uninstalling the app or clearing its data removes it.

## Data sent over the network

EasyKeychain contacts servers operated by third parties in these cases. Each
of them sees your IP address and handles requests under its own policy.

- **Searching for keys.** What you search for (a name, email address, key ID
  or fingerprint) is sent to the keyservers configured in the settings. By
  default these are `keyserver.ubuntu.com` (Canonical) and `keys.openpgp.org`.
  With Web Key Directory lookups enabled, which is the default, a search for
  an email address also contacts the domain of that address.
- **Automatic key updates.** This is on by default and can be turned off in
  the settings. Every three days the app asks the preferred keyserver for
  updates of the keys in your key list, which reveals the fingerprints of
  those keys to it. By default this only happens on unmetered networks.
- **Publishing a key.** Only when you ask for it, your public key is uploaded
  to a keyserver. This makes the key public, including the names and email
  addresses in it. Keys published on `keyserver.ubuntu.com` cannot be removed
  again.
- **Importing a key from a link.** If you import a key from a web address,
  for example a Facebook profile, the app downloads it from that address.

You can route these connections through Tor or another proxy in the settings.

## Permissions and what they are used for

- **Internet and network state:** the features in the previous section.
- **Camera:** scanning QR codes of keys and fingerprints. Images are not
  stored or sent.
- **NFC and USB:** talking to security tokens such as YubiKeys.
- **Photos, videos, audio and files:** reading the files you choose to
  encrypt, decrypt or import.
- **Notifications:** showing the app's own notifications.
- **Run at startup and in the background:** scheduling the automatic key
  updates.

## Notification access (optional)

"Detect encrypted messages in notifications" is off by default. If you turn it
on and grant notification access in the system settings, the app can read the
text of all notifications on your device. It looks for PGP messages in them,
on the device and in memory only. Notification contents are not stored, not
logged and not sent anywhere. Turning the option off stops this.

## Clipboard

The app reads the clipboard when you use a clipboard feature, and when its
encrypt/decrypt screen opens, to point out a PGP message that is in it.
Clipboard contents are not stored or sent.

## Other apps

Other apps can ask EasyKeychain to encrypt, decrypt, sign or verify through
the OpenPGP API. Each app has to be allowed by you first, and you can revoke
that in the settings. What those apps do with the results is up to them.

## Logs

Operation logs are kept on the device. They leave it only if you export or
share them yourself. They can contain key IDs and file names.

## Google Play

If you install the app from Google Play, Google processes data about the
installation under its own privacy policy. EasyKeychain itself contains no
Google Play services and no Google analytics.

## Children

EasyKeychain is not directed at children and does not knowingly collect data
from anyone.

## Changes

Changes to this policy are made in this file. Its history is public in this
repository.

## Contact

Questions about this policy can be asked by opening an issue at
https://github.com/jaonoctus/easy-keychain/issues
