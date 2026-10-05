package ru.nexsqaud.rgv.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Concrete immutable representation of a stack (item, fluid, or energy).
 * Completely decoupled from Minecraft's internal item representations.
 */
public class RgvStack implements RgvIngredient {

    public enum Type {
        ITEM,
        FLUID,
        TAG,
        CUSTOM
    }

    public static final RgvStack EMPTY = new RgvStack("", 0, 0, 1.0f, Type.ITEM, "", Collections.emptyList(), null);

    private final String id;
    private final int meta;
    private final long amount;
    private final float chance;
    private final Type type;
    private final String displayName;
    private final List<String> tooltip;
    private final Object platformPayload;

    public RgvStack(String id, int meta, long amount, float chance, Type type, String displayName, List<String> tooltip, Object platformPayload) {
        this.id = id != null ? id : "";
        this.meta = meta;
        this.amount = amount;
        this.chance = chance;
        this.type = type != null ? type : Type.ITEM;
        this.displayName = displayName != null ? displayName : this.id;
        this.tooltip = tooltip != null ? new ArrayList<>(tooltip) : Collections.emptyList();
        this.platformPayload = platformPayload;
    }

    public static RgvStack of(String id, int meta, long amount) {
        return new RgvStack(id, meta, amount, 1.0f, Type.ITEM, id, Collections.singletonList(id), null);
    }

    public static RgvStack of(String id, int meta, long amount, String displayName) {
        return new RgvStack(id, meta, amount, 1.0f, Type.ITEM, displayName, Collections.singletonList(displayName), null);
    }

    public static RgvStack ofPayload(String id, int meta, long amount, String displayName, List<String> tooltip, Object platformPayload) {
        return new RgvStack(id, meta, amount, 1.0f, Type.ITEM, displayName, tooltip, platformPayload);
    }

    public static RgvStack empty() {
        return EMPTY;
    }

    public String getId() {
        return id;
    }

    public int getMeta() {
        return meta;
    }

    @Override
    public long getAmount() {
        return amount;
    }

    public float getChance() {
        return chance;
    }

    public Type getType() {
        return type;
    }

    @Override
    public boolean isEmpty() {
        return this == EMPTY || amount <= 0 || id.isEmpty();
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public List<String> getTooltip() {
        return Collections.unmodifiableList(tooltip);
    }

    public Object getPlatformPayload() {
        return platformPayload;
    }

    @SuppressWarnings("unchecked")
    public <T> T unwrap(Class<T> type) {
        if (platformPayload != null && type.isInstance(platformPayload)) {
            return (T) platformPayload;
        }
        return null;
    }

    @Override
    public List<RgvStack> getRgvStacks() {
        return Collections.singletonList(this);
    }

    @Override
    public void render(RgvDrawContext context, int x, int y, float delta) {
        if (isEmpty()) return;
        context.drawStack(this, x, y);
    }

    @Override
    public boolean matches(RgvStack other) {
        if (other == null) return false;
        if (this.isEmpty() && other.isEmpty()) return true;
        if (this.isEmpty() || other.isEmpty()) return false;
        if (!Objects.equals(this.id, other.id)) return false;
        if (this.meta != 32767 && other.meta != 32767 && this.meta != other.meta) return false;
        ru.nexsqaud.rgv.core.platform.RgvPlatform platform = ru.nexsqaud.rgv.core.platform.RgvPlatform.get();
        if (platform != null && this.platformPayload != null && other.platformPayload != null) {
            return platform.areStacksEqual(this, other);
        }
        return true;
    }

    public boolean isSameType(RgvStack other) {
        return matches(other);
    }

    @Override
    public RgvStack copyWithAmount(long newAmount) {
        return new RgvStack(this.id, this.meta, newAmount, this.chance, this.type, this.displayName, this.tooltip, this.platformPayload);
    }

    public RgvStack copyWithChance(float newChance) {
        return new RgvStack(this.id, this.meta, this.amount, newChance, this.type, this.displayName, this.tooltip, this.platformPayload);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RgvStack)) return false;
        RgvStack rgvStack = (RgvStack) o;
        return meta == rgvStack.meta &&
               amount == rgvStack.amount &&
               Objects.equals(id, rgvStack.id) &&
               type == rgvStack.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, meta, type);
    }

    @Override
    public String toString() {
        return "RgvStack{" + id + ":" + meta + " x" + amount + (chance < 1.0f ? " @" + (int)(chance * 100) + "%" : "") + "}";
    }
}
