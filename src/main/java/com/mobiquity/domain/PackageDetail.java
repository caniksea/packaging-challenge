package com.mobiquity.domain;

import lombok.Value;

import java.math.BigDecimal;
import java.util.List;

@Value
public class PackageDetail {
    private BigDecimal maxWeight;
    private List<Item> items;
}
