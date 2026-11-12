package dev.jaonoctus.easykeychain.pgp;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.Security;
import java.util.Date;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import dev.jaonoctus.easykeychain.KeychainTestRunner;
import dev.jaonoctus.easykeychain.daos.KeyWritableRepository;
import dev.jaonoctus.easykeychain.operations.results.DecryptVerifyResult;
import dev.jaonoctus.easykeychain.operations.results.PgpSignEncryptResult;
import dev.jaonoctus.easykeychain.service.input.CryptoInputParcel;
import dev.jaonoctus.easykeychain.util.InputData;
import dev.jaonoctus.easykeychain.util.Passphrase;
import dev.jaonoctus.easykeychain.util.TestingUtils;


/**
 * A message this app encrypts must still decrypt after its armor markers were obfuscated and
 * restored, which is the path a message takes through "Encrypt text" and back in via share,
 * clipboard or a detected notification.
 */
@RunWith(KeychainTestRunner.class)
public class PgpMarkerObfuscatorRoundTripTest {
    private static final Passphrase PASSPHRASE = TestingUtils.testPassphrase0;

    @BeforeClass
    public static void setUpOnce() {
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
    }

    @Test
    public void encryptedMessage__decryptsAfterObfuscation() {
        String plaintext = "dies ist ein plaintext ☭";

        String armored = new String(encrypt(plaintext));
        Assert.assertTrue("encryption must produce an armored message",
                armored.startsWith("-----BEGIN PGP MESSAGE-----"));

        String obfuscated = PgpMarkerObfuscator.obfuscate(armored);
        Assert.assertNotEquals("the message must have been obfuscated", armored, obfuscated);
        Assert.assertFalse("no armor line may be left", obfuscated.contains("-----"));

        // what DecryptActivity does with text it is handed
        String restored = PgpHelper.getPgpMessageContent("some chat app: " + obfuscated);
        Assert.assertNotNull("the obfuscated message must be recognized", restored);

        Assert.assertEquals(plaintext, decrypt(restored.getBytes()));
    }

    private byte[] encrypt(String plaintext) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream(plaintext.getBytes());

        PgpSignEncryptOperation op = new PgpSignEncryptOperation(RuntimeEnvironment.getApplication(),
                KeyWritableRepository.create(RuntimeEnvironment.getApplication()), null);

        PgpSignEncryptData.Builder pgpData = PgpSignEncryptData.builder();
        pgpData.setSymmetricPassphrase(PASSPHRASE);
        pgpData.setSymmetricEncryptionAlgorithm(
                PgpSecurityConstants.OpenKeychainSymmetricKeyAlgorithmTags.AES_128);
        pgpData.setEnableAsciiArmorOutput(true);

        PgpSignEncryptResult result = op.execute(pgpData.build(),
                CryptoInputParcel.createCryptoInputParcel(new Date()), new InputData(in, in.available()), out);
        Assert.assertTrue("encryption must succeed", result.success());

        return out.toByteArray();
    }

    private String decrypt(byte[] ciphertext) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream(ciphertext);

        PgpDecryptVerifyOperation op = new PgpDecryptVerifyOperation(RuntimeEnvironment.getApplication(),
                KeyWritableRepository.create(RuntimeEnvironment.getApplication()), null);
        PgpDecryptVerifyInputParcel input = PgpDecryptVerifyInputParcel.builder()
                .setAllowSymmetricDecryption(true)
                .build();
        DecryptVerifyResult result = op.execute(
                input, CryptoInputParcel.createCryptoInputParcel(PASSPHRASE), new InputData(in, in.available()), out);
        Assert.assertTrue("decryption must succeed", result.success());

        return new String(out.toByteArray());
    }
}
