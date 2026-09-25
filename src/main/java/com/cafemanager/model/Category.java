package com.cafemanager.model;

/** Catégorie de produits (Cafés, Thés, ...). */
public class Category {

    /** Catégorie virtuelle "Favoris" (non stockée en base) : regroupe les produits marqués favoris. */
    public static final long FAVORITES_ID = -1L;

    private long id;
    private String name;
    private String icon = "☕";
    private int displayOrder;
    private boolean active = true;

    public Category() {
    }

    public Category(long id, String name, String icon, int displayOrder, boolean active) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.displayOrder = displayOrder;
        this.active = active;
    }

    public static Category favorites() {
        return new Category(FAVORITES_ID, "Favoris", "⭐", -1, true);
    }

    public boolean isFavorites() {
        return id == FAVORITES_ID;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    /** Libellé affiché dans les listes déroulantes. */
    @Override
    public String toString() {
        return (icon == null || icon.isBlank() ? "" : icon + "  ") + name;
    }
}
