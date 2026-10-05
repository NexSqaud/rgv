package ru.nexsqaud.rgv.platform.forge1710.compat.jei;

import net.minecraft.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.nexsqaud.rgv.api.*;
import ru.nexsqaud.rgv.api.widget.RgvWidgetHolder;
import ru.nexsqaud.rgv.platform.forge1710.Forge1710Platform;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Dynamic, reflection-safe bridge to import recipes from JEI / HadEnoughItems into RGV.
 * If JEI is present, queries its IRecipeRegistry and registers all categories and recipes.
 */
@RgvEntrypoint
public class JeiRecipeIntegration implements RgvPlugin {

    private static final Logger LOG = LogManager.getLogger("RGV-JEI");

    @Override
    public void register(RgvRegistry registry) {
        try {
            Class<?> jeiClass = null;
            try {
                jeiClass = Class.forName("mezz.jei.JustEnoughItems");
            } catch (ClassNotFoundException e) {
                try {
                    jeiClass = Class.forName("mezz.jei.api.JEIPlugin");
                } catch (ClassNotFoundException ignored) {
                }
            }

            if (jeiClass == null) {
                LOG.info("JEI / HadEnoughItems not detected on classpath, skipping JEI recipe import.");
                return;
            }

            LOG.info("JEI detected! Attempting to import JEI recipe categories and recipes into RGV...");
            importJeiRecipes(registry, jeiClass);
        } catch (Throwable t) {
            LOG.warn("Failed to import JEI recipes: " + t.getMessage());
        }
    }

    private void importJeiRecipes(RgvRegistry registry, Class<?> jeiClass) {
        try {
            // Attempt to get recipe registry from JEI runtime
            Method getRecipeRegistryMethod = null;
            Object recipeRegistryObj = null;

            try {
                getRecipeRegistryMethod = jeiClass.getMethod("getRecipeRegistry");
                recipeRegistryObj = getRecipeRegistryMethod.invoke(null);
            } catch (Exception ignored) {
            }

            if (recipeRegistryObj == null) {
                LOG.info("JEI recipe registry not directly available yet, JEI integration ready.");
                return;
            }

            // Read categories from IRecipeRegistry: getRecipeCategories()
            Method getCategoriesMethod = recipeRegistryObj.getClass().getMethod("getRecipeCategories");
            @SuppressWarnings("unchecked")
            List<?> categories = (List<?>) getCategoriesMethod.invoke(recipeRegistryObj);

            if (categories == null || categories.isEmpty()) return;

            Method getRecipesMethod = null;
            for (Method m : recipeRegistryObj.getClass().getMethods()) {
                if (m.getName().equals("getRecipes") || m.getName().equals("getRecipeWrappers")) {
                    if (m.getParameterTypes().length == 1) {
                        getRecipesMethod = m;
                        break;
                    }
                }
            }

            int importedCount = 0;
            for (Object catObj : categories) {
                try {
                    Method getUid = catObj.getClass().getMethod("getUid");
                    Method getTitle = catObj.getClass().getMethod("getTitle");
                    String catId = (String) getUid.invoke(catObj);
                    String catTitle = (String) getTitle.invoke(catObj);

                    RgvRecipeCategory rgvCategory = new RgvRecipeCategory("jei." + catId, catTitle, RgvStack.empty());
                    registry.addCategory(rgvCategory);

                    if (getRecipesMethod != null) {
                        @SuppressWarnings("unchecked")
                        List<?> recipeWrappers = (List<?>) getRecipesMethod.invoke(recipeRegistryObj, catObj);
                        if (recipeWrappers != null) {
                            for (Object wrapper : recipeWrappers) {
                                RgvRecipe rgvRecipe = convertJeiRecipe(rgvCategory, wrapper, catId + "_" + (importedCount++));
                                if (rgvRecipe != null) {
                                    registry.addRecipe(rgvRecipe);
                                }
                            }
                        }
                    }
                } catch (Throwable catEx) {
                    LOG.debug("Could not process JEI category: " + catEx.getMessage());
                }
            }

            LOG.info("Successfully imported " + importedCount + " recipes from JEI!");
        } catch (Throwable t) {
            LOG.warn("Error during JEI recipe extraction: " + t.getMessage());
        }
    }

    private RgvRecipe convertJeiRecipe(RgvRecipeCategory category, Object wrapper, String id) {
        try {
            Method getInputsMethod = wrapper.getClass().getMethod("getInputs");
            Method getOutputsMethod = wrapper.getClass().getMethod("getOutputs");

            @SuppressWarnings("unchecked")
            List<?> rawInputs = (List<?>) getInputsMethod.invoke(wrapper);
            @SuppressWarnings("unchecked")
            List<?> rawOutputs = (List<?>) getOutputsMethod.invoke(wrapper);

            final List<RgvIngredient> inputs = new ArrayList<>();
            final List<RgvStack> outputs = new ArrayList<>();

            if (rawInputs != null) {
                for (Object in : rawInputs) {
                    RgvIngredient ing = parseRawIngredient(in);
                    if (ing != null && !ing.isEmpty()) {
                        inputs.add(ing);
                    }
                }
            }

            if (rawOutputs != null) {
                for (Object out : rawOutputs) {
                    if (out instanceof ItemStack) {
                        outputs.add(Forge1710Platform.toRgvStack((ItemStack) out));
                    }
                }
            }

            if (outputs.isEmpty()) return null;

            return new RgvRecipe() {
                @Override public String getId() { return "jei_" + id; }
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

    private RgvIngredient parseRawIngredient(Object obj) {
        if (obj == null) return RgvStack.empty();
        if (obj instanceof ItemStack) {
            return Forge1710Platform.toRgvStack((ItemStack) obj);
        }
        if (obj instanceof List) {
            @SuppressWarnings("unchecked")
            List<?> list = (List<?>) obj;
            List<RgvStack> stacks = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof ItemStack) {
                    stacks.add(Forge1710Platform.toRgvStack((ItemStack) item));
                }
            }
            return RgvIngredientList.of(stacks, 1);
        }
        return RgvStack.empty();
    }
}
