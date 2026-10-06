package ru.nexsqaud.rgv.platform.forge1122.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.config.GuiUtils;
import org.lwjgl.opengl.GL11;
import ru.nexsqaud.rgv.api.RgvDrawContext;
import ru.nexsqaud.rgv.api.RgvStack;
import ru.nexsqaud.rgv.platform.forge1122.Forge1122Platform;

import java.util.List;

/**
 * Implements RgvDrawContext using Minecraft 1.12.2 GlStateManager, Tessellator, and RenderItem.
 */
public class GL11RgvRenderer1122 implements RgvDrawContext {

    private final Minecraft mc;
    private final FontRenderer fontRenderer;
    private final RenderItem renderItem;

    public GL11RgvRenderer1122(Minecraft mc) {
        this.mc = mc;
        this.fontRenderer = mc.fontRenderer;
        this.renderItem = mc.getRenderItem();
    }

    @Override
    public void drawRect(int x, int y, int width, int height, int color) {
        Gui.drawRect(x, y, x + width, y + height, color);
    }

    @Override
    public void drawGradient(int x, int y, int width, int height, int colorStart, int colorEnd) {
        float a1 = (float)(colorStart >> 24 & 255) / 255.0F;
        float r1 = (float)(colorStart >> 16 & 255) / 255.0F;
        float g1 = (float)(colorStart >> 8 & 255) / 255.0F;
        float b1 = (float)(colorStart & 255) / 255.0F;
        float a2 = (float)(colorEnd >> 24 & 255) / 255.0F;
        float r2 = (float)(colorEnd >> 16 & 255) / 255.0F;
        float g2 = (float)(colorEnd >> 8 & 255) / 255.0F;
        float b2 = (float)(colorEnd & 255) / 255.0F;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(x + width, y, 0.0D).color(r1, g1, b1, a1).endVertex();
        buffer.pos(x, y, 0.0D).color(r1, g1, b1, a1).endVertex();
        buffer.pos(x, y + height, 0.0D).color(r2, g2, b2, a2).endVertex();
        buffer.pos(x + width, y + height, 0.0D).color(r2, g2, b2, a2).endVertex();
        tessellator.draw();

        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    @Override
    public void drawTexture(String texturePath, int x, int y, int u, int v, int width, int height, int textureWidth, int textureHeight) {
        mc.getTextureManager().bindTexture(new ResourceLocation(texturePath));
        GlStateManager.enableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        Gui.drawScaledCustomSizeModalRect(x, y, (float) u, (float) v, width, height, width, height, (float) textureWidth, (float) textureHeight);
    }

    @Override
    public void drawText(String text, int x, int y, int color, boolean shadow) {
        if (text == null || text.isEmpty()) return;
        fontRenderer.drawString(text, x, y, color, shadow);
    }

    @Override
    public int getTextWidth(String text) {
        if (text == null || text.isEmpty()) return 0;
        return fontRenderer.getStringWidth(text);
    }

    @Override
    public int getFontHeight() {
        return fontRenderer.FONT_HEIGHT;
    }

    @Override
    public void drawStack(RgvStack stack, int x, int y) {
        if (stack == null || stack.isEmpty()) return;
        ItemStack mcStack = Forge1122Platform.toMinecraftStack(stack);
        if (mcStack == null || mcStack.isEmpty()) return;

        GlStateManager.pushMatrix();
        GlStateManager.enableDepth();
        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        renderItem.zLevel = 200.0F;
        try {
            renderItem.renderItemAndEffectIntoGUI(mcStack, x, y);
        } catch (Throwable ignored) {
        }
        renderItem.zLevel = 0.0F;

        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableDepth();
        GlStateManager.popMatrix();
    }

    @Override
    public void drawStackOverlay(RgvStack stack, int x, int y, String overlayText) {
        if (stack == null || stack.isEmpty()) return;
        ItemStack mcStack = Forge1122Platform.toMinecraftStack(stack);
        if (mcStack == null || mcStack.isEmpty()) return;

        renderItem.renderItemOverlayIntoGUI(fontRenderer, mcStack, x, y, overlayText);
    }

    @Override
    public void drawTooltip(List<String> lines, int x, int y) {
        if (lines == null || lines.isEmpty()) return;

        ScaledResolution res = new ScaledResolution(mc);
        int screenW = res.getScaledWidth();
        int screenH = res.getScaledHeight();

        GlStateManager.pushMatrix();
        GuiUtils.drawHoveringText(lines, x, y, screenW, screenH, -1, fontRenderer);
        GlStateManager.popMatrix();
    }

    @Override
    public void enableScissor(int x, int y, int width, int height) {
        if (width <= 0 || height <= 0 || mc.displayWidth <= 0 || mc.displayHeight <= 0) return;
        ScaledResolution res = new ScaledResolution(mc);
        int scale = res.getScaleFactor();

        int sx = x * scale;
        int sy = mc.displayHeight - (y + height) * scale;
        int sw = width * scale;
        int sh = height * scale;

        if (sx < 0) {
            sw += sx;
            sx = 0;
        }
        if (sy < 0) {
            sh += sy;
            sy = 0;
        }
        if (sx + sw > mc.displayWidth) {
            sw = mc.displayWidth - sx;
        }
        if (sy + sh > mc.displayHeight) {
            sh = mc.displayHeight - sy;
        }
        if (sw <= 0 || sh <= 0) return;

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(sx, sy, sw, sh);
    }

    @Override
    public void disableScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    @Override
    public void pushMatrix() {
        GlStateManager.pushMatrix();
    }

    @Override
    public void popMatrix() {
        GlStateManager.popMatrix();
    }

    @Override
    public void translate(float x, float y, float z) {
        GlStateManager.translate(x, y, z);
    }

    @Override
    public void scale(float sx, float sy, float sz) {
        GlStateManager.scale(sx, sy, sz);
    }

    @Override
    public void setColor(float r, float g, float b, float a) {
        GlStateManager.color(r, g, b, a);
    }

    @Override
    public void resetColor() {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
