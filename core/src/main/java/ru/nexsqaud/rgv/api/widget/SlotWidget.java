package ru.nexsqaud.rgv.api.widget;

import ru.nexsqaud.rgv.api.RgvDrawContext;
import ru.nexsqaud.rgv.api.RgvIngredient;
import ru.nexsqaud.rgv.api.RgvStack;

import java.util.Collections;
import java.util.List;

/**
 * Standard recipe and inventory slot widget.
 */
public class SlotWidget extends RgvWidget {

    private RgvIngredient ingredient;
    private boolean isOutput;
    private boolean isCatalyst;
    private SlotClickListener clickListener;

    public interface SlotClickListener {
        void onSlotClicked(SlotWidget slot, int button);
    }

    public SlotWidget(RgvIngredient ingredient, int x, int y) {
        this(ingredient, x, y, false);
    }

    public SlotWidget(RgvIngredient ingredient, int x, int y, boolean isOutput) {
        super(x, y, isOutput ? 26 : 18, isOutput ? 26 : 18);
        this.ingredient = ingredient;
        this.isOutput = isOutput;
    }

    public RgvIngredient getIngredient() {
        return ingredient;
    }

    public void setIngredient(RgvIngredient ingredient) {
        this.ingredient = ingredient;
    }

    public boolean isOutput() {
        return isOutput;
    }

    public SlotWidget setCatalyst(boolean catalyst) {
        this.isCatalyst = catalyst;
        return this;
    }

    public boolean isCatalyst() {
        return isCatalyst;
    }

    public SlotWidget setClickListener(SlotClickListener clickListener) {
        this.clickListener = clickListener;
        return this;
    }

    @Override
    public void render(RgvDrawContext context, int mouseX, int mouseY, float delta) {
        if (isOutput) {
            context.drawRect(x, y, width, height, 0xFF373737);
            context.drawRect(x + 1, y + 1, width - 2, height - 2, 0xFF8B8B8B);
            context.drawRect(x + 2, y + 2, width - 4, height - 4, 0xFF373737);
            context.drawRect(x + 3, y + 3, width - 6, height - 6, 0xFF8F8F8F);
        } else {
            // Standard 18x18 slot with beveled frame
            context.drawRect(x, y, width, height, 0xFF373737);
            context.drawRect(x + 1, y + 1, width - 2, height - 2, 0xFF8B8B8B);
            context.drawRect(x + 1, y + 1, width - 2, 1, 0xFF373737);
            context.drawRect(x + 1, y + 1, 1, height - 2, 0xFF373737);
            context.drawRect(x + 2, y + 2, width - 3, height - 3, 0xFF8F8F8F);
        }

        if (ingredient != null && !ingredient.isEmpty()) {
            int renderX = x + (width - 16) / 2;
            int renderY = y + (height - 16) / 2;
            ingredient.render(context, renderX, renderY, delta);

            long amount = ingredient.getAmount();
            if (amount > 1) {
                String amountStr = String.valueOf(amount);
                context.drawText(amountStr, x + width - context.getTextWidth(amountStr) - 2, y + height - 9, 0xFFFFFF, true);
            }
        }

        // Hover highlight
        if (isMouseOver(mouseX, mouseY)) {
            context.drawRect(x + 1, y + 1, width - 2, height - 2, 0x80FFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (isMouseOver(mouseX, mouseY)) {
            if (clickListener != null) {
                clickListener.onSlotClicked(this, button);
                return true;
            }
        }
        return false;
    }

    @Override
    public List<String> getTooltip(int mouseX, int mouseY) {
        if (isMouseOver(mouseX, mouseY) && ingredient != null && !ingredient.isEmpty()) {
            return ingredient.getTooltip();
        }
        return Collections.emptyList();
    }
}
