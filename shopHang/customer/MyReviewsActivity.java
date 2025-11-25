package com.shopHang.customer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.shopHang.R;
import com.shopHang.adapters.ReviewAdapter;
import com.shopHang.api.ApiService;
import com.shopHang.api.RetrofitClient;
import com.shopHang.models.Review;
import com.shopHang.models.User;
import com.shopHang.utils.SharedPrefManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.ArrayList;
import java.util.List;

public class MyReviewsActivity extends AppCompatActivity {
    private RecyclerView rvReviews;
    private TextView tvEmpty;
    private ImageButton btnBack;
    private ReviewAdapter adapter;
    private List<Review> reviews = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_reviews);
        
        initViews();
        loadReviews();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Reload reviews khi quay lại màn hình này
        loadReviews();
    }
    
    private void initViews() {
        rvReviews = findViewById(R.id.rvReviews);
        tvEmpty = findViewById(R.id.tvEmpty);
        btnBack = findViewById(R.id.btnBack);
        
        btnBack.setOnClickListener(v -> finish());
        
        rvReviews.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReviewAdapter(reviews);
        rvReviews.setAdapter(adapter);
    }
    
    private void loadReviews() {
        SharedPrefManager prefManager = SharedPrefManager.getInstance(this);
        String rawToken = prefManager.getToken();
        if (rawToken == null || rawToken.isEmpty()) {
            showEmpty();
            return;
        }
        final String token = !rawToken.startsWith("Bearer ") 
            ? "Bearer " + rawToken 
            : rawToken;
        
        // Review API is not available in current implementation
        reviews = new ArrayList<>();
        updateUI();
        Toast.makeText(this, "Chức năng xem đánh giá của tôi đang được phát triển", Toast.LENGTH_LONG).show();
        
        /*
        // Gọi API trực tiếp để lấy reviews của customer hiện tại
        ApiService apiService = RetrofitClient.getInstance().getApiService();
        Call<List<Review>> call = apiService.getMyReviews(token);
        call.enqueue(new Callback<List<Review>>() {
            @Override
            public void onResponse(Call<List<Review>> call, Response<List<Review>> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        reviews = response.body();
                        updateUI();
                    } else {
                        // Response body null, xử lý như empty list
                        reviews = new ArrayList<>();
                        updateUI();
                    }
                } else {
                    // Xử lý lỗi từ server
                    String errorMsg = "Không thể tải đánh giá";
                    if (response.errorBody() != null) {
                        try {
                            errorMsg = response.errorBody().string();
                        } catch (Exception e) {
                            errorMsg = "Lỗi: " + response.code();
                        }
                    }
                    android.util.Log.e("MyReviewsActivity", "Error: " + errorMsg);
                    showEmpty();
                }
            }
            
            @Override
            public void onFailure(Call<List<Review>> call, Throwable t) {
                android.util.Log.e("MyReviewsActivity", "Network error: " + t.getMessage(), t);
                showEmpty();
            }
        });
        */
    }
    
    private void updateUI() {
        if (reviews.isEmpty()) {
            showEmpty();
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvReviews.setVisibility(View.VISIBLE);
            adapter.updateReviews(reviews);
        }
    }
    
    private void showEmpty() {
        tvEmpty.setVisibility(View.VISIBLE);
        rvReviews.setVisibility(View.GONE);
    }
}

