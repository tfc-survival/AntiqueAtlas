package hunternif.mc.atlas.core;

import hunternif.mc.atlas.AntiqueAtlasMod;
import hunternif.mc.atlas.map.objects.marker.MarkersData;
import hunternif.mc.atlas.map.objects.path.DimensionPathsData;
import hunternif.mc.atlas.map.objects.path.PathsData;
import hunternif.mc.atlas.network.client.PathsPacket;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;

public class PlayerEventHandler {
    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        EntityPlayer player = event.player;
        World world = player.world;
        int atlasID = player.getUniqueID().hashCode();

        AtlasData data = AntiqueAtlasMod.atlasData.getAtlasData(atlasID, world);
        // On the player join send the map from the server to the client:
        if (!data.isEmpty()) {
            data.syncOnPlayer(atlasID, player);
        }

        // Same thing with the local markers:
        MarkersData markers = AntiqueAtlasMod.markersData.getMarkersData(atlasID, world);
        if (!markers.isEmpty()) {
            markers.syncOnPlayer(atlasID, player);
        }

        AntiqueAtlasMod.pathsData.getOrCreate(atlasID, world).syncOnPlayer(player);
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // Fires at both the start and the end of every tick, and player.ticksExisted is the
        // same for both, so without this the scan gate opens twice and the whole chunk scan
        // runs back to back.
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        int atlasID = event.player.getUniqueID().hashCode();
        AtlasData data = AntiqueAtlasMod.atlasData.getAtlasData(atlasID, event.player.world);

        // Updating map around player
        ArrayList<TileInfo> newTiles = data.updateMapAroundPlayer(event.player);
        // Normally both sides scan and reach the same tiles, so sending them would be pure
        // duplicate traffic. Detectors that read server-only world data are the exception:
        // there the client skips the scan entirely and has nothing but these packets.
        if (data.needsServerTileSync(event.player.getEntityWorld())) {
            AtlasData.sendTilesToPlayer(atlasID, event.player, newTiles);
        }
    }
}