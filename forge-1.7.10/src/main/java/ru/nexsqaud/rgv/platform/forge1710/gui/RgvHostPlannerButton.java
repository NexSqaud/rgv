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
        super(id, 0, 0, 18, 18, "G");
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
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        boolean hover = this.field_146123_n;
        Gui.drawRect(xPosition, yPosition, xPosition + width, yPosition + height, 0xFF373737);
        Gui.drawRect(xPosition + 1, yPosition + 1, xPosition + width - 1, yPosition + height - 1, hover ? 0xFF337733 : 0xFF3F3F3F);

        FontRenderer fr = mc.fontRenderer;
        int textW = fr.getStringWidth("G");
        int textX = xPosition + (width - textW) / 2;
        int textY = yPosition + (height - 8) / 2;
        fr.drawStringWithShadow("G", textX, textY, hover ? 0x55FF55 : 0xFFFFFF);

        GL11.glEnable(GL11.GL_DEPTH_TEST);
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
