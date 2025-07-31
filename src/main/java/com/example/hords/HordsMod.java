package com.example.hords;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HordsMod implements ModInitializer {
    public static final String MOD_ID = "hords";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Hords Mod wird geladen...");
        
        // Events registrieren
        ServerTickEvents.END_SERVER_TICK.register(HordsSpawner::onServerTick);
        
        // Boss Death Event registrieren (nur für Loot)
        BossDeathHandler.registerEvents();
        
        // ENTFERNT: Achievement Events
        // HordsAchievements.registerEvents();
        
        // Commands registrieren
        CommandRegistrationCallback.EVENT.register(HordCommands::register);
        
        LOGGER.info("Hords Mod erfolgreich geladen!");
    }
}