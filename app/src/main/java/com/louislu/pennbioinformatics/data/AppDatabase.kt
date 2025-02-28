package com.louislu.pennbioinformatics.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.louislu.pennbioinformatics.data.entry.local.DataEntryDao
import com.louislu.pennbioinformatics.data.entry.local.DataEntryEntity
import com.louislu.pennbioinformatics.data.session.local.SessionDao
import com.louislu.pennbioinformatics.data.session.local.SessionEntity


@Database(
    entities = [SessionEntity::class, DataEntryEntity::class],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun dataEntryDao(): DataEntryDao
}