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
import ru.nexsqaud.rgv.api.*;
import ru.nexsqaud.rgv.core.platform.RgvPlatform;
import ru.nexsqaud.rgv.core.recipe.RgvRecipeManager;
import ru.nexsqaud.rgv.core.screen.RgvScreenManager;
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

        ResourceLocation reg = null;
        try {
            reg = stack.getItem().getRegistryName();
        } catch (Throwable ignored) {
        }
        String id = reg != null ? reg.toString() : "unknown";
        int meta = stack.getItemDamage();
        long amount = stack.getCount();

        String displayName = null;
        boolean isWildcard = (meta == OreDictionary.WILDCARD_VALUE || meta == Short.MAX_VALUE || meta < 0);
        try {
            if (isWildcard) {
                ItemStack safe = stack.copy();
                safe.setItemDamage(0);
                displayName = safe.getDisplayName();
            } else {
                displayName = stack.getDisplayName();
            }
        } catch (Throwable t) {
            try {
                displayName = stack.getItem().getItemStackDisplayName(stack);
            } catch (Throwable t2) {
                displayName = id;
            }
        }
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = id;
        }

        List<String> tooltip = null;
        RgvPlatform platform = RgvPlatform.get();
        if (platform instanceof Forge1122Platform) {
            tooltip = ((Forge1122Platform) platform).getTooltip(stack, displayName);
        }
        if (tooltip == null || tooltip.isEmpty()) {
            tooltip = Collections.singletonList(displayName);
        }

        return RgvStack.ofPayload(id, meta, amount, displayName, tooltip, stack.copy());
    }

    public List<String> getTooltip(ItemStack stack, String displayName) {
        return Collections.singletonList(displayName);
    }

    public static List<RgvStack> expandStack(ItemStack s) {
        List<RgvStack> result = new ArrayList<>();
        if (s == null || s.isEmpty() || s.getItem() == null) return result;

        int meta = s.getItemDamage();
        if (meta == OreDictionary.WILDCARD_VALUE || meta == Short.MAX_VALUE) {
            NonNullList<ItemStack> subItems = NonNullList.create();
            try {
                s.getItem().getSubItems(net.minecraft.creativetab.CreativeTabs.SEARCH, subItems);
            } catch (Throwable ignored) {
            }
            if (!subItems.isEmpty()) {
                for (ItemStack sub : subItems) {
                    if (sub != null && !sub.isEmpty() && sub.getItem() != null) {
                        result.add(toRgvStack(sub).copyWithAmount(1));
                    }
                }
                return result;
            }
        }

        result.add(toRgvStack(s).copyWithAmount(1));
        return result;
    }

    public static ItemStack toMinecraftStack(RgvStack stack) {
        if (stack == null || stack.isEmpty()) return ItemStack.EMPTY;

        ItemStack unwrapped = stack.unwrap(ItemStack.class);
        if (unwrapped != null && !unwrapped.isEmpty()) {
            ItemStack copy = unwrapped.copy();
            copy.setCount((int) Math.max(1, Math.min(stack.getAmount(), copy.getMaxStackSize())));
            if (copy.getItemDamage() == OreDictionary.WILDCARD_VALUE || copy.getItemDamage() == Short.MAX_VALUE || copy.getItemDamage() < 0) {
                copy.setItemDamage(0);
            }
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
        if (meta == OreDictionary.WILDCARD_VALUE || meta == Short.MAX_VALUE || meta < 0) {
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
            if (metaA != OreDictionary.WILDCARD_VALUE && metaA != Short.MAX_VALUE
                    && metaB != OreDictionary.WILDCARD_VALUE && metaB != Short.MAX_VALUE
                    && metaA != metaB) return false;
            return ItemStack.areItemStackTagsEqual(sa, sb);
        }
        return true;
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
                        list.addAll(expandStack(ore));
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
        return null;
    }

    @Override
    public String translateKey(String key, Object... args) {
        return key;
    }

    @Override
    public boolean transferRecipe(RgvRecipe recipe, boolean maxCraft) {
        if (recipe == null) return false;
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
    }

    @Override
    public boolean isCheatModeAllowed() {
        return false;
    }

    @Override
    public int getScreenWidth() {
        return 0;
    }

    @Override
    public int getScreenHeight() {
        return 0;
    }

    @Override
    public int getGuiScale() {
        return 1;
    }

    @Override
    public File getConfigDirectory() {
        return new File("config/rgv");
    }

    @Override
    public boolean hasSuitableSlotsFor(RgvRecipe recipe) {
        return false;
    }

    @Override
    public boolean isRecipeViewerPresent() {
        return isJeiPresent();
    }

    @Override
    public boolean isRecipePanelVisible() {
        return false;
    }

    @Override
    public boolean openExternalRecipeViewer(RgvIngredient ingredient, boolean isUsage) {
        return false;
    }

    @Override
    public boolean isMouseButtonDown(int button) {
        return false;
    }

    @Override
    public int getTextWidth(String text) {
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
