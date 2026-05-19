package dev.diskettefox.MadridLibreTransport;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class FavoritesManager {
    private static final String PREFS_NAME = "FavoritesPrefs";
    private static final String KEY_FAVORITES = "favorite_stops_ordered";
    private static final String KEY_FAVORITE_NAMES="favorite_names";

    public static void addFavorite(Context context, String stopId) {
        List<Integer> favorites = getFavorites(context);
        int id = Integer.parseInt(stopId);
        if (!favorites.contains(id)) {
            favorites.add(id);
            saveFavorites(context, favorites);
        }
    }

    public static void removeFavorite(Context context, String stopId) {
        List<Integer> favorites = getFavorites(context);
        favorites.remove(Integer.valueOf(stopId));
        saveFavorites(context, favorites);
    }

    public static void addFavoriteWithName(Context context, String stopId, String customName){
        addFavorite(context, stopId);

        // Saves the name
        SharedPreferences sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        sharedPref.edit().putString(KEY_FAVORITE_NAMES + stopId, customName).apply();
    }

    public static boolean isFavorite(Context context, String stopId) {
        try {
            return getFavorites(context).contains(Integer.parseInt(stopId));
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static void saveFavorites(Context context, List<Integer> favoriteIds) {
        SharedPreferences sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        List<String> stringIds = new ArrayList<>();
        for (Integer id : favoriteIds) {
            stringIds.add(String.valueOf(id));
        }
        sharedPref.edit().putString(KEY_FAVORITES, TextUtils.join(",", stringIds)).apply();
    }

    public static List<Integer> getFavorites(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String favoritesString = sharedPref.getString(KEY_FAVORITES, "");

        if (favoritesString.isEmpty()) return new ArrayList<>();

        List<Integer> favoriteIds = new ArrayList<>();
        for (String id : favoritesString.split(",")) {
            favoriteIds.add(Integer.parseInt(id));
        }
        return favoriteIds;
    }
    public static String getFavoriteName(Context context, String stopId) {
        SharedPreferences pref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return pref.getString(KEY_FAVORITE_NAMES + stopId, null);
    }
}
