package ru.nexsqaud.rgv.platform.forge1710;

import cpw.mods.fml.client.config.GuiUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import ru.nexsqaud.rgv.api.RgvDrawContext;
import ru.nexsqaud.rgv.api.RgvStack;

import java.util.List;

/**
 * Implements RgvDrawContext using Minecraft 1.7.10 OpenGL and RenderItem graphics.
 */
public class GL11RgvRenderer implements RgvDrawContext {

    private final Minecraft mc;
    private final FontRenderer fontRenderer;
    private final RenderItem renderItem;

    public GL11RgvRenderer(Minecraft mc) {
        this.mc = mc;
        this.fontRenderer = mc.fontRenderer;
        this.renderItem = RenderItem.getInstance();
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

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        OpenGlHelper.glBlendFunc(770, 771, 1, 0);
        GL11.glShadeModel(GL11.GL_SMOOTH);

        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.setColorRGBA_F(r1, g1, b1, a1);
        tessellator.addVertex(x + width, y, 0.0D);
        tessellator.addVertex(x, y, 0.0D);
        tessellator.setColorRGBA_F(r2, g2, b2, a2);
        tessellator.addVertex(x, y + height, 0.0D);
        tessellator.addVertex(x + width, y + height, 0.0D);
        tessellator.draw();

        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    @Override
    public void drawTexture(String texturePath, int x, int y, int u, int v, int width, int height, int textureWidth, int textureHeight) {
        mc.getTextureManager().bindTexture(new ResourceLocation(texturePath));
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(770, 771, 1, 0);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        float f = 1.0F / (float) textureWidth;
        float f1 = 1.0F / (float) textureHeight;
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(x, y + height, 0.0D, u * f, (v + height) * f1);
        tessellator.addVertexWithUV(x + width, y + height, 0.0D, (u + width) * f, (v + height) * f1);
        tessellator.addVertexWithUV(x + width, y, 0.0D, (u + width) * f, v * f1);
        tessellator.addVertexWithUV(x, y, 0.0D, u * f, v * f1);
        tessellator.draw();
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
        ItemStack mcStack = Forge1710Platform.toMinecraftStack(stack);
        if (mcStack == null || mcStack.getItem() == null) return;

        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        RenderHelper.enableGUIStandardItemLighting();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        renderItem.zLevel = 200.0F;
        renderItem.renderItemAndEffectIntoGUI(fontRenderer, mc.getTextureManager(), mcStack, x, y);
        ItemStack overlayStack = mcStack.copy();
        overlayStack.stackSize = 1;
        renderItem.renderItemOverlayIntoGUI(fontRenderer, mc.getTextureManager(), overlayStack, x, y, "");
        renderItem.zLevel = 0.0F;

        RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glPopMatrix();
    }

    @Override
    public void drawStackOverlay(RgvStack stack, int x, int y, String overlayText) {
        if (stack == null || stack.isEmpty()) return;
        ItemStack mcStack = Forge1710Platform.toMinecraftStack(stack);
        if (mcStack == null || mcStack.getItem() == null) return;

        renderItem.renderItemOverlayIntoGUI(fontRenderer, mc.getTextureManager(), mcStack, x, y, overlayText);
    }

    @Override
    public void drawTooltip(List<String> lines, int x, int y) {
        if (lines == null || lines.isEmpty()) return;

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        int tooltipTextWidth = 0;
        for (String line : lines) {
            int w = fontRenderer.getStringWidth(line);
            if (w > tooltipTextWidth) tooltipTextWidth = w;
        }

        int tooltipX = x + 12;
        int tooltipY = y - 12;
        int tooltipHeight = 8;
        if (lines.size() > 1) {
            tooltipHeight += 2 + (lines.size() - 1) * 10;
        }

        ScaledResolution res = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        int screenW = res.getScaledWidth();
        int screenH = res.getScaledHeight();

        if (tooltipX + tooltipTextWidth > screenW) {
            tooltipX -= 28 + tooltipTextWidth;
        }
        if (tooltipY + tooltipHeight + 6 > screenH) {
            tooltipY = screenH - tooltipHeight - 6;
        }

        // Draw dark tooltip background
        drawGradient(tooltipX - 3, tooltipY - 4, tooltipTextWidth + 6, 1, 0xF0100010, 0xF0100010);
        drawGradient(tooltipX - 3, tooltipY + tooltipHeight + 3, tooltipTextWidth + 6, 1, 0xF0100010, 0xF0100010);
        drawGradient(tooltipX - 3, tooltipY - 3, tooltipTextWidth + 6, tooltipHeight + 6, 0xF0100010, 0xF0100010);
        drawGradient(tooltipX - 4, tooltipY - 3, 1, tooltipHeight + 6, 0xF0100010, 0xF0100010);
        drawGradient(tooltipX + tooltipTextWidth + 3, tooltipY - 3, 1, tooltipHeight + 6, 0xF0100010, 0xF0100010);

        // Inner border highlight
        int borderColor1 = 0x505000FF;
        int borderColor2 = (borderColor1 & 0xFEFEFE) >> 1 | borderColor1 & 0xFF000000;
        drawGradient(tooltipX - 3, tooltipY - 2, 1, tooltipHeight + 4, borderColor1, borderColor2);
        drawGradient(tooltipX + tooltipTextWidth + 2, tooltipY - 2, 1, tooltipHeight + 4, borderColor1, borderColor2);
        drawGradient(tooltipX - 3, tooltipY - 3, tooltipTextWidth + 6, 1, borderColor1, borderColor1);
        drawGradient(tooltipX - 3, tooltipY + tooltipHeight + 2, tooltipTextWidth + 6, 1, borderColor2, borderColor2);

        // Draw text lines
        int curY = tooltipY;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            fontRenderer.drawStringWithShadow(line, tooltipX, curY, -1);
            if (i == 0) {
                curY += 2;
            }
            curY += 10;
        }

        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    @Override
    public void enableScissor(int x, int y, int width, int height) {
        if (width <= 0 || height <= 0 || mc.displayWidth <= 0 || mc.displayHeight <= 0) return;
        ScaledResolution res = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
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
        GL11.glPushMatrix();
    }

    @Override
    public void popMatrix() {
        GL11.glPopMatrix();
    }

    @Override
    public void translate(float x, float y, float z) {
        GL11.glTranslatef(x, y, z);
    }

    @Override
    public void scale(float sx, float sy, float sz) {
        GL11.glScalef(sx, sy, sz);
    }

    @Override
    public void setColor(float r, float g, float b, float a) {
        GL11.glColor4f(r, g, b, a);
    }

    @Override
    public void resetColor() {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
