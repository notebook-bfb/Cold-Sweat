package dev.momostudios.coldsweat.common.blockentity;

import com.mojang.datafixers.util.Pair;
import dev.momostudios.coldsweat.ColdSweat;
import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.api.temperature.modifier.HearthTempModifier;
import dev.momostudios.coldsweat.api.util.TempHelper;
import dev.momostudios.coldsweat.client.event.HearthDebugRenderer;
import dev.momostudios.coldsweat.common.block.HearthBottomBlock;
import dev.momostudios.coldsweat.common.container.HearthContainer;
import dev.momostudios.coldsweat.common.event.HearthPathManagement;
import dev.momostudios.coldsweat.core.init.BlockEntityInit;
import dev.momostudios.coldsweat.core.init.ParticleTypesInit;
import dev.momostudios.coldsweat.core.network.ColdSweatPacketHandler;
import dev.momostudios.coldsweat.core.network.message.BlockDataUpdateMessage;
import dev.momostudios.coldsweat.core.network.message.HearthResetMessage;
import dev.momostudios.coldsweat.util.config.ConfigSettings;
import dev.momostudios.coldsweat.util.math.CSMath;
import dev.momostudios.coldsweat.util.registries.ModEffects;
import dev.momostudios.coldsweat.util.registries.ModSounds;
import dev.momostudios.coldsweat.util.world.SpreadPath;
import dev.momostudios.coldsweat.util.world.WorldHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;

public class HearthBlockEntity extends RandomizableContainerBlockEntity
{
    ConfigSettings config = ConfigSettings.getInstance();

    ArrayList<SpreadPath> paths = new ArrayList<>();
    // Used as a lookup table for detecting duplicate paths (faster than ArrayList#contains())
    Set<BlockPos> pathLookup = new HashSet<>();

    HashMap<ChunkPos, LevelChunk> loadedChunks = new HashMap<>();

    static int INSULATION_TIME = 1200;

    public static int SLOT_COUNT = 1;
    protected NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    BlockPos blockPos = this.getBlockPos();

    int hotFuel = 0;
    int coldFuel = 0;
    boolean shouldUseHotFuel = false;
    boolean shouldUseColdFuel = false;
    boolean hasHotFuel = false;
    boolean hasColdFuel = false;
    int insulationLevel = 0;

    boolean isPlayerNearby = false;
    int rebuildCooldown = 0;
    boolean forceRebuild = false;
    LinkedList<BlockPos> notifyQueue = new LinkedList<>();

    public int ticksExisted = 0;

    private LevelChunk workingChunk = null;
    private ChunkPos workingCoords = new ChunkPos(this.getBlockPos().getX() >> 4, this.getBlockPos().getZ() >> 4);

    boolean showParticles = true;
    int frozenPaths = 0;
    boolean spreading = true;

    public static final int MAX_FUEL = 1000;

    public HearthBlockEntity(BlockPos pos, BlockState state)
    {
        super(BlockEntityInit.HEARTH_BLOCK_ENTITY_TYPE.get(), pos, state);
        this.addPath(new SpreadPath(this.getBlockPos()));
        HearthPathManagement.HEARTH_POSITIONS.put(this.getBlockPos(), this.spreadRange());
    }

    public int spreadRange()
    {
        return 20;
    }

    public int maxPaths()
    {
        return 6000;
    }

    @Override
    protected Component getDefaultName() {
        return new TranslatableComponent("container." + ColdSweat.MOD_ID + ".hearth");
    }

    @Override
    public Component getDisplayName()
    {
        return this.getCustomName() != null ? this.getCustomName() : this.getDefaultName();
    }

