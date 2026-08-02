package hunternif.mc.atlas.core;

import hunternif.mc.atlas.SettingsConfig;
import hunternif.mc.atlas.ext.ExtTileIdMap;
import hunternif.mc.atlas.ext.TFCTiles;
import hunternif.mc.atlas.util.ByteUtil;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import su.tfcsurvival.api.types.Tree;
import su.tfcsurvival.world.classic.biomes.BiomesTFC;
import su.tfcsurvival.world.classic.chunkdata.ChunkDataTFC;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Detects tiles in a TerraFirmaCraft world.
 * <p>
 * TFC biomes only carry the shape of the land, so a biome alone can't say whether
 * a chunk is bare steppe or old-growth forest. Tree cover, rainfall and temperature
 * live in {@link ChunkDataTFC} instead, and this detector combines the two:
 * the biome (plus terrain elevation) picks the landform, and the chunk's climate
 * plus its dominant tree species picks the vegetation drawn on top of it.
 *
 * @see TFCTiles
 */
public class BiomeDetectorTFC extends BiomeDetectorBase {

	/** Terrain elevation above sea level, in blocks, at which a chunk reads as hilly / mountainous. */
	private static final int HILLS_ELEVATION = 16, MOUNTAINS_ELEVATION = 38;

	/** A river is usually much narrower than a chunk, so it must not compete as a normal majority biome. */
	private static final int MIN_RIVER_SAMPLES = 6, MIN_EDGE_RIVER_SAMPLES = 4;

	/** Yearly average temperature, in degrees C. TFC's own range is roughly -32..35. */
	private static final float TEMP_FROZEN = -2f, TEMP_COLD = 7f;

	/** Yearly rainfall, in mm. TFC's own range is 0..500. */
	private static final float RAIN_ARID = 90f, RAIN_DRY = 180f, RAIN_LUSH = 300f;

	/**
	 * Number of trees TFC will actually place in a chunk, at the boundaries between open
	 * ground, thin woodland and closed canopy.
	 * Mirrors {@code WorldGenTrees}: {@code treesPerChunk = floraDensity * 16 - 2}.
	 */
	private static final int TREES_OPEN = 1, TREES_SPARSE = 3, TREES_DENSE = 8;

	public BiomeDetectorTFC() {
		// Only the ungenerated-chunk fallback inherited from BiomeDetectorBase scans for
		// these, but it should still honour the same settings the overworld detector does.
		setScanPonds(SettingsConfig.performance.doScanPonds);
		setScanRavines(SettingsConfig.performance.doScanRavines);
	}

	@Override
	public boolean requiresServerData() {
		return true;
	}

	@Override
	public int getBiomeID(Chunk chunk) {
		ChunkDataTFC data = ChunkDataTFC.get(chunk);
		if (!data.isInitialized()) {
			// No climate or flora data: either TFC has not finished generating this chunk,
			// or it predates the save being a TFC world and never will have any. Draw it
			// from its biome alone, the way the map looked before TFC was involved.
			return super.getBiomeID(chunk);
		}

		Biome biome = riverBiomeIfPresent(chunk);
		if (biome == null) {
			biome = dominantBiome(chunk);
		}
		if (biome == null) {
			// Not a single resolvable biome in the chunk. Defer rather than reporting
			// NOT_FOUND, which would make the caller erase a tile that is already drawn.
			return super.getBiomeID(chunk);
		}

		float temperature = data.getAverageTemp();

		// Water reads first: shorelines and rivers are what make a map legible.
		if (BiomesTFC.isOceanicBiome(biome) || BiomesTFC.isRiverBiome(biome) || BiomesTFC.isLakeBiome(biome)) {
			return tile(temperature < TEMP_FROZEN ? TFCTiles.ICE : TFCTiles.WATER);
		}
		if (biome == BiomesTFC.GRAVEL_BEACH) {
			return tile(TFCTiles.ROCK_SHORE);
		}
		if (BiomesTFC.isBeachBiome(biome)) {
			return tile(TFCTiles.SHORE);
		}

		int elevation = averageElevation(data);
		boolean mountains = isMountainous(biome, elevation);
		boolean hills = isHilly(biome, elevation);

		if (BiomesTFC.isSwampBiome(biome)) {
			return tile(hills ? TFCTiles.SWAMP_HILLS : TFCTiles.SWAMP);
		}

		int trees = (int) (data.getFloraDensity() * 16 - 2);

		// Compared by identity rather than through BiomesTFC.isMesaBiome, which rolls a
		// fresh Random on every call and so would flicker the tile between rescans.
		if (biome == BiomesTFC.MESA_PLATEAU || biome == BiomesTFC.MESA_PLATEAU_M) {
			return tile(trees > TREES_OPEN ? TFCTiles.MESA_PLATEAU_TREES : TFCTiles.MESA_PLATEAU);
		}

		// Above the treeline the landform itself is the story, not what grows on it.
		if (mountains) {
			if (temperature < TEMP_FROZEN) {
				return tile(TFCTiles.MOUNTAINS_SNOW);
			}
			return tile(trees > TREES_OPEN && dominantTree(data) != null
					? TFCTiles.MOUNTAINS_FOREST : TFCTiles.MOUNTAINS);
		}

		// Tree count is checked before the species, because resolving the species is the
		// expensive part and open ground doesn't need it.
		if (trees > TREES_OPEN) {
			Tree dominant = dominantTree(data);
			if (dominant != null) {
				return tile(forest(dominant, temperature, trees, hills));
			}
		}
		return tile(openGround(temperature, data.getRainfall(), hills));
	}

