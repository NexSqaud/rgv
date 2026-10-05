package ru.nexsqaud.rgv.api;

/**
 * Entrypoint interface for mods providing custom recipes, categories,
 * workstations, or screen handlers to RGV.
 */
public interface RgvPlugin {

    void register(RgvRegistry registry);
}
