package ru.nexsqaud.rgv.platform.forge1122;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;
import org.lwjgl.input.Mouse;
import ru.nexsqaud.rgv.api.*;
import ru.nexsqaud.rgv.core.platform.RgvPlatform;
import ru.nexsqaud.rgv.core.recipe.RgvRecipeManager;
import ru.nexsqaud.rgv.core.screen.RgvScreenManager;
import ru.nexsqaud.rgv.platform.forge1122.client.ClientTooltipHelper1122;
import ru.nexsqaud.rgv.platform.forge1122.compat.jei.JeiIntegration;
import ru.nexsqaud.rgv.platform.forge1122.network.GiveItemMessage1122;
import ru.nexsqaud.rgv.platform.forge1122.network.RgvPacketHandler1122;
import ru.nexsqaud.rgv.platform.forge1122.network.TransferRecipeMessage1122;

import java.io.File;
import java.util.*;

/**
 * Minecraft 1.12.2 implementation of RgvPlatform.
 */
public class Forge1122Platform implements RgvPlatform {

    private static RgvScreenManager screenManager;

    public static void setScreenManager(RgvScreenManager manager) {
        screenManager = manager;
    }

    public static RgvScreenManager getScreenManager() {
        return screenManager;
    }

    public static RgvRecipeManager getRecipeManager() {
        return CommonProxy1122.getRecipeManager();
    }

    public static RgvRecipe findRecipeById(String id) {
        if (id == null) return null;
        for (RgvRecipe r : CommonProxy1122.getRecipeManager().getAllRecipes()) {
            if (Objects.equals(r.getId(), id)) {
                return r;
            }
        }
        return null;
    }

    public static RgvStack toRgvStack(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem() == null) {
            return RgvStack.empty();
        }

        ResourceLocation reg = stack.getItem().getRegistryName();
        String id = reg != null ? reg.toString() : "unknown";
        int meta = stack.getItemDamage();
        long amount = stack.getCount();

        String displayName = stack.getDisplayName();
        List<String> tooltip;
        if (FMLCommonHandler.instance().getSide().isClient()) {
            tooltip = ClientTooltipHelper1122.getTooltip(stack, displayName);
        } else {
            tooltip = Collections.singletonList(displayName);
        }

