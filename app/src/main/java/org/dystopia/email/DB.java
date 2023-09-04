package org.dystopia.email;

/*
  This file is part of FairEmail.

  FairEmail is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  FairEmail is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with FairEmail.  If not, see <http://www.gnu.org/licenses/>.

  Copyright 2018, Marcel Bokhorst (M66B)
*/

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverter;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import javax.mail.Address;
import javax.mail.internet.InternetAddress;

// https://developer.android.com/topic/libraries/architecture/room.html

@Database(
    version = 26,
    entities = {
        EntityIdentity.class,
        EntityAccount.class,
        EntityFolder.class,
        EntityMessage.class,
        EntityAttachment.class,
        EntityOperation.class,
        EntityAnswer.class,
        EntityLog.class
    })
@TypeConverters({DB.Converters.class})
public abstract class DB extends RoomDatabase {
    public abstract DaoIdentity identity();

    public abstract DaoAccount account();

    public abstract DaoFolder folder();

    public abstract DaoMessage message();

    public abstract DaoAttachment attachment();

    public abstract DaoOperation operation();

    public abstract DaoAnswer answer();

    public abstract DaoLog log();

    private static DB sInstance;

    private static final String DB_NAME = "email";

    public static synchronized DB getInstance(Context context) {
        if (sInstance == null) {
            sInstance =
                migrate(
                    Room.databaseBuilder(context.getApplicationContext(), DB.class, DB_NAME)
                        .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING));

            Log.i(
                Helper.TAG,
                "sqlite version=" + exec(sInstance, "SELECT sqlite_version() AS sqlite_version"));
            Log.i(Helper.TAG, "sqlite sync=" + exec(sInstance, "PRAGMA synchronous"));
            Log.i(Helper.TAG, "sqlite journal=" + exec(sInstance, "PRAGMA journal_mode"));
        }

