package com.shopHang.customer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.shopHang.R;
import com.shopHang.api.ApiService;
import com.shopHang.api.RetrofitClient;
import com.shopHang.models.Customer;
import com.shopHang.models.User;
import com.shopHang.utils.SharedPrefManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileActivity extends AppCompatActivity {
    private EditText etName, etEmail, etPhone;
    private Button btnSave;
    private ImageButton btnBack;
    private Customer customer;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
        
        customer = (Customer) getIntent().getSerializableExtra("customer");
        
        initViews();
        loadData();
    }
    
    private void initViews() {
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        btnSave = findViewById(R.id.btnSave);
        btnBack = findViewById(R.id.btnBack);
        
        btnBack.setOnClickListener(v -> finish());
        
        btnSave.setOnClickListener(v -> saveProfile());
    }
    
    private void loadData() {
        SharedPrefManager prefManager = SharedPrefManager.getInstance(this);
        User user = prefManager.getUser();
        
        if (customer != null) {
            etName.setText(customer.getName() != null ? customer.getName() : "");
            etEmail.setText(customer.getEmail() != null ? customer.getEmail() : "");
            etPhone.setText(customer.getPhone() != null ? customer.getPhone() : "");
        } else if (user != null) {
            etName.setText(user.getUsername());
            etEmail.setText(user.getEmail() != null ? user.getEmail() : "");
        }
    }
    
    private void saveProfile() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        
        if (name.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Save to SharedPreferences
        SharedPrefManager prefManager = SharedPrefManager.getInstance(this);
        android.content.SharedPreferences prefs = getSharedPreferences("shophang_pref", MODE_PRIVATE);
        prefs.edit()
            .putString("profile_name", name)
            .putString("profile_email", email)
            .putString("profile_phone", phone)
            .apply();
        
        Toast.makeText(this, "Đã lưu thông tin thành công", Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }
}

