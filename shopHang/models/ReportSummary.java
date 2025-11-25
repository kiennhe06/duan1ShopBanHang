package com.shopHang.models;

public class ReportSummary {
    private TodayReport today;
    private MonthReport thisMonth;
    private Totals totals;
    
    public TodayReport getToday() {
        return today;
    }
    
    public void setToday(TodayReport today) {
        this.today = today;
    }
    
    public MonthReport getThisMonth() {
        return thisMonth;
    }
    
    public void setThisMonth(MonthReport thisMonth) {
        this.thisMonth = thisMonth;
    }
    
    public Totals getTotals() {
        return totals;
    }
    
    public void setTotals(Totals totals) {
        this.totals = totals;
    }
    
    public static class TodayReport {
        private double revenue;
        private int orders;
        
        public double getRevenue() {
            return revenue;
        }
        
        public void setRevenue(double revenue) {
            this.revenue = revenue;
        }
        
        public int getOrders() {
            return orders;
        }
        
        public void setOrders(int orders) {
            this.orders = orders;
        }
    }
    
    public static class MonthReport {
        private double revenue;
        
        public double getRevenue() {
            return revenue;
        }
        
        public void setRevenue(double revenue) {
            this.revenue = revenue;
        }
    }
    
    public static class Totals {
        private int orders;
        private int customers;
        private int products;
        private int lowStockProducts;
        
        public int getOrders() {
            return orders;
        }
        
        public void setOrders(int orders) {
            this.orders = orders;
        }
        
        public int getCustomers() {
            return customers;
        }
        
        public void setCustomers(int customers) {
            this.customers = customers;
        }
        
        public int getProducts() {
            return products;
        }
        
        public void setProducts(int products) {
            this.products = products;
        }
        
        public int getLowStockProducts() {
            return lowStockProducts;
        }
        
        public void setLowStockProducts(int lowStockProducts) {
            this.lowStockProducts = lowStockProducts;
        }
    }
}

