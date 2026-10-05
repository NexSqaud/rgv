package ru.nexsqaud.rgv.api;

import java.util.List;

/**
 * Defines screen areas that should not be obscured by RGV overlays
 * (e.g. modded GUI side tabs or extra buttons).
 */
public interface RgvExclusionZone {

    class Bounds {
        public final int x;
        public final int y;
        public final int width;
        public final int height;

        public Bounds(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public boolean contains(int px, int py) {
            return px >= x && px < x + width && py >= y && py < y + height;
        }
    }

    boolean appliesTo(Object screen);

    List<Bounds> getExclusionZones(Object screen);
}