        return sInstance;
    }

    static String exec(DB db, String command) {
        Cursor cursor = null;
        try {
            cursor = db.query(command, new Object[0]);
            if (cursor != null && cursor.moveToNext()) {
                return cursor.getString(0);
            } else {
                return null;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private static void logMigration(int startVersion, int endVersion) {
        Log.i(Helper.TAG, "DB migration from version " + startVersion + " to " + endVersion);
    }

    private static DB migrate(RoomDatabase.Builder<DB> builder) {
        return builder
            .addCallback(
                new Callback() {
                    @Override
                    public void onOpen(SupportSQLiteDatabase db) {
                        Log.i(Helper.TAG, "Database version=" + db.getVersion());
                        super.onOpen(db);
                    }
                })
            .addMigrations(
                new Migration(1, 2) {
                    @Override
                    public void migrate(SupportSQLiteDatabase db) {
                        logMigration(startVersion, endVersion);
                        db.execSQL(
                            "ALTER TABLE `account` ADD COLUMN `poll_interval` INTEGER NOT NULL DEFAULT 9");
                    }
                })
            .addMigrations(
                new Migration(2, 3) {
                    @Override
                    public void migrate(SupportSQLiteDatabase db) {
                        logMigration(startVersion, endVersion);
                        db.execSQL(
                            "ALTER TABLE `identity` ADD COLUMN `store_sent` INTEGER NOT NULL DEFAULT 0");
                    }
                })
            .addMigrations(
                new Migration(3, 4) {
                    @Override
                    public void migrate(SupportSQLiteDatabase db) {
                        logMigration(startVersion, endVersion);
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS `answer` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `name` TEXT NOT NULL, `text` TEXT NOT NULL)");
                    }
                })
            .addMigrations(
                new Migration(4, 5) {
                    @Override
                    public void migrate(SupportSQLiteDatabase db) {
                        logMigration(startVersion, endVersion);
                        db.execSQL(
                            "ALTER TABLE `account` ADD COLUMN `auth_type` INTEGER NOT NULL DEFAULT 1");
                        db.execSQL(
                            "ALTER TABLE `identity` ADD COLUMN `auth_type` INTEGER NOT NULL DEFAULT 1");
                    }
                })
            .addMigrations(
                new Migration(5, 6) {
                    @Override
                    public void migrate(SupportSQLiteDatabase db) {
                        logMigration(startVersion, endVersion);
                        db.execSQL(
                            "ALTER TABLE `message` ADD COLUMN `ui_found` INTEGER NOT NULL DEFAULT 0");
                    }
                })
            .addMigrations(
                new Migration(6, 7) {
                    @Override
                    public void migrate(SupportSQLiteDatabase db) {
                        logMigration(startVersion, endVersion);
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS `log` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `time` INTEGER NOT NULL, `data` TEXT NOT NULL)");
                        db.execSQL("CREATE  INDEX `index_log_time` ON `log` (`time`)");
                    }
                })
            .addMigrations(
                new Migration(7, 8) {
                    @Override
                    public void migrate(SupportSQLiteDatabase db) {
                        logMigration(startVersion, endVersion);
                        db.execSQL("CREATE  INDEX `index_message_ui_found` ON `message` (`ui_found`)");
                    }
                })
            .addMigrations(
                new Migration(8, 9) {
                    @Override
                    public void migrate(SupportSQLiteDatabase db) {
                        logMigration(startVersion, endVersion);
                        db.execSQL("ALTER TABLE `message` ADD COLUMN `headers` TEXT");
                    }
                })
            .addMigrations(
                new Migration(9, 10) {
                    @Override
                    public void migrate(SupportSQLiteDatabase db) {
                        logMigration(startVersion, endVersion);
                        db.execSQL("ALTER TABLE `folder` ADD COLUMN `unified` INTEGER NOT NULL DEFAULT 0");
                        db.execSQL("CREATE  INDEX `index_folder_unified` ON `folder` (`unified`)");
                        db.execSQL(
                            "UPDATE `folder` SET unified = 1 WHERE type = '" + EntityFolder.INBOX + "'");
                    }
                })
            .addMigrations(
                new Migration(10, 11) {
                    @Override
                    public void migrate(SupportSQLiteDatabase db) {
                        logMigration(startVersion, endVersion);
                        db.execSQL("ALTER TABLE `account` ADD COLUMN `signature` TEXT");
                    }
                })
            .addMigrations(
                new Migration(11, 12) {
                    @Override
                    public void migrate(SupportSQLiteDatabase db) {
                        logMigration(startVersion, endVersion);
                        db.execSQL("ALTER TABLE `message` ADD COLUMN `flagged` INTEGER NOT NULL DEFAULT 0");
                        db.execSQL(
                            "ALTER TABLE `message` ADD COLUMN `ui_flagged` INTEGER NOT NULL DEFAULT 0");
                    }
                })
            .addMigrations(
                new Migration(12, 13) {
                    @Override
                    public void migrate(SupportSQLiteDatabase db) {
                        logMigration(startVersion, endVersion);
                        db.execSQL("ALTER TABLE `message` ADD COLUMN `avatar` TEXT");
                    }
                })
            .addMigrations(
                new Migration(13, 14) {
                    @Override
                    public void migrate(SupportSQLiteDatabase db) {
                        logMigration(startVersion, endVersion);
                        db.execSQL("ALTER TABLE `account` ADD COLUMN `color` INTEGER");
                    }
                })
            .addMigrations(
                new Migration(14, 15) {
                    @Override
                    public void migrate(SupportSQLiteDatabase db) {
                        logMigration(startVersion, endVersion);
                        db.execSQL("ALTER TABLE `attachment` ADD COLUMN `cid` TEXT");
                        db.execSQL(
                            "CREATE UNIQUE INDEX `index_attachment_message_cid` ON `attachment` (`message`, `cid`)");
                    }
                })
            .addMigrations(
                new Migration(15, 16) {
                    @Override
                    public void migrate(SupportSQLiteDatabase db) {
                        logMigration(startVersion, endVersion);
                        db.execSQL("ALTER TABLE `message` ADD COLUMN `size` INTEGER");
                        db.execSQL("ALTER TABLE `message` ADD COLUMN `content` INTEGER NOT NULL DEFAULT 1");
                    }
                })
            .addMigrations(
                new Migration(16, 17) {
                    @Override
                    public void migrate(SupportSQLiteDatabase db) {
                        logMigration(startVersion, endVersion);
                        db.execSQL("ALTER TABLE `message` ADD COLUMN `deliveredto` TEXT");
                    }
                })
            .addMigrations(
                new Migration(17, 18) {
                    @Override
                    public void migrate(SupportSQLiteDatabase db) {
                        logMigration(startVersion, endVersion);
                        db.execSQL("ALTER TABLE `folder` ADD COLUMN `display` TEXT");
                    }
                })
            .addMigrations(
                new Migration(18, 19) {
                    @Override
                    public void migrate(SupportSQLiteDatabase db) {
                        logMigration(startVersion, endVersion);
                        db.execSQL("ALTER TABLE `folder` ADD COLUMN `hide` INTEGER NOT NULL DEFAULT 0");
                    }
                })
            .addMigrations(
                new Migration(19, 20) {
                    @Override
                    public void migrate(SupportSQLiteDatabase db) {
                        logMigration(startVersion, endVersion);
                        db.execSQL("ALTER TABLE `folder` ADD COLUMN `poll_interval` INTEGER");
                    }
                })
            .addMigrations(
                new Migration(20, 21) {
                    @Override
                    public void migrate(SupportSQLiteDatabase db) {
                        logMigration(startVersion, endVersion);
                        db.execSQL(
                            "ALTER TABLE `message` ADD COLUMN `ui_ignored` INTEGER NOT NULL DEFAULT 0");
                        db.execSQL("CREATE INDEX `index_message_ui_ignored` ON `message` (`ui_ignored`)");
                    }
                })
            .addMigrations(
                new Migration(21, 22) {
                    @Override
                    public void migrate(SupportSQLiteDatabase db) {
                        logMigration(startVersion, endVersion);
                        db.execSQL("DROP INDEX `index_message_folder_uid`");
                        db.execSQL(
                            "CREATE UNIQUE INDEX `index_message_folder_uid_ui_found` ON `message` (`folder`, `uid`, `ui_found`)");
                        db.execSQL("DROP INDEX `index_message_msgid_folder`");
                        db.execSQL(
                            "CREATE UNIQUE INDEX `index_message_msgid_folder_ui_found` ON `message` (`msgid`, `folder`, `ui_found`)");
                    }
                })
            .addMigrations(
                new Migration(22, 23) {
                    @Override
                    public void migrate(SupportSQLiteDatabase db) {
                        logMigration(startVersion, endVersion);
                        db.execSQL(
                            "ALTER TABLE `account` ADD COLUMN `starttls` INTEGER NOT NULL DEFAULT 0");
                        db.execSQL(
                            "ALTER TABLE `account` ADD COLUMN `insecure` INTEGER NOT NULL DEFAULT 0");
                        db.execSQL(
                            "ALTER TABLE `identity` ADD COLUMN `insecure` INTEGER NOT NULL DEFAULT 0");
                    }
                })
            .addMigrations(
                new Migration(23, 24) {
                    @Override
                    public void migrate(SupportSQLiteDatabase db) {
                        logMigration(startVersion, endVersion);
                        db.execSQL("ALTER TABLE `message` ADD COLUMN `account_name` TEXT");
                    }
                })
            .addMigrations(
                new Migration(24, 25) {
                    @Override
                    public void migrate(SupportSQLiteDatabase db) {
                        logMigration(startVersion, endVersion);
                        db.execSQL("ALTER TABLE `folder` ADD COLUMN `sync_state` TEXT");
                    }
                })
            .addMigrations(new Migration(25, 26) {
                @Override
                public void migrate(@NonNull SupportSQLiteDatabase db) {
                    logMigration(startVersion, endVersion);
                    db.execSQL("DROP INDEX `index_operation_folder`");
                    db.execSQL("DROP INDEX `index_operation_message`");
                    db.execSQL("DROP TABLE `operation`");
                    db.execSQL("CREATE TABLE `operation`" +
                        " (`id` INTEGER PRIMARY KEY AUTOINCREMENT" +
                        ", `folder` INTEGER NOT NULL" +
                        ", `message` INTEGER" +
                        ", `name` TEXT NOT NULL" +
                        ", `args` TEXT NOT NULL" +
                        ", `created` INTEGER NOT NULL" +
                        ", FOREIGN KEY(`folder`) REFERENCES `folder`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE" +
                        ", FOREIGN KEY(`message`) REFERENCES `message`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)");
                    db.execSQL("CREATE INDEX `index_operation_folder` ON `operation` (`folder`)");
                    db.execSQL("CREATE INDEX `index_operation_message` ON `operation` (`message`)");
                }
            })
            .build();
    }

    public static class Converters {
        @TypeConverter
        public static String[] fromStringArray(String value) {
            return value.split(",");
        }

        @TypeConverter
        public static String toStringArray(String[] value) {
            return TextUtils.join(",", value);
        }

        @TypeConverter
        public static String encodeAddresses(Address[] addresses) {
            if (addresses == null) {
                return null;
            }
            JSONArray jaddresses = new JSONArray();
            if (addresses != null) {
                for (Address address : addresses) {
                    try {
                        if (address instanceof InternetAddress) {
                            String a = ((InternetAddress) address).getAddress();
                            String p = ((InternetAddress) address).getPersonal();
                            JSONObject jaddress = new JSONObject();
                            if (a != null) {
                                jaddress.put("address", a);
                            }
                            if (p != null) {
                                jaddress.put("personal", p);
                            }
                            jaddresses.put(jaddress);
                        } else {
                            JSONObject jaddress = new JSONObject();
                            jaddress.put("address", address.toString());
                            jaddresses.put(jaddress);
                        }
                    } catch (JSONException ex) {
                        Log.e(Helper.TAG, ex + "\n" + Log.getStackTraceString(ex));
                    }
                }
            }
            return jaddresses.toString();
        }

        @TypeConverter
        public static Address[] decodeAddresses(String json) {
            if (json == null) {
                return null;
            }
            List<Address> result = new ArrayList<>();
            try {
                JSONArray jaddresses = new JSONArray(json);
                for (int i = 0; i < jaddresses.length(); i++) {
                    JSONObject jaddress = (JSONObject) jaddresses.get(i);
                    if (jaddress.has("personal")) {
                        result.add(
                            new InternetAddress(jaddress.getString("address"), jaddress.getString("personal")));
                    } else {
                        result.add(new InternetAddress(jaddress.getString("address")));
                    }
                }
            } catch (Throwable ex) {
                // Compose can store invalid addresses
                Log.w(Helper.TAG, ex + "\n" + Log.getStackTraceString(ex));
            }
            return result.toArray(new Address[0]);
        }
    }
}
