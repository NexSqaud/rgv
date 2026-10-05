package ru.nexsqaud.rgv.core.config;

import ru.nexsqaud.rgv.api.RgvStack;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Handles persistent configuration and saved bookmarks/favorites.
 * Simple, zero-dependency serialization to stay platform-independent.
 */
public class RgvConfig {

    private boolean cheatMode = false;
    private boolean overlayEnabled = true;
    private boolean craftableFilter = false;
    private boolean replaceWithNei = true;
    private final List<RgvStack> bookmarks = new ArrayList<>();

    public boolean isReplaceWithNei() {
        return replaceWithNei;
    }

    public void setReplaceWithNei(boolean replaceWithNei) {
        this.replaceWithNei = replaceWithNei;
    }

    public boolean isCheatMode() {
        return cheatMode;
    }

    public void setCheatMode(boolean cheatMode) {
        this.cheatMode = cheatMode;
    }

    public boolean isOverlayEnabled() {
        return overlayEnabled;
    }

    public void setOverlayEnabled(boolean overlayEnabled) {
        this.overlayEnabled = overlayEnabled;
    }

    public boolean isCraftableFilter() {
        return craftableFilter;
    }

    public void setCraftableFilter(boolean craftableFilter) {
        this.craftableFilter = craftableFilter;
    }

    public List<RgvStack> getBookmarks() {
        return Collections.unmodifiableList(bookmarks);
    }

    public boolean isBookmarked(RgvStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        for (RgvStack b : bookmarks) {
            if (b.matches(stack)) {
                return true;
            }
        }
        return false;
    }

    public void toggleBookmark(RgvStack stack) {
        if (stack == null || stack.isEmpty()) return;
        for (int i = 0; i < bookmarks.size(); i++) {
            if (bookmarks.get(i).matches(stack)) {
                bookmarks.remove(i);
                return;
            }
        }
        bookmarks.add(stack.copyWithAmount(1));
    }

    public void load(File configDir) {
        if (configDir == null) return;
        File file = new File(configDir, "rgv.cfg");
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#") || line.isEmpty()) continue;
                if (line.startsWith("cheatMode=")) {
                    cheatMode = Boolean.parseBoolean(line.substring(10));
                } else if (line.startsWith("overlayEnabled=")) {
                    overlayEnabled = Boolean.parseBoolean(line.substring(15));
                } else if (line.startsWith("craftableFilter=")) {
                    craftableFilter = Boolean.parseBoolean(line.substring(16));
                } else if (line.startsWith("replaceWithNei=")) {
                    replaceWithNei = Boolean.parseBoolean(line.substring(15));
                } else if (line.startsWith("bookmark=")) {
                    String[] parts = line.substring(9).split(":");
                    if (parts.length >= 2) {
                        String id = parts[0] + ":" + parts[1];
                        int meta = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
                        bookmarks.add(RgvStack.of(id, meta, 1));
                    }
                }
            }
        } catch (Exception e) {
            // Ignore corrupted config and proceed with defaults
        }
    }

    public void save(File configDir) {
        if (configDir == null) return;
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        File file = new File(configDir, "rgv.cfg");
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println("# RGV Configuration");
            writer.println("cheatMode=" + cheatMode);
            writer.println("overlayEnabled=" + overlayEnabled);
            writer.println("craftableFilter=" + craftableFilter);
            writer.println("replaceWithNei=" + replaceWithNei);
            for (RgvStack b : bookmarks) {
                writer.println("bookmark=" + b.getId() + ":" + b.getMeta());
            }
        } catch (Exception ignored) {
        }
    }
}
