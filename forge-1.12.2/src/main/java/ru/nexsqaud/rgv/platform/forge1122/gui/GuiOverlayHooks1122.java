package ru.nexsqaud.rgv.platform.forge1122.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import ru.nexsqaud.rgv.api.RgvStack;
import ru.nexsqaud.rgv.core.screen.RgvScreenManager;
import ru.nexsqaud.rgv.platform.forge1122.Forge1122Platform;
import ru.nexsqaud.rgv.platform.forge1122.render.GL11RgvRenderer1122;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Event subscriber hooking into Forge 1.12.2 GUI rendering and user input.
 */
public class GuiOverlayHooks1122 {

    private final RgvScreenManager screenManager;
    private final GL11RgvRenderer1122 renderer;

    private RgvHostPlannerButton1122 hostBtn;

    private boolean wasGDown = false;
    private boolean wasADown = false;
    private boolean wasRDown = false;
    private boolean wasUDown = false;

    private static Field fGuiLeft;
    private static Field fGuiTop;
    private static Field fXSize;
    private static Field fYSize;

    static {
        try {
            fGuiLeft = GuiContainer.class.getDeclaredField("guiLeft");
        } catch (NoSuchFieldException e) {
            try {
                fGuiLeft = GuiContainer.class.getDeclaredField("field_147003_i");
            } catch (Exception ignored) {}
        }
        if (fGuiLeft != null) fGuiLeft.setAccessible(true);

        try {
            fGuiTop = GuiContainer.class.getDeclaredField("guiTop");
        } catch (NoSuchFieldException e) {
            try {
                fGuiTop = GuiContainer.class.getDeclaredField("field_147009_r");
            } catch (Exception ignored) {}
        }
        if (fGuiTop != null) fGuiTop.setAccessible(true);

        try {
            fXSize = GuiContainer.class.getDeclaredField("xSize");
        } catch (NoSuchFieldException e) {
            try {
                fXSize = GuiContainer.class.getDeclaredField("field_146999_f");
            } catch (Exception ignored) {}
        }
        if (fXSize != null) fXSize.setAccessible(true);

        try {
            fYSize = GuiContainer.class.getDeclaredField("ySize");
        } catch (NoSuchFieldException e) {
            try {
                fYSize = GuiContainer.class.getDeclaredField("field_147000_g");
            } catch (Exception ignored) {}
        }
        if (fYSize != null) fYSize.setAccessible(true);
    }

    public GuiOverlayHooks1122(RgvScreenManager screenManager) {
        this.screenManager = screenManager;
        this.renderer = new GL11RgvRenderer1122(Minecraft.getMinecraft());
    }

    @SubscribeEvent
    public void onInitGui(GuiScreenEvent.InitGuiEvent.Post event) {
        if (event.getGui() instanceof RgvGuiScreen1122 || Minecraft.getMinecraft().currentScreen instanceof RgvGuiScreen1122) return;

        wasGDown = false;
        wasADown = false;
        wasRDown = false;
        wasUDown = false;

        if (event.getGui().getClass().getName().contains("RecipesGui")) {
            hostBtn = null;
        } else if (event.getGui() instanceof GuiContainer) {
            GuiContainer container = (GuiContainer) event.getGui();
            int guiLeft = 0;
            int guiTop = 0;
            int xSize = 176;
            int ySize = 166;
            try {
                if (fGuiLeft != null) guiLeft = fGuiLeft.getInt(container);
                if (fGuiTop != null) guiTop = fGuiTop.getInt(container);
                if (fXSize != null) xSize = fXSize.getInt(container);
                if (fYSize != null) ySize = fYSize.getInt(container);
            } catch (Exception ignored) {}
            screenManager.updateBounds(event.getGui().width, event.getGui().height, guiLeft, guiTop, xSize, ySize);

            int btnId = 64000;
            hostBtn = new RgvHostPlannerButton1122(btnId, screenManager);
            hostBtn.x = guiLeft - 22;
            hostBtn.y = guiTop + 4;
            event.getButtonList().add(hostBtn);
        }
    }

