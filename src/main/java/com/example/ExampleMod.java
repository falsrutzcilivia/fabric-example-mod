package com.example;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server-side only. When ANY mannequin that has a custom name dies, the game's own
 * death message is broadcast with the mannequin's name in front, e.g.
 *   Reagon_ was slain by Steve using [My Sword]
 * (the item is shown only when it has a custom name, exactly like vanilla).
 */
public class ExampleMod implements ModInitializer {
    public static final String MOD_ID = "modid";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ServerLivingEntityEvents.AFTER_DEATH.register(this::onDeath);
    }

    /** Every mannequin that has a name. */
    boolean matches(LivingEntity entity) {
        if (!"minecraft:mannequin".equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString())) return false;
        return entity.getCustomName() != null;
    }

    void onDeath(LivingEntity entity, DamageSource source) {
        if (!matches(entity)) return;
        MinecraftServer server = entity.level().getServer();
        if (server == null) return;
        // the game builds the message itself: victim name, cause, killer, and "using [custom item name]"
        Component msg = source.getLocalizedDeathMessage(entity);
        server.getPlayerList().broadcastSystemMessage(msg, false);
    }
}
