package com.shopHang.customer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.shopHang.R;
import com.shopHang.adapters.OrderAdapter;
import com.shopHang.api.ApiService;
import com.shopHang.api.RetrofitClient;
import com.shopHang.models.Order;
import com.shopHang.utils.SharedPrefManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.ArrayList;
import java.util.List;

public class OrdersActivity extends AppCompatActivity {
    private RecyclerView rvOrders;
    private OrderAdapter adapter;
    private ProgressBar progressBar;
    private View emptyStateOrders;
    private TextView tabAll, tabPending, tabProcessing, tabDelivering, tabShipped, tabPaid, tabCancelled;
    private String currentFilter = null; // null = all, "pending", "processing", "delivering", "shipped", "paid", "cancelled"
    private List<Order> allOrders = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Kiểm tra đăng nhập
        SharedPrefManager prefManager = SharedPrefManager.getInstance(this);
        if (!prefManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        
        setContentView(R.layout.activity_orders);
        
        initViews();
        setupFilterTabs();
        setupBottomNavigation();
        loadOrders();
    }
    
    private void initViews() {
        rvOrders = findViewById(R.id.rvOrders);
        progressBar = findViewById(R.id.progressBar);
        emptyStateOrders = findViewById(R.id.emptyStateOrders);
        
        // Back button
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
        
        // Filter tabs
        tabAll = findViewById(R.id.tabAll);
        tabPending = findViewById(R.id.tabPending);
        tabProcessing = findViewById(R.id.tabProcessing);
        tabDelivering = findViewById(R.id.tabDelivering);
        tabShipped = findViewById(R.id.tabShipped);
        tabPaid = findViewById(R.id.tabPaid);
        tabCancelled = findViewById(R.id.tabCancelled);
        
        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderAdapter(new ArrayList<>());
        rvOrders.setAdapter(adapter);
    }
    
    private void setupFilterTabs() {
        tabAll.setOnClickListener(v -> setActiveTab(null));
        tabPending.setOnClickListener(v -> setActiveTab("pending"));
        tabProcessing.setOnClickListener(v -> setActiveTab("processing"));
        tabDelivering.setOnClickListener(v -> setActiveTab("delivering"));
        tabShipped.setOnClickListener(v -> setActiveTab("shipped"));
        tabPaid.setOnClickListener(v -> setActiveTab("paid"));
        tabCancelled.setOnClickListener(v -> setActiveTab("cancelled"));
    }
    
    private void setActiveTab(String status) {
        currentFilter = status;
        
        // Reset all tabs
        resetTabStyle(tabAll);
        resetTabStyle(tabPending);
        resetTabStyle(tabProcessing);
        resetTabStyle(tabDelivering);
        resetTabStyle(tabShipped);
        resetTabStyle(tabPaid);
        resetTabStyle(tabCancelled);
        
        // Set active tab
        TextView activeTab = tabAll;
        if (status == null) {
            activeTab = tabAll;
        } else if (status.equals("pending")) {
            activeTab = tabPending;
        } else if (status.equals("processing")) {
            activeTab = tabProcessing;
        } else if (status.equals("delivering")) {
            activeTab = tabDelivering;
        } else if (status.equals("shipped")) {
            activeTab = tabShipped;
        } else if (status.equals("paid")) {
            activeTab = tabPaid;
        } else if (status.equals("cancelled")) {
            activeTab = tabCancelled;
        }
        
        activeTab.setTextColor(getResources().getColor(R.color.primary_green));
        activeTab.setTypeface(null, android.graphics.Typeface.BOLD);
        
        // Filter orders
        filterOrders();
    }
    
    private void resetTabStyle(TextView tab) {
        tab.setTextColor(getResources().getColor(R.color.text_secondary));
        tab.setTypeface(null, android.graphics.Typeface.NORMAL);
    }
    
    private void filterOrders() {
        if (allOrders.isEmpty()) {
            return;
        }
        
        List<Order> filtered = new ArrayList<>();
        if (currentFilter == null) {
            filtered = new ArrayList<>(allOrders);
        } else {
            for (Order order : allOrders) {
                if (order.getStatus() != null && order.getStatus().equals(currentFilter)) {
                    filtered.add(order);
                }
            }
        }
        
        adapter.updateOrders(filtered);
        
        if (filtered.isEmpty()) {
            if (emptyStateOrders != null) {
                emptyStateOrders.setVisibility(View.VISIBLE);
            }
            rvOrders.setVisibility(View.GONE);
        } else {
            if (emptyStateOrders != null) {
                emptyStateOrders.setVisibility(View.GONE);
            }
            rvOrders.setVisibility(View.VISIBLE);
        }
    }
    
    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                return true;
            } else if (itemId == R.id.nav_cart) {
                startActivity(new Intent(this, CartActivity.class));
                return true;
            } else if (itemId == R.id.nav_orders) {
                return true;
            } else if (itemId == R.id.nav_account) {
                startActivity(new Intent(this, AccountActivity.class));
                return true;
            }
            return false;
        });
        bottomNav.setSelectedItemId(R.id.nav_orders);
    }
    
    private void loadOrders() {
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
        
        // Order API is not available in current implementation
        // Show empty state
        progressBar.setVisibility(View.GONE);
        allOrders = new ArrayList<>();
        filterOrders();
        Toast.makeText(this, "Chức năng xem đơn hàng đang được phát triển", Toast.LENGTH_LONG).show();
        
        /*
        ApiService apiService = RetrofitClient.getInstance().getApiService();
        
        // Customer sẽ tự động lấy đơn hàng của mình từ backend
        Call<List<Order>> call = apiService.getOrders(token, null, null, null, null);
        call.enqueue(new Callback<List<Order>>() {
            @Override
            public void onResponse(Call<List<Order>> call, Response<List<Order>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    allOrders = response.body();
                    filterOrders(); // Apply current filter
                } else {
                    String errorMsg = "Không thể tải đơn hàng";
                    if (response.code() == 401) {
                        errorMsg = "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại";
                        SharedPrefManager.getInstance(OrdersActivity.this).logout();
                        startActivity(new Intent(OrdersActivity.this, LoginActivity.class));
                        finish();
                    } else if (response.errorBody() != null) {
                        try {
                            errorMsg = response.errorBody().string();
                        } catch (Exception e) {
                            errorMsg = "Lỗi: " + response.code();
                        }
                    }
                    Toast.makeText(OrdersActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<List<Order>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(OrdersActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        */
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadOrders();
    }
}

