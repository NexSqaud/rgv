package ru.nexsqaud.rgv.platform.forge1710;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import ru.nexsqaud.rgv.core.platform.RgvPlatform;
import ru.nexsqaud.rgv.core.screen.RgvRecipeScreen;
import ru.nexsqaud.rgv.core.screen.RgvScreenManager;
import ru.nexsqaud.rgv.platform.forge1710.gui.GuiOverlayHooks;
import ru.nexsqaud.rgv.platform.forge1710.gui.RgvGuiScreen;

public class ClientProxy extends CommonProxy {

    private RgvScreenManager screenManager;

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);

        Forge1710Platform platform = new Forge1710Platform();
        RgvPlatform.setInstance(platform);

        screenManager = new RgvScreenManager(CommonProxy.getRecipeManager());
        Forge1710Platform.setScreenManager(screenManager);

        screenManager.getRecipeScreen().setListener(new RgvRecipeScreen.ScreenListener() {
            @Override
            public void onOpen() {
                Minecraft mc = Minecraft.getMinecraft();
                if (mc.currentScreen != null && !(mc.currentScreen instanceof RgvGuiScreen)) {
                    mc.displayGuiScreen(new RgvGuiScreen(mc.currentScreen, screenManager, screenManager.getRecipeScreen()));
                }
            }

            @Override
            public void onClose() {
                Minecraft mc = Minecraft.getMinecraft();
                if (mc.currentScreen instanceof RgvGuiScreen) {
                    RgvGuiScreen gui = (RgvGuiScreen) mc.currentScreen;
                    mc.displayGuiScreen(gui.getParentScreen());
                }
            }
        });

        MinecraftForge.EVENT_BUS.register(new GuiOverlayHooks(screenManager));
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);

        new ru.nexsqaud.rgv.platform.forge1710.compat.jei.JeiRecipeIntegration().register(screenManager.getRecipeManager());
        new ru.nexsqaud.rgv.platform.forge1710.compat.nei.NeiRecipeIntegration().register(screenManager.getRecipeManager());
        ru.nexsqaud.rgv.platform.forge1710.compat.nei.NeiInputBridge.register(screenManager);

        screenManager.init();
    }
}
