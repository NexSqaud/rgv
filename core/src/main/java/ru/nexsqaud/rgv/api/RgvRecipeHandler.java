package ru.nexsqaud.rgv.api;

/**
 * Handles transferring recipe ingredients into crafting grids/containers
 * when the user clicks the '+' button in the recipe screen.
 *
 * @param <C> The container or screen type
 */
public interface RgvRecipeHandler<C> {

    boolean canHandle(C container);

    boolean canTransfer(RgvRecipe recipe, C container, RgvInventory inventory);

    void transfer(RgvRecipe recipe, C container, boolean maxCraft);
}
