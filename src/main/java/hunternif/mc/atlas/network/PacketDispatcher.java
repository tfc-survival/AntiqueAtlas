package hunternif.mc.atlas.network;

import hunternif.mc.atlas.AntiqueAtlasMod;
import hunternif.mc.atlas.network.bidirectional.DeleteMarkerPacket;
import hunternif.mc.atlas.network.bidirectional.PutBiomeTilePacket;
import hunternif.mc.atlas.network.client.*;
import hunternif.mc.atlas.network.server.AddMarkerPacket;
import hunternif.mc.atlas.network.server.BrowsingPositionPacket;
import hunternif.mc.atlas.network.server.RegisterTileIdPacket;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/**
 *
 * A wrapper class for the SimpleNetworkWrapper
 *
 */
public class PacketDispatcher
{
	private static byte packetId = 0;

	private static final SimpleNetworkWrapper dispatcher = NetworkRegistry.INSTANCE.newSimpleChannel(AntiqueAtlasMod.CHANNEL);

	/** No need to allow instantiation */
	private PacketDispatcher() {}

	/**
	 *  Registers all packets and handlers - call this during {@link FMLPreInitializationEvent}
	 */
	public static void registerPackets() {
		//ORDER IS IMPORTANT! DON'T INSERT IN THE MIDDLE!
		registerMessage(DeleteMarkerPacket.class);
		registerMessage(PutBiomeTilePacket.class);
		registerMessage(MapDataPacket.class);
		registerMessage(TileGroupsPacket.class);
		registerMessage(ShortDimensionUpdatePacket.class);
		registerMessage(TileNameIDPacket.class);
		registerMessage(ShortTilesPacket.class);
		registerMessage(MarkersPacket.class);
		registerMessage(DeleteCustomGlobalTilePacket.class);
		registerMessage(AddMarkerPacket.class);
		registerMessage(RegisterTileIdPacket.class);
		registerMessage(BrowsingPositionPacket.class);
		registerMessage(IntTilesPacket.class);
		registerMessage(IntDimensionUpdatePacket.class);
		registerMessage(OptionalMarkerPacket.class);

		if(Loader.isModLoaded("immersiveengineering"))
			registerMessage(PickupSamplePacket.class);
	}

	/**
	 * Registers an {@link AbstractMessage} to the appropriate side(s)
	 */
	private static <T extends AbstractMessage<T> & IMessageHandler<T, IMessage>> void registerMessage(Class<T> clazz) {
		if (AbstractMessage.AbstractClientMessage.class.isAssignableFrom(clazz)) {
			PacketDispatcher.dispatcher.registerMessage(clazz, clazz, packetId++, Side.CLIENT);
		} else if (AbstractMessage.AbstractServerMessage.class.isAssignableFrom(clazz)) {
			PacketDispatcher.dispatcher.registerMessage(clazz, clazz, packetId++, Side.SERVER);
		} else {
			PacketDispatcher.dispatcher.registerMessage(clazz, clazz, packetId, Side.CLIENT);
			PacketDispatcher.dispatcher.registerMessage(clazz, clazz, packetId++, Side.SERVER);
		}
	}

	/**
	 * Send this message to everyone.
	 * See {@link SimpleNetworkWrapper#sendToAll(IMessage)}
	 */
	public static void sendToAll(IMessage message) {
		PacketDispatcher.dispatcher.sendToAll(message);
	}

	/**
	 * Send this message to the specified player.
	 * See {@link SimpleNetworkWrapper#sendTo(IMessage, EntityPlayerMP)}
	 */
	public static void sendTo(IMessage message, EntityPlayerMP player) {
		PacketDispatcher.dispatcher.sendTo(message, player);
	}

	/**
	 * Send this message to everyone within a certain range of a point.
	 * See {@link SimpleNetworkWrapper#sendToAllAround(IMessage, NetworkRegistry.TargetPoint)}
	 */
	private static void sendToAllAround(IMessage message, NetworkRegistry.TargetPoint point) {
		PacketDispatcher.dispatcher.sendToAllAround(message, point);
	}

	/**
	 * Sends a message to everyone within a certain range of the coordinates in the same dimension.
	 */
	private static void sendToAllAround(IMessage message, int dimension, double x, double y, double z, double range) {
		PacketDispatcher.sendToAllAround(message, new NetworkRegistry.TargetPoint(dimension, x, y, z, range));
	}

	/**
	 * Sends a message to everyone within a certain range of the player provided.
	 */
	public static void sendToAllAround(IMessage message, EntityPlayer player, double range) {
		PacketDispatcher.sendToAllAround(message, player.getEntityWorld().provider.getDimension(), player.posX, player.posY, player.posZ, range);
	}

	/**
	 * Send this message to everyone within the supplied dimension.
	 * See {@link SimpleNetworkWrapper#sendToDimension(IMessage, int)}
	 */
	public static void sendToDimension(IMessage message, int dimensionId) {
		PacketDispatcher.dispatcher.sendToDimension(message, dimensionId);
	}

	/**
	 * Send this message to the server.
	 * See {@link SimpleNetworkWrapper#sendToServer(IMessage)}
	 */
	public static void sendToServer(IMessage message) {
		PacketDispatcher.dispatcher.sendToServer(message);
	}
}