	/** Tile for a chunk that TFC leaves (nearly) treeless. */
	private TFCTiles openGround(float temperature, float rainfall, boolean hills) {
		if (temperature < TEMP_FROZEN) {
			return hills ? TFCTiles.SNOW_HILLS : TFCTiles.SNOW;
		}
		if (rainfall < RAIN_ARID) {
			return hills ? TFCTiles.DESERT_HILLS : TFCTiles.DESERT;
		}
		if (rainfall < RAIN_DRY && temperature > TEMP_COLD) {
			return hills ? TFCTiles.SAVANNA_HILLS : TFCTiles.SAVANNA;
		}
		if (hills) {
			return TFCTiles.HILLS;
		}
		// Open ground this wet is meadow rather than dry steppe. Keyed off rainfall and not
		// flora density, because reaching this method already means the density was too low
		// to grow trees, so it can't also be high enough to mean anything here.
		return rainfall > RAIN_LUSH ? TFCTiles.MEADOW : TFCTiles.GRASS;
	}

	/** Tile for a wooded chunk, keyed off the species TFC actually plants there. */
	private TFCTiles forest(Tree dominant, float temperature, int trees, boolean hills) {
		boolean dense = trees >= TREES_DENSE;
		boolean sparse = trees <= TREES_SPARSE;

		switch (dominant.leavesType) {
			case conifers:
				if (temperature < TEMP_FROZEN) {
					return hills ? TFCTiles.CONIFER_SNOW_HILLS : TFCTiles.CONIFER_SNOW;
				}
				if (dense) {
					return hills ? TFCTiles.CONIFER_DENSE_HILLS : TFCTiles.CONIFER_DENSE;
				}
				return hills ? TFCTiles.CONIFER_HILLS : TFCTiles.CONIFER;

			case jungle:
				if (sparse) {
					return hills ? TFCTiles.JUNGLE_SPARSE_HILLS : TFCTiles.JUNGLE_SPARSE;
				}
				return hills ? TFCTiles.JUNGLE_HILLS : TFCTiles.JUNGLE;

			case deciduous:
			default:
				if (dominant == Tree.BIRCH || dominant == Tree.ASPEN) {
					if (dense) return TFCTiles.BIRCH_DENSE;
					return hills ? TFCTiles.BIRCH_HILLS : TFCTiles.BIRCH;
				}
				// No snowed-over broadleaf art exists, and under a yearly average this cold
				// the ground is white most of the year anyway, so the snowy set reads truer
				// than a green canopy would.
				if (temperature < TEMP_FROZEN) {
					return hills ? TFCTiles.CONIFER_SNOW_HILLS : TFCTiles.CONIFER_SNOW;
				}
				if (dense) {
					return hills ? TFCTiles.FOREST_DENSE_HILLS : TFCTiles.FOREST_DENSE;
				}
				if (sparse) {
					return hills ? TFCTiles.FOREST_SPARSE_HILLS : TFCTiles.FOREST_SPARSE;
				}
				return hills ? TFCTiles.FOREST_HILLS : TFCTiles.FOREST;
		}
	}

