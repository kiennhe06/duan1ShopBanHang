package com.shopHang.customer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import com.shopHang.R;
import java.io.File;

public class ViewProfileImageActivity extends AppCompatActivity {
    private ImageView ivFullProfileImage;
    private ImageButton btnBack, btnClose;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set fullscreen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        setContentView(R.layout.activity_view_profile_image);
        
        initViews();
        loadProfileImage();
    }
    
    private void initViews() {
        ivFullProfileImage = findViewById(R.id.ivFullProfileImage);
        btnBack = findViewById(R.id.btnBack);
        btnClose = findViewById(R.id.btnClose);
        
        btnBack.setOnClickListener(v -> finish());
        btnClose.setOnClickListener(v -> finish());
        
        // Click on image to close
        ivFullProfileImage.setOnClickListener(v -> finish());
    }
    
    private void loadProfileImage() {
        String imagePath = getIntent().getStringExtra("image_path");
        
        if (imagePath != null) {
            File file = new File(imagePath);
            if (file.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                if (bitmap != null) {
                    ivFullProfileImage.setImageBitmap(bitmap);
                }
            }
        } else {
            // Try to load from SharedPreferences
            android.content.SharedPreferences prefs = getSharedPreferences("shophang_pref", MODE_PRIVATE);
            String savedPath = prefs.getString("profile_image_path", null);
            
            if (savedPath != null) {
                File file = new File(savedPath);
                if (file.exists()) {
                    Bitmap bitmap = BitmapFactory.decodeFile(savedPath);
                    if (bitmap != null) {
                        ivFullProfileImage.setImageBitmap(bitmap);
                    }
                }
            }
        }
    }
}

