package ru.nexsqaud.rgv.api.widget;

import ru.nexsqaud.rgv.api.RgvDrawContext;

import java.util.Collections;
import java.util.List;

/**
 * Recipe progress arrow widget (e.g. for crafting, smelting, macerating).
 */
public class ArrowWidget extends RgvWidget {

    private static final String FURNACE_TEXTURE = "textures/gui/container/furnace.png";

    private final boolean animated;
    private final int durationMs;
    private String tooltipText = "";

    public ArrowWidget(int x, int y, boolean animated, int durationMs) {
        super(x, y, 24, 17);
        this.animated = animated;
        this.durationMs = durationMs > 0 ? durationMs : 10000;
    }

    public ArrowWidget setTooltip(String text) {
        this.tooltipText = text != null ? text : "";
        return this;
    }

    @Override
    public void render(RgvDrawContext context, int mouseX, int mouseY, float delta) {
        // Draw empty arrow background from vanilla furnace texture (u=79, v=34)
        context.drawTexture(FURNACE_TEXTURE, x, y, 79, 34, 24, 17, 256, 256);

        // Progress at real furnace speed (200 ticks = 10,000 ms)
        float progress = animated ? (float) (System.currentTimeMillis() % durationMs) / (float) durationMs : 0f;
        int fillW = (int) (24 * progress);

        // Draw filled arrow progress overlay from vanilla furnace texture (u=176, v=14)
        if (fillW > 0) {
            context.drawTexture(FURNACE_TEXTURE, x, y, 176, 14, fillW, 17, 256, 256);
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
