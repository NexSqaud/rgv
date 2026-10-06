package ru.nexsqaud.rgv.core.screen;

import ru.nexsqaud.rgv.api.*;
import ru.nexsqaud.rgv.core.config.RgvConfig;
import ru.nexsqaud.rgv.core.index.RgvIndex;
import ru.nexsqaud.rgv.core.platform.RgvPlatform;
import ru.nexsqaud.rgv.core.recipe.RgvCraftableSolver;
import ru.nexsqaud.rgv.core.recipe.RgvRecipeManager;
import ru.nexsqaud.rgv.core.widget.SearchBarWidget;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Orchestrates the full EMI-style screen overlay:
 * Right sidebar (item index, search, pagination, craftable filter),
 * Left sidebar (bookmarks / favorites),
 * and the modal recipe viewer dialog.
 */
public class RgvScreenManager implements RgvRecipeScreen.RecipeLookupHandler {

    private final RgvIndex index = new RgvIndex();
    private final RgvRecipeManager recipeManager;
    private final RgvConfig config = new RgvConfig();
    private final RgvRecipeScreen recipeScreen;
    private final SearchBarWidget searchBar;

    private RgvStack hoveredStack = null;
    private boolean isHoveringBookmark = false;

    // Layout coordinates
    private int rightSidebarX;
    private int rightSidebarY;
    private int rightSidebarW;
    private int rightSidebarH;

    private int leftSidebarX;
    private int leftSidebarY;
    private int leftSidebarW;
    private int leftSidebarH;

    private int currentGuiLeft;
    private int currentGuiTop;
    private int currentXSize;
    private int currentYSize;

    public RgvScreenManager() {
        this(new RgvRecipeManager());
    }

    public RgvScreenManager(RgvRecipeManager recipeManager) {
        this.recipeManager = recipeManager != null ? recipeManager : new RgvRecipeManager();
        this.recipeScreen = new RgvRecipeScreen(this.recipeManager);
        this.recipeScreen.setLookupHandler(this);
        this.recipeScreen.setConfig(this.config);
        this.searchBar = new SearchBarWidget(0, 0, 120, 16);
        this.searchBar.setOnTextChanged(text -> index.setSearchQuery(text));
    }

    public boolean isNeiActive() {
        RgvPlatform platform = RgvPlatform.get();
        return config.isReplaceWithNei() && platform != null && platform.isRecipePanelVisible();
    }

    public boolean isNeiRecipeViewerPresent() {
        RgvPlatform platform = RgvPlatform.get();
        return config.isReplaceWithNei() && platform != null && platform.isRecipeViewerPresent();
    }

    public ru.nexsqaud.rgv.core.tree.RgvCraftGraph getCraftGraph() {
        return recipeScreen.getCraftGraph();
    }

    public void openGraphForStack(RgvStack stack) {
        if (stack != null && !stack.isEmpty()) {
            List<RgvRecipe> recipes = recipeManager.getRecipesFor(stack);
            if (!recipes.isEmpty()) {
                RgvInventory inv = RgvPlatform.get() != null ? RgvPlatform.get().getPlayerInventory() : null;
                getCraftGraph().addTabForRecipe(recipes.get(0), 1, recipeManager, inv);
            } else {
                ru.nexsqaud.rgv.core.tree.RgvCraftGraphTab emptyTab = getCraftGraph().createNewEmptyTab();
                emptyTab.selectItem(stack, recipeManager);
            }
        }
        recipeScreen.openGraphView();
    }

    public void openGraph() {
        recipeScreen.openGraphView();
    }

    public RgvIndex getIndex() {
        return index;
    }

    public RgvRecipeManager getRecipeManager() {
        return recipeManager;
    }

    public RgvConfig getConfig() {
        return config;
    }

    public RgvRecipeScreen getRecipeScreen() {
        return recipeScreen;
    }

    public SearchBarWidget getSearchBar() {
        return searchBar;
    }

    public RgvStack getHoveredStack() {
        return hoveredStack;
    }

    public boolean isHoveringBookmark() {
        return isHoveringBookmark;
    }

    public void handleRKey() {
        if (hoveredStack != null && !hoveredStack.isEmpty()) {
            openRecipesFor(hoveredStack);
        }
    }

