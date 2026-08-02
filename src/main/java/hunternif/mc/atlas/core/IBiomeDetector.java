package hunternif.mc.atlas.core;

import net.minecraft.world.chunk.Chunk;

/**
 * Finds the biome ID to be used for a given chunk.
 * @author Hunternif
 */
interface IBiomeDetector {
	int NOT_FOUND = -1;

	/** Finds the biome ID to be used for a given chunk. */
	int getBiomeID(Chunk chunk);

	/** Whether this detector reads world data that only exists on the server. Such a
	 * detector must not run on the client, which would reach different conclusions from
	 * the same chunk and fight the tiles the server sends over. */
	default boolean requiresServerData() {
		return false;
	}
}
