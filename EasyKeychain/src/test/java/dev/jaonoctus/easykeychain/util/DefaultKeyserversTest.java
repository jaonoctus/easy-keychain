package dev.jaonoctus.easykeychain.util;


import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import dev.jaonoctus.easykeychain.KeychainTestRunner;
import dev.jaonoctus.easykeychain.keyimport.HkpKeyserverAddress;


@RunWith(KeychainTestRunner.class)
public class DefaultKeyserversTest {
    @Test
    public void defaults__ubuntuIsPreferredAndKeysOpenPgpOrgIsKept() throws Exception {
        Preferences preferences = Preferences.getPreferences(RuntimeEnvironment.getApplication());
        preferences.clear();

        List<HkpKeyserverAddress> keyservers = preferences.getKeyServers();

        Assert.assertEquals(2, keyservers.size());
        Assert.assertEquals("hkps://keyserver.ubuntu.com", keyservers.get(0).getUrl());
        Assert.assertNull(keyservers.get(0).getOnion());
        Assert.assertEquals("https://keyserver.ubuntu.com:443", keyservers.get(0).getUrlURI().toString());
        Assert.assertEquals("hkps://keys.openpgp.org", keyservers.get(1).getUrl());
        Assert.assertNotNull(keyservers.get(1).getOnion());

        Assert.assertEquals(keyservers.get(0), preferences.getPreferredKeyserver());
    }
}
