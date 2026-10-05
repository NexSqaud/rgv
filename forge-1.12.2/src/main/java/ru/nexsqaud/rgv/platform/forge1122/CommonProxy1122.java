package ru.nexsqaud.rgv.platform.forge1122;

import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import ru.nexsqaud.rgv.core.platform.RgvPlatform;
import ru.nexsqaud.rgv.core.recipe.RgvRecipeManager;
import ru.nexsqaud.rgv.platform.forge1122.network.RgvPacketHandler1122;
import ru.nexsqaud.rgv.platform.forge1122.recipe.VanillaRecipesPlugin1122;

public class CommonProxy1122 {

    private static final RgvRecipeManager RECIPE_MANAGER = new RgvRecipeManager();

    public static RgvRecipeManager getRecipeManager() {
        return RECIPE_MANAGER;
    }

    public void preInit(FMLPreInitializationEvent event) {
        RgvPacketHandler1122.init();
    }

    public void init(FMLInitializationEvent event) {
        if (RgvPlatform.get() == null) {
            RgvPlatform.setInstance(new Forge1122Platform());
        }
    }

    public void postInit(FMLPostInitializationEvent event) {
        new VanillaRecipesPlugin1122().register(RECIPE_MANAGER);
    }
}
