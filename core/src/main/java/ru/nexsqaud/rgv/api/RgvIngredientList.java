package ru.nexsqaud.rgv.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An ingredient that represents multiple alternative stacks (e.g. tag / ore dictionary match).
 * Cycles through the alternatives when rendered.
 */
public class RgvIngredientList implements RgvIngredient {

    private final List<RgvStack> stacks;
    private final long amount;
    private final String tagName;

    public RgvIngredientList(List<RgvStack> stacks, long amount) {
        this(stacks, amount, "");
    }

    public RgvIngredientList(List<RgvStack> stacks, long amount, String tagName) {
        this.stacks = stacks != null ? new ArrayList<>(stacks) : new ArrayList<>();
        this.amount = amount;
        this.tagName = tagName != null ? tagName : "";
    }

    public static RgvIngredient of(List<RgvStack> stacks, long amount) {
        if (stacks == null || stacks.isEmpty()) {
            return RgvStack.empty();
        }
        if (stacks.size() == 1) {
            return stacks.get(0).copyWithAmount(amount);
        }
        return new RgvIngredientList(stacks, amount);
    }

    public static RgvIngredient ofTag(String tagName, List<RgvStack> stacks, long amount) {
        return new RgvIngredientList(stacks, amount, tagName);
    }

    public String getTagName() {
        return tagName;
    }

    @Override
    public List<RgvStack> getRgvStacks() {
        return Collections.unmodifiableList(stacks);
    }

    @Override
    public long getAmount() {
        return amount;
    }

    @Override
    public boolean isEmpty() {
        return stacks.isEmpty() || amount <= 0;
    }

    public RgvStack getActiveStack() {
        if (stacks.isEmpty()) return RgvStack.empty();
        long step = System.currentTimeMillis() / 1500;
        int index = (int) Math.floorMod(step, (long) stacks.size());
        return stacks.get(index);
    }

    @Override
    public String getDisplayName() {
        if (!tagName.isEmpty()) {
            return "#" + tagName;
        }
        return getActiveStack().getDisplayName();
    }

    @Override
    public List<String> getTooltip() {
        List<String> tooltip = new ArrayList<>(getActiveStack().getTooltip());
        if (!tagName.isEmpty()) {
            tooltip.add("\u00a78Tag: " + tagName);
        }
        if (stacks.size() > 1) {
            tooltip.add("\u00a77Accepts " + stacks.size() + " alternatives");
        }
        return tooltip;
    }

    @Override
    public void render(RgvDrawContext context, int x, int y, float delta) {
        getActiveStack().render(context, x, y, delta);
    }

    @Override
    public boolean matches(RgvStack stack) {
        if (stack == null) return false;
        for (RgvStack s : stacks) {
            if (s.matches(stack)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public RgvIngredient copyWithAmount(long newAmount) {
        List<RgvStack> copied = new ArrayList<>(stacks.size());
        for (RgvStack s : stacks) {
            copied.add(s.copyWithAmount(newAmount));
        }
        return new RgvIngredientList(copied, newAmount, this.tagName);
    }
}
