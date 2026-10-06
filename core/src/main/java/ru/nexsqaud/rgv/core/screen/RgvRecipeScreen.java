package ru.nexsqaud.rgv.core.screen;

import ru.nexsqaud.rgv.api.*;
import ru.nexsqaud.rgv.api.widget.RgvWidget;
import ru.nexsqaud.rgv.api.widget.RgvWidgetHolder;
import ru.nexsqaud.rgv.api.widget.SlotWidget;
import ru.nexsqaud.rgv.core.config.RgvConfig;
import ru.nexsqaud.rgv.core.platform.RgvPlatform;
import ru.nexsqaud.rgv.core.platform.TransferHelper;
import ru.nexsqaud.rgv.core.platform.TransferStatus;
import ru.nexsqaud.rgv.core.recipe.RgvRecipeManager;
import ru.nexsqaud.rgv.core.tree.RgvCraftGraph;
import ru.nexsqaud.rgv.core.tree.RgvCraftGraphTab;
import ru.nexsqaud.rgv.core.tree.RgvGraphNode;

import java.util.*;

/**
 * Modal Recipe Viewer & Tabbed Craft Graph Dialog.
 * Supports top-to-bottom craft graphs, free view panning,
 * leaf requirements summary, and manual recipe expansion.
 */
public class RgvRecipeScreen {

    public enum ViewMode {
        RECIPES,
        GRAPH
    }

    public interface ScreenListener {
        void onOpen();
        void onClose();
    }

    public interface RecipeLookupHandler {
        void openRecipesFor(RgvIngredient ingredient);
        void openUsesFor(RgvIngredient ingredient);
    }

    private final RgvRecipeManager recipeManager;
    private final RgvCraftGraph craftGraph = new RgvCraftGraph();
    private ViewMode viewMode = ViewMode.RECIPES;
    private ScreenListener listener;
    private RecipeLookupHandler lookupHandler;
    private RgvConfig config;

    // Recipe View state
    private final List<RgvRecipeCategory> activeCategories = new ArrayList<>();
    private int currentCategoryIndex = 0;
    private List<RgvRecipe> currentRecipes = new ArrayList<>();
    private int currentRecipeIndex = 0;
    private RgvStack targetStack = null;

    // Dialog layout
    private int x;
    private int y;
    private int width = 176;
    private int height = 140;

    // Recipe pagination button hitboxes
    private int prevBtnX, prevBtnY, prevBtnW, prevBtnH;
    private int nextBtnX, nextBtnY, nextBtnW, nextBtnH;

    private final List<RgvWidget> activeWidgets = new ArrayList<>();
    private RgvWidget hoveredWidget = null;
    private RgvStack hoveredLeafStack = null;
    private long hoveredLeafRequired = 0;
    private long hoveredLeafAvailable = 0;
    private RgvGraphNode hoveredGraphNode = null;
    private RgvStack hoveredLeftoverStack = null;
    private long hoveredLeftoverAmount = 0;
    private boolean open = false;
    private long openTime = 0;

    // Item selector pagination for manual new tab
    private int itemPickerPage = 0;

    // Free view panning in graph mode
    private float panX = 0;
    private float panY = 0;
    private boolean isPanning = false;
    private int panDragStartX = 0;
    private int panDragStartY = 0;
    private float panInitialX = 0;
    private float panInitialY = 0;
    private int lastActiveTabIndex = -1;
    private final List<TabHitbox> renderedTabHitboxes = new ArrayList<>();
    private int renderedNewTabBtnX = -1;
    private int renderedNewTabBtnY = -1;

    private static class TabHitbox {
        final int index;
        final int x;
        final int y;
        final int w;
        final int h;
        final int closeX;
        final int closeY;
        final int closeW;
        final int closeH;

        TabHitbox(int index, int x, int y, int w, int h, int closeX, int closeY, int closeW, int closeH) {
            this.index = index;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.closeX = closeX;
            this.closeY = closeY;
            this.closeW = closeW;
            this.closeH = closeH;
        }
    }

    // Graph recipe selection mode (when choosing a craft recipe for an ingredient in the graph)
    private boolean isSelectingForGraph = false;
    private RgvCraftGraphTab graphSelectingTab = null;
    private RgvGraphNode graphSelectingNode = null;

    public RgvRecipeScreen(RgvRecipeManager recipeManager) {
        this.recipeManager = recipeManager;
    }

    public void setConfig(RgvConfig config) {
        this.config = config;
    }

    public void setListener(ScreenListener listener) {
        this.listener = listener;
    }

    public void setLookupHandler(RecipeLookupHandler lookupHandler) {
        this.lookupHandler = lookupHandler;
    }

    public RgvCraftGraph getCraftGraph() {
        return craftGraph;
    }

    public boolean isOpen() {
        return open;
    }

    public boolean isPickerOpen() {
        return isSelectingForGraph;
    }

    public void closePicker() {
        this.isSelectingForGraph = false;
        this.graphSelectingTab = null;
        this.graphSelectingNode = null;
    }

    public void close() {
        this.open = false;
        this.isSelectingForGraph = false;
        this.graphSelectingTab = null;
        this.graphSelectingNode = null;
        if (craftGraph != null) {
            craftGraph.save();
        }
        if (listener != null) {
            listener.onClose();
        }
    }

    public void openRecipePickerForNode(RgvCraftGraphTab tab, RgvGraphNode node, List<RgvRecipe> recipes) {
        if (recipes == null || recipes.isEmpty()) return;
        this.graphSelectingTab = tab;
        this.graphSelectingNode = node;
        this.isSelectingForGraph = true;
        this.targetStack = node != null ? node.getStack() : (tab != null ? tab.getTargetStack() : null);
        this.activeCategories.clear();
        Set<RgvRecipeCategory> cats = new LinkedHashSet<>();
        for (RgvRecipe r : recipes) {
            if (r != null && r.getCategory() != null) cats.add(r.getCategory());
        }
        this.activeCategories.addAll(cats);
        this.currentCategoryIndex = 0;
        this.currentRecipes = new ArrayList<>(recipes);
        this.currentRecipeIndex = 0;
        this.viewMode = ViewMode.RECIPES;
        this.open = true;
        this.openTime = System.currentTimeMillis();
        rebuildActiveLayout();
    }

    public ViewMode getViewMode() {
        return viewMode;
    }

    public void setViewMode(ViewMode viewMode) {
        this.viewMode = viewMode;
        closePicker();

        if (viewMode == ViewMode.RECIPES) {
            // Populate recipes if empty or sync with active graph tab
            RgvCraftGraphTab activeTab = craftGraph.getActiveTab();
            if (activeTab != null && activeTab.getTargetStack() != null && !activeTab.getTargetStack().isEmpty()) {
                List<RgvRecipe> recipes = recipeManager.getRecipesFor(activeTab.getTargetStack());
                if (!recipes.isEmpty()) {
                    openWithRecipes(recipes, "Recipes for: " + activeTab.getTargetStack().getDisplayName());
                    return;
                }
            }
            if (currentRecipes.isEmpty()) {
                List<RgvRecipe> all = recipeManager.getAllRecipes();
                if (!all.isEmpty()) {
                    openWithRecipes(all, "All Recipes");
                    return;
                }
            }
        } else if (viewMode == ViewMode.GRAPH) {
            RgvCraftGraphTab activeTab = craftGraph.getActiveTab();
            if (activeTab != null) {
                centerGraph(activeTab, width - 12, height - 52);
            }
        }
        rebuildActiveLayout();
    }

    public void openWithRecipes(List<RgvRecipe> recipes, String title) {
        if (recipes == null || recipes.isEmpty()) return;
        this.open = true;
        this.openTime = System.currentTimeMillis();
        this.viewMode = ViewMode.RECIPES;
        closePicker();

        this.activeCategories.clear();
        for (RgvRecipe r : recipes) {
            RgvRecipeCategory cat = r.getCategory();
            if (cat != null && !activeCategories.contains(cat)) {
                activeCategories.add(cat);
            }
        }
        if (activeCategories.isEmpty()) {
            activeCategories.addAll(recipeManager.getCategories());
        }

        this.currentCategoryIndex = 0;
        selectCategory(activeCategories.get(0), recipes);

        if (listener != null) {
            listener.onOpen();
        }
    }

    public void openGraphView() {
        if (craftGraph != null) {
            craftGraph.clearPendingTarget();
        }
        this.open = true;
        this.openTime = System.currentTimeMillis();
        this.viewMode = ViewMode.GRAPH;
        closePicker();

        if (craftGraph.isEmpty()) {
            craftGraph.createNewEmptyTab();
        }
        rebuildActiveLayout();

        RgvCraftGraphTab activeTab = craftGraph.getActiveTab();
        if (activeTab != null) {
            centerGraph(activeTab, width - 12, height - 52);
        }

        if (listener != null) {
            listener.onOpen();
        }
    }

