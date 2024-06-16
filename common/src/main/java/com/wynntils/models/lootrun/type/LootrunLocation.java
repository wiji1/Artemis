/*
 * Copyright © Wynntils 2023.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.lootrun.type;

import com.wynntils.utils.type.Pair;

public enum LootrunLocation {
    SILENT_EXPANSE(new Pair<>(1500, -1000), new Pair<>(400, -300)),
    CORKUS(new Pair<>(-1200, -3400), new Pair<>(-1800, -2100)),
    MOLTEN_HEIGHTS_HIKE(new Pair<>(1500, -5600), new Pair<>(1000, -5000)),
    SKY_ISLANDS_EXPLORATION(new Pair<>(1500, -4900), new Pair<>(800, -4400)),
    UNKNOWN(null, null),
    ;

    private final Pair<Integer, Integer> northEastCorner;
    private final Pair<Integer, Integer> southWestCorner;

    LootrunLocation(Pair<Integer, Integer> northEastCorner, Pair<Integer, Integer> southWestCorner) {
        this.northEastCorner = northEastCorner;
        this.southWestCorner = southWestCorner;
    }

    public Pair<Integer, Integer> getNorthEastCorner() {
        return northEastCorner;
    }

    public Pair<Integer, Integer> getSouthWestCorner() {
        return southWestCorner;
    }
}
