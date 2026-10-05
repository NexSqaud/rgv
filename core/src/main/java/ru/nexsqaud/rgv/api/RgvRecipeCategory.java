package ru.nexsqaud.rgv.api;

import java.util.Objects;

/**
 * Defines a category of recipes (e.g. Crafting, Smelting, Brewing, Compressor).
 */
public class RgvRecipeCategory {

    private final String id;
    private final String title;
    private final RgvIngredient icon;
    private final int displayWidth;
    private final int displayHeight;

    public RgvRecipeCategory(String id, String title, RgvIngredient icon) {
        this(id, title, icon, 140, 60);
    }

    public RgvRecipeCategory(String id, String title, RgvIngredient icon, int displayWidth, int displayHeight) {
        this.id = Objects.requireNonNull(id, "category id cannot be null");
        this.title = title != null ? title : id;
        this.icon = icon != null ? icon : RgvStack.empty();
        this.displayWidth = displayWidth;
        this.displayHeight = displayHeight;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public RgvIngredient getIcon() {
        return icon;
    }

    public int getDisplayWidth() {
        return displayWidth;
    }

    public int getDisplayHeight() {
        return displayHeight;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RgvRecipeCategory)) return false;
        RgvRecipeCategory category = (RgvRecipeCategory) o;
        return Objects.equals(id, category.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "RgvRecipeCategory{" + id + ", '" + title + "'}";
    }
}
