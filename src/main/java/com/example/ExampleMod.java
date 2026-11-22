package com.example;

import java.util.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

public class ExampleMod implements ModInitializer {
    public static volatile boolean enabled = false;
    private static final int WINDOW = 20;
    private static final Map<UUID, Long> lastPlace = new HashMap<>();
    private static final Map<UUID, Deque<Long>> intervals = new HashMap<>();

    @Override
    public void onInitialize() {
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (!level.isClientSide() && enabled
                    && player.getItemInHand(hand).is(Items.END_CRYSTAL)) {
                lastPlace.put(player.getUUID(), System.nanoTime());
            }
            return InteractionResult.PASS;
        });

        AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (level.isClientSide() || !enabled || !(entity instanceof EndCrystal)) {
                return InteractionResult.PASS;
            }
            Long placed = lastPlace.remove(player.getUUID());
            if (placed == null) return InteractionResult.PASS;

            long ms = (System.nanoTime() - placed) / 1_000_000;
            Deque<Long> q = intervals.computeIfAbsent(player.getUUID(), k -> new ArrayDeque<>());
            q.addLast(ms);
            if (q.size() >= WINDOW) {
                report(player, q);
                q.clear();
            }
            return InteractionResult.PASS;
        });
    }

    private static void report(Player player, Deque<Long> q) {
        double mean = q.stream().mapToLong(Long::longValue).average().orElse(0);
        double std = Math.sqrt(q.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2)).average().orElse(0));
        long distinct = q.stream().distinct().count();
        boolean suspicious = std < 6 && distinct < WINDOW * 0.5;

        if (player instanceof ServerPlayer sp) {
            sp.sendSystemMessage(Component.literal(String.format(
                "[CrystalCheck] mean=%.0fms std=%.1f distinct=%d/%d -> %s",
                mean, std, distinct, WINDOW, suspicious ? "SUSPICIOUS" : "human-like")));
        }
    }
}
