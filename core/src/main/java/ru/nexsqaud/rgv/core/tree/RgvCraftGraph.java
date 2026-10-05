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
        return tab;
    }

    public RgvCraftGraphTab createNewEmptyTab() {
        RgvCraftGraphTab tab = RgvCraftGraphTab.createEmpty("Plan " + (tabs.size() + 1));
        tabs.add(tab);
        this.activeTabIndex = tabs.size() - 1;
        return tab;
    }

    public void closeTab(int index) {
        if (index >= 0 && index < tabs.size()) {
            tabs.remove(index);
            if (activeTabIndex >= tabs.size()) {
                activeTabIndex = Math.max(0, tabs.size() - 1);
            }
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
}
