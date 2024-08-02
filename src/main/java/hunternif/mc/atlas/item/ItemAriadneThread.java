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

public class ItemAriadneThread extends Item {

    {
        setRegistryName("ariadne_thread");
        setTranslationKey("ariadne_thread");
        setMaxStackSize(1);
    }

    public static final int maxQueueSize = 10;

    public static long getActiveKey(ItemStack stack) {
        initNbt(stack);
        return stack.getTagCompound().getLong(activeKey);
    }

    public static void activate(ItemStack stack, long key, BlockPos start) {
        initNbt(stack);
        stack.getTagCompound().setLong(activeKey, key);
        if (!stack.getTagCompound().hasKey(startKey, Constants.NBT.TAG_LONG))
            stack.getTagCompound().setLong(startKey, start.toLong());
    }

    public static boolean deactivate(ItemStack stack) {
        initNbt(stack);
        if (stack.getTagCompound().hasKey(activeKey)) {
            stack.getTagCompound().removeTag(activeKey);
            return true;
        }
        return false;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer playerIn, EnumHand hand) {
        ItemStack heldItem = playerIn.getHeldItem(hand);

        if (!playerIn.capabilities.isCreativeMode)
            playerIn.getCooldownTracker().setCooldown(this, 20);

        if (playerIn.isSneaking()) {
            if (world.isRemote)
                RecordingHandler.stop(heldItem);

            if (deactivate(heldItem))
                if (!playerIn.capabilities.isCreativeMode)
                    playerIn.getCooldownTracker().setCooldown(this, 20 * 60);

        } else {
            if (world.isRemote) {
                RecordingHandler.start(heldItem, playerIn, hand);
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
            deactivate(heldItem);

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
        return RecordingHandler.isActive() && RecordingHandler.isActiveStack(stack);
    }

    private String colorKey = "color";
    private String defaultColorKey = "default_color";

    public boolean hasColor(ItemStack stack) {
        NBTTagCompound nbttagcompound = stack.getTagCompound();
        return nbttagcompound != null && nbttagcompound.hasKey(colorKey, Constants.NBT.TAG_INT);
    }

    private boolean hasDefaultColor(ItemStack stack) {
        NBTTagCompound nbttagcompound = stack.getTagCompound();
        return nbttagcompound != null && nbttagcompound.hasKey(defaultColorKey, Constants.NBT.TAG_INT);
    }

    public int getColor(ItemStack stack) {
        if (hasColor(stack))
            return stack.getTagCompound().getInteger(colorKey);

        if (hasDefaultColor(stack))
            return stack.getTagCompound().getInteger(defaultColorKey);

        return 0xff5BCF1F;
    }

    public void removeColor(ItemStack stack) {
        if (stack.hasTagCompound()) {
            stack.getTagCompound().removeTag(colorKey);
        }
    }

    public void setColor(ItemStack stack, int color) {
        initNbt(stack);
        stack.getTagCompound().setInteger(colorKey, color);
    }

    private static String startKey = "start";
    private static String segmentsKey = "segments";
    private static String activeKey = "active";

    private static void initNbt(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
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

    public static BlockPos posOfPlayer(EntityPlayer player) {
        return new BlockPos(player.posX, player.posY + 0.5D, player.posZ);
    }

}
