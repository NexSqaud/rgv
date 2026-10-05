package ru.nexsqaud.rgv.platform.forge1710.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import org.lwjgl.opengl.GL11;
import ru.nexsqaud.rgv.core.screen.RgvScreenManager;
import ru.nexsqaud.rgv.platform.forge1710.compat.nei.NeiRecipeHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Dedicated button for NEI's GuiRecipe screens.
 * Positioned on the right edge outside the recipe window (guiLeft + xSize + 2).
 * Drawn during super.drawScreen() so slot tooltips always render ON TOP of it.
 */
public class RgvNeiGraphButton extends RgvGuiButton {

    private final Object guiRecipe;
    private final int targetRow;
    private final RgvScreenManager screenManager;

    public RgvNeiGraphButton(int id, Object guiRecipe, int targetRow, RgvScreenManager screenManager) {
        super(id, 0, 0, 54, 20, "+ Graph");
        this.guiRecipe = guiRecipe;
        this.targetRow = targetRow;
        this.screenManager = screenManager;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (guiRecipe == null || screenManager == null) {
            this.visible = false;
            return;
        }

        // If RGV modal Craft Graph is open, suppress host buttons
        if (screenManager.getRecipeScreen().isOpen()) {
            this.visible = false;
            return;
        }

        int guiLeft = NeiRecipeHelper.getIntField(guiRecipe, "field_147003_i", "guiLeft", (mc.currentScreen.width - 176) / 2);
        int guiTop = NeiRecipeHelper.getIntField(guiRecipe, "field_147009_r", "guiTop", (mc.currentScreen.height - 166) / 2);
        int xSize = NeiRecipeHelper.getIntField(guiRecipe, "field_146999_f", "xSize", 176);

        List<?> handlers = NeiRecipeHelper.getHandlers(guiRecipe);
        int recipetype = NeiRecipeHelper.getIntField(guiRecipe, "recipetype", "recipetype", 0);
        int page = NeiRecipeHelper.getIntField(guiRecipe, "page", "page", 0);

        if (handlers == null || recipetype < 0 || recipetype >= handlers.size()) {
            this.visible = false;
            return;
        }

        Object handler = handlers.get(recipetype);
        int perPage = NeiRecipeHelper.invokeInt(handler, "recipiesPerPage", 1);
        int numRecipes = NeiRecipeHelper.invokeInt(handler, "numRecipes", 0);

        if (numRecipes <= 0) {
            this.visible = false;
            return;
        }

        int recipeIndex = page * perPage + targetRow;
        if (recipeIndex >= numRecipes) {
            this.visible = false;
            return;
        }

        if (perPage <= 1) {
            if (targetRow > 0) {
                this.visible = false;
                return;
            }
            this.visible = true;
            this.xPosition = guiLeft + xSize + 2;
            this.yPosition = guiTop + 4;
            this.width = 54;
            this.height = 20;
            this.displayString = "+ Graph";
        } else {
            // Multi-recipe page (e.g. smelting with 2 recipes per page)
            if (targetRow >= perPage) {
                this.visible = false;
                return;
            }
            this.visible = true;
            this.xPosition = guiLeft + xSize + 2;
            this.yPosition = guiTop + 16 + targetRow * 65;
            this.width = 54;
            this.height = 18;
            this.displayString = "+ Graph #" + (targetRow + 1);
        }

        this.field_146123_n = mouseX >= this.xPosition && mouseY >= this.yPosition
                && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;

        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        boolean hover = this.field_146123_n;
        // Outer dark border
        Gui.drawRect(xPosition, yPosition, xPosition + width, yPosition + height, 0xFF373737);
        // Inner fill
        Gui.drawRect(xPosition + 1, yPosition + 1, xPosition + width - 1, yPosition + height - 1, hover ? 0xFF2E6B2E : 0xFF2A2A2A);
        // Inner highlight border
        Gui.drawRect(xPosition + 2, yPosition + 2, xPosition + width - 2, yPosition + height - 2, hover ? 0xFF3A8A3A : 0xFF3F3F3F);

        FontRenderer fr = mc.fontRenderer;
        int textW = fr.getStringWidth(displayString);
        int textX = xPosition + (width - textW) / 2;
        int textY = yPosition + (height - 8) / 2;
        fr.drawStringWithShadow(displayString, textX, textY, hover ? 0x55FF55 : 0xFFFFFF);

        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    @Override
    public void onClick() {
        if (guiRecipe == null || screenManager == null) return;
        List<?> handlers = NeiRecipeHelper.getHandlers(guiRecipe);
        int recipetype = NeiRecipeHelper.getIntField(guiRecipe, "recipetype", "recipetype", 0);
        int page = NeiRecipeHelper.getIntField(guiRecipe, "page", "page", 0);

        if (handlers != null && recipetype >= 0 && recipetype < handlers.size()) {
            Object handler = handlers.get(recipetype);
            int perPage = NeiRecipeHelper.invokeInt(handler, "recipiesPerPage", 1);
            int numRecipes = NeiRecipeHelper.invokeInt(handler, "numRecipes", 0);
            int recipeIndex = Math.min(numRecipes - 1, Math.max(0, page * perPage + targetRow));
            NeiRecipeHelper.addNeiRecipeIndexToGraph(handler, recipeIndex, screenManager);
        }
    }

    @Override
    public List<String> getTooltip() {
        List<String> tip = new ArrayList<>();
        tip.add("\u00a7a+ Add to Craft Graph");
        tip.add("\u00a77Send this recipe to the RGV Graph Planner");
        return tip;
    }
}
