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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.shopHang.R;
import com.shopHang.adapters.PostPurchaseReviewAdapter;
import com.shopHang.api.ApiService;
import com.shopHang.api.RetrofitClient;
import com.shopHang.models.CreateReviewRequest;
import com.shopHang.models.Order;
import com.shopHang.models.Product;
import com.shopHang.models.Review;
import com.shopHang.utils.SharedPrefManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.ArrayList;
import java.util.List;

public class PostPurchaseReviewActivity extends AppCompatActivity {
    private RecyclerView rvProducts;
    private PostPurchaseReviewAdapter adapter;
    private Button btnSkip, btnSubmit;
    private ProgressBar progressBar;
    private TextView tvTitle;
    private Order order;
    private List<Product> productsToReview = new ArrayList<>();
    
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
        
        setContentView(R.layout.activity_post_purchase_review);
        
        initViews();
        loadProductsFromOrder();
    }
    
    private void initViews() {
        rvProducts = findViewById(R.id.rvProducts);
        btnSkip = findViewById(R.id.btnSkip);
        btnSubmit = findViewById(R.id.btnSubmit);
        progressBar = findViewById(R.id.progressBar);
        tvTitle = findViewById(R.id.tvTitle);
        
        rvProducts.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PostPurchaseReviewAdapter(productsToReview, null);
        rvProducts.setAdapter(adapter);
        
        btnSkip.setOnClickListener(v -> finishAndGoToOrders());
        btnSubmit.setOnClickListener(v -> submitAllReviews());
    }
    
    private void loadProductsFromOrder() {
        if (order != null && order.getItems() != null) {
            for (Order.OrderItem item : order.getItems()) {
                if (item.getProduct() != null) {
                    productsToReview.add(item.getProduct());
                }
            }
            adapter.updateProducts(productsToReview);
            
            if (productsToReview.isEmpty()) {
                finishAndGoToOrders();
            } else {
                tvTitle.setText("Đánh giá " + productsToReview.size() + " sản phẩm đã mua");
            }
        } else {
            finishAndGoToOrders();
        }
    }
    
    private void submitReview(Product product, int rating, String comment, ReviewCallback callback) {
        SharedPrefManager prefManager = SharedPrefManager.getInstance(this);
        String rawToken = prefManager.getToken();
        final String token = !rawToken.startsWith("Bearer ") 
            ? "Bearer " + rawToken 
            : rawToken;
        
        // Review API is not available in current implementation
        if (callback != null) {
            callback.onComplete(false);
        }
        Toast.makeText(this, "Chức năng đánh giá đang được phát triển", Toast.LENGTH_SHORT).show();
        
        /*
        ApiService apiService = RetrofitClient.getInstance().getApiService();
        CreateReviewRequest request = new CreateReviewRequest(product.getId(), null, rating, comment);
        apiService.createReview(token, request).enqueue(new Callback<Review>() {
            @Override
            public void onResponse(Call<Review> call, Response<Review> response) {
                // Luôn gọi callback dù success hay fail để tránh đợi mãi
                if (callback != null) {
                    callback.onComplete(response.isSuccessful());
                }
            }
            
            @Override
            public void onFailure(Call<Review> call, Throwable t) {
                // Luôn gọi callback dù fail để tránh đợi mãi
                if (callback != null) {
                    callback.onComplete(false);
                }
            }
        });
        */
    }
    
    private interface ReviewCallback {
        void onComplete(boolean success);
    }
    
    private void submitAllReviews() {
        List<PostPurchaseReviewAdapter.ReviewData> reviews = adapter.getReviewData();
        List<PostPurchaseReviewAdapter.ReviewData> toSubmit = new ArrayList<>();
        
        for (PostPurchaseReviewAdapter.ReviewData reviewData : reviews) {
            if (reviewData.getRating() > 0) {
                toSubmit.add(reviewData);
            }
        }
        
        if (toSubmit.isEmpty()) {
            finishAndGoToOrders();
            return;
        }
        
        progressBar.setVisibility(View.VISIBLE);
        btnSubmit.setEnabled(false);
        btnSkip.setEnabled(false);
        btnSubmit.setText("Đang gửi...");
        
        final int[] completed = {0};
        final int[] successCount = {0};
        final int total = toSubmit.size();
        
        // Gửi tất cả reviews song song
        for (PostPurchaseReviewAdapter.ReviewData reviewData : toSubmit) {
            submitReview(reviewData.getProduct(), reviewData.getRating(), reviewData.getComment(), success -> {
                synchronized (PostPurchaseReviewActivity.this) {
                    completed[0]++;
                    // Chỉ đếm khi thực sự success
                    if (success) {
                        successCount[0]++;
                    }
                    
                    // Cập nhật UI ngay khi có response
                    runOnUiThread(() -> {
                        if (completed[0] < total) {
                            btnSubmit.setText("Đang gửi... (" + completed[0] + "/" + total + ")");
                        }
                    });
                    
                    // Khi tất cả đã hoàn thành (dù success hay fail)
                    if (completed[0] == total) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            btnSubmit.setEnabled(true);
                            btnSkip.setEnabled(true);
                            btnSubmit.setText("Gửi đánh giá");
                            
                            if (successCount[0] > 0) {
                                Toast.makeText(PostPurchaseReviewActivity.this, 
                                    "Đã gửi " + successCount[0] + "/" + total + " đánh giá", 
                                    Toast.LENGTH_SHORT).show();
                                
                            }
                            
                            // Chuyển màn hình sau 1 giây để người dùng thấy thông báo
                            new android.os.Handler().postDelayed(() -> {
                                finishAndGoToOrders();
                            }, 1000);
                        });
                    }
                }
            });
        }
        
        // Timeout: Nếu sau 10 giây vẫn chưa xong, vẫn chuyển màn hình
        new android.os.Handler().postDelayed(() -> {
            synchronized (PostPurchaseReviewActivity.this) {
                if (completed[0] < total) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnSubmit.setEnabled(true);
                        btnSkip.setEnabled(true);
                        btnSubmit.setText("Gửi đánh giá");
                        
                        if (successCount[0] > 0) {
                            Toast.makeText(PostPurchaseReviewActivity.this, 
                                "Đã gửi " + successCount[0] + "/" + total + " đánh giá (một số có thể chưa hoàn tất)", 
                                Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(PostPurchaseReviewActivity.this, 
                                "Gửi đánh giá đang xử lý. Vui lòng kiểm tra lại sau.", 
                                Toast.LENGTH_LONG).show();
                        }
                        
                        finishAndGoToOrders();
                    });
                }
            }
        }, 10000); // 10 giây timeout
    }
    
    private void finishAndGoToOrders() {
        startActivity(new Intent(this, OrdersActivity.class));
        finish();
    }
}

