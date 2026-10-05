package ru.nexsqaud.rgv.api.widget;

import ru.nexsqaud.rgv.api.RgvDrawContext;

/**
 * Text label widget.
 */
public class TextWidget extends RgvWidget {

    private String text;
    private int color;
    private boolean shadow;

    public TextWidget(String text, int x, int y, int color, boolean shadow) {
        super(x, y, 10, 10);
        this.text = text != null ? text : "";
        this.color = color;
        this.shadow = shadow;
    }

    public void setText(String text) {
        this.text = text != null ? text : "";
    }

    public String getText() {
        return text;
    }

    @Override
    public void render(RgvDrawContext context, int mouseX, int mouseY, float delta) {
        if (!text.isEmpty()) {
            context.drawText(text, x, y, color, shadow);
        }
    }
}
