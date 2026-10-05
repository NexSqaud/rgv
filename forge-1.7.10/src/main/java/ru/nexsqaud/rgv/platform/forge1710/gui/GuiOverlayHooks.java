package ru.nexsqaud.rgv.platform.forge1710.gui;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiScreenEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import ru.nexsqaud.rgv.api.RgvStack;
import ru.nexsqaud.rgv.core.screen.RgvScreenManager;
import ru.nexsqaud.rgv.platform.forge1710.Forge1710Platform;
import ru.nexsqaud.rgv.platform.forge1710.GL11RgvRenderer;
import ru.nexsqaud.rgv.platform.forge1710.compat.nei.NeiInputBridge;
import ru.nexsqaud.rgv.platform.forge1710.compat.nei.NeiRecipeHelper;
import ru.nexsqaud.rgv.platform.forge1710.compat.nei.NeiRecipeIntegration;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Hooks into Forge client GUI rendering, button initialization, and input events
 * to display and control the RGV overlay across all container screens.
 * Seamlessly yields to NEI when installed, rendering native GuiButtons for [+Graph].
 */
public class GuiOverlayHooks {

    private final RgvScreenManager screenManager;
    private final GL11RgvRenderer renderer;

    private RgvNeiGraphButton neiBtn0;
    private RgvNeiGraphButton neiBtn1;
    private RgvHostPlannerButton hostBtn;

    private boolean wasLeftDown = false;
    private boolean wasRightDown = false;
    private boolean wasRDown = false;
    private boolean wasUDown = false;
    private boolean wasADown = false;
    private boolean wasGDown = false;
    private boolean wasFDown = false;
    private boolean wasODown = false;
    private boolean wasEscDown = false;

    private static Field fGuiLeft;
    private static Field fGuiTop;
    private static Field fXSize;
    private static Field fYSize;
    private static Field fTheSlot;
    private static Method mRenderToolTip;

    static {
        try {
            fGuiLeft = GuiContainer.class.getDeclaredField("field_147003_i"); // guiLeft
            fGuiLeft.setAccessible(true);
        } catch (NoSuchFieldException e) {
            try {
                fGuiLeft = GuiContainer.class.getDeclaredField("guiLeft");
                fGuiLeft.setAccessible(true);
            } catch (Exception ignored) {
            }
        }

        try {
            fGuiTop = GuiContainer.class.getDeclaredField("field_147009_r"); // guiTop
            fGuiTop.setAccessible(true);
        } catch (NoSuchFieldException e) {
            try {
                fGuiTop = GuiContainer.class.getDeclaredField("guiTop");
                fGuiTop.setAccessible(true);
            } catch (Exception ignored) {
            }
        }

        try {
            fXSize = GuiContainer.class.getDeclaredField("field_146999_f"); // xSize
            fXSize.setAccessible(true);
        } catch (NoSuchFieldException e) {
            try {
                fXSize = GuiContainer.class.getDeclaredField("xSize");
                fXSize.setAccessible(true);
            } catch (Exception ignored) {
            }
        }

        try {
            fYSize = GuiContainer.class.getDeclaredField("field_147000_g"); // ySize
            fYSize.setAccessible(true);
        } catch (NoSuchFieldException e) {
            try {
                fYSize = GuiContainer.class.getDeclaredField("ySize");
                fYSize.setAccessible(true);
            } catch (Exception ignored) {
            }
        }

        try {
            fTheSlot = GuiContainer.class.getDeclaredField("field_147006_u"); // theSlot
            fTheSlot.setAccessible(true);
        } catch (NoSuchFieldException e) {
            try {
                fTheSlot = GuiContainer.class.getDeclaredField("theSlot");
                fTheSlot.setAccessible(true);
            } catch (Exception ignored) {
            }
        }

        try {
            mRenderToolTip = GuiScreen.class.getDeclaredMethod("func_146285_a", ItemStack.class, int.class, int.class);
            mRenderToolTip.setAccessible(true);
        } catch (NoSuchMethodException e) {
            try {
                mRenderToolTip = GuiScreen.class.getDeclaredMethod("renderToolTip", ItemStack.class, int.class, int.class);
                mRenderToolTip.setAccessible(true);
            } catch (Exception ignored) {
            }
        }
    }

    public GuiOverlayHooks(RgvScreenManager screenManager) {
        this.screenManager = screenManager;
        this.renderer = new GL11RgvRenderer(Minecraft.getMinecraft());
    }

