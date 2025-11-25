package com.shopHang.utils;

import android.content.Context;
import android.util.Log;
import com.squareup.picasso.Picasso;

public class PicassoHelper {
    private static final String TAG = "PicassoHelper";
    
    /**
     * Đảm bảo Picasso đã được khởi tạo
     */
    public static Picasso getPicasso(Context context) {
        try {
            // Thử lấy Picasso singleton instance
            Picasso picasso = Picasso.get();
            if (picasso != null) {
                Log.d(TAG, "Picasso singleton instance found");
                return picasso;
            }
        } catch (IllegalStateException e) {
            Log.w(TAG, "Picasso not initialized as singleton: " + e.getMessage());
        } catch (Exception e) {
            Log.w(TAG, "Error getting Picasso: " + e.getMessage());
        }
        
        // Nếu không có singleton, tạo instance mới với context
        Log.w(TAG, "Creating new Picasso instance with context");
        return new Picasso.Builder(context).build();
    }
    
    /**
     * Kiểm tra Picasso có sẵn không
     */
    public static boolean isPicassoAvailable() {
        try {
            Picasso picasso = Picasso.get();
            return picasso != null;
        } catch (Exception e) {
            return false;
        }
    }
}