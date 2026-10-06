package ru.nexsqaud.rgv.core.tree;

import ru.nexsqaud.rgv.api.RgvInventory;
import ru.nexsqaud.rgv.api.RgvRecipe;
import ru.nexsqaud.rgv.api.RgvStack;
import ru.nexsqaud.rgv.core.recipe.RgvRecipeManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages the collection of craft plan tabs in RGV's Graph Planner.
 */
public class RgvCraftGraph {

    private final List<RgvCraftGraphTab> tabs = new ArrayList<>();
    private int activeTabIndex = 0;

    public List<RgvCraftGraphTab> getTabs() {
        return Collections.unmodifiableList(tabs);
    }

    public int getActiveTabIndex() {
        return activeTabIndex;
    }

    public void setActiveTabIndex(int index) {
        if (index >= 0 && index < tabs.size()) {
            this.activeTabIndex = index;
            save();
        }
    }

    public RgvCraftGraphTab getActiveTab() {
        if (tabs.isEmpty() || activeTabIndex < 0 || activeTabIndex >= tabs.size()) {
            return null;
        }
        return tabs.get(activeTabIndex);
    }

    public RgvCraftGraphTab addTabForRecipe(RgvRecipe recipe, int amount, RgvRecipeManager manager, RgvInventory inventory) {
        if (recipe == null) return null;
        RgvStack output = !recipe.getOutputs().isEmpty() ? recipe.getOutputs().get(0) : RgvStack.empty();
        String title = output.getDisplayName();

        RgvCraftGraphTab tab = new RgvCraftGraphTab(null, title, output, recipe, amount);
        tab.rebuildGraph(manager);
        tabs.add(tab);
        this.activeTabIndex = tabs.size() - 1;
        save();
        return tab;
    }

    public RgvCraftGraphTab createNewEmptyTab() {
        RgvCraftGraphTab tab = RgvCraftGraphTab.createEmpty("Plan " + (tabs.size() + 1));
        tabs.add(tab);
        this.activeTabIndex = tabs.size() - 1;
        save();
        return tab;
    }

    public void closeTab(int index) {
        if (index >= 0 && index < tabs.size()) {
            tabs.remove(index);
            if (activeTabIndex > index) {
                activeTabIndex--;
            } else if (activeTabIndex >= tabs.size()) {
                activeTabIndex = Math.max(0, tabs.size() - 1);
            }
            save();
        }
    }

    public void recalculateAll(RgvRecipeManager manager) {
        for (RgvCraftGraphTab tab : tabs) {
            tab.rebuildGraph(manager);
        }
    }

    public boolean isEmpty() {
        return tabs.isEmpty();
    }

    private RgvCraftGraphTab pendingTargetTab = null;
    private RgvStack pendingTargetStack = null;
    private boolean pendingTargetIsRoot = false;
    private long pendingTargetTimestamp = 0;

    public void setPendingTarget(RgvCraftGraphTab tab, RgvGraphNode node) {
        setPendingTarget(tab, node, node != null ? node.getStack() : (tab != null ? tab.getTargetStack() : null));
    }

    public void setPendingTarget(RgvCraftGraphTab tab, RgvGraphNode node, RgvStack stack) {
        this.pendingTargetTab = tab;
        this.pendingTargetStack = stack;
        this.pendingTargetIsRoot = (node == null || node.getLevel() == 0);
        this.pendingTargetTimestamp = System.currentTimeMillis();
    }

    public boolean hasPendingTarget() {
        if (pendingTargetTab == null || pendingTargetStack == null) return false;
        if (System.currentTimeMillis() - pendingTargetTimestamp > 120_000) {
            clearPendingTarget();
            return false;
        }
        return true;
    }

    public RgvStack getPendingTargetStack() {
        return pendingTargetStack;
    }

    public RgvCraftGraphTab getPendingTargetTab() {
        return pendingTargetTab;
    }

    public boolean applyPendingTarget(RgvRecipe recipe, RgvRecipeManager manager) {
        if (!hasPendingTarget() || recipe == null) return false;
        if (pendingTargetIsRoot || (pendingTargetTab.isSelectingRecipe() && pendingTargetTab.getTargetStack() != null && pendingTargetTab.getTargetStack().matches(pendingTargetStack))) {
            pendingTargetTab.assignRecipe(recipe, pendingTargetStack, manager);
        } else {
            pendingTargetTab.setRecipeForIngredient(pendingTargetStack, recipe, manager);
        }
        clearPendingTarget();
        save();
        return true;
    }

    public void clearPendingTarget() {
        this.pendingTargetTab = null;
        this.pendingTargetStack = null;
        this.pendingTargetIsRoot = false;
        this.pendingTargetTimestamp = 0;
    }

    public void save() {
        ru.nexsqaud.rgv.core.platform.RgvPlatform platform = ru.nexsqaud.rgv.core.platform.RgvPlatform.get();
        if (platform != null) {
            save(platform.getConfigDirectory());
        }
    }

