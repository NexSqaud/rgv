package ru.nexsqaud.rgv.platform.forge1710.client;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;
import org.lwjgl.input.Mouse;
import ru.nexsqaud.rgv.api.RgvIngredient;
import ru.nexsqaud.rgv.api.RgvInventory;
import ru.nexsqaud.rgv.api.RgvRecipe;
import ru.nexsqaud.rgv.api.RgvStack;
import ru.nexsqaud.rgv.platform.forge1710.Forge1710Platform;

import java.util.Collections;
import java.util.List;

/**
 * Client-specific platform implementation for Minecraft 1.7.10.
 * Keeps all client-only dependencies (LWJGL, NEI, ClientTooltipHelper) isolated from the server.
 */
@SideOnly(Side.CLIENT)
public class Forge1710ClientPlatform extends Forge1710Platform {

    @Override
    public List<String> getTooltip(ItemStack stack, String displayName) {
        if (stack == null) return Collections.singletonList(displayName);
        try {
            int meta = stack.getItemDamage();
            boolean isWildcard = (meta == OreDictionary.WILDCARD_VALUE || meta == Short.MAX_VALUE || meta < 0);
            ItemStack safeForTooltip = isWildcard ? stack.copy() : stack;
            if (isWildcard) {
                safeForTooltip.setItemDamage(0);
            }
            List<String> list = ClientTooltipHelper.getTooltip(safeForTooltip, displayName);
            if (list != null && !list.isEmpty()) {
                return list;
            }
        } catch (Throwable ignored) {
        }
        return Collections.singletonList(displayName);
    }

    @Override
    public boolean isInventoryKey(int keyCode) {
        return ClientTooltipHelper.isInventoryKey(keyCode);
    }

    @Override
    public RgvInventory getPlayerInventory() {
        return ClientTooltipHelper.getPlayerInventory();
    }

    @Override
    public boolean transferRecipe(RgvRecipe recipe, boolean maxCraft) {
        if (recipe == null) return false;
        boolean emulated = ClientTransferHelper.transferRecipe(recipe, maxCraft);
        if (emulated) return true;
        sendTransferRecipePacket(recipe.getId(), maxCraft);
        return true;
    }

    @Override
    public void sendGiveItemPacket(RgvStack stack, boolean fullStack) {
        super.sendGiveItemPacket(stack, fullStack);
        ItemStack mcStack = toMinecraftStack(stack);
        if (mcStack != null) {
            ClientTooltipHelper.giveCreativeItem(mcStack, fullStack);
        }
    }

    @Override
    public boolean isCheatModeAllowed() {
        return ClientTooltipHelper.isCheatModeAllowed();
    }

    @Override
    public int getScreenWidth() {
        return ClientTooltipHelper.getScreenWidth();
    }

    @Override
    public int getScreenHeight() {
        return ClientTooltipHelper.getScreenHeight();
    }

    @Override
    public int getGuiScale() {
        return ClientTooltipHelper.getGuiScale();
    }

    @Override
    public boolean hasSuitableSlotsFor(RgvRecipe recipe) {
        return ClientTransferHelper.hasSuitableSlotsFor(recipe);
    }

    @Override
    public boolean isRecipeViewerPresent() {
        return isNeiPresent();
    }

    @Override
    public boolean isRecipePanelVisible() {
        return isNeiPresent() && ru.nexsqaud.rgv.platform.forge1710.compat.nei.NeiRecipeHelper.isNeiPanelVisible();
    }

    @Override
    public boolean openExternalRecipeViewer(RgvIngredient ingredient, boolean isUsage) {
        if (ingredient instanceof RgvStack) {
            RgvStack single = ((RgvStack) ingredient).copyWithAmount(1);
            ItemStack stack = toMinecraftStack(single);
            if (stack != null) {
                return ClientTooltipHelper.openExternalRecipeViewer(stack, isUsage);
            }
        }
        return false;
    }

    @Override
    public boolean isMouseButtonDown(int button) {
        try {
            return Mouse.isButtonDown(button);
        } catch (Throwable t) {
            return true;
        }
    }

    @Override
    public int getTextWidth(String text) {
        return ClientTooltipHelper.getTextWidth(text);
    }
}
