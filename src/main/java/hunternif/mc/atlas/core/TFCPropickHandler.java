package hunternif.mc.atlas.core;

import hunternif.mc.atlas.AntiqueAtlasMod;
import hunternif.mc.atlas.api.AtlasAPI;
import hunternif.mc.atlas.map.objects.marker.DimensionMarkersData;
import hunternif.mc.atlas.map.objects.marker.Marker;
import hunternif.mc.atlas.map.objects.marker.MarkersData;
import hunternif.mc.atlas.registry.TFCMarkerTypes;
import net.dries007.tfc.api.events.ProspectEvent;
import net.dries007.tfc.objects.blocks.stone.BlockOreTFC;
import net.dries007.tfc.objects.items.tool.ItemProspectorPick.ProspectResult;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

import static hunternif.mc.atlas.RegistrarAntiqueAtlas.ATLAS;
import static hunternif.mc.atlas.registry.TFCMarkerTypes.gradePrefix;

@SideOnly(Side.CLIENT)
public class TFCPropickHandler {

    @Optional.Method(modid = "tfc")
    @SubscribeEvent
    public void onUsePropick(ProspectEvent.Client event) {
        ProspectResult.Type type = event.getResultType();
        if (type == ProspectResult.Type.FOUND) {
            EntityPlayer player = event.getPlayer();
            ItemStack mainhand = player.getHeldItemMainhand();
            ItemStack offhand = player.getHeldItemOffhand();

            ItemStack atlasStack = mainhand.getItem() == ATLAS ? mainhand : offhand.getItem() == ATLAS ? offhand : ItemStack.EMPTY;
            if (!atlasStack.isEmpty()) {
                int atlas = atlasStack.getItemDamage();
                BlockPos target = event.getBlockPos();
                IBlockState oreState = player.world.getBlockState(target);
                Block oreBlock = oreState.getBlock();
                if (oreBlock instanceof BlockOreTFC)
                    addOreMaker(
                            atlas, player.world, target,
                            TFCMarkerTypes.getRegistryName(((BlockOreTFC) oreBlock).ore, gradePrefix(oreState.getValue(BlockOreTFC.GRADE))),
                            oreState, oreBlock
                    );

            }
        }

    }

    private static void addOreMaker(int atlas, World world, BlockPos target, String icon, IBlockState oreState, Block oreBlock) {
        if (!isMarkerAlreadyExists(atlas, world, target, icon))
            AtlasAPI.markers.putMarker(
                    world, true, atlas, icon,
                    new ItemStack(
                            oreBlock.getItemDropped(oreState, null, 0),
                            1,
                            oreBlock.damageDropped(oreState)
                    ).getDisplayName(),
                    target.getX(), target.getZ());

    }

    private static boolean isMarkerAlreadyExists(int atlas, World world, BlockPos target, String icon) {
        DimensionMarkersData markersDataInDimension = AntiqueAtlasMod.markersData.getMarkersData(atlas, world).getMarkersDataInDimension(world.provider.getDimension());

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                List<Marker> markersAtChunk = markersDataInDimension.getMarkersAtChunk((target.getX() >> 4) / MarkersData.CHUNK_STEP + x, (target.getZ() >> 4) / MarkersData.CHUNK_STEP + z);
                if (markersAtChunk != null)
                    for (Marker marker : markersAtChunk) {
                        if (marker.getTypeRaw().equals(icon) && distanceSq(marker, target) < 16 * 16)
                            return true;
                    }
            }
        }
        return false;
    }

    private static int distanceSq(Marker marker, BlockPos target) {
        int dx = marker.getX() - target.getX();
        int dz = marker.getZ() - target.getZ();
        return dx * dx + dz * dz;
    }
}
