package com.shopHang.models;

public class StatusUpdateRequest {
    private String status;
    
    public StatusUpdateRequest(String status) {
        this.status = status;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
}

