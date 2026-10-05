package ru.nexsqaud.rgv.platform.forge1710.gui;

import net.minecraft.client.gui.GuiButton;

import java.util.List;

/**
 * Base class for all RGV buttons attached to Minecraft GuiScreens.
 * Extends GuiButton so that Minecraft renders them during super.drawScreen(),
 * ensuring Minecraft tooltips (rendered at the end of drawScreen) are ALWAYS drawn on top.
 */
public abstract class RgvGuiButton extends GuiButton {

    public RgvGuiButton(int id, int x, int y, int width, int height, String text) {
        super(id, x, y, width, height, text);
    }

    public boolean isMouseOver() {
        return this.field_146123_n;
    }

    public abstract void onClick();

    public abstract List<String> getTooltip();
}
