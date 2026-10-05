package ru.nexsqaud.rgv.core.tree;

import ru.nexsqaud.rgv.api.RgvIngredient;
import ru.nexsqaud.rgv.api.RgvInventory;
import ru.nexsqaud.rgv.api.RgvRecipe;
import ru.nexsqaud.rgv.api.RgvStack;
import ru.nexsqaud.rgv.core.recipe.RgvRecipeManager;

import java.util.*;

/**
 * Signature EMI-style Recipe Tree & Cost Solver.
 * Recursively computes the full dependency graph for any item,
 * calculates total raw costs, and factors in current player inventory.
 */
public class RgvRecipeTree {

    public static class Node {
        public final RgvStack stack;
        public final RgvRecipe recipe;
        public final long requiredAmount;
        public final long availableAmount;
        public final long missingAmount;
        public final int depth;
        public final boolean isRawMaterial;
        public final List<Node> children = new ArrayList<>();

        // Layout coordinates for rendering the tree graph
        public int x;
        public int y;
        public int width = 18;
        public int height = 18;

        public Node(RgvStack stack, RgvRecipe recipe, long requiredAmount, long availableAmount, int depth, boolean isRawMaterial) {
            this.stack = stack;
            this.recipe = recipe;
            this.requiredAmount = requiredAmount;
            this.availableAmount = availableAmount;
            this.missingAmount = Math.max(0, requiredAmount - availableAmount);
            this.depth = depth;
            this.isRawMaterial = isRawMaterial;
        }

        public boolean isFullyCraftable() {
            return missingAmount == 0;
        }
    }

    private final RgvRecipeManager recipeManager;
    private final RgvInventory inventory;
    private final RgvStack rootStack;
    private Node rootNode;
    private final Map<RgvStack, Long> totalRawMaterials = new HashMap<>();

    public RgvRecipeTree(RgvRecipeManager recipeManager, RgvInventory inventory, RgvStack rootStack, long amount) {
        this.recipeManager = recipeManager;
        this.inventory = inventory;
        this.rootStack = rootStack.copyWithAmount(amount);
        solve();
    }

    public Node getRootNode() {
        return rootNode;
    }

    public Map<RgvStack, Long> getTotalRawMaterials() {
        return Collections.unmodifiableMap(totalRawMaterials);
    }

    /**
     * Sums up all leaf nodes (base raw materials with no further sub-recipes).
     */
    public Map<RgvStack, Long> getLeafNodeSummary() {
        Map<RgvStack, Long> leaves = new LinkedHashMap<>();
        collectLeafNodes(rootNode, leaves);
        return leaves;
    }

    private void collectLeafNodes(Node node, Map<RgvStack, Long> map) {
        if (node == null) return;
        if (node.children.isEmpty()) {
            RgvStack base = node.stack.copyWithAmount(1);
            long cur = map.getOrDefault(base, 0L);
            map.put(base, cur + node.requiredAmount);
        } else {
            for (Node child : node.children) {
                collectLeafNodes(child, map);
            }
        }
    }

    private void solve() {
        Set<String> visited = new HashSet<>();
        rootNode = resolveNode(rootStack, rootStack.getAmount(), 0, visited);
        layoutTree(rootNode, 10, 10);
    }

    private Node resolveNode(RgvStack target, long amountNeeded, int depth, Set<String> visited) {
        int meta = target.getMeta() == 32767 ? 0 : target.getMeta();
        String key = target.getId() + ":" + meta;
        long availableInInventory = (inventory != null) ? inventory.getAmount(target) : 0;

        List<RgvRecipe> recipes = recipeManager.getRecipesFor(target);
        if (recipes.isEmpty() || visited.contains(key) || depth > 8) {
            addRawMaterial(target, Math.max(0, amountNeeded - availableInInventory));
            return new Node(target, null, amountNeeded, availableInInventory, depth, true);
        }

        RgvRecipe chosenRecipe = recipes.get(0);
        long producedPerCraft = 1;
        for (RgvStack out : chosenRecipe.getOutputs()) {
            if (target.matches(out)) {
                producedPerCraft = Math.max(1, out.getAmount());
                break;
            }
        }

        long missing = Math.max(0, amountNeeded - availableInInventory);
        long craftsNeeded = (long) Math.ceil((double) missing / (double) producedPerCraft);

        Node node = new Node(target, chosenRecipe, amountNeeded, availableInInventory, depth, false);

        if (craftsNeeded > 0) {
            visited.add(key);

            for (RgvIngredient input : chosenRecipe.getInputs()) {
                if (input == null || input.isEmpty()) continue;
                List<RgvStack> stacks = input.getRgvStacks();
                if (stacks.isEmpty()) continue;
                RgvStack primaryInput = stacks.get(0);

                long subRequired;
                try {
                    subRequired = Math.multiplyExact(Math.max(1, input.getAmount()), craftsNeeded);
                } catch (ArithmeticException e) {
                    subRequired = Long.MAX_VALUE / 2;
                }

                Node child = resolveNode(primaryInput, subRequired, depth + 1, new HashSet<>(visited));
                node.children.add(child);
            }

            visited.remove(key);
        }

        return node;
    }

    private void addRawMaterial(RgvStack stack, long amount) {
        if (amount <= 0) return;
        RgvStack base = stack.copyWithAmount(1);
        long current = totalRawMaterials.getOrDefault(base, 0L);
        long next = (current > Long.MAX_VALUE - amount) ? Long.MAX_VALUE / 2 : (current + amount);
        totalRawMaterials.put(base, next);
    }

    /**
     * Compute 2D node coordinates for rendering the tree hierarchy in the GUI.
     */
    private int layoutTree(Node node, int startX, int startY) {
        if (node == null) return startY;
        node.x = startX;
        node.y = startY;

        int childY = startY;
        for (Node child : node.children) {
            childY = layoutTree(child, startX + 36, childY);
            childY += 4;
        }

        return Math.max(startY + 24, childY);
    }
}
