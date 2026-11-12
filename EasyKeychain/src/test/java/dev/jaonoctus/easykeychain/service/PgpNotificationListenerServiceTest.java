package dev.jaonoctus.easykeychain.service;


import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import dev.jaonoctus.easykeychain.Constants;
import dev.jaonoctus.easykeychain.KeychainTestRunner;
import dev.jaonoctus.easykeychain.pgp.PgpMarkerObfuscator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


@RunWith(KeychainTestRunner.class)
public class PgpNotificationListenerServiceTest {
    private static final String ARMORED = "-----BEGIN PGP MESSAGE-----\n"
            + "\n"
            + "wV4DTmkMQQGcaxcSAQdANo1t8BWzhOFza9jwZQIc7uS59MO5E4gmVyK5laDT0w7T\n"
            + "=x9Yz\n"
            + "-----END PGP MESSAGE-----\n";

    @Test
    public void findPgpMessage__armored() {
        String found = PgpNotificationListenerService.findPgpMessage(
                Arrays.<CharSequence>asList("Alice", "hey, read this: " + ARMORED));

        assertEquals(ARMORED.trim(), found.trim());
    }

    @Test
    public void findPgpMessage__obfuscated() {
        String obfuscated = PgpMarkerObfuscator.obfuscate(ARMORED);

        String found = PgpNotificationListenerService.findPgpMessage(
                Collections.<CharSequence>singletonList(obfuscated));

        assertEquals(ARMORED, found);
    }

    @Test
    public void findPgpMessage__ignoresOrdinaryNotifications() {
        assertNull(PgpNotificationListenerService.findPgpMessage(
                Arrays.<CharSequence>asList("Alice", "see you at 8?", "3 new messages")));
        assertNull(PgpNotificationListenerService.findPgpMessage(Collections.<CharSequence>emptyList()));
    }

    @Test
    public void findPgpMessage__ignoresMessagesCutShort() {
        // notifications truncate long texts, and half a message can't be decrypted
        String truncated = ARMORED.substring(0, ARMORED.indexOf("=x9Yz"));
        assertNull(PgpNotificationListenerService.findPgpMessage(
                Collections.<CharSequence>singletonList(truncated)));

        String obfuscated = PgpMarkerObfuscator.obfuscate(ARMORED);
        String truncatedObfuscated = obfuscated.substring(0, obfuscated.length() - 10);
        assertNull(PgpNotificationListenerService.findPgpMessage(
                Collections.<CharSequence>singletonList(truncatedObfuscated)));
    }

    @Test
    public void notificationIdFor__stableAndInRange() {
        int id = PgpNotificationListenerService.notificationIdFor(ARMORED);

        assertEquals(id, PgpNotificationListenerService.notificationIdFor(ARMORED));
        assertNotEquals(id, PgpNotificationListenerService.notificationIdFor(ARMORED + "x"));
        assertInRange(id);
    }

    @Test
    public void notificationIdFor__negativeHashCodes() {
        // "polygenelubricants".hashCode() is Integer.MIN_VALUE, where Math.abs stays negative
        assertEquals(Integer.MIN_VALUE, "polygenelubricants".hashCode());
        assertInRange(PgpNotificationListenerService.notificationIdFor("polygenelubricants"));
    }

    private static void assertInRange(int id) {
        assertTrue(id >= Constants.NotificationIds.PGP_DETECTED_BASE);
        assertTrue(id < Constants.NotificationIds.PGP_DETECTED_BASE + Constants.NotificationIds.PGP_DETECTED_RANGE);
    }
}
