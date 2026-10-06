package ru.nexsqaud.rgv.platform.forge1710.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import ru.nexsqaud.rgv.core.screen.RgvRecipeScreen;
import ru.nexsqaud.rgv.core.screen.RgvScreenManager;
import ru.nexsqaud.rgv.platform.forge1710.GL11RgvRenderer;

/**
 * Dedicated GuiScreen for the RGV Recipe Viewer and Craft Graph Dialog.
 * By running as an active GuiScreen, it completely blocks underlying GuiContainer
 * slots from receiving mouse input, preventing item grabbing or cursor clicks.
 */
public class RgvGuiScreen extends GuiScreen {

    private final GuiScreen parentScreen;
    private final RgvScreenManager screenManager;
    private final RgvRecipeScreen recipeScreen;
    private final GL11RgvRenderer renderer;
    private final long openTime = System.currentTimeMillis();

    public RgvGuiScreen(GuiScreen parentScreen, RgvScreenManager screenManager, RgvRecipeScreen recipeScreen) {
        this.parentScreen = parentScreen;
        this.screenManager = screenManager;
        this.recipeScreen = recipeScreen;
        this.renderer = new GL11RgvRenderer(Minecraft.getMinecraft());
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

        // Clear depth buffer and disable depth test so container slots NEVER bleed through
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        drawDefaultBackground();

        if (recipeScreen.isOpen()) {
            recipeScreen.render(renderer, mouseX, mouseY, partialTicks);
            recipeScreen.renderTooltips(renderer, mouseX, mouseY);
        } else if (this.mc.currentScreen == this) {
            this.mc.displayGuiScreen(parentScreen);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
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
    protected void mouseMovedOrUp(int mouseX, int mouseY, int which) {
        super.mouseMovedOrUp(mouseX, mouseY, which);
        if (which >= 0) {
            recipeScreen.mouseReleased(mouseX, mouseY, which);
        }
    }

    @Override
    public void handleMouseInput() {
        int dWheel = Mouse.getDWheel();
        if (dWheel != 0) {
            recipeScreen.mouseScrolled(dWheel);
        }
        super.handleMouseInput();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        // Escape or inventory key closes popup or immediately restores parent screen
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
