package com.wynntils.models.lootrun;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.persisted.storage.Storage;
import com.wynntils.core.text.StyledText;
import com.wynntils.mc.event.RemoveEntitiesEvent;
import com.wynntils.mc.event.SetEntityDataEvent;
import com.wynntils.models.lootrun.type.LootrunLocation;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.type.Pair;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Pattern;

public class SacrificeHologramHandler {

    private static final Pattern REWARD_CHEST_PATTERN = Pattern.compile("§b§lReward Chest");

    private final Map<LootrunLocation, ArmorStand> currentHolograms = new EnumMap<>(LootrunLocation.class);
    private final Storage<Map<LootrunLocation, Integer>> sacrificePulls = new Storage<>(new EnumMap<>(LootrunLocation.class));

    @SubscribeEvent
    public void onEntitySpawn(SetEntityDataEvent event) {
        ClientLevel level = McUtils.mc().level;
        Entity entity = level.getEntity(event.getId());

        if (entity instanceof ArmorStand && entity.getCustomName() != null) {
            if (REWARD_CHEST_PATTERN.matcher(entity.getCustomName().getString()).matches()) {
                LootrunLocation camp = getClosestCamp(McUtils.player().getX(), McUtils.player().getZ());

                if (currentHolograms.get(camp) != null) {
                    ArmorStand oldStand = currentHolograms.get(camp);
                    oldStand.remove(Entity.RemovalReason.DISCARDED);
                }

                ArmorStand stand = new ArmorStand(level, entity.getX(), entity.getY() - 1.2, entity.getZ());
                level.addEntity(stand);
                stand.setCustomName(StyledText.fromString("§4§lSacrifice Pulls").getComponent());
                stand.setInvisible(true);
                stand.setCustomNameVisible(true);

                currentHolograms.put(camp, stand);
            }
        }
    }

    private LootrunLocation getClosestCamp(double x, double z) {

        for (LootrunLocation value : LootrunLocation.values()) {

            Pair<Integer, Integer> northEast = value.getNorthEastCorner();
            Pair<Integer, Integer> southWest = value.getSouthWestCorner();

            if (northEast == null || southWest == null) continue;

            if(x > southWest.a() && x < northEast.a()
                    && z > southWest.a() && z < southWest.b()) return value;
        }

        return LootrunLocation.UNKNOWN;
    }

    public void setSacrificePulls(LootrunLocation camp, int pulls) {
        sacrificePulls.get().put(camp, pulls);
    }

    public int getSacrificePulls(LootrunLocation camp) {
        return sacrificePulls.get().get(camp);
    }
}
