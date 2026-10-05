package ru.nexsqaud.rgv.platform.forge1710;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.oredict.OreDictionary;
import ru.nexsqaud.rgv.api.RgvIngredient;
import ru.nexsqaud.rgv.api.RgvIngredientList;
import ru.nexsqaud.rgv.api.RgvInventory;
import ru.nexsqaud.rgv.api.RgvRecipe;
import ru.nexsqaud.rgv.api.RgvStack;
import ru.nexsqaud.rgv.core.platform.RgvPlatform;
import ru.nexsqaud.rgv.core.recipe.RgvRecipeManager;
import ru.nexsqaud.rgv.core.screen.RgvScreenManager;
import ru.nexsqaud.rgv.platform.forge1710.client.ClientTooltipHelper;
import ru.nexsqaud.rgv.platform.forge1710.network.GiveItemMessage;
import ru.nexsqaud.rgv.platform.forge1710.network.RgvPacketHandler;
import ru.nexsqaud.rgv.platform.forge1710.network.TransferRecipeMessage;

import java.io.File;
import java.util.*;

/**
 * Implements RgvPlatform for Minecraft 1.7.10 with Forge.
 */
public class Forge1710Platform implements RgvPlatform {

    private static RgvScreenManager screenManager;

    public static void setScreenManager(RgvScreenManager manager) {
        screenManager = manager;
    }

    public static RgvScreenManager getScreenManager() {
        return screenManager;
    }

    public static RgvRecipeManager getRecipeManager() {
        return CommonProxy.getRecipeManager();
    }

    public static RgvRecipe findRecipeById(String id) {
        if (id == null) return null;
        for (RgvRecipe r : CommonProxy.getRecipeManager().getAllRecipes()) {
            if (Objects.equals(r.getId(), id)) {
                return r;
            }
        }
        return null;
    }

    public static RgvStack toRgvStack(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return RgvStack.empty();
        }

        String id = Item.itemRegistry.getNameForObject(stack.getItem());
        if (id == null) id = "unknown:" + stack.getItem().getUnlocalizedName();
        int meta = stack.getItemDamage();
        long amount = stack.stackSize;

        String displayName = stack.getDisplayName();
        List<String> tooltip;
        if (FMLCommonHandler.instance().getSide().isClient()) {
            tooltip = ClientTooltipHelper.getTooltip(stack, displayName);
        } else {
            tooltip = Collections.singletonList(displayName);
        }

