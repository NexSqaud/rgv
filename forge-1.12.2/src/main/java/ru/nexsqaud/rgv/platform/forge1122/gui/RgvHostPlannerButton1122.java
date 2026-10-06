package ru.nexsqaud.rgv.platform.forge1122.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import ru.nexsqaud.rgv.core.screen.RgvScreenManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Native vanilla-styled GuiButton with Graph Icon for the Craft Graph planner button
 * beside the host GuiContainer in 1.12.2.
 */
public class RgvHostPlannerButton1122 extends GuiButton {

    private final RgvScreenManager screenManager;

    public RgvHostPlannerButton1122(int id, RgvScreenManager screenManager) {
        super(id, 0, 0, 18, 18, "");
        this.screenManager = screenManager;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (screenManager == null) {
            this.visible = false;
            return;
        }

        if (screenManager.getRecipeScreen().isOpen()) {
            this.visible = false;
            return;
        }

        this.visible = true;
        this.x = screenManager.getCurrentGuiLeft() - 22;
        this.y = screenManager.getCurrentGuiTop() + 4;
        this.width = 18;
        this.height = 18;

        this.hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;

        GlStateManager.disableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        boolean hover = this.hovered;
        int k = hover ? 2 : 1;
        int vBase = 46 + k * 20;
        int w1 = width / 2;
        int w2 = width - w1;
        int h1 = height / 2;
        int h2 = height - h1;

        mc.getTextureManager().bindTexture(new ResourceLocation("textures/gui/widgets.png"));
        drawTexturedModalRect(x, y, 0, vBase, w1, h1);
        drawTexturedModalRect(x + w1, y, 200 - w2, vBase, w2, h1);
        drawTexturedModalRect(x, y + h1, 0, vBase + 20 - h2, w1, h2);
        drawTexturedModalRect(x + w1, y + h1, 200 - w2, vBase + 20 - h2, w2, h2);

        drawGraphIcon(x + width / 2, y + height / 2, hover);
    }

    public static void drawGraphIcon(int cx, int cy, boolean hover) {
        int rootColor = hover ? 0xFFFFFFFF : 0xFF55FFFF;
        int leafColor = hover ? 0xFF88FF88 : 0xFF55FF55;
        int lineColor = hover ? 0xFFFFFFFF : 0xFFAAAAAA;

        // Top root node (3x3)
        Gui.drawRect(cx - 1, cy - 4, cx + 2, cy - 1, rootColor);
        // Bottom-left leaf node (3x3)
        Gui.drawRect(cx - 5, cy + 2, cx - 2, cy + 5, leafColor);
        // Bottom-right leaf node (3x3)
        Gui.drawRect(cx + 3, cy + 2, cx + 6, cy + 5, leafColor);

        // Left diagonal connector
        Gui.drawRect(cx - 2, cy - 1, cx - 1, cy, lineColor);
        Gui.drawRect(cx - 3, cy, cx - 2, cy + 1, lineColor);
        Gui.drawRect(cx - 4, cy + 1, cx - 3, cy + 2, lineColor);

        // Right diagonal connector
        Gui.drawRect(cx + 2, cy - 1, cx + 3, cy, lineColor);
        Gui.drawRect(cx + 3, cy, cx + 4, cy + 1, lineColor);
        Gui.drawRect(cx + 4, cy + 1, cx + 5, cy + 2, lineColor);
    }

    public void onClicked() {
        if (screenManager != null) {
            screenManager.openGraph();
        }
    }

    public List<String> getTooltip() {
        List<String> tip = new ArrayList<>();
        tip.add("\u00a7bCraft Graph Planner (G)");
        tip.add("\u00a77Plan multi-step crafts & view raw requirements");
        return tip;
    }
}
