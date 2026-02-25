package com.iptvpro.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.iptvpro.app.data.db.dao.*
import com.iptvpro.app.data.db.entity.*

@Database(
    entities = [
        PlaylistEntity::class,
        GroupEntity::class,
        ChannelEntity::class,
        FavoriteEntity::class,
        HistoryEntity::class,
        EpgChannelEntity::class,
        EpgProgramEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun groupDao(): GroupDao
    abstract fun channelDao(): ChannelDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun historyDao(): HistoryDao
    abstract fun epgDao(): EpgDao
}
