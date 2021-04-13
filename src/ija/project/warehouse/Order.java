package ija.project.warehouse;

import ija.project.warehouse.Cart;
import java.util.*;
import javafx.util.*;
import javafx.scene.Group;

/** Represents an order which consists of unlimited amount of pairs:
 *  name-quantity. The same type of goods can be specified multiple times.
 *
 * @author Tomas Beranek (xberan46)
 */
public class Order {

  public List<Pair<String, Integer>> goods = new ArrayList<>();
  private Integer startEpochTime = 0;
  private Integer endEpochTime = Integer.MAX_VALUE;
  private List<Pair<Integer, Pair<String, Integer>>> path = new ArrayList<>();
  private Cart cart = null;


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
  public boolean isFinished(Integer currentEpochTime) {
    if (currentEpochTime >= endEpochTime)
      return true;
    else
      return false;
  }


  /** Checks if the order is active at the given time.
   *
   * @param currentEpochTime The current time of the simulation.
   * @return True - if the order has already started and hasn't finished yet.
   */
  public boolean isActive(Integer currentEpochTime) {
    if (currentEpochTime >= startEpochTime && !this.isFinished(currentEpochTime))
      return true;
    else
      return false;
  }


  /** Passes the given sequence of nodes (path) to the cart.
   *
   * @param group The group object to which a cart is added to be visible.
   * @param path The sequence of nodes (path).
   */
  public void addCart(Group group, List<Pair<Integer, Pair<String, Integer>>> path) {
    this.cart = new Cart(0.0f, 0.0f, 0.0f);
    try {
      group.getChildren().add(this.cart);
    } catch(Exception e) {
      System.out.println(e);
    }
    this.cart.addPath(path);
  }


  /** Check is the order has assigned cart.
   *
   * @return True - if the order has assigned cart.
   */
  public boolean hasCart(){
    return this.cart != null;
  }


  /** Signals to the cart to update it's position.
   *
   * @param currentEpochTime Current simulation time.
   * @param nodes The list of all the nodes.
   */
  public void drawCart(Integer currentEpochTime, Hashtable<Integer, NodeCircle> nodes){
    this.cart.updatePosition(currentEpochTime, nodes);
  }
}
