package ru.nexsqaud.rgv.api;

/**
 * Main registration API passed to RgvPlugin instances during initialization.
 */
public interface RgvRegistry {

    void addCategory(RgvRecipeCategory category);

    void addWorkstation(String categoryId, RgvIngredient workstation);

    void addRecipe(RgvRecipe recipe);

    void addRecipeHandler(RgvRecipeHandler<?> handler);

    void addExclusionZone(RgvExclusionZone exclusionZone);

    void addStackProvider(RgvStackProvider stackProvider);

    void removeRecipe(String recipeId);
}
