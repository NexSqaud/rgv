package ru.nexsqaud.rgv.platform.forge1710.recipe;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.*;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import ru.nexsqaud.rgv.api.*;
import ru.nexsqaud.rgv.api.widget.RgvWidgetHolder;
import ru.nexsqaud.rgv.platform.forge1710.Forge1710Platform;

import java.util.*;

/**
 * Discovers Vanilla 1.7.10 crafting and furnace recipes,
 * and registers recipe transfer handlers for standard containers.
 */
@RgvEntrypoint
public class VanillaRecipesPlugin implements RgvPlugin {

    private static final int[] SHAPELESS_2X2 = {0, 1, 3, 4};

    public static final RgvRecipeCategory CRAFTING_CATEGORY = new RgvRecipeCategory(
            "rgv.crafting", "Crafting", Forge1710Platform.toRgvStack(new ItemStack(Blocks.crafting_table)), 130, 64
    );

    public static final RgvRecipeCategory SMELTING_CATEGORY = new RgvRecipeCategory(
            "rgv.smelting", "Smelting", Forge1710Platform.toRgvStack(new ItemStack(Blocks.furnace)), 126, 60
    );

    @Override
    public void register(RgvRegistry registry) {
        registry.addCategory(CRAFTING_CATEGORY);
        registry.addCategory(SMELTING_CATEGORY);

        registry.addWorkstation(CRAFTING_CATEGORY.getId(), Forge1710Platform.toRgvStack(new ItemStack(Blocks.crafting_table)));
        registry.addWorkstation(SMELTING_CATEGORY.getId(), Forge1710Platform.toRgvStack(new ItemStack(Blocks.furnace)));

        registerCraftingRecipes(registry);
        registerSmeltingRecipes(registry);
        registerTransferHandlers(registry);
    }

