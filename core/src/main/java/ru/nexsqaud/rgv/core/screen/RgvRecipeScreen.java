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

    private final List<RgvWidget> activeWidgets = new ArrayList<>();
    private RgvWidget hoveredWidget = null;
    private RgvStack hoveredLeafStack = null;
    private long hoveredLeafRequired = 0;
    private long hoveredLeafAvailable = 0;
    private RgvGraphNode hoveredGraphNode = null;
    private boolean open = false;

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
        this.open = true;
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
            return;
        }

        RgvRecipe recipe = currentRecipes.get(currentRecipeIndex);
        int recipeW = recipe.getDisplayWidth();
        int recipeH = recipe.getDisplayHeight();

        this.width = Math.max(200, recipeW + 32);
        this.height = Math.max(140, recipeH + 54);

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
                activeWidgets.add(widget);
            }
        }
    }

    public void render(RgvDrawContext context, int mouseX, int mouseY, float delta) {
        if (!open) return;
        hoveredWidget = null;
        hoveredLeafStack = null;
        hoveredGraphNode = null;

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

            String pageText = (currentRecipeIndex + 1) + " / " + currentRecipes.size();
            int pageX = x + width - context.getTextWidth(pageText) - 30;
            context.drawText(pageText, pageX, y + 26, 0x444444, false);

            int prevX = pageX - 14;
            int nextX = x + width - 20;
            boolean prevHover = mouseX >= prevX && mouseX < prevX + 10 && mouseY >= y + 24 && mouseY < y + 36;
            boolean nextHover = mouseX >= nextX && mouseX < nextX + 10 && mouseY >= y + 24 && mouseY < y + 36;

            context.drawText("<", prevX, y + 25, prevHover ? 0xFFFFFF : 0x333333, true);
            context.drawText(">", nextX, y + 25, nextHover ? 0xFFFFFF : 0x333333, true);
        } else {
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
                // [✓ Use Craft] button
                int useW = 86;
                int useX = x + width - useW - 8;
                boolean useHover = mouseX >= useX && mouseX < useX + useW && mouseY >= btnY && mouseY < btnY + 14;
                context.drawRect(useX, btnY, useW, 14, useHover ? 0xFF2A882A : 0xFF1E661E);
                context.drawText("\u2713 Use Craft", useX + 10, btnY + 3, 0xFFFFFF, true);

                // [< Back] button
                int backW = 50;
                int backX = useX - backW - 4;
                boolean backHover = mouseX >= backX && mouseX < backX + backW && mouseY >= btnY && mouseY < btnY + 14;
                context.drawRect(backX, btnY, backW, 14, backHover ? 0xFF666666 : 0xFF444444);
                context.drawText("< Back", backX + 8, btnY + 3, 0xCCCCCC, false);

                // [Reset Raw] button (if node has assigned recipe)
                if (graphSelectingNode != null && graphSelectingNode.hasAssignedRecipe()) {
                    int resetW = 76;
                    int resetX = backX - resetW - 4;
                    boolean resetHover = mouseX >= resetX && mouseX < resetX + resetW && mouseY >= btnY && mouseY < btnY + 14;
                    context.drawRect(resetX, btnY, resetW, 14, resetHover ? 0xFFAA4444 : 0xFF773333);
                    context.drawText("Reset Raw", resetX + 8, btnY + 3, 0xFFFFFF, true);
                }
            } else {
                RgvRecipe current = currentRecipes.get(currentRecipeIndex);
                RgvPlatform platform = RgvPlatform.get();
                TransferStatus status = platform != null ? platform.getTransferStatus(current) : TransferStatus.NO_SUITABLE_CONTAINER;
                boolean canTransfer = (status == TransferStatus.AVAILABLE);

                int btnX = x + width - 22;
                boolean hover = mouseX >= btnX && mouseX < btnX + 14 && mouseY >= btnY && mouseY < btnY + 14;

                context.drawRect(btnX, btnY, 14, 14, canTransfer ? (hover ? 0xFF888888 : 0xFF555555) : 0xFF333333);
                context.drawText("+", btnX + 4, btnY + 3, canTransfer ? 0x55FF55 : 0x777777, true);

                int graphBtnX = btnX - 58;
                int graphBtnY = btnY;
                boolean gHover = mouseX >= graphBtnX && mouseX < graphBtnX + 54 && mouseY >= graphBtnY && mouseY < graphBtnY + 14;
                context.drawRect(graphBtnX, graphBtnY, 54, 14, gHover ? 0xFF55AA55 : 0xFF444444);
                context.drawText("+ Graph", graphBtnX + 4, graphBtnY + 3, 0xFFFFFF, false);
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
            boolean closeHover = mouseX >= closeTabX - 2 && mouseX <= closeTabX + 9 && mouseY >= tabY + 3 && mouseY <= tabY + 14;
            // Contrast compliant: dark on active light tab, bright on inactive dark tab
            context.drawText("x", closeTabX, tabY + 5, closeHover ? 0xFFFF4444 : (isActive ? 0x444444 : 0xCCCCCC), false);

            tabX += tabW + 3;
            if (tabX > x + width - 35) break;
        }

        int newTabBtnX = tabX;
        boolean newHover = mouseX >= newTabBtnX && mouseX < newTabBtnX + 18 && mouseY >= tabY && mouseY < tabY + tabH;
        context.drawRect(newTabBtnX, tabY, 18, tabH, newHover ? 0xFF55AA55 : 0xFF444444);
        context.drawText("+", newTabBtnX + 6, tabY + 5, 0xFFFFFF, false);

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

        renderTopRightLeafRequirements(context, activeTab, inv, mouseX, mouseY, delta);
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

    private void renderTopRightLeafRequirements(RgvDrawContext context, RgvCraftGraphTab tab, RgvInventory inv, int mouseX, int mouseY, float delta) {
        Map<RgvStack, Long> leaves = tab.getLeafNodeRequirements();
        if (leaves.isEmpty()) return;

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
                int backX = useX - backW - 4;
                if (mouseX >= backX && mouseX < backX + backW && mouseY >= btnY && mouseY < btnY + 14) {
                    List<String> tip = new ArrayList<>();
                    tip.add("\u00a77Back to Graph");
                    tip.add("\u00a78Cancel recipe selection");
                    context.drawTooltip(tip, mouseX, mouseY);
                    return;
                }

                if (graphSelectingNode != null && graphSelectingNode.hasAssignedRecipe()) {
                    int resetW = 76;
                    int resetX = backX - resetW - 4;
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

                int graphBtnX = btnX - 58;
                if (mouseX >= graphBtnX && mouseX < graphBtnX + 54 && mouseY >= btnY && mouseY < btnY + 14) {
                    List<String> tip = new ArrayList<>();
                    tip.add("\u00a7bAdd to Graph (+ Graph)");
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
        if (!currentRecipes.isEmpty()) {
            int pageX = x + width - 30;
            int prevX = pageX - 14;
            int nextX = x + width - 20;

            if (mouseX >= prevX && mouseX < prevX + 10 && mouseY >= y + 24 && mouseY < y + 36) {
                if (currentRecipeIndex > 0) currentRecipeIndex--;
                else currentRecipeIndex = currentRecipes.size() - 1;
                rebuildActiveLayout();
                return true;
            }
            if (mouseX >= nextX && mouseX < nextX + 10 && mouseY >= y + 24 && mouseY < y + 36) {
                if (currentRecipeIndex < currentRecipes.size() - 1) currentRecipeIndex++;
                else currentRecipeIndex = 0;
                rebuildActiveLayout();
                return true;
            }
        }

        // Bottom Action Buttons
        int btnY = y + height - 22;
        if (isSelectingForGraph) {
            int useW = 86;
            int useX = x + width - useW - 8;
            if (mouseX >= useX && mouseX < useX + useW && mouseY >= btnY && mouseY < btnY + 14) {
                if (!currentRecipes.isEmpty()) {
                    RgvRecipe chosen = currentRecipes.get(currentRecipeIndex);
                    if (graphSelectingNode != null && graphSelectingTab != null) {
                        graphSelectingTab.setRecipeForIngredient(graphSelectingNode.getStack(), chosen, recipeManager);
                    } else if (graphSelectingTab != null && graphSelectingTab.isSelectingRecipe()) {
                        graphSelectingTab.assignRecipe(chosen, graphSelectingTab.getTargetStack(), recipeManager);
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
            int backX = useX - backW - 4;
            if (mouseX >= backX && mouseX < backX + backW && mouseY >= btnY && mouseY < btnY + 14) {
                closePicker();
                setViewMode(ViewMode.GRAPH);
                if (graphSelectingTab != null) {
                    centerGraph(graphSelectingTab, width - 12, height - 52);
                }
                return true;
            }

            if (graphSelectingNode != null && graphSelectingNode.hasAssignedRecipe()) {
                int resetW = 76;
                int resetX = backX - resetW - 4;
                if (mouseX >= resetX && mouseX < resetX + resetW && mouseY >= btnY && mouseY < btnY + 14) {
                    if (graphSelectingTab != null) {
                        graphSelectingTab.removeRecipeForIngredient(graphSelectingNode.getStack(), recipeManager);
                    }
                    closePicker();
                    setViewMode(ViewMode.GRAPH);
                    if (graphSelectingTab != null) {
                        centerGraph(graphSelectingTab, width - 12, height - 52);
                    }
                    return true;
                }
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

            // [+ Graph] button
            int graphBtnX = btnX - 58;
            if (mouseX >= graphBtnX && mouseX < graphBtnX + 54 && mouseY >= btnY && mouseY < btnY + 14) {
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
            close();
            return true;
        }
        return true;
    }

    private boolean handleGraphModeClick(int mouseX, int mouseY, int button) {
        RgvCraftGraphTab activeTab = craftGraph.getActiveTab();


        // Tab selection and close
        int tabX = x + 6;
        int tabY = y + 25;
        int tabH = 18;
        List<RgvCraftGraphTab> tabs = craftGraph.getTabs();

        for (int i = 0; i < tabs.size(); i++) {
            RgvCraftGraphTab tab = tabs.get(i);
            int titleW = RgvPlatform.get() != null ? (tab.getTitle().length() * 6) : 40;
            int tabW = 3 + 16 + 5 + titleW + 6 + 8 + 4;

            int closeTabX = tabX + 24 + titleW + 6;
            if (mouseX >= closeTabX - 2 && mouseX <= closeTabX + 9 && mouseY >= tabY + 3 && mouseY <= tabY + 14) {
                craftGraph.closeTab(i);
                return true;
            }

            if (mouseX >= tabX && mouseX < tabX + tabW && mouseY >= tabY && mouseY < tabY + tabH) {
                craftGraph.setActiveTabIndex(i);
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
            if (emptyTab != null) {
                centerGraph(emptyTab, width - 12, height - 52);
            }
            return true;
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
                        activeTab.selectItem(items.get(startIdx + i), recipeManager);
                        if (activeTab.isSelectingRecipe()) {
                            if (platform != null && platform.isRecipeViewerPresent()) {
                                craftGraph.setPendingTarget(activeTab, null);
                                close();
                                platform.openExternalRecipeViewer(activeTab.getTargetStack(), false);
                            } else {
                                openRecipePickerForNode(activeTab, null, activeTab.getCandidateRecipes());
                            }
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
            List<RgvRecipe> recipes = activeTab.getCandidateRecipes();
            RgvPlatform platform = RgvPlatform.get();
            if (platform != null && platform.isRecipeViewerPresent()) {
                craftGraph.setPendingTarget(activeTab, null);
                close();
                platform.openExternalRecipeViewer(activeTab.getTargetStack(), false);
            } else {
                openRecipePickerForNode(activeTab, null, recipes);
            }
            return true;
        }

        // Target Amount controls [-] and [+]
        int controlY = y + 48;
        int minusX = x + 72;
        int plusX = x + 88;
        if (mouseX >= minusX && mouseX < minusX + 12 && mouseY >= controlY + 2 && mouseY < controlY + 14) {
            activeTab.decrementAmount(recipeManager);
            return true;
        }
        if (mouseX >= plusX && mouseX < plusX + 12 && mouseY >= controlY + 2 && mouseY < controlY + 14) {
            activeTab.incrementAmount(recipeManager);
            return true;
        }

        if (hoveredGraphNode != null) {
            List<RgvRecipe> recipes = recipeManager.getRecipesFor(hoveredGraphNode.getStack());
            if (!recipes.isEmpty()) {
                RgvPlatform platform = RgvPlatform.get();
                if (platform != null && platform.isRecipeViewerPresent()) {
                    craftGraph.setPendingTarget(activeTab, hoveredGraphNode);
                    close();
                    platform.openExternalRecipeViewer(hoveredGraphNode.getStack(), false);
                } else {
                    openRecipePickerForNode(activeTab, hoveredGraphNode, recipes);
                }
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
}
