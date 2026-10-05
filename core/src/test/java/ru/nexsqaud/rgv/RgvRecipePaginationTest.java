package ru.nexsqaud.rgv;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.nexsqaud.rgv.api.*;
import ru.nexsqaud.rgv.api.widget.RgvWidgetHolder;
import ru.nexsqaud.rgv.core.recipe.RgvRecipeManager;
import ru.nexsqaud.rgv.core.screen.RgvRecipeScreen;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class RgvRecipePaginationTest {

    private static class DummyRecipe implements RgvRecipe {
        private final String id;
        private final RgvRecipeCategory category;
        private final RgvStack output;

        public DummyRecipe(String id, RgvRecipeCategory cat, RgvStack output) {
            this.id = id;
            this.category = cat;
            this.output = output;
        }

        @Override public String getId() { return id; }
        @Override public RgvRecipeCategory getCategory() { return category; }
        @Override public List<RgvIngredient> getInputs() { return Collections.emptyList(); }
        @Override public List<RgvStack> getOutputs() { return Collections.singletonList(output); }
        @Override public void addWidgets(RgvWidgetHolder holder) {}
    }

    private static class MockDrawContext implements RgvDrawContext {
        @Override public void drawRect(int x, int y, int width, int height, int color) {}
        @Override public void drawGradient(int x, int y, int width, int height, int colorStart, int colorEnd) {}
        @Override public void drawTexture(String texturePath, int x, int y, int u, int v, int width, int height, int textureWidth, int textureHeight) {}
        @Override public void drawText(String text, int x, int y, int color, boolean shadow) {}
        @Override public int getTextWidth(String text) { return text != null ? text.length() * 6 : 0; }
        @Override public int getFontHeight() { return 9; }
        @Override public void drawStack(RgvStack stack, int x, int y) {}
        @Override public void drawStackOverlay(RgvStack stack, int x, int y, String overlayText) {}
        @Override public void drawTooltip(List<String> lines, int x, int y) {}
        @Override public void enableScissor(int x, int y, int width, int height) {}
        @Override public void disableScissor() {}
        @Override public void pushMatrix() {}
        @Override public void popMatrix() {}
        @Override public void translate(float x, float y, float z) {}
        @Override public void scale(float sx, float sy, float sz) {}
        @Override public void setColor(float r, float g, float b, float a) {}
        @Override public void resetColor() {}
    }

    private RgvRecipeManager recipeManager;
    private RgvRecipeScreen screen;
    private MockDrawContext drawContext;
    private RgvRecipeCategory testCategory;

    @BeforeEach
    public void setup() {
        recipeManager = new RgvRecipeManager();
        testCategory = new RgvRecipeCategory("crafting", "Crafting", RgvStack.empty());
        recipeManager.addCategory(testCategory);
        screen = new RgvRecipeScreen(recipeManager);
        drawContext = new MockDrawContext();
    }

    @Test
    public void testMultipleRecipePaginationButtons() {
        List<RgvRecipe> recipes = Arrays.asList(
                new DummyRecipe("recipe1", testCategory, RgvStack.of("item1", 0, 1, "Item 1")),
                new DummyRecipe("recipe2", testCategory, RgvStack.of("item2", 0, 1, "Item 2")),
                new DummyRecipe("recipe3", testCategory, RgvStack.of("item3", 0, 1, "Item 3"))
        );
        for (RgvRecipe r : recipes) {
            recipeManager.addRecipe(r);
        }

        screen.openWithRecipes(recipes, "Multiple Recipes");
        assertTrue(screen.isOpen());
        assertEquals(3, screen.getCurrentRecipes().size());
        assertEquals(0, screen.getCurrentRecipeIndex());

        // Render once to populate layout hitboxes
        screen.render(drawContext, 0, 0, 0f);

        int prevX = screen.getPrevBtnX();
        int prevY = screen.getPrevBtnY();
        int prevW = screen.getPrevBtnW();
        int prevH = screen.getPrevBtnH();

        int nextX = screen.getNextBtnX();
        int nextY = screen.getNextBtnY();
        int nextW = screen.getNextBtnW();
        int nextH = screen.getNextBtnH();

        assertTrue(prevW > 0, "Previous button width must be > 0 when multiple recipes exist");
        assertTrue(nextW > 0, "Next button width must be > 0 when multiple recipes exist");

        // Click next button -> should advance from 0 to 1
        boolean handledNext = screen.mouseClicked(nextX + 2, nextY + 2, 0);
        assertTrue(handledNext);
        assertEquals(1, screen.getCurrentRecipeIndex());

        // Re-render
        screen.render(drawContext, 0, 0, 0f);

        // Click next button again -> should advance from 1 to 2
        screen.mouseClicked(screen.getNextBtnX() + 2, screen.getNextBtnY() + 2, 0);
        assertEquals(2, screen.getCurrentRecipeIndex());

        screen.render(drawContext, 0, 0, 0f);

        // Click next button again -> should wrap around to 0
        screen.mouseClicked(screen.getNextBtnX() + 2, screen.getNextBtnY() + 2, 0);
        assertEquals(0, screen.getCurrentRecipeIndex());

        screen.render(drawContext, 0, 0, 0f);

        // Click previous button from 0 -> should wrap around to 2
        boolean handledPrev = screen.mouseClicked(screen.getPrevBtnX() + 2, screen.getPrevBtnY() + 2, 0);
        assertTrue(handledPrev);
        assertEquals(2, screen.getCurrentRecipeIndex());

        screen.render(drawContext, 0, 0, 0f);

        // Click previous button from 2 -> should go to 1
        screen.mouseClicked(screen.getPrevBtnX() + 2, screen.getPrevBtnY() + 2, 0);
        assertEquals(1, screen.getCurrentRecipeIndex());

        // Click previous button from 1 -> should go to 0
        screen.mouseClicked(screen.getPrevBtnX() + 2, screen.getPrevBtnY() + 2, 0);
        assertEquals(0, screen.getCurrentRecipeIndex());
    }

    @Test
    public void testMouseWheelScrollPagination() {
        List<RgvRecipe> recipes = Arrays.asList(
                new DummyRecipe("recipe1", testCategory, RgvStack.of("item1", 0, 1, "Item 1")),
                new DummyRecipe("recipe2", testCategory, RgvStack.of("item2", 0, 1, "Item 2")),
                new DummyRecipe("recipe3", testCategory, RgvStack.of("item3", 0, 1, "Item 3"))
        );
        for (RgvRecipe r : recipes) {
            recipeManager.addRecipe(r);
        }

        screen.openWithRecipes(recipes, "Multiple Recipes");
        assertEquals(0, screen.getCurrentRecipeIndex());

        // Scroll down (negative delta) -> next recipe
        boolean scrolledDown = screen.mouseScrolled(-1);
        assertTrue(scrolledDown);
        assertEquals(1, screen.getCurrentRecipeIndex());

        // Scroll up (positive delta) -> previous recipe
        boolean scrolledUp = screen.mouseScrolled(1);
        assertTrue(scrolledUp);
        assertEquals(0, screen.getCurrentRecipeIndex());

        // Scroll up from 0 -> wrap to last recipe (2)
        screen.mouseScrolled(1);
        assertEquals(2, screen.getCurrentRecipeIndex());
    }

    @Test
    public void testKeyboardArrowPagination() {
        List<RgvRecipe> recipes = Arrays.asList(
                new DummyRecipe("recipe1", testCategory, RgvStack.of("item1", 0, 1, "Item 1")),
                new DummyRecipe("recipe2", testCategory, RgvStack.of("item2", 0, 1, "Item 2"))
        );
        for (RgvRecipe r : recipes) {
            recipeManager.addRecipe(r);
        }

        screen.openWithRecipes(recipes, "Two Recipes");
        assertEquals(0, screen.getCurrentRecipeIndex());

        // Keycode 205 is Right Arrow
        boolean right = screen.keyPressed(205, '\0');
        assertTrue(right);
        assertEquals(1, screen.getCurrentRecipeIndex());

        // Keycode 203 is Left Arrow
        boolean left = screen.keyPressed(203, '\0');
        assertTrue(left);
        assertEquals(0, screen.getCurrentRecipeIndex());
    }

    @Test
    public void testSingleRecipeHasNoPaginationButtons() {
        List<RgvRecipe> recipes = Collections.singletonList(
                new DummyRecipe("recipe1", testCategory, RgvStack.of("item1", 0, 1, "Item 1"))
        );
        recipeManager.addRecipe(recipes.get(0));

        screen.openWithRecipes(recipes, "Single Recipe");
        screen.render(drawContext, 0, 0, 0f);

        assertEquals(0, screen.getPrevBtnW());
        assertEquals(0, screen.getNextBtnW());

        // Mouse scroll returns false when <= 1 recipe
        assertFalse(screen.mouseScrolled(1));
        assertFalse(screen.mouseScrolled(-1));
    }
}