        return RgvStack.ofPayload(id, meta, amount, displayName, tooltip, stack.copy());
    }

    public static ItemStack toMinecraftStack(RgvStack stack) {
        if (stack == null || stack.isEmpty()) return ItemStack.EMPTY;

        ItemStack unwrapped = stack.unwrap(ItemStack.class);
        if (unwrapped != null && !unwrapped.isEmpty()) {
            ItemStack copy = unwrapped.copy();
            copy.setCount((int) Math.max(1, Math.min(stack.getAmount(), copy.getMaxStackSize())));
            return copy;
        }

        ResourceLocation loc = null;
        try {
            if (stack.getId() != null && !stack.getId().isEmpty()) {
                loc = new ResourceLocation(stack.getId());
            }
        } catch (Throwable ignored) {
        }
        Item item = loc != null ? ForgeRegistries.ITEMS.getValue(loc) : null;
        if (item == null) {
            item = Item.getByNameOrId(stack.getId());
        }
        if (item == null) return ItemStack.EMPTY;

        int meta = stack.getMeta();
        if (meta == OreDictionary.WILDCARD_VALUE || meta < 0) {
            meta = 0;
        }
        return new ItemStack(item, (int) Math.min(64, Math.max(1, stack.getAmount())), meta);
    }

    @Override
    public boolean areStacksEqual(RgvStack a, RgvStack b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        ItemStack sa = a.unwrap(ItemStack.class);
        ItemStack sb = b.unwrap(ItemStack.class);
        if (sa != null && sb != null) {
            if (sa.getItem() != sb.getItem()) return false;
            int metaA = sa.getItemDamage();
            int metaB = sb.getItemDamage();
            if (metaA != OreDictionary.WILDCARD_VALUE && metaB != OreDictionary.WILDCARD_VALUE && metaA != metaB) return false;
            return ItemStack.areItemStackTagsEqual(sa, sb);
        }
        return a.matches(b);
    }

    @Override
    public RgvStack getRemainderItem(RgvStack stack) {
        if (stack == null || stack.isEmpty()) return RgvStack.empty();
        ItemStack is = toMinecraftStack(stack);
        if (is != null && !is.isEmpty()) {
            ItemStack container = is.getItem().getContainerItem(is);
            if (container != null && !container.isEmpty()) {
                return toRgvStack(container);
            }
        }
        return RgvStack.empty();
    }

    @Override
    public boolean isInventoryKey(int keyCode) {
        if (FMLCommonHandler.instance().getSide().isClient()) {
            return ClientTooltipHelper1122.isInventoryKey(keyCode);
        }
        return false;
    }

    @Override
    public List<RgvStack> getAllKnownStacks() {
        List<RgvStack> items = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (Item item : ForgeRegistries.ITEMS.getValuesCollection()) {
            if (item == null) continue;

            NonNullList<ItemStack> subItems = NonNullList.create();
            try {
                item.getSubItems(CreativeTabs.SEARCH, subItems);
            } catch (Throwable t) {
                try {
                    item.getSubItems(item.getCreativeTab() != null ? item.getCreativeTab() : CreativeTabs.MISC, subItems);
                } catch (Throwable ignored) {
                }
            }

            if (subItems.isEmpty()) {
                ItemStack def = new ItemStack(item, 1, 0);
                if (!def.isEmpty()) {
                    subItems.add(def);
                }
            }

            for (ItemStack sub : subItems) {
                if (sub != null && !sub.isEmpty() && sub.getItem() != null) {
                    ResourceLocation reg = sub.getItem().getRegistryName();
                    String key = (reg != null ? reg.toString() : "item") + "@" + sub.getItemDamage();
                    if (seen.add(key)) {
                        items.add(toRgvStack(sub));
                    }
                }
            }
        }
        return items;
    }

    @Override
    public List<RgvIngredient> getOreDictionaryIngredients() {
        List<RgvIngredient> result = new ArrayList<>();
        String[] names = OreDictionary.getOreNames();
        for (String name : names) {
            NonNullList<ItemStack> ores = OreDictionary.getOres(name);
            if (ores != null && !ores.isEmpty()) {
                List<RgvStack> list = new ArrayList<>();
                for (ItemStack ore : ores) {
                    if (ore != null && !ore.isEmpty()) {
                        list.add(toRgvStack(ore));
                    }
                }
                if (!list.isEmpty()) {
                    result.add(RgvIngredientList.of(list, 1));
                }
            }
        }
        return result;
    }

    @Override
    public RgvInventory getPlayerInventory() {
        if (FMLCommonHandler.instance().getSide().isClient()) {
            return ClientTooltipHelper1122.getPlayerInventory();
        }
        return null;
    }

    @Override
    public String translateKey(String key, Object... args) {
        if (FMLCommonHandler.instance().getSide().isClient()) {
            return ClientTooltipHelper1122.translate(key, args);
        }
        return key;
    }

    @Override
    public boolean transferRecipe(RgvRecipe recipe, boolean maxCraft) {
        if (recipe == null) return false;
        if (FMLCommonHandler.instance().getSide().isClient()) {
            boolean emulated = ru.nexsqaud.rgv.platform.forge1122.client.ClientTransferHelper1122.transferRecipe(recipe, maxCraft);
            if (emulated) return true;
        }
        sendTransferRecipePacket(recipe.getId(), maxCraft);
        return true;
    }

    @Override
    public void sendTransferRecipePacket(String recipeId, boolean maxCraft) {
        RgvPacketHandler1122.INSTANCE.sendToServer(new TransferRecipeMessage1122(recipeId, maxCraft));
    }

    @Override
    public void sendGiveItemPacket(RgvStack stack, boolean fullStack) {
        if (stack == null || stack.isEmpty()) return;
        RgvPacketHandler1122.INSTANCE.sendToServer(new GiveItemMessage1122(stack.getId(), stack.getMeta(), fullStack ? 64 : 1));
        if (FMLCommonHandler.instance().getSide().isClient()) {
            ItemStack mcStack = toMinecraftStack(stack);
            if (!mcStack.isEmpty()) {
                ClientTooltipHelper1122.giveCreativeItem(mcStack, fullStack);
            }
        }
    }

    @Override
    public boolean isCheatModeAllowed() {
        if (FMLCommonHandler.instance().getSide().isClient()) {
            return ClientTooltipHelper1122.isCheatModeAllowed();
        }
        return false;
    }

    @Override
    public int getScreenWidth() {
        if (FMLCommonHandler.instance().getSide().isClient()) {
            return ClientTooltipHelper1122.getScreenWidth();
        }
        return 0;
    }

    @Override
    public int getScreenHeight() {
        if (FMLCommonHandler.instance().getSide().isClient()) {
            return ClientTooltipHelper1122.getScreenHeight();
        }
        return 0;
    }

    @Override
    public int getGuiScale() {
        if (FMLCommonHandler.instance().getSide().isClient()) {
            return ClientTooltipHelper1122.getGuiScale();
        }
        return 1;
    }

    @Override
    public File getConfigDirectory() {
        return new File("config/rgv");
    }

    @Override
    public boolean hasSuitableSlotsFor(RgvRecipe recipe) {
        if (FMLCommonHandler.instance().getSide().isClient()) {
            return ru.nexsqaud.rgv.platform.forge1122.client.ClientTransferHelper1122.hasSuitableSlotsFor(recipe);
        }
        return false;
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
        if (FMLCommonHandler.instance().getSide().isClient() && ingredient instanceof RgvStack) {
            RgvStack single = ((RgvStack) ingredient).copyWithAmount(1);
            ItemStack stack = toMinecraftStack(single);
            if (!stack.isEmpty()) {
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
        if (FMLCommonHandler.instance().getSide().isClient()) {
            return ClientTooltipHelper1122.getTextWidth(text);
        }
        return text != null ? text.length() * 6 : 0;
    }

    public static boolean isJeiPresent() {
        try {
            return Loader.isModLoaded("jei");
        } catch (Throwable t) {
            return false;
        }
    }
}
