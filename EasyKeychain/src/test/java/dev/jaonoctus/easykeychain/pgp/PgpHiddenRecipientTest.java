package dev.jaonoctus.easykeychain.pgp;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.Security;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;

import org.bouncycastle.bcpg.BCPGInputStream;
import org.bouncycastle.bcpg.Packet;
import org.bouncycastle.bcpg.PacketTags;
import org.bouncycastle.bcpg.PublicKeyEncSessionPacket;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import dev.jaonoctus.easykeychain.KeychainTestRunner;
import dev.jaonoctus.easykeychain.daos.KeyWritableRepository;
import dev.jaonoctus.easykeychain.operations.results.DecryptVerifyResult;
import dev.jaonoctus.easykeychain.operations.results.PgpSignEncryptResult;
import dev.jaonoctus.easykeychain.service.input.CryptoInputParcel;
import dev.jaonoctus.easykeychain.service.input.RequiredInputParcel.RequiredInputType;
import dev.jaonoctus.easykeychain.support.KeyringTestingHelper;
import dev.jaonoctus.easykeychain.support.KeyringTestingHelper.RawPacket;
import dev.jaonoctus.easykeychain.util.InputData;
import dev.jaonoctus.easykeychain.util.Passphrase;


/**
 * Messages whose session key packets don't name the recipient key, as produced by gpg -R.
 */
@RunWith(KeychainTestRunner.class)
public class PgpHiddenRecipientTest {
    private static final String PLAINTEXT = "dies ist ein plaintext ☭";

    private static UncachedKeyRing ring1, ring2;
    private static final Passphrase PASSPHRASE_1 = new Passphrase("RsKrW^raOPcnQ=ZJr-pP");
    private static final Passphrase PASSPHRASE_2 = new Passphrase("x");

    @BeforeClass
    public static void setUpOnce() throws Exception {
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
        // the same keys as PgpEncryptDecryptTest: two ECDH rings with different passphrases
        ring1 = KeyringTestingHelper.readRingFromResource("/test-keys/encrypt_decrypt_key_1.sec");
        ring2 = KeyringTestingHelper.readRingFromResource("/test-keys/encrypt_decrypt_key_2.sec");
    }

    @Before
    public void setUp() {
        KeyWritableRepository repository = KeyWritableRepository.create(RuntimeEnvironment.getApplication());
        repository.saveSecretKeyRing(ring1);
        repository.saveSecretKeyRing(ring2);
    }

    @Test
    public void encrypt__sessionKeyPacketsCarryNoKeyId() throws Exception {
        Assert.assertEquals("with hidden recipients, the key id must be the wildcard",
                0L, firstSessionPacketKeyId(encrypt(ring1, true)));
        Assert.assertNotEquals("without, the key id must be there",
                0L, firstSessionPacketKeyId(encrypt(ring1, false)));
    }

    @Test
    public void decrypt__withCachedPassphrase() {
        // ring 2 is tried with the wrong passphrase along the way, and must not get in the way
        DecryptVerifyResult result = decrypt(encrypt(ring1, true), PASSPHRASE_1, null,
                CryptoInputParcel.createCryptoInputParcel());

        Assert.assertTrue("decryption must succeed", result.success());
        Assert.assertArrayEquals(PLAINTEXT.getBytes(), result.getOutputBytes());
    }

    @Test
    public void decrypt__withProvidedPassphrase() {
        DecryptVerifyResult result = decrypt(encrypt(ring2, true), null, null,
                CryptoInputParcel.createCryptoInputParcel(PASSPHRASE_2));

        Assert.assertTrue("decryption must succeed", result.success());
        Assert.assertArrayEquals(PLAINTEXT.getBytes(), result.getOutputBytes());
    }

    @Test
    public void decrypt__asksForPassphraseIfNoneIsAvailable() {
        DecryptVerifyResult result = decrypt(encrypt(ring1, true), null, null,
                CryptoInputParcel.createCryptoInputParcel());

        Assert.assertFalse(result.success());
        Assert.assertTrue("must ask for a passphrase", result.isPending());
        Assert.assertEquals(RequiredInputType.PASSPHRASE, result.getRequiredInputParcel().mType);
    }

    @Test
    public void decrypt__failsIfNoKeyFits() {
        // only ring 2 may be used, and its passphrase is known, but the message is for ring 1
        DecryptVerifyResult result = decrypt(encrypt(ring1, true), PASSPHRASE_2, ring2.getMasterKeyId(),
                CryptoInputParcel.createCryptoInputParcel());

        Assert.assertFalse("decryption must fail", result.success());
        Assert.assertFalse("there is nothing to ask for", result.isPending());
    }

    private static byte[] encrypt(UncachedKeyRing recipient, boolean hiddenRecipients) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream(PLAINTEXT.getBytes());

        PgpSignEncryptOperation op = new PgpSignEncryptOperation(RuntimeEnvironment.getApplication(),
                KeyWritableRepository.create(RuntimeEnvironment.getApplication()), null);

        PgpSignEncryptData.Builder pgpData = PgpSignEncryptData.builder();
        pgpData.setEncryptionMasterKeyIds(new long[] { recipient.getMasterKeyId() });
        pgpData.setHiddenRecipients(hiddenRecipients);
        pgpData.setSymmetricEncryptionAlgorithm(
                PgpSecurityConstants.OpenKeychainSymmetricKeyAlgorithmTags.AES_128);

        PgpSignEncryptResult result = op.execute(pgpData.build(),
                CryptoInputParcel.createCryptoInputParcel(new Date()), new InputData(in, in.available()), out);
        Assert.assertTrue("encryption must succeed", result.success());

        return out.toByteArray();
    }

    private static DecryptVerifyResult decrypt(byte[] ciphertext, final Passphrase cachedPassphrase,
            Long onlyAllowedMasterKeyId, CryptoInputParcel cryptoInput) {
        PgpDecryptVerifyOperation op = new PgpDecryptVerifyOperation(RuntimeEnvironment.getApplication(),
                KeyWritableRepository.create(RuntimeEnvironment.getApplication()), null) {
            @Override
            public Passphrase getCachedPassphrase(long masterKeyId, long subKeyId) {
                return cachedPassphrase;
            }
        };

        PgpDecryptVerifyInputParcel.Builder input = PgpDecryptVerifyInputParcel.builder()
                .setInputBytes(ciphertext);
        if (onlyAllowedMasterKeyId != null) {
            input.setAllowedKeyIds(Collections.singletonList(onlyAllowedMasterKeyId));
        }

        return op.execute(input.build(), cryptoInput);
    }

    private static long firstSessionPacketKeyId(byte[] ciphertext) throws Exception {
        Iterator<RawPacket> packets = KeyringTestingHelper.parseKeyring(ciphertext);
        Long keyId = null;
        while (packets.hasNext()) {
            RawPacket rawPacket = packets.next();
            if (rawPacket.tag != PacketTags.PUBLIC_KEY_ENC_SESSION) {
                continue;
            }
            Packet packet = new BCPGInputStream(new ByteArrayInputStream(rawPacket.buf)).readPacket();
            long packetKeyId = ((PublicKeyEncSessionPacket) packet).getKeyID();
            if (keyId == null) {
                keyId = packetKeyId;
            } else if (keyId == 0L) {
                Assert.assertEquals("every session key packet must hide its recipient", 0L, packetKeyId);
            }
        }
        Assert.assertNotNull("there must be a session key packet", keyId);
        return keyId;
    }
}
