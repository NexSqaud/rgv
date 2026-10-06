package ru.nexsqaud.rgv.platform.forge1122.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.config.GuiUtils;
import ru.nexsqaud.rgv.api.RgvIngredient;
import ru.nexsqaud.rgv.api.RgvInventory;
import ru.nexsqaud.rgv.api.RgvRecipe;
import ru.nexsqaud.rgv.api.RgvRecipeCategory;
import ru.nexsqaud.rgv.api.RgvStack;
import ru.nexsqaud.rgv.api.widget.RgvWidgetHolder;
import ru.nexsqaud.rgv.core.screen.RgvScreenManager;
import ru.nexsqaud.rgv.platform.forge1122.Forge1122Platform;
import ru.nexsqaud.rgv.platform.forge1122.RgvMod1122;
import ru.nexsqaud.rgv.platform.forge1122.recipe.VanillaRecipesPlugin1122;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Handles rendering and user interaction for the "+ Graph" button on JEI's RecipesGui in 1.12.2.
 */
public class RgvJeiGraphButton1122 {

    private static final ResourceLocation WIDGETS_TEXTURE = new ResourceLocation("textures/gui/widgets.png");
    private static Field fRecipeLayouts;

    public static void render(GuiScreen screen, RgvScreenManager screenManager, int mouseX, int mouseY, float partialTicks) {
        if (screen == null || screenManager == null) return;
        if (screenManager.getRecipeScreen().isOpen()) return;

        List<?> layouts = getRecipeLayouts(screen);
        if (layouts == null || layouts.isEmpty()) return;

        Object hoveredLayout = null;

        for (Object layout : layouts) {
            if (layout == null) continue;
            int[] pos = getButtonPosition(layout);
            int bx = pos[0];
            int by = pos[1];
            int bw = 14;
            int bh = 14;

            boolean hovered = mouseX >= bx && mouseY >= by && mouseX < bx + bw && mouseY < by + bh;
            if (hovered) {
                hoveredLayout = layout;
            }

            GlStateManager.disableLighting();
            GlStateManager.enableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

            int k = hovered ? 2 : 1;
            int vBase = 46 + k * 20;
            int w1 = bw / 2;
            int w2 = bw - w1;
            int h1 = bh / 2;
            int h2 = bh - h1;

            Minecraft.getMinecraft().getTextureManager().bindTexture(WIDGETS_TEXTURE);
            screen.drawTexturedModalRect(bx, by, 0, vBase, w1, h1);
            screen.drawTexturedModalRect(bx + w1, by, 200 - w2, vBase, w2, h1);
            screen.drawTexturedModalRect(bx, by + h1, 0, vBase + 20 - h2, w1, h2);
            screen.drawTexturedModalRect(bx + w1, by + h1, 200 - w2, vBase + 20 - h2, w2, h2);

            RgvHostPlannerButton1122.drawGraphIcon(bx + bw / 2, by + bh / 2, hovered);
        }

        if (hoveredLayout != null) {
            List<String> tip = new ArrayList<>();
            tip.add("\u00a7bAdd to Craft Graph");
            tip.add("\u00a77Send this recipe to the RGV Graph Planner");
            GuiUtils.drawHoveringText(tip, mouseX, mouseY, screen.width, screen.height, -1, Minecraft.getMinecraft().fontRenderer);
        }
    }

    public static boolean mouseClicked(GuiScreen screen, RgvScreenManager screenManager, int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0 || screen == null || screenManager == null) return false;
        if (screenManager.getRecipeScreen().isOpen()) return false;

        List<?> layouts = getRecipeLayouts(screen);
        if (layouts == null || layouts.isEmpty()) return false;

