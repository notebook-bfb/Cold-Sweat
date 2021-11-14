package net.momostudios.coldsweat.core.event;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.momostudios.coldsweat.common.temperature.modifier.*;
import net.momostudios.coldsweat.common.temperature.modifier.block.*;
import net.momostudios.coldsweat.common.temperature.modifier.sereneseasons.SereneSeasonsDummyModifier;
import net.momostudios.coldsweat.common.temperature.modifier.sereneseasons.SereneSeasonsTempModifier;
import net.momostudios.coldsweat.common.world.BlockEffectEntries;
import net.momostudios.coldsweat.common.world.TempModifierEntries;
import net.momostudios.coldsweat.core.event.csevents.TempModifierEvent;

@Mod.EventBusSubscriber
public class InitTempModifiers
{
    // Trigger TempModifierEvent.Init
    @SubscribeEvent
    public static void registerTempModifiers(WorldEvent.Load event) {
        TempModifierEntries.getEntries().flush();
        BlockEffectEntries.getEntries().flush();
        MinecraftForge.EVENT_BUS.post(new TempModifierEvent.Init.Modifier());
        MinecraftForge.EVENT_BUS.post(new TempModifierEvent.Init.Block());
    }

    // Register BlockEffects
    @SubscribeEvent
    public static void registerBlockEffects(TempModifierEvent.Init.Block event)
    {
        event.addBlockEffect(new LavaBlockEffect());
        event.addBlockEffect(new FurnaceBlockEffect());
        event.addBlockEffect(new CampfireBlockEffect());
        event.addBlockEffect(new FireBlockEffect());
        event.addBlockEffect(new IceBlockEffect());
        event.addBlockEffect(new IceboxBlockEffect());
        event.addBlockEffect(new BoilerBlockEffect());
        event.addBlockEffect(new SoulFireBlockEffect());
        event.addBlockEffect(new SoulCampfireBlockEffect());
    }

    // Register TempModifiers
    @SubscribeEvent
    public static void registerTempModifiers(TempModifierEvent.Init.Modifier event)
    {
        event.addModifier(new BlockTempModifier());
        event.addModifier(new BiomeTempModifier());
        event.addModifier(new DepthTempModifier());
        event.addModifier(new LeatherTempModifier());
        event.addModifier(new MinecartTempModifier());
        event.addModifier(new TimeTempModifier());
        event.addModifier(new WaterskinTempModifier());
        event.addModifier(new WeatherTempModifier());
        event.addModifier(new SoulLampTempModifier());
        try {
            event.addModifier(ModList.get().isLoaded("sereneseasons") ?
                    (TempModifier) Class.forName("net.momostudios.coldsweat.common.temperature.modifier.sereneseasons.SereneSeasonsTempModifier").newInstance() :
                    (TempModifier) Class.forName("net.momostudios.coldsweat.common.temperature.modifier.sereneseasons.SereneSeasonsDummyModifier").newInstance());
        }
        catch (Exception e) {
            event.addModifier(new SereneSeasonsDummyModifier());
        }
        event.addModifier(new HearthTempModifier());
    }
}
