package ru.nexsqaud.rgv.platform.forge1122.compat.jei;

import mezz.jei.api.IJeiRuntime;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;

@JEIPlugin
public class RgvJeiPlugin implements IModPlugin {

    @Override
    public void register(IModRegistry registry) {
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        JeiIntegration.init(jeiRuntime);
    }
}
