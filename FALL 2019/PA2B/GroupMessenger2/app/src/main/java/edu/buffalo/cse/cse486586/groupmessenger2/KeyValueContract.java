package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

//https://developer.android.com/training/data-storage/sqlite.html
public final class KeyValueContract {
    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.

    private KeyValueContract() {
    }

    /* Inner class that defines the table contents */
    public static class KVEntry implements BaseColumns {
        public static final String TABLE_NAME = "keyAndValue";
        public static final String COLUMN_NAME_KEY = "key";
        public static final String COLUMN_NAME_VALUE = "value";
    }

    private static final String SQL_CREATE_ENTRIES =
            String.format("CREATE TABLE %s (%s STRING PRIMARY KEY,%s STRING)", KVEntry.TABLE_NAME, KVEntry.COLUMN_NAME_KEY, KVEntry.COLUMN_NAME_VALUE);

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + KVEntry.TABLE_NAME;

    public static class KeyValueDbHelper extends SQLiteOpenHelper {
        // If you change the database schema, you must increment the database version.
        public static final int DATABASE_VERSION=1;
        public static final String DATABASE_NAME = "KV.db";

        public KeyValueDbHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        public void onCreate(SQLiteDatabase db) {
            db.execSQL(SQL_CREATE_ENTRIES);
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // This database is only a cache for online data, so its upgrade policy is
            // to simply to discard the data and start over
            db.execSQL(SQL_DELETE_ENTRIES);
            onCreate(db);
        }

        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            onUpgrade(db, oldVersion, newVersion);
        }
    }

}