    public void handleUKey() {
        if (hoveredStack != null && !hoveredStack.isEmpty()) {
            openUsesFor(hoveredStack);
        }
    }

    public void handleAKey() {
        if (hoveredStack != null && !hoveredStack.isEmpty()) {
            config.toggleBookmark(hoveredStack);
            saveConfigAndTabs();
        }
    }

    public void handleGKey() {
        recipeScreen.openGraphView();
    }

    public void init() {
        RgvPlatform platform = RgvPlatform.get();
        if (platform != null) {
            config.load(platform.getConfigDirectory());
            getCraftGraph().load(platform.getConfigDirectory(), recipeManager);
            index.setAllItems(platform.getAllKnownStacks());
            updateCraftableFilter();
        }
    }

    public void saveConfigAndTabs() {
        RgvPlatform platform = RgvPlatform.get();
        if (platform != null) {
            config.save(platform.getConfigDirectory());
            getCraftGraph().save(platform.getConfigDirectory());
        }
    }

    public void updateCraftableFilter() {
        RgvPlatform platform = RgvPlatform.get();
        if (platform != null && config.isCraftableFilter()) {
            Set<RgvStack> craftable = RgvCraftableSolver.findCraftableStacks(recipeManager, platform.getPlayerInventory());
            index.setCraftableFilter(true, craftable);
        } else {
            index.setCraftableFilter(false, null);
        }
    }

    /**
     * Recompute layout boundaries around the active container GUI.
     */
    public void updateBounds(int screenWidth, int screenHeight, int guiLeft, int guiTop, int xSize, int ySize) {
        this.currentGuiLeft = guiLeft;
        this.currentGuiTop = guiTop;
        this.currentXSize = xSize;
        this.currentYSize = ySize;

        // Right Sidebar: Item Index
        int rightAvailable = screenWidth - (guiLeft + xSize);
        if (rightAvailable >= 72) {
            this.rightSidebarX = guiLeft + xSize + 6;
            this.rightSidebarY = 6;
            this.rightSidebarW = screenWidth - rightSidebarX - 6;
            this.rightSidebarH = screenHeight - 12;
        } else {
            // Screen is too tight, dock on the right edge
            this.rightSidebarW = Math.min(180, screenWidth / 3);
            this.rightSidebarX = screenWidth - rightSidebarW - 6;
            this.rightSidebarY = 6;
            this.rightSidebarH = screenHeight - 12;
        }

        // Search bar at the bottom of right sidebar (leaving room for [C] and [G] buttons)
        this.searchBar.setBounds(rightSidebarX + 40, rightSidebarY + rightSidebarH - 18, rightSidebarW - 40, 16);

        // Left Sidebar: Bookmarks
        this.leftSidebarX = 6;
        this.leftSidebarY = 6;
        this.leftSidebarW = Math.max(36, guiLeft - 12);
        this.leftSidebarH = screenHeight - 12;

        // Update item index grid
        int gridH = rightSidebarH - 42; // Leave room for pagination & search
        this.index.updateLayout(rightSidebarW, gridH);
    }

    public int getCurrentGuiLeft() {
        return currentGuiLeft;
    }

    public int getCurrentGuiTop() {
        return currentGuiTop;
    }

    public void render(RgvDrawContext context, int mouseX, int mouseY, float delta) {
        if (!config.isOverlayEnabled()) return;

        hoveredStack = null;
        isHoveringBookmark = false;

        // In NEI mode, NEI handles the items panel on the right.
        // RGV only renders the [G] button and bookmarks.
        if (isNeiActive()) {
            renderNeiModeOverlay(context, mouseX, mouseY, delta);
            if (recipeScreen.isOpen()) {
                recipeScreen.render(context, mouseX, mouseY, delta);
            }
            renderTooltips(context, mouseX, mouseY);
            return;
        }

        renderBookmarks(context, mouseX, mouseY, delta);
        renderItemIndex(context, mouseX, mouseY, delta);

        if (recipeScreen.isOpen()) {
            recipeScreen.render(context, mouseX, mouseY, delta);
        }

        renderTooltips(context, mouseX, mouseY);
    }