    private void selectCategory(RgvRecipeCategory category, List<RgvRecipe> filteredSubset) {
        currentRecipes.clear();
        if (filteredSubset != null && !filteredSubset.isEmpty()) {
            for (RgvRecipe r : filteredSubset) {
                if (category.equals(r.getCategory())) {
                    currentRecipes.add(r);
                }
            }
        }
        if (currentRecipes.isEmpty()) {
            currentRecipes.addAll(recipeManager.getRecipesByCategory(category));
        }

        currentRecipeIndex = 0;
        rebuildActiveLayout();
    }

    private void rebuildActiveLayout() {
        activeWidgets.clear();
        RgvPlatform platform = RgvPlatform.get();
        int screenW = platform != null ? platform.getScreenWidth() : 400;
        int screenH = platform != null ? platform.getScreenHeight() : 300;

        if (viewMode == ViewMode.GRAPH) {
            this.width = Math.max(340, Math.min(screenW - 20, 480));
            this.height = Math.max(240, Math.min(screenH - 20, 320));
            this.x = (screenW - width) / 2;
            this.y = (screenH - height) / 2;

            RgvCraftGraphTab activeTab = craftGraph.getActiveTab();
            if (activeTab != null) {
                centerGraph(activeTab, width - 12, height - 52);
            }
            return;
        }

        // If categories are empty, load from recipe manager
        if (activeCategories.isEmpty()) {
            activeCategories.addAll(recipeManager.getCategories());
            if (!activeCategories.isEmpty()) {
                selectCategory(activeCategories.get(0), null);
            }
        }

        if (currentRecipes.isEmpty()) {
            this.width = 240;
            this.height = 160;
            this.x = (screenW - width) / 2;
            this.y = (screenH - height) / 2;
            prevBtnW = 0;
            nextBtnW = 0;
            return;
        }

        if (currentRecipes.size() <= 1) {
            prevBtnW = 0;
            nextBtnW = 0;
        }

        RgvRecipe recipe = currentRecipes.get(currentRecipeIndex);
        int recipeW = recipe.getDisplayWidth();
        int recipeH = recipe.getDisplayHeight();

        int minW = isSelectingForGraph ? 240 : 200;
        int minH = isSelectingForGraph ? 150 : 140;
        this.width = Math.max(minW, recipeW + 32);
        this.height = Math.max(minH, recipeH + 54);

        this.x = (screenW - width) / 2;
        this.y = (screenH - height) / 2;

        int contentX = x + (width - recipeW) / 2;
        int contentY = y + 34;

        RgvWidgetHolder holder = new RgvWidgetHolder(recipeW, recipeH);
        recipe.addWidgets(holder);

        for (RgvWidget widget : holder.getWidgets()) {
            if (widget instanceof SlotWidget) {
                SlotWidget slot = (SlotWidget) widget;
                SlotWidget placed = new SlotWidget(slot.getIngredient(), contentX + slot.getX(), contentY + slot.getY(), slot.isOutput());
                placed.setCatalyst(slot.isCatalyst());
                placed.setClickListener((s, button) -> {
                    if (lookupHandler != null && s.getIngredient() != null) {
                        if (button == 0) lookupHandler.openRecipesFor(s.getIngredient());
                        else lookupHandler.openUsesFor(s.getIngredient());
                    }
                });
                activeWidgets.add(placed);
            } else {
                widget.setX(contentX + widget.getX());
                widget.setY(contentY + widget.getY());
                activeWidgets.add(widget);
            }
        }
    }

    public void render(RgvDrawContext context, int mouseX, int mouseY, float delta) {
        if (!open) return;
        hoveredWidget = null;
        hoveredLeafStack = null;
        hoveredGraphNode = null;
        hoveredLeftoverStack = null;
        hoveredLeftoverAmount = 0;

        RgvPlatform platform = RgvPlatform.get();
        int screenW = platform != null ? platform.getScreenWidth() : 400;
        int screenH = platform != null ? platform.getScreenHeight() : 300;

        // Dark modal backdrop
        context.drawRect(0, 0, screenW, screenH, 0x60000000);

        // Dialog frame
        context.drawRect(x, y, width, height, 0xFF373737);
        context.drawRect(x + 1, y + 1, width - 2, height - 2, 0xFFC6C6C6);
        context.drawRect(x + 2, y + 2, width - 4, height - 4, 0xFF8B8B8B);
        context.drawRect(x + 3, y + 3, width - 6, height - 6, 0xFFC6C6C6);

        // Top Header bar
        context.drawRect(x + 4, y + 4, width - 8, 20, 0xFF3F3F3F);

        // Close button [x]
        int closeX = x + width - 18;
        int closeY = y + 6;
        boolean closeHover = (mouseX >= closeX && mouseX < closeX + 14 && mouseY >= closeY && mouseY < closeY + 14);
        context.drawRect(closeX, closeY, 14, 14, closeHover ? 0xFFFF4444 : 0xFF555555);
        context.drawText("x", closeX + 4, closeY + 3, 0xFFFFFF, false);

        // View Mode Switcher buttons: [Recipes] and [Graph]
        int modeBtnX = closeX - 90;
        int modeBtnY = y + 6;
        boolean recHover = (mouseX >= modeBtnX && mouseX < modeBtnX + 42 && mouseY >= modeBtnY && mouseY < modeBtnY + 14);
        boolean graphHover = (mouseX >= modeBtnX + 44 && mouseX < modeBtnX + 86 && mouseY >= modeBtnY && mouseY < modeBtnY + 14);

        context.drawRect(modeBtnX, modeBtnY, 42, 14, viewMode == ViewMode.RECIPES ? 0xFF55AA55 : (recHover ? 0xFF777777 : 0xFF444444));
        context.drawText("Recipes", modeBtnX + 3, modeBtnY + 3, 0xFFFFFF, false);

        context.drawRect(modeBtnX + 44, modeBtnY, 42, 14, viewMode == ViewMode.GRAPH ? 0xFF55AA55 : (graphHover ? 0xFF777777 : 0xFF444444));
        context.drawText("Graph", modeBtnX + 49, modeBtnY + 3, 0xFFFFFF, false);

        if (viewMode == ViewMode.RECIPES) {
            renderRecipesMode(context, mouseX, mouseY, delta);
        } else {
            renderGraphMode(context, mouseX, mouseY, delta);
        }
    }