    public void save(java.io.File configDir) {
        if (configDir == null) return;
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        java.io.File file = new java.io.File(configDir, "rgv_tabs.cfg");
        try (java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(file))) {
            writer.println("# RGV Saved Craft Graph Tabs");
            writer.println("activeTab=" + activeTabIndex);
            for (RgvCraftGraphTab tab : tabs) {
                writer.println("[tab]");
                writer.println("id=" + tab.getId());
                writer.println("title=" + (tab.getTitle() != null ? tab.getTitle() : ""));
                RgvStack target = tab.getTargetStack();
                if (target != null && !target.isEmpty()) {
                    writer.println("targetStack=" + target.getId() + ":" + target.getMeta() + ":" + target.getAmount());
                }
                writer.println("targetAmount=" + tab.getTargetAmount());
                String rId = tab.getSavedRootRecipeId();
                if (rId != null) {
                    writer.println("rootRecipe=" + rId);
                }
                writer.println("isSelectingRecipe=" + tab.isSelectingRecipe());
                for (java.util.Map.Entry<String, String> entry : tab.getSavedAssignedRecipeIds().entrySet()) {
                    writer.println("assignedRecipe=" + entry.getKey() + "=" + entry.getValue());
                }
            }
        } catch (Exception ignored) {
        }
    }

    public void load(java.io.File configDir, RgvRecipeManager recipeManager) {
        if (configDir == null) return;
        java.io.File file = new java.io.File(configDir, "rgv_tabs.cfg");
        if (!file.exists()) return;

        List<RgvCraftGraphTab> loadedTabs = new ArrayList<>();
        int loadedActiveTab = 0;

        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file))) {
            String line;
            String tabId = null;
            String tabTitle = null;
            RgvStack targetStack = null;
            int targetAmount = 1;
            String rootRecipeId = null;
            boolean selectingRecipe = false;
            java.util.Map<String, String> assignedMap = new java.util.LinkedHashMap<>();

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#") || line.isEmpty()) continue;

                if (line.startsWith("activeTab=")) {
                    try {
                        loadedActiveTab = Integer.parseInt(line.substring(10));
                    } catch (Exception ignored) {}
                } else if (line.equals("[tab]")) {
                    if (tabId != null) {
                        RgvCraftGraphTab finishedTab = buildLoadedTab(tabId, tabTitle, targetStack, targetAmount, rootRecipeId, selectingRecipe, assignedMap, recipeManager);
                        if (finishedTab != null) loadedTabs.add(finishedTab);
                    }
                    tabId = null;
                    tabTitle = null;
                    targetStack = null;
                    targetAmount = 1;
                    rootRecipeId = null;
                    selectingRecipe = false;
                    assignedMap = new java.util.LinkedHashMap<>();
                } else if (line.startsWith("id=")) {
                    tabId = line.substring(3);
                } else if (line.startsWith("title=")) {
                    tabTitle = line.substring(6);
                } else if (line.startsWith("targetStack=")) {
                    String[] parts = line.substring(12).split(":");
                    if (parts.length >= 2) {
                        String id = parts[0] + ":" + parts[1];
                        int meta = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
                        int amount = parts.length > 3 ? Integer.parseInt(parts[3]) : 1;
                        targetStack = RgvStack.of(id, meta, amount);
                    }
                } else if (line.startsWith("targetAmount=")) {
                    try {
                        targetAmount = Math.max(1, Integer.parseInt(line.substring(13)));
                    } catch (Exception ignored) {}
                } else if (line.startsWith("rootRecipe=")) {
                    rootRecipeId = line.substring(11);
                } else if (line.startsWith("isSelectingRecipe=")) {
                    selectingRecipe = Boolean.parseBoolean(line.substring(18));
                } else if (line.startsWith("assignedRecipe=")) {
                    String rest = line.substring(15);
                    int eq = rest.indexOf('=');
                    if (eq > 0) {
                        assignedMap.put(rest.substring(0, eq), rest.substring(eq + 1));
                    }
                }
            }
            if (tabId != null) {
                RgvCraftGraphTab finishedTab = buildLoadedTab(tabId, tabTitle, targetStack, targetAmount, rootRecipeId, selectingRecipe, assignedMap, recipeManager);
                if (finishedTab != null) loadedTabs.add(finishedTab);
            }
        } catch (Exception ignored) {
        }

        if (!loadedTabs.isEmpty()) {
            this.tabs.clear();
            this.tabs.addAll(loadedTabs);
            this.activeTabIndex = Math.min(Math.max(0, loadedActiveTab), tabs.size() - 1);
        }
    }

    private RgvCraftGraphTab buildLoadedTab(String id, String title, RgvStack targetStack, int targetAmount,
                                             String rootRecipeId, boolean selectingRecipe,
                                             java.util.Map<String, String> assignedMap, RgvRecipeManager recipeManager) {
        RgvRecipe rootRecipe = null;
        if (recipeManager != null) {
            if (rootRecipeId != null) {
                rootRecipe = recipeManager.getRecipeById(rootRecipeId);
            }
            if (rootRecipe == null && targetStack != null && !targetStack.isEmpty()) {
                List<RgvRecipe> candidates = recipeManager.getRecipesFor(targetStack);
                if (!candidates.isEmpty()) {
                    rootRecipe = candidates.get(0);
                }
            }
        }
        RgvCraftGraphTab tab = new RgvCraftGraphTab(id, title, targetStack, rootRecipe, targetAmount);
        tab.setSelectingRecipe(selectingRecipe);
        tab.setSavedRootRecipeId(rootRecipeId);
        tab.setSavedAssignedRecipeIds(assignedMap);
        if (recipeManager != null) {
            tab.rebuildGraph(recipeManager);
        }
        return tab;
    }
}
