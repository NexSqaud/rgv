package ru.nexsqaud.rgv.api;

import java.util.List;

/**
 * Pure platform-agnostic graphics context for drawing widgets, text, tooltips, and stacks.
 */
public interface RgvDrawContext {

    void drawRect(int x, int y, int width, int height, int color);

    void drawGradient(int x, int y, int width, int height, int colorStart, int colorEnd);

    void drawTexture(String texturePath, int x, int y, int u, int v, int width, int height, int textureWidth, int textureHeight);

    void drawText(String text, int x, int y, int color, boolean shadow);

    int getTextWidth(String text);

    int getFontHeight();

    void drawStack(RgvStack stack, int x, int y);

    void drawStackOverlay(RgvStack stack, int x, int y, String overlayText);

    void drawTooltip(List<String> lines, int x, int y);

    void enableScissor(int x, int y, int width, int height);

    void disableScissor();

    void pushMatrix();

    void popMatrix();

    void translate(float x, float y, float z);

    void scale(float sx, float sy, float sz);

    void setColor(float r, float g, float b, float a);

    void resetColor();
}