    private void renderRecipesMode(RgvDrawContext context, int mouseX, int mouseY, float delta) {
        // Category Tabs
        int tabX = x + 6;
        for (int i = 0; i < activeCategories.size(); i++) {
            RgvRecipeCategory cat = activeCategories.get(i);
            boolean isSelected = (i == currentCategoryIndex);
            context.drawRect(tabX, y + 5, 18, 18, isSelected ? 0xFF8B8B8B : 0xFF2A2A2A);
            cat.getIcon().render(context, tabX + 1, y + 6, delta);

            if (mouseX >= tabX && mouseX < tabX + 18 && mouseY >= y + 5 && mouseY < y + 23) {
                context.drawRect(tabX, y + 5, 18, 18, 0x40FFFFFF);
            }
            tabX += 20;
        }

        // Recipe Pagination & Title
        if (!currentRecipes.isEmpty()) {
            RgvRecipe currentRecipe = currentRecipes.get(currentRecipeIndex);
            String title;
            if (isSelectingForGraph) {
                String targetName = graphSelectingNode != null && graphSelectingNode.getStack() != null ?
                        graphSelectingNode.getStack().getDisplayName() :
                        (graphSelectingTab != null && graphSelectingTab.getTargetStack() != null ? graphSelectingTab.getTargetStack().getDisplayName() : "");
                title = "\u00a76Choose Craft: \u00a70" + targetName;
            } else {
                title = currentRecipe.getCategory().getTitle();
            }
            context.drawText(title, x + 8, y + 26, 0x222222, false);

            if (currentRecipes.size() > 1) {
                String pageText = (currentRecipeIndex + 1) + " / " + currentRecipes.size();
                int textW = context.getTextWidth(pageText);

                int nextX = x + width - 16;
                int textX = nextX - textW - 6;
                int prevX = textX - 12;

                prevBtnX = prevX - 3;
                prevBtnY = y + 21;
                prevBtnW = 14;
                prevBtnH = 14;

                nextBtnX = nextX - 3;
                nextBtnY = y + 21;
                nextBtnW = 14;
                nextBtnH = 14;

                boolean prevHover = mouseX >= prevBtnX && mouseX < prevBtnX + prevBtnW && mouseY >= prevBtnY && mouseY < prevBtnY + prevBtnH;
                boolean nextHover = mouseX >= nextBtnX && mouseX < nextBtnX + nextBtnW && mouseY >= nextBtnY && mouseY < nextBtnY + nextBtnH;

                drawVanillaButton(context, prevBtnX, prevBtnY, prevBtnW, prevBtnH, prevHover, true);
                drawVanillaButton(context, nextBtnX, nextBtnY, nextBtnW, nextBtnH, nextHover, true);

                context.drawText(pageText, textX, y + 26, 0x444444, false);
                context.drawText("<", prevX, y + 25, prevHover ? 0xFFFFFF : 0xAAAAAA, false);
                context.drawText(">", nextX, y + 25, nextHover ? 0xFFFFFF : 0xAAAAAA, false);
            } else {
                prevBtnW = 0;
                nextBtnW = 0;
            }
        } else {
            prevBtnW = 0;
            nextBtnW = 0;
            context.drawText("No crafting recipes found.", x + 12, y + 40, 0x222222, false);
            context.drawText("Obtain via world gathering or mob drops.", x + 12, y + 52, 0x444444, false);
        }

        for (RgvWidget widget : activeWidgets) {
            widget.render(context, mouseX, mouseY, delta);
            if (widget.isMouseOver(mouseX, mouseY)) {
                hoveredWidget = widget;
            }
        }

        // Bottom Action Buttons
        if (!currentRecipes.isEmpty()) {
            int btnY = y + height - 22;

            if (isSelectingForGraph) {
                // [Reset Raw] button on the left (if node has assigned recipe)
                if (graphSelectingNode != null && graphSelectingNode.hasAssignedRecipe()) {
                    int resetX = x + 8;
                    int resetW = 74;
                    boolean resetHover = mouseX >= resetX && mouseX < resetX + resetW && mouseY >= btnY && mouseY < btnY + 14;
                    drawVanillaButton(context, resetX, btnY, resetW, 14, resetHover, true);
                    context.drawText("Reset Raw", resetX + 10, btnY + 3, resetHover ? 0xFF8888 : 0xEE6666, true);
                }

                // [✓ Use Craft] button on the right
                int useW = 86;
                int useX = x + width - useW - 8;
                boolean useHover = mouseX >= useX && mouseX < useX + useW && mouseY >= btnY && mouseY < btnY + 14;
                drawVanillaButton(context, useX, btnY, useW, 14, useHover, true);
                context.drawText("\u2713 Use Craft", useX + 10, btnY + 3, useHover ? 0xFFFFFF : 0x55FF55, true);

                // [< Back] button to the left of Use Craft
                int backW = 50;
                int backX = useX - backW - 6;
                boolean backHover = mouseX >= backX && mouseX < backX + backW && mouseY >= btnY && mouseY < btnY + 14;
                drawVanillaButton(context, backX, btnY, backW, 14, backHover, true);
                context.drawText("< Back", backX + 8, btnY + 3, backHover ? 0xFFFFFF : 0xCCCCCC, false);
            } else {
                RgvRecipe current = currentRecipes.get(currentRecipeIndex);
                RgvPlatform platform = RgvPlatform.get();
                TransferStatus status = platform != null ? platform.getTransferStatus(current) : TransferStatus.NO_SUITABLE_CONTAINER;
                boolean canTransfer = (status == TransferStatus.AVAILABLE);

                int btnX = x + width - 22;
                boolean hover = mouseX >= btnX && mouseX < btnX + 14 && mouseY >= btnY && mouseY < btnY + 14;

                drawVanillaButton(context, btnX, btnY, 14, 14, hover, canTransfer);
                context.drawText("+", btnX + 4, btnY + 3, canTransfer ? (hover ? 0xFFFFFF : 0x55FF55) : 0x777777, true);

                int graphBtnX = btnX - 16;
                int graphBtnY = btnY;
                boolean gHover = mouseX >= graphBtnX && mouseX < graphBtnX + 14 && mouseY >= graphBtnY && mouseY < graphBtnY + 14;
                drawVanillaButton(context, graphBtnX, graphBtnY, 14, 14, gHover, true);
                drawGraphIcon(context, graphBtnX + 7, graphBtnY + 7, gHover);
            }
        }
    }

    private void renderGraphMode(RgvDrawContext context, int mouseX, int mouseY, float delta) {
        // Graph tabs bar
        int tabX = x + 6;
        int tabY = y + 25;
        int tabH = 18;
        List<RgvCraftGraphTab> tabs = craftGraph.getTabs();
        int activeIdx = craftGraph.getActiveTabIndex();

        if (activeIdx != lastActiveTabIndex) {
            lastActiveTabIndex = activeIdx;
            RgvCraftGraphTab activeTab = craftGraph.getActiveTab();
            if (activeTab != null) {
                centerGraph(activeTab, width - 12, height - 52);
            }
        }

        renderedTabHitboxes.clear();
        for (int i = 0; i < tabs.size(); i++) {
            RgvCraftGraphTab tab = tabs.get(i);
            boolean isActive = (i == activeIdx);
            String title = tab.getTitle();
            int titleW = context.getTextWidth(title);
            int tabW = 3 + 16 + 5 + titleW + 6 + 8 + 4;

            context.drawRect(tabX, tabY, tabW, tabH, isActive ? 0xFFC6C6C6 : 0xFF2A2A2A);
            context.drawRect(tabX + 1, tabY + 1, tabW - 2, tabH - 2, isActive ? 0xFFE0E0E0 : 0xFF3A3A3A);

            if (tab.getTargetStack() != null && !tab.getTargetStack().isEmpty()) {
                tab.getTargetStack().render(context, tabX + 3, tabY + 1, delta);
            }

            context.drawText(title, tabX + 24, tabY + 5, isActive ? 0x111111 : 0xAAAAAA, false);

            int closeTabX = tabX + 24 + titleW + 6;
            int closeBtnX = closeTabX - 3;
            int closeBtnY = tabY + 1;
            int closeBtnW = 14;
            int closeBtnH = tabH - 2;
            boolean closeHover = mouseX >= closeBtnX && mouseX < closeBtnX + closeBtnW
                    && mouseY >= closeBtnY && mouseY < closeBtnY + closeBtnH;

            // Contrast compliant: dark on active light tab, bright on inactive dark tab
            context.drawText("x", closeTabX, tabY + 5, closeHover ? 0xFFFF4444 : (isActive ? 0x444444 : 0xCCCCCC), false);

            renderedTabHitboxes.add(new TabHitbox(i, tabX, tabY, tabW, tabH, closeBtnX, closeBtnY, closeBtnW, closeBtnH));

            tabX += tabW + 3;
            if (tabX > x + width - 35) break;
        }

        renderedNewTabBtnX = tabX;
        renderedNewTabBtnY = tabY;
        boolean newHover = mouseX >= tabX && mouseX < tabX + 18 && mouseY >= tabY && mouseY < tabY + tabH;
        context.drawRect(tabX, tabY, 18, tabH, newHover ? 0xFF55AA55 : 0xFF444444);
        context.drawText("+", tabX + 6, tabY + 5, 0xFFFFFF, false);

        RgvCraftGraphTab activeTab = craftGraph.getActiveTab();
        if (activeTab == null) return;

        RgvInventory inv = RgvPlatform.get() != null ? RgvPlatform.get().getPlayerInventory() : null;

        if (activeTab.isSelectingItem()) {
            renderItemSelector(context, activeTab, mouseX, mouseY, delta);
            return;
        }

        if (activeTab.isSelectingRecipe()) {
            renderRecipeSelector(context, activeTab, mouseX, mouseY, delta);
            return;
        }

        // Viewport and node canvas
        int viewportX = x + 6;
        int viewportY = y + 46;
        int viewportW = width - 12;
        int viewportH = height - 52;

        // Viewport background
        context.drawRect(viewportX, viewportY, viewportW, viewportH, 0xFF1E1E1E);

        // Handle mouse dragging for free pan (with safe mouse button polling)
        if (isPanning) {
            RgvPlatform platform = RgvPlatform.get();
            if (platform != null && !platform.isMouseButtonDown(0) && !platform.isMouseButtonDown(1)) {
                isPanning = false;
            } else {
                int dx = mouseX - panDragStartX;
                int dy = mouseY - panDragStartY;
                panX = panInitialX + dx;
                panY = panInitialY + dy;
                clampPan(activeTab, viewportW, viewportH);
            }
        }

        // Clip rendering to viewport so graph cannot draw over borders/tabs
        context.enableScissor(viewportX, viewportY, viewportW, viewportH);

        // Draw background grid dots/lines
        renderGraphGrid(context, viewportX, viewportY, viewportW, viewportH);

        // Render Graph Nodes and Step Lines
        if (activeTab.getRootNode() != null) {
            renderConnectingLines(context, activeTab.getRootNode(), viewportX, viewportY);
            renderGraphNodes(context, activeTab.getRootNode(), viewportX, viewportY, mouseX, mouseY, delta);
        }

        context.disableScissor();

        // Target amount controls
        int controlY = y + 48;
        context.drawRect(x + 8, controlY, 96, 16, 0xDD2A2A2A);
        context.drawRect(x + 9, controlY + 1, 94, 14, 0xDD3A3A3A);
        context.drawText("Target: " + activeTab.getTargetAmount(), x + 12, controlY + 4, 0xFFFFFF, true);

        int minusX = x + 72;
        int plusX = x + 88;
        boolean minusHover = mouseX >= minusX && mouseX < minusX + 12 && mouseY >= controlY + 2 && mouseY < controlY + 14;
        boolean plusHover = mouseX >= plusX && mouseX < plusX + 12 && mouseY >= controlY + 2 && mouseY < controlY + 14;
        context.drawRect(minusX, controlY + 2, 12, 12, minusHover ? 0xFF888888 : 0xFF555555);
        context.drawText("-", minusX + 4, controlY + 3, 0xFFFFFF, false);
        context.drawRect(plusX, controlY + 2, 12, 12, plusHover ? 0xFF888888 : 0xFF555555);
        context.drawText("+", plusX + 3, controlY + 3, 0xFFFFFF, false);

        int nextBoxY = renderTopRightLeafRequirements(context, activeTab, inv, mouseX, mouseY, delta);
        renderTopRightLeftovers(context, activeTab, nextBoxY, mouseX, mouseY, delta);
    }

