package com.example.hords;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;

public class BossDeathHandler {
    
    public static void registerEvents() {
        ServerLivingEntityEvents.AFTER_DEATH.register(BossDeathHandler::onEntityDeath);
    }
    
    private static void onEntityDeath(LivingEntity entity, DamageSource damageSource) {
        if (entity.getWorld() instanceof ServerWorld world) {
            BossLootHandler.handleBossDeath(entity, world);
        }
    }
}