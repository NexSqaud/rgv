package ru.nexsqaud.rgv.platform.forge1710.network;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

/**
 * Initializes and manages network packets for RGV.
 */
public class RgvPacketHandler {

    public static final SimpleNetworkWrapper NETWORK = NetworkRegistry.INSTANCE.newSimpleChannel("rgv");

    public static void init() {
        NETWORK.registerMessage(TransferRecipeMessage.Handler.class, TransferRecipeMessage.class, 0, Side.SERVER);
        NETWORK.registerMessage(GiveItemMessage.Handler.class, GiveItemMessage.class, 1, Side.SERVER);
    }
}
