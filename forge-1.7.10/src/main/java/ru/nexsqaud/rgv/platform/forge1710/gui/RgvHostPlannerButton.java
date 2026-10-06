package ru.nexsqaud.rgv.platform.forge1710.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import org.lwjgl.opengl.GL11;
import ru.nexsqaud.rgv.core.screen.RgvScreenManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Native GuiButton for the [G] Craft Graph planner button beside the host GuiContainer.
 * Drawn during super.drawScreen() so wide slot tooltips always render ON TOP of it.
 */
public class RgvHostPlannerButton extends RgvGuiButton {

    private final RgvScreenManager screenManager;

    public RgvHostPlannerButton(int id, RgvScreenManager screenManager) {
        super(id, 0, 0, 18, 18, "");
        this.screenManager = screenManager;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (screenManager == null) {
            this.visible = false;
            return;
        }

        // Only visible when NEI handles the right-side item index and RGV modal is closed
        if (!screenManager.isNeiActive() || screenManager.getRecipeScreen().isOpen()) {
            this.visible = false;
            return;
        }

        this.visible = true;
        this.xPosition = screenManager.getCurrentGuiLeft() - 22;
        this.yPosition = screenManager.getCurrentGuiTop() + 4;
        this.width = 18;
        this.height = 18;

        this.field_146123_n = mouseX >= this.xPosition && mouseY >= this.yPosition
                && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;

        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        net.minecraft.client.renderer.OpenGlHelper.glBlendFunc(770, 771, 1, 0);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        boolean hover = this.field_146123_n;
        int k = hover ? 2 : 1;
        int vBase = 46 + k * 20;
        int w1 = width / 2;
        int w2 = width - w1;
        int h1 = height / 2;
        int h2 = height - h1;

        mc.getTextureManager().bindTexture(new net.minecraft.util.ResourceLocation("textures/gui/widgets.png"));
        drawTexturedModalRect(xPosition, yPosition, 0, vBase, w1, h1);
        drawTexturedModalRect(xPosition + w1, yPosition, 200 - w2, vBase, w2, h1);
        drawTexturedModalRect(xPosition, yPosition + h1, 0, vBase + 20 - h2, w1, h2);
        drawTexturedModalRect(xPosition + w1, yPosition + h1, 200 - w2, vBase + 20 - h2, w2, h2);

        drawGraphIcon(xPosition + width / 2, yPosition + height / 2, hover);
    }

    public static void drawGraphIcon(int cx, int cy, boolean hover) {
        int rootColor = hover ? 0xFFFFFFFF : 0xFF55FFFF;
        int leafColor = hover ? 0xFF88FF88 : 0xFF55FF55;
        int lineColor = hover ? 0xFFFFFFFF : 0xFFAAAAAA;

        Gui.drawRect(cx - 1, cy - 4, cx + 2, cy - 1, rootColor);
        Gui.drawRect(cx - 5, cy + 2, cx - 2, cy + 5, leafColor);
        Gui.drawRect(cx + 3, cy + 2, cx + 6, cy + 5, leafColor);

        Gui.drawRect(cx - 2, cy - 1, cx - 1, cy, lineColor);
        Gui.drawRect(cx - 3, cy, cx - 2, cy + 1, lineColor);
        Gui.drawRect(cx - 4, cy + 1, cx - 3, cy + 2, lineColor);

        Gui.drawRect(cx + 2, cy - 1, cx + 3, cy, lineColor);
        Gui.drawRect(cx + 3, cy, cx + 4, cy + 1, lineColor);
        Gui.drawRect(cx + 4, cy + 1, cx + 5, cy + 2, lineColor);
    }

    @Override
    public void onClick() {
        if (screenManager != null) {
            screenManager.openGraph();
        }
    }

    @Override
    public List<String> getTooltip() {
        List<String> tip = new ArrayList<>();
        tip.add("\u00a7bCraft Graph Planner (G)");
        tip.add("\u00a77Plan multi-step crafts & view raw requirements");
        return tip;
    }
}
