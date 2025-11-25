package com.shopHang.models;

import com.google.gson.annotations.SerializedName;

public class VoucherValidationResponse {
    private Voucher voucher;
    @SerializedName("discountAmount")
    private double discountAmount;
    private String message;

    public Voucher getVoucher() {
        return voucher;
    }

    public void setVoucher(Voucher voucher) {
        this.voucher = voucher;
    }

    public double getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(double discountAmount) {
        this.discountAmount = discountAmount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