    private void renderGraphGrid(RgvDrawContext context, int vx, int vy, int vw, int vh) {
        // Grid lines provide subtle spatial orientation markers for panning navigation
        int gridSize = 20;
        int startX = (int) (panX % gridSize);
        int startY = (int) (panY % gridSize);

        for (int gx = vx + startX; gx < vx + vw; gx += gridSize) {
            if (gx >= vx) {
                context.drawRect(gx, vy, 1, vh, 0x15FFFFFF);
            }
        }
        for (int gy = vy + startY; gy < vy + vh; gy += gridSize) {
            if (gy >= vy) {
                context.drawRect(vx, gy, vw, 1, 0x15FFFFFF);
            }
        }
    }

    private void renderConnectingLines(RgvDrawContext context, RgvGraphNode node, int vx, int vy) {
        if (node == null) return;

        int px = (int) (vx + panX + node.x + node.width / 2);
        int py = (int) (vy + panY + node.y + node.height);

        for (RgvGraphNode child : node.getChildren()) {
            int cx = (int) (vx + panX + child.x + child.width / 2);
            int cy = (int) (vy + panY + child.y);
            int midY = py + (cy - py) / 2;

            context.drawRect(px, py, 1, midY - py, 0xFF888888);
            if (cx < px) {
                context.drawRect(cx, midY, px - cx + 1, 1, 0xFF888888);
            } else {
                context.drawRect(px, midY, cx - px + 1, 1, 0xFF888888);
            }
            context.drawRect(cx, midY, 1, cy - midY, 0xFF888888);

            renderConnectingLines(context, child, vx, vy);
        }
    }

    private void renderGraphNodes(RgvDrawContext context, RgvGraphNode node, int vx, int vy, int mouseX, int mouseY, float delta) {
        if (node == null) return;

        int sx = (int) (vx + panX + node.x);
        int sy = (int) (vy + panY + node.y);

        // Node hierarchy color coding: Gold for target root, Blue for expanded sub-recipe, Green for craftable leaf, Gray for raw
        int borderColor = 0xFF555555;
        if (node.getLevel() == 0) {
            borderColor = 0xFFE5B83B;
        } else if (node.hasAssignedRecipe()) {
            borderColor = 0xFF3399FF;
        } else if (node.canCraft(recipeManager)) {
            borderColor = 0xFF77AA77;
        }

        // Node box frame
        context.drawRect(sx, sy, node.width, node.height, 0xFF141414);
        context.drawRect(sx + 1, sy + 1, node.width - 2, node.height - 2, borderColor);
        context.drawRect(sx + 2, sy + 2, node.width - 4, node.height - 4, 0xFF2A2A2A);

        // Render stack icon
        if (node.getStack() != null && !node.getStack().isEmpty()) {
            node.getStack().render(context, sx + 4, sy + 4, delta);
        }

        // Amount badge
        String amt = String.valueOf(node.getAmount());
        context.drawText(amt, sx + node.width - context.getTextWidth(amt) - 2, sy + node.height - 9, 0xFFFFFF, true);

        // Small indicator icon for expandable leaf nodes
        if (node.isLeaf() && node.canCraft(recipeManager)) {
            context.drawText("+", sx + 2, sy + 1, 0x55FF55, true);
        }

        // Hover highlight
        if (mouseX >= sx && mouseX < sx + node.width && mouseY >= sy && mouseY < sy + node.height) {
            context.drawRect(sx + 1, sy + 1, node.width - 2, node.height - 2, 0x40FFFFFF);
            hoveredGraphNode = node;
        }

        // Render children
        for (RgvGraphNode child : node.getChildren()) {
            renderGraphNodes(context, child, vx, vy, mouseX, mouseY, delta);
        }
    }

    private int renderTopRightLeafRequirements(RgvDrawContext context, RgvCraftGraphTab tab, RgvInventory inv, int mouseX, int mouseY, float delta) {
        Map<RgvStack, Long> leaves = tab.getLeafNodeRequirements();
        if (leaves.isEmpty()) return y + 48;

        int count = leaves.size();
        int boxW = Math.max(120, Math.min(200, count * 26 + 12));
        int boxH = 34;
        int boxX = x + width - boxW - 8;
        int boxY = y + 48;

        // Background box for requirements
        context.drawRect(boxX, boxY, boxW, boxH, 0xEE1E1E1E);
        context.drawRect(boxX + 1, boxY + 1, boxW - 2, boxH - 2, 0xEE3D3D3D);
        context.drawText("Total Requirements:", boxX + 4, boxY + 3, 0xEEEEEE, true);

        int leafX = boxX + 4;
        int leafY = boxY + 13;

        for (Map.Entry<RgvStack, Long> entry : leaves.entrySet()) {
            RgvStack leaf = entry.getKey();
            long required = entry.getValue();
            long available = inv != null ? inv.getAmount(leaf) : 0;
            boolean satisfied = available >= required;

            // Draw mini slot
            context.drawRect(leafX, leafY, 18, 18, 0xFF141414);
            context.drawRect(leafX + 1, leafY + 1, 16, 16, satisfied ? 0xFF2A552A : 0xFF552A2A);
            leaf.render(context, leafX + 1, leafY + 1, delta);

            String countStr = String.valueOf(required);
            context.drawText(countStr, leafX + 18 - context.getTextWidth(countStr), leafY + 9, satisfied ? 0x55FF55 : 0xFF5555, true);

            // Hover check
            if (mouseX >= leafX && mouseX < leafX + 18 && mouseY >= leafY && mouseY < leafY + 18) {
                hoveredLeafStack = leaf;
                hoveredLeafRequired = required;
                hoveredLeafAvailable = available;
            }

            leafX += 22;
            if (leafX + 20 > boxX + boxW) break;
        }

        return boxY + boxH + 4;
    }

