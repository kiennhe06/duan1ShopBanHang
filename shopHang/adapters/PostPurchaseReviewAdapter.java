package com.shopHang.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.shopHang.R;
import com.shopHang.models.Product;
import java.util.ArrayList;
import java.util.List;

public class PostPurchaseReviewAdapter extends RecyclerView.Adapter<PostPurchaseReviewAdapter.ViewHolder> {
    private List<Product> products;
    private List<ReviewData> reviewDataList;
    
    public static class ReviewData {
        private Product product;
        private int rating;
        private String comment;
        
        public ReviewData(Product product) {
            this.product = product;
            this.rating = 0;
            this.comment = "";
        }
        
        public Product getProduct() {
            return product;
        }
        
        public int getRating() {
            return rating;
        }
        
        public void setRating(int rating) {
            this.rating = rating;
        }
        
        public String getComment() {
            return comment;
        }
        
        public void setComment(String comment) {
            this.comment = comment;
        }
    }
    
    public PostPurchaseReviewAdapter(List<Product> products, Object listener) {
        this.products = new ArrayList<>(products);
        this.reviewDataList = new ArrayList<>();
        for (Product product : products) {
            reviewDataList.add(new ReviewData(product));
        }
    }
    
    public void updateProducts(List<Product> newProducts) {
        this.products = new ArrayList<>(newProducts);
        this.reviewDataList = new ArrayList<>();
        for (Product product : products) {
            reviewDataList.add(new ReviewData(product));
        }
        notifyDataSetChanged();
    }
    
    public List<ReviewData> getReviewData() {
        return reviewDataList;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_post_purchase_review, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = products.get(position);
        ReviewData reviewData = reviewDataList.get(position);
        
        holder.tvProductName.setText(product.getName());
        holder.tvProductPrice.setText(product.getFormattedPrice());
        
        holder.ratingBar.setRating(reviewData.getRating());
        holder.ratingBar.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            if (fromUser) {
                reviewData.setRating((int) rating);
            }
        });
        
        holder.etComment.setText(reviewData.getComment());
        holder.etComment.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(android.text.Editable s) {
                reviewData.setComment(s.toString().trim());
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return products.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvProductName, tvProductPrice;
        RatingBar ratingBar;
        EditText etComment;
        
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvProductPrice = itemView.findViewById(R.id.tvProductPrice);
            ratingBar = itemView.findViewById(R.id.ratingBar);
            etComment = itemView.findViewById(R.id.etComment);
        }
    }
}

