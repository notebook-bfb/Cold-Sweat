package dev.momostudios.coldsweat.util;

import net.minecraft.util.DamageSource;

public class CSDamageTypes
{
    public static final DamageSource COLD = (new DamageSource("cold"))
        .setDamageBypassesArmor()
        .setDamageIsAbsolute();

    public static final DamageSource HOT  = (new DamageSource("hot"))
        .setDamageBypassesArmor()
        .setFireDamage()
        .setDamageIsAbsolute();

    public static final DamageSource COLD_SCALED = (new DamageSource("cold"))
        .setDamageBypassesArmor()
        .setDifficultyScaled()
        .setDamageIsAbsolute();

    public static final DamageSource HOT_SCALED  = (new DamageSource("hot"))
        .setDamageBypassesArmor()
        .setFireDamage()
        .setDifficultyScaled()
        .setDamageIsAbsolute();
}