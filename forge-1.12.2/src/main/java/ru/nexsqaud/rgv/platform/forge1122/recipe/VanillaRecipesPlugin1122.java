package ru.nexsqaud.rgv.platform.forge1122.recipe;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.crafting.IShapedRecipe;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import ru.nexsqaud.rgv.api.*;
import ru.nexsqaud.rgv.api.widget.RgvWidgetHolder;
import ru.nexsqaud.rgv.platform.forge1122.Forge1122Platform;

import java.util.*;

/**
 * Discovers Vanilla and Forge 1.12.2 crafting and furnace recipes,
 * and registers recipe transfer handlers for standard containers.
 */
public class VanillaRecipesPlugin1122 implements RgvPlugin {

    private static final int[] SHAPELESS_2X2 = {0, 1, 3, 4};

    public static final RgvRecipeCategory CRAFTING_CATEGORY = new RgvRecipeCategory(
            "rgv.crafting", "Crafting", Forge1122Platform.toRgvStack(new ItemStack(Blocks.CRAFTING_TABLE)), 130, 64
    );

    public static final RgvRecipeCategory SMELTING_CATEGORY = new RgvRecipeCategory(
            "rgv.smelting", "Smelting", Forge1122Platform.toRgvStack(new ItemStack(Blocks.FURNACE)), 126, 60
    );

    @Override
    public void register(RgvRegistry registry) {
        registry.addCategory(CRAFTING_CATEGORY);
        registry.addCategory(SMELTING_CATEGORY);

        registry.addWorkstation(CRAFTING_CATEGORY.getId(), Forge1122Platform.toRgvStack(new ItemStack(Blocks.CRAFTING_TABLE)));
        registry.addWorkstation(SMELTING_CATEGORY.getId(), Forge1122Platform.toRgvStack(new ItemStack(Blocks.FURNACE)));

        registerCraftingRecipes(registry);
        registerSmeltingRecipes(registry);
        registerTransferHandlers(registry);
    }

    private void registerCraftingRecipes(RgvRegistry registry) {
        int index = 0;
        for (IRecipe recipe : ForgeRegistries.RECIPES.getValuesCollection()) {
            if (recipe == null) continue;
            ItemStack output = recipe.getRecipeOutput();
            if (output.isEmpty()) continue;

            String recipeId = recipe.getRegistryName() != null ? recipe.getRegistryName().toString() : ("crafting_" + (index++));
            RgvStack outputStack = Forge1122Platform.toRgvStack(output);
            List<RgvIngredient> inputGrid = new ArrayList<>(Collections.nCopies(9, RgvStack.empty()));

            NonNullList<Ingredient> ingredients = recipe.getIngredients();
            if (recipe instanceof IShapedRecipe) {
                IShapedRecipe shaped = (IShapedRecipe) recipe;
                int w = shaped.getRecipeWidth();
                int h = shaped.getRecipeHeight();
                for (int y = 0; y < h && y < 3; y++) {
                    for (int x = 0; x < w && x < 3; x++) {
                        int srcIndex = x + y * w;
                        if (srcIndex < ingredients.size()) {
                            inputGrid.set(x + y * 3, convertIngredient(ingredients.get(srcIndex)));
                        }
                    }
                }
            } else {
                int[] gridSlots = ingredients.size() <= 4 ? SHAPELESS_2X2 : new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8};
                for (int i = 0; i < Math.min(ingredients.size(), gridSlots.length); i++) {
                    inputGrid.set(gridSlots[i], convertIngredient(ingredients.get(i)));
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

    private RgvIngredient convertIngredient(Ingredient ingredient) {
        if (ingredient == null || ingredient == Ingredient.EMPTY) return RgvStack.empty();
        ItemStack[] matching = ingredient.getMatchingStacks();
        if (matching == null || matching.length == 0) return RgvStack.empty();

        List<RgvStack> list = new ArrayList<>();
        for (ItemStack s : matching) {
            if (s != null && !s.isEmpty()) {
                list.addAll(Forge1122Platform.expandStack(s));
            }
        }
        if (list.isEmpty()) return RgvStack.empty();
        if (list.size() == 1) return list.get(0);
        return RgvIngredientList.of(list, 1);
    }

    private void registerSmeltingRecipes(RgvRegistry registry) {
        Map<ItemStack, ItemStack> smeltingList = FurnaceRecipes.instance().getSmeltingList();
        int index = 0;

        for (Map.Entry<ItemStack, ItemStack> entry : smeltingList.entrySet()) {
            ItemStack in = entry.getKey();
            ItemStack out = entry.getValue();
            if (in == null || out == null || in.isEmpty() || out.isEmpty()) continue;

            String recipeId = "smelting_" + (index++);
            RgvStack inStack = Forge1122Platform.toRgvStack(in).copyWithAmount(1);
            RgvStack outStack = Forge1122Platform.toRgvStack(out);

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
            public boolean canTransfer(RgvRecipe recipe, ContainerWorkbench container, RgvInventory playerInventory) {
                return recipe.getCategory().getId().equals(CRAFTING_CATEGORY.getId()) && recipe.canCraft(playerInventory);
            }

            @Override
            public void transfer(RgvRecipe recipe, ContainerWorkbench container, boolean maxCraft) {
                transferToCraftingMatrix(container, container.craftMatrix, recipe, false);
            }
        });

        registry.addRecipeHandler(new RgvRecipeHandler<ContainerPlayer>() {
            @Override
            public boolean canHandle(ContainerPlayer container) {
                return container != null;
            }

            @Override
            public boolean canTransfer(RgvRecipe recipe, ContainerPlayer container, RgvInventory playerInventory) {
                return recipe.getCategory().getId().equals(CRAFTING_CATEGORY.getId()) && canFitIn2x2(recipe) && recipe.canCraft(playerInventory);
            }

            @Override
            public void transfer(RgvRecipe recipe, ContainerPlayer container, boolean maxCraft) {
                if (!canFitIn2x2(recipe)) return;
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
                if (!existing.isEmpty()) {
                    for (Slot pSlot : playerSlots) {
                        if (existing.isEmpty()) break;
                        if (pSlot.getHasStack()) {
                            ItemStack pStack = pSlot.getStack();
                            if (pStack.getItem() == existing.getItem() && pStack.getItemDamage() == existing.getItemDamage() && ItemStack.areItemStackTagsEqual(pStack, existing)) {
                                int space = Math.min(pSlot.getSlotStackLimit(), pStack.getMaxStackSize()) - pStack.getCount();
                                int toMove = Math.min(space, existing.getCount());
                                if (toMove > 0) {
                                    pStack.grow(toMove);
                                    existing.shrink(toMove);
                                }
                            }
                        }
                    }
                    for (Slot pSlot : playerSlots) {
                        if (existing.isEmpty()) break;
                        if (!pSlot.getHasStack()) {
                            int toMove = Math.min(pSlot.getSlotStackLimit(), existing.getCount());
                            ItemStack newStack = existing.copy();
                            newStack.setCount(toMove);
                            pSlot.putStack(newStack);
                            existing.shrink(toMove);
                        }
                    }
                    if (!existing.isEmpty() && player != null) {
                        player.dropItem(existing.copy(), false);
                    }
                }
                cSlot.putStack(ItemStack.EMPTY);
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
                    if (ing.matches(Forge1122Platform.toRgvStack(pStack))) {
                        ItemStack taken = pSlot.decrStackSize(1);
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
