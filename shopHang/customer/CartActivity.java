package com.shopHang.customer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.shopHang.R;
import com.shopHang.adapters.CartAdapter;
import com.shopHang.models.CartItem;
import com.shopHang.utils.CartManager;
import com.shopHang.utils.SharedPrefManager;
import java.util.List;

public class CartActivity extends AppCompatActivity {
    private RecyclerView rvCart;
    private CartAdapter adapter;
    private TextView tvTotal;
    private Button btnCheckout;
    private CartManager cartManager;
    private View emptyStateCart;
    
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
        
        setContentView(R.layout.activity_cart);
        
        cartManager = CartManager.getInstance(this);
        initViews();
        setupBottomNavigation();
        loadCart();
    }
    
    private void initViews() {
        rvCart = findViewById(R.id.rvCart);
        tvTotal = findViewById(R.id.tvTotal);
        btnCheckout = findViewById(R.id.btnCheckout);
        emptyStateCart = findViewById(R.id.emptyStateCart);
        
        rvCart.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CartAdapter(new CartAdapter.CartListener() {
            @Override
            public void onQuantityChanged() {
                updateTotal();
            }
            
            @Override
            public void onItemRemoved() {
                loadCart();
            }
            
            @Override
            public void onSelectionChanged() {
                updateTotal();
            }
        });
        rvCart.setAdapter(adapter);
        
        btnCheckout.setOnClickListener(v -> {
            List<CartItem> selectedItems = adapter.getSelectedItems();
            if (selectedItems.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn ít nhất một sản phẩm để thanh toán", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, CheckoutActivity.class);
            // Pass selected items
            intent.putExtra("selectedItems", new java.util.ArrayList<>(selectedItems));
            startActivity(intent);
        });
    }
    
    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                return true;
            } else if (itemId == R.id.nav_cart) {
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
        bottomNav.setSelectedItemId(R.id.nav_cart);
    }
    
    private void loadCart() {
        List<CartItem> items = cartManager.getCartItems();
        adapter.updateCartItems(items);
        updateTotal();
        
        if (items.isEmpty()) {
            btnCheckout.setEnabled(false);
            rvCart.setVisibility(View.GONE);
            if (emptyStateCart != null) {
                emptyStateCart.setVisibility(View.VISIBLE);
            }
        } else {
            btnCheckout.setEnabled(true);
            rvCart.setVisibility(View.VISIBLE);
            if (emptyStateCart != null) {
                emptyStateCart.setVisibility(View.GONE);
            }
        }
    }
    
    private void updateTotal() {
        // Chỉ tính tổng các sản phẩm đã chọn
        List<CartItem> selectedItems = adapter.getSelectedItems();
        double total = 0;
        for (CartItem item : selectedItems) {
            total += item.getSubtotal();
        }
        tvTotal.setText(String.format("%,.0f₫", total));
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadCart();
    }
}
