package com.calmcast.podcast.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add description column to episodes table with default empty string
            database.execSQL(
                "ALTER TABLE episodes ADD COLUMN description TEXT NOT NULL DEFAULT ''"
            )
            // Add description column to downloads table's embedded episode
            database.execSQL(
                "ALTER TABLE downloads ADD COLUMN episode_description TEXT NOT NULL DEFAULT ''"
            )
        }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Clear all playback positions to start fresh with new stable episode IDs
            // This ensures consistency between old unstable IDs and new stable IDs
            database.execSQL("DELETE FROM playback_position")
        }
    }
}
