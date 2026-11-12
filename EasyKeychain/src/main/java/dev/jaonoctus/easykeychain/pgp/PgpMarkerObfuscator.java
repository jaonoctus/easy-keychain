package dev.jaonoctus.easykeychain.pgp;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


/**
 * Rewrites an ASCII-armored PGP message so it no longer looks like one at a glance.
 * <p>
 * The BEGIN/END lines are replaced by {@link #MARKER}, the armor headers and all line breaks are
 * dropped, leaving a single line: <code>MARKER + base64 [+ "=" + crc] + MARKER</code>.
 * <p>
 * This is cosmetic. It hides the message from casual inspection and from naive filters that look
 * for the armor lines, it does not add any cryptographic protection, and anyone who knows the
 * scheme can undo it. The format is the one used by Easy PGP, so both apps read each other's
 * output.
 */
public class PgpMarkerObfuscator {
    public static final String MARKER = "00023CD1";

    private static final String ARMOR_BEGIN = "-----BEGIN PGP MESSAGE-----";
    private static final String ARMOR_END = "-----END PGP MESSAGE-----";
    private static final int ARMOR_LINE_LENGTH = 64;
    /** Length of the armor checksum, "=" followed by four base64 characters. */
    private static final int CHECKSUM_LENGTH = 5;

    // Greedy on purpose: the marker itself consists of base64 characters, so this runs up to the
    // last marker that is separated from the first one by nothing but base64 and whitespace.
    private static final Pattern OBFUSCATED_MESSAGE =
            Pattern.compile(MARKER + "([A-Za-z0-9+/=\\s]+)" + MARKER);

    /**
     * Obfuscates a single armored PGP MESSAGE block. Anything else, such as a cleartext signed
     * message or several concatenated blocks, is returned unchanged.
     */
    @NonNull
    public static String obfuscate(@NonNull String armored) {
        String text = armored.replace("\r\n", "\n").replace('\r', '\n').trim();
        if (!text.startsWith(ARMOR_BEGIN) || !text.endsWith(ARMOR_END)) {
            return armored;
        }

        String inner = text.substring(ARMOR_BEGIN.length(), text.length() - ARMOR_END.length());
        if (inner.contains("-----")) {
            return armored;
        }

        StringBuilder result = new StringBuilder(inner.length() + 2 * MARKER.length());
        result.append(MARKER);
        for (String line : inner.split("\n")) {
            line = line.trim();
            // Armor headers ("Version: ...", "Comment: ...") are the only lines with a colon, the
            // base64 alphabet has none. They identify the implementation and are not needed.
            if (line.length() == 0 || line.indexOf(':') >= 0) {
                continue;
            }
            result.append(line);
        }
        result.append(MARKER);

        return result.toString();
    }

    public static boolean isObfuscated(@NonNull CharSequence input) {
        return OBFUSCATED_MESSAGE.matcher(input).find();
    }

    /**
     * Restores the armored form of an obfuscated message found anywhere in the input.
     *
     * @return the armored message, or null if the input contains no obfuscated message
     */
    @Nullable
    public static String deobfuscate(@NonNull CharSequence input) {
        Matcher matcher = OBFUSCATED_MESSAGE.matcher(input);
        if (!matcher.find()) {
            return null;
        }

        String data = matcher.group(1).replaceAll("\\s+", "");
        if (data.length() == 0) {
            return null;
        }

        // The base64 body is padded to a multiple of four characters, so a trailing checksum is
        // present exactly if five characters are left over and they start with '='. Looking only
        // at the last '=' would mistake the padding of a message without checksum for one.
        String checksum = null;
        int length = data.length();
        if (length % 4 == 1 && length >= CHECKSUM_LENGTH && data.charAt(length - CHECKSUM_LENGTH) == '=') {
            checksum = data.substring(length - CHECKSUM_LENGTH);
            data = data.substring(0, length - CHECKSUM_LENGTH);
        }

        StringBuilder result = new StringBuilder(data.length() + data.length() / ARMOR_LINE_LENGTH + 80);
        result.append(ARMOR_BEGIN).append("\n\n");
        for (int i = 0; i < data.length(); i += ARMOR_LINE_LENGTH) {
            result.append(data, i, Math.min(i + ARMOR_LINE_LENGTH, data.length())).append('\n');
        }
        if (checksum != null) {
            result.append(checksum).append('\n');
        }
        result.append(ARMOR_END).append('\n');

        return result.toString();
    }
}
