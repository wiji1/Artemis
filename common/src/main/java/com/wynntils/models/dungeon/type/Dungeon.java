/*
 * Copyright © Wynntils 2022-2023.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.dungeon.type;

import com.wynntils.utils.EnumUtils;
import java.util.Arrays;
import java.util.stream.Collectors;

public enum Dungeon {
    DECREPIT_SEWERS,
    INFESTED_PIT,
    LOST_SANCTUARY(true),
    UNDERWORLD_CRYPT,
    TIMELOST_SANCTUM,
    SAND_SWEPT_TOMB("Sand-Swept Tomb"),
    ICE_BARROWS,
    UNDERGROWTH_RUINS,
    GALLEONS_GRAVEYARD("Galleon's Graveyard"),
    FALLEN_FACTORY,
    ELDRITCH_OUTLOOK;

    private final String name;
    private final boolean removed;

    Dungeon() {
        this(false);
    }

    Dungeon(boolean removed) {
        this.name = EnumUtils.toNiceString(name());
        this.removed = removed;
    }

    Dungeon(String name) {
        this.name = name;
        this.removed = false;
    }

    public static Dungeon fromName(String name) {
        for (Dungeon dungeon : values()) {
            if (dungeon.getName().equals(name)) {
                return dungeon;
            }
        }

        return null;
    }

    public String getName() {
        return name;
    }

    public boolean isRemoved() {
        return removed;
    }

    public String getInitials() {
        return Arrays.stream(name.split(" ", 2)).map(s -> s.substring(0, 1)).collect(Collectors.joining());
    }

    @Override
    public String toString() {
        return "Dungeon{" + "name='" + name + '\'' + ", removed=" + removed + '}';
    }
}
