package ru.nexsqaud.rgv.api.widget;

import ru.nexsqaud.rgv.api.RgvDrawContext;

import java.util.Collections;
import java.util.List;

/**
 * Recipe progress arrow widget (e.g. for crafting, smelting, macerating).
 */
public class ArrowWidget extends RgvWidget {

    private final boolean animated;
    private final int durationMs;
    private String tooltipText = "";

    public ArrowWidget(int x, int y, boolean animated, int durationMs) {
        super(x, y, 24, 17);
        this.animated = animated;
        this.durationMs = durationMs > 0 ? durationMs : 2000;
    }

    public ArrowWidget setTooltip(String text) {
        this.tooltipText = text != null ? text : "";
        return this;
    }

    @Override
    public void render(RgvDrawContext context, int mouseX, int mouseY, float delta) {
        context.drawRect(x, y + 6, 16, 5, 0xFF8B8B8B);
        for (int i = 0; i < 8; i++) {
            context.drawRect(x + 16 + i, y + 8 - i, 1, 1 + i * 2, 0xFF8B8B8B);
        }

        if (animated) {
            float progress = (float) (System.currentTimeMillis() % durationMs) / (float) durationMs;
            int fillW = (int) (24 * progress);
            if (fillW > 0) {
                int rectPart = Math.min(fillW, 16);
                context.drawRect(x, y + 6, rectPart, 5, 0xFF55FF55);
                if (fillW > 16) {
                    int trianglePart = fillW - 16;
                    for (int i = 0; i < trianglePart; i++) {
                        context.drawRect(x + 16 + i, y + 8 - i, 1, 1 + i * 2, 0xFF55FF55);
                    }
                }
            }
        }
    }

    @Override
    public List<String> getTooltip(int mouseX, int mouseY) {
        if (isMouseOver(mouseX, mouseY) && !tooltipText.isEmpty()) {
            return Collections.singletonList(tooltipText);
        }
        return Collections.emptyList();
    }
}
