package hunternif.mc.atlas;

import hunternif.mc.atlas.item.ItemAriadneThread;
import hunternif.mc.atlas.item.ItemAstrolabe;
import hunternif.mc.atlas.item.ItemAtlas;
import hunternif.mc.atlas.item.ItemEmptyAtlas;
import hunternif.mc.atlas.recipe.*;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.ShapelessOreRecipe;

@Mod.EventBusSubscriber(modid = AntiqueAtlasMod.ID)
@GameRegistry.ObjectHolder(AntiqueAtlasMod.ID)
public class RegistrarAntiqueAtlas {

    @GameRegistry.ObjectHolder("empty_antique_atlas")
    public static ItemEmptyAtlas EMPTY_ATLAS;
    @GameRegistry.ObjectHolder("antique_atlas")
    public static ItemAtlas ATLAS;
    @GameRegistry.ObjectHolder("antique_astrolabe")
    public static ItemAstrolabe ASTROLABE;
    @GameRegistry.ObjectHolder("ariadne_thread")
    public static ItemAriadneThread ARIADNE_THREAD;

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (SettingsConfig.gameplay.itemNeeded) {
            event.getRegistry().register(new ItemEmptyAtlas());
            event.getRegistry().register(new ItemAtlas());
            event.getRegistry().register(new ItemAstrolabe());
            event.getRegistry().register(new ItemAriadneThread());
        }
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        if (SettingsConfig.gameplay.itemNeeded) {
            event.getRegistry().register(new RecipeAriadneThreadPathClearing().setRegistryName("ariadne_thread_clearing"));
            event.getRegistry().register(new ShapelessOreRecipe(new ResourceLocation(AntiqueAtlasMod.ID, "atlas"), new ItemStack(EMPTY_ATLAS), Items.BOOK, Items.COMPASS).setRegistryName("atlas_blank"));
            event.getRegistry().register(new RecipeAtlasCloning().setRegistryName("atlas_clone"));
            event.getRegistry().register(new RecipeAtlasCombining().setRegistryName("atlas_combine"));
            event.getRegistry().register(new RecipeTransferPathToAtlas().setRegistryName("path_transfer"));
            event.getRegistry().register(new RecipeAriadneThreadColoring().setRegistryName("ariadne_thread_coloring"));
        }
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        if (SettingsConfig.gameplay.itemNeeded) {
            ModelLoader.setCustomModelResourceLocation(EMPTY_ATLAS, 0, new ModelResourceLocation(EMPTY_ATLAS.getRegistryName(), "inventory"));
            ModelLoader.setCustomModelResourceLocation(ASTROLABE, 0, new ModelResourceLocation(ASTROLABE.getRegistryName(), "inventory"));
            ModelLoader.setCustomModelResourceLocation(ARIADNE_THREAD, 0, new ModelResourceLocation(ARIADNE_THREAD.getRegistryName(), "inventory"));
            ModelLoader.setCustomMeshDefinition(ATLAS, stack -> new ModelResourceLocation(ATLAS.getRegistryName(), "inventory"));
        }
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void registerColorHandlers(ColorHandlerEvent.Item event) {
        event.getItemColors().registerItemColorHandler((stack, tintIndex) -> ItemAriadneThread.getColor(stack), ARIADNE_THREAD);
    }

    // Probably not needed since Forge for 1.12 does not support transfers from earlier than 1.11.2, but just in case
    @SubscribeEvent
    public static void handleMissingMapping(RegistryEvent.MissingMappings<Item> event) {
        for (RegistryEvent.MissingMappings.Mapping<Item> mapping : event.getMappings()) {
            if (mapping.key.getPath().equalsIgnoreCase("antiqueatlas"))
                mapping.remap(ATLAS);
            else if (mapping.key.getPath().equalsIgnoreCase("emptyantiqueatlas"))
                mapping.remap(EMPTY_ATLAS);
        }
    }
}
