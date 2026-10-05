package ru.nexsqaud.rgv.core.recipe;

import ru.nexsqaud.rgv.api.*;

import java.util.*;

/**
 * Manages all loaded recipes, categories, workstations, and transfer handlers.
 */
public class RgvRecipeManager implements RgvRegistry {

    private final List<RgvRecipeCategory> categories = new ArrayList<>();
    private final Map<String, RgvRecipeCategory> categoryMap = new HashMap<>();
    private final List<RgvRecipe> allRecipes = new ArrayList<>();
    private final Map<String, List<RgvRecipe>> recipesByCategory = new HashMap<>();
    private final Map<String, List<RgvIngredient>> workstations = new HashMap<>();
    private final List<RgvRecipeHandler<?>> recipeHandlers = new ArrayList<>();
    private final List<RgvExclusionZone> exclusionZones = new ArrayList<>();
    private final List<RgvStackProvider> stackProviders = new ArrayList<>();

    @Override
    public void addCategory(RgvRecipeCategory category) {
        if (category == null) return;
        if (!categoryMap.containsKey(category.getId())) {
            categories.add(category);
            categoryMap.put(category.getId(), category);
            recipesByCategory.put(category.getId(), new ArrayList<>());
        }
    }

    @Override
    public void addWorkstation(String categoryId, RgvIngredient workstation) {
        if (categoryId == null || workstation == null) return;
        workstations.computeIfAbsent(categoryId, k -> new ArrayList<>()).add(workstation);
    }

    @Override
    public void addRecipe(RgvRecipe recipe) {
        if (recipe == null) return;
        allRecipes.add(recipe);

        RgvRecipeCategory cat = recipe.getCategory();
        if (cat != null) {
            addCategory(cat);
            recipesByCategory.computeIfAbsent(cat.getId(), k -> new ArrayList<>()).add(recipe);
        }
    }

    @Override
    public void addRecipeHandler(RgvRecipeHandler<?> handler) {
        if (handler != null) {
            recipeHandlers.add(handler);
        }
    }

    @Override
    public void addExclusionZone(RgvExclusionZone exclusionZone) {
        if (exclusionZone != null) {
            exclusionZones.add(exclusionZone);
        }
    }

    @Override
    public void addStackProvider(RgvStackProvider stackProvider) {
        if (stackProvider != null) {
            stackProviders.add(stackProvider);
        }
    }

    @Override
    public void removeRecipe(String recipeId) {
        if (recipeId == null) return;
        Iterator<RgvRecipe> it = allRecipes.iterator();
        while (it.hasNext()) {
            RgvRecipe r = it.next();
            if (Objects.equals(r.getId(), recipeId)) {
                it.remove();
                List<RgvRecipe> catList = recipesByCategory.get(r.getCategory().getId());
                if (catList != null) {
                    catList.remove(r);
                }
            }
        }
    }

    public List<RgvRecipeCategory> getCategories() {
        return Collections.unmodifiableList(categories);
    }

    public RgvRecipeCategory getCategory(String id) {
        return categoryMap.get(id);
    }

    public List<RgvRecipe> getAllRecipes() {
        return Collections.unmodifiableList(allRecipes);
    }

    public List<RgvRecipe> getRecipesByCategory(RgvRecipeCategory category) {
        if (category == null) return Collections.emptyList();
        List<RgvRecipe> list = recipesByCategory.get(category.getId());
        return list != null ? Collections.unmodifiableList(list) : Collections.emptyList();
    }

    public List<RgvIngredient> getWorkstations(RgvRecipeCategory category) {
        if (category == null) return Collections.emptyList();
        List<RgvIngredient> list = workstations.get(category.getId());
        return list != null ? Collections.unmodifiableList(list) : Collections.emptyList();
    }

    /**
     * Find recipes that PRODUCE this ingredient (recipes for 'R').
     */
    public List<RgvRecipe> getRecipesFor(RgvIngredient ingredient) {
        if (ingredient == null || ingredient.isEmpty()) return Collections.emptyList();
        List<RgvRecipe> result = new ArrayList<>();
        for (RgvRecipe recipe : allRecipes) {
            for (RgvStack output : recipe.getOutputs()) {
                if (ingredient.matches(output)) {
                    result.add(recipe);
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Find recipes that CONSUME this ingredient as input (uses for 'U').
     */
    public List<RgvRecipe> getUsesFor(RgvIngredient ingredient) {
        if (ingredient == null || ingredient.isEmpty()) return Collections.emptyList();
        List<RgvRecipe> result = new ArrayList<>();
        for (RgvRecipe recipe : allRecipes) {
            for (RgvIngredient input : recipe.getInputs()) {
                if (input == null || input.isEmpty()) continue;
                for (RgvStack s : ingredient.getRgvStacks()) {
                    if (input.matches(s)) {
                        result.add(recipe);
                        break;
                    }
                }
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public <C> RgvRecipeHandler<C> getHandler(C container) {
        if (container == null) return null;
        for (RgvRecipeHandler<?> handler : recipeHandlers) {
            try {
                if (((RgvRecipeHandler<C>) handler).canHandle(container)) {
                    return (RgvRecipeHandler<C>) handler;
                }
            } catch (ClassCastException ignored) {
            }
        }
        return null;
    }

    public List<RgvExclusionZone> getExclusionZones() {
        return Collections.unmodifiableList(exclusionZones);
    }

    public List<RgvStackProvider> getStackProviders() {
        return Collections.unmodifiableList(stackProviders);
    }
}
