package dev.momostudios.coldsweat.core.init;

import dev.momostudios.coldsweat.common.item.MinecartInsulationItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import dev.momostudios.coldsweat.ColdSweat;
import dev.momostudios.coldsweat.common.block.BoilerBlock;
import dev.momostudios.coldsweat.common.block.HearthBottomBlock;
import dev.momostudios.coldsweat.common.block.IceboxBlock;
import dev.momostudios.coldsweat.common.block.SewingTableBlock;
import dev.momostudios.coldsweat.common.item.FilledWaterskinItem;
import dev.momostudios.coldsweat.common.item.SoulspringLampItem;
import dev.momostudios.coldsweat.common.item.WaterskinItem;
import dev.momostudios.coldsweat.core.itemgroup.ColdSweatGroup;
import net.minecraftforge.registries.RegistryObject;

public class ItemInit
{
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ColdSweat.MOD_ID);

    //Items
    public static final RegistryObject<Item> WATERSKIN = ITEMS.register("waterskin", WaterskinItem::new);
    public static final RegistryObject<Item> FILLED_WATERSKIN = ITEMS.register("filled_waterskin", FilledWaterskinItem::new);
    public static final RegistryObject<Item> MINECART_INSULATION = ITEMS.register("minecart_insulation", MinecartInsulationItem::new);
    public static final RegistryObject<Item> THERMOMETER = ITEMS.register("thermometer", () ->
            new Item((new Item.Properties()).tab(ColdSweatGroup.COLD_SWEAT).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> SOULSPRING_LAMP = ITEMS.register("soulspring_lamp", SoulspringLampItem::new);

    //BlockItems
    public static final RegistryObject<BlockItem> BOILER = ITEMS.register("boiler", () -> new BlockItem(BlockInit.BOILER.get(), BoilerBlock.getItemProperties()));
    public static final RegistryObject<BlockItem> ICEBOX = ITEMS.register("icebox", () -> new BlockItem(BlockInit.ICEBOX.get(), IceboxBlock.getItemProperties()));
    public static final RegistryObject<BlockItem> SEWING_TABLE = ITEMS.register("sewing_table", () -> new BlockItem(BlockInit.SEWING_TABLE.get(), SewingTableBlock.getItemProperties()));
    public static final RegistryObject<BlockItem> HEARTH = ITEMS.register("hearth", () -> new BlockItem(BlockInit.HEARTH_BOTTOM.get(), HearthBottomBlock.getItemProperties()));
}
