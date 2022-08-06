package com.bgsoftware.wildtools.nms.v1_7_R4;

import com.bgsoftware.common.reflection.ReflectField;
import com.bgsoftware.wildtools.utils.items.ToolItemStack;
import net.minecraft.server.v1_7_R4.Block;
import net.minecraft.server.v1_7_R4.BlockCarrots;
import net.minecraft.server.v1_7_R4.BlockCocoa;
import net.minecraft.server.v1_7_R4.BlockCrops;
import net.minecraft.server.v1_7_R4.BlockNetherWart;
import net.minecraft.server.v1_7_R4.BlockPotatoes;
import net.minecraft.server.v1_7_R4.Blocks;
import net.minecraft.server.v1_7_R4.Chunk;
import net.minecraft.server.v1_7_R4.EnchantmentManager;
import net.minecraft.server.v1_7_R4.EntityItem;
import net.minecraft.server.v1_7_R4.EntityLiving;
import net.minecraft.server.v1_7_R4.EntityPlayer;
import net.minecraft.server.v1_7_R4.GameProfileSerializer;
import net.minecraft.server.v1_7_R4.Item;
import net.minecraft.server.v1_7_R4.ItemStack;
import net.minecraft.server.v1_7_R4.Items;
import net.minecraft.server.v1_7_R4.NBTTagCompound;
import net.minecraft.server.v1_7_R4.Packet;
import net.minecraft.server.v1_7_R4.PacketPlayOutCollect;
import net.minecraft.server.v1_7_R4.PacketPlayOutMultiBlockChange;
import net.minecraft.server.v1_7_R4.PlayerChunkMap;
import net.minecraft.server.v1_7_R4.StatisticList;
import net.minecraft.server.v1_7_R4.TileEntity;
import net.minecraft.server.v1_7_R4.TileEntitySkull;
import net.minecraft.server.v1_7_R4.World;
import net.minecraft.server.v1_7_R4.WorldServer;
import org.bukkit.Bukkit;
import org.bukkit.CropState;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NetherWartsState;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_7_R4.CraftChunk;
import org.bukkit.craftbukkit.v1_7_R4.CraftServer;
import org.bukkit.craftbukkit.v1_7_R4.CraftWorld;
import org.bukkit.craftbukkit.v1_7_R4.block.CraftBlock;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftItem;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_7_R4.event.CraftEventFactory;
import org.bukkit.craftbukkit.v1_7_R4.inventory.CraftInventoryPlayer;
import org.bukkit.craftbukkit.v1_7_R4.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_7_R4.util.CraftMagicNumbers;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.material.CocoaPlant;
import org.bukkit.material.Crops;
import org.bukkit.material.NetherWarts;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@SuppressWarnings({"unused", "deprecation"})
public final class NMSAdapter implements com.bgsoftware.wildtools.nms.NMSAdapter {

    private static final ReflectField<ItemStack> ITEM_STACK_HANDLE = new ReflectField<>(CraftItemStack.class, ItemStack.class, "handle");

    @Override
    public String getMappingsHash() {
        return null;
    }

    @Override
    public String getVersion() {
        return "v1_7_R4";
    }

    @Override
    public List<org.bukkit.inventory.ItemStack> getBlockDrops(Player pl, org.bukkit.block.Block bl, boolean silkTouch) {
        List<org.bukkit.inventory.ItemStack> drops = new ArrayList<>();

        EntityPlayer player = ((CraftPlayer) pl).getHandle();
        World world = player.world;
        Block block = world.getType(bl.getX(), bl.getY(), bl.getZ());

        //Checks if player cannot break the block or player in creative mode
        if (!player.a(block) || player.playerInteractManager.isCreative())
            return drops;

        TileEntity tileEntity = world.getTileEntity(bl.getX(), bl.getY(), bl.getZ());

        if (tileEntity instanceof TileEntitySkull) {
            TileEntitySkull tileEntitySkull = (TileEntitySkull) tileEntity;
            if (tileEntitySkull.getSkullType() == 3) {
                ItemStack itemStack = new ItemStack(Items.SKULL, 1, 3);
                NBTTagCompound nbtTagCompound = itemStack.hasTag() ? itemStack.getTag() : new NBTTagCompound();
                assert nbtTagCompound != null;
                NBTTagCompound skullOwnerTag = new NBTTagCompound();
                GameProfileSerializer.serialize(skullOwnerTag, tileEntitySkull.getGameProfile());
                nbtTagCompound.set("SkullOwner", skullOwnerTag);
                itemStack.setTag(nbtTagCompound);
                drops.add(CraftItemStack.asBukkitCopy(itemStack));
                return drops;
            }
        }

        //Checks if player has silk touch
        if ((block.d() && !block.isTileEntity()) && (silkTouch || EnchantmentManager.hasSilkTouchEnchantment(player))) {
            int data = 0;
            Item item = Item.getItemOf(block);
            //Checks if item not null and something else?
            if (item != null && item.n()) {
                data = Block.getId(block);
            }
            //Adds item to drops
            drops.add(CraftItemStack.asBukkitCopy(new ItemStack(item, 1, data)));
        } else {
            int fortuneLevel = getItemInHand(pl).getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS),
                    dropCount = block.getDropCount(fortuneLevel, world.random),
                    blockId = Block.getId(block);

            Item item = block.getDropType(blockId, world.random, fortuneLevel);
            if (item != null) {
                drops.add(CraftItemStack.asBukkitCopy(new ItemStack(item, dropCount, block.getDropData(blockId))));
            }
        }

