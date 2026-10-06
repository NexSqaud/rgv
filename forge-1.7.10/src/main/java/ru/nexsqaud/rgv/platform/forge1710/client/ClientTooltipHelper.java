package ru.nexsqaud.rgv.platform.forge1710.client;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.item.ItemStack;
import ru.nexsqaud.rgv.api.RgvInventory;
import ru.nexsqaud.rgv.platform.forge1710.Forge1710Inventory;

import java.util.ArrayList;
import java.util.List;

@SideOnly(Side.CLIENT)
public class ClientTooltipHelper {

    public static List<String> getTooltip(ItemStack stack, String fallback) {
        List<String> tooltip = new ArrayList<>();
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null && mc.thePlayer != null) {
                @SuppressWarnings("unchecked")
                List<String> mcTooltip = stack.getTooltip(mc.thePlayer, mc.gameSettings != null && mc.gameSettings.advancedItemTooltips);
                if (mcTooltip != null) {
                    tooltip.addAll(mcTooltip);
                    return tooltip;
                }
            }
        } catch (Throwable ignored) {
        }
        tooltip.add(fallback);
        return tooltip;
    }

    public static boolean isInventoryKey(int keyCode) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            return mc != null && mc.gameSettings != null && mc.gameSettings.keyBindInventory.getKeyCode() == keyCode;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static RgvInventory getPlayerInventory() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null && mc.thePlayer != null) {
                return new Forge1710Inventory(mc.thePlayer.inventory);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static boolean isCheatModeAllowed() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            return mc != null && mc.thePlayer != null && mc.thePlayer.capabilities.isCreativeMode;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void giveCreativeItem(ItemStack stack, boolean fullStack) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null && mc.thePlayer != null && mc.thePlayer.capabilities.isCreativeMode && mc.playerController != null) {
                int count = fullStack ? 64 : 1;
                ItemStack copy = stack.copy();
                copy.stackSize = Math.min(count, copy.getMaxStackSize());
                int targetSlot = 36 + mc.thePlayer.inventory.currentItem;
                mc.playerController.sendSlotPacket(copy, targetSlot);
            }
        } catch (Throwable ignored) {
        }
    }

    public static int getScreenWidth() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null && mc.displayWidth > 0 && mc.displayHeight > 0) {
                ScaledResolution res = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
                return res.getScaledWidth();
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    public static int getScreenHeight() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null && mc.displayWidth > 0 && mc.displayHeight > 0) {
                ScaledResolution res = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
                return res.getScaledHeight();
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    public static int getGuiScale() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null && mc.displayWidth > 0 && mc.displayHeight > 0) {
                ScaledResolution res = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
                return res.getScaleFactor();
            }
        } catch (Throwable ignored) {
        }
        return 1;
    }

    public static boolean openExternalRecipeViewer(ItemStack stack, boolean isUsage) {
        if (stack == null || stack.getItem() == null) return false;
        if (!ru.nexsqaud.rgv.platform.forge1710.Forge1710Platform.isNeiPresent()) return false;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) return false;
        net.minecraft.client.gui.GuiScreen current = mc.currentScreen;
        net.minecraft.client.gui.GuiScreen containerParent = null;
        if (current instanceof ru.nexsqaud.rgv.platform.forge1710.gui.RgvGuiScreen) {
            containerParent = ((ru.nexsqaud.rgv.platform.forge1710.gui.RgvGuiScreen) current).getParentScreen();
            mc.currentScreen = containerParent;
        }
        boolean opened = ru.nexsqaud.rgv.platform.forge1710.compat.nei.NeiRecipeHelper.openRecipeGui(stack, isUsage);
        if (!opened && current instanceof ru.nexsqaud.rgv.platform.forge1710.gui.RgvGuiScreen) {
            mc.currentScreen = current;
        }
        return opened;
    }
}
