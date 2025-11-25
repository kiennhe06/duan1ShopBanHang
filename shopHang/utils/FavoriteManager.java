package com.shopHang.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.shopHang.models.Product;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class FavoriteManager {
    private static final String FAVORITE_PREF_NAME = "favorite_pref";
    private static final String KEY_FAVORITES = "favorites";
    
    private static FavoriteManager instance;
    private SharedPreferences sharedPreferences;
    private Gson gson;
    private List<String> favoriteIds;
    
    private FavoriteManager(Context context) {
        sharedPreferences = context.getSharedPreferences(FAVORITE_PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        loadFavorites();
    }
    
    public static synchronized FavoriteManager getInstance(Context context) {
        if (instance == null) {
            instance = new FavoriteManager(context);
        }
        return instance;
    }
    
    private void loadFavorites() {
        String favoritesJson = sharedPreferences.getString(KEY_FAVORITES, null);
        if (favoritesJson != null) {
            Type type = new TypeToken<List<String>>(){}.getType();
            favoriteIds = gson.fromJson(favoritesJson, type);
        } else {
            favoriteIds = new ArrayList<>();
        }
    }
    
    private void saveFavorites() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_FAVORITES, gson.toJson(favoriteIds));
        editor.apply();
    }
    
    public void addFavorite(String productId) {
        if (!favoriteIds.contains(productId)) {
            favoriteIds.add(productId);
            saveFavorites();
        }
    }
    
    public void removeFavorite(String productId) {
        favoriteIds.remove(productId);
        saveFavorites();
    }
    
    public boolean isFavorite(String productId) {
        return favoriteIds.contains(productId);
    }
    
    public List<String> getFavoriteIds() {
        return new ArrayList<>(favoriteIds);
    }
    
    public void toggleFavorite(String productId) {
        if (isFavorite(productId)) {
            removeFavorite(productId);
        } else {
            addFavorite(productId);
        }
    }
}

