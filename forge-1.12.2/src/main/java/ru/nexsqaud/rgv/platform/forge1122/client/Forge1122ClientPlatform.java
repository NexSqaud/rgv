package ru.nexsqaud.rgv.platform.forge1122.client;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.OreDictionary;
import org.lwjgl.input.Mouse;
import ru.nexsqaud.rgv.api.RgvIngredient;
import ru.nexsqaud.rgv.api.RgvInventory;
import ru.nexsqaud.rgv.api.RgvRecipe;
import ru.nexsqaud.rgv.api.RgvStack;
import ru.nexsqaud.rgv.platform.forge1122.Forge1122Platform;
import ru.nexsqaud.rgv.platform.forge1122.compat.jei.JeiIntegration;

import java.util.Collections;
import java.util.List;

/**
 * Client-specific platform implementation for Minecraft 1.12.2.
 * Isolates all client and LWJGL references from the dedicated server.
 */
@SideOnly(Side.CLIENT)
public class Forge1122ClientPlatform extends Forge1122Platform {

    @Override
    public List<String> getTooltip(ItemStack stack, String displayName) {
        if (stack == null || stack.isEmpty()) return Collections.singletonList(displayName);
        try {
            int meta = stack.getItemDamage();
            boolean isWildcard = (meta == OreDictionary.WILDCARD_VALUE || meta == Short.MAX_VALUE || meta < 0);
            ItemStack safeForTooltip = isWildcard ? stack.copy() : stack;
            if (isWildcard) {
                safeForTooltip.setItemDamage(0);
            }
            List<String> list = ClientTooltipHelper1122.getTooltip(safeForTooltip, displayName);
            if (list != null && !list.isEmpty()) {
                return list;
            }
        } catch (Throwable ignored) {
        }
        return Collections.singletonList(displayName);
    }

    @Override
    public boolean isInventoryKey(int keyCode) {
        return ClientTooltipHelper1122.isInventoryKey(keyCode);
    }

    @Override
    public RgvInventory getPlayerInventory() {
        return ClientTooltipHelper1122.getPlayerInventory();
    }

    @Override
    public String translateKey(String key, Object... args) {
        return ClientTooltipHelper1122.translate(key, args);
    }

    @Override
    public boolean transferRecipe(RgvRecipe recipe, boolean maxCraft) {
        if (recipe == null) return false;
        boolean emulated = ClientTransferHelper1122.transferRecipe(recipe, maxCraft);
        if (emulated) return true;
        sendTransferRecipePacket(recipe.getId(), maxCraft);
        return true;
    }

    @Override
    public void sendGiveItemPacket(RgvStack stack, boolean fullStack) {
        super.sendGiveItemPacket(stack, fullStack);
        ItemStack mcStack = toMinecraftStack(stack);
        if (mcStack != null && !mcStack.isEmpty()) {
            ClientTooltipHelper1122.giveCreativeItem(mcStack, fullStack);
        }
    }

    @Override
    public boolean isCheatModeAllowed() {
        return ClientTooltipHelper1122.isCheatModeAllowed();
    }

    @Override
    public int getScreenWidth() {
        return ClientTooltipHelper1122.getScreenWidth();
    }

    @Override
    public int getScreenHeight() {
        return ClientTooltipHelper1122.getScreenHeight();
    }

    @Override
    public int getGuiScale() {
        return ClientTooltipHelper1122.getGuiScale();
    }

    @Override
    public boolean hasSuitableSlotsFor(RgvRecipe recipe) {
        return ClientTransferHelper1122.hasSuitableSlotsFor(recipe);
    }

    @Override
    public boolean isRecipeViewerPresent() {
        return isJeiPresent();
    }

    @Override
    public boolean isRecipePanelVisible() {
        return isJeiPresent() && JeiIntegration.isJeiPanelVisible();
    }

    @Override
    public boolean openExternalRecipeViewer(RgvIngredient ingredient, boolean isUsage) {
        if (ingredient instanceof RgvStack) {
            RgvStack single = ((RgvStack) ingredient).copyWithAmount(1);
            ItemStack stack = toMinecraftStack(single);
            if (stack != null && !stack.isEmpty()) {
                return ClientTooltipHelper1122.openExternalRecipeViewer(stack, isUsage);
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
        return ClientTooltipHelper1122.getTextWidth(text);
    }
}
