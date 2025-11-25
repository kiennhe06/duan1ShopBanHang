package com.shopHang.customer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.shopHang.R;
import com.shopHang.adapters.SizeAdapter;
import com.shopHang.models.Product;
import com.shopHang.utils.CartManager;
import com.shopHang.utils.FavoriteManager;
import com.shopHang.utils.ImageUtils;
import com.squareup.picasso.Picasso;
import java.util.ArrayList;
import java.util.List;

public class ProductDetailActivity extends AppCompatActivity {
    private Product product;
    private ImageView ivProductImage;
    private TextView tvName, tvPrice, tvCategory, tvStock, tvDescription, tvQuantity;
    private ImageButton btnDecrease, btnIncrease, btnFavorite;
    private Button btnAddToCart;
    private RecyclerView rvSizes;
    private SizeAdapter sizeAdapter;
    private FavoriteManager favoriteManager;
    private int quantity = 1;
    private String selectedSize = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);
        
        // Setup toolbar với nút back
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
                getSupportActionBar().setTitle("");
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }
        
        product = (Product) getIntent().getSerializableExtra("product");
        if (product == null) {
            finish();
            return;
        }
        
        initViews();
        displayProduct();
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
    
    private void initViews() {
        favoriteManager = FavoriteManager.getInstance(this);
        
        ivProductImage = findViewById(R.id.ivProductImage);
        tvName = findViewById(R.id.tvProductName);
        tvPrice = findViewById(R.id.tvProductPrice);
        tvCategory = findViewById(R.id.tvProductCategory);
        tvStock = findViewById(R.id.tvProductStock);
        tvDescription = findViewById(R.id.tvProductDescription);
        tvQuantity = findViewById(R.id.tvQuantity);
        btnDecrease = findViewById(R.id.btnDecrease);
        btnIncrease = findViewById(R.id.btnIncrease);
        btnAddToCart = findViewById(R.id.btnAddToCart);
        btnFavorite = findViewById(R.id.btnFavorite);
        rvSizes = findViewById(R.id.rvSizes);
        
        // Setup Size RecyclerView
        rvSizes.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        List<String> sizes = product.getSizes() != null && !product.getSizes().isEmpty() 
            ? product.getSizes() 
            : new ArrayList<>();
        
        // Ẩn size selector nếu không có sizes
        View sizeContainer = (View) rvSizes.getParent();
        if (sizes.isEmpty()) {
            sizeContainer.setVisibility(View.GONE);
        } else {
            sizeContainer.setVisibility(View.VISIBLE);
            sizeAdapter = new SizeAdapter(sizes, size -> {
                selectedSize = size;
                sizeAdapter.setSelectedSize(size);
            });
            rvSizes.setAdapter(sizeAdapter);
        }
        
        // Setup Favorite button
        updateFavoriteButton();
        btnFavorite.setOnClickListener(v -> {
            favoriteManager.toggleFavorite(product.getId());
            updateFavoriteButton();
            Toast.makeText(this, 
                favoriteManager.isFavorite(product.getId()) ? "Đã thêm vào yêu thích" : "Đã xóa khỏi yêu thích", 
                Toast.LENGTH_SHORT).show();
        });
        
        btnDecrease.setOnClickListener(v -> {
            if (quantity > 1) {
                quantity--;
                tvQuantity.setText(String.valueOf(quantity));
            }
        });
        
        btnIncrease.setOnClickListener(v -> {
            if (quantity < product.getStock()) {
                quantity++;
                tvQuantity.setText(String.valueOf(quantity));
            } else {
                Toast.makeText(this, "Không đủ tồn kho", Toast.LENGTH_SHORT).show();
            }
        });
        
        btnAddToCart.setOnClickListener(v -> addToCart());
    }
    
    private void updateFavoriteButton() {
        boolean isFavorite = favoriteManager.isFavorite(product.getId());
        btnFavorite.setImageResource(isFavorite 
            ? android.R.drawable.star_big_on 
            : android.R.drawable.star_big_off);
    }
    
    private void displayProduct() {
        tvName.setText(product.getName());
        tvPrice.setText(product.getFormattedPrice());
        tvCategory.setText(product.getCategoryName() != null && !product.getCategoryName().isEmpty() ? product.getCategoryName() : "N/A");
        tvStock.setText("Tồn kho: " + product.getStock());
        tvDescription.setText(product.getDescription() != null ? product.getDescription() : "Không có mô tả");
        tvQuantity.setText(String.valueOf(quantity));
        
        // Hiển thị ảnh sản phẩm
        String imageUrl = product.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            String fullImageUrl = ImageUtils.getFullImageUrl(imageUrl, this);
            Picasso.get()
                .load(fullImageUrl)
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_foreground)
                .into(ivProductImage);
        } else {
            ivProductImage.setImageResource(R.drawable.ic_launcher_foreground);
        }
    }
    
    private void addToCart() {
        if (quantity > product.getStock()) {
            Toast.makeText(this, "Số lượng vượt quá tồn kho", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Kiểm tra nếu có sizes và chưa chọn size
        boolean hasSizes = product.getSizes() != null && !product.getSizes().isEmpty();
        if (hasSizes && selectedSize == null) {
            Toast.makeText(this, "Vui lòng chọn kích thước", Toast.LENGTH_SHORT).show();
            return;
        }
        
        CartManager.getInstance(this).addToCart(product, quantity, hasSizes ? selectedSize : null);
        Toast.makeText(this, "Đã thêm vào giỏ hàng", Toast.LENGTH_SHORT).show();
    }
}

