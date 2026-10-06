package ru.nexsqaud.rgv.platform.forge1122.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import ru.nexsqaud.rgv.core.screen.RgvRecipeScreen;
import ru.nexsqaud.rgv.core.screen.RgvScreenManager;
import ru.nexsqaud.rgv.platform.forge1122.render.GL11RgvRenderer1122;

import java.io.IOException;

/**
 * Dedicated GuiScreen for the RGV Recipe Viewer and Craft Graph Dialog in 1.12.2.
 * Runs as an active GuiScreen to block underlying container slots from receiving mouse input.
 */
public class RgvGuiScreen1122 extends GuiScreen {

    private final GuiScreen parentScreen;
    private final RgvScreenManager screenManager;
    private final RgvRecipeScreen recipeScreen;
    private final GL11RgvRenderer1122 renderer;
    private final long openTime = System.currentTimeMillis();

    public RgvGuiScreen1122(GuiScreen parentScreen, RgvScreenManager screenManager, RgvRecipeScreen recipeScreen) {
        this.parentScreen = parentScreen;
        this.screenManager = screenManager;
        this.recipeScreen = recipeScreen;
        this.renderer = new GL11RgvRenderer1122(Minecraft.getMinecraft());
    }

    public GuiScreen getParentScreen() {
        return parentScreen;
    }

    @Override
    public void initGui() {
        super.initGui();
        if (parentScreen != null) {
            try {
                parentScreen.setWorldAndResolution(this.mc, this.width, this.height);
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (parentScreen != null) {
            try {
                parentScreen.drawScreen(-1000, -1000, partialTicks);
            } catch (Throwable ignored) {
            }
        }

        GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);
        GlStateManager.disableDepth();

        drawDefaultBackground();

        if (recipeScreen.isOpen()) {
            recipeScreen.render(renderer, mouseX, mouseY, partialTicks);
            recipeScreen.renderTooltips(renderer, mouseX, mouseY);
        } else if (this.mc.currentScreen == this) {
            this.mc.displayGuiScreen(parentScreen);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        if (System.currentTimeMillis() - openTime < 200) {
            return;
        }
        if (recipeScreen.isOpen()) {
            recipeScreen.mouseClicked(mouseX, mouseY, button);
            if (!recipeScreen.isOpen() && this.mc.currentScreen == this) {
                this.mc.displayGuiScreen(parentScreen);
            }
        } else if (this.mc.currentScreen == this) {
            this.mc.displayGuiScreen(parentScreen);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        if (state >= 0) {
            recipeScreen.mouseReleased(mouseX, mouseY, state);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        int dWheel = Mouse.getDWheel();
        if (dWheel != 0) {
            recipeScreen.mouseScrolled(dWheel);
        }
        super.handleMouseInput();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1 || (ru.nexsqaud.rgv.core.platform.RgvPlatform.get() != null && ru.nexsqaud.rgv.core.platform.RgvPlatform.get().isInventoryKey(keyCode))) {
            if (recipeScreen.isPickerOpen()) {
                recipeScreen.closePicker();
                return;
            }
            recipeScreen.close();
            if (this.mc.currentScreen == this) {
                this.mc.displayGuiScreen(parentScreen);
            }
            return;
        }

        if (recipeScreen.isOpen()) {
            recipeScreen.keyPressed(keyCode, typedChar);
            if (!recipeScreen.isOpen() && this.mc.currentScreen == this) {
                this.mc.displayGuiScreen(parentScreen);
            }
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
