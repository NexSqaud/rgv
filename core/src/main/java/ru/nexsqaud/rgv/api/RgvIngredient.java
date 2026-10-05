package ru.nexsqaud.rgv.api;

import java.util.List;

/**
 * Common interface representing an item, fluid, tag, or composite ingredient.
 * Completely decoupled from Minecraft's ItemStack or Forge types.
 */
public interface RgvIngredient {

    List<RgvStack> getRgvStacks();

    long getAmount();

    boolean isEmpty();

    String getDisplayName();

    List<String> getTooltip();

    void render(RgvDrawContext context, int x, int y, float delta);

    boolean matches(RgvStack stack);

    RgvIngredient copyWithAmount(long newAmount);
}
