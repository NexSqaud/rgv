package ru.nexsqaud.rgv;

import org.junit.jupiter.api.Test;
import ru.nexsqaud.rgv.api.*;
import ru.nexsqaud.rgv.api.widget.RgvWidgetHolder;
import ru.nexsqaud.rgv.core.platform.RgvPlatform;
import ru.nexsqaud.rgv.core.platform.TransferStatus;
import ru.nexsqaud.rgv.core.recipe.RgvRecipeManager;
import ru.nexsqaud.rgv.core.tree.RgvCraftGraph;
import ru.nexsqaud.rgv.core.tree.RgvCraftGraphTab;
import ru.nexsqaud.rgv.core.tree.RgvGraphNode;

import java.io.File;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class RgvTransferStatusTest {

    private static class DummyRecipe implements RgvRecipe {
        private final String id;
        private final List<RgvIngredient> inputs;
        private final List<RgvStack> outputs;
        private final RgvRecipeCategory category;

        public DummyRecipe(String id, RgvRecipeCategory cat, List<RgvIngredient> inputs, RgvStack output) {
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
            return Collections.emptyList();
        }
    }

    private static class MockPlatform implements RgvPlatform {
        boolean hasSlots = false;
        MockInventory inventory = new MockInventory();

        @Override
        public boolean hasSuitableSlotsFor(RgvRecipe recipe) {
            return hasSlots;
        }

        @Override
        public RgvInventory getPlayerInventory() {
            return inventory;
        }

        @Override public List<RgvStack> getAllKnownStacks() { return Collections.emptyList(); }
        @Override public List<RgvIngredient> getOreDictionaryIngredients() { return Collections.emptyList(); }
        @Override public String translateKey(String key, Object... args) { return key; }
        @Override public void sendTransferRecipePacket(String recipeId, boolean maxCraft) {}
        @Override public void sendGiveItemPacket(RgvStack stack, boolean fullStack) {}
        @Override public boolean isCheatModeAllowed() { return false; }
        @Override public int getScreenWidth() { return 800; }
        @Override public int getScreenHeight() { return 600; }
        @Override public int getGuiScale() { return 1; }
        @Override public File getConfigDirectory() { return new File("build/tmp/testConfig"); }
        @Override public boolean isRecipeViewerPresent() { return false; }
        @Override public boolean isRecipePanelVisible() { return false; }
        @Override public boolean isMouseButtonDown(int button) { return false; }
    }

    @Test
    public void testTransferStatusEvaluation() {
        MockPlatform platform = new MockPlatform();
        RgvRecipeCategory cat = new RgvRecipeCategory("crafting", "Crafting", RgvStack.empty());
        RgvStack wood = RgvStack.of("minecraft:log", 0, 1, "Wood");
        RgvStack plank = RgvStack.of("minecraft:planks", 0, 4, "Planks");
        DummyRecipe recipe = new DummyRecipe("plank_craft", cat, Collections.singletonList(wood), plank);

        // 1. When container does not have suitable slots
        platform.hasSlots = false;
        assertEquals(TransferStatus.NO_SUITABLE_CONTAINER, platform.getTransferStatus(recipe));

        // 2. When container has slots, but player lacks ingredients
        platform.hasSlots = true;
        assertEquals(TransferStatus.MISSING_INGREDIENTS, platform.getTransferStatus(recipe));

        // 3. When container has slots and player has ingredients
        platform.inventory.add(wood, 10);
        assertEquals(TransferStatus.AVAILABLE, platform.getTransferStatus(recipe));

        // 4. Null recipe
        assertEquals(TransferStatus.NO_SUITABLE_CONTAINER, platform.getTransferStatus(null));
    }

    @Test
    public void testGraphPendingTarget() {
        RgvCraftGraph graph = new RgvCraftGraph();
        RgvRecipeManager manager = new RgvRecipeManager();

        RgvRecipeCategory cat = new RgvRecipeCategory("crafting", "Crafting", RgvStack.empty());
        RgvStack iron = RgvStack.of("minecraft:iron_ingot", 0, 1, "Iron Ingot");
        RgvStack stick = RgvStack.of("minecraft:stick", 0, 1, "Stick");
        RgvStack pickaxe = RgvStack.of("minecraft:iron_pickaxe", 0, 1, "Iron Pickaxe");

        DummyRecipe pickRecipe = new DummyRecipe("pick", cat, Arrays.asList(iron.copyWithAmount(3), stick.copyWithAmount(2)), pickaxe);
        manager.addRecipe(pickRecipe);

        RgvCraftGraphTab tab = graph.addTabForRecipe(pickRecipe, 1, manager, null);
        assertNotNull(tab);
        assertFalse(graph.hasPendingTarget());

        RgvGraphNode ironNode = null;
        for (RgvGraphNode child : tab.getRootNode().getChildren()) {
            if (child.getStack().matches(iron)) {
                ironNode = child;
                break;
            }
        }
        assertNotNull(ironNode);

        // Set pending target on iron node
        graph.setPendingTarget(tab, ironNode);
        assertTrue(graph.hasPendingTarget());
        assertEquals(ironNode.getStack(), graph.getPendingTargetStack());
        assertEquals(tab, graph.getPendingTargetTab());

        // Assign recipe to pending target
        RgvStack ironOre = RgvStack.of("minecraft:iron_ore", 0, 1, "Iron Ore");
        RgvRecipeCategory smeltCat = new RgvRecipeCategory("smelting", "Smelting", RgvStack.empty());
        DummyRecipe smeltRecipe = new DummyRecipe("smelt_iron", smeltCat, Collections.singletonList(ironOre), iron);
        manager.addRecipe(smeltRecipe);

        boolean applied = graph.applyPendingTarget(smeltRecipe, manager);
        assertTrue(applied);
        assertFalse(graph.hasPendingTarget());

        // Verify iron node has assigned recipe now
        assertEquals(smeltRecipe, tab.getAssignedRecipe(iron));
        RgvGraphNode updatedIronNode = null;
        for (RgvGraphNode child : tab.getRootNode().getChildren()) {
            if (child.getStack().matches(iron)) {
                updatedIronNode = child;
                break;
            }
        }
        assertNotNull(updatedIronNode);
        assertTrue(updatedIronNode.hasAssignedRecipe());
        assertEquals(1, updatedIronNode.getChildren().size());
        assertTrue(updatedIronNode.getChildren().get(0).getStack().matches(ironOre));
    }
}
