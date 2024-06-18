/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.lootrun;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.storage.Storage;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.labels.event.EntityLabelVisibilityEvent;
import com.wynntils.mc.event.ContainerClickEvent;
import com.wynntils.mc.event.RemoveEntitiesEvent;
import com.wynntils.mc.event.SetEntityDataEvent;
import com.wynntils.models.lootrun.event.LootrunFinishedEvent;
import com.wynntils.models.lootrun.type.LootrunLocation;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.type.Pair;
import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class SacrificeHologram extends Feature {
    private static final Pattern REWARD_CHEST_PATTERN = Pattern.compile("§b§lReward Chest");
    private static final Pattern SACRIFICE_BUTTON_PATTERN = Pattern.compile(".*§a§lConfirm Sacrifice.*");
    private static final Pattern CLOSE_CHEST_PATTERN = Pattern.compile(".*§c§lClose Chest.*");

    private final Map<LootrunLocation, ArmorStand> currentHolograms = new EnumMap<>(LootrunLocation.class);

    @Persisted
    private final Storage<Map<LootrunLocation, RewardChestInstance>> chestInstances =
            new Storage<>(new EnumMap<>(LootrunLocation.class));

    @SubscribeEvent
    public void onEntitySpawn(SetEntityDataEvent event) {
        ClientLevel level = McUtils.mc().level;
        Entity entity = level.getEntity(event.getId());

        if (entity instanceof ArmorStand && entity.getCustomName() != null) {
            if (REWARD_CHEST_PATTERN.matcher(entity.getCustomName().getString()).matches()) {
                LootrunLocation camp =
                        getClosestCamp(McUtils.player().getX(), McUtils.player().getZ());

                if (currentHolograms.get(camp) != null) {
                    ArmorStand oldStand = currentHolograms.get(camp);
                    oldStand.remove(Entity.RemovalReason.DISCARDED);
                }

                ArmorStand stand = new ArmorStand(level, entity.getX(), entity.getY() - 1.2, entity.getZ());
                level.addEntity(stand);
                stand.setCustomName(StyledText.fromString("§4§lSacrifice Pulls:§c " + getSacrificePulls(camp))
                        .getComponent());
                stand.setInvisible(true);
                stand.setCustomNameVisible(true);

                currentHolograms.put(camp, stand);
            }
        }
    }

    @SubscribeEvent
    public void onDespawn(RemoveEntitiesEvent event) {
        for (Integer entityId : event.getEntityIds()) {
            ClientLevel level = McUtils.mc().level;
            Entity entity = level.getEntity(entityId);

            if (entity instanceof ArmorStand && entity.getCustomName() != null) {
                if (REWARD_CHEST_PATTERN
                        .matcher(entity.getCustomName().getString())
                        .matches()) {
                }
            }
        }
    }

    @SubscribeEvent
    public void onLootrunComplete(LootrunFinishedEvent.Completed event) {
        LootrunLocation camp =
                getClosestCamp(McUtils.player().getX(), McUtils.player().getZ());

        chestInstances
                .get()
                .put(
                        camp,
                        new RewardChestInstance(
                                event.getRewardPulls(),
                                getSacrificePulls(camp),
                                event.getRewardSacrifices(),
                                event.getRewardRerolls()));

        chestInstances.touched();
    }

    //TODO: Make failing lootrun remove sac pulls

    //TODO: Detect gamemode switch for stand visibility

    @SubscribeEvent
    public void onItemClick(ContainerClickEvent event) {
        ItemStack itemStack = event.getItemStack();
        Component name = itemStack.getDisplayName();

        String itemName = name.getString().trim();

        Matcher matcher = SACRIFICE_BUTTON_PATTERN.matcher(name.getString());

        if (matcher.find()) {
            LootrunLocation camp =
                    getClosestCamp(McUtils.player().getX(), McUtils.player().getZ());

            RewardChestInstance instance = chestInstances.get().get(camp);
            if (instance == null) {
                WynntilsMod.warn("Could not find Reward Chest instance, removing sacrifice pulls.");
                return;
            }

            final int previousSacrificePulls = instance.sacrificePulls;

            if (instance.sacrifices > 0) {
                instance.sacrificePulls =
                        (int) ((instance.pulls + previousSacrificePulls) * (0.25 + (0.25 * instance.sacrifices)));
            }

            chestInstances.touched();
            return;
        }

        matcher = CLOSE_CHEST_PATTERN.matcher(name.getString());

        if (matcher.find()) {
            //TODO: Prevent preview from removing sac pulls
            WynntilsMod.info(event.getContainerMenu()
                    .getSlot(4)
                    .getItem()
                    .getDisplayName()
                    .getString());

            LootrunLocation camp =
                    getClosestCamp(McUtils.player().getX(), McUtils.player().getZ());

            RewardChestInstance instance = chestInstances.get().get(camp);
            if (instance == null) return;
            instance.sacrificePulls = 0;
            chestInstances.touched();
            return;
        }
    }

    private LootrunLocation getClosestCamp(double x, double z) {
        for (LootrunLocation value : LootrunLocation.values()) {
            Pair<Integer, Integer> northEast = value.getNorthEastCorner();
            Pair<Integer, Integer> southWest = value.getSouthWestCorner();

            if (northEast == null || southWest == null) continue;

            if (x > southWest.a() && x < northEast.a() && z < southWest.b() && z > northEast.b()) return value;
        }

        return LootrunLocation.UNKNOWN;
    }

    private int getSacrificePulls(LootrunLocation camp) {
        RewardChestInstance instance = chestInstances.get().get(camp);
        return instance == null ? 0 : instance.sacrificePulls;
    }

    public static class RewardChestInstance {
        public int pulls;
        public int sacrificePulls;

        public int sacrifices;
        public int rerolls;

        public RewardChestInstance(int pulls, int sacrificePulls, int sacrifices, int rerolls) {
            this.pulls = pulls;
            this.sacrificePulls = sacrificePulls;

            this.sacrifices = sacrifices;
            this.rerolls = rerolls;
        }
    }
}
