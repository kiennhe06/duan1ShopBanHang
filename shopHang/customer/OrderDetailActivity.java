package com.shopHang.customer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.shopHang.R;
import com.shopHang.adapters.OrderItemAdapter;
import com.shopHang.api.ApiService;
import com.shopHang.api.RetrofitClient;
import com.shopHang.models.Order;
import com.shopHang.utils.SharedPrefManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderDetailActivity extends AppCompatActivity {
    private Order order;
    private TextView tvOrderId, tvStatus, tvDate, tvCustomerName, tvCustomerPhone, tvCustomerEmail, tvCustomerAddress;
    private TextView tvTotalAmount, tvItemCount;
    private RecyclerView rvOrderItems;
    private OrderItemAdapter adapter;
    private Button btnCancelOrder;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        SharedPrefManager prefManager = SharedPrefManager.getInstance(this);
        if (!prefManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        
        order = (Order) getIntent().getSerializableExtra("order");
        if (order == null) {
            finish();
            return;
        }
        
        setContentView(R.layout.activity_order_detail);
        
        setupToolbar();
        initViews();
        displayOrderDetails();
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
                getSupportActionBar().setTitle("Chi tiết đơn hàng");
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }
    
    private void initViews() {
        tvOrderId = findViewById(R.id.tvOrderId);
        tvStatus = findViewById(R.id.tvStatus);
        tvDate = findViewById(R.id.tvDate);
        tvCustomerName = findViewById(R.id.tvCustomerName);
        tvCustomerPhone = findViewById(R.id.tvCustomerPhone);
        tvCustomerEmail = findViewById(R.id.tvCustomerEmail);
        tvCustomerAddress = findViewById(R.id.tvCustomerAddress);
        tvTotalAmount = findViewById(R.id.tvTotalAmount);
        tvItemCount = findViewById(R.id.tvItemCount);
        rvOrderItems = findViewById(R.id.rvOrderItems);
        btnCancelOrder = findViewById(R.id.btnCancelOrder);
        
        rvOrderItems.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderItemAdapter(order != null ? order.getItems() : null);
        rvOrderItems.setAdapter(adapter);
        
        // Setup cancel order button
        btnCancelOrder.setOnClickListener(v -> showCancelOrderDialog());
    }
    
    private void displayOrderDetails() {
        if (order == null) return;
        
        // Order ID
        String orderId = order.getId() != null && order.getId().length() >= 8 
            ? order.getId().substring(0, 8) 
            : (order.getId() != null ? order.getId() : "N/A");
        tvOrderId.setText("Đơn hàng #" + orderId);
        
        // Status
        tvStatus.setText(order.getStatusText());
        
        // Date
        if (order.getCreatedAt() != null && order.getCreatedAt().length() >= 10) {
            tvDate.setText("Ngày đặt: " + order.getCreatedAt().substring(0, 10));
        } else {
            tvDate.setText("Ngày đặt: " + (order.getCreatedAt() != null ? order.getCreatedAt() : "N/A"));
        }
        
        // Customer info - Hiển thị đầy đủ thông tin khách hàng
        if (order.getCustomer() != null) {
            String name = order.getCustomer().getName() != null ? order.getCustomer().getName() : "N/A";
            String phone = order.getCustomer().getPhone() != null ? order.getCustomer().getPhone() : "N/A";
            String email = order.getCustomer().getEmail() != null && !order.getCustomer().getEmail().isEmpty() 
                ? order.getCustomer().getEmail() : "Chưa có email";
            String address = order.getCustomer().getAddress() != null && !order.getCustomer().getAddress().isEmpty()
                ? order.getCustomer().getAddress() : "Chưa có địa chỉ";
            
            tvCustomerName.setText(name);
            tvCustomerPhone.setText(phone);
            tvCustomerEmail.setText(email);
            tvCustomerAddress.setText(address);
        } else {
            tvCustomerName.setText("N/A");
            tvCustomerPhone.setText("N/A");
            tvCustomerEmail.setText("Chưa có email");
            tvCustomerAddress.setText("Chưa có địa chỉ");
        }
        
        // Total amount - Hiển thị đầy đủ thông tin: subtotal, shipping, discount, final amount
        StringBuilder totalText = new StringBuilder();
        totalText.append("Tạm tính: ").append(order.getFormattedSubtotal());
        
        if (order.getShippingFee() > 0) {
            totalText.append("\nPhí vận chuyển: ").append(order.getFormattedShippingFee());
        }
        
        if (order.getDiscountAmount() > 0) {
            totalText.append("\nGiảm giá: -").append(order.getFormattedDiscount());
            if (order.getVoucherCode() != null && !order.getVoucherCode().isEmpty()) {
                totalText.append(" (Mã: ").append(order.getVoucherCode()).append(")");
            }
        }
        
        totalText.append("\nTổng cộng: ").append(order.getFormattedTotal());
        tvTotalAmount.setText(totalText.toString());
        
        // Item count
        int itemCount = order.getItems() != null ? order.getItems().size() : 0;
        tvItemCount.setText("Số sản phẩm: " + itemCount);
        
        // Update adapter
        if (adapter != null) {
            adapter.updateItems(order.getItems());
        }
        
        // Show cancel button only if order can be cancelled (pending or paid, not shipped or cancelled)
        if (btnCancelOrder != null) {
            String status = order.getStatus();
            if (status != null && (status.equals("pending") || status.equals("paid"))) {
                btnCancelOrder.setVisibility(View.VISIBLE);
            } else {
                btnCancelOrder.setVisibility(View.GONE);
            }
        }
    }
    
    private void showCancelOrderDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Hủy đơn hàng")
            .setMessage("Bạn có chắc chắn muốn hủy đơn hàng này?")
            .setPositiveButton("Hủy đơn", (dialog, which) -> cancelOrder())
            .setNegativeButton("Không", null)
            .show();
    }
    
    private void cancelOrder() {
        if (order == null || order.getId() == null) {
            Toast.makeText(this, "Không thể hủy đơn hàng", Toast.LENGTH_SHORT).show();
            return;
        }
        
        btnCancelOrder.setEnabled(false);
        btnCancelOrder.setText("Đang xử lý...");
        
        SharedPrefManager prefManager = SharedPrefManager.getInstance(this);
        String rawToken = prefManager.getToken();
        if (rawToken == null || rawToken.isEmpty()) {
            btnCancelOrder.setEnabled(true);
            btnCancelOrder.setText("Hủy đơn hàng");
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }
        
        final String token = !rawToken.startsWith("Bearer ") 
            ? "Bearer " + rawToken 
            : rawToken;
        
        // Order API is not available in current implementation
        btnCancelOrder.setEnabled(true);
        btnCancelOrder.setText("Hủy đơn hàng");
        Toast.makeText(this, "Chức năng hủy đơn hàng đang được phát triển", Toast.LENGTH_LONG).show();
        
        /*
        ApiService apiService = RetrofitClient.getInstance().getApiService();
        Call<Order> call = apiService.cancelOrder(token, order.getId());
        
        call.enqueue(new Callback<Order>() {
            @Override
            public void onResponse(Call<Order> call, Response<Order> response) {
                btnCancelOrder.setEnabled(true);
                btnCancelOrder.setText("Hủy đơn hàng");
                
                if (response.isSuccessful() && response.body() != null) {
                    order = response.body();
                    Toast.makeText(OrderDetailActivity.this, "Đã hủy đơn hàng thành công", Toast.LENGTH_SHORT).show();
                    // Reload order details
                    displayOrderDetails();
                    // Refresh orders list
                    setResult(RESULT_OK);
                } else {
                    Toast.makeText(OrderDetailActivity.this, "Không thể hủy đơn hàng", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<Order> call, Throwable t) {
                btnCancelOrder.setEnabled(true);
                btnCancelOrder.setText("Hủy đơn hàng");
                Toast.makeText(OrderDetailActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        */
    }
}

