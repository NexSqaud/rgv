package ru.nexsqaud.rgv.platform.forge1122.network;

import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Manages network packets for RGV on Minecraft 1.12.2.
 */
public class RgvPacketHandler1122 {

    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel("rgv1122");

    public static void init() {
        INSTANCE.registerMessage(TransferRecipeMessage1122.Handler.class, TransferRecipeMessage1122.class, 0, Side.SERVER);
        INSTANCE.registerMessage(GiveItemMessage1122.Handler.class, GiveItemMessage1122.class, 1, Side.SERVER);
    }
}