    private void renderNeiModeOverlay(RgvDrawContext context, int mouseX, int mouseY, float delta) {
        renderBookmarks(context, mouseX, mouseY, delta);
        // Craft Graph Planner Button [G] is rendered as a native GuiButton (RgvHostPlannerButton)
        // during super.drawScreen() so it never renders over tooltips.
    }

    private void renderBookmarks(RgvDrawContext context, int mouseX, int mouseY, float delta) {
        List<RgvStack> bookmarks = config.getBookmarks();
        int startX = leftSidebarX;
        int startY = leftSidebarY + 16;

        if (bookmarks.isEmpty()) {
            // Subtle onboarding hint when player has no pinned bookmarks yet
            context.drawText("Bookmarks", startX, leftSidebarY + 4, 0x888888, false);
            context.drawText("Press 'A' over", startX, startY + 4, 0x666666, false);
            context.drawText("item to pin", startX, startY + 14, 0x666666, false);
            return;
        }

        int cols = Math.max(1, leftSidebarW / 18);
        context.drawText("Bookmarks", startX, leftSidebarY + 4, 0xDDDDDD, true);

        for (int i = 0; i < bookmarks.size(); i++) {
            int col = i % cols;
            int row = i / cols;
            int slotX = startX + col * 18;
            int slotY = startY + row * 18;
            if (slotY + 18 > leftSidebarY + leftSidebarH) break;

            RgvStack stack = bookmarks.get(i);
            context.drawRect(slotX, slotY, 18, 18, 0xFF373737);
            context.drawRect(slotX + 1, slotY + 1, 16, 16, 0xFF8B8B8B);
            stack.render(context, slotX + 1, slotY + 1, delta);

            if (mouseX >= slotX && mouseX < slotX + 18 && mouseY >= slotY && mouseY < slotY + 18) {
                context.drawRect(slotX + 1, slotY + 1, 16, 16, 0x80FFFFFF);
                hoveredStack = stack;
                isHoveringBookmark = true;
            }
        }
    }

    private void renderItemIndex(RgvDrawContext context, int mouseX, int mouseY, float delta) {
        int pageY = rightSidebarY + 2;
        String pageInfo = (index.getCurrentPage() + 1) + " / " + index.getTotalPages();
        int infoW = context.getTextWidth(pageInfo);
        int centerX = rightSidebarX + (rightSidebarW - infoW) / 2;

        context.drawText(pageInfo, centerX, pageY + 2, 0xFFFFFF, true);

        int prevBtnX = centerX - 16;
        int nextBtnX = centerX + infoW + 8;
        boolean prevHover = mouseX >= prevBtnX && mouseX < prevBtnX + 10 && mouseY >= pageY && mouseY < pageY + 12;
        boolean nextHover = mouseX >= nextBtnX && mouseX < nextBtnX + 10 && mouseY >= pageY && mouseY < pageY + 12;

        context.drawText("<", prevBtnX, pageY + 2, prevHover ? 0x55FF55 : 0xAAAAAA, true);
        context.drawText(">", nextBtnX, pageY + 2, nextHover ? 0x55FF55 : 0xAAAAAA, true);

        List<RgvStack> pageItems = index.getItemsForCurrentPage();
        int cols = index.getColumns();
        int gridStartY = rightSidebarY + 18;

        if (pageItems.isEmpty()) {
            context.drawText("No items found", rightSidebarX + 4, gridStartY + 20, 0xAAAAAA, false);
            context.drawText("Clear search or [C]", rightSidebarX + 4, gridStartY + 32, 0x777777, false);
        } else {
            for (int i = 0; i < pageItems.size(); i++) {
                int col = i % cols;
                int row = i / cols;
                int slotX = rightSidebarX + col * 18;
                int slotY = gridStartY + row * 18;

                RgvStack stack = pageItems.get(i);
                context.drawRect(slotX, slotY, 18, 18, 0xFF2A2A2A);
                context.drawRect(slotX + 1, slotY + 1, 16, 16, 0xFF3F3F3F);
                stack.render(context, slotX + 1, slotY + 1, delta);

                if (mouseX >= slotX && mouseX < slotX + 18 && mouseY >= slotY && mouseY < slotY + 18) {
                    context.drawRect(slotX + 1, slotY + 1, 16, 16, 0x80FFFFFF);
                    hoveredStack = stack;
                    isHoveringBookmark = false;
                }
            }
        }

        // Craftable Filter Toggle Button [C]
        int craftBtnX = rightSidebarX;
        int craftBtnY = searchBar.getY();
        boolean craftHover = mouseX >= craftBtnX && mouseX < craftBtnX + 18 && mouseY >= craftBtnY && mouseY < craftBtnY + 16;
        boolean craftActive = config.isCraftableFilter();

        context.drawRect(craftBtnX, craftBtnY, 18, 16, craftActive ? 0xFF33AA33 : (craftHover ? 0xFF666666 : 0xFF333333));
        context.drawText("C", craftBtnX + 6, craftBtnY + 4, craftActive ? 0xFFFFFF : 0xCCCCCC, true);

        // Craft Graph Planner Button [G]
        int graphBtnX = rightSidebarX + 20;
        boolean graphHover = mouseX >= graphBtnX && mouseX < graphBtnX + 18 && mouseY >= craftBtnY && mouseY < craftBtnY + 16;
        context.drawRect(graphBtnX, craftBtnY, 18, 16, graphHover ? 0xFF55AA55 : 0xFF333333);
        context.drawText("G", graphBtnX + 6, craftBtnY + 4, 0xFFFFFF, true);

        // Search Bar
        searchBar.render(context, mouseX, mouseY, delta);
    }

