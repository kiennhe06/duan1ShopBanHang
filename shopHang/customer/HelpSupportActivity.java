package com.shopHang.customer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.shopHang.R;

public class HelpSupportActivity extends AppCompatActivity {
    private ImageButton btnBack;
    private LinearLayout layoutContactPhone, layoutContactEmail, layoutFAQ;
    private TextView tvPhone, tvEmail;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_support);
        
        initViews();
    }
    
    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        layoutContactPhone = findViewById(R.id.layoutContactPhone);
        layoutContactEmail = findViewById(R.id.layoutContactEmail);
        layoutFAQ = findViewById(R.id.layoutFAQ);
        tvPhone = findViewById(R.id.tvPhone);
        tvEmail = findViewById(R.id.tvEmail);
        
        btnBack.setOnClickListener(v -> finish());
        
        layoutContactPhone.setOnClickListener(v -> {
            String phone = tvPhone.getText().toString();
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + phone));
            startActivity(intent);
        });
        
        layoutContactEmail.setOnClickListener(v -> {
            String email = tvEmail.getText().toString();
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:" + email));
            intent.putExtra(Intent.EXTRA_SUBJECT, "Hỗ trợ khách hàng");
            startActivity(Intent.createChooser(intent, "Gửi email"));
        });
        
        layoutFAQ.setOnClickListener(v -> {
            android.widget.Toast.makeText(this, "Câu hỏi thường gặp sẽ được cập nhật sau", android.widget.Toast.LENGTH_SHORT).show();
        });
    }
}

