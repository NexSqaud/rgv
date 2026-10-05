package ru.nexsqaud.rgv.core.platform;

/**
 * Indicates whether a recipe can be transferred into the currently opened container.
 */
public enum TransferStatus {
    AVAILABLE,
    MISSING_INGREDIENTS,
    NO_SUITABLE_CONTAINER
}