        return drops;
    }

    @Override
    public List<org.bukkit.inventory.ItemStack> getCropDrops(Player pl, org.bukkit.block.Block bl) {
        List<org.bukkit.inventory.ItemStack> drops = new ArrayList<>();

        EntityPlayer player = ((CraftPlayer) pl).getHandle();
        World world = player.world;
        Block block = world.getType(bl.getX(), bl.getY(), bl.getZ());

        int age = ((CraftBlock) bl).getData();
        int fortuneLevel = getItemInHand(pl).getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS);

        if (block instanceof BlockCrops) {
            if (age >= 7) {
                //Give the item itself to the player
                if (block instanceof BlockCarrots) {
                    drops.add(CraftItemStack.asBukkitCopy(new ItemStack(Items.CARROT, 1, 0)));
                } else if (block instanceof BlockPotatoes) {
                    drops.add(CraftItemStack.asBukkitCopy(new ItemStack(Items.POTATO, 1, 0)));
                } else {
                    drops.add(CraftItemStack.asBukkitCopy(new ItemStack(Items.WHEAT, 1, 0)));
                }
                //Give the "seeds" to the player. I run -1 iteration for "replant"
                for (int i = 0; i < (fortuneLevel + 3) - 1; i++) {
                    if (world.random.nextInt(15) <= age) {
                        if (block instanceof BlockCarrots) {
                            drops.add(CraftItemStack.asBukkitCopy(new ItemStack(Items.CARROT, 1, 0)));
                        } else if (block instanceof BlockPotatoes) {
                            drops.add(CraftItemStack.asBukkitCopy(new ItemStack(Items.POTATO, 1, 0)));
                            if (world.random.nextInt(50) == 0) {
                                drops.add(CraftItemStack.asBukkitCopy(new ItemStack(Items.POTATO_POISON, 1, 0)));
                            }
                        } else {
                            drops.add(CraftItemStack.asBukkitCopy(new ItemStack(Items.SEEDS, 1, 0)));
                        }
                    }
                }
            }
        } else if (block instanceof BlockCocoa) {
            if (age >= 2) {
                drops.add(CraftItemStack.asBukkitCopy(new ItemStack(Items.INK_SACK, 3, 3)));
            }
        } else if (block instanceof BlockNetherWart) {
            if (age >= 3) {
                int amount = 2 + world.random.nextInt(3);
                if (fortuneLevel > 0) {
                    amount += world.random.nextInt(fortuneLevel + 1);
                }
                drops.add(CraftItemStack.asBukkitCopy(new ItemStack(Items.NETHER_STALK, amount)));
            }
        }

        return drops;
    }

    @Override
    public int getExpFromBlock(org.bukkit.block.Block block, Player player) {
        World world = ((CraftWorld) block.getWorld()).getHandle();
        EntityPlayer entityPlayer = ((CraftPlayer) player).getHandle();
        return world.getType(block.getX(), block.getY(), block.getZ())
                .getExpDrop(world, block.getData(), EnchantmentManager.getBonusBlockLootEnchantmentLevel(entityPlayer));
    }

    @Override
    public int getTag(ToolItemStack toolItemStack, String key, int def) {
        ItemStack nmsStack = (ItemStack) toolItemStack.getNMSItem();
        NBTTagCompound tagCompound = nmsStack.getTag();
        return tagCompound == null || !tagCompound.hasKey(key) ? def : tagCompound.getInt(key);
    }

    @Override
    public void setTag(ToolItemStack toolItemStack, String key, int value) {
        ItemStack nmsStack = (ItemStack) toolItemStack.getNMSItem();
        NBTTagCompound tagCompound = nmsStack.getTag();
        if (tagCompound == null) {
            nmsStack.setTag(new NBTTagCompound());
            tagCompound = nmsStack.getTag();
        }
        tagCompound.setInt(key, value);
    }

    @Override
    public String getTag(ToolItemStack toolItemStack, String key, String def) {
        ItemStack nmsStack = (ItemStack) toolItemStack.getNMSItem();
        NBTTagCompound tagCompound = nmsStack.getTag();
        return tagCompound == null || !tagCompound.hasKey(key) ? def : tagCompound.getString(key);
    }

    @Override
    public void setTag(ToolItemStack toolItemStack, String key, String value) {
        ItemStack nmsStack = (ItemStack) toolItemStack.getNMSItem();
        NBTTagCompound tagCompound = nmsStack.getTag();
        if (tagCompound == null) {
            nmsStack.setTag(new NBTTagCompound());
            tagCompound = nmsStack.getTag();
        }
        tagCompound.setString(key, value);
    }

    @Override
    public void clearTasks(ToolItemStack toolItemStack) {
        ItemStack nmsStack = (ItemStack) toolItemStack.getNMSItem();
        NBTTagCompound tagCompound = nmsStack.getTag();
        if (tagCompound != null)
            tagCompound.remove("task-id");
    }

    @Override
    public void breakTool(ToolItemStack toolItemStack, Player player) {
        ItemStack nmsStack = (ItemStack) toolItemStack.getNMSItem();
        EntityPlayer entityPlayer = ((CraftPlayer) player).getHandle();

        entityPlayer.a(nmsStack);
        nmsStack.count -= 1;

        entityPlayer.a(StatisticList.BREAK_ITEM_COUNT[Item.getId(nmsStack.getItem())]);
        if (nmsStack.count == 0) {
            entityPlayer.bG();
        }

        if (nmsStack.count < 0)
            nmsStack.count = 0;

        if (nmsStack.count == 0)
            CraftEventFactory.callPlayerItemBreakEvent(entityPlayer, nmsStack);

        nmsStack.setData(0);
    }

    @Override
    public Object[] createSyncedItem(org.bukkit.inventory.ItemStack other) {
        CraftItemStack craftItemStack;
        ItemStack handle;
        if (other instanceof CraftItemStack) {
            craftItemStack = (CraftItemStack) other;
            handle = ITEM_STACK_HANDLE.get(other);
        } else {
            handle = CraftItemStack.asNMSCopy(other);
            craftItemStack = CraftItemStack.asCraftMirror(handle);
        }

        return new Object[]{craftItemStack, handle};
    }

    @Override
    public org.bukkit.inventory.ItemStack getItemInHand(Player player) {
        ItemStack itemStack = ((CraftInventoryPlayer) player.getInventory()).getInventory().getItemInHand();
        return CraftItemStack.asBukkitCopy(itemStack);
    }

    @Override
    public org.bukkit.inventory.ItemStack getItemInHand(Player player, Event e) {
        return getItemInHand(player);
    }

    @Override
    public boolean isFullyGrown(org.bukkit.block.Block block) {
        if (block.getState().getData() instanceof Crops)
            return ((Crops) block.getState().getData()).getState() == CropState.RIPE;
        else if (block.getState().getData() instanceof CocoaPlant)
            return ((CocoaPlant) block.getState().getData()).getSize() == CocoaPlant.CocoaPlantSize.LARGE;
        else if (block.getState().getData() instanceof NetherWarts)
            return ((NetherWarts) block.getState().getData()).getState() == NetherWartsState.RIPE;
        else if (block.getType() == Material.CARROT || block.getType() == Material.POTATO)
            return ((CraftBlock) block).getData() == CropState.RIPE.getData();

        return true;
    }

    @Override
    public void setCropState(org.bukkit.block.Block block, CropState cropState) {
        if (block.getType() == Material.COCOA) {
            CocoaPlant cocoaPlant = (CocoaPlant) block.getState().getData();
            switch (cropState) {
                case SEEDED:
                case GERMINATED:
                case VERY_SMALL:
                case SMALL:
                    cocoaPlant.setSize(CocoaPlant.CocoaPlantSize.SMALL);
                    break;
                case MEDIUM:
                    cocoaPlant.setSize(CocoaPlant.CocoaPlantSize.MEDIUM);
                    break;
                case TALL:
                case VERY_TALL:
                case RIPE:
                    cocoaPlant.setSize(CocoaPlant.CocoaPlantSize.LARGE);
                    break;
            }
            ((CraftBlock) block).setData(cocoaPlant.getData());
        } else if (block.getType() == Material.MELON_BLOCK || block.getType() == Material.PUMPKIN) {
            block.setType(Material.AIR);
        } else {
            ((CraftBlock) block).setData(cropState.getData());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<Player> getOnlinePlayers() {
        CraftServer craftServer = (CraftServer) Bukkit.getServer();
        List<CraftPlayer> players = new ArrayList<>();
        try {
            Field playersField = craftServer.getClass().getDeclaredField("playerView");
            playersField.setAccessible(true);
            players = (List<CraftPlayer>) playersField.get(craftServer);
            playersField.setAccessible(false);
        } catch (Exception ignored) {
        }
        return Collections.unmodifiableCollection(players);
    }

    @Override
    public void setBlockFast(Location location, int combinedId) {
        World world = ((CraftWorld) location.getWorld()).getHandle();
        Chunk chunk = world.getChunkAt(location.getChunk().getX(), location.getChunk().getZ());

        int x = location.getBlockX(), y = location.getBlockY(), z = location.getBlockZ();

        if (combinedId == 0)
            world.a(null, 2001, x, y, z, Block.getId(world.getType(x, y, z)) + (world.getData(x, y, z) << 12));

        chunk.a(x & 0x0f, y, z & 0x0f, Block.getById(combinedId), 2);
    }

    @Override
    public void refreshChunk(org.bukkit.Chunk bukkitChunk, Set<Location> blocksList) {
        Chunk chunk = ((CraftChunk) bukkitChunk).getHandle();
        int blocksAmount = blocksList.size();
        short[] values = new short[blocksAmount];

        Location firstLocation = null;

        int counter = 0;
        for (Location location : blocksList) {
            if (firstLocation == null)
                firstLocation = location;

            values[counter++] = (short) ((location.getBlockX() & 15) << 12 | (location.getBlockZ() & 15) << 8 | location.getBlockY());
        }

        sendPacketToRelevantPlayers((WorldServer) chunk.world, chunk.locX, chunk.locZ,
                new PacketPlayOutMultiBlockChange(blocksAmount, values, chunk));
    }

    @Override
    public int getCombinedId(Location location) {
        World world = ((CraftWorld) location.getWorld()).getHandle();
        return Block.getId(world.getType(location.getBlockX(), location.getBlockY(), location.getBlockZ()));
    }

    @Override
    public int getFarmlandId() {
        return Block.getId(Blocks.SOIL);
    }

    @Override
    public void setCombinedId(Location location, int combinedId) {
        World world = ((CraftWorld) location.getWorld()).getHandle();
        world.setTypeAndData(location.getBlockX(), location.getBlockY(), location.getBlockZ(), Block.getById(combinedId), 2, 18);
    }

    @Override
    public Enchantment getGlowEnchant() {
        return new Enchantment(101) {
            @Override
            public String getName() {
                return "WildToolsGlow";
            }

            @Override
            public int getMaxLevel() {
                return 1;
            }

            @Override
            public int getStartLevel() {
                return 0;
            }

            @Override
            public EnchantmentTarget getItemTarget() {
                return null;
            }

            @Override
            public boolean conflictsWith(Enchantment enchantment) {
                return false;
            }

            @Override
            public boolean canEnchantItem(org.bukkit.inventory.ItemStack itemStack) {
                return true;
            }
        };
    }

    @Override
    public boolean isOutsideWorldborder(Location location) {
        return false;
    }

    @Override
    public BlockPlaceEvent getFakePlaceEvent(Player player, Location location, org.bukkit.block.Block copyBlock) {
        FakeCraftBlock fakeBlock = FakeCraftBlock.at(location, copyBlock.getType());
        org.bukkit.block.Block original = location.getBlock();
        return new BlockPlaceEvent(
                fakeBlock,
                original.getState(),
                fakeBlock.getRelative(BlockFace.DOWN),
                new org.bukkit.inventory.ItemStack(copyBlock.getType()),
                player,
                true
        );
    }

    @Override
    public void playPickupAnimation(LivingEntity livingEntity, org.bukkit.entity.Item item) {
        EntityLiving entityLiving = ((CraftLivingEntity) livingEntity).getHandle();
        EntityItem entityItem = (EntityItem) ((CraftItem) item).getHandle();
        ((WorldServer) entityLiving.world).getTracker().a(entityItem, new PacketPlayOutCollect(entityItem.getId(), entityLiving.getId()));
    }

    @Override
    public boolean isAxeType(Material material) {
        return Items.DIAMOND_AXE.getDestroySpeed(new ItemStack(Items.DIAMOND_AXE), CraftMagicNumbers.getBlock(material)) == 8.0F;
    }

    @Override
    public boolean isShovelType(Material material) {
        return Items.DIAMOND_SPADE.getDestroySpeed(new ItemStack(Items.DIAMOND_SPADE), CraftMagicNumbers.getBlock(material)) == 8.0F;
    }

    @Override
    public Object getDroppedItem(org.bukkit.inventory.ItemStack itemStack, Location location) {
        WorldServer world = ((CraftWorld) location.getWorld()).getHandle();
        EntityItem entityitem = new EntityItem(world, location.getX(), location.getY(), location.getZ(), CraftItemStack.asNMSCopy(itemStack));
        entityitem.pickupDelay = 10;
        return entityitem;
    }

    @Override
    public void dropItems(List<Object> droppedItemsRaw) {
        droppedItemsRaw.removeIf(droppedItem -> !(droppedItem instanceof EntityItem));

        for (Object entityItem : droppedItemsRaw) {
            if (canMerge((EntityItem) entityItem)) {
                for (Object otherEntityItem : droppedItemsRaw) {
                    if (entityItem != otherEntityItem && canMerge((EntityItem) otherEntityItem)) {
                        if (mergeEntityItems((EntityItem) entityItem, (EntityItem) otherEntityItem))
                            break;
                    }
                }
            }
        }

        droppedItemsRaw.forEach(droppedItemObject -> {
            EntityItem entityItem = (EntityItem) droppedItemObject;
            if (entityItem.isAlive()) {
                entityItem.world.addEntity(entityItem);
            }
        });
    }

    private static boolean canMerge(EntityItem entityItem) {
        ItemStack itemStack = entityItem.getItemStack();
        return itemStack.count < itemStack.getMaxStackSize();
    }

    private static boolean mergeEntityItems(EntityItem entityItem, EntityItem otherEntity) {
        ItemStack itemOfEntity = entityItem.getItemStack();
        ItemStack itemOfOtherEntity = otherEntity.getItemStack();
        if (canMergeTogether(itemOfEntity, itemOfOtherEntity)) {
            mergeItems(entityItem, itemOfEntity, otherEntity, itemOfOtherEntity);
            entityItem.pickupDelay = Math.max(entityItem.pickupDelay, otherEntity.pickupDelay);
            if (itemOfOtherEntity.count <= 0) {
                otherEntity.die();
            }
        }

        return entityItem.dead;
    }

    private static boolean canMergeTogether(ItemStack itemStack, ItemStack otherItem) {
        if (itemStack.getItem() != otherItem.getItem())
            return false;

        if (itemStack.count + otherItem.count > otherItem.getMaxStackSize())
            return false;

        if (itemStack.hasTag() ^ otherItem.hasTag())
            return false;

        return !otherItem.hasTag() || otherItem.getTag().equals(itemStack.getTag());
    }

    private static void mergeItems(EntityItem entityItem, ItemStack itemStack, EntityItem otherEntity, ItemStack otherItem) {
        int amountLeftUntilFullStack = Math.min(itemStack.getMaxStackSize() - itemStack.count, otherItem.count);
        ItemStack itemStackClone = itemStack.cloneItemStack();
        itemStackClone.count += amountLeftUntilFullStack;
        entityItem.setItemStack(itemStackClone);
        otherItem.count -= amountLeftUntilFullStack;
        if (otherItem.count <= 0) {
            otherEntity.setItemStack(otherItem);
        }
    }

    private void sendPacketToRelevantPlayers(WorldServer worldServer, int chunkX, int chunkZ, Packet packet) {
        PlayerChunkMap playerChunkMap = worldServer.getPlayerChunkMap();
        for (Object entityHuman : worldServer.players) {
            if (entityHuman instanceof EntityPlayer && playerChunkMap.a((EntityPlayer) entityHuman, chunkX, chunkZ))
                ((EntityPlayer) entityHuman).playerConnection.sendPacket(packet);
        }
    }

    private static class FakeCraftBlock extends CraftBlock {

        private Material blockType;

        FakeCraftBlock(CraftChunk craftChunk, int x, int y, int z, Material material) {
            super(craftChunk, x, y, z);
            this.blockType = material;
        }

        @Override
        public Material getType() {
            return blockType;
        }

        @Override
        public void setType(Material type) {
            this.blockType = type;
            super.setType(type);
        }

        static FakeCraftBlock at(Location location, Material type) {
            CraftChunk craftChunk = (CraftChunk) location.getChunk();
            return new FakeCraftBlock(craftChunk, location.getBlockX(), location.getBlockY(), location.getBlockZ(), type);
        }

    }

}
