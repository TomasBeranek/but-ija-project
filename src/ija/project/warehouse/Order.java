package ija.project.warehouse;

import java.util.*;
import javafx.util.*;


public class Order {

  private List<Pair<String, Integer>> goods = new ArrayList<>();
  private Integer startEpochTime = 0;
  private Integer endEpochTime = Integer.MAX_VALUE;

  // cart handling in orders?

  public Order(Integer epochTime, List<Pair<String, Integer>> goods) {
    this.startEpochTime = epochTime;
    this.goods = goods;
  }

  public boolean isFinished (Integer currentEpochTime) {
    if (currentEpochTime >= endEpochTime)
      return true;
    else
      return false;
  }
}
