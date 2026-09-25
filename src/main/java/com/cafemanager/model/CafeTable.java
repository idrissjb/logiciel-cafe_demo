package com.cafemanager.model;

import java.time.LocalDateTime;

/** Table (ou emplacement de terrasse) du café. */
public class CafeTable {

    private long id;
    private String name;
    private TableStatus status = TableStatus.LIBRE;
    private int displayOrder;

    // Informations de la commande en cours (remplies par le DAO, non persistées ici)
    private Long openOrderId;
    private long openOrderTotal;
    private int openOrderItems;
    private LocalDateTime openSince;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public TableStatus getStatus() { return status; }
    public void setStatus(TableStatus status) { this.status = status; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
    public Long getOpenOrderId() { return openOrderId; }
    public void setOpenOrderId(Long openOrderId) { this.openOrderId = openOrderId; }
    public long getOpenOrderTotal() { return openOrderTotal; }
    public void setOpenOrderTotal(long openOrderTotal) { this.openOrderTotal = openOrderTotal; }
    public int getOpenOrderItems() { return openOrderItems; }
    public void setOpenOrderItems(int openOrderItems) { this.openOrderItems = openOrderItems; }
    public LocalDateTime getOpenSince() { return openSince; }
    public void setOpenSince(LocalDateTime openSince) { this.openSince = openSince; }

    @Override
    public String toString() {
        return name;
    }
}
