package ru.nexsqaud.rgv.platform.forge1122.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/**
 * Sent from client to server to cheat an item into inventory in 1.12.2.
 */
public class GiveItemMessage1122 implements IMessage {

    private String itemId;
    private int meta;
    private int amount;

    public GiveItemMessage1122() {
    }

    public GiveItemMessage1122(String itemId, int meta, int amount) {
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

    public static class Handler implements IMessageHandler<GiveItemMessage1122, IMessage> {
        @Override
        public IMessage onMessage(GiveItemMessage1122 message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            if (player == null) return null;

            player.getServerWorld().addScheduledTask(() -> {
                boolean isOp = player.getServer() != null && player.getServer().getPlayerList().canSendCommands(player.getGameProfile());
                boolean isCreative = player.capabilities.isCreativeMode;
                if (!isOp && !isCreative) {
                    return;
                }

                ResourceLocation loc = null;
                try {
                    if (message.itemId != null && !message.itemId.isEmpty()) {
                        loc = new ResourceLocation(message.itemId);
                    }
                } catch (Throwable ignored) {
                }
                Item item = loc != null ? ForgeRegistries.ITEMS.getValue(loc) : null;
                if (item == null) {
                    item = Item.getByNameOrId(message.itemId);
                }
                if (item == null) return;

                int count = Math.max(1, Math.min(message.amount, item.getItemStackLimit()));
                int meta = (message.meta < 0 || message.meta == 32767 || message.meta == Short.MAX_VALUE) ? 0 : message.meta;
                ItemStack stack = new ItemStack(item, count, meta);
                if (stack.isEmpty()) return;

                boolean added = player.inventory.addItemStackToInventory(stack);
                if (!added) {
                    EntityItem entityItem = player.dropItem(stack, false);
                    if (entityItem != null) {
                        entityItem.setNoPickupDelay();
                        entityItem.setOwner(player.getName());
                    }
                }
                player.openContainer.detectAndSendChanges();
            });

            return null;
        }
    }
}
