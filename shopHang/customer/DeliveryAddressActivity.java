package com.shopHang.customer;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.shopHang.R;
import com.shopHang.api.ApiService;
import com.shopHang.api.RetrofitClient;
import com.shopHang.models.Customer;
import com.shopHang.utils.SharedPrefManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DeliveryAddressActivity extends AppCompatActivity {
    private EditText etAddress;
    private Button btnSave;
    private ImageButton btnBack;
    private TextView tvCurrentAddress;
    private Customer customer;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delivery_address);
        
        customer = (Customer) getIntent().getSerializableExtra("customer");
        
        initViews();
        loadAddress();
    }
    
    private void initViews() {
        etAddress = findViewById(R.id.etAddress);
        btnSave = findViewById(R.id.btnSave);
        btnBack = findViewById(R.id.btnBack);
        tvCurrentAddress = findViewById(R.id.tvCurrentAddress);
        
        btnBack.setOnClickListener(v -> finish());
        
        btnSave.setOnClickListener(v -> saveAddress());
    }
    
    private void loadAddress() {
        // Load from SharedPreferences first
        SharedPreferences prefs = getSharedPreferences("shophang_pref", MODE_PRIVATE);
        String savedAddress = prefs.getString("delivery_address", null);
        
        if (savedAddress != null && !savedAddress.isEmpty()) {
            etAddress.setText(savedAddress);
            tvCurrentAddress.setText("Địa chỉ hiện tại: " + savedAddress);
        } else if (customer != null && customer.getAddress() != null && !customer.getAddress().isEmpty()) {
            etAddress.setText(customer.getAddress());
            tvCurrentAddress.setText("Địa chỉ hiện tại: " + customer.getAddress());
        } else {
            tvCurrentAddress.setText("Chưa có địa chỉ");
        }
    }
    
    private void saveAddress() {
        String address = etAddress.getText().toString().trim();
        
        if (address.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập địa chỉ", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Save to SharedPreferences
        SharedPreferences prefs = getSharedPreferences("shophang_pref", MODE_PRIVATE);
        prefs.edit().putString("delivery_address", address).apply();
        
        // Address is already saved to SharedPreferences above
        Toast.makeText(this, "Đã lưu địa chỉ thành công", Toast.LENGTH_SHORT).show();
        finish();
    }
}

