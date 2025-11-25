package com.shopHang.customer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.shopHang.R;
import com.shopHang.adapters.ReviewAdapter;
import com.shopHang.api.ApiService;
import com.shopHang.api.RetrofitClient;
import com.shopHang.models.CreateReviewRequest;
import com.shopHang.models.Product;
import com.shopHang.models.Review;
import com.shopHang.models.ReviewResponse;
import com.shopHang.utils.SharedPrefManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.ArrayList;
import java.util.List;

public class ProductReviewsActivity extends AppCompatActivity {
    private Product product;
    private RecyclerView rvReviews;
    private ReviewAdapter adapter;
    private TextView tvAverageRating, tvTotalReviews, tvNoReviews;
    private RatingBar ratingBar;
    private EditText etComment;
    private Button btnSubmitReview;
    private ProgressBar progressBar;
    private List<Review> reviews = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        SharedPrefManager prefManager = SharedPrefManager.getInstance(this);
        if (!prefManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        
        setContentView(R.layout.activity_product_reviews);
        
        product = (Product) getIntent().getSerializableExtra("product");
        if (product == null) {
            finish();
            return;
        }
        
        initViews();
        setupToolbar();
        loadReviews();
    }
    
    private void initViews() {
        rvReviews = findViewById(R.id.rvReviews);
        tvAverageRating = findViewById(R.id.tvAverageRating);
        tvTotalReviews = findViewById(R.id.tvTotalReviews);
        tvNoReviews = findViewById(R.id.tvNoReviews);
        ratingBar = findViewById(R.id.ratingBar);
        etComment = findViewById(R.id.etComment);
        btnSubmitReview = findViewById(R.id.btnSubmitReview);
        progressBar = findViewById(R.id.progressBar);
        
        rvReviews.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReviewAdapter(new ArrayList<>());
        rvReviews.setAdapter(adapter);
        
        btnSubmitReview.setOnClickListener(v -> submitReview());
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
                getSupportActionBar().setTitle("Đánh giá sản phẩm");
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }
    
    private void loadReviews() {
        progressBar.setVisibility(View.VISIBLE);
        SharedPrefManager prefManager = SharedPrefManager.getInstance(this);
        String rawToken = prefManager.getToken();
        
        if (rawToken == null || rawToken.isEmpty()) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }
        
        final String token = !rawToken.startsWith("Bearer ") 
            ? "Bearer " + rawToken 
            : rawToken;
        
        // Review API is not available in current implementation
        progressBar.setVisibility(View.GONE);
        reviews = new ArrayList<>();
        adapter.updateReviews(reviews);
        tvAverageRating.setText("0.0");
        tvTotalReviews.setText("0 đánh giá");
        Toast.makeText(this, "Chức năng đánh giá đang được phát triển", Toast.LENGTH_LONG).show();
        
        /*
        ApiService apiService = RetrofitClient.getInstance().getApiService();
        Call<ReviewResponse> call = apiService.getProductReviews(token, product.getId());
        
        call.enqueue(new Callback<ReviewResponse>() {
            @Override
            public void onResponse(Call<ReviewResponse> call, Response<ReviewResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    ReviewResponse reviewResponse = response.body();
                    reviews = reviewResponse.getReviews() != null ? reviewResponse.getReviews() : new ArrayList<>();
                    adapter.updateReviews(reviews);
                    
                    double avgRating = reviewResponse.getAverageRating();
                    int total = reviewResponse.getTotalReviews();
                    
                    tvAverageRating.setText(String.format("%.1f", avgRating));
                    tvTotalReviews.setText(total + " đánh giá");
                    
                    if (reviews.isEmpty()) {
                        tvNoReviews.setVisibility(View.VISIBLE);
                        rvReviews.setVisibility(View.GONE);
                    } else {
                        tvNoReviews.setVisibility(View.GONE);
                        rvReviews.setVisibility(View.VISIBLE);
                    }
                } else {
                    Toast.makeText(ProductReviewsActivity.this, "Không thể tải đánh giá", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<ReviewResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ProductReviewsActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        */
        
        if (reviews.isEmpty()) {
            tvNoReviews.setVisibility(View.VISIBLE);
            rvReviews.setVisibility(View.GONE);
        } else {
            tvNoReviews.setVisibility(View.GONE);
            rvReviews.setVisibility(View.VISIBLE);
        }
    }
    
    private void submitReview() {
        int rating = (int) ratingBar.getRating();
        String comment = etComment.getText().toString().trim();
        
        if (rating == 0) {
            Toast.makeText(this, "Vui lòng chọn số sao đánh giá", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (comment.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập bình luận", Toast.LENGTH_SHORT).show();
            return;
        }
        
        progressBar.setVisibility(View.VISIBLE);
        btnSubmitReview.setEnabled(false);
        
        SharedPrefManager prefManager = SharedPrefManager.getInstance(this);
        String rawToken = prefManager.getToken();
        final String token = !rawToken.startsWith("Bearer ") 
            ? "Bearer " + rawToken 
            : rawToken;
        
        // Review API is not available in current implementation
        progressBar.setVisibility(View.GONE);
        btnSubmitReview.setEnabled(true);
        Toast.makeText(this, "Chức năng đánh giá đang được phát triển", Toast.LENGTH_LONG).show();
        
        /*
        // Backend sẽ tự động lấy customerId từ user hiện tại
        ApiService apiService = RetrofitClient.getInstance().getApiService();
        CreateReviewRequest request = new CreateReviewRequest(product.getId(), null, rating, comment);
        apiService.createReview(token, request).enqueue(new Callback<Review>() {
            @Override
            public void onResponse(Call<Review> call, Response<Review> response) {
                progressBar.setVisibility(View.GONE);
                btnSubmitReview.setEnabled(true);
                if (response.isSuccessful()) {
                    Toast.makeText(ProductReviewsActivity.this, "Đánh giá thành công!", Toast.LENGTH_SHORT).show();
                    ratingBar.setRating(0);
                    etComment.setText("");
                    loadReviews();
                    // Refresh MyReviewsActivity nếu đang mở
                    setResult(RESULT_OK);
                } else {
                    String errorMsg = "Đánh giá thất bại";
                    if (response.errorBody() != null) {
                        try {
                            errorMsg = response.errorBody().string();
                        } catch (Exception e) {
                            errorMsg = "Lỗi: " + response.code();
                        }
                    }
                    Toast.makeText(ProductReviewsActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }
            
            @Override
            public void onFailure(Call<Review> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnSubmitReview.setEnabled(true);
                Toast.makeText(ProductReviewsActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        */
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

