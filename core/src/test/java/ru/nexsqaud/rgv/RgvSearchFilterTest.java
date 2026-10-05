package ru.nexsqaud.rgv;

import org.junit.jupiter.api.Test;
import ru.nexsqaud.rgv.api.RgvStack;
import ru.nexsqaud.rgv.core.index.RgvSearchFilter;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class RgvSearchFilterTest {

    @Test
    public void testPlainTextSearch() {
        RgvStack ironIngot = RgvStack.of("minecraft:iron_ingot", 0, 1, "Iron Ingot");
        RgvStack goldIngot = RgvStack.of("minecraft:gold_ingot", 0, 1, "Gold Ingot");

        RgvSearchFilter filter = new RgvSearchFilter("iron");
        assertTrue(filter.test(ironIngot));
        assertFalse(filter.test(goldIngot));
    }

    @Test
    public void testModSearch() {
        RgvStack mcApple = RgvStack.of("minecraft:apple", 0, 1, "Apple");
        RgvStack bcPipe = RgvStack.of("buildcraft:pipe_wood", 0, 1, "Wooden Pipe");

        RgvSearchFilter filterMc = new RgvSearchFilter("@minecraft");
        assertTrue(filterMc.test(mcApple));
        assertFalse(filterMc.test(bcPipe));

        RgvSearchFilter filterBc = new RgvSearchFilter("@buildcraft");
        assertFalse(filterBc.test(mcApple));
        assertTrue(filterBc.test(bcPipe));
    }

    @Test
    public void testTooltipSearch() {
        RgvStack sword = RgvStack.ofPayload("minecraft:diamond_sword", 0, 1, "Diamond Sword",
                Arrays.asList("Diamond Sword", "+7 Attack Damage", "Durability: 1561"), null);

        RgvSearchFilter filterDamage = new RgvSearchFilter("#damage");
        assertTrue(filterDamage.test(sword));

        RgvSearchFilter filterEnergy = new RgvSearchFilter("#energy");
        assertFalse(filterEnergy.test(sword));
    }

    @Test
    public void testTagSearch() {
        RgvStack copper = RgvStack.ofPayload("ic2:itemIngotCopper", 0, 1, "Copper Ingot",
                Arrays.asList("Copper Ingot", "OreDict: ingotCopper"), null);

        RgvSearchFilter filterTag = new RgvSearchFilter("$ingotcopper");
        assertTrue(filterTag.test(copper));
    }

    @Test
    public void testRegexSearch() {
        RgvStack stack1 = RgvStack.of("minecraft:wool", 1, 1, "Orange Wool");
        RgvStack stack2 = RgvStack.of("minecraft:wool", 2, 1, "Magenta Wool");

        RgvSearchFilter regexFilter = new RgvSearchFilter("/^Orange.*/");
        assertTrue(regexFilter.test(stack1));
        assertFalse(regexFilter.test(stack2));
    }

    @Test
    public void testNegationSearch() {
        RgvStack ironIngot = RgvStack.of("minecraft:iron_ingot", 0, 1, "Iron Ingot");
        RgvStack ironOre = RgvStack.of("minecraft:iron_ore", 0, 1, "Iron Ore");

        RgvSearchFilter filter = new RgvSearchFilter("iron -ore");
        assertTrue(filter.test(ironIngot));
        assertFalse(filter.test(ironOre));
    }
}
