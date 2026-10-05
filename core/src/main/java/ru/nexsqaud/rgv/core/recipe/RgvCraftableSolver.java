package ru.nexsqaud.rgv.core.recipe;

import ru.nexsqaud.rgv.api.RgvInventory;
import ru.nexsqaud.rgv.api.RgvRecipe;
import ru.nexsqaud.rgv.api.RgvStack;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Calculates which items can currently be crafted from the player's inventory.
 * Used for the EMI-style "Craftable Only" filter toggle.
 */
public class RgvCraftableSolver {

    public static Set<RgvStack> findCraftableStacks(RgvRecipeManager recipeManager, RgvInventory inventory) {
        Set<RgvStack> craftable = new HashSet<>();
        if (recipeManager == null || inventory == null) {
            return craftable;
        }

        List<RgvRecipe> allRecipes = recipeManager.getAllRecipes();
        for (RgvRecipe recipe : allRecipes) {
            if (recipe.canCraft(inventory)) {
                for (RgvStack output : recipe.getOutputs()) {
                    if (!output.isEmpty()) {
                        craftable.add(output.copyWithAmount(1));
                    }
                }
            }
        }

        return craftable;
    }
}
