package com.shopHang.api;

import com.shopHang.models.*;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;

public interface ApiService {
    // ========== AUTH ==========
    @POST("api/auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);
    
    @POST("api/auth/register")
    Call<AuthResponse> register(@Body RegisterRequest request);
    
    @POST("api/auth/logout")
    Call<ApiResponse> logout(@Header("Authorization") String token);
    
    @GET("api/auth/me")
    Call<AuthResponse> getCurrentUser(@Header("Authorization") String token);
    
    // ========== PRODUCTS ==========
    @GET("api/products")
    Call<ProductListResponse> getProducts(
        @Query("page") Integer page,
        @Query("limit") Integer limit,
        @Query("category") String category,
        @Query("search") String search
    );
    
    @GET("api/products/{id}")
    Call<Product> getProduct(@Path("id") String id);
    
    // ========== SEARCH ==========
    @GET("api/search")
    Call<SearchResponse> searchProducts(
        @Query("q") String query,
        @Query("category") String category,
        @Query("minPrice") Double minPrice,
        @Query("maxPrice") Double maxPrice
    );
    
    // ========== INNER CLASSES ==========
    class ApiResponse {
        private String message;
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
    }
    
    class ProductListResponse {
        private List<Product> products;
        private int currentPage;
        private int totalPages;
        private int totalProducts;
        
        public List<Product> getProducts() {
            return products;
        }
        
        public void setProducts(List<Product> products) {
            this.products = products;
        }
        
        public int getCurrentPage() {
            return currentPage;
        }
        
        public void setCurrentPage(int currentPage) {
            this.currentPage = currentPage;
        }
        
        public int getTotalPages() {
            return totalPages;
        }
        
        public void setTotalPages(int totalPages) {
            this.totalPages = totalPages;
        }
        
        public int getTotalProducts() {
            return totalProducts;
        }
        
        public void setTotalProducts(int totalProducts) {
            this.totalProducts = totalProducts;
        }
    }
    
    class SearchResponse {
        private List<Product> products;
        private int count;
        
        public List<Product> getProducts() {
            return products;
        }
        
        public void setProducts(List<Product> products) {
            this.products = products;
        }
        
        public int getCount() {
            return count;
        }
        
        public void setCount(int count) {
            this.count = count;
        }
    }
}
