package com.example.fluentui.ui.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Main Database configuration.
 * Includes a manual migration (1 to 2) to add the isDynamic column without data loss.
 */
@Database(
    entities = [TileEntity::class],
    version = 2,
    exportSchema = false
)
abstract class TileDatabase : RoomDatabase() {

    abstract fun tileDao(): TileDao

    companion object {
        @Volatile
        private var INSTANCE: TileDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tiles ADD COLUMN isDynamic INTEGER NOT NULL DEFAULT 1")
            }
        }

        fun getDatabase(context: Context): TileDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TileDatabase::class.java,
                    "fluent_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}