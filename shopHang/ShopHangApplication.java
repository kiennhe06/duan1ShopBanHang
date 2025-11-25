package com.shopHang;

import android.app.Application;
import android.util.Log;
import com.squareup.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;
import okhttp3.Cache;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ShopHangApplication extends Application {
    private static final String TAG = "ShopHangApplication";
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Tạo cache directory cho OkHttp
        File cacheDir = new File(getCacheDir(), "picasso-cache");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        Cache cache = new Cache(cacheDir, 50 * 1024 * 1024); // 50MB cache
        
        // Interceptor để thêm headers và retry khi gặp lỗi
        Interceptor retryInterceptor = new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request originalRequest = chain.request();
                
                // Thêm headers để load ảnh từ CDN (tương thích với cả WiFi và 5G)
                Request.Builder requestBuilder = originalRequest.newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36")
                    .header("Accept", "image/webp,image/apng,image/*,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9,vi;q=0.8")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Connection", "keep-alive")
                    .header("Cache-Control", "no-cache")
                    .method(originalRequest.method(), originalRequest.body());
                
                Request request = requestBuilder.build();
                int maxRetries = 3; // Retry tối đa 3 lần
                int retryCount = 0;
                
                Response response = null;
                IOException lastException = null;
                
                while (retryCount <= maxRetries) {
                    try {
                        response = chain.proceed(request);
                        // Nếu thành công, trả về ngay
                        if (response.isSuccessful()) {
                            Log.d(TAG, "Image loaded successfully: " + request.url());
                            return response;
                        }
                        // Nếu là lỗi 504 hoặc 502, đóng response và retry
                        int code = response.code();
                        if (code == 504 || code == 502 || code == 503) {
                            Log.w(TAG, "Server error " + code + ", retrying: " + request.url());
                            if (response != null) {
                                response.close();
                            }
                        } else {
                            // Lỗi khác, trả về ngay
                            return response;
                        }
                    } catch (IOException e) {
                        lastException = e;
                        Log.w(TAG, "Network error, retrying: " + e.getMessage());
                    }
                    
                    retryCount++;
                    if (retryCount <= maxRetries) {
                        try {
                            // Đợi một chút trước khi retry (exponential backoff)
                            Thread.sleep(1000 * retryCount);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
                
                // Nếu vẫn không thành công sau khi retry, throw exception
                if (lastException != null) {
                    Log.e(TAG, "Failed to load image after retries: " + request.url(), lastException);
                    throw lastException;
                }
                if (response != null) {
                    return response;
                }
                throw new IOException("Failed to get response after " + maxRetries + " retries");
            }
        };
        
        // Cấu hình OkHttpClient với timeout dài hơn và retry (tối ưu cho cả WiFi và 5G)
        OkHttpClient client = new OkHttpClient.Builder()
            .cache(cache)
            .connectTimeout(30, TimeUnit.SECONDS) // Tăng timeout cho 5G network
            .readTimeout(45, TimeUnit.SECONDS) // Timeout 45 giây cho 5G (có thể chậm hơn WiFi)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true) // Tự động retry khi mất kết nối
            .addInterceptor(retryInterceptor) // Thêm retry interceptor với headers
            .build();
        
        // Cấu hình Picasso
        Picasso picasso = new Picasso.Builder(this)
            .downloader(new OkHttp3Downloader(client))
            .indicatorsEnabled(false) // Tắt debug indicators
            .loggingEnabled(true) // Bật logging để debug
            .build();
        
        try {
            Picasso.setSingletonInstance(picasso);
            Log.d(TAG, "Picasso configured with extended timeout and retry mechanism");
            Log.d(TAG, "Picasso instance: " + Picasso.get());
        } catch (IllegalStateException e) {
            // Picasso đã được set rồi, không sao
            Log.w(TAG, "Picasso already initialized: " + e.getMessage());
        }
    }
}
