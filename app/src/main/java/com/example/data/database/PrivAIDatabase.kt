package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        TranscriptionEntity::class,
        OCREntity::class,
        SummaryEntity::class,
        NoteEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PrivAIDatabase : RoomDatabase() {

    abstract fun transcriptionDao(): TranscriptionDao
    abstract fun ocrDao(): OCRDao
    abstract fun summaryDao(): SummaryDao
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: PrivAIDatabase? = null

        fun getDatabase(context: Context): PrivAIDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PrivAIDatabase::class.java,
                    "privai_secure_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
