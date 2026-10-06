package ru.nexsqaud.rgv;

import org.junit.jupiter.api.Test;
import ru.nexsqaud.rgv.api.*;
import ru.nexsqaud.rgv.api.widget.RgvWidgetHolder;
import ru.nexsqaud.rgv.core.recipe.RgvRecipeManager;
import ru.nexsqaud.rgv.core.tree.RgvCraftGraph;
import ru.nexsqaud.rgv.core.tree.RgvCraftGraphTab;
import ru.nexsqaud.rgv.core.tree.RgvGraphNode;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class RgvCraftGraphTest {

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
    public void testTopToBottomLayoutAndPlayerRecipeExpansion() {
        RgvRecipeCategory cat = new RgvRecipeCategory("crafting", "Crafting", RgvStack.empty());
        RgvRecipeManager manager = new RgvRecipeManager();

        RgvStack log = RgvStack.of("minecraft:log", 0, 1, "Oak Log");
        RgvStack plank = RgvStack.of("minecraft:planks", 0, 4, "Oak Planks");
        RgvStack stick = RgvStack.of("minecraft:stick", 0, 4, "Stick");
        RgvStack iron = RgvStack.of("minecraft:iron_ingot", 0, 1, "Iron Ingot");
        RgvStack pickaxe = RgvStack.of("minecraft:iron_pickaxe", 0, 1, "Iron Pickaxe");

        // 1 Log -> 4 Planks
        SimpleRecipe plankRecipe = new SimpleRecipe("planks", cat, Collections.singletonList(log), plank);
        manager.addRecipe(plankRecipe);

        // 2 Planks -> 4 Sticks
        SimpleRecipe stickRecipe = new SimpleRecipe("sticks", cat, Collections.singletonList(plank.copyWithAmount(2)), stick);
        manager.addRecipe(stickRecipe);

        // 3 Iron + 2 Sticks -> 1 Pickaxe
        SimpleRecipe pickaxeRecipe = new SimpleRecipe("pickaxe", cat,
                Arrays.asList(iron.copyWithAmount(3), stick.copyWithAmount(2)), pickaxe);
        manager.addRecipe(pickaxeRecipe);

        RgvCraftGraph graph = new RgvCraftGraph();
        MockInventory inventory = new MockInventory();

        // 1. Add tab for pickaxe
        RgvCraftGraphTab tab = graph.addTabForRecipe(pickaxeRecipe, 1, manager, inventory);
        assertNotNull(tab);
        assertEquals("Iron Pickaxe", tab.getTitle());

        // Initial state: Level 0 (Pickaxe) -> Level 1 (Iron x3, Stick x2)
        RgvGraphNode root = tab.getRootNode();
        assertNotNull(root);
        assertEquals(0, root.getLevel());
        assertEquals(2, root.getChildren().size());

        // Level 0 y is 0, children y is 65 (Top-to-Bottom!)
        assertEquals(0, root.y);
        assertEquals(65, root.getChildren().get(0).y);
        assertEquals(65, root.getChildren().get(1).y);

        // Initially no recipes assigned to ingredients -> both are leaf nodes!
        Map<RgvStack, Long> initialLeaves = tab.getLeafNodeRequirements();
        assertEquals(3L, initialLeaves.get(iron.copyWithAmount(1)).longValue());
        assertEquals(2L, initialLeaves.get(stick.copyWithAmount(1)).longValue());

        // 2. Player selects recipe for Stick (2 Planks -> 4 Sticks)
        tab.setRecipeForIngredient(stick, stickRecipe, manager);

        // Now Stick has assigned recipe, expands to Level 2 (Planks)
        RgvGraphNode stickNode = null;
        for (RgvGraphNode child : tab.getRootNode().getChildren()) {
            if (child.getStack().matches(stick)) {
                stickNode = child;
                break;
            }
        }
        assertNotNull(stickNode);
        assertTrue(stickNode.hasAssignedRecipe());
        assertEquals(1, stickNode.getChildren().size());

        RgvGraphNode plankNode = stickNode.getChildren().get(0);
        assertEquals(2, plankNode.getLevel());
        assertEquals(130, plankNode.y); // Level 2 y = 2 * 65 = 130
        assertEquals(2L, plankNode.getAmount()); // 2 Planks needed for 1 craft of sticks

        // Leaves are now: 3 Iron + 2 Planks
        Map<RgvStack, Long> midLeaves = tab.getLeafNodeRequirements();
        assertEquals(3L, midLeaves.get(iron.copyWithAmount(1)).longValue());
        assertEquals(2L, midLeaves.get(plank.copyWithAmount(1)).longValue());
        assertFalse(midLeaves.containsKey(stick.copyWithAmount(1))); // Stick is no longer leaf!

        // 3. Player selects recipe for Planks (1 Log -> 4 Planks)
        tab.setRecipeForIngredient(plank, plankRecipe, manager);

        // Planks expand to Level 3 (Oak Log)
        RgvGraphNode newStickNode = null;
        for (RgvGraphNode child : tab.getRootNode().getChildren()) {
            if (child.getStack().matches(stick)) {
                newStickNode = child;
                break;
            }
        }
        assertNotNull(newStickNode);
        RgvGraphNode newPlankNode = newStickNode.getChildren().get(0);
        assertEquals(1, newPlankNode.getChildren().size());
        RgvGraphNode logNode = newPlankNode.getChildren().get(0);
        assertEquals(3, logNode.getLevel());
        assertEquals(195, logNode.y); // Level 3 y = 3 * 65 = 195
        assertEquals(1L, logNode.getAmount()); // 1 Log produces 4 planks (enough for 2)

        // Leaves are now: 3 Iron + 1 Log
        Map<RgvStack, Long> finalLeaves = tab.getLeafNodeRequirements();
        assertEquals(3L, finalLeaves.get(iron.copyWithAmount(1)).longValue());
        assertEquals(1L, finalLeaves.get(log.copyWithAmount(1)).longValue());

        // 4. Test quantity multiplier scaling (targetAmount = 2)
        tab.incrementAmount(manager);
        assertEquals(2, tab.getTargetAmount());

        Map<RgvStack, Long> scaledLeaves = tab.getLeafNodeRequirements();
        assertEquals(6L, scaledLeaves.get(iron.copyWithAmount(1)).longValue());
        assertEquals(1L, scaledLeaves.get(log.copyWithAmount(1)).longValue());

        // 5. Test manual tab creation
        RgvCraftGraphTab newTab = graph.createNewEmptyTab();
        assertEquals(2, graph.getTabs().size());
        assertEquals(1, graph.getActiveTabIndex());
        assertTrue(newTab.isSelectingItem());

        newTab.selectItem(pickaxe, manager);
        assertFalse(newTab.isSelectingItem());
        assertEquals("Iron Pickaxe", newTab.getTitle());

        // 6. Test closing tabs
        graph.closeTab(1);
        assertEquals(1, graph.getTabs().size());
        assertEquals(0, graph.getActiveTabIndex());
    }

    @Test
    public void testMultiSlotCraftingAndAmountRequirements() {
        RgvRecipeCategory cat = new RgvRecipeCategory("crafting", "Crafting", RgvStack.empty());
        RgvRecipeManager manager = new RgvRecipeManager();

        RgvStack wheat = RgvStack.of("minecraft:wheat", 0, 1, "Wheat");
        RgvStack hayBale = RgvStack.of("minecraft:hay_block", 0, 1, "Hay Bale");

        // 9 Wheat (1 in each of 9 slots) -> 1 Hay Bale
        List<RgvIngredient> nineWheatInputs = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            nineWheatInputs.add(wheat.copyWithAmount(1));
        }
        SimpleRecipe hayRecipe = new SimpleRecipe("hay_bale", cat, nineWheatInputs, hayBale);
        manager.addRecipe(hayRecipe);

        RgvCraftGraph graph = new RgvCraftGraph();
        MockInventory inventory = new MockInventory();

        // Check canCraft: with 8 wheat, cannot craft 9-slot recipe
        inventory.add(wheat, 8);
        assertFalse(hayRecipe.canCraft(inventory), "Should not be able to craft Hay Bale with only 8 wheat");

        // With 9 wheat, can craft
        inventory.add(wheat, 9);
        assertTrue(hayRecipe.canCraft(inventory), "Should be able to craft Hay Bale with 9 wheat");

        // Add to graph planner
        RgvCraftGraphTab tab = graph.addTabForRecipe(hayRecipe, 1, manager, inventory);
        assertNotNull(tab);

        // Verify root node amount is 1
        assertEquals(1, tab.getRootNode().getAmount());

        // Verify child node for wheat has amount 9 (NOT 81!)
        assertEquals(1, tab.getRootNode().getChildren().size());
        RgvGraphNode wheatChild = tab.getRootNode().getChildren().get(0);
        assertEquals(9, wheatChild.getAmount(), "9 slots of 1 wheat must equal 9 wheat required, not 81");

        // Verify leaf requirements sum is 9
        Map<RgvStack, Long> leaves = tab.getLeafNodeRequirements();
        assertEquals(1, leaves.size());
        assertEquals(9L, leaves.get(wheat.copyWithAmount(1)).longValue());

        // Scale target to 3 Hay Bales -> requires 27 wheat
        tab.setTargetAmount(3, manager);
        assertEquals(3, tab.getTargetAmount());
        assertEquals(27, tab.getRootNode().getChildren().get(0).getAmount());
        assertEquals(27L, tab.getLeafNodeRequirements().get(wheat.copyWithAmount(1)).longValue());
    }

    @Test
    public void testCraftGraphLeftoversSingleRecipeBatchOutput() {
        RgvRecipeCategory cat = new RgvRecipeCategory("crafting", "Crafting", RgvStack.empty());
        RgvRecipeManager manager = new RgvRecipeManager();

        RgvStack coal = RgvStack.of("minecraft:coal", 0, 1, "Coal");
        RgvStack stick = RgvStack.of("minecraft:stick", 0, 1, "Stick");
        RgvStack torch = RgvStack.of("minecraft:torch", 0, 4, "Torch");

        SimpleRecipe torchRecipe = new SimpleRecipe("torch", cat, Arrays.asList(coal, stick), torch);
        manager.addRecipe(torchRecipe);

        RgvCraftGraph graph = new RgvCraftGraph();
        RgvCraftGraphTab tab = graph.addTabForRecipe(torchRecipe, 1, manager, null);
        assertNotNull(tab);

        // When target is 1 torch, recipe produces 4 torches -> 3 torches leftover
        Map<RgvStack, Long> leftovers = tab.getLeftovers();
        assertEquals(1, leftovers.size());
        assertEquals(3L, leftovers.get(torch.copyWithAmount(1)).longValue());

        // When target is 4 torches -> 0 torches leftover
        tab.setTargetAmount(4, manager);
        leftovers = tab.getLeftovers();
        assertTrue(leftovers.isEmpty());

        // When target is 5 torches -> 2 crafts produce 8 torches -> 3 torches leftover
        tab.setTargetAmount(5, manager);
        leftovers = tab.getLeftovers();
        assertEquals(1, leftovers.size());
        assertEquals(3L, leftovers.get(torch.copyWithAmount(1)).longValue());
    }

    @Test
    public void testCraftGraphLeftoversMultiLevelCrafting() {
        RgvRecipeCategory cat = new RgvRecipeCategory("crafting", "Crafting", RgvStack.empty());
        RgvRecipeManager manager = new RgvRecipeManager();

        RgvStack log = RgvStack.of("minecraft:log", 0, 1, "Oak Log");
        RgvStack plank = RgvStack.of("minecraft:planks", 0, 4, "Oak Planks");
        RgvStack stick = RgvStack.of("minecraft:stick", 0, 4, "Stick");
        RgvStack pickaxe = RgvStack.of("minecraft:iron_pickaxe", 0, 1, "Wooden Pickaxe");

        SimpleRecipe plankRecipe = new SimpleRecipe("planks", cat, Collections.singletonList(log), plank);
        SimpleRecipe stickRecipe = new SimpleRecipe("sticks", cat, Collections.singletonList(plank.copyWithAmount(2)), stick);
        SimpleRecipe pickaxeRecipe = new SimpleRecipe("pickaxe", cat,
                Arrays.asList(plank.copyWithAmount(3), stick.copyWithAmount(2)), pickaxe);

        manager.addRecipe(plankRecipe);
        manager.addRecipe(stickRecipe);
        manager.addRecipe(pickaxeRecipe);

        RgvCraftGraph graph = new RgvCraftGraph();
        RgvCraftGraphTab tab = graph.addTabForRecipe(pickaxeRecipe, 1, manager, null);

        // Assign sub-recipe for sticks
        tab.setRecipeForIngredient(stick, stickRecipe, manager);
        // Assign sub-recipe for planks
        tab.setRecipeForIngredient(plank, plankRecipe, manager);

        Map<RgvStack, Long> leftovers = tab.getLeftovers();
        assertFalse(leftovers.isEmpty());

        // Sticks: 1 craft produces 4, 2 consumed for pickaxe -> 2 leftover
        assertEquals(2L, leftovers.get(stick.copyWithAmount(1)).longValue());

        // Planks:
        // Pickaxe needs 3 planks -> 1 craft of log produces 4 -> 1 leftover.
        // Stick needs 2 planks -> 1 craft of log produces 4 -> 2 leftover.
        // Total planks produced = 8, consumed = 5 -> 3 leftover.
        assertEquals(3L, leftovers.get(plank.copyWithAmount(1)).longValue());

        // Pickaxe has 0 leftover
        assertFalse(leftovers.containsKey(pickaxe.copyWithAmount(1)));
        // Log is raw material leaf -> 0 leftover
        assertFalse(leftovers.containsKey(log.copyWithAmount(1)));
    }

    @Test
    void testTabPersistenceAcrossSessions(@org.junit.jupiter.api.io.TempDir java.io.File tempDir) {
        RgvRecipeManager manager = new RgvRecipeManager();
        RgvRecipeCategory cat = new RgvRecipeCategory("crafting", "Crafting", RgvStack.empty());
        manager.addCategory(cat);

        RgvStack ironOre = RgvStack.of("minecraft:iron_ore", 0, 1, "Iron Ore");
        RgvStack ironIngot = RgvStack.of("minecraft:iron_ingot", 0, 1, "Iron Ingot");
        RgvStack ironSword = RgvStack.of("minecraft:iron_sword", 0, 1, "Iron Sword");
        RgvStack stick = RgvStack.of("minecraft:stick", 0, 1, "Stick");

        SimpleRecipe smeltRecipe = new SimpleRecipe("smelt_iron", cat, Collections.singletonList(ironOre), ironIngot);
        SimpleRecipe swordRecipe = new SimpleRecipe("sword_recipe", cat, Arrays.asList(ironIngot.copyWithAmount(2), stick), ironSword);

        manager.addRecipe(smeltRecipe);
        manager.addRecipe(swordRecipe);

        RgvCraftGraph originalGraph = new RgvCraftGraph();
        RgvCraftGraphTab tab1 = originalGraph.addTabForRecipe(swordRecipe, 3, manager, null);
        tab1.setRecipeForIngredient(ironIngot, smeltRecipe, manager);

        RgvCraftGraphTab tab2 = originalGraph.createNewEmptyTab();
        tab2.setTitle("Custom Plan 2");

        originalGraph.setActiveTabIndex(0);

        // Save tabs to disk
        originalGraph.save(tempDir);
        java.io.File savedFile = new java.io.File(tempDir, "rgv_tabs.cfg");
        assertTrue(savedFile.exists(), "rgv_tabs.cfg must be created");

        // Load into fresh graph
        RgvCraftGraph loadedGraph = new RgvCraftGraph();
        loadedGraph.load(tempDir, manager);

        assertEquals(2, loadedGraph.getTabs().size(), "Both tabs should be restored");
        assertEquals(0, loadedGraph.getActiveTabIndex());

        RgvCraftGraphTab loadedTab1 = loadedGraph.getTabs().get(0);
        assertEquals("Iron Sword", loadedTab1.getTitle());
        assertEquals(3, loadedTab1.getTargetAmount());
        assertNotNull(loadedTab1.getRootNode(), "Root node must be rebuilt");
        assertEquals(3L, loadedTab1.getRootNode().getAmount());
        assertTrue(loadedTab1.hasRecipeForIngredient(ironIngot), "Sub-recipe for iron ingot must be restored");
        assertEquals(smeltRecipe.getId(), loadedTab1.getAssignedRecipe(ironIngot).getId());

        RgvCraftGraphTab loadedTab2 = loadedGraph.getTabs().get(1);
        assertEquals("Custom Plan 2", loadedTab2.getTitle());
    }

    @Test
    void testPendingTargetApplicationForRootAndSubNode() {
        RgvRecipeManager manager = new RgvRecipeManager();
        RgvRecipeCategory cat = new RgvRecipeCategory("crafting", "Crafting", RgvStack.empty());
        manager.addCategory(cat);

        RgvStack ironOre = RgvStack.of("minecraft:iron_ore", 0, 1, "Iron Ore");
        RgvStack ironIngot = RgvStack.of("minecraft:iron_ingot", 0, 1, "Iron Ingot");
        RgvStack ironSword = RgvStack.of("minecraft:iron_sword", 0, 1, "Iron Sword");
        RgvStack stick = RgvStack.of("minecraft:stick", 0, 1, "Stick");

        SimpleRecipe smeltRecipe = new SimpleRecipe("smelt_iron", cat, Collections.singletonList(ironOre), ironIngot);
        SimpleRecipe swordRecipe = new SimpleRecipe("sword_recipe", cat, Arrays.asList(ironIngot.copyWithAmount(2), stick), ironSword);
        manager.addRecipe(smeltRecipe);
        manager.addRecipe(swordRecipe);

        RgvCraftGraph graph = new RgvCraftGraph();
        RgvCraftGraphTab tab = graph.createNewEmptyTab();
        assertTrue(tab.isSelectingItem());

        // 1. Select item for empty tab and set pending target
        tab.setTargetStack(ironSword);
        tab.setSelectingItem(false);
        tab.setSelectingRecipe(true);
        graph.setPendingTarget(tab, null, ironSword);

        assertTrue(graph.hasPendingTarget());
        assertEquals(ironSword, graph.getPendingTargetStack());

        // 2. Apply recipe from external viewer (NEI/JEI) to pending target
        boolean applied = graph.applyPendingTarget(swordRecipe, manager);
        assertTrue(applied);
        assertFalse(graph.hasPendingTarget(), "Pending target must be cleared after application");
        assertFalse(tab.isSelectingRecipe());
        assertNotNull(tab.getRootNode());
        assertEquals(swordRecipe.getId(), tab.getRootRecipe().getId());

        // 3. Set pending target on a child ingredient node (iron ingot)
        RgvGraphNode ironIngotNode = null;
        for (RgvGraphNode child : tab.getRootNode().getChildren()) {
            if (child.getStack().matches(ironIngot)) {
                ironIngotNode = child;
                break;
            }
        }
        assertNotNull(ironIngotNode, "Iron ingot child node must exist");
        assertEquals(1, ironIngotNode.getLevel());

        graph.setPendingTarget(tab, ironIngotNode, ironIngotNode.getStack());
        assertTrue(graph.hasPendingTarget());

        // 4. Apply smelting recipe to child node
        boolean appliedSub = graph.applyPendingTarget(smeltRecipe, manager);
        assertTrue(appliedSub);
        assertFalse(graph.hasPendingTarget());
        assertTrue(tab.hasRecipeForIngredient(ironIngot));
        assertEquals(smeltRecipe.getId(), tab.getAssignedRecipe(ironIngot).getId());

        // Verify the child node now has iron ore as a child
        RgvGraphNode updatedIngotNode = null;
        for (RgvGraphNode child : tab.getRootNode().getChildren()) {
            if (child.getStack().matches(ironIngot)) {
                updatedIngotNode = child;
                break;
            }
        }
        assertNotNull(updatedIngotNode);
        assertFalse(updatedIngotNode.getChildren().isEmpty(), "Iron ingot should now expand with smelting input");
        assertTrue(updatedIngotNode.getChildren().get(0).getStack().matches(ironOre));
    }
}
