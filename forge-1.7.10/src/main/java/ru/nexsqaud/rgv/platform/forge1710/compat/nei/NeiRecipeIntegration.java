package ru.nexsqaud.rgv.platform.forge1710.compat.nei;

import net.minecraft.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.nexsqaud.rgv.api.*;
import ru.nexsqaud.rgv.api.widget.RgvWidgetHolder;
import ru.nexsqaud.rgv.platform.forge1710.Forge1710Platform;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Dynamic, reflection-safe bridge to import recipes from NEI (Not Enough Items) into RGV.
 * Automatically pulls all 1.7.10 mod machine recipes (IC2, GregTech, EnderIO, Thermal Expansion, etc.).
 */
@RgvEntrypoint
public class NeiRecipeIntegration implements RgvPlugin {

    private static final Logger LOG = LogManager.getLogger("RGV-NEI");

    private static boolean imported = false;

    public static void ensureRecipesImported(RgvRegistry registry) {
        if (imported || registry == null) return;
        try {
            Class<?> guiCraftingRecipeClass = Class.forName("codechicken.nei.recipe.GuiCraftingRecipe");
            Field handlersField = guiCraftingRecipeClass.getField("craftinghandlers");
            @SuppressWarnings("unchecked")
            ArrayList<?> handlers = (ArrayList<?>) handlersField.get(null);
            if (handlers != null && !handlers.isEmpty()) {
                new NeiRecipeIntegration().importNeiRecipes(registry, guiCraftingRecipeClass);
                imported = true;
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void register(RgvRegistry registry) {
        try {
            Class<?> guiCraftingRecipeClass = null;
            try {
                guiCraftingRecipeClass = Class.forName("codechicken.nei.recipe.GuiCraftingRecipe");
            } catch (ClassNotFoundException e) {
                LOG.info("NEI not detected on classpath, skipping NEI recipe import.");
                return;
            }

            LOG.info("NEI detected! Importing mod machine recipes from NEI crafting handlers into RGV...");
            importNeiRecipes(registry, guiCraftingRecipeClass);
            imported = true;
        } catch (Throwable t) {
            LOG.warn("Failed to import NEI recipes: " + t.getMessage());
        }
    }

    private void importNeiRecipes(RgvRegistry registry, Class<?> guiCraftingRecipeClass) {
        try {
            Field handlersField = guiCraftingRecipeClass.getField("craftinghandlers");
            @SuppressWarnings("unchecked")
            ArrayList<?> handlers = (ArrayList<?>) handlersField.get(null);

            if (handlers == null || handlers.isEmpty()) {
                LOG.info("No NEI crafting handlers registered yet.");
                return;
            }

            int importedCount = 0;
            for (Object handler : handlers) {
                try {
                    Method getRecipeNameMethod = handler.getClass().getMethod("getRecipeName");
                    String recipeName = (String) getRecipeNameMethod.invoke(handler);
                    if (recipeName == null || recipeName.isEmpty()) recipeName = handler.getClass().getSimpleName();

                    // Skip vanilla crafting and furnace since RGV already registers high-fidelity vanilla handlers
                    if (recipeName.equalsIgnoreCase("Crafting") || recipeName.equalsIgnoreCase("Smelting")) {
                        continue;
                    }

                    String catId = "nei." + recipeName.toLowerCase().replaceAll("[^a-z0-9_]", "_");
                    RgvRecipeCategory category = new RgvRecipeCategory(catId, recipeName, RgvStack.empty());
                    registry.addCategory(category);

                    Method numRecipesMethod = handler.getClass().getMethod("numRecipes");
                    int num = (int) numRecipesMethod.invoke(handler);

                    Method getIngredientStacksMethod = handler.getClass().getMethod("getIngredientStacks", int.class);
                    Method getResultStackMethod = null;
                    try {
                        getResultStackMethod = handler.getClass().getMethod("getResultStack", int.class);
                    } catch (Exception ignored) {
                    }

                    for (int i = 0; i < num; i++) {
                        List<?> ingredientStacks = (List<?>) getIngredientStacksMethod.invoke(handler, i);
                        Object resultStackObj = getResultStackMethod != null ? getResultStackMethod.invoke(handler, i) : null;

                        RgvRecipe rgvRecipe = convertNeiRecipe(category, catId + "_" + (importedCount++), ingredientStacks, resultStackObj);
                        if (rgvRecipe != null) {
                            registry.addRecipe(rgvRecipe);
                        }
                    }
                } catch (Throwable hEx) {
                    LOG.debug("Could not process NEI handler: " + hEx.getMessage());
                }
            }

            LOG.info("Successfully imported " + importedCount + " recipes from NEI!");
        } catch (Throwable t) {
            LOG.warn("Error during NEI recipe extraction: " + t.getMessage());
        }
    }

    private RgvRecipe convertNeiRecipe(RgvRecipeCategory category, String id, List<?> ingredientStacks, Object resultObj) {
        try {
            final List<RgvIngredient> inputs = new ArrayList<>();
            final List<RgvStack> outputs = new ArrayList<>();

            if (resultObj != null) {
                RgvStack out = parsePositionedStack(resultObj);
                if (out != null && !out.isEmpty()) {
                    outputs.add(out);
                }
            }

            if (ingredientStacks != null) {
                for (Object pStack : ingredientStacks) {
                    RgvIngredient in = parsePositionedStackIngredient(pStack);
                    if (in != null && !in.isEmpty()) {
                        inputs.add(in);
                    }
                }
            }

            if (outputs.isEmpty()) return null;

            return new RgvRecipe() {
                @Override public String getId() { return "nei_" + id; }
                @Override public RgvRecipeCategory getCategory() { return category; }
                @Override public List<RgvIngredient> getInputs() { return inputs; }
                @Override public List<RgvStack> getOutputs() { return outputs; }

                @Override
                public void addWidgets(RgvWidgetHolder holder) {
                    int inX = 6;
                    int inY = 6;
                    for (int i = 0; i < Math.min(inputs.size(), 9); i++) {
                        holder.addSlot(inputs.get(i), inX + (i % 3) * 18, inY + (i / 3) * 18);
                    }
                    holder.addArrow(66, 22, true);
                    for (int i = 0; i < Math.min(outputs.size(), 3); i++) {
                        holder.addOutputSlot(outputs.get(i), 96 + i * 26, 18);
                    }
                }
            };
        } catch (Throwable t) {
            return null;
        }
    }

    private RgvStack parsePositionedStack(Object pStack) {
        if (pStack == null) return null;
        try {
            Field itemField = pStack.getClass().getField("item");
            ItemStack stack = (ItemStack) itemField.get(pStack);
            if (stack != null && stack.getItem() != null) {
                return Forge1710Platform.toRgvStack(stack);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private RgvIngredient parsePositionedStackIngredient(Object pStack) {
        if (pStack == null) return null;
        try {
            Field itemsField = pStack.getClass().getField("items");
            ItemStack[] items = (ItemStack[]) itemsField.get(pStack);
            if (items != null && items.length > 0) {
                List<RgvStack> stacks = new ArrayList<>();
                for (ItemStack s : items) {
                    if (s != null && s.getItem() != null) {
                        stacks.add(Forge1710Platform.toRgvStack(s));
                    }
                }
                return RgvIngredientList.of(stacks, 1);
            }
        } catch (Exception ignored) {
        }
        return parsePositionedStack(pStack);
    }
}
