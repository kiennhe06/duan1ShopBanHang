package com.shopHang.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ReviewResponse {
    @SerializedName("reviews")
    private List<Review> reviews;
    @SerializedName("averageRating")
    private double averageRating;
    @SerializedName("totalReviews")
    private int totalReviews;
    
    public ReviewResponse() {
    }
    
    public List<Review> getReviews() {
        return reviews;
    }
    
    public void setReviews(List<Review> reviews) {
        this.reviews = reviews;
    }
    
    public double getAverageRating() {
        return averageRating;
    }
    
    public void setAverageRating(double averageRating) {
        this.averageRating = averageRating;
    }
    
    public int getTotalReviews() {
        return totalReviews;
    }
    
    public void setTotalReviews(int totalReviews) {
        this.totalReviews = totalReviews;
    }
}

