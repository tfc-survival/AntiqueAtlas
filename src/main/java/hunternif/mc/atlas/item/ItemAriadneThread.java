package hunternif.mc.atlas.item;

import hunternif.mc.atlas.client.ariadne.thread.RecordingHandler;
import hunternif.mc.atlas.map.objects.path.Path;
import net.minecraft.block.BlockCauldron;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.List;

import static hunternif.mc.atlas.RegistrarAntiqueAtlas.ARIADNE_THREAD;

public class ItemAriadneThread extends Item {

    {
        setRegistryName("ariadne_thread");
        setTranslationKey("ariadne_thread");
    }

    public static final int maxQueueSize = 10;

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer playerIn, EnumHand hand) {
        ItemStack heldItem = playerIn.getHeldItem(hand);

        if (!playerIn.capabilities.isCreativeMode)
            playerIn.getCooldownTracker().setCooldown(this, 20);

        int activeItem = getActiveItem(playerIn);
        if (playerIn.isSneaking()) {
            if (hand == EnumHand.MAIN_HAND && activeItem == playerIn.inventory.currentItem ||
                    hand == EnumHand.OFF_HAND && activeItem == 45) {

                if (world.isRemote)
                    RecordingHandler.stop();

                markActive(heldItem, false, playerIn);

                if (!playerIn.capabilities.isCreativeMode)
                    playerIn.getCooldownTracker().setCooldown(this, 20 * 60);
            }
        } else {
            if (activeItem == -2) {
                if (heldItem.getCount() == 1) {
                    markActive(heldItem, true, playerIn);
                } else {
                    ItemStack r = heldItem.copy();
                    r.setCount(1);
                    markActive(r, true, playerIn);
                    if (playerIn.addItemStackToInventory(r)) {
                        heldItem.shrink(1);
                    }
                }
                if (world.isRemote)
                    RecordingHandler.start(heldItem);
            }
        }

        return new ActionResult<>(EnumActionResult.SUCCESS, heldItem);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        IBlockState blockState = worldIn.getBlockState(pos);
        if (blockState.getBlock() == Blocks.CAULDRON) {
            ItemStack heldItem = player.getHeldItem(hand);
            int level = blockState.getValue(BlockCauldron.LEVEL);
            if (level > 0) {
                Blocks.CAULDRON.setWaterLevel(worldIn, pos, blockState, level - 1);
                removeColor(heldItem);
                return EnumActionResult.SUCCESS;
            }
            markActive(heldItem, false, player);

            return EnumActionResult.FAIL;
        }
        return EnumActionResult.PASS;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        tooltip.add(I18n.format("item.ariadne_thread.tooltip1"));
        tooltip.add(I18n.format("item.ariadne_thread.tooltip2"));
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return isActive(stack);
    }

    private String displayKey = "display";
    private String colorKey = "color";

    public boolean hasColor(ItemStack stack) {
        NBTTagCompound nbttagcompound = stack.getTagCompound();
        return nbttagcompound != null &&
                nbttagcompound.hasKey(displayKey, Constants.NBT.TAG_COMPOUND) &&
                nbttagcompound.getCompoundTag(displayKey).hasKey(colorKey, Constants.NBT.TAG_INT);
    }

    public int getColor(ItemStack stack) {
        if (hasColor(stack))
            return stack.getTagCompound().getCompoundTag(displayKey).getInteger(colorKey);

        return 0xff5BCF1F;
    }

    public void removeColor(ItemStack stack) {
        if (stack.hasTagCompound()) {
            stack.getTagCompound().getCompoundTag(displayKey).removeTag(colorKey);
        }
    }

    public void setColor(ItemStack stack, int color) {
        stack.getOrCreateSubCompound(displayKey).setInteger(colorKey, color);
    }

    public static int getActiveItem(EntityPlayer player) {
        if (isActive(player.getHeldItemMainhand()))
            return player.inventory.currentItem;

        if (isActive(player.getHeldItemOffhand()))
            return 40;

        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            if (isActive(player.inventory.getStackInSlot(i))) {
                return i;
            }
        }

        if (isActive(player.inventory.getItemStack()))
            return -1;

        return -2;
    }

    private static String startKey = "start";
    private static String segmentsKey = "segments";
    private static String activeKey = "active";

    public static boolean isActive(ItemStack stack) {
        return stack.getItem() == ARIADNE_THREAD && RecordingHandler.isActive() && stack.hasTagCompound() && stack.getTagCompound().getBoolean(activeKey);
    }

    public static void markActive(ItemStack stack, boolean active, EntityPlayer player) {
        initNbt(stack);
        if (active)
            if (!stack.getTagCompound().hasKey(segmentsKey))
                stack.getTagCompound().setLong(startKey, posOfPlayer(player).toLong());

        stack.getTagCompound().setBoolean(activeKey, active);
    }

    private static void initNbt(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
    }

    public static BlockPos posOfPlayer(EntityPlayer player) {
        return new BlockPos(player.posX, player.posY + 0.5D, player.posZ);
    }


    public static void append(ItemStack stack, List<Short> addition) {
        initNbt(stack);

        byte[] segmentsBytes = stack.getTagCompound().getByteArray(segmentsKey);
        short[] segments = Path.loadSegments(segmentsBytes, new short[segmentsBytes.length / 2 + addition.size()]);

        for (int i = 0; i < addition.size(); i++) {
            segments[i + segmentsBytes.length / 2] = addition.get(i);
        }
        stack.getTagCompound().setByteArray(segmentsKey, Path.saveSegments(segments));
    }

    public static BlockPos getStart(ItemStack stack) {
        initNbt(stack);
        if (stack.getTagCompound().hasKey(startKey, Constants.NBT.TAG_LONG))
            return BlockPos.fromLong(stack.getTagCompound().getLong(startKey));
        else
            return null;
    }

    public static short[] getPath(ItemStack stack) {
        if (!stack.hasTagCompound())
            return null;

        byte[] segmentsBytes = stack.getTagCompound().getByteArray(segmentsKey);
        if (segmentsBytes.length == 0)
            return null;

        return Path.loadSegments(segmentsBytes);
    }

    public static void clearPath(ItemStack stack) {
        if (stack.hasTagCompound()) {
            stack.getTagCompound().removeTag(startKey);
            stack.getTagCompound().removeTag(segmentsKey);
            stack.getTagCompound().removeTag(activeKey);
        }
    }

}
