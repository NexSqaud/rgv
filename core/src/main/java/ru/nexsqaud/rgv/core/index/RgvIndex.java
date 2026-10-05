package ru.nexsqaud.rgv.core.index;

import ru.nexsqaud.rgv.api.RgvStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Item index and pagination manager for the RGV item browser sidebar.
 */
public class RgvIndex {

    private final List<RgvStack> allItems = new ArrayList<>();
    private final List<RgvStack> filteredItems = new ArrayList<>();

    private String currentQuery = "";
    private RgvSearchFilter activeFilter = new RgvSearchFilter("");
    private boolean craftableOnly = false;
    private Set<RgvStack> cachedCraftableStacks = Collections.emptySet();

    private int columns = 9;
    private int rows = 12;
    private int currentPage = 0;

    public void setAllItems(List<RgvStack> items) {
        allItems.clear();
        if (items != null) {
            allItems.addAll(items);
        }
        rebuildFilter();
    }

    public List<RgvStack> getAllItems() {
        return Collections.unmodifiableList(allItems);
    }

    public void setCraftableFilter(boolean craftableOnly, Set<RgvStack> craftableStacks) {
        this.craftableOnly = craftableOnly;
        this.cachedCraftableStacks = craftableStacks != null ? craftableStacks : Collections.emptySet();
        rebuildFilter();
    }

    public boolean isCraftableOnly() {
        return craftableOnly;
    }

    public void setSearchQuery(String query) {
        this.currentQuery = query != null ? query : "";
        this.activeFilter = new RgvSearchFilter(this.currentQuery);
        rebuildFilter();
    }

    public String getSearchQuery() {
        return currentQuery;
    }

    public void rebuildFilter() {
        filteredItems.clear();
        for (RgvStack stack : allItems) {
            if (activeFilter.test(stack)) {
                if (craftableOnly) {
                    if (isStackCraftable(stack)) {
                        filteredItems.add(stack);
                    }
                } else {
                    filteredItems.add(stack);
                }
            }
        }
        clampPage();
    }

    private boolean isStackCraftable(RgvStack stack) {
        for (RgvStack craftable : cachedCraftableStacks) {
            if (craftable.matches(stack)) {
                return true;
            }
        }
        return false;
    }

    public void updateLayout(int availableWidth, int availableHeight) {
        // Each slot is 18x18 pixels
        int newCols = Math.max(1, availableWidth / 18);
        int newRows = Math.max(1, availableHeight / 18);

        if (newCols != this.columns || newRows != this.rows) {
            this.columns = newCols;
            this.rows = newRows;
            clampPage();
        }
    }

    public int getColumns() {
        return columns;
    }

    public int getRows() {
        return rows;
    }

    public int getPageSize() {
        return Math.max(1, columns * rows);
    }

    public int getTotalPages() {
        int size = filteredItems.size();
        int pageSize = getPageSize();
        return Math.max(1, (int) Math.ceil((double) size / (double) pageSize));
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setPage(int page) {
        this.currentPage = page;
        clampPage();
    }

    public void nextPage() {
        if (currentPage < getTotalPages() - 1) {
            currentPage++;
        } else {
            currentPage = 0; // Wrap around
        }
    }

    public void prevPage() {
        if (currentPage > 0) {
            currentPage--;
        } else {
            currentPage = getTotalPages() - 1; // Wrap around
        }
    }

    private void clampPage() {
        int max = getTotalPages() - 1;
        if (currentPage > max) currentPage = max;
        if (currentPage < 0) currentPage = 0;
    }

    public List<RgvStack> getItemsForCurrentPage() {
        int pageSize = getPageSize();
        int startIndex = currentPage * pageSize;
        if (startIndex >= filteredItems.size()) {
            return Collections.emptyList();
        }
        int endIndex = Math.min(startIndex + pageSize, filteredItems.size());
        return filteredItems.subList(startIndex, endIndex);
    }

    public int getFilteredCount() {
        return filteredItems.size();
    }
}
