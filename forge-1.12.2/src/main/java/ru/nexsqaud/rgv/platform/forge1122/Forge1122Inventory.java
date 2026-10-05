package ru.nexsqaud.rgv.platform.forge1122;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import ru.nexsqaud.rgv.api.RgvIngredient;
import ru.nexsqaud.rgv.api.RgvInventory;
import ru.nexsqaud.rgv.api.RgvStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Wraps Minecraft 1.12.2 InventoryPlayer into RgvInventory.
 */
public class Forge1122Inventory implements RgvInventory {

    private final InventoryPlayer playerInventory;

    public Forge1122Inventory(InventoryPlayer playerInventory) {
        this.playerInventory = playerInventory;
    }

    @Override
    public long getAmount(RgvIngredient ingredient) {
        if (ingredient == null || ingredient.isEmpty() || playerInventory == null) {
            return 0;
        }

        long count = 0;
        for (ItemStack stack : playerInventory.mainInventory) {
            if (stack != null && !stack.isEmpty()) {
                RgvStack rgv = Forge1122Platform.toRgvStack(stack);
                if (ingredient.matches(rgv)) {
                    count += stack.getCount();
                }
            }
        }
        for (ItemStack stack : playerInventory.offHandInventory) {
            if (stack != null && !stack.isEmpty()) {
                RgvStack rgv = Forge1122Platform.toRgvStack(stack);
                if (ingredient.matches(rgv)) {
                    count += stack.getCount();
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

        for (ItemStack stack : playerInventory.mainInventory) {
            if (stack != null && !stack.isEmpty()) {
                list.add(Forge1122Platform.toRgvStack(stack));
            }
        }
        for (ItemStack stack : playerInventory.offHandInventory) {
            if (stack != null && !stack.isEmpty()) {
                list.add(Forge1122Platform.toRgvStack(stack));
            }
        }
        return list;
    }
}
