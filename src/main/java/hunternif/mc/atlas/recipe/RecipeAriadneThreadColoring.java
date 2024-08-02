package hunternif.mc.atlas.recipe;

import hunternif.mc.atlas.item.ItemAriadneThread;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.oredict.DyeUtils;

import static hunternif.mc.atlas.RegistrarAntiqueAtlas.ARIADNE_THREAD;

public class RecipeAriadneThreadColoring extends RecipeBase {
    @Override
    public boolean matches(InventoryCrafting inv, World worldIn) {
        ItemStack ball = ItemStack.EMPTY;
        boolean dyeFound = false;
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.getItem() == ARIADNE_THREAD) {
                if (ball.isEmpty())
                    ball = stack;
                else
                    return false;

            } else if (DyeUtils.isDye(stack)) {
                dyeFound = true;

            } else if(!stack.isEmpty())
                return false;
        }
        return !ball.isEmpty() && dyeFound;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        ItemStack ball = ItemStack.EMPTY;
        int resultRed = 0;
        int resultGreen = 0;
        int resultBlue = 0;
        int totalValue = 0;
        int colorCount = 0;
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.getItem() == ARIADNE_THREAD) {
                if (ball.isEmpty()) {
                    ball = stack.copy();
                    if (ItemAriadneThread.hasColor(ball)) {
                        int color = ItemAriadneThread.getColor(ball);

                        int red = (color >> 16 & 0xff);
                        int green = (color >> 8 & 0xff);
                        int blue = (color & 0xff);

                        totalValue += Math.max(red, Math.max(green, blue));
                        resultRed += red;
                        resultGreen += green;
                        resultBlue += blue;
                        ++colorCount;
                    }
                } else
                    return ItemStack.EMPTY;

            } else if (DyeUtils.isDye(stack)) {
                float[] color = DyeUtils.colorFromStack(stack).get().getColorComponentValues();
                int red = (int) (color[0] * 255);
                int green = (int) (color[1] * 255);
                int blue = (int) (color[2] * 255);

                totalValue += Math.max(red, Math.max(green, blue));
                resultRed += red;
                resultGreen += green;
                resultBlue += blue;
                ++colorCount;

            } else if(!stack.isEmpty())
                return ItemStack.EMPTY;

        }
        if (ball.isEmpty())
            return ItemStack.EMPTY;

        resultRed /= colorCount;
        resultGreen /= colorCount;
        resultBlue /= colorCount;
        float factor = ((float) totalValue / (float) colorCount) / (float) Math.max(resultRed, Math.max(resultGreen, resultBlue));
        resultRed *= factor;
        resultGreen *= factor;
        resultGreen *= factor;

        int resultColor = resultRed << 16 | resultGreen << 8 | resultBlue;

        ball.setCount(1);
        ItemAriadneThread.setColor(ball, resultColor);

        return ball;
    }

    @Override
    public boolean canFit(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return ItemStack.EMPTY;
    }
}
