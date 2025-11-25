package com.shopHang.customer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.shopHang.R;
import com.shopHang.api.ApiService;
import com.shopHang.api.RetrofitClient;
import com.shopHang.models.Customer;
import com.shopHang.models.User;
import com.shopHang.utils.SharedPrefManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class AccountActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_GALLERY = 1001;
    private static final int REQUEST_CODE_CAMERA = 1002;
    
    private TextView tvUserName, tvUserEmail;
    private ImageView ivProfileImage, ivCameraIcon;
    private Button btnEditProfile, btnLogout;
    private ImageButton btnBack;
    private LinearLayout layoutDeliveryAddress, layoutOrderHistory, layoutFavoriteProducts;
    private LinearLayout layoutMyReviews, layoutAccountSettings, layoutHelpSupport;
    
    private Customer customer;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Kiểm tra đăng nhập
        SharedPrefManager prefManager = SharedPrefManager.getInstance(this);
        if (!prefManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        
        setContentView(R.layout.activity_account);
        
        initViews();
        setupBottomNavigation();
        loadUserInfo();
        loadCustomerInfo();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Reload customer info when returning from other activities
        loadCustomerInfo();
        // Reload profile image
        loadProfileImage();
    }
    
    private void initViews() {
        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        ivProfileImage = findViewById(R.id.ivProfileImage);
        ivCameraIcon = findViewById(R.id.ivCameraIcon);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnLogout = findViewById(R.id.btnLogout);
        btnBack = findViewById(R.id.btnBack);
        
        layoutDeliveryAddress = findViewById(R.id.layoutDeliveryAddress);
        layoutOrderHistory = findViewById(R.id.layoutOrderHistory);
        layoutFavoriteProducts = findViewById(R.id.layoutFavoriteProducts);
        layoutMyReviews = findViewById(R.id.layoutMyReviews);
        layoutAccountSettings = findViewById(R.id.layoutAccountSettings);
        layoutHelpSupport = findViewById(R.id.layoutHelpSupport);
        
        // Back button
        btnBack.setOnClickListener(v -> finish());
        
        // Profile image click - single click to view, long press for menu
        ivProfileImage.setOnClickListener(v -> {
            // Check if image exists, if yes view it, if no show picker
            SharedPreferences prefs = getSharedPreferences("shophang_pref", MODE_PRIVATE);
            String imagePath = prefs.getString("profile_image_path", null);
            if (imagePath != null && new File(imagePath).exists()) {
                viewProfileImage();
            } else {
                showImagePickerDialog();
            }
        });
        ivProfileImage.setOnLongClickListener(v -> {
            showProfileImageMenu();
            return true;
        });
        ivCameraIcon.setOnClickListener(v -> showImagePickerDialog());
        
        // Load saved profile image
        loadProfileImage();
        
        // Edit profile button
        btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(this, EditProfileActivity.class);
            if (customer != null) {
                intent.putExtra("customer", customer);
            }
            startActivity(intent);
        });
        
        // Logout button
        btnLogout.setOnClickListener(v -> showLogoutDialog());
        
        // Delivery Address
        layoutDeliveryAddress.setOnClickListener(v -> {
            Intent intent = new Intent(this, DeliveryAddressActivity.class);
            if (customer != null) {
                intent.putExtra("customer", customer);
            }
            startActivity(intent);
        });
        
        // Order History
        layoutOrderHistory.setOnClickListener(v -> {
            startActivity(new Intent(this, OrdersActivity.class));
        });
        
        // Favorite Products
        layoutFavoriteProducts.setOnClickListener(v -> {
            startActivity(new Intent(this, FavoriteProductsActivity.class));
        });
        
        // My Reviews
        layoutMyReviews.setOnClickListener(v -> {
            startActivity(new Intent(this, MyReviewsActivity.class));
        });
        
        // Account Settings
        layoutAccountSettings.setOnClickListener(v -> {
            startActivity(new Intent(this, AccountSettingsActivity.class));
        });
        
        // Help & Support
        layoutHelpSupport.setOnClickListener(v -> {
            startActivity(new Intent(this, HelpSupportActivity.class));
        });
    }
    
    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_cart) {
                startActivity(new Intent(this, CartActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_orders) {
                startActivity(new Intent(this, OrdersActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_account) {
                return true;
            }
            return false;
        });
        bottomNav.setSelectedItemId(R.id.nav_account);
    }
    
    private void loadUserInfo() {
        SharedPrefManager prefManager = SharedPrefManager.getInstance(this);
        User user = prefManager.getUser();
        
        if (user != null) {
            // Set username - if customer has name, use it, otherwise use username
            String displayName = user.getUsername();
            if (customer != null && customer.getName() != null && !customer.getName().isEmpty()) {
                displayName = customer.getName();
            }
            tvUserName.setText(displayName);
            
            // Set email
            String email = user.getEmail();
            if (email == null || email.isEmpty()) {
                if (customer != null && customer.getEmail() != null && !customer.getEmail().isEmpty()) {
                    email = customer.getEmail();
                } else {
                    email = "Chưa có email";
                }
            }
            tvUserEmail.setText(email);
        }
    }
    
    private void loadCustomerInfo() {
        SharedPrefManager prefManager = SharedPrefManager.getInstance(this);
        String rawToken = prefManager.getToken();
        if (rawToken == null || rawToken.isEmpty()) {
            return;
        }
        final String token = !rawToken.startsWith("Bearer ") 
            ? "Bearer " + rawToken 
            : rawToken;
        
        User user = prefManager.getUser();
        if (user == null || user.getEmail() == null) {
            return;
        }
        
        // Load customer info from SharedPreferences instead of API
        android.content.SharedPreferences prefs = getSharedPreferences("shophang_pref", MODE_PRIVATE);
        String profileName = prefs.getString("profile_name", null);
        String profileEmail = prefs.getString("profile_email", null);
        String profilePhone = prefs.getString("profile_phone", null);
        
        if (profileName != null || profileEmail != null || profilePhone != null) {
            // Create a simple customer object from saved data
            customer = new Customer(
                profileName != null ? profileName : user.getUsername(),
                profileEmail != null ? profileEmail : user.getEmail(),
                profilePhone != null ? profilePhone : "",
                prefs.getString("delivery_address", "")
            );
            loadUserInfo(); // Reload to show customer name
        }
    }
    
    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Đăng xuất")
            .setMessage("Bạn có chắc chắn muốn đăng xuất?")
            .setPositiveButton("Có", (dialog, which) -> logout())
            .setNegativeButton("Không", null)
            .show();
    }
    
    private void logout() {
        SharedPrefManager.getInstance(this).logout();
        Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
    
    private void showImagePickerDialog() {
        String[] options = {"Chọn từ thư viện", "Chụp ảnh", "Hủy"};
        new AlertDialog.Builder(this)
            .setTitle("Chọn ảnh đại diện")
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    // Choose from gallery
                    pickImageFromGallery();
                } else if (which == 1) {
                    // Take photo
                    takePhoto();
                }
            })
            .show();
    }
    
    private void pickImageFromGallery() {
        // Use ACTION_GET_CONTENT to access all image sources including file managers
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        // Add flag to allow persistent URI access
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        
        // Also try ACTION_PICK as fallback for better gallery integration
        Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        
        // Create chooser with both options
        Intent chooserIntent = Intent.createChooser(intent, "Chọn ảnh đại diện");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{pickIntent});
        
        startActivityForResult(chooserIntent, REQUEST_CODE_GALLERY);
    }
    
    private void takePhoto() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_CODE_CAMERA);
        } else {
            Toast.makeText(this, "Không thể mở camera", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == REQUEST_CODE_GALLERY) {
                Uri selectedImage = data.getData();
                if (selectedImage != null) {
                    // Grant persistent permission to read the image
                    try {
                        getContentResolver().takePersistableUriPermission(selectedImage, 
                            Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (SecurityException e) {
                        // Ignore if permission already granted or not needed
                    }
                    saveProfileImage(selectedImage);
                }
            } else if (requestCode == REQUEST_CODE_CAMERA) {
                // Get bitmap from camera result
                if (data.getExtras() != null) {
                    Bitmap photo = (Bitmap) data.getExtras().get("data");
                    if (photo != null) {
                        saveProfileImageBitmap(photo);
                    }
                }
            }
        }
    }
    
    private void saveProfileImage(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) {
                inputStream.close();
            }
            
            if (bitmap != null) {
                saveProfileImageBitmap(bitmap);
            }
        } catch (IOException e) {
            Toast.makeText(this, "Lỗi khi lưu ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void saveProfileImageBitmap(Bitmap bitmap) {
        try {
            // Save to internal storage
            File file = new File(getFilesDir(), "profile_image.jpg");
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush();
            fos.close();
            
            // Save path to SharedPreferences
            SharedPreferences prefs = getSharedPreferences("shophang_pref", MODE_PRIVATE);
            prefs.edit().putString("profile_image_path", file.getAbsolutePath()).apply();
            
            // Display the image
            ivProfileImage.setImageBitmap(bitmap);
            Toast.makeText(this, "Đã cập nhật ảnh đại diện", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "Lỗi khi lưu ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void loadProfileImage() {
        SharedPreferences prefs = getSharedPreferences("shophang_pref", MODE_PRIVATE);
        String imagePath = prefs.getString("profile_image_path", null);
        
        if (imagePath != null) {
            File file = new File(imagePath);
            if (file.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                if (bitmap != null) {
                    ivProfileImage.setImageBitmap(bitmap);
                }
            }
        }
    }
    
    private void viewProfileImage() {
        SharedPreferences prefs = getSharedPreferences("shophang_pref", MODE_PRIVATE);
        String imagePath = prefs.getString("profile_image_path", null);
        
        if (imagePath != null) {
            File file = new File(imagePath);
            if (file.exists()) {
                Intent intent = new Intent(this, ViewProfileImageActivity.class);
                intent.putExtra("image_path", imagePath);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Không tìm thấy ảnh đại diện", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Chưa có ảnh đại diện", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showProfileImageMenu() {
        SharedPreferences prefs = getSharedPreferences("shophang_pref", MODE_PRIVATE);
        String imagePath = prefs.getString("profile_image_path", null);
        boolean hasImage = imagePath != null && new File(imagePath).exists();
        
        if (hasImage) {
            String[] options = {"Xem ảnh", "Đổi ảnh", "Xóa ảnh", "Hủy"};
            new AlertDialog.Builder(this)
                .setTitle("Ảnh đại diện")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        viewProfileImage();
                    } else if (which == 1) {
                        showImagePickerDialog();
                    } else if (which == 2) {
                        deleteProfileImage();
                    }
                })
                .show();
        } else {
            showImagePickerDialog();
        }
    }
    
    private void deleteProfileImage() {
        new AlertDialog.Builder(this)
            .setTitle("Xóa ảnh đại diện")
            .setMessage("Bạn có chắc chắn muốn xóa ảnh đại diện?")
            .setPositiveButton("Xóa", (dialog, which) -> {
                SharedPreferences prefs = getSharedPreferences("shophang_pref", MODE_PRIVATE);
                String imagePath = prefs.getString("profile_image_path", null);
                
                if (imagePath != null) {
                    File file = new File(imagePath);
                    if (file.exists()) {
                        file.delete();
                    }
                }
                
                prefs.edit().remove("profile_image_path").apply();
                ivProfileImage.setImageResource(0);
                ivProfileImage.setBackgroundResource(R.drawable.profile_placeholder);
                Toast.makeText(this, "Đã xóa ảnh đại diện", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Hủy", null)
            .show();
    }
}
