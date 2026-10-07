package ru.nexsqaud.rgv.platform.forge1710.network;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;

/**
 * Sent from client to server to cheat an item into inventory (creative / OP only).
 */
public class GiveItemMessage implements IMessage {

    private String itemId;
    private int meta;
    private int amount;

    public GiveItemMessage() {
    }

    public GiveItemMessage(String itemId, int meta, int amount) {
        this.itemId = itemId != null ? itemId : "";
        this.meta = meta;
        this.amount = amount;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.itemId = ByteBufUtils.readUTF8String(buf);
        this.meta = buf.readInt();
        this.amount = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, itemId);
        buf.writeInt(meta);
        buf.writeInt(amount);
    }

    public static class Handler implements IMessageHandler<GiveItemMessage, IMessage> {
        @Override
        public IMessage onMessage(final GiveItemMessage message, final MessageContext ctx) {
            ru.nexsqaud.rgv.platform.forge1710.CommonProxy.addServerTask(new Runnable() {
                @Override
                public void run() {
                    EntityPlayerMP player = ctx.getServerHandler().playerEntity;
                    if (player == null) return;

                    boolean isOp = MinecraftServer.getServer().getConfigurationManager().func_152596_g(player.getGameProfile());
                    boolean isCreative = player.capabilities.isCreativeMode;
                    if (!isOp && !isCreative) {
                        return;
                    }

                    Item item = (Item) Item.itemRegistry.getObject(message.itemId);
                    if (item == null) return;

                    int count = Math.max(1, Math.min(message.amount, item.getItemStackLimit()));
                    int meta = (message.meta < 0 || message.meta == 32767 || message.meta == Short.MAX_VALUE) ? 0 : message.meta;
                    ItemStack stack = new ItemStack(item, count, meta);

                    boolean added = player.inventory.addItemStackToInventory(stack);
                    if (!added || stack.stackSize > 0) {
                        EntityItem drop = player.dropPlayerItemWithRandomChoice(stack, false);
                        if (drop != null) {
                            drop.delayBeforeCanPickup = 0;
                        }
                    }
                    player.inventoryContainer.detectAndSendChanges();
                }
            });

            return null;
        }
    }
}
