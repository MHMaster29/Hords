package com.example.hords;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.minecraft.world.Heightmap;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Formatting;
import net.minecraft.enchantment.Enchantments;

import java.util.*;

public class HordsSpawner {
    
    private static final Random RANDOM = new Random();
    private static final int HORD_SPAWN_CHANCE = 1800;
    private static final int MIN_PLAYER_DISTANCE = 20;
    private static final int MAX_PLAYER_DISTANCE = 50;
    private static final int MIN_HORD_SIZE = 10;
    private static final int MAX_HORD_SIZE = 20;
    
    public enum HordType {
        ZOMBIE_HORD,
        SKELETON_HORD,
        MIXED_HORD,
        ZOMBIFIED_HORD,
        END_HORD,
        OCEAN_HORD,
        PIGLIN_HORD,
        MAGMACUBE_HORD,
        NETHERFORTRESS_HORD,
        NETHER_MIXED_HORD,
        SOULSAND_HORD
    }
    
    public static void onServerTick(MinecraftServer server) {
        // Überworld
        ServerWorld overworld = server.getOverworld();
        if (overworld.getDifficulty() != Difficulty.PEACEFUL) {
            for (PlayerEntity player : overworld.getPlayers()) {
                if (RANDOM.nextInt(HORD_SPAWN_CHANCE) == 0) {
                    spawnOverworldHord(overworld, player);
                }
            }
        }
        
        // Nether
        ServerWorld nether = server.getWorld(net.minecraft.world.World.NETHER);
        if (nether != null && nether.getDifficulty() != Difficulty.PEACEFUL) {
            for (PlayerEntity player : nether.getPlayers()) {
                if (RANDOM.nextInt(HORD_SPAWN_CHANCE) == 0) {
                    spawnNetherHord(nether, player);
                }
            }
        }
        
        // End
        ServerWorld end = server.getWorld(net.minecraft.world.World.END);
        if (end != null && end.getDifficulty() != Difficulty.PEACEFUL) {
            for (PlayerEntity player : end.getPlayers()) {
                if (RANDOM.nextInt(HORD_SPAWN_CHANCE) == 0) {
                    spawnEndHord(end, player);
                }
            }
        }
    }
    
    private static void spawnOverworldHord(ServerWorld world, PlayerEntity player) {
        boolean isInOcean = isPlayerInOcean(world, player.getBlockPos());
        
        // Prüfe ob es Nacht ist (nur für Nicht-Ocean Horden)
        if (!isInOcean && !isNightTime(world)) {
            return; // Spawne keine Horden außer Ocean-Horden während des Tages
        }
        
        HordType hordType;
        if (isInOcean) {
            hordType = HordType.OCEAN_HORD;
        } else {
            HordType[] overworldTypes = {HordType.ZOMBIE_HORD, HordType.SKELETON_HORD, HordType.MIXED_HORD};
            hordType = overworldTypes[RANDOM.nextInt(overworldTypes.length)];
        }
        
        Vec3d playerPos = player.getPos();
        BlockPos spawnPos;
        
        if (hordType == HordType.OCEAN_HORD) {
            spawnPos = findOceanSpawnPosition(world, playerPos);
        } else {
            spawnPos = findSpawnPosition(world, playerPos);
        }
        
        if (spawnPos == null) return;
        
        int hordSize = MIN_HORD_SIZE + RANDOM.nextInt(MAX_HORD_SIZE - MIN_HORD_SIZE);
        spawnSpecificHord(world, spawnPos, hordType, hordSize);
        
        Text message = Text.literal("A " + getHordTypeName(hordType) + " has appeared nearby!")
            .formatted(Formatting.RED, Formatting.BOLD);
        player.sendMessage(message, false);
    }
    
    // Neue Methode um zu prüfen ob es Nacht ist
    private static boolean isNightTime(ServerWorld world) {
        long timeOfDay = world.getTimeOfDay() % 24000L;
        // Nacht ist von 13000 bis 23000 (ungefähr von Sonnenuntergang bis Sonnenaufgang)
        return timeOfDay >= 13000L && timeOfDay <= 23000L;
    }
    
    private static void spawnNetherHord(ServerWorld world, PlayerEntity player) {
        BlockPos playerPos = player.getBlockPos();
        
        boolean isInBasaltDelta = isPlayerInBasaltDelta(world, playerPos);
        boolean isInNetherFortress = isPlayerInNetherFortress(world, playerPos);
        boolean isInSoulSandValley = isPlayerInSoulSandValley(world, playerPos);
        
        HordType hordType;
        if (isInSoulSandValley) {
            HordType[] soulSandTypes = {HordType.SOULSAND_HORD, HordType.ZOMBIFIED_HORD, HordType.PIGLIN_HORD, HordType.NETHER_MIXED_HORD};
            hordType = soulSandTypes[RANDOM.nextInt(soulSandTypes.length)];
        } else if (isInNetherFortress) {
            HordType[] fortressTypes = {HordType.NETHERFORTRESS_HORD, HordType.ZOMBIFIED_HORD, HordType.PIGLIN_HORD, HordType.NETHER_MIXED_HORD};
            hordType = fortressTypes[RANDOM.nextInt(fortressTypes.length)];
        } else if (isInBasaltDelta) {
            HordType[] basaltTypes = {HordType.MAGMACUBE_HORD, HordType.ZOMBIFIED_HORD, HordType.PIGLIN_HORD, HordType.NETHER_MIXED_HORD};
            hordType = basaltTypes[RANDOM.nextInt(basaltTypes.length)];
        } else {
            HordType[] netherTypes = {HordType.ZOMBIFIED_HORD, HordType.PIGLIN_HORD, HordType.NETHER_MIXED_HORD};
            hordType = netherTypes[RANDOM.nextInt(netherTypes.length)];
        }
        
        Vec3d playerPosVec = player.getPos();
        BlockPos spawnPos = findNetherSpawnPosition(world, playerPosVec);
        
        if (spawnPos == null) return;
        
        int hordSize = MIN_HORD_SIZE + RANDOM.nextInt(MAX_HORD_SIZE - MIN_HORD_SIZE);
        spawnSpecificHord(world, spawnPos, hordType, hordSize);
        
        Text message = Text.literal("A " + getHordTypeName(hordType) + " has appeared nearby!")
            .formatted(Formatting.DARK_RED, Formatting.BOLD);
        player.sendMessage(message, false);
    }
    
