package ru.nexsqaud.rgv.platform.forge1710;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
        modid = RgvMod.MODID,
        name = RgvMod.NAME,
        version = RgvMod.VERSION,
        acceptedMinecraftVersions = "[1.7.10]",
        acceptableRemoteVersions = "*"
)
public class RgvMod {

    public static final String MODID = "rgv";
    public static final String NAME = "RGV";
    public static final String VERSION = "1.0.0";

    public static final Logger LOG = LogManager.getLogger(NAME);

    @Mod.Instance(MODID)
    public static RgvMod instance;

    @SidedProxy(
            clientSide = "ru.nexsqaud.rgv.platform.forge1710.ClientProxy",
            serverSide = "ru.nexsqaud.rgv.platform.forge1710.CommonProxy"
    )
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOG.info("RGV (Recipe Graph Viewer) pre-initializing on Minecraft 1.7.10...");
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        LOG.info("RGV initializing platform bridge and overlays...");
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        LOG.info("RGV post-initializing recipes and item index...");
        proxy.postInit(event);
        LOG.info("RGV initialized successfully!");
    }
}
