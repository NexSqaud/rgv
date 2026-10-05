package ru.nexsqaud.rgv.api.widget;

import ru.nexsqaud.rgv.api.RgvDrawContext;

import java.util.Collections;
import java.util.List;

/**
 * Animated burning flame widget (e.g. for furnaces, generators).
 */
public class FlameWidget extends RgvWidget {

    private static final String FURNACE_TEXTURE = "textures/gui/container/furnace.png";

    private final boolean animated;
    private final int durationMs;
    private String tooltipText = "Smelting / Fuel";

    public FlameWidget(int x, int y, boolean animated, int durationMs) {
        super(x, y, 14, 14);
        this.animated = animated;
        this.durationMs = durationMs > 0 ? durationMs : 10000;
    }

    public FlameWidget setTooltip(String tooltipText) {
        this.tooltipText = tooltipText != null ? tooltipText : "";
        return this;
    }

    @Override
    public void render(RgvDrawContext context, int mouseX, int mouseY, float delta) {
        // 1. Draw unlit flame background from vanilla furnace texture (u=56, v=36)
        context.drawTexture(FURNACE_TEXTURE, x, y, 56, 36, 14, 14, 256, 256);

        // 2. Burn progress (real furnace cycle speed: 200 ticks = 10,000 ms)
        float burnProgress = 1.0f;
        if (animated) {
            burnProgress = 1.0f - ((float) (System.currentTimeMillis() % durationMs) / (float) durationMs);
        }

        int flameHeight = (int) (14 * burnProgress);
        if (flameHeight > 0) {
            int yOffset = 14 - flameHeight;
            // 3. Draw lit flame from vanilla furnace texture (u=176, v=0) burning from top to bottom
            context.drawTexture(FURNACE_TEXTURE, x, y + yOffset, 176, yOffset, 14, flameHeight, 256, 256);
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
