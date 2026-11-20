package com.example;

import net.fabricmc.api.ModInitializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class ExampleMod implements ModInitializer {
    public static final String MOD_ID = "modid";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    static final String TAG = "NULL";
    static final Set<String> NAMES = new HashSet<>();

    @Override
    public void onInitialize() {
        loadNames();
        ServerLivingEntityEvents.AFTER_DEATH.register(this::onDeath);
    }

    void loadNames() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("nulldeath-names.json");
        if (!Files.exists(file)) {
            System.out.println("[nulldeath] config/nulldeath-names.json not found - only entities tagged " + TAG + " will be handled.");
            return;
        }
        try (Reader r = Files.newBufferedReader(file)) {
            JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
            for (JsonElement e : o.getAsJsonArray("names")) NAMES.add(e.getAsString());
            System.out.println("[nulldeath] loaded " + NAMES.size() + " names.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    boolean matches(LivingEntity entity) {
        if (entity.getTags().contains(TAG)) return true;
        if (!"minecraft:mannequin".equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString())) return false;
        Component n = entity.getCustomName();
        return n != null && NAMES.contains(n.getString());
    }

    void onDeath(LivingEntity entity, DamageSource source) {
        if (!matches(entity)) return;
        MinecraftServer server = entity.getServer();
        if (server == null) return;

        MutableComponent name = Component.literal("NULL").withStyle(ChatFormatting.OBFUSCATED);
        Entity attacker = source.getEntity();

        MutableComponent msg;
        if (source.is(DamageTypes.MACE_SMASH)) {
            msg = withWeapon(name.append(Component.literal(" was smashed by ")).append(who(attacker)), attacker);
        } else if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            msg = attacker != null
                    ? name.append(Component.literal(" was blown up by ")).append(attacker.getDisplayName())
                    : name.append(Component.literal(" blew up"));
        } else if (source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            msg = name.append(Component.literal(" fell out of the world"));
        } else if (source.is(DamageTypeTags.IS_FALL)) {
            msg = name.append(Component.literal(" fell from a high place"));
        } else if (attacker != null) {
            msg = withWeapon(name.append(Component.literal(" was slain by ")).append(who(attacker)), attacker);
        } else {
            msg = name.append(Component.literal(" died"));
        }
        server.getPlayerList().broadcastSystemMessage(msg, false);
    }

    static Component who(Entity attacker) {
        return attacker != null ? attacker.getDisplayName() : Component.literal("something");
    }

    static MutableComponent withWeapon(MutableComponent msg, Entity attacker) {
        if (attacker instanceof LivingEntity living) {
            ItemStack stack = living.getMainHandItem();
            if (!stack.isEmpty()) {
                msg.append(Component.literal(" using ")).append(stack.getHoverName());
            }
        }
        return msg;
    }
}
