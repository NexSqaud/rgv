package ru.nexsqaud.rgv;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.nexsqaud.rgv.api.*;
import ru.nexsqaud.rgv.api.widget.RgvWidgetHolder;
import ru.nexsqaud.rgv.core.config.RgvConfig;
import ru.nexsqaud.rgv.core.platform.RgvPlatform;
import ru.nexsqaud.rgv.core.recipe.RgvRecipeManager;
import ru.nexsqaud.rgv.core.screen.RgvScreenManager;
import ru.nexsqaud.rgv.core.tree.RgvCraftGraph;
import ru.nexsqaud.rgv.core.tree.RgvCraftGraphTab;

import java.io.File;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class RgvNeiCompatTest {

    private MockPlatform platform;
    private RgvScreenManager screenManager;

    @BeforeEach
    void setUp() {
        platform = new MockPlatform();
        RgvPlatform.setInstance(platform);
        screenManager = new RgvScreenManager();
    }

    @AfterEach
    void tearDown() {
        RgvPlatform.setInstance(null);
    }

    @Test
    void testNeiActiveFlag() {
        // By default mock platform has no recipe viewer or panel
        assertFalse(screenManager.isNeiActive(), "Without panel, isNeiActive should be false");
        assertFalse(screenManager.isNeiRecipeViewerPresent(), "Without viewer, isNeiRecipeViewerPresent should be false");

        // When viewer is present but panel is hidden (e.g. creative mode or 'O' key)
        platform.hasViewer = true;
        platform.hasPanel = false;
        assertFalse(screenManager.isNeiActive(), "With viewer present but panel hidden, isNeiActive should be false (RGV sidebar falls back)");
        assertTrue(screenManager.isNeiRecipeViewerPresent(), "With viewer present, isNeiRecipeViewerPresent should be true");

        // When viewer and panel are active
        platform.hasPanel = true;
        assertTrue(screenManager.isNeiActive(), "With panel active, isNeiActive should be true");

        // When config disables NEI replacement
        screenManager.getConfig().setReplaceWithNei(false);
        assertFalse(screenManager.isNeiActive(), "With config replaceWithNei false, isNeiActive should be false");
        assertFalse(screenManager.isNeiRecipeViewerPresent(), "With config replaceWithNei false, isNeiRecipeViewerPresent should be false");
    }

    @Test
    void testOpenRecipesDelegationInNeiMode() {
        platform.hasViewer = true;
        platform.hasPanel = true;
        RgvStack testStack = RgvStack.of("minecraft:iron_ingot", 0, 1, "Iron Ingot");

        screenManager.openRecipesFor(testStack);
        assertTrue(platform.lastRecipeRequested, "NEI mode should delegate recipe lookup to platform external viewer");
        assertFalse(platform.lastUsageRequested, "Recipe lookup should not be usage");
        assertEquals(testStack, platform.lastIngredientRequested);

        screenManager.openUsesFor(testStack);
        assertTrue(platform.lastUsageRequested, "NEI mode should delegate usage lookup to platform external viewer");
        assertEquals(testStack, platform.lastIngredientRequested);
    }

    @Test
    void testAddNeiRecipeToCraftGraph() {
        RgvRecipeCategory category = new RgvRecipeCategory("nei.crafting", "Crafting", RgvStack.empty());
        RgvStack wood = RgvStack.of("minecraft:planks", 0, 4, "Oak Planks");
        RgvStack log = RgvStack.of("minecraft:log", 0, 1, "Oak Log");

        RgvRecipe neiRecipe = new RgvRecipe() {
            @Override public String getId() { return "nei_log_to_planks"; }
            @Override public RgvRecipeCategory getCategory() { return category; }
            @Override public List<RgvIngredient> getInputs() { return Collections.singletonList(log); }
            @Override public List<RgvStack> getOutputs() { return Collections.singletonList(wood); }
            @Override public void addWidgets(RgvWidgetHolder holder) {}
        };

        screenManager.getRecipeManager().addRecipe(neiRecipe);
        screenManager.getCraftGraph().addTabForRecipe(neiRecipe, 8, screenManager.getRecipeManager(), null);

        RgvCraftGraph graph = screenManager.getCraftGraph();
        assertEquals(1, graph.getTabs().size());
        RgvCraftGraphTab tab = graph.getTabs().get(0);
        assertEquals("Oak Planks", tab.getTitle());
        assertEquals(8, tab.getTargetAmount());
        assertNotNull(tab.getRootNode());
        assertEquals(8, tab.getRootNode().getAmount());
    }

    private static class MockPlatform implements RgvPlatform {
        boolean hasViewer = false;
        boolean hasPanel = false;
        boolean lastRecipeRequested = false;
        boolean lastUsageRequested = false;
        RgvIngredient lastIngredientRequested = null;

        @Override public List<RgvStack> getAllKnownStacks() { return Collections.emptyList(); }
        @Override public List<RgvIngredient> getOreDictionaryIngredients() { return Collections.emptyList(); }
        @Override public RgvInventory getPlayerInventory() { return null; }
        @Override public String translateKey(String key, Object... args) { return key; }
        @Override public void sendTransferRecipePacket(String recipeId, boolean maxCraft) {}
        @Override public void sendGiveItemPacket(RgvStack stack, boolean fullStack) {}
        @Override public boolean isCheatModeAllowed() { return false; }
        @Override public int getScreenWidth() { return 800; }
        @Override public int getScreenHeight() { return 600; }
        @Override public int getGuiScale() { return 2; }
        @Override public File getConfigDirectory() { return new File("build/tmp/testConfig"); }

        @Override
        public boolean isRecipeViewerPresent() {
            return hasViewer;
        }

        @Override
        public boolean isRecipePanelVisible() {
            return hasPanel;
        }

        @Override
        public boolean openExternalRecipeViewer(RgvIngredient ingredient, boolean isUsage) {
            this.lastIngredientRequested = ingredient;
            if (isUsage) {
                this.lastUsageRequested = true;
            } else {
                this.lastRecipeRequested = true;
            }
            return true;
        }
    }
}
