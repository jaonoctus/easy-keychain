package dev.jaonoctus.easykeychain.daos;


import androidx.sqlite.db.SupportSQLiteDatabase;
import dev.jaonoctus.easykeychain.Database;
import dev.jaonoctus.easykeychain.KeychainDatabase;


class AbstractDao {
    private final KeychainDatabase db;
    private final DatabaseNotifyManager databaseNotifyManager;

    AbstractDao(KeychainDatabase db, DatabaseNotifyManager databaseNotifyManager) {
        this.db = db;
        this.databaseNotifyManager = databaseNotifyManager;
    }

    Database getDatabase() {
        return db.getSqlDelightDatabase();
    }

    SupportSQLiteDatabase getWritableDb() {
        return db.getWritableDatabase();
    }

    DatabaseNotifyManager getDatabaseNotifyManager() {
        return databaseNotifyManager;
    }
}