    private void renderTopRightLeftovers(RgvDrawContext context, RgvCraftGraphTab tab, int startY, int mouseX, int mouseY, float delta) {
        Map<RgvStack, Long> leftovers = tab.getLeftovers();
        if (leftovers.isEmpty()) return;

        int count = leftovers.size();
        int boxW = Math.max(120, Math.min(200, count * 26 + 12));
        int boxH = 34;
        int boxX = x + width - boxW - 8;
        int boxY = startY;

        // Background box for leftovers
        context.drawRect(boxX, boxY, boxW, boxH, 0xEE1E1E1E);
        context.drawRect(boxX + 1, boxY + 1, boxW - 2, boxH - 2, 0xEE3D3D3D);
        context.drawText("Leftovers:", boxX + 4, boxY + 3, 0xAADDFF, true);

        int slotX = boxX + 4;
        int slotY = boxY + 13;

        for (Map.Entry<RgvStack, Long> entry : leftovers.entrySet()) {
            RgvStack stack = entry.getKey();
            long amount = entry.getValue();

            // Mini slot with muted blue tint for surplus
            context.drawRect(slotX, slotY, 18, 18, 0xFF141414);
            context.drawRect(slotX + 1, slotY + 1, 16, 16, 0xFF22384D);
            stack.render(context, slotX + 1, slotY + 1, delta);

            String countStr = "+" + amount;
            context.drawText(countStr, slotX + 18 - context.getTextWidth(countStr), slotY + 9, 0x88CCFF, true);

            // Hover check
            if (mouseX >= slotX && mouseX < slotX + 18 && mouseY >= slotY && mouseY < slotY + 18) {
                hoveredLeftoverStack = stack;
                hoveredLeftoverAmount = amount;
            }

            slotX += 22;
            if (slotX + 20 > boxX + boxW) break;
        }
    }

    private void renderItemSelector(RgvDrawContext context, RgvCraftGraphTab tab, int mouseX, int mouseY, float delta) {
        context.drawText("Select an item to craft in this tab:", x + 10, y + 46, 0x222222, false);

        RgvPlatform platform = RgvPlatform.get();
        if (platform == null) return;
        List<RgvStack> items = platform.getAllKnownStacks();

        int cols = Math.max(1, (width - 24) / 18);
        int rows = Math.max(1, (height - 80) / 18);
        int pageSize = cols * rows;
        int totalPages = Math.max(1, (int) Math.ceil((double) items.size() / pageSize));
        if (itemPickerPage >= totalPages) itemPickerPage = totalPages - 1;

        // Page controls
        String pInfo = (itemPickerPage + 1) + " / " + totalPages;
        context.drawText(pInfo, x + width - 60, y + 46, 0x444444, false);

        int startX = x + 10;
        int startY = y + 60;
        int startIdx = itemPickerPage * pageSize;

        for (int i = 0; i < pageSize && startIdx + i < items.size(); i++) {
            int col = i % cols;
            int row = i / cols;
            int slotX = startX + col * 18;
            int slotY = startY + row * 18;

            RgvStack stack = items.get(startIdx + i);
            context.drawRect(slotX, slotY, 18, 18, 0xFF373737);
            stack.render(context, slotX + 1, slotY + 1, delta);

            if (mouseX >= slotX && mouseX < slotX + 18 && mouseY >= slotY && mouseY < slotY + 18) {
                context.drawRect(slotX + 1, slotY + 1, 16, 16, 0x80FFFFFF);
                hoveredLeafStack = stack;
                hoveredLeafRequired = 0;
            }
        }
    }

    private void renderRecipeSelector(RgvDrawContext context, RgvCraftGraphTab tab, int mouseX, int mouseY, float delta) {
        context.drawText("Select a recipe for " + tab.getTargetStack().getDisplayName() + ":", x + 10, y + 46, 0x222222, false);

        List<RgvRecipe> recipes = tab.getCandidateRecipes();
        if (recipes.isEmpty()) {
            context.drawText("No crafting recipes found for this item.", x + 12, y + 64, 0x222222, false);
            context.drawText("It is a raw resource or mob drop.", x + 12, y + 76, 0x444444, false);
            return;
        }

        int recipeY = y + 62;

        for (int i = 0; i < Math.min(recipes.size(), 4); i++) {
            RgvRecipe r = recipes.get(i);
            boolean hover = mouseX >= x + 10 && mouseX < x + width - 20 && mouseY >= recipeY && mouseY < recipeY + 30;

            context.drawRect(x + 10, recipeY, width - 20, 28, hover ? 0xFF888888 : 0xFF555555);
            context.drawRect(x + 11, recipeY + 1, width - 22, 26, hover ? 0xFFAAAAAA : 0xFF777777);

            // Recipe category icon
            r.getCategory().getIcon().render(context, x + 14, recipeY + 5, delta);
            context.drawText(r.getCategory().getTitle() + " Recipe #" + (i + 1), x + 34, recipeY + 10, 0xFFFFFF, true);

            recipeY += 32;
        }
    }

    public void centerGraph(RgvCraftGraphTab tab, int viewportW, int viewportH) {
        if (tab == null || tab.getRootNode() == null) {
            panX = 0;
            panY = 0;
            return;
        }
        float graphCenterX = (tab.getMinX() + tab.getMaxX()) / 2.0f;
        panX = (viewportW / 2.0f) - graphCenterX;

        int graphH = tab.getMaxY() - tab.getMinY();
        if (graphH < viewportH - 60) {
            panY = Math.max(30.0f, (viewportH - graphH) / 2.0f) - tab.getMinY();
        } else {
            panY = 30.0f - tab.getMinY();
        }
        clampPan(tab, viewportW, viewportH);
    }

    private void clampPan(RgvCraftGraphTab tab, int viewportW, int viewportH) {
        if (tab == null || tab.getRootNode() == null) return;
        int minX = tab.getMinX();
        int maxX = tab.getMaxX();
        int minY = tab.getMinY();
        int maxY = tab.getMaxY();

        // At least 60px of the graph remains inside the viewport horizontally
        float minPanX = 60 - maxX;
        float maxPanX = viewportW - 60 - minX;
        if (maxPanX < minPanX) {
            panX = (minPanX + maxPanX) / 2.0f;
        } else {
            panX = Math.max(minPanX, Math.min(maxPanX, panX));
        }

        // At least 40px remains inside viewport vertically
        float minPanY = 40 - maxY;
        float maxPanY = viewportH - 40 - minY;
        if (maxPanY < minPanY) {
            panY = (minPanY + maxPanY) / 2.0f;
        } else {
            panY = Math.max(minPanY, Math.min(maxPanY, panY));
        }
    }

