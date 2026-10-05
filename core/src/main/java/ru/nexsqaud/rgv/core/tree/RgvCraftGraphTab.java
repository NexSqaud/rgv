package ru.nexsqaud.rgv.core.tree;

import ru.nexsqaud.rgv.api.RgvIngredient;
import ru.nexsqaud.rgv.api.RgvInventory;
import ru.nexsqaud.rgv.api.RgvRecipe;
import ru.nexsqaud.rgv.api.RgvStack;
import ru.nexsqaud.rgv.core.recipe.RgvRecipeManager;

import java.util.*;

/**
 * Represents an individual tab in the Craft Graph planner.
 * Supports interactive, player-driven recipe expansion top-to-bottom.
 */
public class RgvCraftGraphTab {

    private final String id;
    private String title;
    private RgvStack targetStack;
    private RgvRecipe rootRecipe;
    private int targetAmount = 1;

    // Assigned recipe per ingredient key (id:meta -> chosen recipe)
    private final Map<String, RgvRecipe> assignedRecipes = new LinkedHashMap<>();

    // The root of the layout tree (Level 0)
    private RgvGraphNode rootNode;

    // Graph bounding box in graph coordinates
    private int minX = 0;
    private int maxX = 100;
    private int minY = 0;
    private int maxY = 100;

    // State for manual tab creation item/recipe selection
    private boolean isSelectingItem = false;
    private boolean isSelectingRecipe = false;
    private List<RgvRecipe> candidateRecipes = new ArrayList<>();

