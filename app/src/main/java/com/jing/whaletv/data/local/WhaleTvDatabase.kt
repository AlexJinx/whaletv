package com.jing.whaletv.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ChannelEntity::class,
        StreamEntity::class,
        ProgramEntity::class,
        SyncStateEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class WhaleTvDatabase : RoomDatabase() {
    abstract fun channelDao(): ChannelDao
    abstract fun programDao(): ProgramDao
    abstract fun syncStateDao(): SyncStateDao

    companion object {
        @Volatile private var instance: WhaleTvDatabase? = null

        fun get(context: Context): WhaleTvDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    WhaleTvDatabase::class.java,
                    "whale-tv.db",
                )
                    .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