    public void renderTooltips(RgvDrawContext context, int mouseX, int mouseY) {
        if (!open) return;

        if (viewMode == ViewMode.RECIPES) {
            if (hoveredWidget != null) {
                List<String> tip = hoveredWidget.getTooltip(mouseX, mouseY);
                if (!tip.isEmpty()) {
                    context.drawTooltip(tip, mouseX, mouseY);
                    return;
                }
            }

            if (currentRecipes.size() > 1) {
                if (mouseX >= prevBtnX && mouseX < prevBtnX + prevBtnW && mouseY >= prevBtnY && mouseY < prevBtnY + prevBtnH) {
                    List<String> tip = new ArrayList<>();
                    tip.add("\u00a7fPrevious Recipe");
                    tip.add("\u00a77Show previous recipe variant");
                    context.drawTooltip(tip, mouseX, mouseY);
                    return;
                }
                if (mouseX >= nextBtnX && mouseX < nextBtnX + nextBtnW && mouseY >= nextBtnY && mouseY < nextBtnY + nextBtnH) {
                    List<String> tip = new ArrayList<>();
                    tip.add("\u00a7fNext Recipe");
                    tip.add("\u00a77Show next recipe variant");
                    context.drawTooltip(tip, mouseX, mouseY);
                    return;
                }
            }

            int btnY = y + height - 22;

            if (isSelectingForGraph) {
                int useW = 86;
                int useX = x + width - useW - 8;
                if (mouseX >= useX && mouseX < useX + useW && mouseY >= btnY && mouseY < btnY + 14) {
                    List<String> tip = new ArrayList<>();
                    tip.add("\u00a7aUse This Recipe");
                    tip.add("\u00a77Apply this recipe to the craft graph");
                    context.drawTooltip(tip, mouseX, mouseY);
                    return;
                }

                int backW = 50;
                int backX = useX - backW - 6;
                if (mouseX >= backX && mouseX < backX + backW && mouseY >= btnY && mouseY < btnY + 14) {
                    List<String> tip = new ArrayList<>();
                    tip.add("\u00a77Back to Graph");
                    tip.add("\u00a78Cancel recipe selection");
                    context.drawTooltip(tip, mouseX, mouseY);
                    return;
                }

                if (graphSelectingNode != null && graphSelectingNode.hasAssignedRecipe()) {
                    int resetW = 74;
                    int resetX = x + 8;
                    if (mouseX >= resetX && mouseX < resetX + resetW && mouseY >= btnY && mouseY < btnY + 14) {
                        List<String> tip = new ArrayList<>();
                        tip.add("\u00a7cReset to Raw Material");
                        tip.add("\u00a77Clear assigned recipe and treat as raw ingredient");
                        context.drawTooltip(tip, mouseX, mouseY);
                        return;
                    }
                }
            } else {
                int btnX = x + width - 22;
                if (mouseX >= btnX && mouseX < btnX + 14 && mouseY >= btnY && mouseY < btnY + 14) {
                    List<String> tip = new ArrayList<>();
                    RgvRecipe current = !currentRecipes.isEmpty() ? currentRecipes.get(currentRecipeIndex) : null;
                    RgvPlatform platform = RgvPlatform.get();
                    TransferStatus status = platform != null ? platform.getTransferStatus(current) : TransferStatus.NO_SUITABLE_CONTAINER;

                    if (status == TransferStatus.AVAILABLE) {
                        tip.add("\u00a7aTransfer Ingredients (+)");
                        tip.add("\u00a77Click to fill open crafting container");
                        tip.add("\u00a78Right-click / shift-click to craft maximum");
                    } else if (status == TransferStatus.MISSING_INGREDIENTS) {
                        tip.add("\u00a7cTransfer Disabled");
                        tip.add("\u00a7cMissing required ingredients in inventory");
                    } else {
                        tip.add("\u00a7cTransfer Disabled");
                        String required = platform != null ? platform.getRequiredContainerDescription(current) : TransferHelper.getRequiredContainerDescription(current);
                        tip.add("\u00a7cRequires " + required);
                        if (platform != null && current != null) {
                            RgvInventory inv = platform.getPlayerInventory();
                            if (inv != null && !current.canCraft(inv)) {
                                tip.add("\u00a7cMissing required ingredients");
                            }
                        }
                    }
                    context.drawTooltip(tip, mouseX, mouseY);
                    return;
                }

                int graphBtnX = btnX - 16;
                if (mouseX >= graphBtnX && mouseX < graphBtnX + 14 && mouseY >= btnY && mouseY < btnY + 14) {
                    List<String> tip = new ArrayList<>();
                    tip.add("\u00a7bAdd to Craft Graph");
                    tip.add("\u00a77Creates a new tab in the Craft Graph planner");
                    context.drawTooltip(tip, mouseX, mouseY);
                    return;
                }
            }
        } else {
            // Graph Mode tooltips
            if (hoveredGraphNode != null && hoveredGraphNode.getStack() != null) {
                List<String> tip = new ArrayList<>(hoveredGraphNode.getStack().getTooltip());
                tip.add("\u00a7eRequired for plan: " + hoveredGraphNode.getAmount());
                if (hoveredGraphNode.hasAssignedRecipe()) {
                    tip.add("\u00a7aCrafted with: " + hoveredGraphNode.getAssignedRecipe().getCategory().getTitle());
                    if (hoveredGraphNode.getSurplusAmount() > 0) {
                        tip.add("\u00a7bSurplus produced: +" + hoveredGraphNode.getSurplusAmount());
                    }
                    tip.add("\u00a77Click to change or reset recipe");
                } else if (hoveredGraphNode.canCraft(recipeManager)) {
                    tip.add("\u00a7bClick to select crafting recipe");
                } else {
                    tip.add("\u00a77Raw Material (No craft recipes)");
                }
                context.drawTooltip(tip, mouseX, mouseY);
                return;
            }

            if (hoveredLeafStack != null && !hoveredLeafStack.isEmpty()) {
                List<String> tip = new ArrayList<>(hoveredLeafStack.getTooltip());
                if (hoveredLeafRequired > 0) {
                    tip.add("\u00a7eRequired: " + hoveredLeafRequired);
                    tip.add("\u00a7bIn Inventory: " + hoveredLeafAvailable);
                    if (hoveredLeafAvailable < hoveredLeafRequired) {
                        tip.add("\u00a7cMissing: " + (hoveredLeafRequired - hoveredLeafAvailable));
                    } else {
                        tip.add("\u00a7aReady to Craft!");
                    }
                }
                context.drawTooltip(tip, mouseX, mouseY);
                return;
            }

            if (hoveredLeftoverStack != null && !hoveredLeftoverStack.isEmpty()) {
                List<String> tip = new ArrayList<>(hoveredLeftoverStack.getTooltip());
                tip.add("\u00a7bLeftover from crafting: +" + hoveredLeftoverAmount);
                context.drawTooltip(tip, mouseX, mouseY);
                return;
            }
        }
    }

    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (!open) return false;

        // Close button [x]
        int closeX = x + width - 18;
        int closeY = y + 6;
        if (mouseX >= closeX && mouseX < closeX + 14 && mouseY >= closeY && mouseY < closeY + 14) {
            close();
            return true;
        }

        // View Mode Switcher
        int modeBtnX = closeX - 90;
        int modeBtnY = y + 6;
        if (mouseX >= modeBtnX && mouseX < modeBtnX + 42 && mouseY >= modeBtnY && mouseY < modeBtnY + 14) {
            setViewMode(ViewMode.RECIPES);
            return true;
        }
        if (mouseX >= modeBtnX + 44 && mouseX < modeBtnX + 86 && mouseY >= modeBtnY && mouseY < modeBtnY + 14) {
            setViewMode(ViewMode.GRAPH);
            return true;
        }

