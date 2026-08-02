package hunternif.mc.atlas.core;

import hunternif.mc.atlas.AntiqueAtlasMod;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;
import su.tfcsurvival.TerraFirmaCraft;

/**
 * Entry point for the TerraFirmaCraft-specific parts of the map.
 * <p>
 * Everything that names a TFC class is reached through here and guarded by
 * {@link #loaded}, so on a game without TFC those symbols are never resolved.
 */
public class TFCSupport {
	private static boolean loaded;

	private static IBiomeDetector detector;

	/** Called from {@code CommonProxy.postInit}, alongside the other mod-presence checks,
	 * so that presence is sampled at a defined point in the mod lifecycle. */
	public static void init() {
		loaded = AntiqueAtlasMod.tfcIntegration && Loader.isModLoaded("tfc");
		if (loaded) {
			// Built here rather than on first use: worlds tick on more than one thread, so a
			// lazy singleton would be a data race. Guarded, so without TFC installed the
			// detector class -- and the TFC types it names -- are never even loaded.
			detector = new BiomeDetectorTFC();
		}
	}

	/**
	 * Whether this world's terrain was built by TFC.
	 * <p>
	 * Two things have to hold. TFC only attaches its per-chunk data to worlds using its
	 * own world type, so a TFC install alone isn't enough. And it only generates the
	 * overworld -- the Nether and the End read their world type from the save through
	 * {@code DerivedWorldInfo}, so they report TFC's world type too and would otherwise
	 * be taken away from the detectors written for them.
	 */
	public static boolean isTFCWorld(World world) {
		return loaded
				&& world.provider.getDimensionType() == DimensionType.OVERWORLD
				&& hasTFCWorldType(world);
	}

	/** Shared detector for TFC worlds. Only meaningful once {@link #isTFCWorld} has passed. */
	static IBiomeDetector getDetector() {
		return detector;
	}

	/** Kept separate so the reference to TFC is only resolved once {@link #loaded} has passed. */
	private static boolean hasTFCWorldType(World world) {
		return world.getWorldType() == TerraFirmaCraft.getWorldType();
	}
}
