package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.model.UserSettings
import com.example.model.WaterLog

@Database(
    entities = [UserSettings::class, WaterLog::class],
    version = 2,
    exportSchema = false
)
abstract class HydroPulseDatabase : RoomDatabase() {
    abstract fun hydrationDao(): HydrationDao

    companion object {
        @Volatile
        private var INSTANCE: HydroPulseDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE user_settings ADD COLUMN textScale REAL NOT NULL DEFAULT 0.90"
                )
                db.execSQL(
                    "ALTER TABLE user_settings ADD COLUMN themeMode TEXT NOT NULL DEFAULT 'SYSTEM'"
                )
            }
        }

        fun getDatabase(context: Context): HydroPulseDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    HydroPulseDatabase::class.java,
                    "hydropulse_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
