package ru.nexsqaud.rgv.api.widget;

import ru.nexsqaud.rgv.api.RgvDrawContext;

import java.util.Collections;
import java.util.List;

/**
 * Animated burning flame widget (e.g. for furnaces, generators).
 */
public class FlameWidget extends RgvWidget {

    private final boolean animated;
    private final int durationMs;
    private String tooltipText = "";

    public FlameWidget(int x, int y, boolean animated, int durationMs) {
        super(x, y, 14, 14);
        this.animated = animated;
        this.durationMs = durationMs > 0 ? durationMs : 1600;
    }

    public FlameWidget setTooltip(String tooltipText) {
        this.tooltipText = tooltipText != null ? tooltipText : "";
        return this;
    }

    @Override
    public void render(RgvDrawContext context, int mouseX, int mouseY, float delta) {
        // Base background flame silhouette
        context.drawRect(x + 2, y + 2, 10, 10, 0xFF3F3F3F);

        float burnProgress = 1.0f;
        if (animated) {
            burnProgress = 1.0f - ((float) (System.currentTimeMillis() % durationMs) / (float) durationMs);
        }

        int flameHeight = (int) (10 * burnProgress);
        if (flameHeight > 0) {
            int top = y + 2 + (10 - flameHeight);
            context.drawGradient(x + 2, top, 10, flameHeight, 0xFFFFCC00, 0xFFFF3300);
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
