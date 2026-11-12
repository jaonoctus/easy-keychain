# Bouncy Castle OpenPGP module, patched

This directory holds the sources of the OpenPGP module (`pg`) of
[Bouncy Castle](https://www.bouncycastle.org), with a set of patches that the
app depends on. Only this module is vendored. The provider itself comes from
Maven Central, as `org.bouncycastle:bcprov-jdk18on` and `bcutil-jdk18on`.

- **Upstream:** https://github.com/bcgit/bc-java, tag `r1rv86` (release 1.86).
- **Contents:** `pg/src/main/java` of that tag, with the patches applied.
  The module's tests and its variants for other Java versions are not included.
- **Build:** `pg/build.gradle` is our own. The module is built standalone as
  the Gradle project `:bcpg`, see `settings.gradle`.
- **License:** see `LICENSE.md`, taken unchanged from upstream.

## The patches

`openkeychain-patches.diff` is the complete difference between upstream's
`r1rv86` and what is in `pg/`. It is the patch set OpenKeychain maintained in
its fork (https://github.com/open-keychain/bouncycastle, branch `ok-rv77`),
ported to 1.86, plus two changes made during the port:

- GNU dummy and divert-to-card secret keys, with the card serial stored in
  the IV
- opaque public keys for unknown algorithms and unknown ECC curves
- literal data length helpers
- the armor reader tolerating redundant CRs, and no `Version:` armor header
  by default
- an `IOException` instead of an unchecked exception for unsupported
  signature versions
- access to the raw encrypted session key, and user attribute helpers
- new: `PGPSignatureGenerator.init()` tolerates a null private key, which is
  what signing on a security token passes
- new: a truncated divert-to-card serial number raises a
  `MalformedPacketException`

## Updating to a newer Bouncy Castle

1. Check out the new upstream tag of bc-java.
2. Apply `openkeychain-patches.diff` to it (`git apply -3`) and resolve the
   conflicts.
3. Replace `pg/src/main/java` here with the result, and regenerate
   `openkeychain-patches.diff` as the diff against the new tag.
4. Set the same version for `bcprov-jdk18on` and `bcutil-jdk18on` in
   `pg/build.gradle` and in `EasyKeychain/build.gradle`.
