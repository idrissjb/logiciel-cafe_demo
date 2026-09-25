package com.cafemanager.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.cafemanager.util.Money;

/**
 * Commande / ticket. Toute la logique de calcul (lignes, remise, total) est ici :
 * l'interface ne fait qu'afficher ce que renvoie ce modèle.
 */
public class Order {

    private Long id;                       // null tant que la commande n'est pas enregistrée
    private String ticketNumber;           // "000125"
    private Long tableId;
    private String tableName;
    private Long serverId;
    private String serverName;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime paidAt;
    private OrderStatus status = OrderStatus.OPEN;
    private final List<OrderItem> items = new ArrayList<>();
    private DiscountMode discountMode = DiscountMode.NONE;
    private long discountValue;            // centimes (AMOUNT) ou pourcentage entier (PERCENT)
    private PaymentMethod paymentMethod;
    /** Total tel qu'enregistré en base (utile pour l'historique, où les lignes ne sont pas chargées). */
    private long storedTotal;

    // ---------------------------------------------------------------- lignes

    /** Ajoute 1 unité du produit ; si le produit est déjà dans le ticket, incrémente sa quantité. */
    public OrderItem addProduct(Product p) {
        Optional<OrderItem> existing = findItem(p.getId());
        if (existing.isPresent()) {
            existing.get().setQuantity(existing.get().getQuantity() + 1);
            return existing.get();
        }
        OrderItem item = new OrderItem(p.getId(), p.getName(), p.getPriceCents(), 1);
        items.add(item);
        return item;
    }

    public Optional<OrderItem> findItem(Long productId) {
        return items.stream().filter(i -> i.getProductId() != null && i.getProductId().equals(productId)).findFirst();
    }

    /** Modifie la quantité d'une ligne ; retourne false si la ligne a été supprimée (quantité <= 0). */
    public boolean changeQuantity(OrderItem item, int delta) {
        int q = item.getQuantity() + delta;
        if (q <= 0) {
            items.remove(item);
            return false;
        }
        item.setQuantity(q);
        return true;
    }

    public void removeItem(OrderItem item) {
        items.remove(item);
    }

    public void clearItems() {
        items.clear();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public int itemCount() {
        return items.stream().mapToInt(OrderItem::getQuantity).sum();
    }

    // --------------------------------------------------------------- montants

    public long subtotal() {
        return items.stream().mapToLong(OrderItem::lineTotal).sum();
    }

    public long discount() {
        long sub = subtotal();
        switch (discountMode) {
            case PERCENT:
                return Math.min(sub, Money.percentOf(sub, discountValue));
            case AMOUNT:
                return Math.min(sub, discountValue);
            default:
                return 0;
        }
    }

    public long total() {
        return subtotal() - discount();
    }

    public void setDiscount(DiscountMode mode, long value) {
        this.discountMode = mode == null ? DiscountMode.NONE : mode;
        this.discountValue = this.discountMode == DiscountMode.NONE ? 0 : Math.max(0, value);
    }

    public boolean isPersisted() {
        return id != null;
    }

    // ---------------------------------------------------------- getters/setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTicketNumber() { return ticketNumber; }
    public void setTicketNumber(String ticketNumber) { this.ticketNumber = ticketNumber; }
    public Long getTableId() { return tableId; }
    public void setTableId(Long tableId) { this.tableId = tableId; }
    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    public Long getServerId() { return serverId; }
    public void setServerId(Long serverId) { this.serverId = serverId; }
    public String getServerName() { return serverName; }
    public void setServerName(String serverName) { this.serverName = serverName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public List<OrderItem> getItems() { return items; }
    public List<OrderItem> getItemsView() { return Collections.unmodifiableList(items); }
    public DiscountMode getDiscountMode() { return discountMode; }
    public long getDiscountValue() { return discountValue; }
    public long getStoredTotal() { return storedTotal; }
    public void setStoredTotal(long storedTotal) { this.storedTotal = storedTotal; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
}
