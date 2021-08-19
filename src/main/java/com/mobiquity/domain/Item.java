package com.mobiquity.domain;

import lombok.Value;

import java.math.BigDecimal;

@Value
public class Item {
    private int index;
    private BigDecimal weight, cost;
}
