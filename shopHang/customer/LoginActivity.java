package com.shopHang.customer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.shopHang.R;
import com.shopHang.api.ApiService;
import com.shopHang.api.RetrofitClient;
import com.shopHang.models.AuthResponse;
import com.shopHang.models.LoginRequest;
import com.shopHang.utils.SharedPrefManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private EditText etUsername, etPassword;
    private Button btnLogin;
    private TextView tvRegister;
    private CheckBox cbRememberMe;
    private SharedPrefManager prefManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        prefManager = SharedPrefManager.getInstance(this);
        
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        cbRememberMe = findViewById(R.id.cbRememberMe);
        
        // Load remembered username và password nếu có
        loadRememberedCredentials();
        
        btnLogin.setOnClickListener(v -> login());
        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }
    
    private void loadRememberedCredentials() {
        if (prefManager.isRememberMe()) {
            String username = prefManager.getRememberedUsername();
            String password = prefManager.getRememberedPassword();
            if (!username.isEmpty()) {
                etUsername.setText(username);
            }
            if (!password.isEmpty()) {
                etPassword.setText(password);
            }
            cbRememberMe.setChecked(true);
        }
    }
    
    private void login() {
        final String username = etUsername.getText().toString().trim();
        final String password = etPassword.getText().toString().trim();
        
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }
        
        btnLogin.setEnabled(false);
        btnLogin.setText("Đang đăng nhập...");
        
        ApiService apiService = RetrofitClient.getInstance().getApiService();
        Call<AuthResponse> call = apiService.login(new LoginRequest(username, password));
        
        call.enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Đăng nhập");
                
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authResponse = response.body();
                    String role = authResponse.getUser().getRole();
                    
                    SharedPrefManager prefManager = SharedPrefManager.getInstance(LoginActivity.this);
                    prefManager.saveToken("Bearer " + authResponse.getToken());
                    prefManager.saveUser(authResponse.getUser());
                    
                    // Lưu remember me nếu được chọn (dùng final variables)
                    boolean remember = cbRememberMe.isChecked();
                    prefManager.saveRememberMe(remember, username, remember ? password : "");
                    
                    Toast.makeText(LoginActivity.this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();
                    
                    // Chuyển đến Home
                    startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, "Tên đăng nhập hoặc mật khẩu không đúng", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Đăng nhập");
                Toast.makeText(LoginActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}

