package ru.nexsqaud.rgv.platform.forge1122.compat.jei;

import mezz.jei.api.IJeiRuntime;
import mezz.jei.api.recipe.IFocus;
import net.minecraft.item.ItemStack;

import java.lang.reflect.Method;

public class JeiIntegration {

    private static IJeiRuntime jeiRuntime;

    public static void init(IJeiRuntime runtime) {
        jeiRuntime = runtime;
    }

    public static IJeiRuntime getJeiRuntime() {
        return jeiRuntime;
    }

    public static boolean isJeiPanelVisible() {
        if (jeiRuntime == null) return false;
        try {
            Object overlay = jeiRuntime.getIngredientListOverlay();
            if (overlay == null) return false;
            Method m = overlay.getClass().getMethod("isListDisplayed");
            return (boolean) m.invoke(overlay);
        } catch (Throwable t) {
            return true;
        }
    }

    public static boolean openRecipeGui(ItemStack stack, boolean isUsage) {
        if (jeiRuntime == null || stack == null || stack.isEmpty()) return false;
        try {
            ItemStack queryStack = stack.copy();
            queryStack.setCount(1);
            IFocus.Mode mode = isUsage ? IFocus.Mode.INPUT : IFocus.Mode.OUTPUT;
            IFocus<ItemStack> focus = jeiRuntime.getRecipeRegistry().createFocus(mode, queryStack);
            if (focus == null) return false;
            java.util.List<?> categories = jeiRuntime.getRecipeRegistry().getRecipeCategories(focus);
            if (categories == null || categories.isEmpty()) {
                return false;
            }
            jeiRuntime.getRecipesGui().show(focus);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
