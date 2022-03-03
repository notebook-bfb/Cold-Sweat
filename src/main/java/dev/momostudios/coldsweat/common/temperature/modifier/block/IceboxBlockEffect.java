package dev.momostudios.coldsweat.common.temperature.modifier.block;

import dev.momostudios.coldsweat.common.block.IceboxBlock;
import dev.momostudios.coldsweat.common.temperature.Temperature;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import dev.momostudios.coldsweat.util.math.CSMath;

public class IceboxBlockEffect extends BlockEffect
{
    @Override
    public double getTemperature(Player player, BlockState state, BlockPos pos, double distance)
    {
        if (this.hasBlock(state) && state.getValue(IceboxBlock.FROSTED))
        {
            return CSMath.blend(-0.27, 0, distance, 0.5, 5);
        }
        return 0;
    }

    @Override
    public boolean hasBlock(BlockState block)
    {
        return block.getBlock() instanceof IceboxBlock;
    }

    @Override
    public double minEffect() {
        return CSMath.convertUnits(-40, Temperature.Units.F, Temperature.Units.MC, false);
    }

    @Override
    public double minTemperature() {
        return CSMath.convertUnits(32, Temperature.Units.F, Temperature.Units.MC, true);
    }
}
