package ru.nexsqaud.rgv.platform.forge1710;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import ru.nexsqaud.rgv.core.platform.RgvPlatform;
import ru.nexsqaud.rgv.core.recipe.RgvRecipeManager;
import ru.nexsqaud.rgv.platform.forge1710.network.RgvPacketHandler;
import ru.nexsqaud.rgv.platform.forge1710.recipe.VanillaRecipesPlugin;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class CommonProxy {

    private static final RgvRecipeManager RECIPE_MANAGER = new RgvRecipeManager();
    private static final Queue<Runnable> SERVER_TASKS = new ConcurrentLinkedQueue<>();

    public static RgvRecipeManager getRecipeManager() {
        return RECIPE_MANAGER;
    }

    public static void addServerTask(Runnable task) {
        if (task != null) {
            SERVER_TASKS.add(task);
        }
    }

    public void preInit(FMLPreInitializationEvent event) {
        RgvPacketHandler.init();
    }

    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance().bus().register(this);
        if (RgvPlatform.get() == null) {
            RgvPlatform.setInstance(new Forge1710Platform());
        }
    }

    public void postInit(FMLPostInitializationEvent event) {
        new VanillaRecipesPlugin().register(RECIPE_MANAGER);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Runnable task;
            while ((task = SERVER_TASKS.poll()) != null) {
                try {
                    task.run();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
    }
}
