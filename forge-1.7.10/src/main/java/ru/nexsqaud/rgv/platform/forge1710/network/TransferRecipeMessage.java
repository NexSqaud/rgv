package ru.nexsqaud.rgv.platform.forge1710.network;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import ru.nexsqaud.rgv.api.RgvRecipe;
import ru.nexsqaud.rgv.api.RgvRecipeHandler;
import ru.nexsqaud.rgv.platform.forge1710.Forge1710Inventory;
import ru.nexsqaud.rgv.platform.forge1710.Forge1710Platform;

/**
 * Sent from client to server to transfer recipe items into container slots.
 */
public class TransferRecipeMessage implements IMessage {

    private String recipeId;
    private boolean maxCraft;

    public TransferRecipeMessage() {
    }

    public TransferRecipeMessage(String recipeId, boolean maxCraft) {
        this.recipeId = recipeId != null ? recipeId : "";
        this.maxCraft = maxCraft;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.recipeId = ByteBufUtils.readUTF8String(buf);
        this.maxCraft = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, recipeId);
        buf.writeBoolean(maxCraft);
    }

    public static class Handler implements IMessageHandler<TransferRecipeMessage, IMessage> {
        @Override
        public IMessage onMessage(final TransferRecipeMessage message, final MessageContext ctx) {
            ru.nexsqaud.rgv.platform.forge1710.CommonProxy.addServerTask(new Runnable() {
                @Override
                @SuppressWarnings("unchecked")
                public void run() {
                    EntityPlayerMP player = ctx.getServerHandler().playerEntity;
                    if (player == null || player.openContainer == null) return;

                    Container container = player.openContainer;
                    RgvRecipe recipe = Forge1710Platform.findRecipeById(message.recipeId);
                    if (recipe == null) return;

                    RgvRecipeHandler handler = Forge1710Platform.getRecipeManager().getHandler(container);
                    if (handler != null && handler.canTransfer(recipe, container, new Forge1710Inventory(player.inventory))) {
                        handler.transfer(recipe, container, message.maxCraft);
                        container.detectAndSendChanges();
                    }
                }
            });
            return null;
        }
    }
}
