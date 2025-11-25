package com.shopHang.api;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    // Default URL - có thể được override bằng setBaseUrl()
    private static String BASE_URL = "http://10.0.2.2:3000/";    // For real device, use your computer's IP: "http://192.168.x.x:3000/"
    
    private static RetrofitClient instance;
    private ApiService apiService;
    
    private RetrofitClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        
        OkHttpClient client = new OkHttpClient.Builder()
            .addInterceptor(logging)
            .build();
        
        Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build();
        
        apiService = retrofit.create(ApiService.class);
    }
    
    public static synchronized RetrofitClient getInstance() {
        if (instance == null) {
            instance = new RetrofitClient();
        }
        return instance;
    }
    
    public ApiService getApiService() {
        return apiService;
    }
    
    public static void setBaseUrl(String baseUrl) {
        // Đảm bảo baseUrl có trailing slash
        if (baseUrl != null && !baseUrl.isEmpty()) {
            if (!baseUrl.endsWith("/")) {
                baseUrl = baseUrl + "/";
            }
            BASE_URL = baseUrl;
        }
        
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        
        OkHttpClient client = new OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build();
        
        Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build();
        
        instance = new RetrofitClient();
        instance.apiService = retrofit.create(ApiService.class);
    }
    
    /**
     * Lấy BASE_URL hiện tại (không có trailing slash)
     */
    public static String getBaseUrl() {
        // Trả về không có trailing slash để dùng cho ImageUtils
        return BASE_URL.endsWith("/") ? BASE_URL.substring(0, BASE_URL.length() - 1) : BASE_URL;
    }
}

