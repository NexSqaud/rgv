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
    private String savedRootRecipeId;
    private int targetAmount = 1;

    // Assigned recipe per ingredient key (id:meta -> chosen recipe)
    private final Map<String, RgvRecipe> assignedRecipes = new LinkedHashMap<>();
    private final Map<String, String> savedAssignedRecipeIds = new LinkedHashMap<>();

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
        this.savedRootRecipeId = rootRecipe != null ? rootRecipe.getId() : null;
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

    public void setTargetStack(RgvStack targetStack) {
        this.targetStack = targetStack;
        if (targetStack != null && !targetStack.isEmpty()) {
            this.title = targetStack.getDisplayName();
        }
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
        this.savedRootRecipeId = recipe != null ? recipe.getId() : null;
        this.targetStack = stack != null ? stack : (!recipe.getOutputs().isEmpty() ? recipe.getOutputs().get(0) : RgvStack.empty());
        this.title = this.targetStack.getDisplayName();
        this.isSelectingItem = false;
        this.isSelectingRecipe = false;
        this.candidateRecipes.clear();
        this.assignedRecipes.clear();
        this.savedAssignedRecipeIds.clear();
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
        savedAssignedRecipeIds.put(key, recipe.getId());
        rebuildGraph(manager);
    }

    public void removeRecipeForIngredient(RgvStack stack, RgvRecipeManager manager) {
        if (stack == null || stack.isEmpty()) return;
        String key = getIngredientKey(stack);
        assignedRecipes.remove(key);
        savedAssignedRecipeIds.remove(key);
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

    public void setSelectingRecipe(boolean selectingRecipe) {
        this.isSelectingRecipe = selectingRecipe;
    }

    public Map<String, RgvRecipe> getAssignedRecipes() {
        return Collections.unmodifiableMap(assignedRecipes);
    }

    public String getSavedRootRecipeId() {
        return savedRootRecipeId != null ? savedRootRecipeId : (rootRecipe != null ? rootRecipe.getId() : null);
    }

    public void setSavedRootRecipeId(String id) {
        this.savedRootRecipeId = id;
    }

    public Map<String, String> getSavedAssignedRecipeIds() {
        return Collections.unmodifiableMap(savedAssignedRecipeIds);
    }

    public void setSavedAssignedRecipeIds(Map<String, String> map) {
        if (map != null) {
            this.savedAssignedRecipeIds.clear();
            this.savedAssignedRecipeIds.putAll(map);
        }
    }

    public void recalculate(RgvRecipeManager manager, RgvInventory inventory) {
        rebuildGraph(manager);
    }

    public void rebuildGraph(RgvRecipeManager manager) {
        if (manager != null) {
            if (rootRecipe == null && savedRootRecipeId != null) {
                this.rootRecipe = manager.getRecipeById(savedRootRecipeId);
            }
            if (rootRecipe == null && targetStack != null && !targetStack.isEmpty()) {
                List<RgvRecipe> candidates = manager.getRecipesFor(targetStack);
                if (!candidates.isEmpty()) {
                    this.rootRecipe = candidates.get(0);
                    this.savedRootRecipeId = this.rootRecipe.getId();
                }
            }
            for (Map.Entry<String, String> entry : savedAssignedRecipeIds.entrySet()) {
                if (!assignedRecipes.containsKey(entry.getKey())) {
                    RgvRecipe r = manager.getRecipeById(entry.getValue());
                    if (r != null) {
                        assignedRecipes.put(entry.getKey(), r);
                    }
                }
            }
        }

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

    public Map<RgvStack, Long> getLeftovers() {
        Map<RgvStack, Long> leftovers = new LinkedHashMap<>();
        if (rootNode == null || rootRecipe == null || targetStack == null || targetStack.isEmpty()) {
            return leftovers;
        }

        Map<String, Long> produced = new LinkedHashMap<>();
        Map<String, Long> consumed = new LinkedHashMap<>();
        Map<String, RgvStack> stackSample = new LinkedHashMap<>();

        String rootKey = getIngredientKey(targetStack);
        consumed.put(rootKey, (long) targetAmount);
        stackSample.put(rootKey, targetStack.copyWithAmount(1));

        collectProductionAndConsumption(rootNode, produced, consumed, stackSample);

        for (Map.Entry<String, Long> entry : produced.entrySet()) {
            String key = entry.getKey();
            long prod = entry.getValue();
            long cons = consumed.getOrDefault(key, 0L);
            if (prod > cons) {
                RgvStack sample = stackSample.get(key);
                if (sample != null && !sample.isEmpty()) {
                    leftovers.put(sample.copyWithAmount(1), prod - cons);
                }
            }
        }

        return leftovers;
    }

    private void collectProductionAndConsumption(RgvGraphNode node, Map<String, Long> produced, Map<String, Long> consumed, Map<String, RgvStack> stackSample) {
        if (node == null) return;

        boolean isRoot = (node == rootNode);
        RgvRecipe recipe = isRoot ? rootRecipe : node.getAssignedRecipe();

        if (recipe != null) {
            RgvStack primaryOutput = isRoot ? targetStack : node.getStack();
            long outPerCraft = 1;
            for (RgvStack out : recipe.getOutputs()) {
                if (primaryOutput.matches(out)) {
                    outPerCraft = Math.max(1, out.getAmount());
                    break;
                }
            }

            long needed = isRoot ? targetAmount : node.getAmount();
            long craftsNeeded = (long) Math.ceil((double) needed / (double) outPerCraft);

            for (RgvStack out : recipe.getOutputs()) {
                if (out == null || out.isEmpty()) continue;
                String outKey = getIngredientKey(out);
                long amt = out.getAmount() * craftsNeeded;
                produced.put(outKey, produced.getOrDefault(outKey, 0L) + amt);
                stackSample.putIfAbsent(outKey, out.copyWithAmount(1));
            }

            ru.nexsqaud.rgv.core.platform.RgvPlatform platform = ru.nexsqaud.rgv.core.platform.RgvPlatform.get();
            if (platform != null) {
                for (RgvIngredient ing : recipe.getInputs()) {
                    if (ing == null || ing.isEmpty()) continue;
                    List<RgvStack> stacks = ing.getRgvStacks();
                    if (stacks.isEmpty()) continue;
                    RgvStack primary = stacks.get(0);
                    RgvStack rem = platform.getRemainderItem(primary);
                    if (rem != null && !rem.isEmpty()) {
                        String remKey = getIngredientKey(rem);
                        long remAmt = Math.max(1, ing.getAmount()) * craftsNeeded;
                        produced.put(remKey, produced.getOrDefault(remKey, 0L) + remAmt);
                        stackSample.putIfAbsent(remKey, rem.copyWithAmount(1));
                    }
                }
            }
        }

        if (!isRoot) {
            String nodeKey = getIngredientKey(node.getStack());
            consumed.put(nodeKey, consumed.getOrDefault(nodeKey, 0L) + node.getAmount());
            stackSample.putIfAbsent(nodeKey, node.getStack().copyWithAmount(1));
        }

        for (RgvGraphNode child : node.getChildren()) {
            collectProductionAndConsumption(child, produced, consumed, stackSample);
        }
    }
}
