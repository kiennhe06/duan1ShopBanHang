package com.shopHang.customer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.shopHang.R;
import com.shopHang.adapters.ProductAdapter;
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

public class AllProductsActivity extends AppCompatActivity {
    private RecyclerView rvProducts;
    private ProductAdapter adapter;
    private EditText etSearch;
    private ProgressBar progressBar;
    private View emptyStateProducts;
    private List<Product> allProducts = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Kiểm tra đăng nhập và phân quyền
        SharedPrefManager prefManager = SharedPrefManager.getInstance(this);
        if (!prefManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        
        setContentView(R.layout.activity_all_products);
        
        initViews();
        setupToolbar();
        setupBottomNavigation();
        loadProducts();
    }
    
    private void initViews() {
        rvProducts = findViewById(R.id.rvProducts);
        etSearch = findViewById(R.id.etSearch);
        progressBar = findViewById(R.id.progressBar);
        emptyStateProducts = findViewById(R.id.emptyStateProducts);
        
        rvProducts.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new ProductAdapter(new ArrayList<>(), product -> {
            Intent intent = new Intent(this, ProductDetailActivity.class);
            intent.putExtra("product", product);
            startActivity(intent);
        });
        rvProducts.setAdapter(adapter);
        
        findViewById(R.id.ivSearch).setOnClickListener(v -> performSearch());
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
                getSupportActionBar().setTitle("Tất cả sản phẩm");
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }
    
    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        if (bottomNav != null) {
            bottomNav.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    startActivity(new Intent(this, HomeActivity.class));
                    finish();
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
    }
    
    private void loadProducts() {
        progressBar.setVisibility(View.VISIBLE);
        SharedPrefManager prefManager = SharedPrefManager.getInstance(this);
        String rawToken = prefManager.getToken();
        
        if (rawToken == null || rawToken.isEmpty()) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
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
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().getProducts() != null) {
                    allProducts = response.body().getProducts();
                    adapter.updateProducts(allProducts);
                    if (allProducts.isEmpty()) {
                        if (emptyStateProducts != null) {
                            emptyStateProducts.setVisibility(View.VISIBLE);
                        }
                        rvProducts.setVisibility(View.GONE);
                    } else {
                        if (emptyStateProducts != null) {
                            emptyStateProducts.setVisibility(View.GONE);
                        }
                        rvProducts.setVisibility(View.VISIBLE);
                    }
                } else {
                    String errorMsg = "Không thể tải sản phẩm";
                    if (response.code() == 401) {
                        errorMsg = "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại";
                        SharedPrefManager.getInstance(AllProductsActivity.this).logout();
                        startActivity(new Intent(AllProductsActivity.this, LoginActivity.class));
                        finish();
                    }
                    Toast.makeText(AllProductsActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<ApiService.ProductListResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AllProductsActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void performSearch() {
        String query = etSearch.getText().toString().trim();
        if (query.isEmpty()) {
            loadProducts();
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
                    adapter.updateProducts(allProducts);
                    if (allProducts.isEmpty()) {
                        if (emptyStateProducts != null) {
                            emptyStateProducts.setVisibility(View.VISIBLE);
                        }
                        rvProducts.setVisibility(View.GONE);
                    } else {
                        if (emptyStateProducts != null) {
                            emptyStateProducts.setVisibility(View.GONE);
                        }
                        rvProducts.setVisibility(View.VISIBLE);
                    }
                }
            }
            
            @Override
            public void onFailure(Call<ApiService.SearchResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AllProductsActivity.this, "Lỗi tìm kiếm", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

