package hunternif.mc.atlas.ext;

import hunternif.mc.atlas.client.TextureSet;

/**
 * The pseudo-biome tiles used to draw TerraFirmaCraft terrain, each paired with the
 * texture set that draws it.
 * <p>
 * TFC biomes only describe the <i>shape</i> of the land (ocean, hills, mountains,
 * mesa...); tree cover and climate live in {@code ChunkDataTFC} instead. So a tile
 * can't be picked from the biome alone -- the detector resolves both per chunk and
 * picks one of the constants below.
 * <p>
 * Name and texture live together here so that adding a tile is a single edit: the ID
 * registration and the texture registration both iterate {@link #values()}. Splitting
 * them would let a tile end up with an ID but no texture, or no ID at all, and a tile
 * with no ID silently vanishes from the map.
 *
 * @see hunternif.mc.atlas.core.BiomeDetectorTFC
 */
public enum TFCTiles {
	// Water & shores:
	WATER("tfcWater", TextureSet.WATER),
	ICE("tfcIce", TextureSet.ICE),
	SHORE("tfcShore", TextureSet.SHORE),
	ROCK_SHORE("tfcRockShore", TextureSet.ROCK_SHORE),

	// Wetlands:
	SWAMP("tfcSwamp", TextureSet.SWAMP),
	SWAMP_HILLS("tfcSwampHills", TextureSet.SWAMP_HILLS),

	// Mesa:
	MESA("tfcMesa", TextureSet.MESA),
	MESA_BRYCE("tfcMesaBryce", TextureSet.BRYCE),
	MESA_PLATEAU("tfcMesaPlateau", TextureSet.PLATEAU_MESA),
	MESA_PLATEAU_TREES("tfcMesaPlateauTrees", TextureSet.PLATEAU_MESA_TREES),

	// Treeless ground, by climate and elevation:
	DESERT("tfcDesert", TextureSet.DESERT),
	DESERT_HILLS("tfcDesertHills", TextureSet.DESERT_HILLS),
	SAVANNA("tfcSavanna", TextureSet.SAVANNA),
	SAVANNA_HILLS("tfcSavannaHills", TextureSet.SAVANNA_CLIFFS),
	GRASS("tfcGrass", TextureSet.PLAINS),
	MEADOW("tfcMeadow", TextureSet.SUNFLOWERS),
	HILLS("tfcHills", TextureSet.HILLS),
	SNOW("tfcSnow", TextureSet.SNOW),
	SNOW_HILLS("tfcSnowHills", TextureSet.SNOW_HILLS),
	MOUNTAINS("tfcMountains", TextureSet.MOUNTAINS_NAKED),
	MOUNTAINS_SNOW("tfcMountainsSnow", TextureSet.MOUNTAINS_SNOW_CAPS),
	MOUNTAINS_FOREST("tfcMountainsForest", TextureSet.MOUNTAINS_ALL),

	// Broadleaf forest, by density and elevation:
	FOREST_SPARSE("tfcForestSparse", TextureSet.SPARSE_FOREST),
	FOREST_SPARSE_HILLS("tfcForestSparseHills", TextureSet.SPARSE_FOREST_HILLS),
	FOREST("tfcForest", TextureSet.FOREST),
	FOREST_HILLS("tfcForestHills", TextureSet.FOREST_HILLS),
	FOREST_DENSE("tfcForestDense", TextureSet.DENSE_FOREST),
	FOREST_DENSE_HILLS("tfcForestDenseHills", TextureSet.DENSE_FOREST_HILLS),

	// Birch / aspen forest:
	BIRCH("tfcBirch", TextureSet.BIRCH),
	BIRCH_HILLS("tfcBirchHills", TextureSet.BIRCH_HILLS),
	BIRCH_DENSE("tfcBirchDense", TextureSet.DENSE_BIRCH),

	// Coniferous forest:
	CONIFER("tfcConifer", TextureSet.PINES),
	CONIFER_HILLS("tfcConiferHills", TextureSet.PINES_HILLS),
	CONIFER_DENSE("tfcConiferDense", TextureSet.MEGA_TAIGA),
	CONIFER_DENSE_HILLS("tfcConiferDenseHills", TextureSet.MEGA_TAIGA_HILLS),
	CONIFER_SNOW("tfcConiferSnow", TextureSet.SNOW_PINES),
	CONIFER_SNOW_HILLS("tfcConiferSnowHills", TextureSet.SNOW_PINES_HILLS),

	// Tropical forest:
	JUNGLE("tfcJungle", TextureSet.JUNGLE),
	JUNGLE_HILLS("tfcJungleHills", TextureSet.JUNGLE_HILLS),
	JUNGLE_SPARSE("tfcJungleSparse", TextureSet.JUNGLE_EDGE),
	JUNGLE_SPARSE_HILLS("tfcJungleSparseHills", TextureSet.JUNGLE_EDGE_HILLS);

	/** Unique name this tile is registered and saved under. */
	public final String tileName;

	/** Texture set that draws it. Only read on the client. */
	public final TextureSet texture;

	TFCTiles(String tileName, TextureSet texture) {
		this.tileName = tileName;
		this.texture = texture;
	}
}
