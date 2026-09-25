package com.cafemanager.model;

/** Ligne de commande : un produit, une quantité, un prix unitaire figé au moment de la vente. */
public class OrderItem {

    private Long productId;
    private String productName;
    private long unitPriceCents;
    private int quantity;

    public OrderItem() {
    }

    public OrderItem(Long productId, String productName, long unitPriceCents, int quantity) {
        this.productId = productId;
        this.productName = productName;
        this.unitPriceCents = unitPriceCents;
        this.quantity = quantity;
    }

    public long lineTotal() {
        return unitPriceCents * quantity;
    }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public long getUnitPriceCents() { return unitPriceCents; }
    public void setUnitPriceCents(long unitPriceCents) { this.unitPriceCents = unitPriceCents; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
