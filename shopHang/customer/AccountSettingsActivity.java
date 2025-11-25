package com.shopHang.customer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.shopHang.R;
import com.shopHang.models.User;
import com.shopHang.utils.SharedPrefManager;

public class AccountSettingsActivity extends AppCompatActivity {
    private ImageButton btnBack;
    private TextView tvUsername, tvEmail, tvRole;
    private LinearLayout layoutChangePassword;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_settings);
        
        initViews();
        loadUserInfo();
    }
    
    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvUsername = findViewById(R.id.tvUsername);
        tvEmail = findViewById(R.id.tvEmail);
        tvRole = findViewById(R.id.tvRole);
        layoutChangePassword = findViewById(R.id.layoutChangePassword);
        
        btnBack.setOnClickListener(v -> finish());
        
        layoutChangePassword.setOnClickListener(v -> {
            // TODO: Implement change password
            android.widget.Toast.makeText(this, "Chức năng đổi mật khẩu sẽ được cập nhật sau", android.widget.Toast.LENGTH_SHORT).show();
        });
    }
    
    private void loadUserInfo() {
        SharedPrefManager prefManager = SharedPrefManager.getInstance(this);
        User user = prefManager.getUser();
        
        if (user != null) {
            tvUsername.setText("Tên đăng nhập: " + user.getUsername());
            tvEmail.setText("Email: " + (user.getEmail() != null ? user.getEmail() : "Chưa có email"));
            tvRole.setText("Vai trò: " + (user.getRole() != null ? user.getRole() : "Khách hàng"));
        }
    }
}