    @Override
    public CompoundTag getUpdateTag()
    {
        CompoundTag tag = super.getUpdateTag();
        tag.putInt("hotFuel",  this.getHotFuel());
        tag.putInt("coldFuel", this.getColdFuel());
        tag.putInt("insulationLevel", insulationLevel);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag)
    {
        super.handleUpdateTag(tag);
        this.setHotFuel(tag.getInt("hotFuel"));
        this.setColdFuel(tag.getInt("coldFuel"));
        insulationLevel = tag.getInt("insulationLevel");
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt)
    {
        handleUpdateTag(pkt.getTag());
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket()
    {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> itemsIn)
    {
        this.items = itemsIn;
    }

    public static <T extends BlockEntity> void tickSelf(Level level, BlockPos pos, BlockState state, T te)
    {
        if (te instanceof HearthBlockEntity hearth)
        {
            hearth.tick(level, pos);
        }
    }

    public void tick(Level level, BlockPos pos)
    {
        if (paths.isEmpty()) addPath(new SpreadPath(pos));

        this.ticksExisted++;

        if (rebuildCooldown > 0) rebuildCooldown--;

        List<Player> players = new ArrayList<>();

        if (this.level != null && this.ticksExisted % 20 == 0)
        {
            this.isPlayerNearby = false;
            for (Player player : this.level.players())
            {
                if (player.blockPosition().closerThan(pos, this.spreadRange()))
                {
                    players.add(player);
                    this.isPlayerNearby = true;
                }
            }
        }

        // Reset if a nearby block has been updated
        if (forceRebuild || (rebuildCooldown <= 0 && !notifyQueue.isEmpty()))
        {
            boolean shouldRebuild = false;

            // If the rebuild is forced, skip the queue and rebuild immediately
            if (forceRebuild)
                shouldRebuild = true;
            else
            {
                // Iterate over every position in the queue
                // If any of them are in the path lookup, rebuild
                for (BlockPos notifyPos : notifyQueue)
                {
                    if (pathLookup.contains(notifyPos))
                    {
                        shouldRebuild = true;
                        break;
                    }
                }
            }

            if (shouldRebuild)
            {
                // Reset cooldown
                this.rebuildCooldown = 100;

                // Reset paths
                this.replacePaths(List.of(new SpreadPath(pos)));
                frozenPaths = 0;
                spreading = true;

                // Tell client to reset paths too
                if (!level.isClientSide)
                    ColdSweatPacketHandler.INSTANCE.send(PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4)), new HearthResetMessage(pos));
            }
            notifyQueue.clear();
            forceRebuild = false;
        }
        if (level.isClientSide)
        {
            if (showParticles)
                HearthDebugRenderer.HEARTH_LOCATIONS.put(pos, pathLookup);
            else HearthDebugRenderer.HEARTH_LOCATIONS.remove(pos);
        }

