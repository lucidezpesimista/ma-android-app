package com.ma.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ma.app.data.model.Node

@Database(
    entities = [Node::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun nodeDao(): NodeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Migración 1->2: Añade campos de tarea/evento al modelo Node.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE nodes ADD COLUMN nodeType TEXT NOT NULL DEFAULT 'NOTE'")
                db.execSQL("ALTER TABLE nodes ADD COLUMN isCompleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE nodes ADD COLUMN dueDate INTEGER")
                db.execSQL("ALTER TABLE nodes ADD COLUMN priority TEXT NOT NULL DEFAULT 'NONE'")
                db.execSQL("ALTER TABLE nodes ADD COLUMN completedAt INTEGER")

                // Índices nuevos
                db.execSQL("CREATE INDEX IF NOT EXISTS index_nodes_dueDate ON nodes(dueDate)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_nodes_isCompleted ON nodes(isCompleted)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_nodes_nodeType ON nodes(nodeType)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ma_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration() // Solo si falla la migración
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
