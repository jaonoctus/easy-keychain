package dev.jaonoctus.easykeychain.pgp;


import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


/**
 * Marker obfuscation exists to stop a message looking like PGP, so what it emits matters as much
 * as whether it round trips. Both are pinned here.
 */
public class PgpMarkerObfuscatorTest {
    private static final String MARKER = PgpMarkerObfuscator.MARKER;

    private static final String LINE_1 = "wV4DTmkMQQGcaxcSAQdANo1t8BWzhOFza9jwZQIc7uS59MO5E4gmVyK5laDT0w7T";
    private static final String LINE_2 = "gjnZ6sV1Qccg1SHgYRLREAHraEeRmWVwelavItS5HvTJ2LRU6NFrZV6wlkaQ";
    private static final String CHECKSUM = "=x9Yz";

    /** What Bouncy Castle produces: headers, a blank line, the body, a checksum. */
    private static final String ARMORED = "-----BEGIN PGP MESSAGE-----\n"
            + "Version: BCPG v1.86\n"
            + "Comment: something\n"
            + "\n"
            + LINE_1 + "\n"
            + LINE_2 + "\n"
            + CHECKSUM + "\n"
            + "-----END PGP MESSAGE-----\n";

    private static final String NORMALIZED = "-----BEGIN PGP MESSAGE-----\n"
            + "\n"
            + LINE_1 + "\n"
            + LINE_2 + "\n"
            + CHECKSUM + "\n"
            + "-----END PGP MESSAGE-----\n";

    @Test
    public void obfuscate__isSingleLineBetweenTwoMarkers() {
        String obfuscated = PgpMarkerObfuscator.obfuscate(ARMORED);

        assertEquals(MARKER + LINE_1 + LINE_2 + CHECKSUM + MARKER, obfuscated);
    }

    @Test
    public void obfuscate__leavesNoTraceOfArmor() {
        String obfuscated = PgpMarkerObfuscator.obfuscate(ARMORED);

        assertFalse(obfuscated.contains("-----"));
        assertFalse(obfuscated.contains("PGP"));
        assertFalse(obfuscated.contains("Version"));
        assertFalse(obfuscated.contains("Comment"));
        assertFalse(obfuscated.contains("\n"));
        // a doubled marker would be a more distinctive signature than a single one
        assertFalse(obfuscated.contains(MARKER + MARKER));
    }

    @Test
    public void obfuscate__handlesWindowsLineEndings() {
        String obfuscated = PgpMarkerObfuscator.obfuscate(ARMORED.replace("\n", "\r\n"));

        assertEquals(MARKER + LINE_1 + LINE_2 + CHECKSUM + MARKER, obfuscated);
    }

    @Test
    public void obfuscate__leavesCleartextSignatureAlone() {
        String signed = "-----BEGIN PGP SIGNED MESSAGE-----\nHash: SHA256\n\nhello\n"
                + "-----BEGIN PGP SIGNATURE-----\n\nabcd\n=abcd\n-----END PGP SIGNATURE-----\n";

        assertEquals(signed, PgpMarkerObfuscator.obfuscate(signed));
    }

    @Test
    public void obfuscate__leavesMultipleBlocksAlone() {
        String twoBlocks = ARMORED + ARMORED;

        assertEquals(twoBlocks, PgpMarkerObfuscator.obfuscate(twoBlocks));
    }

    @Test
    public void obfuscate__leavesPlainTextAlone() {
        assertEquals("hello world", PgpMarkerObfuscator.obfuscate("hello world"));
    }

    @Test
    public void deobfuscate__roundTrip() {
        String obfuscated = PgpMarkerObfuscator.obfuscate(ARMORED);

        assertEquals(NORMALIZED, PgpMarkerObfuscator.deobfuscate(obfuscated));
    }

    @Test
    public void deobfuscate__findsMessageInSurroundingText() {
        String obfuscated = PgpMarkerObfuscator.obfuscate(ARMORED);

        assertEquals(NORMALIZED, PgpMarkerObfuscator.deobfuscate("Alice: " + obfuscated + " (sent 10:42)"));
    }

    @Test
    public void deobfuscate__toleratesInsertedWhitespace() {
        String wrapped = MARKER + "\n" + LINE_1 + " \n" + LINE_2 + "\r\n" + CHECKSUM + "\n" + MARKER;

        assertEquals(NORMALIZED, PgpMarkerObfuscator.deobfuscate(wrapped));
    }

    @Test
    public void deobfuscate__paddingIsNotMistakenForChecksum() {
        // no checksum, and the body ends in base64 padding
        String body = "wV4DTmkMQQGcaxcSAQdANo1t8BWzhOFza9jwZQIc7uS59MO5E4gmVyK5laDTQQ==";

        String armored = PgpMarkerObfuscator.deobfuscate(MARKER + body + MARKER);

        assertEquals("-----BEGIN PGP MESSAGE-----\n\n" + body + "\n-----END PGP MESSAGE-----\n", armored);
    }

    @Test
    public void deobfuscate__paddingFollowedByChecksum() {
        String body = "wV4DTmkMQQGcaxcSAQdANo1t8BWzhOFza9jwZQIc7uS59MO5E4gmVyK5laDTQQ==";

        String armored = PgpMarkerObfuscator.deobfuscate(MARKER + body + CHECKSUM + MARKER);

        assertEquals("-----BEGIN PGP MESSAGE-----\n\n" + body + "\n" + CHECKSUM
                + "\n-----END PGP MESSAGE-----\n", armored);
    }

    @Test
    public void deobfuscate__rejectsOtherInput() {
        assertNull(PgpMarkerObfuscator.deobfuscate("hello world"));
        assertNull(PgpMarkerObfuscator.deobfuscate(ARMORED));
        // a single marker, as left by a notification that cut the message short
        assertNull(PgpMarkerObfuscator.deobfuscate(MARKER + LINE_1));
        assertNull(PgpMarkerObfuscator.deobfuscate(MARKER + MARKER));
        assertNull(PgpMarkerObfuscator.deobfuscate(MARKER + "not base64!" + MARKER));
    }

    @Test
    public void isObfuscated() {
        assertTrue(PgpMarkerObfuscator.isObfuscated(PgpMarkerObfuscator.obfuscate(ARMORED)));
        assertFalse(PgpMarkerObfuscator.isObfuscated(ARMORED));
        assertFalse(PgpMarkerObfuscator.isObfuscated("hello world"));
    }

    @Test
    public void pgpHelper__readsObfuscatedMessages() {
        String obfuscated = PgpMarkerObfuscator.obfuscate(ARMORED);

        assertEquals(NORMALIZED, PgpHelper.getPgpMessageContent(obfuscated));
        assertTrue(PgpHelper.containsPgpMessage(obfuscated));
        assertTrue(PgpHelper.containsPgpMessage(ARMORED));
        assertFalse(PgpHelper.containsPgpMessage("hello world"));
    }
}