        if (hotFuel > 0 || coldFuel > 0)
        {
            // Gradually increases insulation amount
            if (insulationLevel < INSULATION_TIME)
                insulationLevel++;

            if (this.isPlayerNearby)
            {
                if (this.ticksExisted % 10 == 0)
                {
                    showParticles = level.isClientSide
                            && Minecraft.getInstance().options.particles == ParticleStatus.ALL
                            && !HearthPathManagement.DISABLED_HEARTHS.contains(Pair.of(pos, level.dimension().location().toString()));
                }

                /*
                 Partition the points into logical "sub-maps" to be iterated over separately each tick
                */
                int pathCount = paths.size();
                // Size of each partition (defaults to 1/30th of the total paths)
                int partSize = CSMath.clamp(pathCount / 30, 10, 200);
                // Number of partitions
                int partCount = (int) CSMath.ceil(pathCount / (double) partSize);
                // Index of the last point being worked on this tick
                int lastIndex = Math.min(pathCount, partSize * ((this.ticksExisted % partCount) + 1));
                // Index of the first point being worked on this tick
                int firstIndex = Math.max(0, lastIndex - partSize);

                /*
                 Iterate over the specified partition of paths
                 */
                for (int i = firstIndex; i < Math.min(paths.size(), lastIndex); i++)
                {
                    // This operation is really fast because it's an ArrayList
                    SpreadPath spreadPath = paths.get(i);

                    int x = spreadPath.getX();
                    int y = spreadPath.getY();
                    int z = spreadPath.getZ();

                    try
                    {
                        // Don't try to spread if the path is frozen
                        if (spreadPath.isFrozen())
                        {
                            // Remove a 3D checkerboard pattern of paths after the Hearth is finished spreading
                            // (Halves the number of iterations)
                            // The Hearth is "finished spreading" when all paths are frozen
                            if (!spreading && ((y % 2 == 0) == (x % 2 != z % 2)))
                            {
                                paths.remove(i);
                                // Go back and iterate over the new path at this index
                                i--;
                            }
                            continue;
                        }

                        /*
                         Try to spread to new blocks
                         */
                        if (pathCount < this.maxPaths() && spreadPath.withinDistance(pos, this.spreadRange()))
                        {
                            /*
                             Get the chunk at this position
                             */
                            ChunkPos chunkPos = new ChunkPos(x >> 4, z >> 4);
                            LevelChunk chunk;

                            if (chunkPos == workingCoords)
                                chunk = workingChunk;
                            else
                            {
                                if ((chunk = loadedChunks.get(chunkPos)) == null)
                                {
                                    loadedChunks.put(chunkPos, chunk = (LevelChunk) level.getChunkSource().getChunk(x >> 4, z >> 4, ChunkStatus.FULL, false));
                                    if (chunk == null) continue;
                                }
                                workingCoords = chunkPos;
                                workingChunk = chunk;
                            }

                            /*
                             Spreading algorithm
                             */
                            BlockPos pathPos = spreadPath.getPos();
                            if (!WorldHelper.canSeeSky(chunk, level, pathPos.above(), 64))
                            {
                                // Try to spread in every direction from the current position
                                for (Direction direction : Direction.values())
                                {
                                    SpreadPath tryPath = spreadPath.offset(direction);

                                    // Avoid duplicate paths (ArrayList isn't duplicate-safe like Sets/Maps)
                                    if (pathLookup.add(tryPath.getPos()) && !WorldHelper.isSpreadBlocked(chunk, pathPos, direction))
                                    {
                                        // Add the new path to the temporary list and lookup table
                                        paths.add(tryPath);
                                    }
                                }
                            }
                            // Remove this path if it has skylight access
                            else
                            {
                                pathLookup.remove(pathPos);
                                paths.remove(i);
                                i--;
                                continue;
                            }
                        }
                        if (!spreadPath.isFrozen())
                        {
                            // Track frozen paths to know when the Hearth is done spreading
                            spreadPath.freeze();
                            this.frozenPaths++;
                        }
                    }

                    /*
                     Give insulation & spawn particles
                     */
                    finally
                    {
                        if (level.isClientSide && showParticles)
                        {
                            // Air Particles
                            Random rand = new Random();
                            if (!Minecraft.getInstance().options.renderDebug && rand.nextFloat() < (spreading ? 0.016f : 0.032f))
                            {
                                float xr = rand.nextFloat();
                                float yr = rand.nextFloat();
                                float zr = rand.nextFloat();
                                float xm = rand.nextFloat() / 20 - 0.025f;
                                float zm = rand.nextFloat() / 20 - 0.025f;

                                level.addParticle(ParticleTypesInit.HEARTH_AIR.get(), false, x + xr, y + yr, z + zr, xm, 0, zm);
                            }
                        }

                        // Give insulation to players
                        if (!level.isClientSide)
                        {
                            for (int p = 0; p < players.size(); p++)
                            {
                                Player player = players.get(p);
                                // If player is null or not in range, skip
                                if (player == null || CSMath.getDistance(spreadPath.getPos(), player.blockPosition()) > 1)
                                    continue;

                                players.remove(p);
                                p--;

                                MobEffectInstance effect = player.getEffect(ModEffects.INSULATION);

                                if (effect == null || effect.getDuration() < 60 && !WorldHelper.canSeeSky(player.level, new BlockPos(player.getX(), CSMath.ceil(player.getY()), player.getZ()), 64))
                                {
                                    this.insulatePlayer(player);
                                    break;
                                }
                            }
                        }
                    }
                }

                // Drain fuel
                if (this.ticksExisted % 80 == 0)
                {
                    if (shouldUseColdFuel)
                        this.setColdFuel(coldFuel - 1);
                    if (shouldUseHotFuel)
                        this.setHotFuel(hotFuel - 1);

                    shouldUseColdFuel = false;
                    shouldUseHotFuel = false;
                }
            }
        }