    public RgvCraftGraphTab(String id, String title, RgvStack targetStack, RgvRecipe rootRecipe, int amount) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.title = title != null ? title : (targetStack != null ? targetStack.getDisplayName() : "New Plan");
        this.targetStack = targetStack;
        this.rootRecipe = rootRecipe;
        this.targetAmount = Math.max(1, amount);
    }

    public static RgvCraftGraphTab createEmpty(String title) {
        RgvCraftGraphTab tab = new RgvCraftGraphTab(UUID.randomUUID().toString(), title, null, null, 1);
        tab.isSelectingItem = true;
        return tab;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public RgvStack getTargetStack() {
        return targetStack;
    }

    public RgvRecipe getRootRecipe() {
        return rootRecipe;
    }

    public int getTargetAmount() {
        return targetAmount;
    }

    public void setTargetAmount(int amount, RgvRecipeManager manager) {
        this.targetAmount = Math.max(1, amount);
        rebuildGraph(manager);
    }

    public void incrementAmount(RgvRecipeManager manager) {
        setTargetAmount(this.targetAmount + 1, manager);
    }

    public void decrementAmount(RgvRecipeManager manager) {
        if (this.targetAmount > 1) {
            setTargetAmount(this.targetAmount - 1, manager);
        }
    }

    public boolean isSelectingItem() {
        return isSelectingItem;
    }

    public void setSelectingItem(boolean selectingItem) {
        this.isSelectingItem = selectingItem;
    }

    public boolean isSelectingRecipe() {
        return isSelectingRecipe;
    }

    public List<RgvRecipe> getCandidateRecipes() {
        return candidateRecipes;
    }

    public RgvGraphNode getRootNode() {
        return rootNode;
    }

    public int getMinX() {
        return minX;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMinY() {
        return minY;
    }

    public int getMaxY() {
        return maxY;
    }

    public void selectItem(RgvStack stack, RgvRecipeManager manager) {
        if (stack == null || stack.isEmpty()) return;
        List<RgvRecipe> recipes = manager.getRecipesFor(stack);
        if (recipes.isEmpty()) return;

        if (recipes.size() == 1) {
            assignRecipe(recipes.get(0), stack, manager);
        } else {
            this.targetStack = stack;
            this.candidateRecipes = recipes;
            this.isSelectingItem = false;
            this.isSelectingRecipe = true;
        }
    }

    public void assignRecipe(RgvRecipe recipe, RgvStack stack, RgvRecipeManager manager) {
        this.rootRecipe = recipe;
        this.targetStack = stack != null ? stack : (!recipe.getOutputs().isEmpty() ? recipe.getOutputs().get(0) : RgvStack.empty());
        this.title = this.targetStack.getDisplayName();
        this.isSelectingItem = false;
        this.isSelectingRecipe = false;
        this.candidateRecipes.clear();
        this.assignedRecipes.clear();
        rebuildGraph(manager);
    }

    private String getIngredientKey(RgvStack stack) {
        if (stack == null) return "";
        int meta = stack.getMeta() == 32767 ? 0 : stack.getMeta();
        return stack.getId() + ":" + meta;
    }

    public void setRecipeForIngredient(RgvStack stack, RgvRecipe recipe, RgvRecipeManager manager) {
        if (stack == null || stack.isEmpty() || recipe == null) return;
        String key = getIngredientKey(stack);
        assignedRecipes.put(key, recipe);
        rebuildGraph(manager);
    }

    public void removeRecipeForIngredient(RgvStack stack, RgvRecipeManager manager) {
        if (stack == null || stack.isEmpty()) return;
        String key = getIngredientKey(stack);
        assignedRecipes.remove(key);
        rebuildGraph(manager);
    }

    public boolean hasRecipeForIngredient(RgvStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        String key = getIngredientKey(stack);
        return assignedRecipes.containsKey(key);
    }

    public RgvRecipe getAssignedRecipe(RgvStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        String key = getIngredientKey(stack);
        return assignedRecipes.get(key);
    }

    public void recalculate(RgvRecipeManager manager, RgvInventory inventory) {
        rebuildGraph(manager);
    }

    public void rebuildGraph(RgvRecipeManager manager) {
        if (targetStack == null || targetStack.isEmpty() || rootRecipe == null || manager == null) {
            this.rootNode = null;
            return;
        }

        long outPerCraft = 1;
        for (RgvStack out : rootRecipe.getOutputs()) {
            if (targetStack.matches(out)) {
                outPerCraft = Math.max(1, out.getAmount());
                break;
            }
        }

        long craftsNeeded = (long) Math.ceil((double) targetAmount / (double) outPerCraft);
        long totalOutput = craftsNeeded * outPerCraft;

        this.rootNode = new RgvGraphNode(targetStack, totalOutput, 0, rootRecipe);

        Set<String> ancestors = new HashSet<>();
        ancestors.add(rootNode.getItemKey());

        buildChildren(rootNode, rootRecipe, craftsNeeded, 1, ancestors, manager);
        layoutGraph();
    }

    private static class GroupedInput {
        final RgvStack stack;
        final String key;
        long countPerCraft = 0;

        GroupedInput(RgvStack stack, String key) {
            this.stack = stack;
            this.key = key;
        }
    }

    private void buildChildren(RgvGraphNode parentNode, RgvRecipe recipe, long craftsNeeded, int level, Set<String> ancestors, RgvRecipeManager manager) {
        if (recipe == null || recipe.getInputs() == null || craftsNeeded <= 0 || level >= 16) return;

        Map<String, GroupedInput> grouped = new LinkedHashMap<>();
        for (RgvIngredient ing : recipe.getInputs()) {
            if (ing == null || ing.isEmpty()) continue;
            List<RgvStack> stacks = ing.getRgvStacks();
            if (stacks.isEmpty()) continue;
            RgvStack primary = stacks.get(0);
            String key = getIngredientKey(primary);

            GroupedInput g = grouped.computeIfAbsent(key, k -> new GroupedInput(primary, key));
            g.countPerCraft += Math.max(1, ing.getAmount());
        }

        for (GroupedInput g : grouped.values()) {
            long requiredCount;
            try {
                requiredCount = Math.multiplyExact(g.countPerCraft, craftsNeeded);
            } catch (ArithmeticException e) {
                requiredCount = Long.MAX_VALUE / 2;
            }
            RgvRecipe assigned = assignedRecipes.get(g.key);

            if (assigned != null && !ancestors.contains(g.key)) {
                // Expanded sub-recipe
                RgvGraphNode child = new RgvGraphNode(g.stack, requiredCount, level, assigned);
                parentNode.addChild(child);

                long subOutPerCraft = 1;
                for (RgvStack out : assigned.getOutputs()) {
                    if (g.stack.matches(out)) {
                        subOutPerCraft = Math.max(1, out.getAmount());
                        break;
                    }
                }

                long subCraftsNeeded = (long) Math.ceil((double) requiredCount / (double) subOutPerCraft);
                Set<String> nextAncestors = new HashSet<>(ancestors);
                nextAncestors.add(g.key);

                buildChildren(child, assigned, subCraftsNeeded, level + 1, nextAncestors, manager);
            } else {
                // Raw leaf requirement
                RgvGraphNode child = new RgvGraphNode(g.stack, requiredCount, level, null);
                parentNode.addChild(child);
            }
        }
    }

    private static final int NODE_WIDTH = 24;
    private static final int NODE_HEIGHT = 24;
    private static final int SIBLING_GAP = 18;
    private static final int LEVEL_HEIGHT = 65;

    private void layoutGraph() {
        if (rootNode == null) {
            minX = 0;
            maxX = 100;
            minY = 0;
            maxY = 100;
            return;
        }

        calculateSubtreeWidth(rootNode);
        assignCoordinates(rootNode, 0);

        // Calculate bounding box
        int[] bounds = new int[]{Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE};
        computeBounds(rootNode, bounds);
        minX = bounds[0];
        maxX = bounds[1];
        minY = bounds[2];
        maxY = bounds[3];
    }

    private int calculateSubtreeWidth(RgvGraphNode node) {
        if (node.getChildren().isEmpty()) {
            node.subtreeWidth = NODE_WIDTH + SIBLING_GAP;
            return node.subtreeWidth;
        }
        int total = 0;
        for (RgvGraphNode child : node.getChildren()) {
            total += calculateSubtreeWidth(child);
        }
        node.subtreeWidth = Math.max(NODE_WIDTH + SIBLING_GAP, total);
        return node.subtreeWidth;
    }

    private void assignCoordinates(RgvGraphNode node, int startX) {
        node.y = node.getLevel() * LEVEL_HEIGHT;
        node.width = NODE_WIDTH;
        node.height = NODE_HEIGHT;

        if (node.getChildren().isEmpty()) {
            node.x = startX + (node.subtreeWidth - NODE_WIDTH) / 2;
        } else {
            int currentX = startX;
            for (RgvGraphNode child : node.getChildren()) {
                assignCoordinates(child, currentX);
                currentX += child.subtreeWidth;
            }
            RgvGraphNode first = node.getChildren().get(0);
            RgvGraphNode last = node.getChildren().get(node.getChildren().size() - 1);
            node.x = (first.x + last.x) / 2;
        }
    }

    private void computeBounds(RgvGraphNode node, int[] bounds) {
        if (node == null) return;
        bounds[0] = Math.min(bounds[0], node.x);
        bounds[1] = Math.max(bounds[1], node.x + node.width);
        bounds[2] = Math.min(bounds[2], node.y);
        bounds[3] = Math.max(bounds[3], node.y + node.height);

        for (RgvGraphNode child : node.getChildren()) {
            computeBounds(child, bounds);
        }
    }

    /**
     * Calculates the sum of all leaf nodes (unexpanded base raw materials).
     */
    public Map<RgvStack, Long> getLeafNodeRequirements() {
        Map<RgvStack, Long> leaves = new LinkedHashMap<>();
        if (rootNode != null) {
            collectLeafNodes(rootNode, leaves);
        }
        return leaves;
    }

    private void collectLeafNodes(RgvGraphNode node, Map<RgvStack, Long> map) {
        if (node == null) return;
        if (node.isLeaf()) {
            RgvStack base = node.getStack().copyWithAmount(1);
            long cur = map.getOrDefault(base, 0L);
            map.put(base, cur + node.getAmount());
        } else {
            for (RgvGraphNode child : node.getChildren()) {
                collectLeafNodes(child, map);
            }
        }
    }
}
