package com.shopHang.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.shopHang.api.RetrofitClient;

public class ImageUtils {
    private static final String TAG = "ImageUtils";
    private static final String PREF_NAME = "shophang_pref";
    private static final String KEY_BASE_URL = "base_url";
    
    // Default URL - sẽ được override nếu có trong SharedPreferences
    private static final String DEFAULT_BASE_URL = "http://10.0.2.2:3000"; // Emulator
    // Để dùng trên thiết bị thật, cần thay bằng IP của máy tính: "http://192.168.x.x:3000"
    
    /**
     * Lấy BASE_URL từ RetrofitClient hoặc SharedPreferences hoặc dùng default
     */
    private static String getBaseUrl(Context context) {
        // Ưu tiên 1: Lấy từ RetrofitClient (luôn đúng với network hiện tại)
        try {
            String retrofitBaseUrl = RetrofitClient.getBaseUrl();
            if (retrofitBaseUrl != null && !retrofitBaseUrl.isEmpty()) {
                Log.d(TAG, "Using BASE_URL from RetrofitClient: " + retrofitBaseUrl);
                return retrofitBaseUrl;
            }
        } catch (Exception e) {
            Log.w(TAG, "Error getting BASE_URL from RetrofitClient: " + e.getMessage());
        }
        
        // Ưu tiên 2: Lấy từ SharedPreferences
        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String savedUrl = prefs.getString(KEY_BASE_URL, null);
            if (savedUrl != null && !savedUrl.isEmpty()) {
                Log.d(TAG, "Using saved BASE_URL from SharedPreferences: " + savedUrl);
                return savedUrl;
            }
        }
        
        // Ưu tiên 3: Dùng default (chỉ dùng khi không có cách nào khác)
        Log.w(TAG, "Using default BASE_URL (may not work on real device): " + DEFAULT_BASE_URL);
        return DEFAULT_BASE_URL;
    }
    
    /**
     * Lưu BASE_URL vào SharedPreferences
     */
    public static void setBaseUrl(Context context, String baseUrl) {
        if (context != null && baseUrl != null && !baseUrl.isEmpty()) {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString(KEY_BASE_URL, baseUrl).apply();
            Log.d(TAG, "Saved BASE_URL to SharedPreferences: " + baseUrl);
        }
    }
    
    /**
     * Chuyển đổi URL ảnh thành full URL
     * @param imageUrl URL ảnh (có thể là relative hoặc absolute)
     * @param context Context để lấy BASE_URL từ SharedPreferences
     * @return Full URL để load ảnh
     */
    public static String getFullImageUrl(String imageUrl, Context context) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            Log.d(TAG, "Image URL is null or empty");
            return null;
        }
        
        // Trim whitespace
        imageUrl = imageUrl.trim();
        
        // Kiểm tra URL có hợp lệ không (ít nhất phải có http:// hoặc https://)
        if (imageUrl.length() < 7) {
            Log.w(TAG, "Image URL too short, may be invalid: " + imageUrl);
            return null;
        }
        
        // Nếu URL đã là full URL (bắt đầu với http:// hoặc https://), trả về nguyên
        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            // Validate URL format cơ bản
            if (imageUrl.contains(" ") || imageUrl.contains("\n") || imageUrl.contains("\t")) {
                Log.w(TAG, "Image URL contains invalid characters: " + imageUrl);
                // Loại bỏ các ký tự không hợp lệ
                imageUrl = imageUrl.replaceAll("[\\s\\n\\t]", "");
            }
            // Đảm bảo URL không có trailing whitespace
            imageUrl = imageUrl.trim();
            Log.d(TAG, "Using absolute URL (from web admin): " + imageUrl);
            Log.d(TAG, "URL length: " + imageUrl.length());
            return imageUrl;
        }
        
        // Nếu URL là relative, thêm base URL
        String baseUrl = getBaseUrl(context);
        String fullUrl;
        if (imageUrl.startsWith("/")) {
            fullUrl = baseUrl + imageUrl;
        } else {
            fullUrl = baseUrl + "/" + imageUrl;
        }
        
        Log.d(TAG, "Converted relative URL: " + imageUrl + " -> " + fullUrl);
        return fullUrl;
    }
    
    /**
     * Overload method không cần context (dùng default BASE_URL)
     * @deprecated Nên dùng getFullImageUrl(String, Context) để có BASE_URL đúng
     */
    @Deprecated
    public static String getFullImageUrl(String imageUrl) {
        return getFullImageUrl(imageUrl, null);
    }
    
    /**
     * Kiểm tra URL có hợp lệ không
     */
    public static boolean isValidUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        return url.startsWith("http://") || url.startsWith("https://");
    }
    
    /**
     * Lấy base URL hiện tại (public method)
     */
    public static String getCurrentBaseUrl(Context context) {
        return getBaseUrl(context);
    }
    
    /**
     * Lấy base URL mặc định (không cần context)
     */
    public static String getDefaultBaseUrl() {
        return DEFAULT_BASE_URL;
    }
}

