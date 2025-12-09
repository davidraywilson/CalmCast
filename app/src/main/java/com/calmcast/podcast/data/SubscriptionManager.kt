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
        private const val KEY_SUBSCRIPTION_IDS = "subscription_ids"
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

    suspend fun getSubscriptions(): List<Podcast> = withContext(Dispatchers.IO) {
        try {
            val subscriptionIds = getSubscriptionIds()
            subscriptionIds.mapNotNull { id ->
                val podcast = podcastDao.getPodcast(id)
                if (podcast == null) {
                    Log.w(TAG, "Podcast with id $id not found in database but exists in subscriptions")
                }
                podcast
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
    
    suspend fun getSubscribedPodcastIds(): List<String> = withContext(Dispatchers.IO) {
        try {
            getSubscriptionIds()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting subscription IDs", e)
            emptyList()
        }
    }
}