        return RgvStack.ofPayload(id, meta, amount, displayName, tooltip, stack.copy());
    }

    public static ItemStack toMinecraftStack(RgvStack rgv) {
        if (rgv == null || rgv.isEmpty()) return null;
        ItemStack unwrapped = rgv.unwrap(ItemStack.class);
        if (unwrapped != null) {
            ItemStack copy = unwrapped.copy();
            copy.stackSize = (int) Math.max(1, Math.min(rgv.getAmount(), copy.getMaxStackSize()));
            return copy;
        }

        Item item = (Item) Item.itemRegistry.getObject(rgv.getId());
        if (item == null) return null;

        int meta = rgv.getMeta() == 32767 ? 0 : rgv.getMeta();
        int count = (int) Math.max(1, Math.min(rgv.getAmount(), item.getItemStackLimit()));
        return new ItemStack(item, count, meta);
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
            if (metaA != 32767 && metaB != 32767 && metaA != metaB) return false;
            return ItemStack.areItemStackTagsEqual(sa, sb);
        }
        return a.matches(b);
    }

    @Override
    public boolean isInventoryKey(int keyCode) {
        if (FMLCommonHandler.instance().getSide().isClient()) {
            return ClientTooltipHelper.isInventoryKey(keyCode);
        }
        return false;
    }

    @Override
    public List<RgvStack> getAllKnownStacks() {
        List<RgvStack> items = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        Iterator iterator = Item.itemRegistry.iterator();
        while (iterator.hasNext()) {
            Object obj = iterator.next();
            if (!(obj instanceof Item)) continue;
            Item item = (Item) obj;

            List<ItemStack> subItems = new ArrayList<>();
            try {
                item.getSubItems(item, CreativeTabs.tabAllSearch, subItems);
            } catch (Throwable t) {
                try {
                    item.getSubItems(item, null, subItems);
                } catch (Throwable ignored) {
                }
            }

            if (subItems.isEmpty()) {
                if (item.getCreativeTab() != null || item.getHasSubtypes()) {
                    subItems.add(new ItemStack(item, 1, 0));
                }
            }

            for (ItemStack sub : subItems) {
                if (sub != null && sub.getItem() != null) {
                    RgvStack rgv = toRgvStack(sub);
                    String key = rgv.getId() + ":" + rgv.getMeta();
                    if (!seen.contains(key)) {
                        seen.add(key);
                        items.add(rgv);
                    }
                }
            }
        }

        items.sort(Comparator.comparing(RgvStack::getDisplayName));
        return items;
    }

    @Override
    public List<RgvIngredient> getOreDictionaryIngredients() {
        List<RgvIngredient> list = new ArrayList<>();
        String[] oreNames = OreDictionary.getOreNames();
        for (String name : oreNames) {
            ArrayList<ItemStack> ores = OreDictionary.getOres(name);
            if (ores != null && !ores.isEmpty()) {
                List<RgvStack> stacks = new ArrayList<>();
                for (ItemStack s : ores) {
                    if (s != null && s.getItem() != null) {
                        stacks.add(toRgvStack(s));
                    }
                }
                if (!stacks.isEmpty()) {
                    list.add(RgvIngredientList.ofTag(name, stacks, 1));
                }
            }
        }
        return list;
    }

    @Override
    public RgvInventory getPlayerInventory() {
        if (FMLCommonHandler.instance().getSide().isClient()) {
            return ClientTooltipHelper.getPlayerInventory();
        }
        return null;
    }

    @Override
    public String translateKey(String key, Object... args) {
        if (args != null && args.length > 0) {
            return StatCollector.translateToLocalFormatted(key, args);
        }
        return StatCollector.translateToLocal(key);
    }

    @Override
    public boolean transferRecipe(RgvRecipe recipe, boolean maxCraft) {
        if (recipe == null) return false;
        if (FMLCommonHandler.instance().getSide().isClient()) {
            boolean emulated = ru.nexsqaud.rgv.platform.forge1710.client.ClientTransferHelper.transferRecipe(recipe, maxCraft);
            if (emulated) return true;
        }
        sendTransferRecipePacket(recipe.getId(), maxCraft);
        return true;
    }

    @Override
    public void sendTransferRecipePacket(String recipeId, boolean maxCraft) {
        RgvPacketHandler.NETWORK.sendToServer(new TransferRecipeMessage(recipeId, maxCraft));
    }

    @Override
    public void sendGiveItemPacket(RgvStack stack, boolean fullStack) {
        if (stack == null || stack.isEmpty()) return;
        int count = fullStack ? 64 : 1;
        RgvPacketHandler.NETWORK.sendToServer(new GiveItemMessage(stack.getId(), stack.getMeta(), count));
        if (FMLCommonHandler.instance().getSide().isClient()) {
            ItemStack mcStack = toMinecraftStack(stack);
            if (mcStack != null) {
                ClientTooltipHelper.giveCreativeItem(mcStack, fullStack);
            }
        }
    }

    @Override
    public boolean isCheatModeAllowed() {
        if (FMLCommonHandler.instance().getSide().isClient()) {
            return ClientTooltipHelper.isCheatModeAllowed();
        }
        return false;
    }

    @Override
    public int getScreenWidth() {
        if (FMLCommonHandler.instance().getSide().isClient()) {
            return ClientTooltipHelper.getScreenWidth();
        }
        return 0;
    }

    @Override
    public int getScreenHeight() {
        if (FMLCommonHandler.instance().getSide().isClient()) {
            return ClientTooltipHelper.getScreenHeight();
        }
        return 0;
    }

    @Override
    public int getGuiScale() {
        if (FMLCommonHandler.instance().getSide().isClient()) {
            return ClientTooltipHelper.getGuiScale();
        }
        return 1;
    }

    @Override
    public File getConfigDirectory() {
        return new File("config/rgv");
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
            ItemStack stack = toMinecraftStack((RgvStack) ingredient);
            if (stack != null && isNeiPresent()) {
                return ru.nexsqaud.rgv.platform.forge1710.compat.nei.NeiRecipeHelper.openRecipeGui(stack, isUsage);
            }
        }
        return false;
    }

    @Override
    public boolean isMouseButtonDown(int button) {
        try {
            return org.lwjgl.input.Mouse.isButtonDown(button);
        } catch (Throwable t) {
            return true;
        }
    }

    public static boolean isNeiPresent() {
        try {
            return Loader.isModLoaded("NotEnoughItems");
        } catch (Throwable t) {
            return false;
        }
    }
}
