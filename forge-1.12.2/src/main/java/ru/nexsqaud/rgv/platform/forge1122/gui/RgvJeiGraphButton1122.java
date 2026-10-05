package ru.nexsqaud.rgv.platform.forge1122.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import ru.nexsqaud.rgv.api.RgvIngredient;
import ru.nexsqaud.rgv.api.RgvInventory;
import ru.nexsqaud.rgv.api.RgvRecipe;
import ru.nexsqaud.rgv.api.RgvStack;
import ru.nexsqaud.rgv.core.screen.RgvRecipeScreen;
import ru.nexsqaud.rgv.core.screen.RgvScreenManager;
import ru.nexsqaud.rgv.core.tree.RgvCraftGraphTab;
import ru.nexsqaud.rgv.platform.forge1122.Forge1122Platform;
import ru.nexsqaud.rgv.platform.forge1122.RgvMod1122;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Dedicated GuiButton for JEI's RecipesGui in 1.12.2 to import recipe into RGV Craft Graph.
 */
public class RgvJeiGraphButton1122 extends GuiButton {

    private final GuiScreen guiScreen;
    private final RgvScreenManager screenManager;

    public RgvJeiGraphButton1122(int id, GuiScreen guiScreen, RgvScreenManager screenManager) {
        super(id, 0, 0, 54, 20, "+ Graph");
        this.guiScreen = guiScreen;
        this.screenManager = screenManager;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (guiScreen == null || screenManager == null) {
            this.visible = false;
            return;
        }

        if (screenManager.getRecipeScreen().isOpen()) {
            this.visible = false;
            return;
        }

        int guiLeft = getIntField(guiScreen, "guiLeft", (mc.currentScreen.width - 176) / 2);
        int guiTop = getIntField(guiScreen, "guiTop", (mc.currentScreen.height - 166) / 2);
        int xSize = getIntField(guiScreen, "xSize", 176);

        this.visible = true;
        this.x = guiLeft + xSize + 2;
        this.y = guiTop + 24;
        this.width = 54;
        this.height = 20;

        this.hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;

        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        boolean hover = this.hovered;
        Gui.drawRect(x, y, x + width, y + height, 0xFF373737);
        Gui.drawRect(x + 1, y + 1, x + width - 1, y + height - 1, hover ? 0xFF337733 : 0xFF3F3F3F);

        FontRenderer fr = mc.fontRenderer;
        int textW = fr.getStringWidth("+ Graph");
        int textX = x + (width - textW) / 2;
        int textY = y + (height - 8) / 2;
        fr.drawStringWithShadow("+ Graph", textX, textY, hover ? 0x55FF55 : 0xFFFFFF);

        GlStateManager.enableDepth();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public void onClicked() {
        if (guiScreen == null || screenManager == null) return;
        RgvRecipe recipe = extractJeiRecipe(guiScreen);
        if (recipe != null) {
            RgvInventory inv = ru.nexsqaud.rgv.core.platform.RgvPlatform.get() != null ? ru.nexsqaud.rgv.core.platform.RgvPlatform.get().getPlayerInventory() : null;
            screenManager.getCraftGraph().addTabForRecipe(recipe, 1, RgvMod1122.getRecipeManager(), inv);
            screenManager.openGraph();
        }
    }

    private static int getIntField(Object obj, String fieldName, int def) {
        try {
            Field f = obj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return f.getInt(obj);
        } catch (Throwable ignored) {
            try {
                Field f = obj.getClass().getSuperclass().getDeclaredField(fieldName);
                f.setAccessible(true);
                return f.getInt(obj);
            } catch (Throwable ignored2) {
                return def;
            }
        }
    }

    private RgvRecipe extractJeiRecipe(GuiScreen screen) {
        try {
            Field fLayouts = null;
            for (Field f : screen.getClass().getDeclaredFields()) {
                if (List.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    List<?> list = (List<?>) f.get(screen);
                    if (list != null && !list.isEmpty() && list.get(0).getClass().getName().contains("RecipeLayout")) {
                        fLayouts = f;
                        break;
                    }
                }
            }
            if (fLayouts == null) {
                for (Field f : screen.getClass().getSuperclass().getDeclaredFields()) {
                    if (List.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        List<?> list = (List<?>) f.get(screen);
                        if (list != null && !list.isEmpty() && list.get(0).getClass().getName().contains("RecipeLayout")) {
                            fLayouts = f;
                            break;
                        }
                    }
                }
            }

            if (fLayouts != null) {
                List<?> layouts = (List<?>) fLayouts.get(screen);
                if (layouts != null && !layouts.isEmpty()) {
                    Object layout = layouts.get(0);
                    return createRecipeFromJeiLayout(layout);
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return null;
    }

    private RgvRecipe createRecipeFromJeiLayout(Object layout) {
        try {
            Method mGetItemStacks = layout.getClass().getMethod("getItemStacks");
            Object itemStacks = mGetItemStacks.invoke(layout);
            Method mGetGuiIngredients = itemStacks.getClass().getMethod("getGuiIngredients");
            java.util.Map<?, ?> guiIngredients = (java.util.Map<?, ?>) mGetGuiIngredients.invoke(itemStacks);

            List<RgvIngredient> inputs = new ArrayList<>();
            RgvStack output = null;

            for (java.util.Map.Entry<?, ?> entry : guiIngredients.entrySet()) {
                Object guiIng = entry.getValue();
                Method mIsInput = guiIng.getClass().getMethod("isInput");
                boolean isInput = (boolean) mIsInput.invoke(guiIng);
                Method mGetAll = guiIng.getClass().getMethod("getAllIngredients");
                List<?> all = (List<?>) mGetAll.invoke(guiIng);

                if (all != null && !all.isEmpty()) {
                    List<RgvStack> candidates = new ArrayList<>();
                    for (Object ingObj : all) {
                        if (ingObj instanceof ItemStack) {
                            candidates.add(Forge1122Platform.toRgvStack((ItemStack) ingObj));
                        }
                    }
                    if (!candidates.isEmpty()) {
                        if (isInput) {
                            inputs.add(candidates.size() == 1 ? candidates.get(0) : ru.nexsqaud.rgv.api.RgvIngredientList.of(candidates, 1));
                        } else if (output == null) {
                            output = candidates.get(0);
                        }
                    }
                }
            }

            if (output != null) {
                final RgvStack finalOutput = output;
                final List<RgvIngredient> finalInputs = inputs;
                return new RgvRecipe() {
                    @Override
                    public String getId() {
                        return "jei_" + finalOutput.getId() + "_" + System.identityHashCode(this);
                    }

                    @Override
                    public ru.nexsqaud.rgv.api.RgvRecipeCategory getCategory() {
                        return ru.nexsqaud.rgv.platform.forge1122.recipe.VanillaRecipesPlugin1122.CRAFTING_CATEGORY;
                    }

                    @Override
                    public List<RgvIngredient> getInputs() {
                        return finalInputs;
                    }

                    @Override
                    public List<RgvStack> getOutputs() {
                        return Collections.singletonList(finalOutput);
                    }

                    @Override
                    public void addWidgets(ru.nexsqaud.rgv.api.widget.RgvWidgetHolder holder) {
                        int x = 4;
                        for (RgvIngredient in : finalInputs) {
                            holder.addSlot(in, x, 18);
                            x += 18;
                        }
                        holder.addArrow(x + 4, 18, false);
                        holder.addOutputSlot(finalOutput, x + 32, 18);
                    }
                };
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return null;
    }
}
