package ru.nexsqaud.rgv.platform.forge1122.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import ru.nexsqaud.rgv.core.screen.RgvRecipeScreen;
import ru.nexsqaud.rgv.core.screen.RgvScreenManager;

/**
 * Native GuiButton for the [G] Craft Graph planner button beside the host GuiContainer in 1.12.2.
 */
public class RgvHostPlannerButton1122 extends GuiButton {

    private final RgvScreenManager screenManager;

    public RgvHostPlannerButton1122(int id, RgvScreenManager screenManager) {
        super(id, 0, 0, 18, 18, "G");
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
        GlStateManager.disableDepth();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        boolean hover = this.hovered;
        Gui.drawRect(x, y, x + width, y + height, 0xFF373737);
        Gui.drawRect(x + 1, y + 1, x + width - 1, y + height - 1, hover ? 0xFF337733 : 0xFF3F3F3F);

        FontRenderer fr = mc.fontRenderer;
        int textW = fr.getStringWidth("G");
        int textX = x + (width - textW) / 2;
        int textY = y + (height - 8) / 2;
        fr.drawStringWithShadow("G", textX, textY, hover ? 0x55FF55 : 0xFFFFFF);

        GlStateManager.enableDepth();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public void onClicked() {
        if (screenManager != null) {
            screenManager.openGraph();
        }
    }
}