    @SubscribeEvent
    public void onInitGuiPost(GuiScreenEvent.InitGuiEvent.Post event) {
        if (event.gui instanceof RgvGuiScreen) return;
        if (!(event.gui instanceof GuiContainer)) return;

        GuiContainer container = (GuiContainer) event.gui;

        if (Forge1710Platform.isNeiPresent()) {
            NeiInputBridge.register(screenManager);
            NeiRecipeHelper.ensureCreativeGuiHandlerRegistered(screenManager);
            NeiRecipeIntegration.ensureRecipesImported(screenManager.getRecipeManager());
        }

        // Avoid duplicate button registration
        if (event.buttonList != null) {
            for (Object b : event.buttonList) {
                if (b instanceof RgvGuiButton) return;
            }

            int btnId = 918270;
            if (NeiRecipeHelper.isGuiRecipe(container)) {
                neiBtn0 = new RgvNeiGraphButton(btnId++, container, 0, screenManager);
                neiBtn1 = new RgvNeiGraphButton(btnId++, container, 1, screenManager);
                hostBtn = null;
                event.buttonList.add(neiBtn0);
                event.buttonList.add(neiBtn1);
            } else {
                neiBtn0 = null;
                neiBtn1 = null;
                hostBtn = new RgvHostPlannerButton(btnId++, screenManager);
                event.buttonList.add(hostBtn);
            }
        }
    }

