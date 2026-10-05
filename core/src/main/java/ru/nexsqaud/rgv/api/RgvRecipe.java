package ru.nexsqaud.rgv.api;

import ru.nexsqaud.rgv.api.widget.RgvWidgetHolder;

import java.util.Collections;
import java.util.List;

/**
 * Common recipe interface across all Minecraft versions.
 */
public interface RgvRecipe {

    String getId();

    RgvRecipeCategory getCategory();

    List<RgvIngredient> getInputs();

    List<RgvStack> getOutputs();

    default List<RgvIngredient> getCatalysts() {
        return Collections.emptyList();
    }

    default int getDisplayWidth() {
        return getCategory().getDisplayWidth();
    }

    default int getDisplayHeight() {
        return getCategory().getDisplayHeight();
    }

    void addWidgets(RgvWidgetHolder holder);

    default boolean canCraft(RgvInventory inventory) {
        if (inventory == null) return false;
        java.util.Map<String, Long> requiredCounts = new java.util.LinkedHashMap<>();
        java.util.Map<String, RgvIngredient> ingredientSample = new java.util.LinkedHashMap<>();

        for (RgvIngredient input : getInputs()) {
            if (input == null || input.isEmpty()) continue;
            List<RgvStack> stacks = input.getRgvStacks();
            if (stacks.isEmpty()) continue;
            RgvStack primary = stacks.get(0);
            String key = primary.getId() + ":" + primary.getMeta();
            requiredCounts.put(key, requiredCounts.getOrDefault(key, 0L) + Math.max(1, input.getAmount()));
            ingredientSample.putIfAbsent(key, input);
        }

        for (java.util.Map.Entry<String, Long> entry : requiredCounts.entrySet()) {
            RgvIngredient ing = ingredientSample.get(entry.getKey());
            if (inventory.getAmount(ing) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }
}
