package ru.nexsqaud.rgv.api.widget;

import ru.nexsqaud.rgv.api.RgvDrawContext;
import java.util.Collections;
import java.util.List;

/**
 * Base abstract widget for recipe layouts and GUI screens.
 */
public abstract class RgvWidget {

    protected int x;
    protected int y;
    protected int width;
    protected int height;

    public RgvWidget(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public abstract void render(RgvDrawContext context, int mouseX, int mouseY, float delta);

    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        return false;
    }

    public boolean keyPressed(int keyCode, char typedChar) {
        return false;
    }

    public List<String> getTooltip(int mouseX, int mouseY) {
        return Collections.emptyList();
    }
}
