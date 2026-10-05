package ru.nexsqaud.rgv.api.widget;

import ru.nexsqaud.rgv.api.RgvIngredient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Collects and builds widgets for an RgvRecipe presentation.
 */
public class RgvWidgetHolder {

    private final int width;
    private final int height;
    private final List<RgvWidget> widgets = new ArrayList<>();

    public RgvWidgetHolder(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public List<RgvWidget> getWidgets() {
        return Collections.unmodifiableList(widgets);
    }

    public <T extends RgvWidget> T addWidget(T widget) {
        if (widget != null) {
            widgets.add(widget);
        }
        return widget;
    }

    public SlotWidget addSlot(RgvIngredient ingredient, int x, int y) {
        return addWidget(new SlotWidget(ingredient, x, y));
    }

    public SlotWidget addOutputSlot(RgvIngredient ingredient, int x, int y) {
        return addWidget(new SlotWidget(ingredient, x, y, true));
    }

    public SlotWidget addLargeSlot(RgvIngredient ingredient, int x, int y) {
        return addWidget(new SlotWidget(ingredient, x, y, true));
    }

    public ArrowWidget addArrow(int x, int y, boolean animated) {
        return addWidget(new ArrowWidget(x, y, animated, 10000));
    }

    public ArrowWidget addArrow(int x, int y, boolean animated, int durationMs) {
        return addWidget(new ArrowWidget(x, y, animated, durationMs));
    }

    public FlameWidget addFlame(int x, int y, boolean animated) {
        return addWidget(new FlameWidget(x, y, animated, 10000));
    }

    public FlameWidget addFlame(int x, int y, boolean animated, int durationMs) {
        return addWidget(new FlameWidget(x, y, animated, durationMs));
    }

    public TextWidget addText(String text, int x, int y, int color, boolean shadow) {
        return addWidget(new TextWidget(text, x, y, color, shadow));
    }
}
