package ru.nexsqaud.rgv.platform.forge1710.compat.nei;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.nexsqaud.rgv.api.*;
import ru.nexsqaud.rgv.api.widget.RgvWidgetHolder;
import ru.nexsqaud.rgv.core.platform.RgvPlatform;
import ru.nexsqaud.rgv.core.screen.RgvRecipeScreen;
import ru.nexsqaud.rgv.core.screen.RgvScreenManager;
import ru.nexsqaud.rgv.platform.forge1710.Forge1710Platform;
import ru.nexsqaud.rgv.platform.forge1710.GL11RgvRenderer;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Reflection-safe helper for deep integration with NEI (Not Enough Items) screens.
 * Adds the [+Graph] button into NEI's GuiRecipe screens and extracts active recipes directly into RGV Craft Graph.
 */
public class NeiRecipeHelper {

    private static final Logger LOG = LogManager.getLogger("RGV-NEI");
    private static Object creativeHandlerProxy = null;

    public static boolean isNeiPanelVisible() {
        if (!Forge1710Platform.isNeiPresent()) return false;
        try {
            Class<?> cfg = Class.forName("codechicken.nei.NEIClientConfig");
            Method isEnabled = cfg.getMethod("isEnabled");
            Method isHidden = cfg.getMethod("isHidden");
            boolean enabled = (Boolean) isEnabled.invoke(null);
            boolean hidden = (Boolean) isHidden.invoke(null);
            if (!enabled || hidden) {
                return false;
            }

            try {
                Class<?> lm = Class.forName("codechicken.nei.LayoutManager");
                Field fItemPanel = lm.getField("itemPanel");
                Object itemPanel = fItemPanel.get(null);
                Field fDrawWidgets = lm.getDeclaredField("drawWidgets");
                fDrawWidgets.setAccessible(true);
                Object drawWidgets = fDrawWidgets.get(null);

                if (drawWidgets instanceof Iterable && itemPanel != null) {
                    for (Object w : (Iterable<?>) drawWidgets) {
                        if (w == itemPanel) {
                            return true;
                        }
                    }
                    if (drawWidgets instanceof java.util.Collection && !((java.util.Collection<?>) drawWidgets).isEmpty()) {
                        return false;
                    }
                }
            } catch (Throwable ignored) {
            }

            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean isTextInputFocused() {
        if (!Forge1710Platform.isNeiPresent()) return false;
        try {
            Class<?> lm = Class.forName("codechicken.nei.LayoutManager");
            Method m = lm.getMethod("getInputFocused");
            return m.invoke(null) != null;
        } catch (Throwable t) {
            return false;
        }
    }

    public static void ensureCreativeGuiHandlerRegistered(final RgvScreenManager screenManager) {
        if (!Forge1710Platform.isNeiPresent() || screenManager == null) return;
        try {
            Class<?> guiInfo = Class.forName("codechicken.nei.api.GuiInfo");
            Field fGuiHandlers = guiInfo.getField("guiHandlers");
            @SuppressWarnings("unchecked")
            LinkedList<Object> handlers = (LinkedList<Object>) fGuiHandlers.get(null);
            if (handlers == null) return;

            if (creativeHandlerProxy != null && !handlers.isEmpty() && handlers.getLast() == creativeHandlerProxy) {
                return;
            }

            if (creativeHandlerProxy == null) {
                Class<?> handlerClass = Class.forName("codechicken.nei.api.INEIGuiHandler");
                creativeHandlerProxy = Proxy.newProxyInstance(
                        NeiRecipeHelper.class.getClassLoader(),
                        new Class<?>[]{handlerClass},
                        new InvocationHandler() {
                            @Override
                            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                                String name = method.getName();
                                if ("modifyVisiblity".equals(name)) {
                                    Object gui = args[0];
                                    Object visiblityData = args[1];
                                    if (gui != null && visiblityData != null && screenManager.getConfig().isReplaceWithNei()) {
                                        try {
                                            Class<?> creativeClass = Class.forName("net.minecraft.client.gui.inventory.GuiContainerCreative");
                                            if (creativeClass.isInstance(gui)) {
                                                boolean isHidden = false;
                                                try {
                                                    Class<?> cfg = Class.forName("codechicken.nei.NEIClientConfig");
                                                    Method mHidden = cfg.getMethod("isHidden");
                                                    isHidden = (Boolean) mHidden.invoke(null);
                                                } catch (Throwable ignored) {
                                                }

                                                if (!isHidden) {
                                                    Field fShowItemSection = visiblityData.getClass().getField("showItemSection");
                                                    Field fShowItemPanel = visiblityData.getClass().getField("showItemPanel");
                                                    Field fShowSearchSection = visiblityData.getClass().getField("showSearchSection");
                                                    Field fShowWidgets = visiblityData.getClass().getField("showWidgets");
                                                    Field fShowNEI = visiblityData.getClass().getField("showNEI");

                                                    fShowItemSection.setBoolean(visiblityData, true);
                                                    fShowItemPanel.setBoolean(visiblityData, true);
                                                    fShowSearchSection.setBoolean(visiblityData, true);
                                                    fShowWidgets.setBoolean(visiblityData, true);
                                                    fShowNEI.setBoolean(visiblityData, true);
                                                }
                                            }
                                        } catch (Throwable ignored) {
                                        }
                                    }
                                    return visiblityData;
                                } else if ("handleDragNDrop".equals(name) || "hideItemPanelSlot".equals(name)) {
                                    return Boolean.FALSE;
                                } else if ("getItemSpawnSlots".equals(name) || "getInventoryAreas".equals(name)) {
                                    return Collections.emptyList();
                                }

                                if (method.getReturnType() == boolean.class) return Boolean.FALSE;
                                if (method.getReturnType() == int.class) return 0;
                                return null;
                            }
                        }
                );
            }

            handlers.remove(creativeHandlerProxy);
            Class<?> apiClass = Class.forName("codechicken.nei.api.API");
            Class<?> handlerClass = Class.forName("codechicken.nei.api.INEIGuiHandler");
            Method regMethod = apiClass.getMethod("registerNEIGuiHandler", handlerClass);
            regMethod.invoke(null, creativeHandlerProxy);
            LOG.info("Registered RGV Creative NEI GuiHandler at end of NEI guiHandlers chain.");
        } catch (Throwable t) {
            LOG.debug("Could not register Creative NEI GuiHandler: " + t.getMessage());
        }
    }

    public static boolean isGuiRecipe(Object gui) {
        if (gui == null) return false;
        Class<?> c = gui.getClass();
        while (c != null && c != Object.class) {
            if ("codechicken.nei.recipe.GuiRecipe".equals(c.getName())) {
                return true;
            }
            c = c.getSuperclass();
        }
        return false;
    }

    public static boolean openRecipeGui(ItemStack stack, boolean isUsage) {
        if (stack == null || stack.getItem() == null) return false;
        try {
            String className = isUsage ? "codechicken.nei.recipe.GuiUsageRecipe" : "codechicken.nei.recipe.GuiCraftingRecipe";
            Class<?> clazz = Class.forName(className);
            Method m = clazz.getMethod("openRecipeGui", String.class, Object[].class);
            Object result = m.invoke(null, "item", new Object[]{stack});
            return Boolean.TRUE.equals(result);
        } catch (Throwable t) {
            LOG.warn("Failed to open NEI recipe GUI: " + t.getMessage());
            return false;
        }
    }

    public static ItemStack getStackMouseOver(GuiContainer container) {
        if (container == null) return null;
        try {
            Class<?> clazz = Class.forName("codechicken.nei.guihook.GuiContainerManager");
            Method m = clazz.getMethod("getStackMouseOver", GuiContainer.class);
            ItemStack stack = (ItemStack) m.invoke(null, container);
            if (stack != null && stack.getItem() != null) {
                return stack;
            }
        } catch (Throwable ignored) {
        }

        // Fallback: check slots in container
        try {
            Field fGuiLeft = GuiContainer.class.getDeclaredField("field_147003_i");
            fGuiLeft.setAccessible(true);
            Field fGuiTop = GuiContainer.class.getDeclaredField("field_147009_r");
            fGuiTop.setAccessible(true);
            int guiLeft = fGuiLeft.getInt(container);
            int guiTop = fGuiTop.getInt(container);

            Minecraft mc = Minecraft.getMinecraft();
            int mouseX = org.lwjgl.input.Mouse.getX() * mc.currentScreen.width / mc.displayWidth;
            int mouseY = mc.currentScreen.height - org.lwjgl.input.Mouse.getY() * mc.currentScreen.height / mc.displayHeight - 1;

            for (Object obj : container.inventorySlots.inventorySlots) {
                if (obj instanceof Slot) {
                    Slot s = (Slot) obj;
                    if (s.getHasStack() && mouseX >= guiLeft + s.xDisplayPosition && mouseX < guiLeft + s.xDisplayPosition + 16
                            && mouseY >= guiTop + s.yDisplayPosition && mouseY < guiTop + s.yDisplayPosition + 16) {
                        return s.getStack();
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static void renderRecipeTooltips(GL11RgvRenderer renderer, Object guiRecipe, int mouseX, int mouseY, List<?> buttonList) {
        if (guiRecipe == null || renderer == null || buttonList == null) return;

        for (Object btn : buttonList) {
            if (btn instanceof ru.nexsqaud.rgv.platform.forge1710.gui.RgvNeiGraphButton) {
                ru.nexsqaud.rgv.platform.forge1710.gui.RgvNeiGraphButton rgvBtn = (ru.nexsqaud.rgv.platform.forge1710.gui.RgvNeiGraphButton) btn;
                if (rgvBtn.visible && rgvBtn.isMouseOver()) {
                    List<String> tip = rgvBtn.getTooltip();
                    if (tip != null && !tip.isEmpty()) {
                        renderer.drawTooltip(tip, mouseX, mouseY);
                        break;
                    }
                }
            }
        }
    }

    public static boolean mouseClicked(Object guiRecipe, int mouseX, int mouseY, int button, RgvScreenManager screenManager) {
        if (button != 0 || guiRecipe == null || screenManager == null) return false;

        int guiLeft = getIntField(guiRecipe, "field_147003_i", "guiLeft", (Minecraft.getMinecraft().currentScreen.width - 176) / 2);
        int guiTop = getIntField(guiRecipe, "field_147009_r", "guiTop", (Minecraft.getMinecraft().currentScreen.height - 166) / 2);
        int xSize = getIntField(guiRecipe, "field_146999_f", "xSize", 176);

        // Check side [+ Graph] button as fallback
        int sideBtnX = guiLeft + xSize + 2;
        int sideBtnY = guiTop + 4;
        if (mouseX >= sideBtnX && mouseX < sideBtnX + 54 && mouseY >= sideBtnY && mouseY < sideBtnY + 20) {
            addCurrentRecipeToGraph(guiRecipe, mouseX, mouseY, screenManager);
            return true;
        }

        return false;
    }

    public static void addCurrentRecipeToGraph(Object guiRecipe, int mouseX, int mouseY, RgvScreenManager screenManager) {
        if (guiRecipe == null || screenManager == null) return;
        List<?> handlers = getHandlers(guiRecipe);
        int recipetype = getIntField(guiRecipe, "recipetype", "recipetype", 0);
        int page = getIntField(guiRecipe, "page", "page", 0);

        if (handlers != null && recipetype >= 0 && recipetype < handlers.size()) {
            Object handler = handlers.get(recipetype);
            int perPage = invokeInt(handler, "recipiesPerPage", 1);
            int numRecipes = invokeInt(handler, "numRecipes", 0);
            if (numRecipes <= 0) return;
            if (perPage <= 0) perPage = 1;

            int guiTop = getIntField(guiRecipe, "field_147009_r", "guiTop", (Minecraft.getMinecraft().currentScreen.height - 166) / 2);
            int offset = 0;
            if (perPage > 1 && mouseY >= guiTop + 16 + 65) {
                offset = 1;
            }

            int targetIndex = Math.min(numRecipes - 1, Math.max(0, page * perPage + offset));
            addNeiRecipeIndexToGraph(handler, targetIndex, screenManager);
        }
    }

    public static void addNeiRecipeIndexToGraph(Object handler, int recipeIndex, RgvScreenManager screenManager) {
        if (handler == null || screenManager == null) return;
        try {
            RgvRecipe rgvRecipe = createRgvRecipeFromNei(handler, recipeIndex);
            if (rgvRecipe != null) {
                screenManager.getRecipeManager().addRecipe(rgvRecipe);
                RgvInventory inv = RgvPlatform.get() != null ? RgvPlatform.get().getPlayerInventory() : null;
                screenManager.getCraftGraph().addTabForRecipe(rgvRecipe, 1, screenManager.getRecipeManager(), inv);
                screenManager.getRecipeScreen().openGraphView();
            }
        } catch (Throwable t) {
            LOG.warn("Failed to add NEI recipe to graph: " + t.getMessage(), t);
        }
    }

    public static RgvRecipe createRgvRecipeFromNei(Object handler, int recipeIndex) {
        try {
            Method getResultStackMethod = handler.getClass().getMethod("getResultStack", int.class);
            Object pResult = getResultStackMethod.invoke(handler, recipeIndex);
            if (pResult == null) return null;

            ItemStack outItem = getPositionedStackItem(pResult);
            if (outItem == null || outItem.getItem() == null) return null;
            RgvStack outStack = Forge1710Platform.toRgvStack(outItem);

            Method getRecipeNameMethod = handler.getClass().getMethod("getRecipeName");
            String recipeName = (String) getRecipeNameMethod.invoke(handler);
            if (recipeName == null || recipeName.isEmpty()) recipeName = handler.getClass().getSimpleName();
            boolean isCrafting = recipeName.equalsIgnoreCase("Crafting");

            Method getIngredientStacksMethod = handler.getClass().getMethod("getIngredientStacks", int.class);
            List<?> pIngredients = (List<?>) getIngredientStacksMethod.invoke(handler, recipeIndex);

            final List<RgvIngredient> inputs = new ArrayList<>();
            final List<SlotPlacement> placements = new ArrayList<>();

            if (pIngredients != null) {
                for (Object pIn : pIngredients) {
                    if (pIn == null) continue;
                    RgvIngredient ing = parsePositionedIngredient(pIn, isCrafting);
                    if (ing != null && !ing.isEmpty()) {
                        inputs.add(ing);
                        int rx = getIntField(pIn, "relx", "relx", 0);
                        int ry = getIntField(pIn, "rely", "rely", 0);
                        placements.add(new SlotPlacement(ing, rx, ry));
                    }
                }
            }

            final String catId = "nei." + recipeName.toLowerCase().replaceAll("[^a-z0-9_]", "_");
            final RgvRecipeCategory category = new RgvRecipeCategory(catId, recipeName, outStack);
            final String recipeId = catId + "_" + System.currentTimeMillis() + "_" + recipeIndex;

            return new RgvRecipe() {
                @Override public String getId() { return recipeId; }
                @Override public RgvRecipeCategory getCategory() { return category; }
                @Override public List<RgvIngredient> getInputs() { return inputs; }
                @Override public List<RgvStack> getOutputs() { return Collections.singletonList(outStack); }

                @Override
                public void addWidgets(RgvWidgetHolder holder) {
                    if (!placements.isEmpty()) {
                        for (SlotPlacement p : placements) {
                            holder.addSlot(p.ingredient, p.x, p.y);
                        }
                    } else {
                        for (int i = 0; i < Math.min(inputs.size(), 9); i++) {
                            holder.addSlot(inputs.get(i), 6 + (i % 3) * 18, 6 + (i / 3) * 18);
                        }
                    }
                    holder.addArrow(66, 22, true);
                    holder.addOutputSlot(outStack, 96, 18);
                }
            };
        } catch (Throwable t) {
            LOG.warn("Could not create RgvRecipe from NEI handler: " + t.getMessage());
            return null;
        }
    }

    private static class SlotPlacement {
        final RgvIngredient ingredient;
        final int x;
        final int y;

        SlotPlacement(RgvIngredient ingredient, int x, int y) {
            this.ingredient = ingredient;
            this.x = x;
            this.y = y;
        }
    }

    private static RgvIngredient parsePositionedIngredient(Object pStack, boolean isCrafting) {
        try {
            Field itemsField = pStack.getClass().getField("items");
            ItemStack[] items = (ItemStack[]) itemsField.get(pStack);
            if (items != null && items.length > 0) {
                List<RgvStack> stacks = new ArrayList<>();
                long amt = isCrafting ? 1 : Math.max(1, items[0] != null ? items[0].stackSize : 1);
                for (ItemStack s : items) {
                    if (s != null && s.getItem() != null) {
                        stacks.add(Forge1710Platform.toRgvStack(s).copyWithAmount(amt));
                    }
                }
                return RgvIngredientList.of(stacks, amt);
            }
        } catch (Throwable ignored) {
        }

        ItemStack item = getPositionedStackItem(pStack);
        if (item != null && item.getItem() != null) {
            RgvStack stack = Forge1710Platform.toRgvStack(item);
            if (isCrafting) {
                return stack.copyWithAmount(1);
            }
            return stack;
        }
        return RgvStack.empty();
    }

    private static ItemStack getPositionedStackItem(Object pStack) {
        if (pStack == null) return null;
        try {
            Field f = pStack.getClass().getField("item");
            return (ItemStack) f.get(pStack);
        } catch (Throwable ignored) {
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static List<?> getHandlers(Object guiRecipe) {
        try {
            Method m = guiRecipe.getClass().getMethod("getCurrentRecipeHandlers");
            return (List<?>) m.invoke(guiRecipe);
        } catch (Throwable t) {
            try {
                Field f = guiRecipe.getClass().getField("currenthandlers");
                return (List<?>) f.get(guiRecipe);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    public static int getIntField(Object obj, String field1, String field2, int def) {
        try {
            Field f = obj.getClass().getField(field1);
            return f.getInt(obj);
        } catch (Throwable t1) {
            try {
                Field f = obj.getClass().getField(field2);
                return f.getInt(obj);
            } catch (Throwable t2) {
                try {
                    Field f = obj.getClass().getDeclaredField(field1);
                    f.setAccessible(true);
                    return f.getInt(obj);
                } catch (Throwable t3) {
                    try {
                        Field f = obj.getClass().getDeclaredField(field2);
                        f.setAccessible(true);
                        return f.getInt(obj);
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
        return def;
    }

    public static int invokeInt(Object obj, String method, int def) {
        try {
            Method m = obj.getClass().getMethod(method);
            return (Integer) m.invoke(obj);
        } catch (Throwable ignored) {
        }
        return def;
    }
}