	/**
	 * Whether the chunk should be drawn as mountains. TFC's landform biomes and the
	 * terrain it actually built can disagree -- the edge of a mountain range is still
	 * flat ground, and a plains biome can throw up a tall ridge -- so a chunk needs to
	 * both be named and be shaped like a mountain, or be high enough that it doesn't
	 * matter what the biome claims.
	 */
	private boolean isMountainous(Biome biome, int elevation) {
		return elevation >= MOUNTAINS_ELEVATION
				|| (BiomesTFC.isMountainBiome(biome) && elevation >= HILLS_ELEVATION);
	}

	/** Whether the chunk should be drawn with a rolling, broken skyline. */
	private boolean isHilly(Biome biome, int elevation) {
		return elevation >= HILLS_ELEVATION
				|| BiomesTFC.isMountainBiome(biome)
				|| biome == BiomesTFC.HIGH_HILLS
				|| biome == BiomesTFC.HIGH_HILLS_EDGE
				|| biome == BiomesTFC.ROLLING_HILLS
				|| biome == BiomesTFC.HIGH_PLAINS;
	}

	/**
	 * The species TFC would place most of in this chunk. {@code WorldGenTrees} takes the
	 * dominance-sorted list of valid trees and rotates it by the chunk's flora diversity
	 * before favouring the head of the list, so the same rotation is applied here --
	 * otherwise the map would show the dominant species rather than the one on the ground.
	 */
	private Tree dominantTree(ChunkDataTFC data) {
		List<Tree> trees = data.getValidTrees();
		if (trees.isEmpty()) {
			return null;
		}
		Collections.rotate(trees, -(int) (data.getFloraDiversity() * (trees.size() - 1f)));
		return trees.get(0);
	}

	/**
	 * Mean height of the stone surface above sea level. Read from TFC's own sea level
	 * offset rather than the vanilla height map, which would count trees as terrain and
	 * turn every dense forest into hills.
	 */
	private int averageElevation(ChunkDataTFC data) {
		int total = 0;
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				total += data.getSeaLevelOffset(x, z);
			}
		}
		return total / 256;
	}

	/** Most common biome in the chunk, weighting water and beaches up so coastlines stay connected. */
	private Biome dominantBiome(Chunk chunk) {
		int[] chunkBiomes;
		try {
			chunkBiomes = ByteUtil.unsignedByteToIntArray(biomeArrayMethod.invoke(chunk));
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}

		Map<Biome, Integer> occurrences = new HashMap<>();
		for (int chunkBiome : chunkBiomes) {
			Biome biome = Biome.getBiomeForId(chunkBiome);
			if (biome != null) {
				occurrences.merge(biome, priorityForBiome(biome), Integer::sum);
			}
		}
		return occurrences.isEmpty() ? null
				: Collections.max(occurrences.entrySet(), Comparator.comparingInt(Map.Entry::getValue)).getKey();
	}

	/**
	 * TFC rivers are thin enough that even with water priority they often lose the
	 * chunk vote to the surrounding land. Promote a chunk to river when the river
	 * covers a meaningful slice of the chunk, or when it reaches multiple edges.
	 */
	private Biome riverBiomeIfPresent(Chunk chunk) {
		int[] chunkBiomes;
		try {
			chunkBiomes = ByteUtil.unsignedByteToIntArray(biomeArrayMethod.invoke(chunk));
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}

		int riverSamples = 0;
		boolean north = false, south = false, west = false, east = false;
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				Biome biome = Biome.getBiomeForId(chunkBiomes[x << 4 | z]);
				if (BiomesTFC.isRiverBiome(biome)) {
					riverSamples++;
					north |= z == 0;
					south |= z == 15;
					west |= x == 0;
					east |= x == 15;
				}
			}
		}

		if (riverSamples >= MIN_RIVER_SAMPLES
				|| (riverSamples >= MIN_EDGE_RIVER_SAMPLES && touchesMultipleEdges(north, south, west, east))) {
			return BiomesTFC.RIVER;
		}
		return null;
	}

	private boolean touchesMultipleEdges(boolean north, boolean south, boolean west, boolean east) {
		int edges = 0;
		if (north) edges++;
		if (south) edges++;
		if (west) edges++;
		if (east) edges++;
		return edges >= 2;
	}

	private int tile(TFCTiles tile) {
		int id = ExtTileIdMap.instance().getPseudoBiomeID(tile.tileName);
		return id == ExtTileIdMap.NOT_FOUND ? NOT_FOUND : id;
	}
}
