package dev.diskettefox.madridtransport

import android.content.Context
import android.text.TextUtils
import androidx.core.content.edit

class FavoritesManager {
    companion object {
        private const val PREFS_NAME = "FavoritesPrefs"
        private const val KEY_FAVORITES = "favorite_stops_ordered"
        private const val KEY_FAVORITE_NAMES = "favorite_names"

        @JvmStatic
        fun addFavorite(context: Context, stopId: String) {
            val favorites = getFavorites(context).toMutableList()
            val id = stopId.toIntOrNull() ?: return
            if (!favorites.contains(id)) {
                favorites.add(id)
                saveFavorites(context, favorites)
            }
        }

        @JvmStatic
        fun removeFavorite(context: Context, stopId: String) {
            val favorites = getFavorites(context).toMutableList()
            val id = stopId.toIntOrNull() ?: return
            favorites.remove(id)
            saveFavorites(context, favorites)
        }

        @JvmStatic
        fun addFavoriteWithName(context: Context, stopId: String, customName: String) {
            addFavorite(context, stopId)

            val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            sharedPref.edit { putString(KEY_FAVORITE_NAMES + stopId, customName) }
        }

        @JvmStatic
        fun isFavorite(context: Context, stopId: String): Boolean {
            return try {
                val id = stopId.toInt()
                getFavorites(context).contains(id)
            } catch (_: NumberFormatException) {
                false
            }
        }

        @JvmStatic
        fun saveFavorites(context: Context, favoriteIds: List<Int>) {
            val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val stringIds = favoriteIds.map { it.toString() }
            sharedPref.edit { putString(KEY_FAVORITES, TextUtils.join(",", stringIds)) }
        }

        @JvmStatic
        fun getFavorites(context: Context): List<Int> {
            val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val favoritesString = sharedPref.getString(KEY_FAVORITES, "") ?: ""

            if (favoritesString.isEmpty()) return emptyList()

            val favoriteIds = mutableListOf<Int>()
            for (id in favoritesString.split(",")) {
                id.toIntOrNull()?.let { favoriteIds.add(it) }
            }
            return favoriteIds
        }

        @JvmStatic
        fun getFavoriteName(context: Context, stopId: String): String? {
            val pref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return pref.getString(KEY_FAVORITE_NAMES + stopId, null)
        }

        @JvmStatic
        fun clearFavorites(context: Context) {
            val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            sharedPref.edit { clear() }
        }

        @JvmStatic
        fun exportFavoritesToJson(context: Context): String {
            val favorites = getFavorites(context)
            val jsonArray = org.json.JSONArray()
            for (id in favorites) {
                val stopId = id.toString()
                val customName = getFavoriteName(context, stopId)
                val obj = org.json.JSONObject()
                obj.put("stopId", stopId)
                if (!customName.isNullOrEmpty()) {
                    obj.put("name", customName)
                }
                jsonArray.put(obj)
            }
            return jsonArray.toString(2)
        }

        @JvmStatic
        fun importFavoritesFromJson(context: Context, jsonString: String): Boolean {
            return try {
                val trimmed = jsonString.trim()
                if (trimmed.isEmpty()) return false

                val jsonArray = if (trimmed.startsWith("{")) {
                    val obj = org.json.JSONObject(trimmed)
                    obj.optJSONArray("favorites") ?: org.json.JSONArray()
                } else if (trimmed.startsWith("[")) {
                    org.json.JSONArray(trimmed)
                } else {
                    return false
                }

                var importedCount = 0
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.opt(i)
                    if (item is org.json.JSONObject) {
                        val stopId = item.optString("stopId").ifEmpty { item.optString("id") }
                        val name = item.optString("name", "")
                        if (stopId.isNotEmpty()) {
                            if (name.isNotEmpty()) {
                                addFavoriteWithName(context, stopId, name)
                            } else {
                                addFavorite(context, stopId)
                            }
                            importedCount++
                        }
                    } else if (item != null) {
                        val stopId = item.toString().trim()
                        if (stopId.isNotEmpty() && stopId.toIntOrNull() != null) {
                            addFavorite(context, stopId)
                            importedCount++
                        }
                    }
                }
                importedCount > 0
            } catch (_: Exception) {
                false
            }
        }
    }
}
