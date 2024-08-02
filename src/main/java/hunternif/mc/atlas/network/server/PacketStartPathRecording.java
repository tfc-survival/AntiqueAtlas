package hunternif.mc.atlas.network.server;

import hunternif.mc.atlas.RegistrarAntiqueAtlas;
import hunternif.mc.atlas.item.ItemAriadneThread;
import hunternif.mc.atlas.network.AbstractMessage;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.relauncher.Side;

import java.io.IOException;

public class PacketStartPathRecording extends AbstractMessage.AbstractServerMessage<PacketStartPathRecording> {
    private EnumHand hand;
    private long activeKey;

    public PacketStartPathRecording() {
    }

    public PacketStartPathRecording(EnumHand hand, long activeKey) {
        this.hand = hand;
        this.activeKey = activeKey;
    }

    @Override
    protected void read(PacketBuffer buffer) throws IOException {
        int i = buffer.readByte();
        hand = 0 <= i && i < EnumHand.values().length ? EnumHand.values()[i] : EnumHand.MAIN_HAND;
        activeKey = buffer.readLong();
    }

    @Override
    protected void write(PacketBuffer buffer) throws IOException {
        buffer.writeByte(hand.ordinal());
        buffer.writeLong(activeKey);
    }

    @Override
    protected void process(EntityPlayer player, Side side) {
        ItemStack heldItem = player.getHeldItem(hand);
        if (heldItem.getItem() == RegistrarAntiqueAtlas.ARIADNE_THREAD) {
            ItemAriadneThread.activate(heldItem, activeKey);
        }
    }
}
