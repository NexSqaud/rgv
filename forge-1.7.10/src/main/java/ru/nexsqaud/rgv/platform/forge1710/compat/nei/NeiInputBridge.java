package ru.nexsqaud.rgv.platform.forge1710.compat.nei;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import ru.nexsqaud.rgv.core.screen.RgvScreenManager;
import ru.nexsqaud.rgv.platform.forge1710.Forge1710Platform;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Bridges NEI's container input pipeline (IContainerInputHandler) with RGV using dynamic proxy reflection.
 * Ensures that RGV modal dialogs, Graph hotkeys ('G'), and bookmark hotkeys ('A') intercept events cleanly,
 * while allowing NEI to receive all 'R' / 'U' recipe lookups and full control of the right-side item index.
 */
public class NeiInputBridge {

    private static final Logger LOG = LogManager.getLogger("RGV-NEI-Input");
    private static boolean registered = false;

    public static void register(final RgvScreenManager screenManager) {
        if (registered || screenManager == null) return;
        if (!Forge1710Platform.isNeiPresent()) return;

        try {
            Class<?> handlerInterface = Class.forName("codechicken.nei.guihook.IContainerInputHandler");
            Class<?> managerClass = Class.forName("codechicken.nei.guihook.GuiContainerManager");

            Object proxy = Proxy.newProxyInstance(
                    NeiInputBridge.class.getClassLoader(),
                    new Class<?>[]{handlerInterface},
                    new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            String name = method.getName();
                            if ("keyTyped".equals(name)) {
                                GuiContainer gui = (GuiContainer) args[0];
                                char keyChar = (Character) args[1];
                                int keyCode = (Integer) args[2];
                                return handleKeyTyped(gui, keyChar, keyCode, screenManager);
                            } else if ("mouseClicked".equals(name)) {
                                GuiContainer gui = (GuiContainer) args[0];
                                int mouseX = (Integer) args[1];
                                int mouseY = (Integer) args[2];
                                int button = (Integer) args[3];
                                return handleMouseClicked(gui, mouseX, mouseY, button, screenManager);
                            } else if ("mouseScrolled".equals(name)) {
                                GuiContainer gui = (GuiContainer) args[0];
                                int mouseX = (Integer) args[1];
                                int mouseY = (Integer) args[2];
                                int scrolled = (Integer) args[3];
                                return handleMouseScrolled(gui, mouseX, mouseY, scrolled, screenManager);
                            }

                            if (method.getReturnType() == boolean.class) {
                                return Boolean.FALSE;
                            }
                            return null;
                        }
                    }
            );

            Method addMethod = managerClass.getMethod("addInputHandler", handlerInterface);
            addMethod.invoke(null, proxy);
            registered = true;
            LOG.info("Registered RGV into NEI IContainerInputHandler pipeline successfully.");
        } catch (Throwable t) {
            LOG.debug("Could not register NEI IContainerInputHandler: " + t.getMessage());
        }
    }

    private static boolean handleKeyTyped(GuiContainer gui, char keyChar, int keyCode, RgvScreenManager screenManager) {
        if (!screenManager.getConfig().isOverlayEnabled()) return false;

        // If modal recipe/graph screen is open:
        if (screenManager.getRecipeScreen().isOpen()) {
            if (keyCode == 1) { // Escape closes modal without closing the host container
                screenManager.getRecipeScreen().close();
                return true;
            }
            return screenManager.getRecipeScreen().keyPressed(keyCode, keyChar);
        }

        // Inside NEI's GuiRecipe screen:
        if (NeiRecipeHelper.isGuiRecipe(gui)) {
            if (keyCode == Keyboard.KEY_G) {
                Minecraft mc = Minecraft.getMinecraft();
                if (mc == null || mc.displayWidth <= 0 || mc.displayHeight <= 0 || mc.currentScreen == null) return false;
                int mouseX = Mouse.getX() * mc.currentScreen.width / mc.displayWidth;
                int mouseY = mc.currentScreen.height - Mouse.getY() * mc.currentScreen.height / mc.displayHeight - 1;
                NeiRecipeHelper.addCurrentRecipeToGraph(gui, mouseX, mouseY, screenManager);
                return true;
            }
            return false;
        }

        // Inside standard GuiContainer:
        if (keyCode == Keyboard.KEY_G) {
            ItemStack hovered = NeiRecipeHelper.getStackMouseOver(gui);
            if (hovered != null && hovered.getItem() != null) {
                screenManager.openGraphForStack(Forge1710Platform.toRgvStack(hovered));
            } else {
                screenManager.openGraph();
            }
            return true;
        }

        if (keyCode == Keyboard.KEY_A) {
            ItemStack hovered = NeiRecipeHelper.getStackMouseOver(gui);
            if (hovered != null && hovered.getItem() != null) {
                screenManager.getConfig().toggleBookmark(Forge1710Platform.toRgvStack(hovered));
                return true;
            }
        }

        // Bookmark hover shortcut
        if (screenManager.isHoveringBookmark() && screenManager.getHoveredStack() != null) {
            if (keyCode == Keyboard.KEY_R) {
                screenManager.handleRKey();
                return true;
            }
            if (keyCode == Keyboard.KEY_U) {
                screenManager.handleUKey();
                return true;
            }
        }

        return false;
    }

    private static boolean handleMouseClicked(GuiContainer gui, int mouseX, int mouseY, int button, RgvScreenManager screenManager) {
        if (!screenManager.getConfig().isOverlayEnabled()) return false;

        if (screenManager.getRecipeScreen().isOpen()) {
            screenManager.getRecipeScreen().mouseClicked(mouseX, mouseY, button);
            return true;
        }

        if (NeiRecipeHelper.isGuiRecipe(gui)) {
            if (button == 0 && NeiRecipeHelper.mouseClicked(gui, mouseX, mouseY, button, screenManager)) {
                return true;
            }
            return false;
        }

        return screenManager.mouseClicked(mouseX, mouseY, button);
    }

    private static boolean handleMouseScrolled(GuiContainer gui, int mouseX, int mouseY, int scrolled, RgvScreenManager screenManager) {
        if (screenManager.getRecipeScreen().isOpen()) {
            return screenManager.getRecipeScreen().mouseScrolled(scrolled);
        }
        return false;
    }
}
