package ru.nexsqaud.rgv.api;

/**
 * Allows screens to provide the RgvStack currently under the mouse cursor
 * for recipe lookup ('R') and usage lookup ('U').
 */
public interface RgvStackProvider {

    boolean appliesTo(Object screen);

    RgvStack getStackAt(Object screen, int mouseX, int mouseY);
}
