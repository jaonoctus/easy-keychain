package dev.jaonoctus.easykeychain.provider;


import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import dev.jaonoctus.easykeychain.KeychainTestRunner;
import dev.jaonoctus.easykeychain.daos.KeyNicknameDao;
import dev.jaonoctus.easykeychain.daos.KeyRepository;
import dev.jaonoctus.easykeychain.daos.KeyWritableRepository;
import dev.jaonoctus.easykeychain.model.UnifiedKeyInfo;
import dev.jaonoctus.easykeychain.pgp.UncachedKeyRing;
import dev.jaonoctus.easykeychain.support.KeyringTestingHelper;


@RunWith(KeychainTestRunner.class)
public class KeyNicknameDaoTest {
    private UncachedKeyRing testKeyring;
    private long masterKeyId;
    private KeyWritableRepository keyRepository;
    private KeyNicknameDao keyNicknameDao;

    @Before
    public void setUp() throws Exception {
        testKeyring = KeyringTestingHelper.readRingFromResource("/test-keys/authenticate_multisub_with_revoked.asc");
        masterKeyId = testKeyring.getMasterKeyId();

        keyRepository = KeyWritableRepository.create(RuntimeEnvironment.getApplication());
        keyRepository.saveSecretKeyRing(testKeyring);
        keyNicknameDao = KeyNicknameDao.create(RuntimeEnvironment.getApplication());
    }

    @Test
    public void noNickname__displayNameIsTheKeysName() {
        UnifiedKeyInfo keyInfo = keyRepository.getUnifiedKeyInfo(masterKeyId);

        Assert.assertNull(keyNicknameDao.getNickname(masterKeyId));
        Assert.assertNull(keyInfo.nickname());
        Assert.assertNotNull(keyInfo.name());
        Assert.assertEquals(keyInfo.name(), keyInfo.displayName());
    }

    @Test
    public void setNickname__showsUpInKeyInfo() {
        String name = keyRepository.getUnifiedKeyInfo(masterKeyId).name();

        keyNicknameDao.setNickname(masterKeyId, "  Work laptop ");

        UnifiedKeyInfo keyInfo = keyRepository.getUnifiedKeyInfo(masterKeyId);
        Assert.assertEquals("Work laptop", keyNicknameDao.getNickname(masterKeyId));
        Assert.assertEquals("Work laptop", keyInfo.nickname());
        Assert.assertEquals("Work laptop", keyInfo.displayName());
        Assert.assertEquals("the key's own name must be untouched", name, keyInfo.name());
        Assert.assertTrue("nickname must be searchable", keyInfo.uidSearchString().contains("work laptop"));
    }

    @Test
    public void setNickname__replacesTheOldOne() {
        keyNicknameDao.setNickname(masterKeyId, "first");
        keyNicknameDao.setNickname(masterKeyId, "second");

        Assert.assertEquals("second", keyRepository.getUnifiedKeyInfo(masterKeyId).nickname());
        Assert.assertEquals("a nickname must not duplicate the key in the list",
                1, keyRepository.getAllUnifiedKeyInfo().size());
    }

    @Test
    public void setNickname__blankRemovesIt() {
        keyNicknameDao.setNickname(masterKeyId, "something");
        keyNicknameDao.setNickname(masterKeyId, "   ");
        Assert.assertNull(keyRepository.getUnifiedKeyInfo(masterKeyId).nickname());

        keyNicknameDao.setNickname(masterKeyId, "something");
        keyNicknameDao.setNickname(masterKeyId, null);
        Assert.assertNull(keyRepository.getUnifiedKeyInfo(masterKeyId).nickname());
    }

    @Test
    public void setNickname__isCutToMaxLength() {
        StringBuilder tooLong = new StringBuilder();
        for (int i = 0; i < KeyNicknameDao.MAX_LENGTH + 20; i++) {
            tooLong.append('x');
        }

        keyNicknameDao.setNickname(masterKeyId, tooLong.toString());

        Assert.assertEquals(KeyNicknameDao.MAX_LENGTH, keyNicknameDao.getNickname(masterKeyId).length());
    }

    @Test
    public void nickname__survivesUpdatingTheKey() {
        keyNicknameDao.setNickname(masterKeyId, "Work laptop");

        // importing the key again rewrites all of its rows
        keyRepository.saveSecretKeyRing(testKeyring);

        Assert.assertEquals("Work laptop", keyRepository.getUnifiedKeyInfo(masterKeyId).nickname());
    }

    @Test
    public void nickname__isRemovedWithTheKey() {
        keyNicknameDao.setNickname(masterKeyId, "Work laptop");

        Assert.assertTrue(keyRepository.deleteKeyRing(masterKeyId));
        Assert.assertNull(keyNicknameDao.getNickname(masterKeyId));

        // and does not come back if the key is imported again
        keyRepository.saveSecretKeyRing(testKeyring);
        Assert.assertNull(keyRepository.getUnifiedKeyInfo(masterKeyId).nickname());
    }
}
