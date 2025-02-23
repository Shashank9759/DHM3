package com.example.dhm30.Data.Database


import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.dhm30.Data.Entities.ActivityLog
import com.example.dhm30.Data.DAOs.ActivityLogDao
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ActivityLog::class],  version = 2 )
abstract class AppDatabase : RoomDatabase() {
    abstract fun activityLogDao(): ActivityLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "activity_log_db"
                )
                    .addMigrations(MIGRATION_1_2) // Register the migration
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create a new table without the 'transitionType' column
        database.execSQL(
            """
            CREATE TABLE activity_logs_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                activityType TEXT NOT NULL,
                timestamp TEXT NOT NULL
            )
            """
        )

        // Copy data from the old table to the new table
        database.execSQL(
            """
            INSERT INTO activity_logs_new (id, activityType, timestamp)
            SELECT id, activityType, timestamp
            FROM activity_logs
            """
        )

        // Drop the old table
        database.execSQL("DROP TABLE activity_logs")

        // Rename the new table to match the old table's name
        database.execSQL("ALTER TABLE activity_logs_new RENAME TO activity_logs")
    }
}
