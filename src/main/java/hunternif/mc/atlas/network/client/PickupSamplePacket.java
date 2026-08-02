package hunternif.mc.atlas.network.client;

import blusunrize.immersiveengineering.api.tool.ExcavatorHandler;
import flaxbeard.immersivepetroleum.api.crafting.PumpjackHandler;
import hunternif.mc.atlas.AntiqueAtlasMod;
import hunternif.mc.atlas.api.AtlasAPI;
import hunternif.mc.atlas.map.objects.marker.DimensionMarkersData;
import hunternif.mc.atlas.map.objects.marker.Marker;
import hunternif.mc.atlas.map.objects.marker.MarkersData;
import hunternif.mc.atlas.network.AbstractMessage;
import hunternif.mc.atlas.util.TimeTFC;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;

import static hunternif.mc.atlas.AntiqueAtlasMod.ieMarkerEmpty;
import static hunternif.mc.atlas.RegistrarAntiqueAtlas.ATLAS;

public class PickupSamplePacket extends AbstractMessage.AbstractClientMessage<PickupSamplePacket> {
    public PickupSamplePacket() {
    }

    public PickupSamplePacket(long timestamp, String mineral, boolean infinite, int depletion, int oil, String resType, int x, int z) {
        this.timestamp = timestamp;
        this.mineral = mineral;
        this.infinite = infinite;
        this.depletion = depletion;
        this.oil = oil;
        this.resType = resType;
        this.x = x;
        this.z = z;
    }

    private long timestamp;
    private String mineral;
    private boolean infinite;
    private int depletion;
    private int oil;
    private String resType;

    private int x, z;

    @Override
    protected void read(PacketBuffer buffer) throws IOException {
        timestamp = buffer.readLong();
        mineral = buffer.readString(20);
        infinite = buffer.readBoolean();
        depletion = buffer.readInt();
        oil = buffer.readInt();
        resType = buffer.readString(20);
        x = buffer.readInt();
        z = buffer.readInt();
    }

    @Override
    protected void write(PacketBuffer buffer) throws IOException {
        buffer.writeLong(timestamp);
        buffer.writeString(mineral);
        buffer.writeBoolean(infinite);
        buffer.writeInt(depletion);
        buffer.writeInt(oil);
        buffer.writeString(resType);
        buffer.writeInt(x);
        buffer.writeInt(z);
    }

    @Override
    protected void process(EntityPlayer player, Side side) {
        ItemStack heldItemOffhand = player.getHeldItemOffhand();
        ItemStack heldItemMainhand = player.getHeldItemMainhand();
        int atlas =
                heldItemOffhand.getItem() == ATLAS ? heldItemOffhand.getItemDamage() :
                        heldItemMainhand.getItem() == ATLAS ? heldItemMainhand.getItemDamage() :
                                -1;

        String label = prepareLabel(timestamp, mineral, infinite, depletion, oil, resType);

        addSampleMarker(atlas, Minecraft.getMinecraft().world, x, z, label);
    }

    @SideOnly(Side.CLIENT)
    public static void addSampleMarker(int atlas, WorldClient world, int cx, int cz, String label) {
        DimensionMarkersData markersDataInDimension = AntiqueAtlasMod.markersData.getMarkersData(atlas, world).getMarkersDataInDimension(world.provider.getDimension());

        int x = (cx << 4) + 8;
        int z = (cz << 4) + 8;
        List<Marker> markersAtChunk = markersDataInDimension.getMarkersAtChunk(cx / MarkersData.CHUNK_STEP, cz / MarkersData.CHUNK_STEP);

        boolean needToAdd = true;

        if (markersAtChunk != null)
            for (Marker marker : markersAtChunk) {
                if (marker.getX() == x && marker.getZ() == z) {
                    if (marker.getLabel().startsWith("Глубинное месторождение: ")) {
                        if (marker.getLabel().equals(label)) {
                            needToAdd = false;
                        } else {
                            AtlasAPI.markers.deleteMarker(world, atlas, marker.getId());
                        }
                    }
                }
            }

        if (needToAdd)
            AtlasAPI.markers.putMarker(world, true, atlas, ieMarkerEmpty, label, x, z);
    }

    @SideOnly(Side.CLIENT)
    public static String prepareLabel(long timestamp, String mineral, boolean infinite, int depletion, int oil, String resType) {
        String label = "Глубинное месторождение: ";

        if (mineral.isEmpty())
            label += I18n.format("chat.immersiveengineering.info.coresample.noMineral");
        else {

            if (infinite)
                label += "\u221e ";
            else
                label += (ExcavatorHandler.mineralVeinCapacity - depletion) + " ";

            String unloc = "desc.immersiveengineering.info.mineral." + mineral;
            String s0 = I18n.format(unloc);
            label += unloc.equals(s0) ? mineral : s0;
        }

        label += ", ";

        if (oil > 0) {
            PumpjackHandler.ReservoirType res = null;
            for (PumpjackHandler.ReservoirType type : PumpjackHandler.reservoirList.keySet()) {
                if (resType.equals(type.name)) {
                    res = type;
                }
            }
            if (res != null) {
                int est = (oil / 1000) * 1000;
                String test = new DecimalFormat("#,###.##").format(est);
                Fluid f = FluidRegistry.getFluid(res.fluid);
                String fluidName = f.getLocalizedName(new FluidStack(f, 1));

                label += I18n.format("chat.immersivepetroleum.info.coresample.oil", test, fluidName);
            } else
                label += I18n.format("chat.immersivepetroleum.info.coresample.noOil");
        } else
            label += I18n.format("chat.immersivepetroleum.info.coresample.noOil");

        label += ", " + I18n.format("tfc.tooltip.date", TimeTFC.getTimeAndDate(timestamp));
        return label;
    }

}
