package com.example.fluentui.ui.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TileEntity::class, FolderEntity::class], // Ajout de FolderEntity
    version = 3, // Passage à la version 3
    exportSchema = false
)
@TypeConverters(Converters::class) // Activation des convertisseurs Gson
abstract class TileDatabase : RoomDatabase() {

    abstract fun tileDao(): TileDao
    abstract fun folderDao(): FolderDao // Accès au nouveau DAO

    companion object {
        @Volatile
        private var INSTANCE: TileDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tiles ADD COLUMN isDynamic INTEGER NOT NULL DEFAULT 1")
            }
        }

        // Nouvelle migration pour ajouter la table sans perdre les tuiles existantes
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `drawer_folders` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `apps` TEXT NOT NULL, PRIMARY KEY(`id`))")
            }
        }

        fun getDatabase(context: Context): TileDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TileDatabase::class.java,
                    "fluent_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}