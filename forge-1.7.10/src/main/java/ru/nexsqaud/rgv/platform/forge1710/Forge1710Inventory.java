package ru.nexsqaud.rgv.platform.forge1710;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import ru.nexsqaud.rgv.api.RgvIngredient;
import ru.nexsqaud.rgv.api.RgvInventory;
import ru.nexsqaud.rgv.api.RgvStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Wraps Minecraft 1.7.10 InventoryPlayer into RgvInventory.
 */
public class Forge1710Inventory implements RgvInventory {

    private final InventoryPlayer playerInventory;

    public Forge1710Inventory(InventoryPlayer playerInventory) {
        this.playerInventory = playerInventory;
    }

    @Override
    public long getAmount(RgvIngredient ingredient) {
        if (ingredient == null || ingredient.isEmpty() || playerInventory == null) {
            return 0;
        }

        long count = 0;
        // Main inventory slots 0..35
        for (int i = 0; i < playerInventory.mainInventory.length; i++) {
            ItemStack stack = playerInventory.mainInventory[i];
            if (stack != null && stack.getItem() != null) {
                RgvStack rgv = Forge1710Platform.toRgvStack(stack);
                if (ingredient.matches(rgv)) {
                    count += stack.stackSize;
                }
            }
        }
        return count;
    }

    @Override
    public boolean hasIngredient(RgvIngredient ingredient) {
        return getAmount(ingredient) >= (ingredient != null ? ingredient.getAmount() : 1);
    }

    @Override
    public List<RgvStack> getAllStacks() {
        List<RgvStack> list = new ArrayList<>();
        if (playerInventory == null) return list;

        for (int i = 0; i < playerInventory.mainInventory.length; i++) {
            ItemStack stack = playerInventory.mainInventory[i];
            if (stack != null && stack.getItem() != null) {
                list.add(Forge1710Platform.toRgvStack(stack));
            }
        }
        return list;
    }
}
