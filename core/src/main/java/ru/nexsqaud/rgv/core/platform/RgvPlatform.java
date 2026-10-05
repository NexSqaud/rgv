package ru.nexsqaud.rgv.core.platform;

import ru.nexsqaud.rgv.api.RgvIngredient;
import ru.nexsqaud.rgv.api.RgvInventory;
import ru.nexsqaud.rgv.api.RgvRecipe;
import ru.nexsqaud.rgv.api.RgvStack;

import java.io.File;
import java.util.List;

/**
 * Service Provider Interface (SPI) implemented by the host platform
 * (e.g. Forge 1.7.10, Fabric 1.20, NeoForge, etc.).
 */
public interface RgvPlatform {

    class Holder {
        private static RgvPlatform INSTANCE;
    }

    static void setInstance(RgvPlatform platform) {
        Holder.INSTANCE = platform;
    }

    static RgvPlatform get() {
        return Holder.INSTANCE;
    }

    List<RgvStack> getAllKnownStacks();

    List<RgvIngredient> getOreDictionaryIngredients();

    RgvInventory getPlayerInventory();

    String translateKey(String key, Object... args);

    void sendTransferRecipePacket(String recipeId, boolean maxCraft);

    default boolean transferRecipe(RgvRecipe recipe, boolean maxCraft) {
        if (recipe != null) {
            sendTransferRecipePacket(recipe.getId(), maxCraft);
            return true;
        }
        return false;
    }

    default boolean hasSuitableSlotsFor(RgvRecipe recipe) {
        return false;
    }

    default TransferStatus getTransferStatus(RgvRecipe recipe) {
        if (recipe == null) {
            return TransferStatus.NO_SUITABLE_CONTAINER;
        }
        if (!hasSuitableSlotsFor(recipe)) {
            return TransferStatus.NO_SUITABLE_CONTAINER;
        }
        RgvInventory inv = getPlayerInventory();
        if (inv == null || !recipe.canCraft(inv)) {
            return TransferStatus.MISSING_INGREDIENTS;
        }
        return TransferStatus.AVAILABLE;
    }

    default String getRequiredContainerDescription(RgvRecipe recipe) {
        return TransferHelper.getRequiredContainerDescription(recipe);
    }

    void sendGiveItemPacket(RgvStack stack, boolean fullStack);

    boolean isCheatModeAllowed();

    int getScreenWidth();

    int getScreenHeight();

    int getGuiScale();

    File getConfigDirectory();

    default boolean isRecipeViewerPresent() {
        return false;
    }

    default boolean isRecipePanelVisible() {
        return false;
    }

    default boolean openExternalRecipeViewer(RgvIngredient ingredient, boolean isUsage) {
        return false;
    }

    default boolean areStacksEqual(RgvStack a, RgvStack b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.matches(b);
    }

    default boolean isInventoryKey(int keyCode) {
        return false;
    }

    default boolean isMouseButtonDown(int button) {
        return true;
    }
}