    private void renderTooltips(RgvDrawContext context, int mouseX, int mouseY) {
        if (recipeScreen.isOpen()) {
            recipeScreen.renderTooltips(context, mouseX, mouseY);
            return;
        }

        if (hoveredStack != null && !hoveredStack.isEmpty()) {
            List<String> tip = new ArrayList<>(hoveredStack.getTooltip());
            if (config.isCheatMode()) {
                tip.add("\u00a7cCheat Mode: Left-Click: Stack, Right-Click: 1");
            } else {
                tip.add("\u00a77Press \u00a7eR\u00a77: Recipes | \u00a7eU\u00a77: Uses | \u00a7eA\u00a77: Bookmark");
            }
            context.drawTooltip(tip, mouseX, mouseY);
            return;
        }

        // Craftable button tooltip
        int craftBtnX = rightSidebarX;
        int craftBtnY = searchBar.getY();
        if (mouseX >= craftBtnX && mouseX < craftBtnX + 18 && mouseY >= craftBtnY && mouseY < craftBtnY + 16) {
            List<String> tip = new ArrayList<>();
            tip.add("\u00a7aCraftable Items Only");
            tip.add(config.isCraftableFilter() ? "\u00a77Status: \u00a7aENABLED" : "\u00a77Status: \u00a7cDISABLED");
            context.drawTooltip(tip, mouseX, mouseY);
            return;
        }

        // Graph button tooltip
        int graphBtnX = rightSidebarX + 20;
        if (mouseX >= graphBtnX && mouseX < graphBtnX + 18 && mouseY >= craftBtnY && mouseY < craftBtnY + 16) {
            List<String> tip = new ArrayList<>();
            tip.add("\u00a7bCraft Graph Planner (G)");
            tip.add("\u00a77Plan multi-step crafts & view raw requirements");
            context.drawTooltip(tip, mouseX, mouseY);
            return;
        }
    }

    public RgvStack getStackAt(int mouseX, int mouseY) {
        List<RgvStack> bookmarks = config.getBookmarks();
        if (!bookmarks.isEmpty()) {
            int startX = leftSidebarX;
            int startY = leftSidebarY + 16;
            int cols = Math.max(1, leftSidebarW / 18);
            for (int i = 0; i < bookmarks.size(); i++) {
                int col = i % cols;
                int row = i / cols;
                int slotX = startX + col * 18;
                int slotY = startY + row * 18;
                if (slotY + 18 > leftSidebarY + leftSidebarH) break;
                if (mouseX >= slotX && mouseX < slotX + 18 && mouseY >= slotY && mouseY < slotY + 18) {
                    return bookmarks.get(i);
                }
            }
        }

        if (!isNeiActive()) {
            List<RgvStack> pageItems = index.getItemsForCurrentPage();
            int cols = index.getColumns();
            int gridStartY = rightSidebarY + 18;
            for (int i = 0; i < pageItems.size(); i++) {
                int col = i % cols;
                int row = i / cols;
                int slotX = rightSidebarX + col * 18;
                int slotY = gridStartY + row * 18;
                if (mouseX >= slotX && mouseX < slotX + 18 && mouseY >= slotY && mouseY < slotY + 18) {
                    return pageItems.get(i);
                }
            }
        }

        return null;
    }