    @SubscribeEvent
    public void onActionPerformedPre(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (event.button instanceof RgvGuiButton) {
            ((RgvGuiButton) event.button).onClick();
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onDrawScreenPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (Minecraft.getMinecraft().currentScreen instanceof RgvGuiScreen) {
            return;
        }
        if (!(event.gui instanceof GuiContainer)) return;
        GuiContainer container = (GuiContainer) event.gui;

        // Ensure NEI bridge, creative handler, and recipes are connected
        if (Forge1710Platform.isNeiPresent()) {
            NeiInputBridge.register(screenManager);
            NeiRecipeHelper.ensureCreativeGuiHandlerRegistered(screenManager);
            NeiRecipeIntegration.ensureRecipesImported(screenManager.getRecipeManager());
        }

        // Check if this is NEI's recipe viewer (GuiCraftingRecipe or GuiUsageRecipe)
        if (NeiRecipeHelper.isGuiRecipe(container)) {
            handleNeiRecipeScreen(container, event.mouseX, event.mouseY, event.renderPartialTicks);
            return;
        }

        // Standard GuiContainer (Inventory, Chests, etc.)
        int guiLeft = 0;
        int guiTop = 0;
        int xSize = 176;
        int ySize = 166;

        try {
            if (fGuiLeft != null) guiLeft = fGuiLeft.getInt(container);
            if (fGuiTop != null) guiTop = fGuiTop.getInt(container);
            if (fXSize != null) xSize = fXSize.getInt(container);
            if (fYSize != null) ySize = fYSize.getInt(container);
        } catch (Exception ignored) {
        }

        screenManager.updateBounds(event.gui.width, event.gui.height, guiLeft, guiTop, xSize, ySize);

        // Handle mouse click and release transitions
        boolean isLeftDown = Mouse.isButtonDown(0);
        if (isLeftDown && !wasLeftDown) {
            screenManager.mouseClicked(event.mouseX, event.mouseY, 0);
        } else if (!isLeftDown && wasLeftDown) {
            screenManager.mouseReleased(event.mouseX, event.mouseY, 0);
        }
        wasLeftDown = isLeftDown;

        boolean isRightDown = Mouse.isButtonDown(1);
        if (isRightDown && !wasRightDown) {
            screenManager.mouseClicked(event.mouseX, event.mouseY, 1);
        } else if (!isRightDown && wasRightDown) {
            screenManager.mouseReleased(event.mouseX, event.mouseY, 1);
        }
        wasRightDown = isRightDown;

        // Handle mouse scroll wheel (only when NEI is inactive or recipe modal is open)
        if (!screenManager.isNeiActive() || screenManager.getRecipeScreen().isOpen()) {
            int dWheel = Mouse.getDWheel();
            if (dWheel != 0) {
                screenManager.mouseScrolled(dWheel);
            }
        }

        // Keyboard handling
        if (screenManager.getRecipeScreen().isOpen()) {
            boolean isEsc = Keyboard.isKeyDown(Keyboard.KEY_ESCAPE);
            if (isEsc && !wasEscDown) {
                screenManager.getRecipeScreen().close();
            }
            wasEscDown = isEsc;
        } else if (screenManager.isNeiActive()) {
            // When NEI is active:
            // Do NOT intercept 'R' or 'U' for container items; NEI handles them.
            // Only handle 'G' (Craft Graph) and 'A' (Bookmark), and R/U if hovering an RGV bookmark.
            if (!NeiRecipeHelper.isTextInputFocused()) {
                boolean isG = Keyboard.isKeyDown(Keyboard.KEY_G);
                if (isG && !wasGDown) {
                    ItemStack hovered = getHoveredItemStack(container);
                    if (hovered != null && hovered.getItem() != null) {
                        screenManager.openGraphForStack(Forge1710Platform.toRgvStack(hovered));
                    } else {
                        screenManager.openGraph();
                    }
                }
                wasGDown = isG;

                boolean isA = Keyboard.isKeyDown(Keyboard.KEY_A);
                if (isA && !wasADown) {
                    ItemStack hovered = getHoveredItemStack(container);
                    if (hovered != null && hovered.getItem() != null) {
                        screenManager.getConfig().toggleBookmark(Forge1710Platform.toRgvStack(hovered));
                    }
                }
                wasADown = isA;

                if (screenManager.isHoveringBookmark()) {
                    boolean isR = Keyboard.isKeyDown(Keyboard.KEY_R);
                    if (isR && !wasRDown) {
                        screenManager.handleRKey();
                    }
                    wasRDown = isR;

                    boolean isU = Keyboard.isKeyDown(Keyboard.KEY_U);
                    if (isU && !wasUDown) {
                        screenManager.handleUKey();
                    }
                    wasUDown = isU;
                }
            }
        } else {
            // Standalone RGV mode: Full hotkey support
            boolean isCtrl = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);

            if (screenManager.getSearchBar().isFocused()) {
                boolean isEsc = Keyboard.isKeyDown(Keyboard.KEY_ESCAPE);
                if (isEsc && !wasEscDown) {
                    screenManager.getSearchBar().setFocused(false);
                }
                wasEscDown = isEsc;
            } else {
                boolean isR = Keyboard.isKeyDown(Keyboard.KEY_R);
                if (isR && !wasRDown) {
                    ItemStack hovered = getHoveredItemStack(container);
                    if (hovered != null && hovered.getItem() != null) {
                        screenManager.openRecipesFor(Forge1710Platform.toRgvStack(hovered));
                    }
                }
                wasRDown = isR;

                boolean isU = Keyboard.isKeyDown(Keyboard.KEY_U);
                if (isU && !wasUDown) {
                    ItemStack hovered = getHoveredItemStack(container);
                    if (hovered != null && hovered.getItem() != null) {
                        screenManager.openUsesFor(Forge1710Platform.toRgvStack(hovered));
                    }
                }
                wasUDown = isU;

                boolean isA = Keyboard.isKeyDown(Keyboard.KEY_A);
                if (isA && !wasADown) {
                    ItemStack hovered = getHoveredItemStack(container);
                    if (hovered != null && hovered.getItem() != null) {
                        screenManager.getConfig().toggleBookmark(Forge1710Platform.toRgvStack(hovered));
                    }
                }
                wasADown = isA;

                boolean isG = Keyboard.isKeyDown(Keyboard.KEY_G);
                if (isG && !wasGDown) {
                    ItemStack hovered = getHoveredItemStack(container);
                    if (hovered != null && hovered.getItem() != null) {
                        screenManager.openGraphForStack(Forge1710Platform.toRgvStack(hovered));
                    } else {
                        screenManager.openGraph();
                    }
                }
                wasGDown = isG;

                boolean isF = Keyboard.isKeyDown(Keyboard.KEY_F);
                if (isCtrl && isF && !wasFDown) {
                    screenManager.getSearchBar().setFocused(true);
                }
                wasFDown = isF;

                boolean isO = Keyboard.isKeyDown(Keyboard.KEY_O);
                if (isCtrl && isO && !wasODown) {
                    screenManager.getConfig().setOverlayEnabled(!screenManager.getConfig().isOverlayEnabled());
                }
                wasODown = isO;
            }
        }

        // Render RGV Overlay (bookmarks and, if NEI is inactive, item index)
        screenManager.render(renderer, event.mouseX, event.mouseY, event.renderPartialTicks);

        // Check if a container slot is currently hovered by Minecraft
        Slot hoveredSlot = getHoveredSlot(container);
        if (hoveredSlot == null || !hoveredSlot.getHasStack()) {
            // Render tooltip for RgvHostPlannerButton if mouse is over it and not over a slot
            if (hostBtn != null && hostBtn.visible && hostBtn.isMouseOver()) {
                renderer.drawTooltip(hostBtn.getTooltip(), event.mouseX, event.mouseY);
            }
        } else {
            // A slot is hovered: re-render the slot's tooltip on the very top of everything,
            // ensuring wide item tooltips are never covered by RGV bookmarks or sidebars!
            reRenderSlotTooltip(container, hoveredSlot.getStack(), event.mouseX, event.mouseY);
        }
    }

