package ru.nexsqaud.rgv.core.widget;

import ru.nexsqaud.rgv.api.RgvDrawContext;
import ru.nexsqaud.rgv.api.widget.RgvWidget;

import java.util.function.Consumer;

/**
 * Interactive text search bar with clear button and cursor support.
 */
public class SearchBarWidget extends RgvWidget {

    private String text = "";
    private boolean focused = false;
    private int cursorPosition = 0;
    private Consumer<String> onTextChanged;

    public SearchBarWidget(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    public void setOnTextChanged(Consumer<String> onTextChanged) {
        this.onTextChanged = onTextChanged;
    }

    public String getText() {
        return text;
    }

    public void setText(String newText) {
        this.text = newText != null ? newText : "";
        this.cursorPosition = this.text.length();
        if (onTextChanged != null) {
            onTextChanged.accept(this.text);
        }
    }

    public boolean isFocused() {
        return focused;
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    @Override
    public void render(RgvDrawContext context, int mouseX, int mouseY, float delta) {
        // Background & border
        int borderColor = focused ? 0xFFFFFFFF : 0xFF707070;
        context.drawRect(x, y, width, height, borderColor);
        context.drawRect(x + 1, y + 1, width - 2, height - 2, 0xFF141414);

        // Text or placeholder
        if (text.isEmpty() && !focused) {
            context.drawText("Search... (@mod, #tip, $tag)", x + 4, y + (height - 8) / 2, 0x808080, false);
        } else {
            context.drawText(text, x + 4, y + (height - 8) / 2, 0xFFFFFF, false);
        }

        // Blinking cursor
        if (focused && (System.currentTimeMillis() / 500) % 2 == 0) {
            int cursorX = x + 4 + context.getTextWidth(text.substring(0, Math.min(cursorPosition, text.length())));
            context.drawRect(cursorX, y + 2, 1, height - 4, 0xFFFFFFFF);
        }

        // Clear [x] button if text is not empty
        if (!text.isEmpty()) {
            int btnX = x + width - 12;
            int btnY = y + (height - 8) / 2;
            boolean btnHover = mouseX >= btnX && mouseX <= btnX + 8 && mouseY >= btnY && mouseY <= btnY + 8;
            context.drawText("x", btnX, btnY, btnHover ? 0xFFFF5555 : 0xAAAAAA, false);
        }
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (!text.isEmpty()) {
            int btnX = x + width - 12;
            int btnY = y + (height - 8) / 2;
            if (mouseX >= btnX && mouseX <= btnX + 8 && mouseY >= btnY && mouseY <= btnY + 8) {
                setText("");
                return true;
            }
        }

        if (isMouseOver(mouseX, mouseY)) {
            focused = true;
            cursorPosition = text.length();
            return true;
        } else {
            focused = false;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, char typedChar) {
        if (!focused) return false;

        // Backspace: 14 in LWJGL or \b
        if (keyCode == 14 || typedChar == '\b') {
            if (cursorPosition > 0 && !text.isEmpty()) {
                text = text.substring(0, cursorPosition - 1) + text.substring(cursorPosition);
                cursorPosition--;
                if (onTextChanged != null) onTextChanged.accept(text);
            }
            return true;
        }

        // Delete: 211 in LWJGL
        if (keyCode == 211) {
            if (cursorPosition < text.length()) {
                text = text.substring(0, cursorPosition) + text.substring(cursorPosition + 1);
                if (onTextChanged != null) onTextChanged.accept(text);
            }
            return true;
        }

        // Left Arrow: 203
        if (keyCode == 203) {
            if (cursorPosition > 0) cursorPosition--;
            return true;
        }

        // Right Arrow: 205
        if (keyCode == 205) {
            if (cursorPosition < text.length()) cursorPosition++;
            return true;
        }

        // Home: 199
        if (keyCode == 199) {
            cursorPosition = 0;
            return true;
        }

        // End: 207
        if (keyCode == 207) {
            cursorPosition = text.length();
            return true;
        }

        // Printable characters
        if (typedChar >= 32 && typedChar != 127) {
            text = text.substring(0, cursorPosition) + typedChar + text.substring(cursorPosition);
            cursorPosition++;
            if (onTextChanged != null) onTextChanged.accept(text);
            return true;
        }

        return false;
    }
}
