package dev.jaonoctus.easykeychain.daos;


import android.content.Context;

import androidx.annotation.Nullable;
import dev.jaonoctus.easykeychain.KeyNicknamesQueries;
import dev.jaonoctus.easykeychain.KeychainDatabase;


/**
 * Nicknames the user gave to keys. They are local to this device and not part of the keys.
 */
public class KeyNicknameDao extends AbstractDao {
    public static final int MAX_LENGTH = 64;

    private final KeyNicknamesQueries queries = getDatabase().getKeyNicknamesQueries();

    public static KeyNicknameDao create(Context context) {
        KeychainDatabase database = KeychainDatabase.getInstance(context);
        DatabaseNotifyManager databaseNotifyManager = DatabaseNotifyManager.create(context);
        return new KeyNicknameDao(database, databaseNotifyManager);
    }

    private KeyNicknameDao(KeychainDatabase database, DatabaseNotifyManager databaseNotifyManager) {
        super(database, databaseNotifyManager);
    }

    @Nullable
    public String getNickname(long masterKeyId) {
        return queries.selectByMasterKeyId(masterKeyId).executeAsOneOrNull();
    }

    /**
     * Sets the nickname of a key. A null or blank nickname removes it.
     */
    public void setNickname(long masterKeyId, @Nullable String nickname) {
        String trimmed = nickname == null ? "" : nickname.trim();
        if (trimmed.length() > MAX_LENGTH) {
            trimmed = trimmed.substring(0, MAX_LENGTH).trim();
        }

        if (trimmed.isEmpty()) {
            queries.deleteByMasterKeyId(masterKeyId);
        } else {
            queries.replaceNickname(masterKeyId, trimmed);
        }
        getDatabaseNotifyManager().notifyKeyChange(masterKeyId);
    }
}
