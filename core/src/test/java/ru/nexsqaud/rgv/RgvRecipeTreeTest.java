package ru.nexsqaud.rgv;

import org.junit.jupiter.api.Test;
import ru.nexsqaud.rgv.api.*;
import ru.nexsqaud.rgv.api.widget.RgvWidgetHolder;
import ru.nexsqaud.rgv.core.recipe.RgvRecipeManager;
import ru.nexsqaud.rgv.core.tree.RgvRecipeTree;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class RgvRecipeTreeTest {

    private static class SimpleRecipe implements RgvRecipe {
        private final String id;
        private final List<RgvIngredient> inputs;
        private final List<RgvStack> outputs;
        private final RgvRecipeCategory category;

        public SimpleRecipe(String id, RgvRecipeCategory cat, List<RgvIngredient> inputs, RgvStack output) {
            this.id = id;
            this.category = cat;
            this.inputs = inputs;
            this.outputs = Collections.singletonList(output);
        }

        @Override public String getId() { return id; }
        @Override public RgvRecipeCategory getCategory() { return category; }
        @Override public List<RgvIngredient> getInputs() { return inputs; }
        @Override public List<RgvStack> getOutputs() { return outputs; }
        @Override public void addWidgets(RgvWidgetHolder holder) {}
    }

    private static class MockInventory implements RgvInventory {
        private final Map<String, Long> items = new HashMap<>();

        public void add(RgvStack stack, long count) {
            items.put(stack.getId() + ":" + stack.getMeta(), count);
        }

        @Override
        public long getAmount(RgvIngredient ingredient) {
            if (ingredient == null || ingredient.isEmpty()) return 0;
            long total = 0;
            for (RgvStack s : ingredient.getRgvStacks()) {
                total += items.getOrDefault(s.getId() + ":" + s.getMeta(), 0L);
            }
            return total;
        }

        @Override
        public boolean hasIngredient(RgvIngredient ingredient) {
            return getAmount(ingredient) >= (ingredient != null ? ingredient.getAmount() : 1);
        }

        @Override
        public List<RgvStack> getAllStacks() {
            List<RgvStack> res = new ArrayList<>();
            for (Map.Entry<String, Long> e : items.entrySet()) {
                String[] p = e.getKey().split(":");
                res.add(RgvStack.of(p[0] + ":" + p[1], Integer.parseInt(p[2]), e.getValue()));
            }
            return res;
        }
    }

    @Test
    public void testRecipeTreeCalculationWithoutInventory() {
        RgvRecipeCategory cat = new RgvRecipeCategory("crafting", "Crafting", RgvStack.empty());
        RgvRecipeManager manager = new RgvRecipeManager();

        RgvStack log = RgvStack.of("minecraft:log", 0, 1, "Oak Log");
        RgvStack plank = RgvStack.of("minecraft:planks", 0, 4, "Oak Planks");
        RgvStack stick = RgvStack.of("minecraft:stick", 0, 4, "Stick");
        RgvStack iron = RgvStack.of("minecraft:iron_ingot", 0, 1, "Iron Ingot");
        RgvStack pickaxe = RgvStack.of("minecraft:iron_pickaxe", 0, 1, "Iron Pickaxe");

        // 1 Log -> 4 Planks
        manager.addRecipe(new SimpleRecipe("planks", cat, Collections.singletonList(log), plank));
        // 2 Planks -> 4 Sticks
        manager.addRecipe(new SimpleRecipe("sticks", cat, Collections.singletonList(plank.copyWithAmount(2)), stick));
        // 3 Iron + 2 Sticks -> 1 Pickaxe
        manager.addRecipe(new SimpleRecipe("pickaxe", cat,
                Arrays.asList(iron.copyWithAmount(3), stick.copyWithAmount(2)), pickaxe));

        // Solve for 1 Pickaxe with empty inventory
        RgvRecipeTree tree = new RgvRecipeTree(manager, new MockInventory(), pickaxe, 1);
        RgvRecipeTree.Node root = tree.getRootNode();

        assertNotNull(root);
        assertEquals(2, root.children.size()); // Iron and Stick children

        // Check raw materials: 3 Iron and at least 1 Log
        Map<RgvStack, Long> raw = tree.getTotalRawMaterials();
        assertTrue(raw.containsKey(iron.copyWithAmount(1)));
        assertEquals(3L, raw.get(iron.copyWithAmount(1)).longValue());

        assertTrue(raw.containsKey(log.copyWithAmount(1)));
        assertEquals(1L, raw.get(log.copyWithAmount(1)).longValue());
    }

    @Test
    public void testRecipeTreeWithExistingInventory() {
        RgvRecipeCategory cat = new RgvRecipeCategory("crafting", "Crafting", RgvStack.empty());
        RgvRecipeManager manager = new RgvRecipeManager();

        RgvStack log = RgvStack.of("minecraft:log", 0, 1, "Oak Log");
        RgvStack plank = RgvStack.of("minecraft:planks", 0, 4, "Oak Planks");
        RgvStack stick = RgvStack.of("minecraft:stick", 0, 4, "Stick");
        RgvStack iron = RgvStack.of("minecraft:iron_ingot", 0, 1, "Iron Ingot");
        RgvStack pickaxe = RgvStack.of("minecraft:iron_pickaxe", 0, 1, "Iron Pickaxe");

        manager.addRecipe(new SimpleRecipe("planks", cat, Collections.singletonList(log), plank));
        manager.addRecipe(new SimpleRecipe("sticks", cat, Collections.singletonList(plank.copyWithAmount(2)), stick));
        manager.addRecipe(new SimpleRecipe("pickaxe", cat,
                Arrays.asList(iron.copyWithAmount(3), stick.copyWithAmount(2)), pickaxe));

        // Player already has 2 Sticks and 3 Iron in inventory!
        MockInventory inventory = new MockInventory();
        inventory.add(stick, 2);
        inventory.add(iron, 3);

        RgvRecipeTree tree = new RgvRecipeTree(manager, inventory, pickaxe, 1);
        RgvRecipeTree.Node root = tree.getRootNode();

        assertNotNull(root);
        // Target pickaxe is missing 1 (to be crafted), but all ingredients (iron and sticks) are present (missing = 0)!
        assertEquals(1, root.missingAmount);
        for (RgvRecipeTree.Node child : root.children) {
            assertEquals(0, child.missingAmount);
        }

        // Total extra raw materials needed should be 0 because player has everything!
        Map<RgvStack, Long> raw = tree.getTotalRawMaterials();
        assertTrue(raw.isEmpty());
    }

    @Test
    public void testRecipeTreeLeftoversCalculation() {
        RgvRecipeCategory cat = new RgvRecipeCategory("crafting", "Crafting", RgvStack.empty());
        RgvRecipeManager manager = new RgvRecipeManager();

        RgvStack log = RgvStack.of("minecraft:log", 0, 1, "Oak Log");
        RgvStack plank = RgvStack.of("minecraft:planks", 0, 4, "Oak Planks");

        // 1 Log -> 4 Planks
        manager.addRecipe(new SimpleRecipe("planks", cat, Collections.singletonList(log), plank));

        // Solve for 1 Plank with empty inventory -> crafts 1 log into 4 planks -> 3 planks leftover
        RgvRecipeTree tree = new RgvRecipeTree(manager, new MockInventory(), plank, 1);
        Map<RgvStack, Long> leftovers = tree.getTotalLeftovers();

        assertNotNull(leftovers);
        assertEquals(1, leftovers.size());
        assertEquals(3L, leftovers.get(plank.copyWithAmount(1)).longValue());
    }
}
