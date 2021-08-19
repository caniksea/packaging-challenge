package com.mobiquity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum Constant {
    ITEM_INDEX("index"), ITEM_WEIGHT("weight"),
    ITEM_COST("cost");

    public final String VALUE;
}
