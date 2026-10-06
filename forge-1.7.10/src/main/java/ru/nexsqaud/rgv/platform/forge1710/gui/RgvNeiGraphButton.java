package ru.nexsqaud.rgv.platform.forge1710.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import ru.nexsqaud.rgv.core.screen.RgvScreenManager;
import ru.nexsqaud.rgv.platform.forge1710.compat.nei.NeiRecipeHelper;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

/**
 * Native vanilla-styled GuiButton with Graph Icon for NEI's GuiRecipe screens.
 * Positioned inside the recipe window.
 */
public class RgvNeiGraphButton extends RgvGuiButton {

    private final Object guiRecipe;
    private final int targetRow;
    private final RgvScreenManager screenManager;

    public RgvNeiGraphButton(int id, Object guiRecipe, int targetRow, RgvScreenManager screenManager) {
        super(id, 0, 0, 14, 14, "");
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

        if (targetRow >= perPage) {
            this.visible = false;
            return;
        }

        this.visible = true;
        this.width = 14;
        this.height = 14;

        Point p = NeiRecipeHelper.getRecipePosition(guiRecipe, targetRow);
        if (p != null) {
            this.xPosition = guiLeft + p.x + 132;
            this.yPosition = guiTop + p.y + (perPage > 1 ? 46 : 56);
        } else {
            this.xPosition = guiLeft + xSize - 22;
            this.yPosition = guiTop + (perPage <= 1 ? 36 : (22 + targetRow * 65));
        }

        this.field_146123_n = mouseX >= this.xPosition && mouseY >= this.yPosition
                && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;

        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(770, 771, 1, 0);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        boolean hover = this.field_146123_n;
        int k = hover ? 2 : 1;
        int vBase = 46 + k * 20;
        int w1 = width / 2;
        int w2 = width - w1;
        int h1 = height / 2;
        int h2 = height - h1;

        mc.getTextureManager().bindTexture(new ResourceLocation("textures/gui/widgets.png"));
        drawTexturedModalRect(xPosition, yPosition, 0, vBase, w1, h1);
        drawTexturedModalRect(xPosition + w1, yPosition, 200 - w2, vBase, w2, h1);
        drawTexturedModalRect(xPosition, yPosition + h1, 0, vBase + 20 - h2, w1, h2);
        drawTexturedModalRect(xPosition + w1, yPosition + h1, 200 - w2, vBase + 20 - h2, w2, h2);

        RgvHostPlannerButton.drawGraphIcon(xPosition + width / 2, yPosition + height / 2, hover);
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
        tip.add("\u00a7bAdd to Craft Graph");
        tip.add("\u00a77Send this recipe to the RGV Graph Planner");
        return tip;
    }
}
