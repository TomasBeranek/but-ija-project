package ija.project.warehouse;

import java.util.*;
import javafx.util.*;

/** Represents an order which consists of unlimited amount of pairs:
 *  name-quantity. The same type of goods can be specified multiple times.
 *
 * @author Tomas Beranek (xberan46)
 */
public class Order {

  private List<Pair<String, Integer>> goods = new ArrayList<>();
  private Integer startEpochTime = 0;
  private Integer endEpochTime = Integer.MAX_VALUE;

  // cart handling in orders?


  /**
   * @param epochTime The orders’s start time.
   * @param goods The list of goods.
   */
  public Order(Integer epochTime, List<Pair<String, Integer>> goods) {
    this.startEpochTime = epochTime;
    this.goods = goods;
  }


  /** Checks if the order has already been processed.
   *
   * @param currentEpochTime The current time of the simulation.
   * @return True - if the order has already been processed.
   */
  public boolean isFinished (Integer currentEpochTime) {
    if (currentEpochTime >= endEpochTime)
      return true;
    else
      return false;
  }
}
