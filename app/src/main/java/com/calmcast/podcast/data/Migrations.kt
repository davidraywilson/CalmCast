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

    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add newEpisodeCount column to podcasts table with default value of 0
            database.execSQL(
                "ALTER TABLE podcasts ADD COLUMN newEpisodeCount INTEGER NOT NULL DEFAULT 0"
            )
        }
    }

    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add lastViewedAt column to podcasts table with default value of 0 (epoch millis)
            database.execSQL(
                "ALTER TABLE podcasts ADD COLUMN lastViewedAt INTEGER NOT NULL DEFAULT 0"
            )
        }
    }

    val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add publishDateMillis column to episodes table for fast sorting
            database.execSQL(
                "ALTER TABLE episodes ADD COLUMN publishDateMillis INTEGER NOT NULL DEFAULT 0"
            )
            // Add episode_publishDateMillis column to downloads table's embedded episode
            database.execSQL(
                "ALTER TABLE downloads ADD COLUMN episode_publishDateMillis INTEGER NOT NULL DEFAULT 0"
            )
        }
    }

    val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Drop episodes table; episodes will no longer be cached in the database
            database.execSQL("DROP TABLE IF EXISTS episodes")
            
            // Recreate podcasts table without episodeCount and newEpisodeCount columns
            // SQLite doesn't support dropping columns, so we need to recreate the table
            database.execSQL(
                "CREATE TABLE podcasts_new (id TEXT PRIMARY KEY NOT NULL, title TEXT NOT NULL, author TEXT NOT NULL, description TEXT NOT NULL, imageUrl TEXT, feedUrl TEXT, lastSeenEpisodeId TEXT, lastViewedAt INTEGER NOT NULL)"
            )
            database.execSQL(
                "INSERT INTO podcasts_new (id, title, author, description, imageUrl, feedUrl, lastViewedAt) SELECT id, title, author, description, imageUrl, feedUrl, lastViewedAt FROM podcasts"
            )
            database.execSQL("DROP TABLE podcasts")
            database.execSQL("ALTER TABLE podcasts_new RENAME TO podcasts")
        }
    }
}