    private void registerCraftingRecipes(RgvRegistry registry) {
        @SuppressWarnings("unchecked")
        List<IRecipe> recipeList = CraftingManager.getInstance().getRecipeList();
        int index = 0;

        for (IRecipe recipe : recipeList) {
            if (recipe == null || recipe.getRecipeOutput() == null) continue;
            String recipeId = "crafting_" + (index++);

            RgvStack outputStack = Forge1710Platform.toRgvStack(recipe.getRecipeOutput());
            List<RgvIngredient> inputGrid = new ArrayList<>(Collections.nCopies(9, RgvStack.empty()));

            if (recipe instanceof ShapedRecipes) {
                ShapedRecipes shaped = (ShapedRecipes) recipe;
                int w = shaped.recipeWidth;
                int h = shaped.recipeHeight;
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int srcIndex = x + y * w;
                        if (srcIndex < shaped.recipeItems.length && shaped.recipeItems[srcIndex] != null) {
                            inputGrid.set(x + y * 3, Forge1710Platform.toRgvStack(shaped.recipeItems[srcIndex]).copyWithAmount(1));
                        }
                    }
                }
            } else if (recipe instanceof ShapelessRecipes) {
                ShapelessRecipes shapeless = (ShapelessRecipes) recipe;
                @SuppressWarnings("unchecked")
                List<ItemStack> items = shapeless.recipeItems;
                int[] gridSlots = items.size() <= 4 ? SHAPELESS_2X2 : new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8};
                for (int i = 0; i < Math.min(items.size(), gridSlots.length); i++) {
                    if (items.get(i) != null) {
                        inputGrid.set(gridSlots[i], Forge1710Platform.toRgvStack(items.get(i)).copyWithAmount(1));
                    }
                }
            } else if (recipe instanceof ShapedOreRecipe) {
                ShapedOreRecipe oreShaped = (ShapedOreRecipe) recipe;
                Object[] inputItems = oreShaped.getInput();
                int w = 3;
                try {
                    java.lang.reflect.Field fWidth = ShapedOreRecipe.class.getDeclaredField("width");
                    fWidth.setAccessible(true);
                    w = fWidth.getInt(oreShaped);
                } catch (Exception ignored) {
                }

                int h = (inputItems.length + w - 1) / w;
                for (int y = 0; y < h && y < 3; y++) {
                    for (int x = 0; x < w && x < 3; x++) {
                        int srcIndex = x + y * w;
                        if (srcIndex < inputItems.length) {
                            inputGrid.set(x + y * 3, convertOreInput(inputItems[srcIndex]));
                        }
                    }
                }
            } else if (recipe instanceof ShapelessOreRecipe) {
                ShapelessOreRecipe oreShapeless = (ShapelessOreRecipe) recipe;
                ArrayList<Object> inputs = oreShapeless.getInput();
                int[] gridSlots = inputs.size() <= 4 ? SHAPELESS_2X2 : new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8};
                for (int i = 0; i < Math.min(inputs.size(), gridSlots.length); i++) {
                    inputGrid.set(gridSlots[i], convertOreInput(inputs.get(i)));
                }
            }

            for (int i = 0; i < inputGrid.size(); i++) {
                RgvIngredient ing = inputGrid.get(i);
                if (ing != null && !ing.isEmpty()) {
                    if (ing.getAmount() != 1) {
                        inputGrid.set(i, ing.copyWithAmount(1));
                    }
                }
            }

            final List<RgvIngredient> finalInputs = Collections.unmodifiableList(new ArrayList<>(inputGrid));

            registry.addRecipe(new RgvRecipe() {
                @Override
                public String getId() {
                    return recipeId;
                }

                @Override
                public RgvRecipeCategory getCategory() {
                    return CRAFTING_CATEGORY;
                }

                @Override
                public List<RgvIngredient> getInputs() {
                    return finalInputs;
                }

                @Override
                public List<RgvStack> getOutputs() {
                    return Collections.singletonList(outputStack);
                }

                @Override
                public void addWidgets(RgvWidgetHolder holder) {
                    for (int y = 0; y < 3; y++) {
                        for (int x = 0; x < 3; x++) {
                            RgvIngredient ing = finalInputs.get(x + y * 3);
                            holder.addSlot(ing, x * 18 + 4, y * 18 + 4);
                        }
                    }
                    holder.addArrow(64, 22, false);
                    holder.addOutputSlot(outputStack, 94, 18);
                }
            });
        }
    }

    private RgvIngredient convertOreInput(Object input) {
        if (input == null) return RgvStack.empty();
        if (input instanceof ItemStack) {
            List<RgvStack> expanded = Forge1710Platform.expandStack((ItemStack) input);
            if (expanded.isEmpty()) return RgvStack.empty();
            if (expanded.size() == 1) return expanded.get(0);
            return RgvIngredientList.of(expanded, 1);
        }
        if (input instanceof ItemStack[]) {
            ItemStack[] array = (ItemStack[]) input;
            List<RgvStack> stacks = new ArrayList<>();
            for (ItemStack s : array) {
                if (s != null && s.getItem() != null) {
                    stacks.addAll(Forge1710Platform.expandStack(s));
                }
            }
            return stacks.isEmpty() ? RgvStack.empty() : RgvIngredientList.of(stacks, 1);
        }
        if (input instanceof List) {
            @SuppressWarnings("unchecked")
            List<?> list = (List<?>) input;
            List<RgvStack> stacks = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof ItemStack) {
                    ItemStack s = (ItemStack) item;
                    if (s.getItem() != null) {
                        stacks.addAll(Forge1710Platform.expandStack(s));
                    }
                }
            }
            return stacks.isEmpty() ? RgvStack.empty() : RgvIngredientList.of(stacks, 1);
        }
        if (input instanceof String) {
            ArrayList<ItemStack> ores = OreDictionary.getOres((String) input);
            List<RgvStack> stacks = new ArrayList<>();
            for (ItemStack s : ores) {
                if (s != null && s.getItem() != null) {
                    stacks.addAll(Forge1710Platform.expandStack(s));
                }
            }
            return stacks.isEmpty() ? RgvStack.empty() : RgvIngredientList.ofTag((String) input, stacks, 1);
        }
        return RgvStack.empty();
    }

    private void registerSmeltingRecipes(RgvRegistry registry) {
        @SuppressWarnings("unchecked")
        Map<ItemStack, ItemStack> smeltingList = FurnaceRecipes.smelting().getSmeltingList();
        int index = 0;

        for (Map.Entry<ItemStack, ItemStack> entry : smeltingList.entrySet()) {
            ItemStack in = entry.getKey();
            ItemStack out = entry.getValue();
            if (in == null || out == null || in.getItem() == null || out.getItem() == null) continue;

            String recipeId = "smelting_" + (index++);
            RgvStack inStack = Forge1710Platform.toRgvStack(in).copyWithAmount(1);
            RgvStack outStack = Forge1710Platform.toRgvStack(out);

            registry.addRecipe(new RgvRecipe() {
                @Override
                public String getId() {
                    return recipeId;
                }

                @Override
                public RgvRecipeCategory getCategory() {
                    return SMELTING_CATEGORY;
                }

                @Override
                public List<RgvIngredient> getInputs() {
                    return Collections.singletonList(inStack);
                }

                @Override
                public List<RgvStack> getOutputs() {
                    return Collections.singletonList(outStack);
                }

                @Override
                public void addWidgets(RgvWidgetHolder holder) {
                    holder.addLargeSlot(inStack, 16, 8);
                    holder.addFlame(22, 38, true, 10000);
                    holder.addArrow(51, 13, true, 10000);
                    holder.addOutputSlot(outStack, 84, 8);
                }
            });
        }
    }

    private void registerTransferHandlers(RgvRegistry registry) {
        registry.addRecipeHandler(new RgvRecipeHandler<ContainerWorkbench>() {
            @Override
            public boolean canHandle(ContainerWorkbench container) {
                return container != null;
            }

            @Override
            public boolean canTransfer(RgvRecipe recipe, ContainerWorkbench container, RgvInventory inventory) {
                return recipe.getCategory().equals(CRAFTING_CATEGORY) && recipe.canCraft(inventory);
            }

            @Override
            public void transfer(RgvRecipe recipe, ContainerWorkbench container, boolean maxCraft) {
                if (!recipe.getCategory().equals(CRAFTING_CATEGORY)) return;
                transferToCraftingMatrix(container, container.craftMatrix, recipe, false);
            }
        });

        registry.addRecipeHandler(new RgvRecipeHandler<ContainerPlayer>() {
            @Override
            public boolean canHandle(ContainerPlayer container) {
                return container != null;
            }

            @Override
            public boolean canTransfer(RgvRecipe recipe, ContainerPlayer container, RgvInventory inventory) {
                return recipe.getCategory().equals(CRAFTING_CATEGORY) && canFitIn2x2(recipe) && recipe.canCraft(inventory);
            }

            @Override
            public void transfer(RgvRecipe recipe, ContainerPlayer container, boolean maxCraft) {
                if (!recipe.getCategory().equals(CRAFTING_CATEGORY) || !canFitIn2x2(recipe)) return;
                transferToCraftingMatrix(container, container.craftMatrix, recipe, true);
            }
        });
    }

    private boolean canFitIn2x2(RgvRecipe recipe) {
        if (recipe == null) return false;
        List<RgvIngredient> inputs = recipe.getInputs();
        if (inputs.size() <= 4) return true;
        int[] outOfBounds = {2, 5, 6, 7, 8};
        for (int idx : outOfBounds) {
            if (idx < inputs.size()) {
                RgvIngredient ing = inputs.get(idx);
                if (ing != null && !ing.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    private void transferToCraftingMatrix(Container container, InventoryCrafting craftMatrix, RgvRecipe recipe, boolean is2x2) {
        if (container == null || craftMatrix == null || recipe == null) return;
        @SuppressWarnings("unchecked")
        List<Slot> allSlots = container.inventorySlots;
        if (allSlots == null) return;

        List<Slot> craftSlots = new ArrayList<>();
        List<Slot> playerSlots = new ArrayList<>();
        EntityPlayer player = null;

        for (Slot slot : allSlots) {
            if (slot == null) continue;
            if (slot.inventory == craftMatrix) {
                craftSlots.add(slot);
            } else if (slot.inventory instanceof InventoryPlayer) {
                if (container instanceof ContainerPlayer && slot.slotNumber >= 5 && slot.slotNumber <= 8) {
                    continue;
                }
                playerSlots.add(slot);
                if (player == null) {
                    player = ((InventoryPlayer) slot.inventory).player;
                }
            }
        }

        // Return existing items in craftMatrix to player inventory or drop them
        for (Slot cSlot : craftSlots) {
            if (cSlot.getHasStack()) {
                ItemStack existing = cSlot.getStack();
                if (existing != null && existing.stackSize > 0) {
                    for (Slot pSlot : playerSlots) {
                        if (existing.stackSize <= 0) break;
                        if (pSlot.getHasStack()) {
                            ItemStack pStack = pSlot.getStack();
                            if (pStack.getItem() == existing.getItem() && pStack.getItemDamage() == existing.getItemDamage() && ItemStack.areItemStackTagsEqual(pStack, existing)) {
                                int space = Math.min(pSlot.getSlotStackLimit(), pStack.getMaxStackSize()) - pStack.stackSize;
                                int toMove = Math.min(space, existing.stackSize);
                                if (toMove > 0) {
                                    pStack.stackSize += toMove;
                                    existing.stackSize -= toMove;
                                }
                            }
                        }
                    }
                    for (Slot pSlot : playerSlots) {
                        if (existing.stackSize <= 0) break;
                        if (!pSlot.getHasStack()) {
                            int toMove = Math.min(pSlot.getSlotStackLimit(), existing.stackSize);
                            ItemStack newStack = existing.copy();
                            newStack.stackSize = toMove;
                            pSlot.putStack(newStack);
                            existing.stackSize -= toMove;
                        }
                    }
                    if (existing.stackSize > 0 && player != null) {
                        player.dropPlayerItemWithRandomChoice(existing.copy(), false);
                    }
                }
                cSlot.putStack(null);
            }
        }

        List<RgvIngredient> inputs = recipe.getInputs();
        int[] slotMapping = is2x2 ? SHAPELESS_2X2 : new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8};

        for (int craftSlotIdx = 0; craftSlotIdx < Math.min(slotMapping.length, craftSlots.size()); craftSlotIdx++) {
            int inputIdx = slotMapping[craftSlotIdx];
            if (inputIdx >= inputs.size()) break;
            RgvIngredient ing = inputs.get(inputIdx);
            if (ing == null || ing.isEmpty()) continue;

            Slot targetSlot = craftSlots.get(craftSlotIdx);
            for (Slot pSlot : playerSlots) {
                if (pSlot.getHasStack()) {
                    ItemStack pStack = pSlot.getStack();
                    if (ing.matches(Forge1710Platform.toRgvStack(pStack))) {
                        ItemStack taken = pSlot.decrStackSize(1);
                        if (pSlot.getStack() != null && pSlot.getStack().stackSize <= 0) {
                            pSlot.putStack(null);
                        }
                        targetSlot.putStack(taken);
                        break;
                    }
                }
            }
        }

        container.onCraftMatrixChanged(craftMatrix);
        container.detectAndSendChanges();
    }
}