    public boolean isBookmarkAt(int mouseX, int mouseY) {
        List<RgvStack> bookmarks = config.getBookmarks();
        if (!bookmarks.isEmpty()) {
            int startX = leftSidebarX;
            int startY = leftSidebarY + 16;
            int cols = Math.max(1, leftSidebarW / 18);
            for (int i = 0; i < bookmarks.size(); i++) {
                int col = i % cols;
                int row = i / cols;
                int slotX = startX + col * 18;
                int slotY = startY + row * 18;
                if (slotY + 18 > leftSidebarY + leftSidebarH) break;
                if (mouseX >= slotX && mouseX < slotX + 18 && mouseY >= slotY && mouseY < slotY + 18) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (!config.isOverlayEnabled()) return false;

        // Modal recipe screen consumes clicks first
        if (recipeScreen.isOpen()) {
            return recipeScreen.mouseClicked(mouseX, mouseY, button);
        }

        // In NEI mode, handle only [G] button and bookmarks; pass everything else to NEI
        if (isNeiActive()) {
            int btnX = currentGuiLeft - 22;
            int btnY = currentGuiTop + 4;
            if (mouseX >= btnX && mouseX < btnX + 18 && mouseY >= btnY && mouseY < btnY + 18) {
                openGraph();
                return true;
            }

            RgvStack bStack = getStackAt(mouseX, mouseY);
            if (bStack == null && isHoveringBookmark) {
                bStack = hoveredStack;
            }
            if (bStack != null && (isBookmarkAt(mouseX, mouseY) || isHoveringBookmark)) {
                if (button == 0) {
                    openRecipesFor(bStack);
                    return true;
                } else if (button == 1) {
                    config.toggleBookmark(bStack);
                    hoveredStack = null;
                    return true;
                }
            }

            return false;
        }

        // Search bar click
        if (searchBar.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        // Craftable filter button click
        int craftBtnX = rightSidebarX;
        int craftBtnY = searchBar.getY();
        if (mouseX >= craftBtnX && mouseX < craftBtnX + 18 && mouseY >= craftBtnY && mouseY < craftBtnY + 16) {
            config.setCraftableFilter(!config.isCraftableFilter());
            updateCraftableFilter();
            return true;
        }

        // Craft Graph planner button [G] click
        int graphBtnX = rightSidebarX + 20;
        if (mouseX >= graphBtnX && mouseX < graphBtnX + 18 && mouseY >= craftBtnY && mouseY < craftBtnY + 16) {
            recipeScreen.openGraphView();
            return true;
        }

        // Page buttons < and >
        int pageY = rightSidebarY + 2;
        String pageInfo = (index.getCurrentPage() + 1) + " / " + index.getTotalPages();
        int infoW = RgvPlatform.get() != null ? 30 : 20;
        int centerX = rightSidebarX + (rightSidebarW - infoW) / 2;
        int prevBtnX = centerX - 16;
        int nextBtnX = centerX + infoW + 8;

        if (mouseX >= prevBtnX && mouseX < prevBtnX + 10 && mouseY >= pageY && mouseY < pageY + 12) {
            index.prevPage();
            return true;
        }
        if (mouseX >= nextBtnX && mouseX < nextBtnX + 10 && mouseY >= pageY && mouseY < pageY + 12) {
            index.nextPage();
            return true;
        }

        // Click on Stack in Index or Bookmarks
        RgvStack clickedStack = getStackAt(mouseX, mouseY);
        if (clickedStack == null) {
            clickedStack = hoveredStack;
        }

        if (clickedStack != null && !clickedStack.isEmpty()) {
            boolean isBookmark = isBookmarkAt(mouseX, mouseY) || (isHoveringBookmark && clickedStack == hoveredStack);
            if (isBookmark && button == 1) {
                config.toggleBookmark(clickedStack);
                hoveredStack = null;
                return true;
            }

            RgvPlatform platform = RgvPlatform.get();
            if (config.isCheatMode() && platform != null && platform.isCheatModeAllowed()) {
                platform.sendGiveItemPacket(clickedStack, button == 0);
                return true;
            }

            if (button == 0) {
                openRecipesFor(clickedStack);
            } else if (button == 1) {
                openUsesFor(clickedStack);
            }
            return true;
        }

        return false;
    }


    public void mouseReleased(int mouseX, int mouseY, int button) {
        if (recipeScreen.isOpen()) {
            recipeScreen.mouseReleased(mouseX, mouseY, button);
        }
    }

    public boolean mouseScrolled(int scrollDelta) {
        if (!config.isOverlayEnabled()) return false;
        if (recipeScreen.isOpen()) return false;

        if (scrollDelta > 0) {
            index.prevPage();
            return true;
        } else if (scrollDelta < 0) {
            index.nextPage();
            return true;
        }
        return false;
    }

    public boolean keyPressed(int keyCode, char typedChar, boolean isCtrlDown) {
        if (!config.isOverlayEnabled()) {
            // Ctrl+O toggles overlay back on
            if (isCtrlDown && (keyCode == 24 || typedChar == 'o' || typedChar == 'O')) {
                config.setOverlayEnabled(true);
                return true;
            }
            return false;
        }

        // Modal recipe screen consumes key first
        if (recipeScreen.isOpen()) {
            if (recipeScreen.keyPressed(keyCode, typedChar)) {
                return true;
            }
        }

        // Search bar typing
        if (searchBar.isFocused()) {
            if (keyCode == 1) { // Escape unfocuses search bar
                searchBar.setFocused(false);
                return true;
            }
            if (searchBar.keyPressed(keyCode, typedChar)) {
                return true;
            }
        }

        // Ctrl+F: Focus Search
        if (isCtrlDown && (keyCode == 33 || typedChar == 'f' || typedChar == 'F')) {
            searchBar.setFocused(true);
            return true;
        }

        // Ctrl+O: Toggle Overlay
        if (isCtrlDown && (keyCode == 24 || typedChar == 'o' || typedChar == 'O')) {
            config.setOverlayEnabled(!config.isOverlayEnabled());
            return true;
        }

        // Shortcuts when hovering a stack
        if (hoveredStack != null && !hoveredStack.isEmpty() && !searchBar.isFocused()) {
            // 'R' (Key 19): Recipes
            if (keyCode == 19 || typedChar == 'r' || typedChar == 'R') {
                openRecipesFor(hoveredStack);
                return true;
            }
            // 'U' (Key 22): Uses
            if (keyCode == 22 || typedChar == 'u' || typedChar == 'U') {
                openUsesFor(hoveredStack);
                return true;
            }
            // 'A' (Key 30): Bookmark
            if (keyCode == 30 || typedChar == 'a' || typedChar == 'A') {
                config.toggleBookmark(hoveredStack);
                return true;
            }
        }

        // 'G' (Key 34): Open Graph View directly
        if (!searchBar.isFocused() && (keyCode == 34 || typedChar == 'g' || typedChar == 'G')) {
            recipeScreen.openGraphView();
            return true;
        }

        return false;
    }

    @Override
    public void openRecipesFor(RgvIngredient ingredient) {
        if (isNeiRecipeViewerPresent()) {
            RgvPlatform platform = RgvPlatform.get();
            if (platform != null && platform.openExternalRecipeViewer(ingredient, false)) {
                return;
            }
        }
        List<RgvRecipe> recipes = recipeManager.getRecipesFor(ingredient);
        if (!recipes.isEmpty()) {
            recipeScreen.openWithRecipes(recipes, "Recipes for: " + ingredient.getDisplayName());
        }
    }

    @Override
    public void openUsesFor(RgvIngredient ingredient) {
        if (isNeiRecipeViewerPresent()) {
            RgvPlatform platform = RgvPlatform.get();
            if (platform != null && platform.openExternalRecipeViewer(ingredient, true)) {
                return;
            }
        }
        List<RgvRecipe> uses = recipeManager.getUsesFor(ingredient);
        if (!uses.isEmpty()) {
            recipeScreen.openWithRecipes(uses, "Uses for: " + ingredient.getDisplayName());
        }
    }
}
