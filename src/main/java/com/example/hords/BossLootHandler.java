package com.example.hords;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potions;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class BossLootHandler {
    
    private static final Random RANDOM = new Random();
    
    public static void handleBossDeath(LivingEntity boss, ServerWorld world) {
        // Prüfe ob es ein Boss ist (hat custom name mit "Leader", "Lord", "King", etc.)
        if (boss.getCustomName() == null) {
            return;
        }
        
        String customName = boss.getCustomName().getString();
        if (!isBossEntity(boss, customName)) {
            return;
        }
        
        Vec3d deathPos = boss.getPos();
        spawnBossLoot(world, deathPos, customName);
    }
    
    private static boolean isBossEntity(LivingEntity entity, String customName) {
        // Liste der Boss-Namen aus dem HordsSpawner
        return customName.contains("Leader") || 
               customName.contains("Lord") || 
               customName.contains("King") || 
               customName.contains("Warlord") || 
               customName.contains("Commander") || 
               customName.contains("Overlord") || 
               customName.contains("Wraith");
    }
    
    private static void spawnBossLoot(ServerWorld world, Vec3d deathPos, String bossName) {
        // Loot wird IMMER gedroppt (keine Chests mehr)
        dropLootItems(world, deathPos, bossName);
    }
    
    private static void dropLootItems(ServerWorld world, Vec3d pos, String bossName) {
        List<ItemStack> lootItems = generateBossLoot();
        
        for (ItemStack item : lootItems) {
            if (!item.isEmpty()) {
                ItemEntity itemEntity = new ItemEntity(world, pos.x, pos.y + 1, pos.z, item);
                itemEntity.setVelocity(
                    (RANDOM.nextDouble() - 0.5) * 0.2,
                    RANDOM.nextDouble() * 0.2 + 0.1,
                    (RANDOM.nextDouble() - 0.5) * 0.2
                );
                world.spawnEntity(itemEntity);
            }
        }
        
        spawnExperienceOrbs(world, new BlockPos((int)pos.x, (int)pos.y, (int)pos.z));
    }
    
    private static List<ItemStack> generateBossLoot() {
        // Alle möglichen Loot-Items definieren
        List<PossibleLoot> allPossibleLoot = new ArrayList<>();
        
        // Häufige Items (50% Chance)
        allPossibleLoot.add(new PossibleLoot(Items.GOLDEN_APPLE, 1, 5, 0.50f));
        allPossibleLoot.add(new PossibleLoot(Items.DIAMOND, 1, 5, 0.50f));
        allPossibleLoot.add(new PossibleLoot(Items.EMERALD, 2, 7, 0.50f));
        allPossibleLoot.add(new PossibleLoot(Items.IRON_INGOT, 3, 12, 0.50f));
        allPossibleLoot.add(new PossibleLoot(Items.GOLD_INGOT, 2, 8, 0.50f));
        
        // Mittlere Items (20-33% Chance)
        allPossibleLoot.add(new PossibleLoot(Items.ENDER_PEARL, 1, 4, 0.20f));
        allPossibleLoot.add(new PossibleLoot(Items.EXPERIENCE_BOTTLE, 1, 8, 0.25f));
        allPossibleLoot.add(new PossibleLoot(Items.GOLDEN_CARROT, 1, 7, 0.20f));
        allPossibleLoot.add(new PossibleLoot(Items.TNT, 1, 6, 0.33f));
        allPossibleLoot.add(new PossibleLoot(Items.NAME_TAG, 1, 1, 0.33f));
        
        // Seltene Items (5-16.5% Chance)
        allPossibleLoot.add(new PossibleLoot(Items.DIAMOND_BLOCK, 1, 2, 0.165f));
        allPossibleLoot.add(new PossibleLoot(Items.ANCIENT_DEBRIS, 1, 4, 0.10f));
        allPossibleLoot.add(new PossibleLoot(Items.GHAST_TEAR, 1, 3, 0.075f));
        allPossibleLoot.add(new PossibleLoot(Items.TRIDENT, 1, 1, 0.05f));
        allPossibleLoot.add(new PossibleLoot(Items.SHULKER_SHELL, 1, 3, 0.10f));
        
        // Ultra-seltene Items (0.75-3.75% Chance)
        allPossibleLoot.add(new PossibleLoot(Items.ENCHANTED_GOLDEN_APPLE, 1, 1, 0.0075f));
        allPossibleLoot.add(new PossibleLoot(Items.NETHERITE_INGOT, 1, 1, 0.02f));
        allPossibleLoot.add(new PossibleLoot(Items.TOTEM_OF_UNDYING, 1, 1, 0.0375f));
        
        // Tränke separat handhaben
        if (RANDOM.nextFloat() < 0.50f) {
            int potions = 1 + RANDOM.nextInt(3);
            for (int i = 0; i < potions; i++) {
                ItemStack potion = createRandomPotion();
                allPossibleLoot.add(new PossibleLoot(potion, 1.0f)); // Garantiert, da bereits Chance geprüft
            }
        }
        
        // Liste mischen für Zufälligkeit
        Collections.shuffle(allPossibleLoot);
        
        // Maximal 5 verschiedene Items auswählen
        List<ItemStack> finalLoot = new ArrayList<>();
        int itemCount = 0;
        
        for (PossibleLoot possibleItem : allPossibleLoot) {
            if (itemCount >= 5) break; // Maximal 5 verschiedene Items
            
            if (possibleItem.shouldDrop()) {
                finalLoot.add(possibleItem.createItemStack());
                itemCount++;
            }
        }
        
        return finalLoot;
    }
    
    // Hilfsklasse für mögliche Loot-Items
    private static class PossibleLoot {
        private final net.minecraft.item.Item item;
        private final int minAmount;
        private final int maxAmount;
        private final float chance;
        private final ItemStack customStack; // Für Tränke
        
        // Konstruktor für normale Items
        public PossibleLoot(net.minecraft.item.Item item, int minAmount, int maxAmount, float chance) {
            this.item = item;
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
            this.chance = chance;
            this.customStack = null;
        }
        
        // Konstruktor für custom ItemStacks (Tränke)
        public PossibleLoot(ItemStack customStack, float chance) {
            this.item = null;
            this.minAmount = 1;
            this.maxAmount = 1;
            this.chance = chance;
            this.customStack = customStack;
        }
        
        public boolean shouldDrop() {
            return RANDOM.nextFloat() < chance;
        }
        
        public ItemStack createItemStack() {
            if (customStack != null) {
                return customStack.copy();
            }
            
            int amount = minAmount;
            if (maxAmount > minAmount) {
                amount += RANDOM.nextInt(maxAmount - minAmount + 1);
            }
            
            return new ItemStack(item, amount);
        }
    }
    
    private static ItemStack createRandomPotion() {
        ItemStack potion = new ItemStack(Items.POTION);
        
        // Verschiedene Trank-Arten
        switch (RANDOM.nextInt(8)) {
            case 0:
                potion.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Potions.HEALING));
                break;
            case 1:
                potion.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Potions.STRONG_HEALING));
                break;
            case 2:
                potion.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Potions.REGENERATION));
                break;
            case 3:
                potion.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Potions.STRENGTH));
                break;
            case 4:
                potion.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Potions.SWIFTNESS));
                break;
            case 5:
                potion.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Potions.FIRE_RESISTANCE));
                break;
            case 6:
                potion.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Potions.NIGHT_VISION));
                break;
            case 7:
                potion.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Potions.WATER_BREATHING));
                break;
        }
        
        return potion;
    }
    
    private static void spawnExperienceOrbs(ServerWorld world, BlockPos pos) {
        // Spawn 50-150 XP in mehreren Orbs
        int totalXP = 50 + RANDOM.nextInt(101);
        
        while (totalXP > 0) {
            int orbValue = Math.min(totalXP, 20 + RANDOM.nextInt(31)); // 20-50 XP pro Orb
            
            ExperienceOrbEntity orb = new ExperienceOrbEntity(world, 
                pos.getX() + 0.5 + (RANDOM.nextDouble() - 0.5) * 2,
                pos.getY() + 1,
                pos.getZ() + 0.5 + (RANDOM.nextDouble() - 0.5) * 2,
                orbValue);
            
            world.spawnEntity(orb);
            totalXP -= orbValue;
        }
    }
}