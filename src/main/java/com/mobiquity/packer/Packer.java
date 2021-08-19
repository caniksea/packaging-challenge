package com.mobiquity.packer;

import com.mobiquity.domain.Item;
import com.mobiquity.domain.PackageDetail;
import com.mobiquity.exception.APIException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class Packer {

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
    String temp = itemStr.replace("(", "");
    temp = temp.replace(")", "");
    String itemDataArr[] = temp.split(",");
    int index = Integer.parseInt(itemDataArr[0]);
    BigDecimal weight = new BigDecimal(itemDataArr[1]);
    BigDecimal cost = new BigDecimal(itemDataArr[2].substring(1));
    if (maxWeight.compareTo(weight) > 0) {
      Item item = new Item(index, weight, cost);
      return Optional.of(item);
    }
    return Optional.empty();
  }

  /**
   * transform line into object of PackageDetail
   *
   * @param line
   * @return
   */
  private static PackageDetail transformLine(String line) {
    String data[] = line.split(":");
    BigDecimal maxWeight = new BigDecimal(data[0].trim());
    String itemArr[] = data[1].trim().split(" ");
    Stream<String> itemArrList = Arrays.stream(itemArr);
    List<Item> packageItems = itemArrList.map(itemStr -> Packer.getItems(maxWeight, itemStr))
            .filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
    Collections.sort(packageItems, Comparator.comparing(Item::getWeight)); // sort by weight asc
    Collections.sort(packageItems, (i1, i2) -> i2.getCost().compareTo(i1.getCost())); // then sort by cost desc
    return new PackageDetail(maxWeight, packageItems);
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
      if ( maxWeight.compareTo(itemWeight) > 0) {
        maxWeight = maxWeight.subtract(itemWeight);
        items.add(item);
        packageWeight = packageWeight.add(itemWeight);
      }
      if (packageWeight.compareTo(packageDetail.getMaxWeight()) >= 0) break;
    }
    String ret = items.stream().map(item -> String.valueOf(item.getIndex())).collect(Collectors.joining(","));
    ret = ret.isEmpty() ? "-" : ret;
    return ret;
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