    private static void spawnEndHord(ServerWorld world, PlayerEntity player) {
        Vec3d playerPos = player.getPos();
        BlockPos spawnPos = findSpawnPosition(world, playerPos);
        
        if (spawnPos == null) return;
        
        int hordSize = MIN_HORD_SIZE + RANDOM.nextInt(MAX_HORD_SIZE - MIN_HORD_SIZE);
        spawnSpecificHord(world, spawnPos, HordType.END_HORD, hordSize);
        
        Text message = Text.literal("An " + getHordTypeName(HordType.END_HORD) + " has appeared nearby!")
            .formatted(Formatting.DARK_PURPLE, Formatting.BOLD);
        player.sendMessage(message, false);
    }
    
    public static void spawnSpecificHord(ServerWorld world, BlockPos center, HordType type, int size) {
        switch (type) {
            case ZOMBIE_HORD:
                spawnZombieHordInternal(world, center, size);
                break;
            case SKELETON_HORD:
                spawnSkeletonHordInternal(world, center, size);
                break;
            case MIXED_HORD:
                spawnMixedHordInternal(world, center, size);
                break;
            case ZOMBIFIED_HORD:
                spawnZombifiedHordInternal(world, center, size);
                break;
            case END_HORD:
                spawnEndHordInternal(world, center, size);
                break;
            case OCEAN_HORD:
                spawnOceanHordInternal(world, center, size);
                break;
            case PIGLIN_HORD:
                spawnPiglinHordInternal(world, center, size);
                break;
            case MAGMACUBE_HORD:
                spawnMagmacubeHordInternal(world, center, size);
                break;
            case NETHERFORTRESS_HORD:
                spawnNetherFortressHordInternal(world, center, size);
                break;
            case NETHER_MIXED_HORD:
                spawnNetherMixedHordInternal(world, center, size);
                break;
            case SOULSAND_HORD:
                spawnSoulSandHordInternal(world, center, size);
                break;
        }
    }
    
    // Hilfsmethode um einen "mächtigen" Bogen zu erstellen
    private static ItemStack createPowerfulBow() {
        // Da Verzauberungen in 1.21.8 kompliziert sind, geben wir einfach einen normalen Bogen
        // Die Bosse bekommen dafür sehr starke Effekte um den Schaden zu kompensieren
        return new ItemStack(Items.BOW);
    }
    
    private static void spawnZombieHordInternal(ServerWorld world, BlockPos center, int size) {
        ZombieEntity boss = new ZombieEntity(world);
        boss.setPosition(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
        createBoss(boss);
        world.spawnEntity(boss);
        
        // Zombie-Typen für mehr Vielfalt
        List<EntityType<? extends HostileEntity>> zombieTypes = Arrays.asList(
            EntityType.ZOMBIE,
            EntityType.ZOMBIE,
            EntityType.ZOMBIE, // Mehr normale Zombies
            EntityType.ZOMBIE_VILLAGER,
            EntityType.HUSK
        );
        
        for (int i = 0; i < size - 1; i++) {
            EntityType<? extends HostileEntity> mobType = zombieTypes.get(RANDOM.nextInt(zombieTypes.size()));
            HostileEntity mob = mobType.create(world, SpawnReason.COMMAND);
            
            if (mob != null) {
                BlockPos spawnPos = getRandomPositionAround(center, 5);
                mob.setPosition(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                
                // Zombies haben keine Waffen (kämpfen mit Händen)
                if (RANDOM.nextFloat() < 0.3f) {
                    enhanceMob(mob);
                }
                
                world.spawnEntity(mob);
            }
        }
    }
    
    private static void spawnSkeletonHordInternal(ServerWorld world, BlockPos center, int size) {
        SkeletonEntity boss = new SkeletonEntity(EntityType.SKELETON, world);
        boss.setPosition(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
        createBoss(boss);
        
        // HIER IST DIE ÄNDERUNG: Skelett-Boss bekommt mächtigen Bogen und sehr starke Effekte
        boss.equipStack(EquipmentSlot.MAINHAND, createPowerfulBow());
        // Sehr starke Effekte um Power 5 + Flame zu simulieren
        boss.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, Integer.MAX_VALUE, 4, false, false)); // Stärke 5
        boss.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false)); // Feuerresistenz
        // Der Boss-Bogen wird praktisch wie Power 5 + Flame wirken durch die Stärke
        
        world.spawnEntity(boss);
        
        // Skeleton-Typen für mehr Vielfalt
        List<EntityType<? extends HostileEntity>> skeletonTypes = Arrays.asList(
            EntityType.SKELETON,
            EntityType.SKELETON,
            EntityType.SKELETON, // Mehr normale Skelette
            EntityType.STRAY,
            EntityType.BOGGED
        );
        
