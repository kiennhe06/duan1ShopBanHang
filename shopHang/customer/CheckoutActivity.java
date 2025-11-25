package com.shopHang.customer;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.shopHang.R;
import com.shopHang.adapters.CartAdapter;
import com.shopHang.adapters.VoucherAdapter;
import com.shopHang.api.ApiService;
import com.shopHang.api.RetrofitClient;
import com.shopHang.models.CartItem;
import com.shopHang.models.CreateOrderRequest;
import com.shopHang.models.Customer;
import com.shopHang.models.Order;
import com.shopHang.models.Voucher;
import com.shopHang.utils.CartManager;
import com.shopHang.utils.SharedPrefManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.ArrayList;
import java.util.List;

public class CheckoutActivity extends AppCompatActivity {
    private RecyclerView rvCartItems, rvVouchers;
    private CartAdapter cartAdapter;
    private VoucherAdapter voucherAdapter;
    private EditText etName, etPhone, etEmail, etAddress, etVoucherCode;
    private TextView tvSubtotal, tvShippingFee, tvDiscount, tvTotal, tvBottomTotal, tvAddress, tvVoucherMessage, tvVoucherCount, tvNoVouchers;
    private Button btnApplyVoucher;
    private String appliedVoucherCode = null;
    private double discountAmount = 0;
    private Button btnPlaceOrder;
    private ImageButton btnBack;
    private ProgressBar progressBar;
    private RadioButton rbCOD, rbOnline;
    private CardView cardPaymentCOD, cardPaymentOnline;
    private View layoutDiscount;
    private CartManager cartManager;
    private List<CartItem> selectedItems;
    private static final double SHIPPING_FEE = 30000; // 30.000₫
    private String selectedPaymentMethod = "cash"; // cash or online
    private LocationManager locationManager;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private boolean isActivityDestroyed = false;
    
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
        
        setContentView(R.layout.activity_checkout);
        
        cartManager = CartManager.getInstance(this);
        
