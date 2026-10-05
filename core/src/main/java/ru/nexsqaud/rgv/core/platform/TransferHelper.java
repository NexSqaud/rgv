package ru.nexsqaud.rgv.core.platform;

import ru.nexsqaud.rgv.api.RgvIngredient;
import ru.nexsqaud.rgv.api.RgvRecipe;

import java.util.List;

public final class TransferHelper {

    private TransferHelper() {}

    public static boolean isSmeltingRecipe(RgvRecipe recipe) {
        if (recipe == null || recipe.getCategory() == null) return false;
        String id = recipe.getCategory().getId() != null ? recipe.getCategory().getId().toLowerCase() : "";
        String title = recipe.getCategory().getTitle() != null ? recipe.getCategory().getTitle().toLowerCase() : "";
        return id.contains("smelt") || id.contains("furnace") || title.contains("smelt");
    }

    public static boolean isCraftingRecipe(RgvRecipe recipe) {
        if (recipe == null || recipe.getCategory() == null) return false;
        String id = recipe.getCategory().getId() != null ? recipe.getCategory().getId().toLowerCase() : "";
        String title = recipe.getCategory().getTitle() != null ? recipe.getCategory().getTitle().toLowerCase() : "";
        return id.contains("craft") || title.contains("craft");
    }

    public static boolean canFitIn2x2(RgvRecipe recipe) {
        if (recipe == null) return false;
        List<RgvIngredient> inputs = recipe.getInputs();
        if (inputs == null || inputs.size() <= 4) return true;
        int[] outOfBounds = {2, 5, 6, 7, 8};
        for (int idx : outOfBounds) {
            if (idx < inputs.size()) {
                RgvIngredient ing = inputs.get(idx);
                if (ing != null && !ing.isEmpty()) return false;
            }
        }
        return true;
    }

    public static String getRequiredContainerDescription(RgvRecipe recipe) {
        if (recipe == null) return "suitable container";
        if (isSmeltingRecipe(recipe)) {
            return "furnace";
        }
        if (isCraftingRecipe(recipe)) {
            if (canFitIn2x2(recipe)) {
                return "2x2 crafting grid";
            }
            return "3x3 crafting grid";
        }
        if (recipe.getCategory() != null && recipe.getCategory().getTitle() != null && !recipe.getCategory().getTitle().isEmpty()) {
            return recipe.getCategory().getTitle();
        }
        if (recipe.getInputs() != null && recipe.getInputs().size() <= 9) {
            if (canFitIn2x2(recipe)) {
                return "2x2 crafting grid";
            }
            return "3x3 crafting grid";
        }
        return "suitable container";
    }
}
