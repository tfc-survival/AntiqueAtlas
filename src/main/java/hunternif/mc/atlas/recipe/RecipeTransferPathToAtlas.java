package hunternif.mc.atlas.recipe;

import hunternif.mc.atlas.AntiqueAtlasMod;
import hunternif.mc.atlas.RegistrarAntiqueAtlas;
import hunternif.mc.atlas.api.AtlasAPI;
import hunternif.mc.atlas.item.ItemAriadneThread;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static hunternif.mc.atlas.RegistrarAntiqueAtlas.ARIADNE_THREAD;
import static hunternif.mc.atlas.RegistrarAntiqueAtlas.ATLAS;

public class RecipeTransferPathToAtlas extends ShapelessRecipes {
    public RecipeTransferPathToAtlas() {
        super(AntiqueAtlasMod.ID + ":atlas", new ItemStack(ATLAS), NonNullList.from(Ingredient.EMPTY, Ingredient.fromItem(ATLAS), Ingredient.fromItem(ARIADNE_THREAD)));
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        ItemStack r = findIngs(inv).getLeft().copy();
        r.setCount(1);
        return r;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inv) {
        NonNullList<ItemStack> r = NonNullList.<ItemStack>withSize(inv.getSizeInventory(), ItemStack.EMPTY);
        for (int i = 0; i < r.size(); ++i) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.getItem() == ARIADNE_THREAD) {
                ItemStack copy = stack.copy();
                copy.setCount(1);
                r.set(i, copy);
            }
        }
        return r;
    }

    private Pair<ItemStack, ItemStack> findIngs(IInventory inv) {
        List<ItemStack> items = IntStream.range(0, inv.getSizeInventory()).mapToObj(inv::getStackInSlot).filter(i -> !i.isEmpty()).collect(Collectors.toList());
        if (items.get(0).getItem() == ATLAS)
            return Pair.of(items.get(0), items.get(1));
        else
            return Pair.of(items.get(1), items.get(0));
    }

    @Override
    public ItemStack getRecipeOutput() {
        return ItemStack.EMPTY;
    }

    @SubscribeEvent
    public void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.crafting.getItem() != RegistrarAntiqueAtlas.ATLAS || !matches((InventoryCrafting) event.craftMatrix, null))
            return;

        World world = event.player.getEntityWorld();
        if (world.isRemote)
            return;

        Pair<ItemStack, ItemStack> input = findIngs(event.craftMatrix);
        ItemStack atlas = input.getLeft();
        ItemStack ball = input.getRight();

        if (!ball.hasTagCompound())
            return;

        NBTTagCompound displayNbt = ball.getTagCompound().getCompoundTag("display");

        int color;
        if (displayNbt != null && displayNbt.hasKey("color", 3)) {
            color = displayNbt.getInteger("color");
        } else
            color = 0x31A500;

        BlockPos start = ItemAriadneThread.getStart(ball);
        short[] segments = ItemAriadneThread.getPath(ball);
        if (start != null && segments != null)
            AtlasAPI.paths.addPath(world, atlas.getItemDamage(), ball.getDisplayName(), color, start.getX(), start.getZ(), segments);
    }
}