        // Lấy danh sách sản phẩm đã chọn từ Intent
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("selectedItems")) {
            selectedItems = (List<CartItem>) intent.getSerializableExtra("selectedItems");
        } else {
            // Nếu không có, lấy tất cả từ cart (backward compatibility)
            selectedItems = cartManager.getCartItems();
        }
        
        initViews();
        setupPaymentMethods();
        
        // Reset voucher khi mở màn hình checkout mới
        appliedVoucherCode = null;
        discountAmount = 0;
        etVoucherCode.setText("");
        tvVoucherMessage.setVisibility(View.GONE);
        voucherAdapter.setSelectedVoucher(null);
        
        loadCartItems();
        loadVouchers();
        loadCustomerInfo();
        updateTotals();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        isActivityDestroyed = true;
    }
    
    private void initViews() {
        rvCartItems = findViewById(R.id.rvCartItems);
        rvVouchers = findViewById(R.id.rvVouchers);
        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvShippingFee = findViewById(R.id.tvShippingFee);
        tvTotal = findViewById(R.id.tvTotal);
        tvBottomTotal = findViewById(R.id.tvBottomTotal);
        tvAddress = findViewById(R.id.tvAddress);
        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etEmail = findViewById(R.id.etEmail);
        etAddress = findViewById(R.id.etAddress);
        etVoucherCode = findViewById(R.id.etVoucherCode);
        btnApplyVoucher = findViewById(R.id.btnApplyVoucher);
        tvDiscount = findViewById(R.id.tvDiscount);
        tvVoucherMessage = findViewById(R.id.tvVoucherMessage);
        tvVoucherCount = findViewById(R.id.tvVoucherCount);
        tvNoVouchers = findViewById(R.id.tvNoVouchers);
        layoutDiscount = findViewById(R.id.layoutDiscount);
        btnPlaceOrder = findViewById(R.id.btnPlaceOrder);
        btnBack = findViewById(R.id.btnBack);
        progressBar = findViewById(R.id.progressBar);
        
        // Setup Voucher RecyclerView
        rvVouchers.setLayoutManager(new LinearLayoutManager(this));
        voucherAdapter = new VoucherAdapter(new ArrayList<>(), voucher -> {
            // Khi chọn voucher từ danh sách
            applyVoucherByCode(voucher.getCode());
        });
        rvVouchers.setAdapter(voucherAdapter);
        
        // Voucher apply button
        btnApplyVoucher.setOnClickListener(v -> applyVoucher());
        
        // Back button
        btnBack.setOnClickListener(v -> finish());
        
        // Get location button
        TextView btnGetLocation = findViewById(R.id.btnGetLocation);
        btnGetLocation.setOnClickListener(v -> {
            // Lấy vị trí hiện tại
            getCurrentLocation();
        });
        
        // Change address button
        TextView btnChangeAddressText = findViewById(R.id.btnChangeAddress);
        btnChangeAddressText.setOnClickListener(v -> {
            // Hiển thị form nhập địa chỉ
            showAddressInputDialog();
        });
        
        // Initialize LocationManager
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        
        // Lưu thông tin khi người dùng thay đổi
        etName.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                saveCustomerInfo();
            }
        });
        
        etPhone.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                saveCustomerInfo();
            }
        });
        
        etEmail.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                saveCustomerInfo();
            }
        });
        
        // Update address display when user types
        etAddress.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String address = etAddress.getText().toString().trim();
                if (address.length() > 0) {
                    if (address.length() > 40) {
                        tvAddress.setText(address.substring(0, 37) + "...");
                    } else {
                        tvAddress.setText(address);
                    }
                    // Lưu thông tin
                    saveCustomerInfo();
                }
            }
        });
        
        // Setup RecyclerView for cart items
        rvCartItems.setLayoutManager(new LinearLayoutManager(this));
        cartAdapter = new CartAdapter(new CartAdapter.CartListener() {
            @Override
            public void onQuantityChanged() {
                updateTotals();
            }
            
            @Override
            public void onItemRemoved() {
                loadCartItems();
                updateTotals();
            }
            
            @Override
            public void onSelectionChanged() {
                updateTotals();
            }
        });
        rvCartItems.setAdapter(cartAdapter);
        
        btnPlaceOrder.setOnClickListener(v -> placeOrder());
    }
    
    private void setupPaymentMethods() {
        rbCOD = findViewById(R.id.rbCOD);
        rbOnline = findViewById(R.id.rbOnline);
        cardPaymentCOD = findViewById(R.id.cardPaymentCOD);
        cardPaymentOnline = findViewById(R.id.cardPaymentOnline);
        
        // COD selected by default
        rbCOD.setChecked(true);
        selectedPaymentMethod = "cash";
        
        // COD click
        cardPaymentCOD.setOnClickListener(v -> {
            rbCOD.setChecked(true);
            rbOnline.setChecked(false);
            selectedPaymentMethod = "cash";
        });
        
        // Online click
        cardPaymentOnline.setOnClickListener(v -> {
            rbOnline.setChecked(true);
            rbCOD.setChecked(false);
            selectedPaymentMethod = "online";
        });
        
        rbCOD.setOnClickListener(v -> {
            rbCOD.setChecked(true);
            rbOnline.setChecked(false);
            selectedPaymentMethod = "cash";
        });
        
        rbOnline.setOnClickListener(v -> {
            rbOnline.setChecked(true);
            rbCOD.setChecked(false);
            selectedPaymentMethod = "online";
        });
    }
    
    private void loadCartItems() {
        if (selectedItems != null) {
            cartAdapter.updateCartItems(selectedItems);
        }
    }
    
    private void loadCustomerInfo() {
        // Lấy thông tin khách hàng từ SharedPreferences
        SharedPreferences prefs = getSharedPreferences("shophang_pref", MODE_PRIVATE);
        
        // Load tên
        String savedName = prefs.getString("customer_name", null);
        if (savedName != null && !savedName.isEmpty()) {
            etName.setText(savedName);
        }
        
        // Load số điện thoại
        String savedPhone = prefs.getString("customer_phone", null);
        if (savedPhone != null && !savedPhone.isEmpty()) {
            etPhone.setText(savedPhone);
        }
        
        // Load email
        String savedEmail = prefs.getString("customer_email", null);
        if (savedEmail != null && !savedEmail.isEmpty()) {
            etEmail.setText(savedEmail);
        }
        
        // Load địa chỉ
        String savedAddress = prefs.getString("delivery_address", null);
        if (savedAddress != null && !savedAddress.isEmpty()) {
            if (savedAddress.length() > 40) {
                tvAddress.setText(savedAddress.substring(0, 37) + "...");
            } else {
                tvAddress.setText(savedAddress);
            }
            etAddress.setText(savedAddress);
        } else {
            tvAddress.setText("Chưa có địa chỉ");
        }
    }
    
    private void saveCustomerInfo() {
        // Lưu thông tin khách hàng vào SharedPreferences
        SharedPreferences prefs = getSharedPreferences("shophang_pref", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        
        if (!name.isEmpty()) {
            editor.putString("customer_name", name);
        }
        
        if (!phone.isEmpty()) {
            editor.putString("customer_phone", phone);
        }
        
        if (!email.isEmpty()) {
            editor.putString("customer_email", email);
        }
        
        if (!address.isEmpty()) {
            editor.putString("delivery_address", address);
        }
        
        editor.apply();
    }
    
    private void showAddressInputDialog() {
        // Hiển thị form nhập địa chỉ
        CardView cardAddressInput = findViewById(R.id.cardAddressInput);
        if (cardAddressInput.getVisibility() == View.GONE) {
            cardAddressInput.setVisibility(View.VISIBLE);
            etAddress.requestFocus();
        } else {
            cardAddressInput.setVisibility(View.GONE);
        }
    }
    
    private void getCurrentLocation() {
        // Kiểm tra quyền truy cập vị trí
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED
            && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            // Yêu cầu quyền
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, 
                           Manifest.permission.ACCESS_COARSE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        
        // Kiểm tra GPS có bật không
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) 
            && !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            Toast.makeText(this, "Vui lòng bật GPS hoặc vị trí để sử dụng tính năng này", 
                Toast.LENGTH_LONG).show();
            return;
        }
        
        // Hiển thị loading
        Toast.makeText(this, "Đang lấy vị trí...", Toast.LENGTH_SHORT).show();
        
        // Chạy trên background thread để tránh ANR
        new Thread(() -> {
            try {
                Location location = null;
                
                // Thử lấy vị trí cuối cùng đã biết trước (nhanh hơn)
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                        == PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) 
                        == PackageManager.PERMISSION_GRANTED) {
                    
                    if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                        location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    }
                    if (location == null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    }
                }
                
                // Nếu không có vị trí cuối cùng, yêu cầu cập nhật vị trí mới
                if (location == null) {
                    final Location[] result = new Location[1];
                    final Object lock = new Object();
                    final boolean[] isDone = {false};
                    
                    LocationListener locationListener = new LocationListener() {
                        @Override
                        public void onLocationChanged(Location loc) {
                            synchronized (lock) {
                                if (!isDone[0]) {
                                    result[0] = loc;
                                    isDone[0] = true;
                                    lock.notify();
                                }
                            }
                            // Dừng cập nhật vị trí
                            try {
                                locationManager.removeUpdates(this);
                            } catch (SecurityException e) {
                                e.printStackTrace();
                            }
                        }
                        
                        @Override
                        public void onStatusChanged(String provider, int status, Bundle extras) {}
                        
                        @Override
                        public void onProviderEnabled(String provider) {}
                        
                        @Override
                        public void onProviderDisabled(String provider) {}
                    };
                    
                    try {
                        // Yêu cầu cập nhật vị trí với timeout
                        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                        } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
                        }
                        
                        // Đợi tối đa 5 giây
                        synchronized (lock) {
                            if (!isDone[0]) {
                                lock.wait(5000);
                            }
                            if (isDone[0] && result[0] != null) {
                                location = result[0];
                            }
                            // Dừng cập nhật nếu chưa dừng
                            try {
                                locationManager.removeUpdates(locationListener);
                            } catch (SecurityException e) {
                                e.printStackTrace();
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        try {
                            locationManager.removeUpdates(locationListener);
                        } catch (SecurityException ex) {
                            ex.printStackTrace();
                        }
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }
                }
                
                // Lấy địa chỉ từ tọa độ (chạy trên background thread)
                if (location != null) {
                    final double lat = location.getLatitude();
                    final double lon = location.getLongitude();
                    // Chuyển về main thread để cập nhật UI
                    runOnUiThread(() -> getAddressFromLocation(lat, lon));
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Không thể lấy vị trí. Vui lòng thử lại hoặc nhập thủ công.", 
                            Toast.LENGTH_LONG).show();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Lỗi khi lấy vị trí: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
    
    private void getAddressFromLocation(double latitude, double longitude) {
        // Kiểm tra internet trước
        if (!isNetworkAvailable()) {
            if (!isActivityDestroyed) {
                runOnUiThread(() -> {
                    if (!isActivityDestroyed) {
                        Toast.makeText(this, "Không có kết nối internet. Vui lòng nhập địa chỉ thủ công.", 
                            Toast.LENGTH_LONG).show();
                        CardView cardAddressInput = findViewById(R.id.cardAddressInput);
                        if (cardAddressInput != null) {
                            cardAddressInput.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }
            return;
        }
        
        // Chạy geocoding trên background thread với timeout
        new Thread(() -> {
            try {
                // Kiểm tra Geocoder có khả dụng không
                if (!Geocoder.isPresent()) {
                    if (!isActivityDestroyed) {
                        runOnUiThread(() -> {
                            if (!isActivityDestroyed) {
                                Toast.makeText(this, "Dịch vụ địa chỉ không khả dụng. Vui lòng nhập thủ công.", 
                                    Toast.LENGTH_LONG).show();
                                CardView cardAddressInput = findViewById(R.id.cardAddressInput);
                                if (cardAddressInput != null) {
                                    cardAddressInput.setVisibility(View.VISIBLE);
                                }
                            }
                        });
                    }
                    return;
                }
                
                Geocoder geocoder = new Geocoder(this, java.util.Locale.getDefault());
                java.util.List<Address> addresses = null;
                
                // Thử lấy địa chỉ nhiều lần nếu cần
                int maxRetries = 2;
                for (int attempt = 0; attempt < maxRetries; attempt++) {
                    try {
                        addresses = geocoder.getFromLocation(latitude, longitude, 1);
                        if (addresses != null && !addresses.isEmpty()) {
                            break; // Thành công, thoát vòng lặp
                        }
                    } catch (java.io.IOException e) {
                        android.util.Log.w("CheckoutActivity", "Geocoder attempt " + (attempt + 1) + " failed: " + e.getMessage());
                        if (attempt < maxRetries - 1) {
                            // Đợi một chút trước khi thử lại
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                return;
                            }
                        } else {
                            // Lần thử cuối cùng thất bại, thử dùng locale khác
                            try {
                                geocoder = new Geocoder(this, java.util.Locale.ENGLISH);
                                addresses = geocoder.getFromLocation(latitude, longitude, 1);
                            } catch (Exception ex) {
                                android.util.Log.w("CheckoutActivity", "Geocoder with English locale also failed: " + ex.getMessage());
                            }
                        }
                    }
                }
                
                // Nếu vẫn không lấy được, thử với locale tiếng Việt
                if ((addresses == null || addresses.isEmpty()) && !isActivityDestroyed) {
                    try {
                        geocoder = new Geocoder(this, new java.util.Locale("vi", "VN"));
                        addresses = geocoder.getFromLocation(latitude, longitude, 1);
                    } catch (Exception e) {
                        android.util.Log.w("CheckoutActivity", "Geocoder with Vietnamese locale failed: " + e.getMessage());
                    }
                }
                
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    StringBuilder addressString = new StringBuilder();
                    
                    // Lấy địa chỉ đầy đủ
                    for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                        if (address.getAddressLine(i) != null) {
                            if (addressString.length() > 0) {
                                addressString.append(", ");
                            }
                            addressString.append(address.getAddressLine(i));
                        }
                    }
                    
                    // Nếu không có địa chỉ đầy đủ, ghép từ các phần
                    if (addressString.length() == 0) {
                        if (address.getThoroughfare() != null) {
                            addressString.append(address.getThoroughfare());
                        }
                        if (address.getSubLocality() != null) {
                            if (addressString.length() > 0) addressString.append(", ");
                            addressString.append(address.getSubLocality());
                        }
                        if (address.getLocality() != null) {
                            if (addressString.length() > 0) addressString.append(", ");
                            addressString.append(address.getLocality());
                        }
                        if (address.getAdminArea() != null) {
                            if (addressString.length() > 0) addressString.append(", ");
                            addressString.append(address.getAdminArea());
                        }
                    }
                    
                    final String fullAddress = addressString.toString();
                    // Chuyển về main thread để cập nhật UI
                    if (!isActivityDestroyed) {
                        runOnUiThread(() -> {
                            if (!isActivityDestroyed && etAddress != null && tvAddress != null) {
                                if (!fullAddress.isEmpty()) {
                                    // Cập nhật địa chỉ
                                    etAddress.setText(fullAddress);
                                    if (fullAddress.length() > 40) {
                                        tvAddress.setText(fullAddress.substring(0, 37) + "...");
                                    } else {
                                        tvAddress.setText(fullAddress);
                                    }
                                    // Lưu địa chỉ
                                    saveCustomerInfo();
                                    // Hiển thị form nhập địa chỉ để người dùng có thể chỉnh sửa
                                    CardView cardAddressInput = findViewById(R.id.cardAddressInput);
                                    if (cardAddressInput != null) {
                                        cardAddressInput.setVisibility(View.VISIBLE);
                                    }
                                    Toast.makeText(this, "Đã lấy vị trí thành công! Bạn có thể chỉnh sửa nếu cần.", 
                                        Toast.LENGTH_SHORT).show();
                                } else {
                                    // Nếu không có địa chỉ, yêu cầu nhập thủ công
                                    CardView cardAddressInput = findViewById(R.id.cardAddressInput);
                                    if (cardAddressInput != null) {
                                        cardAddressInput.setVisibility(View.VISIBLE);
                                    }
                                    Toast.makeText(this, "Không thể lấy địa chỉ. Vui lòng nhập thủ công.", 
                                        Toast.LENGTH_LONG).show();
                                }
                            }
                        });
                    }
                } else {
                    // Không tìm thấy địa chỉ
                    if (!isActivityDestroyed) {
                        runOnUiThread(() -> {
                            if (!isActivityDestroyed && etAddress != null) {
                                CardView cardAddressInput = findViewById(R.id.cardAddressInput);
                                if (cardAddressInput != null) {
                                    cardAddressInput.setVisibility(View.VISIBLE);
                                }
                                Toast.makeText(this, "Không tìm thấy địa chỉ. Vui lòng nhập thủ công.", 
                                    Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (!isActivityDestroyed) {
                    runOnUiThread(() -> {
                        if (!isActivityDestroyed && etAddress != null) {
                            CardView cardAddressInput = findViewById(R.id.cardAddressInput);
                            if (cardAddressInput != null) {
                                cardAddressInput.setVisibility(View.VISIBLE);
                            }
                            Toast.makeText(this, "Lỗi khi lấy địa chỉ. Vui lòng nhập thủ công.", 
                                Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }
    
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = 
            (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Quyền được cấp, lấy vị trí
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Cần quyền truy cập vị trí để sử dụng tính năng này", 
                    Toast.LENGTH_LONG).show();
            }
        }
    }
    
    private void updateTotals() {
        // Luôn lấy lại selectedItems từ adapter để đảm bảo dữ liệu mới nhất
        if (cartAdapter != null) {
            selectedItems = cartAdapter.getSelectedItems();
        }
        
        if (selectedItems == null || selectedItems.isEmpty()) {
            // Nếu không có items, set về 0
            tvSubtotal.setText("0₫");
            tvShippingFee.setText(String.format("%,.0f₫", SHIPPING_FEE));
            if (layoutDiscount != null) {
                layoutDiscount.setVisibility(View.GONE);
            }
            tvTotal.setText("0₫");
            tvBottomTotal.setText("0₫");
            return;
        }
        
        // Tính lại subtotal từ đầu
        double subtotal = 0;
        for (CartItem item : selectedItems) {
            subtotal += item.getSubtotal();
        }
        
        // Tính total với discount
        double total = subtotal + SHIPPING_FEE - discountAmount;
        if (total < 0) total = 0;
        
        // Cập nhật UI
        tvSubtotal.setText(String.format("%,.0f₫", subtotal));
        tvShippingFee.setText(String.format("%,.0f₫", SHIPPING_FEE));
        
        // Hiển thị discount nếu có
        if (discountAmount > 0 && layoutDiscount != null) {
            layoutDiscount.setVisibility(View.VISIBLE);
            tvDiscount.setText(String.format("-%s", String.format("%,.0f₫", discountAmount)));
        } else {
            if (layoutDiscount != null) {
                layoutDiscount.setVisibility(View.GONE);
            }
        }
        
        tvTotal.setText(String.format("%,.0f₫", total));
        tvBottomTotal.setText(String.format("%,.0f₫", total));
        
        android.util.Log.d("CheckoutActivity", String.format("updateTotals: subtotal=%,.0f, shipping=%,.0f, discount=%,.0f, total=%,.0f", 
            subtotal, SHIPPING_FEE, discountAmount, total));
    }
    
    private void loadVouchers() {
        SharedPrefManager prefManager = SharedPrefManager.getInstance(this);
        String rawToken = prefManager.getToken();
        if (rawToken == null || rawToken.isEmpty()) {
            android.util.Log.e("CheckoutActivity", "Token is null or empty");
            if (tvNoVouchers != null && rvVouchers != null) {
                tvNoVouchers.setVisibility(View.VISIBLE);
                tvNoVouchers.setText("Vui lòng đăng nhập để xem voucher");
                rvVouchers.setVisibility(View.GONE);
            }
            return;
        }
        
        // Đảm bảo token có format đúng
        final String token = !rawToken.startsWith("Bearer ") 
            ? "Bearer " + rawToken 
            : rawToken;
        
        // Voucher API is not available in current implementation
        // Commenting out voucher loading
        /*
        ApiService apiService = RetrofitClient.getInstance().getApiService();
        Call<List<Voucher>> call = apiService.getVouchers(token);
        call.enqueue(new Callback<List<Voucher>>() {
            @Override
            public void onResponse(Call<List<Voucher>> call, Response<List<Voucher>> response) {
                if (isActivityDestroyed) return;
                
                if (response.isSuccessful() && response.body() != null) {
                    List<Voucher> vouchers = response.body();
                    android.util.Log.d("CheckoutActivity", "Loaded " + (vouchers != null ? vouchers.size() : 0) + " vouchers");
                    
                    if (voucherAdapter != null) {
                        voucherAdapter.updateVouchers(vouchers);
                        if (appliedVoucherCode != null) {
                            voucherAdapter.setSelectedVoucher(appliedVoucherCode);
                        }
                    }
                    
                    // Hiển thị số lượng voucher
                    if (tvVoucherCount != null) {
                        if (vouchers != null && !vouchers.isEmpty()) {
                            tvVoucherCount.setText("(" + vouchers.size() + " voucher)");
                            tvVoucherCount.setVisibility(View.VISIBLE);
                        } else {
                            tvVoucherCount.setVisibility(View.GONE);
                        }
                    }
                    
                    // Hiển thị thông báo khi không có voucher
                    if (tvNoVouchers != null && rvVouchers != null) {
                        if (vouchers == null || vouchers.isEmpty()) {
                            tvNoVouchers.setVisibility(View.VISIBLE);
                            tvNoVouchers.setText("Không có voucher nào khả dụng");
                            rvVouchers.setVisibility(View.GONE);
                        } else {
                            tvNoVouchers.setVisibility(View.GONE);
                            rvVouchers.setVisibility(View.VISIBLE);
                        }
                    }
                } else {
                    // Lỗi khi load vouchers
                    String errorMsg = "Không thể tải danh sách voucher";
                    if (response.errorBody() != null) {
                        try {
                            String errorBody = response.errorBody().string();
                            android.util.Log.e("CheckoutActivity", "Error response: " + errorBody);
                            errorMsg = "Lỗi: " + response.code();
                        } catch (Exception e) {
                            android.util.Log.e("CheckoutActivity", "Error reading error body: " + e.getMessage());
                            errorMsg = "Lỗi: " + response.code();
                        }
                    } else {
                        android.util.Log.e("CheckoutActivity", "Response not successful. Code: " + response.code());
                    }
                    
                    if (tvNoVouchers != null && rvVouchers != null) {
                        tvNoVouchers.setVisibility(View.VISIBLE);
                        tvNoVouchers.setText(errorMsg);
                        rvVouchers.setVisibility(View.GONE);
                    }
                }
            }
            
            @Override
            public void onFailure(Call<List<Voucher>> call, Throwable t) {
                if (isActivityDestroyed) return;
                android.util.Log.e("CheckoutActivity", "Error loading vouchers: " + t.getMessage());
                if (t.getCause() != null) {
                    android.util.Log.e("CheckoutActivity", "Cause: " + t.getCause().getMessage());
                }
                
                // Hiển thị thông báo lỗi
                if (tvNoVouchers != null && rvVouchers != null) {
                    tvNoVouchers.setVisibility(View.VISIBLE);
                    tvNoVouchers.setText("Không thể tải danh sách voucher. Vui lòng kiểm tra kết nối mạng.");
                    rvVouchers.setVisibility(View.GONE);
                }
                
                if (tvVoucherCount != null) {
                    tvVoucherCount.setVisibility(View.GONE);
                }
            }
        });
        */
        
        // Voucher API not available - hide voucher section
        if (tvNoVouchers != null && rvVouchers != null) {
            tvNoVouchers.setVisibility(View.VISIBLE);
            tvNoVouchers.setText("Chức năng voucher đang được phát triển");
            rvVouchers.setVisibility(View.GONE);
        }
        if (tvVoucherCount != null) {
            tvVoucherCount.setVisibility(View.GONE);
        }
    }
    
    private void applyVoucherByCode(String voucherCode) {
        if (voucherCode == null || voucherCode.isEmpty()) {
            android.util.Log.e("CheckoutActivity", "Voucher code is null or empty");
            return;
        }
        
        // Điền mã voucher vào ô input
        String code = voucherCode.toUpperCase().trim();
        android.util.Log.d("CheckoutActivity", "Applying voucher code: " + code);
        
        etVoucherCode.setText(code);
        etVoucherCode.setSelection(code.length()); // Đặt cursor ở cuối
        
        // Focus vào ô input để người dùng thấy rõ
        etVoucherCode.requestFocus();
        
        // Scroll đến ô input để người dùng thấy rõ mã đã được chọn
        etVoucherCode.post(() -> {
            ScrollView scrollView = findViewById(R.id.scrollView);
            if (scrollView != null) {
                // Sử dụng requestRectangleOnScreen để scroll đến view
                etVoucherCode.requestRectangleOnScreen(
                    new android.graphics.Rect(0, 0, etVoucherCode.getWidth(), etVoucherCode.getHeight()),
                    true
                );
            }
        });
        
        // Đợi một chút để đảm bảo text đã được set vào EditText, sau đó tự động áp dụng voucher
        etVoucherCode.postDelayed(() -> {
            // Kiểm tra lại xem text có đúng không
            String currentText = etVoucherCode.getText().toString().trim().toUpperCase();
            android.util.Log.d("CheckoutActivity", "Current text in EditText: " + currentText + ", Expected: " + code);
            
            if (currentText.equals(code)) {
                android.util.Log.d("CheckoutActivity", "Text matches, calling applyVoucher()");
                applyVoucher();
            } else {
                // Nếu text chưa đúng, set lại và thử lại
                android.util.Log.w("CheckoutActivity", "Text doesn't match, setting again");
                etVoucherCode.setText(code);
                etVoucherCode.postDelayed(() -> {
                    android.util.Log.d("CheckoutActivity", "Retrying applyVoucher()");
                    applyVoucher();
                }, 100);
            }
        }, 100);
    }
    
    private void applyVoucher() {
        String voucherCode = etVoucherCode.getText().toString().trim().toUpperCase();
        android.util.Log.d("CheckoutActivity", "applyVoucher() called with code: " + voucherCode);
        
        if (voucherCode.isEmpty()) {
            android.util.Log.e("CheckoutActivity", "Voucher code is empty in applyVoucher()");
            Toast.makeText(this, "Vui lòng nhập mã voucher", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Tính subtotal hiện tại
        if (selectedItems == null) {
            selectedItems = cartAdapter.getSelectedItems();
        }
        double subtotal = 0;
        for (CartItem item : selectedItems) {
            subtotal += item.getSubtotal();
        }
        
        btnApplyVoucher.setEnabled(false);
        btnApplyVoucher.setText("Đang kiểm tra...");
        progressBar.setVisibility(View.VISIBLE);
        
        ApiService apiService = RetrofitClient.getInstance().getApiService();
        String rawToken = SharedPrefManager.getInstance(this).getToken();
        if (rawToken == null || rawToken.isEmpty()) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            btnApplyVoucher.setEnabled(true);
            btnApplyVoucher.setText("Áp dụng");
            progressBar.setVisibility(View.GONE);
            return;
        }
        
        // Voucher API is not available in current implementation
        btnApplyVoucher.setEnabled(true);
        btnApplyVoucher.setText("Áp dụng");
        progressBar.setVisibility(View.GONE);
        tvVoucherMessage.setVisibility(View.VISIBLE);
        tvVoucherMessage.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        tvVoucherMessage.setText("✗ Chức năng voucher đang được phát triển");
        Toast.makeText(this, "Chức năng voucher đang được phát triển", Toast.LENGTH_SHORT).show();
        
        /*
        // Đảm bảo token có format đúng
        final String token = !rawToken.startsWith("Bearer ") 
            ? "Bearer " + rawToken 
            : rawToken;
        
        Call<com.shopHang.models.VoucherValidationResponse> call = 
            apiService.validateVoucher(token, voucherCode, subtotal);
        
        call.enqueue(new Callback<com.shopHang.models.VoucherValidationResponse>() {
            @Override
            public void onResponse(Call<com.shopHang.models.VoucherValidationResponse> call, 
                                 Response<com.shopHang.models.VoucherValidationResponse> response) {
                btnApplyVoucher.setEnabled(true);
                btnApplyVoucher.setText("Áp dụng");
                progressBar.setVisibility(View.GONE);
                
                if (response.isSuccessful() && response.body() != null) {
                    com.shopHang.models.VoucherValidationResponse validation = response.body();
                    if (validation.getVoucher() != null) {
                        appliedVoucherCode = voucherCode;
                        discountAmount = validation.getDiscountAmount();
                        
                        android.util.Log.d("CheckoutActivity", String.format("Voucher applied: code=%s, discountAmount=%,.0f", 
                            voucherCode, discountAmount));
                        
                        // Highlight voucher được chọn trong danh sách
                        if (voucherAdapter != null) {
                            voucherAdapter.setSelectedVoucher(voucherCode);
                        }
                        
                        tvVoucherMessage.setVisibility(View.VISIBLE);
                        tvVoucherMessage.setTextColor(getResources().getColor(R.color.cart_primary));
                        com.shopHang.models.Voucher voucher = validation.getVoucher();
                        String discountText = voucher.getDiscountType().equals("percentage") 
                            ? String.format("Giảm %s%%", (int)voucher.getDiscountValue())
                            : String.format("Giảm %s", String.format("%,.0f₫", voucher.getDiscountValue()));
                        tvVoucherMessage.setText("✓ " + discountText + " - " + (validation.getMessage() != null ? validation.getMessage() : "Áp dụng voucher thành công"));
                        
                        // Cập nhật số tiền ngay lập tức
                        updateTotals();
                        Toast.makeText(CheckoutActivity.this, "Áp dụng voucher thành công", Toast.LENGTH_SHORT).show();
                    } else {
                        tvVoucherMessage.setVisibility(View.VISIBLE);
                        tvVoucherMessage.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                        tvVoucherMessage.setText("✗ " + (validation.getMessage() != null ? validation.getMessage() : "Voucher không hợp lệ"));
                        appliedVoucherCode = null;
                        discountAmount = 0;
                        updateTotals();
                    }
                } else {
                    String errorMsg = "Voucher không hợp lệ";
                    if (response.errorBody() != null) {
                        try {
                            errorMsg = response.errorBody().string();
                        } catch (Exception e) {
                            errorMsg = "Lỗi: " + response.code();
                        }
                    }
                    tvVoucherMessage.setVisibility(View.VISIBLE);
                    tvVoucherMessage.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    tvVoucherMessage.setText("✗ " + errorMsg);
                    appliedVoucherCode = null;
                    discountAmount = 0;
                    updateTotals();
                }
            }
            
            @Override
            public void onFailure(Call<com.shopHang.models.VoucherValidationResponse> call, Throwable t) {
                btnApplyVoucher.setEnabled(true);
                btnApplyVoucher.setText("Áp dụng");
                progressBar.setVisibility(View.GONE);
                Toast.makeText(CheckoutActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        */
    }
    
    private String getSelectedPaymentMethod() {
        return selectedPaymentMethod;
    }
    
    private void placeOrder() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        
        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên và số điện thoại", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (address.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập địa chỉ giao hàng", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (selectedItems == null || selectedItems.isEmpty()) {
            Toast.makeText(this, "Không có sản phẩm để thanh toán", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Lưu tất cả thông tin khách hàng vào SharedPreferences
        saveCustomerInfo();
        
        progressBar.setVisibility(View.VISIBLE);
        btnPlaceOrder.setEnabled(false);
        btnPlaceOrder.setText("Đang xử lý...");
        
        createCustomerAndOrder(name, phone, email, address);
    }
    
    private void createCustomerAndOrder(String name, String phone, String email, String address) {
        // Save customer info to SharedPreferences
        android.content.SharedPreferences prefs = getSharedPreferences("shophang_pref", MODE_PRIVATE);
        prefs.edit()
            .putString("checkout_name", name)
            .putString("checkout_phone", phone)
            .putString("checkout_email", email)
            .putString("checkout_address", address)
            .apply();
        
        // Note: Order API is not available in the current implementation
        // This is a simplified version that only saves customer info
        progressBar.setVisibility(View.GONE);
        btnPlaceOrder.setEnabled(true);
        btnPlaceOrder.setText("Đặt Hàng");
        Toast.makeText(this, "Thông tin đã được lưu. Chức năng đặt hàng đang được phát triển.", Toast.LENGTH_LONG).show();
    }
    
    private void createOrder(String customerId) {
        SharedPrefManager prefManager = SharedPrefManager.getInstance(this);
        String rawToken = prefManager.getToken();
        if (rawToken == null || rawToken.isEmpty()) {
            progressBar.setVisibility(View.GONE);
            btnPlaceOrder.setEnabled(true);
            btnPlaceOrder.setText("Đặt Hàng");
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }
        final String token = !rawToken.startsWith("Bearer ") 
            ? "Bearer " + rawToken 
            : rawToken;
        
        ApiService apiService = RetrofitClient.getInstance().getApiService();
        
        List<CreateOrderRequest.OrderItemRequest> items = new ArrayList<>();
        for (CartItem cartItem : selectedItems) {
            items.add(new CreateOrderRequest.OrderItemRequest(
                cartItem.getProduct().getId(),
                cartItem.getQuantity()
            ));
        }
        
        String paymentMethod = getSelectedPaymentMethod();
        
        // Log để debug
        android.util.Log.d("CheckoutActivity", String.format("Creating order: customerId=%s, items=%d, paymentMethod=%s, voucherCode=%s, discountAmount=%,.0f", 
            customerId, items.size(), paymentMethod, appliedVoucherCode, discountAmount));
        
        // Truyền voucherCode và shippingFee lên server
        CreateOrderRequest orderRequest = new CreateOrderRequest(
            customerId, 
            items, 
            paymentMethod,
            appliedVoucherCode != null && !appliedVoucherCode.isEmpty() ? appliedVoucherCode : null, // Gửi mã voucher đã áp dụng lên server
            SHIPPING_FEE // Gửi phí vận chuyển lên server
        );
        // Order API is not available in current implementation
        // Show message instead
        progressBar.setVisibility(View.GONE);
        btnPlaceOrder.setEnabled(true);
        btnPlaceOrder.setText("Đặt Hàng");
        Toast.makeText(this, "Chức năng đặt hàng đang được phát triển", Toast.LENGTH_LONG).show();
        
        /*
        Call<Order> call = apiService.createOrder(token, orderRequest);
        
        call.enqueue(new Callback<Order>() {
            @Override
            public void onResponse(Call<Order> call, Response<Order> response) {
                progressBar.setVisibility(View.GONE);
                btnPlaceOrder.setEnabled(true);
                btnPlaceOrder.setText("Đặt Hàng");
                
                if (response.isSuccessful() && response.body() != null) {
                    android.util.Log.d("CheckoutActivity", "Order created successfully");
                    
                    // Xóa các sản phẩm đã thanh toán khỏi giỏ hàng
                    for (CartItem item : selectedItems) {
                        cartManager.removeFromCart(item.getProduct().getId());
                    }
                    
                    // Reset voucher sau khi đặt hàng thành công
                    appliedVoucherCode = null;
                    discountAmount = 0;
                    etVoucherCode.setText("");
                    tvVoucherMessage.setVisibility(View.GONE);
                    
                    // Cập nhật lại số tiền để đảm bảo UI đúng
                    updateTotals();
                    
                    // Reload danh sách voucher để cập nhật số lượt còn lại (nếu voucher đã hết lượt sẽ không hiển thị)
                    // Note: Màn hình sẽ finish() ngay sau đó nên reload này chỉ có tác dụng nếu người dùng quay lại
                    loadVouchers();
                    
                    String paymentMethod = getSelectedPaymentMethod();
                    String paymentMsg = "Đặt hàng thành công!";
                    if ("cash".equals(paymentMethod)) {
                        paymentMsg = "Đặt hàng thành công! Bạn sẽ thanh toán khi nhận hàng (COD).";
                    } else if ("online".equals(paymentMethod)) {
                        paymentMsg = "Đặt hàng thành công! Vui lòng thanh toán qua MoMo.";
                    }
                    
                    // Hiển thị thông báo thành công
                    Toast.makeText(CheckoutActivity.this, paymentMsg, Toast.LENGTH_LONG).show();
                    
                    // Chuyển đến màn hình đánh giá sau khi mua hàng
                    Intent reviewIntent = new Intent(CheckoutActivity.this, PostPurchaseReviewActivity.class);
                    reviewIntent.putExtra("order", response.body());
                    startActivity(reviewIntent);
                    finish();
                } else {
                    String errorMsg = "Đặt hàng thất bại";
                    if (response.errorBody() != null) {
                        try {
                            errorMsg = response.errorBody().string();
                        } catch (Exception e) {
                            errorMsg = "Lỗi: " + response.code();
                        }
                    }
                    Toast.makeText(CheckoutActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }
            
            @Override
            public void onFailure(Call<Order> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnPlaceOrder.setEnabled(true);
                btnPlaceOrder.setText("Đặt Hàng");
                Toast.makeText(CheckoutActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
        */
    }
}