        if (viewMode == ViewMode.RECIPES) {
            return handleRecipeModeClick(mouseX, mouseY, button);
        } else {
            return handleGraphModeClick(mouseX, mouseY, button);
        }
    }

    private boolean handleRecipeModeClick(int mouseX, int mouseY, int button) {
        // Category tabs
        int tabX = x + 6;
        for (int i = 0; i < activeCategories.size(); i++) {
            if (mouseX >= tabX && mouseX < tabX + 18 && mouseY >= y + 5 && mouseY < y + 23) {
                currentCategoryIndex = i;
                selectCategory(activeCategories.get(i), null);
                return true;
            }
            tabX += 20;
        }

        // Page buttons < and >
        if (currentRecipes.size() > 1) {
            if (mouseX >= prevBtnX && mouseX < prevBtnX + prevBtnW && mouseY >= prevBtnY && mouseY < prevBtnY + prevBtnH) {
                if (currentRecipeIndex > 0) currentRecipeIndex--;
                else currentRecipeIndex = currentRecipes.size() - 1;
                rebuildActiveLayout();
                return true;
            }
            if (mouseX >= nextBtnX && mouseX < nextBtnX + nextBtnW && mouseY >= nextBtnY && mouseY < nextBtnY + nextBtnH) {
                if (currentRecipeIndex < currentRecipes.size() - 1) currentRecipeIndex++;
                else currentRecipeIndex = 0;
                rebuildActiveLayout();
                return true;
            }
        }

        // Bottom Action Buttons
        int btnY = y + height - 22;
        if (isSelectingForGraph) {
            if (graphSelectingNode != null && graphSelectingNode.hasAssignedRecipe()) {
                int resetX = x + 8;
                int resetW = 74;
                if (mouseX >= resetX && mouseX < resetX + resetW && mouseY >= btnY && mouseY < btnY + 14) {
                    if (graphSelectingTab != null) {
                        graphSelectingTab.removeRecipeForIngredient(graphSelectingNode.getStack(), recipeManager);
                        craftGraph.save();
                    }
                    closePicker();
                    setViewMode(ViewMode.GRAPH);
                    if (graphSelectingTab != null) {
                        centerGraph(graphSelectingTab, width - 12, height - 52);
                    }
                    return true;
                }
            }

            int useW = 86;
            int useX = x + width - useW - 8;
            if (mouseX >= useX && mouseX < useX + useW && mouseY >= btnY && mouseY < btnY + 14) {
                if (!currentRecipes.isEmpty()) {
                    RgvRecipe chosen = currentRecipes.get(currentRecipeIndex);
                    if (graphSelectingNode != null && graphSelectingTab != null) {
                        graphSelectingTab.setRecipeForIngredient(graphSelectingNode.getStack(), chosen, recipeManager);
                        craftGraph.save();
                    } else if (graphSelectingTab != null && graphSelectingTab.isSelectingRecipe()) {
                        graphSelectingTab.assignRecipe(chosen, graphSelectingTab.getTargetStack(), recipeManager);
                        craftGraph.save();
                    }
                    closePicker();
                    setViewMode(ViewMode.GRAPH);
                    if (graphSelectingTab != null) {
                        centerGraph(graphSelectingTab, width - 12, height - 52);
                    }
                }
                return true;
            }

            int backW = 50;
            int backX = useX - backW - 6;
            if (mouseX >= backX && mouseX < backX + backW && mouseY >= btnY && mouseY < btnY + 14) {
                closePicker();
                setViewMode(ViewMode.GRAPH);
                if (graphSelectingTab != null) {
                    centerGraph(graphSelectingTab, width - 12, height - 52);
                }
                return true;
            }
        } else {
            // Transfer '+' button
            int btnX = x + width - 22;
            if (mouseX >= btnX && mouseX < btnX + 14 && mouseY >= btnY && mouseY < btnY + 14) {
                if (!currentRecipes.isEmpty() && RgvPlatform.get() != null) {
                    RgvRecipe r = currentRecipes.get(currentRecipeIndex);
                    TransferStatus status = RgvPlatform.get().getTransferStatus(r);
                    if (status == TransferStatus.AVAILABLE) {
                        boolean transferred = RgvPlatform.get().transferRecipe(r, button == 1);
                        if (transferred) {
                            close();
                        }
                    }
                }
                return true;
            }

            // Add to Graph button
            int graphBtnX = btnX - 16;
            if (mouseX >= graphBtnX && mouseX < graphBtnX + 14 && mouseY >= btnY && mouseY < btnY + 14) {
                if (!currentRecipes.isEmpty()) {
                    RgvRecipe r = currentRecipes.get(currentRecipeIndex);
                    RgvInventory inv = RgvPlatform.get() != null ? RgvPlatform.get().getPlayerInventory() : null;
                    RgvCraftGraphTab newTab = craftGraph.addTabForRecipe(r, 1, recipeManager, inv);
                    setViewMode(ViewMode.GRAPH);
                    if (newTab != null) {
                        centerGraph(newTab, width - 12, height - 52);
                    }
                }
                return true;
            }
        }

        // Widgets
        for (RgvWidget widget : activeWidgets) {
            if (widget.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }

        if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + height) {
            if (System.currentTimeMillis() - openTime > 250) {
                close();
            }
            return true;
        }
        return true;
    }

    private boolean handleGraphModeClick(int mouseX, int mouseY, int button) {
        RgvCraftGraphTab activeTab = craftGraph.getActiveTab();


        // Tab selection and close using cached rendered hitboxes if available
        if (!renderedTabHitboxes.isEmpty()) {
            for (TabHitbox hit : renderedTabHitboxes) {
                if (mouseX >= hit.closeX && mouseX < hit.closeX + hit.closeW
                        && mouseY >= hit.closeY && mouseY < hit.closeY + hit.closeH) {
                    craftGraph.closeTab(hit.index);
                    lastActiveTabIndex = -1;
                    RgvCraftGraphTab active = craftGraph.getActiveTab();
                    if (active != null) {
                        centerGraph(active, width - 12, height - 52);
                    }
                    return true;
                }

                if (mouseX >= hit.x && mouseX < hit.x + hit.w
                        && mouseY >= hit.y && mouseY < hit.y + hit.h) {
                    craftGraph.setActiveTabIndex(hit.index);
                    RgvCraftGraphTab active = craftGraph.getActiveTab();
                    if (active != null) {
                        centerGraph(active, width - 12, height - 52);
                    }
                    return true;
                }
            }

            if (renderedNewTabBtnX >= 0 && mouseX >= renderedNewTabBtnX && mouseX < renderedNewTabBtnX + 18
                    && mouseY >= renderedNewTabBtnY && mouseY < renderedNewTabBtnY + 18) {
                RgvCraftGraphTab emptyTab = craftGraph.createNewEmptyTab();
                lastActiveTabIndex = -1;
                if (emptyTab != null) {
                    centerGraph(emptyTab, width - 12, height - 52);
                }
                return true;
            }
        } else {
            // Fallback calculation using platform font metrics
            int tabX = x + 6;
            int tabY = y + 25;
            int tabH = 18;
            List<RgvCraftGraphTab> tabs = craftGraph.getTabs();
            RgvPlatform platform = RgvPlatform.get();

            for (int i = 0; i < tabs.size(); i++) {
                RgvCraftGraphTab tab = tabs.get(i);
                int titleW = platform != null ? platform.getTextWidth(tab.getTitle()) : 40;
                int tabW = 3 + 16 + 5 + titleW + 6 + 8 + 4;

                int closeTabX = tabX + 24 + titleW + 6;
                int closeBtnX = closeTabX - 3;
                int closeBtnY = tabY + 1;
                int closeBtnW = 14;
                int closeBtnH = tabH - 2;

                if (mouseX >= closeBtnX && mouseX < closeBtnX + closeBtnW
                        && mouseY >= closeBtnY && mouseY < closeBtnY + closeBtnH) {
                    craftGraph.closeTab(i);
                    lastActiveTabIndex = -1;
                    RgvCraftGraphTab active = craftGraph.getActiveTab();
                    if (active != null) {
                        centerGraph(active, width - 12, height - 52);
                    }
                    return true;
                }

                if (mouseX >= tabX && mouseX < tabX + tabW && mouseY >= tabY && mouseY < tabY + tabH) {
                    craftGraph.setActiveTabIndex(i);
                    lastActiveTabIndex = -1;
                    RgvCraftGraphTab active = craftGraph.getActiveTab();
                    if (active != null) {
                        centerGraph(active, width - 12, height - 52);
                    }
                    return true;
                }

                tabX += tabW + 3;
                if (tabX > x + width - 35) break;
            }

            // [+] New Tab button
            int newTabBtnX = tabX;
            if (mouseX >= newTabBtnX && mouseX < newTabBtnX + 18 && mouseY >= tabY && mouseY < tabY + tabH) {
                RgvCraftGraphTab emptyTab = craftGraph.createNewEmptyTab();
                lastActiveTabIndex = -1;
                if (emptyTab != null) {
                    centerGraph(emptyTab, width - 12, height - 52);
                }
                return true;
            }
        }

        if (activeTab == null) return true;

        if (activeTab.isSelectingItem()) {
            RgvPlatform platform = RgvPlatform.get();
            if (platform != null) {
                List<RgvStack> items = platform.getAllKnownStacks();
                int cols = Math.max(1, (width - 24) / 18);
                int rows = Math.max(1, (height - 80) / 18);
                int pageSize = cols * rows;
                int startX = x + 10;
                int startY = y + 60;
                int startIdx = itemPickerPage * pageSize;

                for (int i = 0; i < pageSize && startIdx + i < items.size(); i++) {
                    int col = i % cols;
                    int row = i / cols;
                    int slotX = startX + col * 18;
                    int slotY = startY + row * 18;

                    if (mouseX >= slotX && mouseX < slotX + 18 && mouseY >= slotY && mouseY < slotY + 18) {
                        RgvStack selected = items.get(startIdx + i);
                        if (platform.isRecipeViewerPresent()) {
                            activeTab.setTargetStack(selected);
                            activeTab.setSelectingItem(false);
                            activeTab.setSelectingRecipe(true);
                            craftGraph.setPendingTarget(activeTab, null, selected);
                            if (platform.openExternalRecipeViewer(selected, false)) {
                                close();
                                return true;
                            }
                            craftGraph.clearPendingTarget();
                        }
                        activeTab.selectItem(selected, recipeManager);
                        if (activeTab.isSelectingRecipe()) {
                            openRecipePickerForNode(activeTab, null, activeTab.getCandidateRecipes());
                        } else {
                            centerGraph(activeTab, width - 12, height - 52);
                        }
                        return true;
                    }
                }
            }
            return true;
        }

        if (activeTab.isSelectingRecipe()) {
            RgvPlatform platform = RgvPlatform.get();
            if (platform != null && platform.isRecipeViewerPresent()) {
                craftGraph.setPendingTarget(activeTab, null, activeTab.getTargetStack());
                if (platform.openExternalRecipeViewer(activeTab.getTargetStack(), false)) {
                    close();
                    return true;
                }
                craftGraph.clearPendingTarget();
            }
            List<RgvRecipe> recipes = activeTab.getCandidateRecipes();
            openRecipePickerForNode(activeTab, null, recipes);
            return true;
        }

        // Target Amount controls [-] and [+]
        int controlY = y + 48;
        int minusX = x + 72;
        int plusX = x + 88;
        if (mouseX >= minusX && mouseX < minusX + 12 && mouseY >= controlY + 2 && mouseY < controlY + 14) {
            activeTab.decrementAmount(recipeManager);
            craftGraph.save();
            return true;
        }
        if (mouseX >= plusX && mouseX < plusX + 12 && mouseY >= controlY + 2 && mouseY < controlY + 14) {
            activeTab.incrementAmount(recipeManager);
            craftGraph.save();
            return true;
        }

        if (hoveredGraphNode != null) {
            RgvPlatform platform = RgvPlatform.get();
            if (platform != null && platform.isRecipeViewerPresent()) {
                craftGraph.setPendingTarget(activeTab, hoveredGraphNode, hoveredGraphNode.getStack());
                if (platform.openExternalRecipeViewer(hoveredGraphNode.getStack(), false)) {
                    close();
                    return true;
                }
                craftGraph.clearPendingTarget();
            }
            List<RgvRecipe> recipes = recipeManager.getRecipesFor(hoveredGraphNode.getStack());
            if (!recipes.isEmpty()) {
                openRecipePickerForNode(activeTab, hoveredGraphNode, recipes);
                return true;
            }
        }

        // Viewport background click starts pan drag
        int viewportX = x + 6;
        int viewportY = y + 46;
        int viewportW = width - 12;
        int viewportH = height - 52;
        if (mouseX >= viewportX && mouseX < viewportX + viewportW && mouseY >= viewportY && mouseY < viewportY + viewportH) {
            isPanning = true;
            panDragStartX = mouseX;
            panDragStartY = mouseY;
            panInitialX = panX;
            panInitialY = panY;
            return true;
        }

        return true;
    }

    public void mouseReleased(int mouseX, int mouseY, int state) {
        isPanning = false;
    }

    public boolean mouseScrolled(int scrollDelta) {
        if (!open) return false;
        if (viewMode == ViewMode.RECIPES && currentRecipes.size() > 1) {
            if (scrollDelta > 0) {
                if (currentRecipeIndex > 0) currentRecipeIndex--;
                else currentRecipeIndex = currentRecipes.size() - 1;
                rebuildActiveLayout();
                return true;
            } else if (scrollDelta < 0) {
                if (currentRecipeIndex < currentRecipes.size() - 1) currentRecipeIndex++;
                else currentRecipeIndex = 0;
                rebuildActiveLayout();
                return true;
            }
        }
        if (viewMode == ViewMode.GRAPH) {
            if (scrollDelta != 0) {
                panY += (scrollDelta > 0 ? 20 : -20);
                RgvCraftGraphTab activeTab = craftGraph.getActiveTab();
                if (activeTab != null) {
                    clampPan(activeTab, width - 12, height - 52);
                }
                return true;
            }
        }
        return false;
    }

    public boolean keyPressed(int keyCode, char typedChar) {
        if (!open) return false;
        if (isSelectingForGraph) {
            if (keyCode == 1) { // Escape cancels recipe selection and returns to graph
                closePicker();
                setViewMode(ViewMode.GRAPH);
                if (graphSelectingTab != null) {
                    centerGraph(graphSelectingTab, width - 12, height - 52);
                }
                return true;
            }
        }
        if (keyCode == 1 || (ru.nexsqaud.rgv.core.platform.RgvPlatform.get() != null && ru.nexsqaud.rgv.core.platform.RgvPlatform.get().isInventoryKey(keyCode))) {
            close();
            return true;
        }

        // Left (203) / Right (205) arrow navigation
        if (viewMode == ViewMode.RECIPES && !currentRecipes.isEmpty()) {
            if (keyCode == 203) {
                currentRecipeIndex = (currentRecipeIndex - 1 + currentRecipes.size()) % currentRecipes.size();
                rebuildActiveLayout();
                return true;
            } else if (keyCode == 205) {
                currentRecipeIndex = (currentRecipeIndex + 1) % currentRecipes.size();
                rebuildActiveLayout();
                return true;
            }
        } else if (viewMode == ViewMode.GRAPH && craftGraph.getTabs().size() > 1) {
            if (keyCode == 203) {
                int newIdx = (craftGraph.getActiveTabIndex() - 1 + craftGraph.getTabs().size()) % craftGraph.getTabs().size();
                craftGraph.setActiveTabIndex(newIdx);
                return true;
            } else if (keyCode == 205) {
                int newIdx = (craftGraph.getActiveTabIndex() + 1) % craftGraph.getTabs().size();
                craftGraph.setActiveTabIndex(newIdx);
                return true;
            }
        }

        // Hotkeys R (19), U (22), A (30) on hovered items inside RgvRecipeScreen
        RgvStack hovered = null;
        if (viewMode == ViewMode.RECIPES) {
            if (hoveredWidget instanceof SlotWidget) {
                SlotWidget sw = (SlotWidget) hoveredWidget;
                if (sw.getIngredient() != null && !sw.getIngredient().getRgvStacks().isEmpty()) {
                    hovered = sw.getIngredient().getRgvStacks().get(0);
                }
            }
        } else if (viewMode == ViewMode.GRAPH) {
            if (hoveredGraphNode != null && hoveredGraphNode.getStack() != null) {
                hovered = hoveredGraphNode.getStack();
            } else if (hoveredLeafStack != null && !hoveredLeafStack.isEmpty()) {
                hovered = hoveredLeafStack;
            } else if (hoveredLeftoverStack != null && !hoveredLeftoverStack.isEmpty()) {
                hovered = hoveredLeftoverStack;
            }
        }

        if (hovered != null && !hovered.isEmpty()) {
            if (keyCode == 19) { // 'R'
                if (lookupHandler != null) {
                    lookupHandler.openRecipesFor(hovered);
                } else {
                    List<RgvRecipe> recipes = recipeManager.getRecipesFor(hovered);
                    if (!recipes.isEmpty()) {
                        openWithRecipes(recipes, "Recipes for: " + hovered.getDisplayName());
                    }
                }
                return true;
            } else if (keyCode == 22) { // 'U'
                if (lookupHandler != null) {
                    lookupHandler.openUsesFor(hovered);
                } else {
                    List<RgvRecipe> usages = recipeManager.getUsesFor(hovered);
                    if (!usages.isEmpty()) {
                        openWithRecipes(usages, "Usages of: " + hovered.getDisplayName());
                    }
                }
                return true;
            } else if (keyCode == 30) { // 'A'
                if (config != null) {
                    config.toggleBookmark(hovered);
                    return true;
                }
            }
        }

        return false;
    }

    public int getCurrentRecipeIndex() {
        return currentRecipeIndex;
    }

    public List<RgvRecipe> getCurrentRecipes() {
        return Collections.unmodifiableList(currentRecipes);
    }

    public int getPrevBtnX() { return prevBtnX; }
    public int getPrevBtnY() { return prevBtnY; }
    public int getPrevBtnW() { return prevBtnW; }
    public int getPrevBtnH() { return prevBtnH; }

    public int getNextBtnX() { return nextBtnX; }
    public int getNextBtnY() { return nextBtnY; }
    public int getNextBtnW() { return nextBtnW; }
    public int getNextBtnH() { return nextBtnH; }

    public static void drawVanillaButton(RgvDrawContext context, int x, int y, int width, int height, boolean hovered, boolean enabled) {
        int state = !enabled ? 0 : (hovered ? 2 : 1);
        int vBase = 46 + state * 20;
        int w1 = width / 2;
        int w2 = width - w1;
        int h1 = height / 2;
        int h2 = height - h1;

        context.drawTexture("minecraft:textures/gui/widgets.png", x, y, 0, vBase, w1, h1, 256, 256);
        context.drawTexture("minecraft:textures/gui/widgets.png", x + w1, y, 200 - w2, vBase, w2, h1, 256, 256);
        context.drawTexture("minecraft:textures/gui/widgets.png", x, y + h1, 0, vBase + 20 - h2, w1, h2, 256, 256);
        context.drawTexture("minecraft:textures/gui/widgets.png", x + w1, y + h1, 200 - w2, vBase + 20 - h2, w2, h2, 256, 256);
    }

    public static void drawGraphIcon(RgvDrawContext context, int cx, int cy, boolean hover) {
        int rootColor = hover ? 0xFFFFFFFF : 0xFF55FFFF;
        int leafColor = hover ? 0xFF88FF88 : 0xFF55FF55;
        int lineColor = hover ? 0xFFFFFFFF : 0xFFAAAAAA;

        // Top root node (3x3)
        context.drawRect(cx - 1, cy - 4, 3, 3, rootColor);
        // Bottom-left leaf node (3x3)
        context.drawRect(cx - 5, cy + 2, 3, 3, leafColor);
        // Bottom-right leaf node (3x3)
        context.drawRect(cx + 3, cy + 2, 3, 3, leafColor);

        // Connecting lines (diagonal pixels)
        context.drawRect(cx - 2, cy - 1, 1, 1, lineColor);
        context.drawRect(cx - 3, cy, 1, 1, lineColor);
        context.drawRect(cx - 4, cy + 1, 1, 1, lineColor);

        context.drawRect(cx + 2, cy - 1, 1, 1, lineColor);
        context.drawRect(cx + 3, cy, 1, 1, lineColor);
        context.drawRect(cx + 4, cy + 1, 1, 1, lineColor);
    }
}
