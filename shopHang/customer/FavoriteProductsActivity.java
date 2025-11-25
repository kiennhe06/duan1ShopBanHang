package com.shopHang.customer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.shopHang.R;
import com.shopHang.adapters.ProductAdapter;
import com.shopHang.api.ApiService;
import com.shopHang.api.RetrofitClient;
import com.shopHang.models.Product;
import com.shopHang.utils.FavoriteManager;
import com.shopHang.utils.SharedPrefManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.ArrayList;
import java.util.List;

public class FavoriteProductsActivity extends AppCompatActivity {
    private RecyclerView rvFavoriteProducts;
    private TextView tvEmpty;
    private ImageButton btnBack;
    private ProductAdapter adapter;
    private List<Product> favoriteProducts = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite_products);
        
        initViews();
        loadFavoriteProducts();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Reload when returning from product detail (favorite might have changed)
        loadFavoriteProducts();
    }
    
    private void initViews() {
        rvFavoriteProducts = findViewById(R.id.rvFavoriteProducts);
        tvEmpty = findViewById(R.id.tvEmpty);
        btnBack = findViewById(R.id.btnBack);
        
        btnBack.setOnClickListener(v -> finish());
        
        rvFavoriteProducts.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new ProductAdapter(favoriteProducts, product -> {
            Intent intent = new Intent(this, ProductDetailActivity.class);
            intent.putExtra("product", product);
            startActivity(intent);
        });
        rvFavoriteProducts.setAdapter(adapter);
    }
    
    private void loadFavoriteProducts() {
        FavoriteManager favoriteManager = FavoriteManager.getInstance(this);
        List<String> favoriteIds = favoriteManager.getFavoriteIds();
        
        if (favoriteIds.isEmpty()) {
            updateUI();
            return;
        }
        
        // Load products from API
        SharedPrefManager prefManager = SharedPrefManager.getInstance(this);
        String rawToken = prefManager.getToken();
        if (rawToken == null || rawToken.isEmpty()) {
            updateUI();
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
                    List<Product> allProducts = response.body().getProducts();
                    favoriteProducts = new ArrayList<>();
                    
                    // Filter only favorite products
                    for (Product product : allProducts) {
                        if (product.getId() != null && favoriteIds.contains(product.getId())) {
                            favoriteProducts.add(product);
                        }
                    }
                    
                    updateUI();
                } else {
                    updateUI();
                }
            }
            
            @Override
            public void onFailure(Call<ApiService.ProductListResponse> call, Throwable t) {
                updateUI();
            }
        });
    }
    
    private void updateUI() {
        if (favoriteProducts.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvFavoriteProducts.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvFavoriteProducts.setVisibility(View.VISIBLE);
            adapter.updateProducts(favoriteProducts);
        }
    }
}

