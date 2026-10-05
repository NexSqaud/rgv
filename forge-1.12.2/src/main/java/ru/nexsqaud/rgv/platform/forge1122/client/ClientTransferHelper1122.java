package ru.nexsqaud.rgv.platform.forge1122.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import ru.nexsqaud.rgv.api.RgvIngredient;
import ru.nexsqaud.rgv.api.RgvRecipe;
import ru.nexsqaud.rgv.platform.forge1122.Forge1122Platform;

import java.util.*;

@SideOnly(Side.CLIENT)
public class ClientTransferHelper1122 {

    public static boolean transferRecipe(RgvRecipe recipe, boolean maxCraft) {
        if (recipe == null) return false;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.player == null || mc.playerController == null) return false;

        EntityPlayerSP player = mc.player;
        Container container = player.openContainer;
        if (container == null) return false;

        // Ensure the mouse cursor is not currently holding an item
        if (!player.inventory.getItemStack().isEmpty()) return false;

        boolean isWorkbench = container instanceof ContainerWorkbench;
        boolean isPlayerCrafting = container instanceof ContainerPlayer;
        if (!isWorkbench && !isPlayerCrafting) {
            return false;
        }

        boolean is2x2 = isPlayerCrafting;
        if (is2x2 && !canFitIn2x2(recipe)) {
            return false;
        }

        List<Slot> allSlots = container.inventorySlots;
        if (allSlots == null || allSlots.isEmpty()) return false;

        List<Slot> craftSlots = new ArrayList<>();
        List<Slot> playerSlots = new ArrayList<>();

        for (Slot slot : allSlots) {
            if (slot == null) continue;
            if (isWorkbench) {
                if (slot.slotNumber >= 1 && slot.slotNumber <= 9) {
                    craftSlots.add(slot);
                } else if (slot.slotNumber >= 10 && slot.slotNumber <= 45) {
                    playerSlots.add(slot);
                }
            } else {
                if (slot.slotNumber >= 1 && slot.slotNumber <= 4) {
                    craftSlots.add(slot);
                } else if ((slot.slotNumber >= 9 && slot.slotNumber <= 44) || slot.slotNumber == 45) {
                    playerSlots.add(slot);
                }
            }
        }

        if (craftSlots.isEmpty()) return false;

        // Step 1: Clear existing items in crafting grid by shift-clicking them into inventory
        for (Slot cSlot : craftSlots) {
            if (cSlot.getHasStack()) {
                mc.playerController.windowClick(container.windowId, cSlot.slotNumber, 0, ClickType.QUICK_MOVE, player);
                if (cSlot.getHasStack()) {
                    // Inventory was full, unable to clear crafting matrix
                    return false;
                }
            }
        }