        for (Object layout : layouts) {
            if (layout == null) continue;
            int[] pos = getButtonPosition(layout);
            int bx = pos[0];
            int by = pos[1];
            int bw = 14;
            int bh = 14;

            if (mouseX >= bx && mouseY >= by && mouseX < bx + bw && mouseY < by + bh) {
                RgvRecipe recipe = extractJeiRecipe(layout);
                if (recipe != null) {
                    RgvInventory inv = ru.nexsqaud.rgv.core.platform.RgvPlatform.get() != null
                            ? ru.nexsqaud.rgv.core.platform.RgvPlatform.get().getPlayerInventory() : null;
                    screenManager.getCraftGraph().addTabForRecipe(recipe, 1, RgvMod1122.getRecipeManager(), inv);
                    screenManager.openGraph();
                    Minecraft.getMinecraft().getSoundHandler().playSound(
                            net.minecraft.client.audio.PositionedSoundRecord.getMasterRecord(
                                    net.minecraft.init.SoundEvents.UI_BUTTON_CLICK, 1.0F
                            )
                    );
                    return true;
                }
            }
        }
        return false;
    }

    private static int[] getButtonPosition(Object layout) {
        try {
            Method mTransfer = layout.getClass().getMethod("getRecipeTransferButton");
            Object transferBtn = mTransfer.invoke(layout);
            if (transferBtn instanceof GuiButton) {
                GuiButton btn = (GuiButton) transferBtn;
                return new int[]{btn.x - 15, btn.y};
            }
        } catch (Throwable ignored) {}

        int posX = getIntField(layout, "posX", 0);
        int posY = getIntField(layout, "posY", 0);
        return new int[]{posX + 154 - 15, posY + 45};
    }

    private static List<?> getRecipeLayouts(GuiScreen screen) {
        try {
            if (fRecipeLayouts == null) {
                for (Field f : screen.getClass().getDeclaredFields()) {
                    if (List.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        List<?> list = (List<?>) f.get(screen);
                        if (list != null && !list.isEmpty() && list.get(0).getClass().getName().contains("RecipeLayout")) {
                            fRecipeLayouts = f;
                            return list;
                        }
                    }
                }
                for (Field f : screen.getClass().getSuperclass().getDeclaredFields()) {
                    if (List.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        List<?> list = (List<?>) f.get(screen);
                        if (list != null && !list.isEmpty() && list.get(0).getClass().getName().contains("RecipeLayout")) {
                            fRecipeLayouts = f;
                            return list;
                        }
                    }
                }
            } else {
                return (List<?>) fRecipeLayouts.get(screen);
            }
        } catch (Throwable ignored) {}
        return Collections.emptyList();
    }

    public static int getIntField(Object obj, String fieldName, int def) {
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

    private static RgvRecipe extractJeiRecipe(Object layout) {
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
                // First check if an existing registered recipe matches to ensure persistent graph tabs
                List<RgvRecipe> existingRecipes = RgvMod1122.getRecipeManager().getRecipesFor(output);
                for (RgvRecipe r : existingRecipes) {
                    boolean outputMatched = false;
                    for (RgvStack o : r.getOutputs()) {
                        if (o.matches(output) && o.getAmount() == output.getAmount()) {
                            outputMatched = true;
                            break;
                        }
                    }
                    if (outputMatched && inputsMatch(r.getInputs(), inputs)) {
                        return r;
                    }
                }
                if (existingRecipes.size() == 1) {
                    return existingRecipes.get(0);
                }

                String catUid = "";
                try {
                    Method mCat = layout.getClass().getMethod("getRecipeCategory");
                    Object catObj = mCat.invoke(layout);
                    Method mUid = catObj.getClass().getMethod("getUid");
                    catUid = (String) mUid.invoke(catObj);
                } catch (Throwable ignored) {}

                final boolean isSmelting = catUid != null && catUid.toLowerCase().contains("smelt");
                final RgvRecipeCategory category = isSmelting
                        ? VanillaRecipesPlugin1122.SMELTING_CATEGORY
                        : VanillaRecipesPlugin1122.CRAFTING_CATEGORY;

                final RgvStack finalOutput = output;
                final List<RgvIngredient> finalInputs = inputs;
                final String stableId = "jei_" + finalOutput.getId().replace(':', '_') + "_" + finalOutput.getMeta() + "_" + inputs.size();

                RgvRecipe existingSynthetic = RgvMod1122.getRecipeManager().getRecipeById(stableId);
                if (existingSynthetic != null) {
                    return existingSynthetic;
                }

                RgvRecipe syntheticRecipe = new RgvRecipe() {
                    @Override
                    public String getId() {
                        return stableId;
                    }

                    @Override
                    public RgvRecipeCategory getCategory() {
                        return category;
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
                    public void addWidgets(RgvWidgetHolder holder) {
                        if (isSmelting) {
                            if (!finalInputs.isEmpty()) {
                                holder.addSlot(finalInputs.get(0), 18, 4);
                            }
                            holder.addArrow(44, 22, false);
                            holder.addOutputSlot(finalOutput, 76, 22);
                        } else {
                            int startX = 6;
                            int startY = 6;
                            for (int idx = 0; idx < finalInputs.size() && idx < 9; idx++) {
                                int ix = startX + (idx % 3) * 18;
                                int iy = startY + (idx / 3) * 18;
                                holder.addSlot(finalInputs.get(idx), ix, iy);
                            }
                            holder.addArrow(66, 24, false);
                            holder.addOutputSlot(finalOutput, 96, 24);
                        }
                    }
                };

                RgvMod1122.getRecipeManager().addRecipe(syntheticRecipe);
                return syntheticRecipe;
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return null;
    }

    private static boolean inputsMatch(List<RgvIngredient> rInputs, List<RgvIngredient> jeiInputs) {
        List<RgvIngredient> rFiltered = new ArrayList<>();
        for (RgvIngredient in : rInputs) {
            if (in != null && !in.isEmpty()) rFiltered.add(in);
        }
        List<RgvIngredient> jFiltered = new ArrayList<>();
        for (RgvIngredient in : jeiInputs) {
            if (in != null && !in.isEmpty()) jFiltered.add(in);
        }
        if (rFiltered.size() != jFiltered.size()) return false;
        for (int i = 0; i < rFiltered.size(); i++) {
            RgvIngredient rIn = rFiltered.get(i);
            RgvIngredient jIn = jFiltered.get(i);
            if (!rIn.matches(jIn.getRgvStacks().isEmpty() ? RgvStack.empty() : jIn.getRgvStacks().get(0))) {
                return false;
            }
        }
        return true;
    }
}
