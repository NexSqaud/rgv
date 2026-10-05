package ru.nexsqaud.rgv.platform.forge1122;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.nexsqaud.rgv.core.recipe.RgvRecipeManager;
import ru.nexsqaud.rgv.core.screen.RgvScreenManager;

@Mod(
        modid = RgvMod1122.MODID,
        name = RgvMod1122.NAME,
        version = RgvMod1122.VERSION,
        acceptedMinecraftVersions = "[1.12.2]",
        acceptableRemoteVersions = "*"
)
public class RgvMod1122 {

    public static final String MODID = "rgv";
    public static final String NAME = "Recipe Graph Viewer";
    public static final String VERSION = "1.0.0";

    public static final Logger LOG = LogManager.getLogger(NAME);

    @Mod.Instance(MODID)
    public static RgvMod1122 instance;

    @SidedProxy(
            clientSide = "ru.nexsqaud.rgv.platform.forge1122.ClientProxy1122",
            serverSide = "ru.nexsqaud.rgv.platform.forge1122.CommonProxy1122"
    )
    public static CommonProxy1122 proxy;

    private static RgvScreenManager screenManager;

    public static void setScreenManager(RgvScreenManager sm) {
        screenManager = sm;
    }

    public static RgvScreenManager getScreenManager() {
        return screenManager;
    }

    public static RgvRecipeManager getRecipeManager() {
        return screenManager != null ? screenManager.getRecipeManager() : null;
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOG.info("RGV (Recipe Graph Viewer) pre-initializing on Minecraft 1.12.2...");
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        LOG.info("RGV initializing platform bridge and overlays for 1.12.2...");
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        LOG.info("RGV post-initializing recipes and item index for 1.12.2...");
        proxy.postInit(event);
        LOG.info("RGV 1.12.2 initialized successfully!");
    }
}