        // Step 2: Determine how many sets to craft
        int[] slotMapping = is2x2 ? new int[]{0, 1, 3, 4} : new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8};
        List<RgvIngredient> inputs = recipe.getInputs();

        int timesToCraft = 1;
        if (maxCraft) {
            timesToCraft = calculateMaxCraft(recipe, playerSlots, slotMapping);
        }

        // Track how many items each craft slot still needs
        int[] needed = new int[craftSlots.size()];
        for (int i = 0; i < craftSlots.size(); i++) {
            int inputIdx = slotMapping[i];
            if (inputIdx < inputs.size() && inputs.get(inputIdx) != null && !inputs.get(inputIdx).isEmpty()) {
                needed[i] = timesToCraft;
            } else {
                needed[i] = 0;
            }
        }

        // Step 3: Emulate moving items by hand
        while (hasRemainingNeeded(needed)) {
            int targetIdx = getFirstNeededIndex(needed);
            if (targetIdx == -1) break;
            int inputIdx = slotMapping[targetIdx];
            RgvIngredient ing = inputs.get(inputIdx);

            // Find a player slot with a matching item
            Slot sourceSlot = null;
            for (Slot pSlot : playerSlots) {
                if (pSlot.getHasStack() && ing.matches(Forge1122Platform.toRgvStack(pSlot.getStack()))) {
                    sourceSlot = pSlot;
                    break;
                }
            }
            if (sourceSlot == null) {
                break;
            }

            // Pick up source stack with left click (mode PICKUP, button 0)
            mc.playerController.windowClick(container.windowId, sourceSlot.slotNumber, 0, ClickType.PICKUP, player);

            ItemStack cursor = player.inventory.getItemStack();
            int cursorCount = cursor != null && !cursor.isEmpty() ? cursor.getCount() : 0;
            if (cursorCount <= 0) break;
            ru.nexsqaud.rgv.api.RgvStack cursorRgv = Forge1122Platform.toRgvStack(cursor);

            // Distribute to all target slots that need this ingredient
            for (int i = 0; i < craftSlots.size() && cursorCount > 0; i++) {
                if (needed[i] > 0) {
                    int tInputIdx = slotMapping[i];
                    if (tInputIdx < inputs.size() && inputs.get(tInputIdx).matches(cursorRgv)) {
                        int toPlace = Math.min(needed[i], cursorCount);
                        for (int p = 0; p < toPlace; p++) {
                            // Right click drops 1 item into crafting slot (mode PICKUP, button 1)
                            mc.playerController.windowClick(container.windowId, craftSlots.get(i).slotNumber, 1, ClickType.PICKUP, player);
                        }
                        needed[i] -= toPlace;
                        cursorCount -= toPlace;
                    }
                }
            }

            // Put remaining items back into sourceSlot (mode PICKUP, button 0)
            if (!player.inventory.getItemStack().isEmpty()) {
                mc.playerController.windowClick(container.windowId, sourceSlot.slotNumber, 0, ClickType.PICKUP, player);
            }
        }

        // Final safety: ensure cursor is never holding an item
        if (!player.inventory.getItemStack().isEmpty()) {
            for (Slot pSlot : playerSlots) {
                if (!pSlot.getHasStack()) {
                    mc.playerController.windowClick(container.windowId, pSlot.slotNumber, 0, ClickType.PICKUP, player);
                    break;
                }
            }
        }

        return true;
    }

    private static boolean canFitIn2x2(RgvRecipe recipe) {
        if (recipe == null) return false;
        List<RgvIngredient> inputs = recipe.getInputs();
        if (inputs.size() <= 4) return true;
        int[] outOfBounds = {2, 5, 6, 7, 8};
        for (int idx : outOfBounds) {
            if (idx < inputs.size()) {
                RgvIngredient ing = inputs.get(idx);
                if (ing != null && !ing.isEmpty()) return false;
            }
        }
        return true;
    }

    private static int calculateMaxCraft(RgvRecipe recipe, List<Slot> playerSlots, int[] slotMapping) {
        List<RgvIngredient> inputs = recipe.getInputs();
        Map<RgvIngredient, Integer> counts = new LinkedHashMap<>();
        for (int idx : slotMapping) {
            if (idx < inputs.size()) {
                RgvIngredient ing = inputs.get(idx);
                if (ing != null && !ing.isEmpty()) {
                    counts.put(ing, counts.getOrDefault(ing, 0) + 1);
                }
            }
        }

        int maxCraft = 64;
        for (Map.Entry<RgvIngredient, Integer> entry : counts.entrySet()) {
            RgvIngredient ing = entry.getKey();
            int neededPerCraft = entry.getValue();
            int available = 0;
            for (Slot pSlot : playerSlots) {
                if (pSlot.getHasStack() && ing.matches(Forge1122Platform.toRgvStack(pSlot.getStack()))) {
                    available += pSlot.getStack().getCount();
                }
            }
            int possible = available / neededPerCraft;
            maxCraft = Math.min(maxCraft, possible);
        }
        return Math.max(1, maxCraft);
    }

    private static boolean hasRemainingNeeded(int[] needed) {
        for (int n : needed) {
            if (n > 0) return true;
        }
        return false;
    }

    private static int getFirstNeededIndex(int[] needed) {
        for (int i = 0; i < needed.length; i++) {
            if (needed[i] > 0) return i;
        }
        return -1;
    }
}
