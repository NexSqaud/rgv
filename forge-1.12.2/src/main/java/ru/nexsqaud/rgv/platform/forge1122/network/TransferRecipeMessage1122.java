package ru.nexsqaud.rgv.platform.forge1122.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import ru.nexsqaud.rgv.api.RgvRecipe;
import ru.nexsqaud.rgv.api.RgvRecipeHandler;
import ru.nexsqaud.rgv.platform.forge1122.Forge1122Inventory;
import ru.nexsqaud.rgv.platform.forge1122.Forge1122Platform;
import ru.nexsqaud.rgv.platform.forge1122.RgvMod1122;

/**
 * Sent from client to server to transfer recipe items into container slots in 1.12.2.
 */
public class TransferRecipeMessage1122 implements IMessage {

    private String recipeId;
    private boolean maxCraft;

    public TransferRecipeMessage1122() {
    }

    public TransferRecipeMessage1122(String recipeId, boolean maxCraft) {
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

    public static class Handler implements IMessageHandler<TransferRecipeMessage1122, IMessage> {
        @Override
        @SuppressWarnings("unchecked")
        public IMessage onMessage(TransferRecipeMessage1122 message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            if (player == null) return null;

            player.getServerWorld().addScheduledTask(() -> {
                Container container = player.openContainer;
                if (container == null) return;
                RgvRecipe recipe = Forge1122Platform.findRecipeById(message.recipeId);
                if (recipe == null) return;

                RgvRecipeHandler handler = Forge1122Platform.getRecipeManager() != null ? Forge1122Platform.getRecipeManager().getHandler(container) : null;
                if (handler != null && handler.canTransfer(recipe, container, new Forge1122Inventory(player.inventory))) {
                    handler.transfer(recipe, container, message.maxCraft);
                    container.detectAndSendChanges();
                }
            });

            return null;
        }
    }
}
