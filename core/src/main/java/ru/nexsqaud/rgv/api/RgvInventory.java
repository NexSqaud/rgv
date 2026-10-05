package ru.nexsqaud.rgv.api;

import java.util.List;

/**
 * Abstract inventory interface used for craftability calculation,
 * recipe tree resolution, and recipe transfer.
 */
public interface RgvInventory {

    long getAmount(RgvIngredient ingredient);

    boolean hasIngredient(RgvIngredient ingredient);

    List<RgvStack> getAllStacks();
}