        // Input fuel
        if (this.ticksExisted % 10 == 0)
        {
            ItemStack fuelStack = this.getItems().get(0);
            if (!fuelStack.isEmpty())
            {
                int itemFuel = getItemFuel(fuelStack);
                if (itemFuel != 0)
                {
                    int fuel = itemFuel > 0 ? hotFuel : coldFuel;
                    if (fuel < MAX_FUEL - Math.abs(itemFuel) * 0.75)
                    {
                        if (fuelStack.hasContainerItem())
                        {
                            if (fuelStack.getCount() == 1)
                            {
                                this.setItem(0, fuelStack.getContainerItem());
                                addFuel(itemFuel, hotFuel, coldFuel);
                            }
                        }
                        else
                        {
                            int consumeCount = Math.min((int) Math.floor((MAX_FUEL - fuel) / (double) Math.abs(itemFuel)), fuelStack.getCount());
                            fuelStack.shrink(consumeCount);
                            addFuel(itemFuel * consumeCount, hotFuel, coldFuel);
                        }
                    }
                }
            }
        }

        // Particles
        //long startNanos = System.nanoTime();
        if (level.isClientSide)
        {
            Random rand = new Random();
            if (rand.nextDouble() < coldFuel / 3000d)
            {
                double d0 = pos.getX() + 0.5d;
                double d1 = pos.getY() + 1.8d;
                double d2 = pos.getZ() + 0.5d;
                double d3 = (rand.nextDouble() - 0.5) / 4;
                double d4 = (rand.nextDouble() - 0.5) / 4;
                double d5 = (rand.nextDouble() - 0.5) / 4;
                level.addParticle(ParticleTypesInit.STEAM.get(), d0 + d3, d1 + d4, d2 + d5, 0.0D, 0.04D, 0.0D);
            }
            if (rand.nextDouble() < hotFuel / 3000d)
            {
                double d0 = pos.getX() + 0.5d;
                double d1 = pos.getY() + 1.8d;
                double d2 = pos.getZ() + 0.5d;
                double d3 = (rand.nextDouble() - 0.5) / 2;
                double d4 = (rand.nextDouble() - 0.5) / 2;
                double d5 = (rand.nextDouble() - 0.5) / 2;
                SimpleParticleType particle = Math.random() < 0.5 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE;
                level.addParticle(particle, d0 + d3, d1 + d4, d2 + d5, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    boolean insulatePlayer(Player player)
    {
        // Get the player's temperature
        if (!(shouldUseHotFuel && shouldUseColdFuel))
        {
            HearthTempModifier mod = TempHelper.getModifier(player, Temperature.Type.WORLD, HearthTempModifier.class);
            double temp = (mod != null) ? mod.getLastInput().get() : TempHelper.getTemperature(player, Temperature.Type.WORLD).get();

            // Tell the hearth to use hot fuel
            shouldUseHotFuel = shouldUseHotFuel || (hotFuel > 0 && temp < config.minTemp);
            // Tell the hearth to use cold fuel
            shouldUseColdFuel = shouldUseColdFuel || (coldFuel > 0 && temp > config.maxTemp);
        }

        if (shouldUseHotFuel || shouldUseColdFuel)
        {
            int effectLevel = Math.min(9, (int) ((insulationLevel / (double) INSULATION_TIME) * 9));
            player.addEffect(new MobEffectInstance(ModEffects.INSULATION, 100, effectLevel, false, false, true));
            return true;
        }
        return false;
    }

    public static int getItemFuel(ItemStack item)
    {
        return ConfigSettings.HEARTH_FUEL.get().getOrDefault(item.getItem(), 0d).intValue();
    }

    public int getHotFuel()
    {
        return this.hotFuel;
    }

    public int getColdFuel()
    {
        return this.coldFuel;
    }

    public void setHotFuel(int amount)
    {
        this.hotFuel = CSMath.clamp(amount, 0, MAX_FUEL);
        this.updateFuelState();

        if (amount == 0 && hasHotFuel)
        {
            hasHotFuel = false;
            level.playSound(null, blockPos, ModSounds.HEARTH_FUEL, SoundSource.BLOCKS, 1, (float) Math.random() * 0.2f + 0.9f);
        }
        else hasHotFuel = true;
    }

    public void setColdFuel(int amount)
    {
        this.coldFuel = CSMath.clamp(amount, 0, MAX_FUEL);
        this.updateFuelState();

        if (amount == 0 && hasColdFuel)
        {
            hasColdFuel = false;
            level.playSound(null, blockPos, ModSounds.HEARTH_FUEL, SoundSource.BLOCKS, 1, (float) Math.random() * 0.2f + 0.9f);
        }
        else hasColdFuel = true;
    }

    public void addFuel(int amount)
    {
        this.addFuel(amount, this.getHotFuel(), this.getColdFuel());
    }

    public void addFuel(int amount, int hotFuel, int coldFuel)
    {
        if (amount > 0)
            setHotFuel(hotFuel + Math.abs(amount));
        else if (amount < 0)
            setColdFuel(coldFuel + Math.abs(amount));
    }

    public void updateFuelState()
    {
        if (level != null && !level.isClientSide)
        {
            int hotFuel = this.getHotFuel();
            int coldFuel = this.getColdFuel();

            BlockState state = level.getBlockState(blockPos);
            int waterLevel = coldFuel == 0 ? 0 : (coldFuel < MAX_FUEL / 2 ? 1 : 2);
            int lavaLevel = hotFuel == 0 ? 0 : (hotFuel < MAX_FUEL / 2 ? 1 : 2);

            BlockState desiredState = state.setValue(HearthBottomBlock.WATER, waterLevel).setValue(HearthBottomBlock.LAVA, lavaLevel);
            if (state.getValue(HearthBottomBlock.WATER) != waterLevel || state.getValue(HearthBottomBlock.LAVA) != lavaLevel)
                level.setBlock(blockPos, desiredState, 3);

            this.setChanged();

            CompoundTag tag = new CompoundTag();
            this.saveAdditional(tag);
            ColdSweatPacketHandler.INSTANCE.send(PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(blockPos)), new BlockDataUpdateMessage(this));
        }
    }

    @Override
    public int getContainerSize()
    {
        return SLOT_COUNT;
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory playerInv)
    {
        return new HearthContainer(id, playerInv, this);
    }

    @Override
    public void load(CompoundTag tag)
    {
        super.load(tag);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        this.setColdFuel(tag.getInt("coldFuel"));
        this.setHotFuel(tag.getInt("hotFuel"));
        this.insulationLevel = tag.getInt("insulationLevel");
        ContainerHelper.loadAllItems(tag, this.items);
    }

    @Override
    public void saveAdditional(CompoundTag tag)
    {
        super.saveAdditional(tag);
        tag.putInt("coldFuel", this.getColdFuel());
        tag.putInt("hotFuel", this.getHotFuel());
        tag.putInt("insulationLevel", insulationLevel);
        ContainerHelper.saveAllItems(tag, this.items);
    }

    public void replacePaths(Collection<SpreadPath> newPaths)
    {
        paths.clear();
        this.addPaths(newPaths);
        pathLookup.clear();
        pathLookup.addAll(newPaths.stream().map(SpreadPath::getPos).toList());
    }

    public void addPath(SpreadPath path)
    {
        paths.add(path);
    }

    public void addPaths(Collection<SpreadPath> newPaths)
    {
        paths.addAll(newPaths);
    }

    public void sendBlockUpdate(BlockPos pos)
    {
        notifyQueue.add(pos);
    }

    public void resetPaths()
    {
        forceRebuild = true;
    }

    @Override
    public void setRemoved()
    {
        super.setRemoved();
        HearthPathManagement.HEARTH_POSITIONS.remove(this.blockPos);
    }

    public Set<BlockPos> getPaths()
    {
        return pathLookup;
    }
}