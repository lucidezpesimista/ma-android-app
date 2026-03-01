package com.ma.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ma.app.data.model.Node

@Database(
    entities = [Node::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun nodeDao(): NodeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ma_database"
                )
                    .fallbackToDestructiveMigration() // Para MVP, migraciones destructivas están OK
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
