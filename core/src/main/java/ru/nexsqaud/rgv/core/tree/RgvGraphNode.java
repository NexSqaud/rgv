package ru.nexsqaud.rgv.core.tree;

import ru.nexsqaud.rgv.api.RgvRecipe;
import ru.nexsqaud.rgv.api.RgvStack;
import ru.nexsqaud.rgv.core.recipe.RgvRecipeManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Node in the interactive Crafting Graph.
 * Level 0 = final target output.
 * Level 1 = direct ingredients for root recipe.
 * Level 2+ = ingredients for assigned sub-recipes.
 */
public class RgvGraphNode {

    private final RgvStack stack;
    private final long amount;
    private final int level;
    private RgvRecipe assignedRecipe;
    private RgvGraphNode parent;
    private final List<RgvGraphNode> children = new ArrayList<>();

    // Layout coordinates in graph space
    public int x;
    public int y;
    public int width = 24;
    public int height = 24;
    public int subtreeWidth = 0;

    public RgvGraphNode(RgvStack stack, long amount, int level, RgvRecipe assignedRecipe) {
        this.stack = stack;
        this.amount = amount;
        this.level = level;
        this.assignedRecipe = assignedRecipe;
    }

    public RgvStack getStack() {
        return stack;
    }

    public long getAmount() {
        return amount;
    }

    public int getLevel() {
        return level;
    }

    public RgvRecipe getAssignedRecipe() {
        return assignedRecipe;
    }

    public void setAssignedRecipe(RgvRecipe assignedRecipe) {
        this.assignedRecipe = assignedRecipe;
    }

    public RgvGraphNode getParent() {
        return parent;
    }

    public void setParent(RgvGraphNode parent) {
        this.parent = parent;
    }

    public List<RgvGraphNode> getChildren() {
        return children;
    }

    public void addChild(RgvGraphNode child) {
        if (child != null) {
            child.setParent(this);
            this.children.add(child);
        }
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }

    public boolean hasAssignedRecipe() {
        return assignedRecipe != null;
    }

    public String getItemKey() {
        if (stack == null || stack.isEmpty()) return "";
        return stack.getId() + ":" + stack.getMeta();
    }

    public boolean canCraft(RgvRecipeManager manager) {
        if (manager == null || stack == null || stack.isEmpty()) return false;
        return !manager.getRecipesFor(stack).isEmpty();
    }

    public boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public long getSurplusAmount() {
        if (assignedRecipe == null || stack == null || stack.isEmpty() || amount <= 0) return 0;
        long outPerCraft = 1;
        for (RgvStack out : assignedRecipe.getOutputs()) {
            if (stack.matches(out)) {
                outPerCraft = Math.max(1, out.getAmount());
                break;
            }
        }
        long craftsNeeded = (long) Math.ceil((double) amount / (double) outPerCraft);
        return (craftsNeeded * outPerCraft) - amount;
    }
}
