package com.shopHang.customer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.shopHang.R;
import com.shopHang.adapters.ProductAdapter;
import com.shopHang.adapters.CategoryAdapter;
import com.shopHang.api.ApiService;
import com.shopHang.api.RetrofitClient;
import com.shopHang.models.Product;
import com.shopHang.utils.CartManager;
import com.shopHang.utils.SharedPrefManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {
    private RecyclerView rvFeaturedProducts, rvNewProducts, rvCategories;
    private ProductAdapter featuredAdapter, newProductsAdapter;
    private CategoryAdapter categoryAdapter;
    private EditText etSearch;
    private ProgressBar progressBar;
    private TextView tvCartBadge, tvEmptyFeatured, tvEmptyNew;
    private List<Product> allProducts = new ArrayList<>();
    private List<String> categories = new ArrayList<>();
    private String selectedCategory = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Kiểm tra đăng nhập
        SharedPrefManager prefManager = SharedPrefManager.getInstance(this);
        if (!prefManager.isLoggedIn()) {
            // Chưa đăng nhập -> chuyển đến Login
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_home);
        
        initViews();
        setupBottomNavigation();
        loadProducts();
        updateCartBadge();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Reload products khi quay lại màn hình để cập nhật ảnh mới từ web admin
        // Chỉ reload nếu đã load products trước đó (tránh reload khi mới mở app)
        if (allProducts != null && !allProducts.isEmpty()) {
            android.util.Log.d("HomeActivity", "onResume: Reloading products to get latest images from web admin");
            loadProducts();
        }
        updateCartBadge();
    }
    
    private void initViews() {
        etSearch = findViewById(R.id.etSearch);
        progressBar = findViewById(R.id.progressBar);
        tvCartBadge = findViewById(R.id.tvCartBadge);
        tvEmptyFeatured = findViewById(R.id.tvEmptyFeatured);
        tvEmptyNew = findViewById(R.id.tvEmptyNew);
        
        // Setup Category RecyclerView
        rvCategories = findViewById(R.id.rvCategories);
        rvCategories.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        categoryAdapter = new CategoryAdapter(new ArrayList<>(), category -> {
            selectedCategory = category.equals("Tất cả") ? null : category;
            categoryAdapter.setSelectedCategory(category);
            filterProductsByCategory();
        });
        rvCategories.setAdapter(categoryAdapter);
        
        rvFeaturedProducts = findViewById(R.id.rvFeaturedProducts);
        rvFeaturedProducts.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        featuredAdapter = new ProductAdapter(new ArrayList<>(), product -> {
            Intent intent = new Intent(this, ProductDetailActivity.class);
            intent.putExtra("product", product);
            startActivity(intent);
        });
        rvFeaturedProducts.setAdapter(featuredAdapter);
        
        rvNewProducts = findViewById(R.id.rvNewProducts);
        rvNewProducts.setLayoutManager(new GridLayoutManager(this, 2));
        newProductsAdapter = new ProductAdapter(new ArrayList<>(), product -> {
            Intent intent = new Intent(this, ProductDetailActivity.class);
            intent.putExtra("product", product);
            startActivity(intent);
        });
        rvNewProducts.setAdapter(newProductsAdapter);
        
        findViewById(R.id.ivSearch).setOnClickListener(v -> performSearch());
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });
        findViewById(R.id.tvViewAllFeatured).setOnClickListener(v -> showAllProducts("featured"));
        findViewById(R.id.tvViewAllNew).setOnClickListener(v -> showAllProducts("new"));
    }
    
    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_cart) {
                startActivity(new Intent(this, CartActivity.class));
                return true;
            } else if (itemId == R.id.nav_orders) {
                startActivity(new Intent(this, OrdersActivity.class));
                return true;
            } else if (itemId == R.id.nav_account) {
                startActivity(new Intent(this, AccountActivity.class));
                return true;
            }
            return false;
        });
        bottomNav.setSelectedItemId(R.id.nav_home);
    }
    
    private void loadProducts() {
        loadProducts(false);
    }
    
    private void loadProducts(boolean isFiltering) {
        progressBar.setVisibility(View.VISIBLE);
        SharedPrefManager prefManager = SharedPrefManager.getInstance(this);
        String rawToken = prefManager.getToken();
        
        // Kiểm tra đăng nhập
        if (rawToken == null || rawToken.isEmpty()) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        
        // Đảm bảo token có format đúng
        final String token = !rawToken.startsWith("Bearer ") 
            ? "Bearer " + rawToken 
            : rawToken;
        
        ApiService apiService = RetrofitClient.getInstance().getApiService();
        
        // Nếu đang filter, chỉ load products của category đó
        // Nếu không, load tất cả để extract categories
        Call<ApiService.ProductListResponse> call = apiService.getProducts(1, 1000, isFiltering ? selectedCategory : null, null);
        call.enqueue(new Callback<ApiService.ProductListResponse>() {
            @Override
            public void onResponse(Call<ApiService.ProductListResponse> call, Response<ApiService.ProductListResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().getProducts() != null) {
                    allProducts = response.body().getProducts();
                    
                    // Log để debug imageUrl
                    android.util.Log.d("HomeActivity", "=== PRODUCTS LOADED FROM API ===");
                    android.util.Log.d("HomeActivity", "Total products: " + allProducts.size());
                    for (int i = 0; i < Math.min(allProducts.size(), 3); i++) {
                        Product p = allProducts.get(i);
                        android.util.Log.d("HomeActivity", "Product[" + i + "]: " + p.getName());
                        android.util.Log.d("HomeActivity", "  - ImageURL: " + (p.getImageUrl() != null ? p.getImageUrl() : "NULL"));
                        android.util.Log.d("HomeActivity", "  - ImageURL length: " + (p.getImageUrl() != null ? p.getImageUrl().length() : 0));
                    }
                    
                    if (!isFiltering) {
                        // Chỉ extract categories khi load lần đầu
                        extractCategories();
                    }
                    updateProductLists();
                } else {
                    String errorMsg = "Không thể tải sản phẩm";
                    if (response.code() == 401) {
                        errorMsg = "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại";
                        SharedPrefManager.getInstance(HomeActivity.this).logout();
                        startActivity(new Intent(HomeActivity.this, LoginActivity.class));
                        finish();
                    } else if (response.errorBody() != null) {
                        try {
                            errorMsg = response.errorBody().string();
                        } catch (Exception e) {
                            errorMsg = "Lỗi: " + response.code();
                        }
                    }
                    Toast.makeText(HomeActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<ApiService.ProductListResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(HomeActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void extractCategories() {
        // Load tất cả products để extract categories (chỉ khi categories chưa được extract)
        if (!categories.isEmpty() && categories.size() > 1) {
            return; // Đã có categories rồi
        }
        
        SharedPrefManager prefManager = SharedPrefManager.getInstance(this);
        String rawToken = prefManager.getToken();
        
        if (rawToken == null || rawToken.isEmpty()) {
            return;
        }
        
        final String token = !rawToken.startsWith("Bearer ") 
            ? "Bearer " + rawToken 
            : rawToken;
        
        ApiService apiService = RetrofitClient.getInstance().getApiService();
        Call<ApiService.ProductListResponse> call = apiService.getProducts(1, 1000, null, null);
        call.enqueue(new Callback<ApiService.ProductListResponse>() {
            @Override
            public void onResponse(Call<ApiService.ProductListResponse> call, Response<ApiService.ProductListResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getProducts() != null) {
                    categories.clear();
                    categories.add("Tất cả"); // Thêm "Tất cả" vào đầu danh sách
                    
                    for (Product product : response.body().getProducts()) {
                        String categoryName = product.getCategoryName();
                        if (categoryName != null && !categoryName.isEmpty()) {
                            if (!categories.contains(categoryName)) {
                                categories.add(categoryName);
                            }
                        }
                    }
                    
                    categoryAdapter.updateCategories(categories);
                    if (selectedCategory == null) {
                        categoryAdapter.setSelectedCategory("Tất cả");
                    }
                }
            }
            
            @Override
            public void onFailure(Call<ApiService.ProductListResponse> call, Throwable t) {
                // Ignore error, categories will be empty
            }
        });
    }
    
    private void filterProductsByCategory() {
        loadProducts(true);
    }
    
    private void updateProductLists() {
        // Featured products (first 8 để hiển thị nhiều hơn)
        int featuredCount = Math.min(8, allProducts.size());
        List<Product> featured = featuredCount > 0 ? 
            new ArrayList<>(allProducts.subList(0, featuredCount)) : new ArrayList<>();
        featuredAdapter.updateProducts(featured);
        
        if (featured.isEmpty()) {
            if (tvEmptyFeatured != null) {
                tvEmptyFeatured.setVisibility(View.VISIBLE);
            }
            rvFeaturedProducts.setVisibility(View.GONE);
        } else {
            if (tvEmptyFeatured != null) {
                tvEmptyFeatured.setVisibility(View.GONE);
            }
            rvFeaturedProducts.setVisibility(View.VISIBLE);
        }
        
        // New products (last 8 để hiển thị nhiều hơn)
        int newCount = Math.min(8, allProducts.size());
        int startIndex = Math.max(0, allProducts.size() - newCount);
        List<Product> newProducts = newCount > 0 ? 
            new ArrayList<>(allProducts.subList(startIndex, allProducts.size())) : new ArrayList<>();
        newProductsAdapter.updateProducts(newProducts);
        
        if (newProducts.isEmpty()) {
            if (tvEmptyNew != null) {
                tvEmptyNew.setVisibility(View.VISIBLE);
            }
            rvNewProducts.setVisibility(View.GONE);
        } else {
            if (tvEmptyNew != null) {
                tvEmptyNew.setVisibility(View.GONE);
            }
            rvNewProducts.setVisibility(View.VISIBLE);
        }
    }
    
    private void performSearch() {
        String query = etSearch.getText().toString().trim();
        if (query.isEmpty()) {
            updateProductLists();
            return;
        }
        
        progressBar.setVisibility(View.VISIBLE);
        SharedPrefManager prefManager = SharedPrefManager.getInstance(this);
        String rawToken = prefManager.getToken();
        
        if (rawToken == null || rawToken.isEmpty()) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Đảm bảo token có format đúng
        final String token = !rawToken.startsWith("Bearer ") 
            ? "Bearer " + rawToken 
            : rawToken;
        
        ApiService apiService = RetrofitClient.getInstance().getApiService();
        
        Call<ApiService.SearchResponse> call = apiService.searchProducts(query, null, null, null);
        call.enqueue(new Callback<ApiService.SearchResponse>() {
            @Override
            public void onResponse(Call<ApiService.SearchResponse> call, Response<ApiService.SearchResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().getProducts() != null) {
                    allProducts = response.body().getProducts();
                    updateProductLists();
                }
            }
            
            @Override
            public void onFailure(Call<ApiService.SearchResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(HomeActivity.this, "Lỗi tìm kiếm", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void showAllProducts(String type) {
        Intent intent = new Intent(this, AllProductsActivity.class);
        startActivity(intent);
    }
    
    private void updateCartBadge() {
        // Hiển thị số loại sản phẩm khác nhau, không phải tổng số lượng
        // Khi thêm cùng sản phẩm, số lượng badge không tăng, chỉ giá tiền tăng
        int count = CartManager.getInstance(this).getItemCount();
        if (count > 0) {
            tvCartBadge.setText(String.valueOf(count));
            tvCartBadge.setVisibility(View.VISIBLE);
        } else {
            tvCartBadge.setVisibility(View.GONE);
        }
    }
}