    @SubscribeEvent
    public void onActionPerformed(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (event.getButton() instanceof RgvHostPlannerButton1122) {
            ((RgvHostPlannerButton1122) event.getButton()).onClicked();
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onDrawScreenPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (event.getGui() instanceof RgvGuiScreen1122 || Minecraft.getMinecraft().currentScreen instanceof RgvGuiScreen1122) return;

        if (event.getGui().getClass().getName().contains("RecipesGui")) {
            GuiScreen screen = event.getGui();
            int guiLeft = RgvJeiGraphButton1122.getIntField(screen, "guiLeft", (screen.width - 176) / 2);
            int guiTop = RgvJeiGraphButton1122.getIntField(screen, "guiTop", (screen.height - 166) / 2);
            int xSize = RgvJeiGraphButton1122.getIntField(screen, "xSize", 176);
            int ySize = RgvJeiGraphButton1122.getIntField(screen, "ySize", 166);
            screenManager.updateBounds(screen.width, screen.height, guiLeft, guiTop, xSize, ySize);

            if (screenManager.getRecipeScreen().isOpen()) {
                screenManager.getRecipeScreen().render(renderer, event.getMouseX(), event.getMouseY(), event.getRenderPartialTicks());
                screenManager.getRecipeScreen().renderTooltips(renderer, event.getMouseX(), event.getMouseY());
                return;
            }

            RgvJeiGraphButton1122.render(screen, screenManager, event.getMouseX(), event.getMouseY(), event.getRenderPartialTicks());
            return;
        }

        if (!(event.getGui() instanceof GuiContainer)) return;
        GuiContainer container = (GuiContainer) event.getGui();

        int guiLeft = 0;
        int guiTop = 0;
        int xSize = 176;
        int ySize = 166;
        try {
            if (fGuiLeft != null) guiLeft = fGuiLeft.getInt(container);
            if (fGuiTop != null) guiTop = fGuiTop.getInt(container);
            if (fXSize != null) xSize = fXSize.getInt(container);
            if (fYSize != null) ySize = fYSize.getInt(container);
        } catch (Exception ignored) {}

        screenManager.updateBounds(event.getGui().width, event.getGui().height, guiLeft, guiTop, xSize, ySize);

        // Render panels (render includes tooltips)
        screenManager.render(renderer, event.getMouseX(), event.getMouseY(), event.getRenderPartialTicks());

        Slot hoveredSlot = container.getSlotUnderMouse();
        if (hoveredSlot == null || !hoveredSlot.getHasStack()) {
            if (hostBtn != null && hostBtn.visible && hostBtn.isMouseOver()) {
                GuiUtils.drawHoveringText(hostBtn.getTooltip(), event.getMouseX(), event.getMouseY(),
                        container.width, container.height, -1, Minecraft.getMinecraft().fontRenderer);
            }
        }
    }

    @SubscribeEvent
    public void onMouseInput(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (event.getGui() instanceof RgvGuiScreen1122 || Minecraft.getMinecraft().currentScreen instanceof RgvGuiScreen1122) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.displayWidth <= 0 || mc.displayHeight <= 0) return;

        int mouseX = Mouse.getEventX() * event.getGui().width / mc.displayWidth;
        int mouseY = event.getGui().height - Mouse.getEventY() * event.getGui().height / mc.displayHeight - 1;

        int btn = Mouse.getEventButton();
        boolean btnState = Mouse.getEventButtonState();
        int dWheel = Mouse.getEventDWheel();

        if (event.getGui().getClass().getName().contains("RecipesGui")) {
            if (screenManager.getRecipeScreen().isOpen()) {
                if (dWheel != 0) {
                    if (screenManager.getRecipeScreen().mouseScrolled(dWheel)) {
                        event.setCanceled(true);
                        return;
                    }
                }
                if (btn >= 0) {
                    if (btnState) {
                        if (screenManager.getRecipeScreen().mouseClicked(mouseX, mouseY, btn)) {
                            event.setCanceled(true);
                            return;
                        }
                    } else {
                        screenManager.getRecipeScreen().mouseReleased(mouseX, mouseY, btn);
                    }
                }
                return;
            }

            if (btn == 0 && btnState) {
                if (RgvJeiGraphButton1122.mouseClicked(event.getGui(), screenManager, mouseX, mouseY, btn)) {
                    event.setCanceled(true);
                    return;
                }
            }
            return;
        }

        if (!(event.getGui() instanceof GuiContainer)) return;

        if (dWheel != 0) {
            if (screenManager.mouseScrolled(dWheel)) {
                event.setCanceled(true);
                return;
            }
        }

        if (btn >= 0) {
            if (btnState) {
                if (screenManager.mouseClicked(mouseX, mouseY, btn)) {
                    event.setCanceled(true);
                }
            } else {
                screenManager.mouseReleased(mouseX, mouseY, btn);
            }
        }
    }

