package ru.nexsqaud.rgv.platform.forge1122;

import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import ru.nexsqaud.rgv.core.platform.RgvPlatform;
import ru.nexsqaud.rgv.core.screen.RgvRecipeScreen;
import ru.nexsqaud.rgv.core.screen.RgvScreenManager;
import ru.nexsqaud.rgv.platform.forge1122.gui.GuiOverlayHooks1122;
import ru.nexsqaud.rgv.platform.forge1122.gui.RgvGuiScreen1122;

public class ClientProxy1122 extends CommonProxy1122 {

    private RgvScreenManager screenManager;

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);

        Forge1122Platform platform = new Forge1122Platform();
        RgvPlatform.setInstance(platform);

        screenManager = new RgvScreenManager(CommonProxy1122.getRecipeManager());
        Forge1122Platform.setScreenManager(screenManager);
        RgvMod1122.setScreenManager(screenManager);

        screenManager.getRecipeScreen().setListener(new RgvRecipeScreen.ScreenListener() {
            @Override
            public void onOpen() {
                Minecraft mc = Minecraft.getMinecraft();
                if (mc.currentScreen != null && !(mc.currentScreen instanceof RgvGuiScreen1122)) {
                    mc.displayGuiScreen(new RgvGuiScreen1122(mc.currentScreen, screenManager, screenManager.getRecipeScreen()));
                }
            }

            @Override
            public void onClose() {
                Minecraft mc = Minecraft.getMinecraft();
                if (mc.currentScreen instanceof RgvGuiScreen1122) {
                    RgvGuiScreen1122 gui = (RgvGuiScreen1122) mc.currentScreen;
                    mc.displayGuiScreen(gui.getParentScreen());
                }
            }
        });

        MinecraftForge.EVENT_BUS.register(new GuiOverlayHooks1122(screenManager));
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);

        screenManager.init();
    }
}