        for (int i = 0; i < size - 1; i++) {
            EntityType<? extends HostileEntity> mobType = skeletonTypes.get(RANDOM.nextInt(skeletonTypes.size()));
            HostileEntity mob = mobType.create(world, SpawnReason.COMMAND);
            
            if (mob != null) {
                BlockPos spawnPos = getRandomPositionAround(center, 5);
                mob.setPosition(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                
                // Alle Skeleton-Typen bekommen normale Bögen
                mob.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
                mob.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.085F);
                
                if (RANDOM.nextFloat() < 0.3f) {
                    enhanceMob(mob);
                }
                
                world.spawnEntity(mob);
            }
        }
    }
    
    private static void spawnMixedHordInternal(ServerWorld world, BlockPos center, int size) {
        ZombieEntity boss = new ZombieEntity(world);
        boss.setPosition(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
        createBoss(boss);
        world.spawnEntity(boss);
        
        for (int i = 0; i < size - 1; i++) {
            int mobChoice = RANDOM.nextInt(5); // Erweitert für mehr Mob-Typen
            
            if (mobChoice == 0) {
                ZombieEntity zombie = new ZombieEntity(world);
                BlockPos spawnPos = getRandomPositionAround(center, 7);
                zombie.setPosition(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                
                // Zombies haben keine Waffen (kämpfen mit Händen)
                if (RANDOM.nextFloat() < 0.4f) {
                    enhanceMob(zombie);
                }
                
                world.spawnEntity(zombie);
                
            } else if (mobChoice == 1) {
                SkeletonEntity skeleton = new SkeletonEntity(EntityType.SKELETON, world);
                BlockPos spawnPos = getRandomPositionAround(center, 7);
                skeleton.setPosition(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                
                skeleton.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
                skeleton.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.085F);
                
                if (RANDOM.nextFloat() < 0.4f) {
                    enhanceMob(skeleton);
                }
                
                world.spawnEntity(skeleton);
                
            } else if (mobChoice == 2) {
                StrayEntity stray = new StrayEntity(EntityType.STRAY, world);
                BlockPos spawnPos = getRandomPositionAround(center, 7);
                stray.setPosition(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                
                stray.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
                stray.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.085F);
                
                if (RANDOM.nextFloat() < 0.4f) {
                    enhanceMob(stray);
                }
                
                world.spawnEntity(stray);
                
            } else if (mobChoice == 3) {
                BoggedEntity bogged = new BoggedEntity(EntityType.BOGGED, world);
                BlockPos spawnPos = getRandomPositionAround(center, 7);
                bogged.setPosition(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                
                bogged.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
                bogged.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.085F);
                
                if (RANDOM.nextFloat() < 0.4f) {
                    enhanceMob(bogged);
                }
                
                world.spawnEntity(bogged);
                
            } else {
                CreeperEntity creeper = new CreeperEntity(EntityType.CREEPER, world);
                BlockPos spawnPos = getRandomPositionAround(center, 7);
                creeper.setPosition(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                
                // Creepers haben keine Waffen (explodieren)
                if (RANDOM.nextFloat() < 0.4f) {
                    enhanceMob(creeper);
                }
                
                world.spawnEntity(creeper);
            }
        }
    }
    
    private static void spawnZombifiedHordInternal(ServerWorld world, BlockPos center, int size) {
        ZombifiedPiglinEntity boss = new ZombifiedPiglinEntity(EntityType.ZOMBIFIED_PIGLIN, world);
        boss.setPosition(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
        
        if (boss.getAttributeInstance(EntityAttributes.MAX_HEALTH) != null) {
            boss.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(40.0D);
            boss.setHealth(40.0F);
        }
        
        boss.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, Integer.MAX_VALUE, 2, false, false));
        boss.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, Integer.MAX_VALUE, 1, false, false));
        boss.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, Integer.MAX_VALUE, 2, false, false));
        
        // Gemischte Rüstung und Schwert für Zombified Piglin Boss
        boss.equipStack(EquipmentSlot.MAINHAND, new ItemStack(RANDOM.nextBoolean() ? Items.IRON_SWORD : Items.DIAMOND_SWORD));
        boss.equipStack(EquipmentSlot.FEET, new ItemStack(RANDOM.nextBoolean() ? Items.IRON_BOOTS : Items.DIAMOND_BOOTS));
        boss.equipStack(EquipmentSlot.LEGS, new ItemStack(RANDOM.nextBoolean() ? Items.IRON_LEGGINGS : Items.DIAMOND_LEGGINGS));
        boss.equipStack(EquipmentSlot.CHEST, new ItemStack(RANDOM.nextBoolean() ? Items.IRON_CHESTPLATE : Items.DIAMOND_CHESTPLATE));
        boss.equipStack(EquipmentSlot.HEAD, new ItemStack(RANDOM.nextBoolean() ? Items.IRON_HELMET : Items.DIAMOND_HELMET));
        
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            boss.setEquipmentDropChance(slot, 0.0F);
        }
        
        boss.setCustomName(Text.literal("Hord Leader").formatted(Formatting.RED, Formatting.BOLD));
        boss.setCustomNameVisible(true);
        
        world.spawnEntity(boss);
        
        for (int i = 0; i < size - 1; i++) {
            ZombifiedPiglinEntity zombifiedPiglin = new ZombifiedPiglinEntity(EntityType.ZOMBIFIED_PIGLIN, world);
            BlockPos spawnPos = getRandomPositionAround(center, 6);
            zombifiedPiglin.setPosition(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
            
            // Zombified Piglins haben normalerweise goldene Schwerter
            zombifiedPiglin.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_SWORD));
            zombifiedPiglin.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.085F);
            
            if (RANDOM.nextFloat() < 0.3f) {
                enhanceMob(zombifiedPiglin);
            }
            
            world.spawnEntity(zombifiedPiglin);
        }
    }
    
    private static void spawnEndHordInternal(ServerWorld world, BlockPos center, int size) {
        EndermanEntity boss = new EndermanEntity(EntityType.ENDERMAN, world);
        boss.setPosition(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
        
        if (boss.getAttributeInstance(EntityAttributes.MAX_HEALTH) != null) {
            boss.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(80.0D);
            boss.setHealth(80.0F);
        }
        
        boss.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, Integer.MAX_VALUE, 2, false, false));
        boss.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, Integer.MAX_VALUE, 1, false, false));
        boss.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, Integer.MAX_VALUE, 2, false, false));
        boss.setCustomName(Text.literal("Void Lord").formatted(Formatting.DARK_PURPLE, Formatting.BOLD));
        boss.setCustomNameVisible(true);
        
        world.spawnEntity(boss);
        
        for (int i = 0; i < size - 1; i++) {
            // 70% Endermen, 30% Shulkers
            if (RANDOM.nextFloat() < 0.7f) {
                EndermanEntity enderman = new EndermanEntity(EntityType.ENDERMAN, world);
                BlockPos spawnPos = getRandomPositionAround(center, 8);
                enderman.setPosition(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                
                if (RANDOM.nextFloat() < 0.4f) {
                    enhanceMob(enderman);
                }
                
                world.spawnEntity(enderman);
            } else {
                // Shulker spawnen
                ShulkerEntity shulker = new ShulkerEntity(EntityType.SHULKER, world);
                BlockPos spawnPos = getRandomPositionAround(center, 8);
                // Shulkers spawnen etwas höher da sie sich an Blöcke heften
                spawnPos = new BlockPos(spawnPos.getX(), spawnPos.getY() + 1, spawnPos.getZ());
                shulker.setPosition(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                
                if (RANDOM.nextFloat() < 0.3f) {
                    if (shulker.getAttributeInstance(EntityAttributes.MAX_HEALTH) != null) {
                        double currentHealth = shulker.getAttributeInstance(EntityAttributes.MAX_HEALTH).getBaseValue();
                        shulker.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(currentHealth * 1.3);
                        shulker.setHealth(shulker.getMaxHealth());
                    }
                }
                
                world.spawnEntity(shulker);
            }
        }
    }
    
    private static void spawnOceanHordInternal(ServerWorld world, BlockPos center, int size) {
        ElderGuardianEntity boss = new ElderGuardianEntity(EntityType.ELDER_GUARDIAN, world);
        boss.setPosition(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
        
        if (boss.getAttributeInstance(EntityAttributes.MAX_HEALTH) != null) {
            boss.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(160.0D);
            boss.setHealth(160.0F);
        }
        
        boss.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, Integer.MAX_VALUE, 2, false, false));
        boss.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, Integer.MAX_VALUE, 1, false, false));
        boss.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, Integer.MAX_VALUE, 2, false, false));
        boss.setCustomName(Text.literal("Ocean Lord").formatted(Formatting.DARK_AQUA, Formatting.BOLD));
        boss.setCustomNameVisible(true);
        
        world.spawnEntity(boss);
        
        for (int i = 0; i < size - 1; i++) {
            int mobChoice = RANDOM.nextInt(3);
            
            if (mobChoice == 0 || mobChoice == 1) {
                DrownedEntity drowned = new DrownedEntity(EntityType.DROWNED, world);
                BlockPos spawnPos = getRandomPositionAround(center, 8);
                drowned.setPosition(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                
                // Drowned haben keine Waffen - kämpfen mit den Händen
                drowned.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                drowned.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0F);
                
                if (RANDOM.nextFloat() < 0.3f) {
                    enhanceMob(drowned);
                }
                
                world.spawnEntity(drowned);
                
            } else {
                GuardianEntity guardian = new GuardianEntity(EntityType.GUARDIAN, world);
                BlockPos spawnPos = getRandomPositionAround(center, 8);
                guardian.setPosition(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                
                // Guardians haben keine Waffen, verwenden Laser
                if (RANDOM.nextFloat() < 0.3f) {
                    if (guardian.getAttributeInstance(EntityAttributes.MAX_HEALTH) != null) {
                        double currentHealth = guardian.getAttributeInstance(EntityAttributes.MAX_HEALTH).getBaseValue();
                        guardian.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(currentHealth * 1.2);
                        guardian.setHealth(guardian.getMaxHealth());
                    }
                }
                
                world.spawnEntity(guardian);
            }
        }
    }
    
    private static void spawnPiglinHordInternal(ServerWorld world, BlockPos center, int size) {
        PiglinBruteEntity boss = new PiglinBruteEntity(EntityType.PIGLIN_BRUTE, world);
        boss.setPosition(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
        
        if (boss.getAttributeInstance(EntityAttributes.MAX_HEALTH) != null) {
            boss.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(100.0D);
            boss.setHealth(100.0F);
        }
        
        boss.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, Integer.MAX_VALUE, 2, false, false));
        boss.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, Integer.MAX_VALUE, 1, false, false));
        boss.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, Integer.MAX_VALUE, 2, false, false));
        
        // Goldene Ausrüstung
        boss.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_AXE));
        boss.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
        boss.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.GOLDEN_CHESTPLATE));
        
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            boss.setEquipmentDropChance(slot, 0.0F);
        }
        
        boss.setCustomName(Text.literal("Piglin Warlord").formatted(Formatting.GOLD, Formatting.BOLD));
        boss.setCustomNameVisible(true);
        
        world.spawnEntity(boss);
        
        for (int i = 0; i < size - 1; i++) {
            PiglinEntity piglin = new PiglinEntity(EntityType.PIGLIN, world);
            BlockPos spawnPos = getRandomPositionAround(center, 6);
            piglin.setPosition(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
            
            // Normale Piglin-Waffen
            if (RANDOM.nextBoolean()) {
                piglin.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
            } else {
                piglin.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_SWORD));
            }
            piglin.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.15F);
            
            if (RANDOM.nextFloat() < 0.4f) {
                enhanceMob(piglin);
            }
            
            world.spawnEntity(piglin);
        }
    }
    
    private static void spawnMagmacubeHordInternal(ServerWorld world, BlockPos center, int size) {
        MagmaCubeEntity boss = new MagmaCubeEntity(EntityType.MAGMA_CUBE, world);
        boss.setPosition(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
        boss.setSize(4, true);
        
        if (boss.getAttributeInstance(EntityAttributes.MAX_HEALTH) != null) {
            boss.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(80.0D);
            boss.setHealth(80.0F);
        }
        
        boss.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, Integer.MAX_VALUE, 2, false, false));
        boss.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, Integer.MAX_VALUE, 1, false, false));
        boss.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, Integer.MAX_VALUE, 2, false, false));
        boss.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));
        boss.setCustomName(Text.literal("Molten King").formatted(Formatting.RED, Formatting.BOLD));
        boss.setCustomNameVisible(true);
        
        world.spawnEntity(boss);
        
        for (int i = 0; i < size - 1; i++) {
            MagmaCubeEntity magmaCube = new MagmaCubeEntity(EntityType.MAGMA_CUBE, world);
            BlockPos spawnPos = getRandomPositionAround(center, 8);
            magmaCube.setPosition(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
            
            // Magma Cubes haben verschiedene Größen
            int size_choice = RANDOM.nextInt(10);
            if (size_choice < 2) {
                magmaCube.setSize(1, true); // Klein
            } else if (size_choice < 7) {
                magmaCube.setSize(2, true); // Mittel
            } else {
                magmaCube.setSize(3, true); // Groß
            }
            
            // Magma Cubes haben keine Waffen, springen und verursachen Kontaktschaden
            if (RANDOM.nextFloat() < 0.4f) {
                if (magmaCube.getAttributeInstance(EntityAttributes.MAX_HEALTH) != null) {
                    double currentHealth = magmaCube.getAttributeInstance(EntityAttributes.MAX_HEALTH).getBaseValue();
                    magmaCube.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(currentHealth * 1.4);
                    magmaCube.setHealth(magmaCube.getMaxHealth());
                }
                
                magmaCube.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));
            }
            
            world.spawnEntity(magmaCube);
        }
    }
    
    private static void spawnNetherFortressHordInternal(ServerWorld world, BlockPos center, int size) {
        WitherSkeletonEntity boss = new WitherSkeletonEntity(EntityType.WITHER_SKELETON, world);
        boss.setPosition(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
        
        if (boss.getAttributeInstance(EntityAttributes.MAX_HEALTH) != null) {
            boss.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(40.0D);
            boss.setHealth(40.0F);
        }
        
        boss.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, Integer.MAX_VALUE, 2, false, false));
        boss.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, Integer.MAX_VALUE, 1, false, false));
        boss.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, Integer.MAX_VALUE, 2, false, false));
        boss.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));
        
        // Gemischte Rüstung und Schwert
        boss.equipStack(EquipmentSlot.MAINHAND, new ItemStack(RANDOM.nextBoolean() ? Items.IRON_SWORD : Items.DIAMOND_SWORD));
        boss.equipStack(EquipmentSlot.FEET, new ItemStack(RANDOM.nextBoolean() ? Items.IRON_BOOTS : Items.DIAMOND_BOOTS));
        boss.equipStack(EquipmentSlot.LEGS, new ItemStack(RANDOM.nextBoolean() ? Items.IRON_LEGGINGS : Items.DIAMOND_LEGGINGS));
        boss.equipStack(EquipmentSlot.CHEST, new ItemStack(RANDOM.nextBoolean() ? Items.IRON_CHESTPLATE : Items.DIAMOND_CHESTPLATE));
        boss.equipStack(EquipmentSlot.HEAD, new ItemStack(RANDOM.nextBoolean() ? Items.IRON_HELMET : Items.DIAMOND_HELMET));
        
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            boss.setEquipmentDropChance(slot, 0.0F);
        }
        
        boss.setCustomName(Text.literal("Fortress Commander").formatted(Formatting.DARK_GRAY, Formatting.BOLD));
        boss.setCustomNameVisible(true);
        
        world.spawnEntity(boss);
        
        for (int i = 0; i < size - 1; i++) {
            if (RANDOM.nextFloat() < 0.6f) {
                WitherSkeletonEntity witherSkeleton = new WitherSkeletonEntity(EntityType.WITHER_SKELETON, world);
                BlockPos spawnPos = getRandomPositionAround(center, 6);
                witherSkeleton.setPosition(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                
                // Wither Skelette haben normalerweise Steinschwerter
                witherSkeleton.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));
                witherSkeleton.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.085F);
                
                if (RANDOM.nextFloat() < 0.4f) {
                    enhanceMob(witherSkeleton);
                }
                
                world.spawnEntity(witherSkeleton);
                
            } else {
                BlazeEntity blaze = new BlazeEntity(EntityType.BLAZE, world);
                BlockPos spawnPos = getRandomPositionAround(center, 8);
                blaze.setPosition(spawnPos.getX() + 0.5, spawnPos.getY() + 2, spawnPos.getZ() + 0.5);
                
                if (RANDOM.nextFloat() < 0.3f) {
                    if (blaze.getAttributeInstance(EntityAttributes.MAX_HEALTH) != null) {
                        double currentHealth = blaze.getAttributeInstance(EntityAttributes.MAX_HEALTH).getBaseValue();
                        blaze.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(currentHealth * 1.3);
                        blaze.setHealth(blaze.getMaxHealth());
                    }
                    
                    blaze.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));
                }
                
                world.spawnEntity(blaze);
            }
        }
    }
    
    private static void spawnNetherMixedHordInternal(ServerWorld world, BlockPos center, int size) {
        ZombifiedPiglinEntity boss = new ZombifiedPiglinEntity(EntityType.ZOMBIFIED_PIGLIN, world);
        boss.setPosition(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
        
        if (boss.getAttributeInstance(EntityAttributes.MAX_HEALTH) != null) {
            boss.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(40.0D);
            boss.setHealth(40.0F);
        }
        
        boss.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, Integer.MAX_VALUE, 2, false, false));
        boss.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, Integer.MAX_VALUE, 1, false, false));
        boss.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, Integer.MAX_VALUE, 2, false, false));
        boss.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));
        
        // Gemischte Rüstung und Schwert
        boss.equipStack(EquipmentSlot.MAINHAND, new ItemStack(RANDOM.nextBoolean() ? Items.IRON_SWORD : Items.DIAMOND_SWORD));
        boss.equipStack(EquipmentSlot.FEET, new ItemStack(RANDOM.nextBoolean() ? Items.IRON_BOOTS : Items.DIAMOND_BOOTS));
        boss.equipStack(EquipmentSlot.LEGS, new ItemStack(RANDOM.nextBoolean() ? Items.IRON_LEGGINGS : Items.DIAMOND_LEGGINGS));
        boss.equipStack(EquipmentSlot.CHEST, new ItemStack(RANDOM.nextBoolean() ? Items.IRON_CHESTPLATE : Items.DIAMOND_CHESTPLATE));
        boss.equipStack(EquipmentSlot.HEAD, new ItemStack(RANDOM.nextBoolean() ? Items.IRON_HELMET : Items.DIAMOND_HELMET));
        
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            boss.setEquipmentDropChance(slot, 0.0F);
        }
        
        boss.setCustomName(Text.literal("Nether Overlord").formatted(Formatting.DARK_RED, Formatting.BOLD));
        boss.setCustomNameVisible(true);
        
        world.spawnEntity(boss);
        
        for (int i = 0; i < size - 1; i++) {
            int mobChoice = RANDOM.nextInt(3);
            
            if (mobChoice == 0) {
                PiglinEntity piglin = new PiglinEntity(EntityType.PIGLIN, world);
                BlockPos spawnPos = getRandomPositionAround(center, 7);
                piglin.setPosition(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                
                // Piglins haben normalerweise Crossbow oder goldenes Schwert
                if (RANDOM.nextBoolean()) {
                    piglin.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
                } else {
                    piglin.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_SWORD));
                }
                piglin.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.085F);
                
                if (RANDOM.nextFloat() < 0.3f) {
                    enhanceMob(piglin);
                }
                
                world.spawnEntity(piglin);
                
            } else if (mobChoice == 1) {
                ZombifiedPiglinEntity zombifiedPiglin = new ZombifiedPiglinEntity(EntityType.ZOMBIFIED_PIGLIN, world);
                BlockPos spawnPos = getRandomPositionAround(center, 7);
                zombifiedPiglin.setPosition(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                
                // Zombified Piglins haben normalerweise goldene Schwerter
                zombifiedPiglin.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_SWORD));
                zombifiedPiglin.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.085F);
                
                if (RANDOM.nextFloat() < 0.3f) {
                    enhanceMob(zombifiedPiglin);
                }
                
                world.spawnEntity(zombifiedPiglin);
                
            } else {
                BlazeEntity blaze = new BlazeEntity(EntityType.BLAZE, world);
                BlockPos spawnPos = getRandomPositionAround(center, 7);
                blaze.setPosition(spawnPos.getX() + 0.5, spawnPos.getY() + 3, spawnPos.getZ() + 0.5);
                
                // Blazes haben keine Waffen, feuern Feuerbälle
                if (RANDOM.nextFloat() < 0.3f) {
                    if (blaze.getAttributeInstance(EntityAttributes.MAX_HEALTH) != null) {
                        double currentHealth = blaze.getAttributeInstance(EntityAttributes.MAX_HEALTH).getBaseValue();
                        blaze.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(currentHealth * 1.2);
                        blaze.setHealth(blaze.getMaxHealth());
                    }
                }
                
                world.spawnEntity(blaze);
            }
        }
    }
    
    private static void spawnSoulSandHordInternal(ServerWorld world, BlockPos center, int size) {
        SkeletonEntity boss = new SkeletonEntity(EntityType.SKELETON, world);
        boss.setPosition(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
        
        if (boss.getAttributeInstance(EntityAttributes.MAX_HEALTH) != null) {
            boss.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(40.0D);
            boss.setHealth(40.0F);
        }
        
        boss.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, Integer.MAX_VALUE, 2, false, false));
        boss.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, Integer.MAX_VALUE, 1, false, false));
        boss.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, Integer.MAX_VALUE, 2, false, false));
        boss.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));
        
        // HIER IST AUCH EINE ÄNDERUNG: Soul Sand Hord Boss bekommt auch mächtigen Bogen und starke Effekte
        boss.equipStack(EquipmentSlot.MAINHAND, createPowerfulBow());
        // Sehr starke Effekte um Power 5 + Flame zu simulieren
        boss.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, Integer.MAX_VALUE, 4, false, false)); // Stärke 5
        boss.equipStack(EquipmentSlot.FEET, new ItemStack(RANDOM.nextBoolean() ? Items.IRON_BOOTS : Items.DIAMOND_BOOTS));
        boss.equipStack(EquipmentSlot.LEGS, new ItemStack(RANDOM.nextBoolean() ? Items.IRON_LEGGINGS : Items.DIAMOND_LEGGINGS));
        boss.equipStack(EquipmentSlot.CHEST, new ItemStack(RANDOM.nextBoolean() ? Items.IRON_CHESTPLATE : Items.DIAMOND_CHESTPLATE));
        boss.equipStack(EquipmentSlot.HEAD, new ItemStack(RANDOM.nextBoolean() ? Items.IRON_HELMET : Items.DIAMOND_HELMET));
        
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            boss.setEquipmentDropChance(slot, 0.0F);
        }
        
        boss.setCustomName(Text.literal("Soul Wraith").formatted(Formatting.DARK_BLUE, Formatting.BOLD));
        boss.setCustomNameVisible(true);
        
        world.spawnEntity(boss);
        
        for (int i = 0; i < size - 1; i++) {
            // 80% Skeletons, 20% Ghasts
            if (RANDOM.nextFloat() < 0.8f) {
                SkeletonEntity skeleton = new SkeletonEntity(EntityType.SKELETON, world);
                BlockPos spawnPos = getRandomPositionAround(center, 8);
                skeleton.setPosition(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                
                skeleton.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
                
                if (RANDOM.nextFloat() < 0.4f) {
                    enhanceMob(skeleton);
                }
                
                world.spawnEntity(skeleton);
            } else {
                // Ghast spawnen
                GhastEntity ghast = new GhastEntity(EntityType.GHAST, world);
                BlockPos spawnPos = getRandomPositionAround(center, 12); // Größerer Radius für Ghasts
                // Ghasts spawnen höher in der Luft
                spawnPos = new BlockPos(spawnPos.getX(), spawnPos.getY() + 8, spawnPos.getZ());
                ghast.setPosition(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                
                if (RANDOM.nextFloat() < 0.3f) {
                    if (ghast.getAttributeInstance(EntityAttributes.MAX_HEALTH) != null) {
                        double currentHealth = ghast.getAttributeInstance(EntityAttributes.MAX_HEALTH).getBaseValue();
                        ghast.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(currentHealth * 1.2);
                        ghast.setHealth(ghast.getMaxHealth());
                    }
                }
                
                world.spawnEntity(ghast);
            }
        }
    }
    
    private static void createBoss(HostileEntity boss) {
        if (boss.getAttributeInstance(EntityAttributes.MAX_HEALTH) != null) {
            boss.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(40.0D);
            boss.setHealth(40.0F);
        }
        
        boss.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, Integer.MAX_VALUE, 2, false, false));
        boss.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, Integer.MAX_VALUE, 1, false, false));
        boss.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, Integer.MAX_VALUE, 2, false, false));
        
        // Gemischte Rüstung aus Diamant und Eisen
        boss.equipStack(EquipmentSlot.FEET, new ItemStack(RANDOM.nextBoolean() ? Items.IRON_BOOTS : Items.DIAMOND_BOOTS));
        boss.equipStack(EquipmentSlot.LEGS, new ItemStack(RANDOM.nextBoolean() ? Items.IRON_LEGGINGS : Items.DIAMOND_LEGGINGS));
        boss.equipStack(EquipmentSlot.CHEST, new ItemStack(RANDOM.nextBoolean() ? Items.IRON_CHESTPLATE : Items.DIAMOND_CHESTPLATE));
        boss.equipStack(EquipmentSlot.HEAD, new ItemStack(RANDOM.nextBoolean() ? Items.IRON_HELMET : Items.DIAMOND_HELMET));
        
        // Schwert (Eisen oder Diamant) nur für Zombies
        if (boss instanceof ZombieEntity) {
            ItemStack weapon = RANDOM.nextBoolean() ? 
                new ItemStack(Items.IRON_SWORD) : new ItemStack(Items.DIAMOND_SWORD);
            boss.equipStack(EquipmentSlot.MAINHAND, weapon);
        }
        
        // Verhindere, dass die Rüstung gedroppt wird
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            boss.setEquipmentDropChance(slot, 0.0F);
        }
        
        boss.setCustomName(Text.literal("Hord Leader").formatted(Formatting.RED, Formatting.BOLD));
        boss.setCustomNameVisible(true);
    }
    
    private static void enhanceMob(HostileEntity mob) {
        int enhancement = RANDOM.nextInt(3);
        
        switch (enhancement) {
            case 0:
                if (mob.getAttributeInstance(EntityAttributes.MAX_HEALTH) != null) {
                    double currentHealth = mob.getAttributeInstance(EntityAttributes.MAX_HEALTH).getBaseValue();
                    mob.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(currentHealth * 1.5);
                    mob.setHealth(mob.getMaxHealth());
                }
                break;
            case 1:
                mob.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, Integer.MAX_VALUE, 0, false, false));
                break;
            case 2:
                if (RANDOM.nextBoolean()) {
                    mob.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
                }
                break;
        }
    }
    
    private static boolean isPlayerInOcean(ServerWorld world, BlockPos playerPos) {
        net.minecraft.registry.entry.RegistryEntry<net.minecraft.world.biome.Biome> biome = world.getBiome(playerPos);
        String biomeName = biome.getIdAsString();
        
        return biomeName.contains("ocean");
    }
    
    private static boolean isPlayerInSoulSandValley(ServerWorld world, BlockPos playerPos) {
        net.minecraft.registry.entry.RegistryEntry<net.minecraft.world.biome.Biome> biome = world.getBiome(playerPos);
        String biomeName = biome.getIdAsString();
        
        return biomeName.contains("soul_sand_valley");
    }
    
    private static boolean isPlayerInNetherFortress(ServerWorld world, BlockPos playerPos) {
        for (int x = -5; x <= 5; x++) {
            for (int y = -5; y <= 5; y++) {
                for (int z = -5; z <= 5; z++) {
                    BlockPos checkPos = playerPos.add(x, y, z);
                    if (world.getBlockState(checkPos).getBlock() == net.minecraft.block.Blocks.NETHER_BRICKS) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    private static boolean isPlayerInBasaltDelta(ServerWorld world, BlockPos playerPos) {
        net.minecraft.registry.entry.RegistryEntry<net.minecraft.world.biome.Biome> biome = world.getBiome(playerPos);
        String biomeName = biome.getIdAsString();
        
        return biomeName.contains("basalt_deltas");
    }
    
    private static BlockPos findOceanSpawnPosition(ServerWorld world, Vec3d playerPos) {
        for (int attempts = 0; attempts < 30; attempts++) {
            double angle = RANDOM.nextDouble() * 2 * Math.PI;
            double distance = MIN_PLAYER_DISTANCE + RANDOM.nextDouble() * (MAX_PLAYER_DISTANCE - MIN_PLAYER_DISTANCE);
            
            int x = (int) (playerPos.x + Math.cos(angle) * distance);
            int z = (int) (playerPos.z + Math.sin(angle) * distance);
            
            for (int y = world.getSeaLevel() - 15; y <= world.getSeaLevel() + 5; y++) {
                BlockPos pos = new BlockPos(x, y, z);
                
                if (world.getBlockState(pos).getBlock() == net.minecraft.block.Blocks.WATER &&
                    world.getBlockState(pos.up()).getBlock() == net.minecraft.block.Blocks.WATER &&
                    world.getBlockState(pos.up(2)).getBlock() == net.minecraft.block.Blocks.WATER) {
                    return pos;
                }
            }
        }
        return null;
    }
    
    private static BlockPos findSpawnPosition(ServerWorld world, Vec3d playerPos) {
        for (int attempts = 0; attempts < 20; attempts++) {
            double angle = RANDOM.nextDouble() * 2 * Math.PI;
            double distance = MIN_PLAYER_DISTANCE + RANDOM.nextDouble() * (MAX_PLAYER_DISTANCE - MIN_PLAYER_DISTANCE);
            
            int x = (int) (playerPos.x + Math.cos(angle) * distance);
            int z = (int) (playerPos.z + Math.sin(angle) * distance);
            int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
            
            BlockPos pos = new BlockPos(x, y, z);
            
            if (world.getBlockState(pos).isAir() && 
                world.getBlockState(pos.down()).isSolidBlock(world, pos.down())) {
                return pos;
            }
        }
        return null;
    }
    
    private static BlockPos findNetherSpawnPosition(ServerWorld world, Vec3d playerPos) {
        for (int attempts = 0; attempts < 30; attempts++) {
            double angle = RANDOM.nextDouble() * 2 * Math.PI;
            double distance = MIN_PLAYER_DISTANCE + RANDOM.nextDouble() * (MAX_PLAYER_DISTANCE - MIN_PLAYER_DISTANCE);
            
            int x = (int) (playerPos.x + Math.cos(angle) * distance);
            int z = (int) (playerPos.z + Math.sin(angle) * distance);
            
            for (int y = 10; y < 120; y++) {
                BlockPos pos = new BlockPos(x, y, z);
                BlockPos posAbove = pos.up();
                BlockPos posAbove2 = pos.up(2);
                
                if (world.getBlockState(pos).isSolidBlock(world, pos) && 
                    world.getBlockState(posAbove).isAir() && 
                    world.getBlockState(posAbove2).isAir() &&
                    world.getBlockState(posAbove2.up()).isAir() &&
                    !isNearLava(world, posAbove) &&
                    y < 115) {
                    
                    return posAbove;
                }
            }
        }
        return null;
    }
    
    private static boolean isNearLava(ServerWorld world, BlockPos pos) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos checkPos = pos.add(dx, dy, dz);
                    if (world.getBlockState(checkPos).getBlock() == net.minecraft.block.Blocks.LAVA) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    private static BlockPos getRandomPositionAround(BlockPos center, int radius) {
        double angle = RANDOM.nextDouble() * 2 * Math.PI;
        double distance = RANDOM.nextDouble() * radius;
        
        int x = center.getX() + (int) (Math.cos(angle) * distance);
        int z = center.getZ() + (int) (Math.sin(angle) * distance);
        
        return new BlockPos(x, center.getY(), z);
    }
    
    private static String getHordTypeName(HordType type) {
        switch (type) {
            case ZOMBIE_HORD: return "Zombie Hord";
            case SKELETON_HORD: return "Skeleton Hord";
            case MIXED_HORD: return "Mixed Hord";
            case ZOMBIFIED_HORD: return "Zombified Hord";
            case END_HORD: return "End Hord";
            case OCEAN_HORD: return "Ocean Hord";
            case PIGLIN_HORD: return "Piglin Hord";
            case MAGMACUBE_HORD: return "Magmacube Hord";
            case NETHERFORTRESS_HORD: return "Nether Fortress Hord";
            case NETHER_MIXED_HORD: return "Nether Mixed Hord";
            case SOULSAND_HORD: return "Soul Sand Hord";
            default: return "Unknown Hord";
        }
    }
}