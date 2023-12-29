/*
 * Copyright © Wynntils 2022-2023.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.items.items.game;

import com.wynntils.models.character.type.ClassType;
import com.wynntils.models.gear.type.ConsumableType;
import com.wynntils.models.gear.type.GearTier;
import com.wynntils.models.items.properties.CraftedItemProperty;
import com.wynntils.models.items.properties.GearTierItemProperty;
import com.wynntils.models.items.properties.LeveledItemProperty;
import com.wynntils.models.items.properties.UsesItemProperty;
import com.wynntils.models.stats.type.StatActualValue;
import com.wynntils.models.stats.type.StatPossibleValues;
import com.wynntils.models.stats.type.StatType;
import com.wynntils.models.wynnitem.type.ItemEffect;
import com.wynntils.models.wynnitem.type.NamedItemEffect;
import com.wynntils.utils.type.CappedValue;
import java.util.List;

public class CraftedConsumableItem extends GameItem
        implements UsesItemProperty, GearTierItemProperty, LeveledItemProperty, CraftedItemProperty {
    private final String name;
    private final ConsumableType consumableType;
    private final int level;
    private final List<StatActualValue> identifications;
    private final List<NamedItemEffect> namedEffects;
    private final List<ItemEffect> effects;
    private final CappedValue uses;

    public CraftedConsumableItem(
            String name,
            ConsumableType consumableType,
            int level,
            List<StatActualValue> identifications,
            List<NamedItemEffect> namedEffects,
            List<ItemEffect> effects,
            CappedValue uses) {
        this.name = name;
        this.consumableType = consumableType;
        this.level = level;
        this.identifications = identifications;
        this.namedEffects = namedEffects;
        this.effects = effects;
        this.uses = uses;
    }

    public String getName() {
        return name;
    }

    public ConsumableType getConsumableType() {
        return consumableType;
    }

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public List<StatType> getStatTypes() {
        return identifications.stream().map(StatActualValue::statType).toList();
    }

    @Override
    public List<StatActualValue> getIdentifications() {
        return identifications;
    }

    @Override
    public List<StatPossibleValues> getPossibleValues() {
        // We can't know the possible values of crafted consumables
        return List.of();
    }

    @Override
    public ClassType getRequiredClass() {
        // Crafted consumables can be used by any class
        return null;
    }

    public List<NamedItemEffect> getNamedEffects() {
        return namedEffects;
    }

    public List<ItemEffect> getEffects() {
        return effects;
    }

    @Override
    public CappedValue getUses() {
        return uses;
    }

    @Override
    public GearTier getGearTier() {
        return GearTier.CRAFTED;
    }

    public boolean isHealing() {
        return getEffects().stream().anyMatch(e -> e.type().equals("Heal"));
    }

    @Override
    public String toString() {
        return "CraftedConsumableItem{" + "name='"
                + name + '\'' + ", consumableType="
                + consumableType + ", level="
                + level + ", identifications="
                + identifications + ", namedEffects="
                + namedEffects + ", effects="
                + effects + ", uses="
                + uses + '}';
    }
}
