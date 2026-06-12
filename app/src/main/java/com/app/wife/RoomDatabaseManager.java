package com.wife.app;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {MessageEntity.class, CallEntity.class, FileEntity.class}, version = 1, exportSchema = false)
public abstract class RoomDatabaseManager extends RoomDatabase {
    private static volatile RoomDatabaseManager instance;

    public abstract MessageDao messageDao();
    public abstract CallDao callDao();
    public abstract FileDao fileDao();

    public static RoomDatabaseManager getInstance(Context context) {
        if (instance == null) {
            synchronized (RoomDatabaseManager.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                                    RoomDatabaseManager.class, "wife_database")
                            .fallbackToDestructiveMigration()
                            .allowMainThreadQueries()
                            .build();
                }
            }
        }
        return instance;
    }
}
