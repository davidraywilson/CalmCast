package com.calmcast.podcast.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SubscriptionManager(private val context: Context, private val podcastDao: PodcastDao) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val TAG = "SubscriptionManager"
        private const val PREFS_NAME = "calmcast_subscriptions"
        private const val KEY_SUBSCRIPTIONS = "subscriptions_list"
        private const val KEY_SUBSCRIPTION_IDS = "subscription_ids"
        private const val KEY_TADDY_MIGRATED = "taddy_migration_complete"
    }

    init {
        // Migrate from old format to new format on first load
        migrateFromOldFormat()
    }

    private fun migrateFromOldFormat() {
        // Handle migration from Podchaser
        if (!sharedPreferences.getBoolean(KEY_TADDY_MIGRATED, false)) {
            sharedPreferences.edit()
                .remove(KEY_SUBSCRIPTIONS)
                .putBoolean(KEY_TADDY_MIGRATED, true)
                .apply()
            return
        }

        // Migrate from old Podcast object format to ID-only format
        if (!sharedPreferences.getBoolean("subscription_ids_migrated", false)) {
            try {
                val oldJson = sharedPreferences.getString(KEY_SUBSCRIPTIONS, null)
                if (oldJson != null) {
                    val type = object : TypeToken<List<Podcast>>() {}.type
                    val podcasts = gson.fromJson<List<Podcast>?>(oldJson, type) ?: emptyList()
                    val ids = podcasts.map { it.id }
                    saveSubscriptionIds(ids)
                    Log.d(TAG, "Migrated ${ids.size} subscriptions from old format to ID-only format")
                }
                sharedPreferences.edit()
                    .remove(KEY_SUBSCRIPTIONS)
                    .putBoolean("subscription_ids_migrated", true)
                    .apply()
            } catch (e: Exception) {
                Log.e(TAG, "Error migrating subscriptions", e)
            }
        }
    }

    suspend fun addSubscription(podcast: Podcast): Boolean = withContext(Dispatchers.IO) {
        try {
            val subscriptionIds = getSubscriptionIds().toMutableList()

            // Avoid duplicates
            if (subscriptionIds.contains(podcast.id)) {
                return@withContext false
            }

            subscriptionIds.add(podcast.id)
            saveSubscriptionIds(subscriptionIds)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error adding subscription", e)
            false
        }
    }

    suspend fun removeSubscription(podcastId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val subscriptionIds = getSubscriptionIds().toMutableList()
            val initialSize = subscriptionIds.size
            subscriptionIds.remove(podcastId)
            val removed = subscriptionIds.size < initialSize

            if (removed) {
                saveSubscriptionIds(subscriptionIds)
            }
            removed
        } catch (e: Exception) {
            Log.e(TAG, "Error removing subscription", e)
            false
        }
    }

    private suspend fun getSubscriptionIds(): List<String> = withContext(Dispatchers.IO) {
        try {
            val json = sharedPreferences.getString(KEY_SUBSCRIPTION_IDS, null) ?: return@withContext emptyList()
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson<List<String>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error reading subscription IDs", e)
            emptyList()
        }
    }

    private fun saveSubscriptionIds(ids: List<String>) {
        try {
            val json = gson.toJson(ids)
            sharedPreferences.edit().putString(KEY_SUBSCRIPTION_IDS, json).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving subscription IDs", e)
        }
    }

    suspend fun getSubscriptions(): List<PodcastWithEpisodes> = withContext(Dispatchers.IO) {
        try {
            val subscriptionIds = getSubscriptionIds()
            subscriptionIds.mapNotNull { id ->
                val podcastWithEpisodes = podcastDao.getPodcastWithEpisodes(id)
                if (podcastWithEpisodes == null) {
                    Log.w(TAG, "Podcast with id $id not found in database but exists in subscriptions")
                }
                podcastWithEpisodes
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting subscriptions", e)
            emptyList()
        }
    }

    suspend fun isSubscribed(podcastId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val subscriptionIds = getSubscriptionIds()
            subscriptionIds.contains(podcastId)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking subscription status", e)
            false
        }
    }

    suspend fun getPodcastById(podcastId: String): PodcastWithEpisodes? = withContext(Dispatchers.IO) {
        try {
            val subscriptionIds = getSubscriptionIds()
            if (subscriptionIds.contains(podcastId)) {
                podcastDao.getPodcastWithEpisodes(podcastId)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting podcast by ID", e)
            null
        }
    }

    suspend fun clearAllSubscriptions(): Boolean = withContext(Dispatchers.IO) {
        try {
            sharedPreferences.edit()
                .remove(KEY_SUBSCRIPTION_IDS)
                .remove(KEY_SUBSCRIPTIONS)  // Also remove old format if it exists
                .apply()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing subscriptions", e)
            false
        }
    }
}