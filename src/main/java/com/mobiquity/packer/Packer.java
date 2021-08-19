package com.mobiquity.packer;

import com.mobiquity.Constant;
import com.mobiquity.domain.Item;
import com.mobiquity.domain.PackageDetail;
import com.mobiquity.exception.APIException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class Packer {

  private static int lineCounter = 0, itemCounter = 0; // counters used for error reporting.
  private final String ITEM_INDEX = "index";

  private Packer() {
  }

  /**
   * Transform item string into item, then
   * if item weight is less than max weight return item object
   *
   * @param maxWeight
   * @param itemStr
   * @return
   */
  private static Optional<Item> getItems(BigDecimal maxWeight, String itemStr) {
    itemCounter++;
    String temp = itemStr.replace("(", "");
    temp = temp.replace(")", "");
    String itemDataArr[] = temp.split(",");
    if (itemDataArr.length < 3) throw new RuntimeException(String.format("Invalid item data at position %s, line %s", itemCounter, lineCounter));
    String indexStr = itemDataArr[0].trim();
    int index = getIntegerItemValue(indexStr, Constant.ITEM_INDEX.VALUE);
    String weightStr = itemDataArr[1].trim();
    String costStr = itemDataArr[2].trim();
    BigDecimal weight = getBDItemValue(weightStr, Constant.ITEM_WEIGHT.VALUE);
    BigDecimal cost = getBDItemValue(costStr, Constant.ITEM_COST.VALUE);
    if (maxWeight.compareTo(weight) >= 0) {
      Item item = new Item(index, weight, cost);
      return Optional.of(item);
    }
    return Optional.empty();
  }

  private static BigDecimal getBDItemValue(String data, String dataType) {
    checkNullItem(data, dataType);
    if (dataType.equalsIgnoreCase(Constant.ITEM_COST.VALUE) && !NumberUtils.isParsable(data))
        data = data.substring(1);
    if (!NumberUtils.isParsable(data)) throw new RuntimeException(String.format("Invalid value for item %s provided at line: %s, item data position: %s", dataType, lineCounter, itemCounter));
    return new BigDecimal(data);
  }

  private static void checkNullItem(String data, String dataType) {
    if (data.isEmpty()) throw new RuntimeException(String.format("No value for item %s provided at line: %s, item data position: %s", dataType, lineCounter, itemCounter));
  }

  private static int getIntegerItemValue(String data, String dataType) {
    checkNullItem(data, dataType);
    if (!NumberUtils.isParsable(data)) throw new RuntimeException(String.format("Invalid value for item %s provided at line: %s, item data position: %s", dataType, lineCounter, itemCounter));
    return Integer.parseInt(data);
  }

  /**
   * transform line into object of PackageDetail
   *
   * @param line
   * @return
   */
  private static PackageDetail transformLine(String line) {
    lineCounter++;
    String data[] = line.split(":");
    if (data.length < 2) throw new RuntimeException("Invalid line on file at line: " + lineCounter);
    String maxWeightStr = data[0].trim();
    BigDecimal maxWeight = getMaxWeight(maxWeightStr);
    String items = data[1].trim();
    if (items.isEmpty()) throw new RuntimeException("No items provided at line: " + lineCounter);
    String itemArr[] = items.split(" ");
    Stream<String> itemArrList = Arrays.stream(itemArr);
    List<Item> packageItems = itemArrList.map(itemStr -> Packer.getItems(maxWeight, itemStr))
            .flatMap(Optional::stream).collect(Collectors.toList());
    itemCounter = 0; // clear item counter.
    if (!packageItems.isEmpty() && packageItems.size() > 1) {
      log.info("Before sort: {}", packageItems);
      Collections.sort(packageItems, Comparator.comparing(Item::getWeight)); // sort by weight asc
      log.info("After weight sort: {}", packageItems);
      Collections.sort(packageItems, (i1, i2) -> i2.getCost().compareTo(i1.getCost())); // then sort by cost desc
      log.info("After cost sort: {}", packageItems);
    }
    return new PackageDetail(maxWeight, packageItems);
  }

  private static BigDecimal getMaxWeight(String data) {
    if (data.isEmpty()) throw new RuntimeException("No max weight provided at line: " + lineCounter);
    if (!NumberUtils.isParsable(data)) throw new RuntimeException("Invalid max weight provided at line: " + lineCounter);
    return new BigDecimal(data);
  }

  /**
   * Given an object of PackageDetail, determine suitable items based
   * on max weight constraint
   *
   * @param packageDetail
   * @return
   */
  private static String processPackageDetail(PackageDetail packageDetail) {
    BigDecimal maxWeight = packageDetail.getMaxWeight();
    BigDecimal packageWeight = BigDecimal.ZERO;
    List<Item> items = new ArrayList<>();
    for (Item item : packageDetail.getItems()) {
      BigDecimal itemWeight = item.getWeight();
      if (maxWeight.compareTo(itemWeight) >= 0) {
        items.add(item);
        maxWeight = maxWeight.subtract(itemWeight);
        packageWeight = packageWeight.add(itemWeight);
      }
      if (packageWeight.compareTo(packageDetail.getMaxWeight()) >= 0) break;
    }
    if (items.isEmpty()) {
      return "-";
    }
    if (items.size() > 1)
      Collections.sort(items, Comparator.comparing(Item::getIndex));
    return items.stream().map(item -> String.valueOf(item.getIndex())).collect(Collectors.joining(","));
  }

  /**
   * process line
   *
   * @param line
   * @return
   */
  private static String process(String line) {
    PackageDetail packageDetail = transformLine(line);
    return processPackageDetail(packageDetail);
  }

  public static String pack(String filePath) throws APIException {
    log.info("Reading file: {}", filePath);
    Path path = Paths.get(filePath);
    try(Stream<String> lines = Files.lines(path)) {
      Stream<String> itemsInPackage = lines.map(Packer::process);
      return itemsInPackage.collect(Collectors.joining("\n"));
    } catch (Exception e) {
      log.error("An error occurred: {}", e.getMessage());
      throw new APIException(e.getMessage(), e);
    }
  }
}
