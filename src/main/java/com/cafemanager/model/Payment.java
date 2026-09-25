package com.cafemanager.model;

import java.time.LocalDateTime;

public class Payment {

    private long id;
    private long orderId;
    private PaymentMethod method;
    private long amountDueCents;
    private long amountReceivedCents;
    private long changeCents;
    private LocalDateTime paidAt;
    private Long cashierId;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getOrderId() { return orderId; }
    public void setOrderId(long orderId) { this.orderId = orderId; }
    public PaymentMethod getMethod() { return method; }
    public void setMethod(PaymentMethod method) { this.method = method; }
    public long getAmountDueCents() { return amountDueCents; }
    public void setAmountDueCents(long amountDueCents) { this.amountDueCents = amountDueCents; }
    public long getAmountReceivedCents() { return amountReceivedCents; }
    public void setAmountReceivedCents(long amountReceivedCents) { this.amountReceivedCents = amountReceivedCents; }
    public long getChangeCents() { return changeCents; }
    public void setChangeCents(long changeCents) { this.changeCents = changeCents; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public Long getCashierId() { return cashierId; }
    public void setCashierId(Long cashierId) { this.cashierId = cashierId; }
}