    private void handleNeiRecipeScreen(GuiContainer container, int mouseX, int mouseY, float delta) {
        if (screenManager.getRecipeScreen().isOpen()) {
            int guiLeft = 0;
            int guiTop = 0;
            int xSize = 176;
            int ySize = 166;
            try {
                if (fGuiLeft != null) guiLeft = fGuiLeft.getInt(container);
                if (fGuiTop != null) guiTop = fGuiTop.getInt(container);
                if (fXSize != null) xSize = fXSize.getInt(container);
                if (fYSize != null) ySize = fYSize.getInt(container);
            } catch (Exception ignored) {
            }
            screenManager.updateBounds(container.width, container.height, guiLeft, guiTop, xSize, ySize);

            // Render Craft Graph modal
            screenManager.getRecipeScreen().render(renderer, mouseX, mouseY, delta);
            screenManager.getRecipeScreen().renderTooltips(renderer, mouseX, mouseY);

            // Handle modal clicks & scrolling
            boolean isLeftDown = Mouse.isButtonDown(0);
            if (isLeftDown && !wasLeftDown) {
                screenManager.getRecipeScreen().mouseClicked(mouseX, mouseY, 0);
            }
            wasLeftDown = isLeftDown;

            boolean isRightDown = Mouse.isButtonDown(1);
            if (isRightDown && !wasRightDown) {
                screenManager.getRecipeScreen().mouseClicked(mouseX, mouseY, 1);
            }
            wasRightDown = isRightDown;

            int dWheel = Mouse.getDWheel();
            if (dWheel != 0) {
                screenManager.getRecipeScreen().mouseScrolled(dWheel);
            }

            boolean isEsc = Keyboard.isKeyDown(Keyboard.KEY_ESCAPE);
            if (isEsc && !wasEscDown) {
                screenManager.getRecipeScreen().close();
            }
            wasEscDown = isEsc;
            return;
        }

        // If no slot is hovered, render tooltips for RgvNeiGraphButton if hovered
        Slot hoveredSlot = getHoveredSlot(container);
        if (hoveredSlot == null || !hoveredSlot.getHasStack()) {
            if (neiBtn0 != null && neiBtn0.visible && neiBtn0.isMouseOver()) {
                renderer.drawTooltip(neiBtn0.getTooltip(), mouseX, mouseY);
            } else if (neiBtn1 != null && neiBtn1.visible && neiBtn1.isMouseOver()) {
                renderer.drawTooltip(neiBtn1.getTooltip(), mouseX, mouseY);
            }
        }

        boolean isLeftDown = Mouse.isButtonDown(0);
        if (isLeftDown && !wasLeftDown) {
            NeiRecipeHelper.mouseClicked(container, mouseX, mouseY, 0, screenManager);
        }
        wasLeftDown = isLeftDown;

        boolean isG = Keyboard.isKeyDown(Keyboard.KEY_G);
        if (isG && !wasGDown) {
            NeiRecipeHelper.addCurrentRecipeToGraph(container, mouseX, mouseY, screenManager);
        }
        wasGDown = isG;
    }

    private Slot getHoveredSlot(GuiContainer container) {
        if (fTheSlot != null && container != null) {
            try {
                return (Slot) fTheSlot.get(container);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private void reRenderSlotTooltip(GuiContainer container, ItemStack stack, int mouseX, int mouseY) {
        if (mRenderToolTip != null && container != null && stack != null) {
            try {
                mRenderToolTip.invoke(container, stack, mouseX, mouseY);
            } catch (Throwable ignored) {
            }
        }
    }

    private ItemStack getHoveredItemStack(GuiContainer container) {
        Slot s = getHoveredSlot(container);
        if (s != null && s.getHasStack()) {
            return s.getStack();
        }
        RgvStack hovered = screenManager.getHoveredStack();
        if (hovered != null && !hovered.isEmpty()) {
            return Forge1710Platform.toMinecraftStack(hovered);
        }
        return null;
    }
}