    @SubscribeEvent
    public void onKeyboardInput(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if (event.getGui() instanceof RgvGuiScreen1122 || Minecraft.getMinecraft().currentScreen instanceof RgvGuiScreen1122) return;

        int key = Keyboard.getEventKey();
        boolean state = Keyboard.getEventKeyState();

        if (event.getGui().getClass().getName().contains("RecipesGui")) {
            if (screenManager.getRecipeScreen().isOpen()) {
                if (state) {
                    if (key == Keyboard.KEY_ESCAPE) {
                        screenManager.getRecipeScreen().close();
                        event.setCanceled(true);
                        return;
                    }
                    if (screenManager.getRecipeScreen().keyPressed(key, Keyboard.getEventCharacter())) {
                        event.setCanceled(true);
                        return;
                    }
                }
                return;
            }

            if (state) {
                if (key == Keyboard.KEY_G && !wasGDown) {
                    wasGDown = true;
                    Object hoveredObj = getJeiIngredientUnderMouse(event.getGui());
                    if (hoveredObj instanceof ItemStack && !((ItemStack) hoveredObj).isEmpty()) {
                        screenManager.openGraphForStack(Forge1122Platform.toRgvStack((ItemStack) hoveredObj));
                        event.setCanceled(true);
                        return;
                    } else {
                        screenManager.openGraph();
                        event.setCanceled(true);
                        return;
                    }
                }
            } else {
                if (key == Keyboard.KEY_G) wasGDown = false;
            }
            return;
        }

        if (!(event.getGui() instanceof GuiContainer)) return;
        GuiContainer container = (GuiContainer) event.getGui();

        if (state) {
            if (key == Keyboard.KEY_G && !wasGDown) {
                wasGDown = true;
                ItemStack hovered = getHoveredItemStack(container);
                if (!hovered.isEmpty()) {
                    screenManager.openGraphForStack(Forge1122Platform.toRgvStack(hovered));
                } else {
                    screenManager.openGraph();
                }
                event.setCanceled(true);
                return;
            } else if (key == Keyboard.KEY_A && !wasADown) {
                wasADown = true;
                ItemStack hovered = getHoveredItemStack(container);
                if (!hovered.isEmpty()) {
                    screenManager.getConfig().toggleBookmark(Forge1122Platform.toRgvStack(hovered));
                    event.setCanceled(true);
                    return;
                }
            } else if (!Forge1122Platform.isJeiPresent()) {
                if (key == Keyboard.KEY_R && !wasRDown) {
                    wasRDown = true;
                    ItemStack hovered = getHoveredItemStack(container);
                    if (!hovered.isEmpty()) {
                        screenManager.openRecipesFor(Forge1122Platform.toRgvStack(hovered));
                        event.setCanceled(true);
                        return;
                    }
                } else if (key == Keyboard.KEY_U && !wasUDown) {
                    wasUDown = true;
                    ItemStack hovered = getHoveredItemStack(container);
                    if (!hovered.isEmpty()) {
                        screenManager.openUsesFor(Forge1122Platform.toRgvStack(hovered));
                        event.setCanceled(true);
                        return;
                    }
                }
            }
        } else {
            if (key == Keyboard.KEY_G) wasGDown = false;
            if (key == Keyboard.KEY_A) wasADown = false;
            if (key == Keyboard.KEY_R) wasRDown = false;
            if (key == Keyboard.KEY_U) wasUDown = false;
        }

        boolean isCtrl = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
        if (screenManager.keyPressed(key, Keyboard.getEventCharacter(), isCtrl)) {
            event.setCanceled(true);
        }
    }

    private Object getJeiIngredientUnderMouse(GuiScreen screen) {
        try {
            Method m = screen.getClass().getMethod("getIngredientUnderMouse");
            return m.invoke(screen);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private ItemStack getHoveredItemStack(GuiContainer container) {
        Slot slot = container.getSlotUnderMouse();
        if (slot != null && slot.getHasStack()) {
            return slot.getStack();
        }
        RgvStack hoveredIndex = screenManager.getHoveredStack();
        if (hoveredIndex != null && !hoveredIndex.isEmpty()) {
            return Forge1122Platform.toMinecraftStack(hoveredIndex);
        }
        return ItemStack.EMPTY;
    }
}
