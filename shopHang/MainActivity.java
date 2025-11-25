package com.shopHang;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.shopHang.utils.SharedPrefManager;

public class MainActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Luôn yêu cầu đăng nhập lại khi khởi động app
        // Không giữ nguyên trạng thái đăng nhập
        startActivity(new Intent(this, com.shopHang.customer.LoginActivity.class));
        finish();
    }
}

