package com.shopHang.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.shopHang.models.User;

public class SharedPrefManager {
    private static final String SHARED_PREF_NAME = "shophang_pref";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_USER = "user";
    private static final String KEY_REMEMBER_USERNAME = "remember_username";
    private static final String KEY_REMEMBER_PASSWORD = "remember_password";
    private static final String KEY_REMEMBER_ME = "remember_me";
    
    private static SharedPrefManager instance;
    private SharedPreferences sharedPreferences;
    private Gson gson;
    
    private SharedPrefManager(Context context) {
        sharedPreferences = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }
    
    public static synchronized SharedPrefManager getInstance(Context context) {
        if (instance == null) {
            instance = new SharedPrefManager(context);
        }
        return instance;
    }
    
    public void saveToken(String token) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_TOKEN, token);
        editor.apply();
    }
    
    public String getToken() {
        return sharedPreferences.getString(KEY_TOKEN, null);
    }
    
    public void saveUser(User user) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_USER, gson.toJson(user));
        editor.apply();
    }
    
    public User getUser() {
        String userJson = sharedPreferences.getString(KEY_USER, null);
        if (userJson != null) {
            return gson.fromJson(userJson, User.class);
        }
        return null;
    }
    
    public boolean isLoggedIn() {
        return getToken() != null;
    }
    
    public void logout() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        // Xóa token và user, nhưng giữ lại remember info nếu đã chọn remember
        editor.remove(KEY_TOKEN);
        editor.remove(KEY_USER);
        editor.apply();
    }
    
    // Remember me functionality
    public void saveRememberMe(boolean remember, String username, String password) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_REMEMBER_ME, remember);
        if (remember) {
            editor.putString(KEY_REMEMBER_USERNAME, username);
            editor.putString(KEY_REMEMBER_PASSWORD, password);
        } else {
            // Xóa thông tin remember nếu không chọn
            editor.remove(KEY_REMEMBER_USERNAME);
            editor.remove(KEY_REMEMBER_PASSWORD);
        }
        editor.apply();
    }
    
    public boolean isRememberMe() {
        return sharedPreferences.getBoolean(KEY_REMEMBER_ME, false);
    }
    
    public String getRememberedUsername() {
        return sharedPreferences.getString(KEY_REMEMBER_USERNAME, "");
    }
    
    public String getRememberedPassword() {
        return sharedPreferences.getString(KEY_REMEMBER_PASSWORD, "");
    }
    
    public void clearRememberMe() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_REMEMBER_ME);
        editor.remove(KEY_REMEMBER_USERNAME);
        editor.remove(KEY_REMEMBER_PASSWORD);
        editor.apply();
    }
}

