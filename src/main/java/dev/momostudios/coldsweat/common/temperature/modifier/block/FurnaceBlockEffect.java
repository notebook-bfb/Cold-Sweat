package dev.momostudios.coldsweat.common.temperature.modifier.block;

import net.minecraft.block.AbstractFurnaceBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import dev.momostudios.coldsweat.util.CSMath;
import dev.momostudios.coldsweat.util.Units;

public class FurnaceBlockEffect extends BlockEffect
{
    @Override
    public double getTemperature(PlayerEntity player, BlockState state, BlockPos pos, double distance)
    {
        if (this.hasBlock(state) && state.get(AbstractFurnaceBlock.LIT))
        {
            return CSMath.blend(0.32, 0, distance, 0.5, 7);
        }
        return 0;
    }

    @Override
    public boolean hasBlock(BlockState block)
    {
        return block.getBlock() instanceof AbstractFurnaceBlock;
    }

    @Override
    public double maxEffect() {
        return CSMath.convertUnits(40, Units.F, Units.MC, false);
    }

    @Override
    public double maxTemperature() {
        return CSMath.convertUnits(600, Units.F, Units.MC, true);
    }
}