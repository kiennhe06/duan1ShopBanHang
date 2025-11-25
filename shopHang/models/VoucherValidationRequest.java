package com.shopHang.models;

import com.google.gson.annotations.SerializedName;

public class VoucherValidationRequest {
    private String code;
    @SerializedName("totalAmount")
    private double totalAmount;

    public VoucherValidationRequest(String code, double totalAmount) {
        this.code = code;
        this.totalAmount = totalAmount;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }
}





