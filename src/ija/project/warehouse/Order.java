package ija.project.warehouse;

import ija.project.warehouse.Cart;

import java.util.*;

import javafx.util.*;
import javafx.scene.Group;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;


/** Represents an order which consists of unlimited amount of pairs:
 *  name-quantity. The same type of goods can be specified multiple times.
 *
 * @author Tomas Beranek (xberan46)
 */
public class Order {
  public List<Pair<String, Integer>> goods = new ArrayList<>();
  private Long startEpochTime = 0L;
  private Long endEpochTime = Long.MAX_VALUE;
  private List<Pair<Integer, Pair<String, Integer>>> path = new ArrayList<>();
  public Cart cart = null;


  /**
   * @param epochTime The orders’s start time.
   * @param goods The list of goods.
   */
  public Order(Long epochTime, List<Pair<String, Integer>> goods) {
    this.startEpochTime = epochTime;
    this.goods = goods;
  }


  /** Checks if the order has already been processed.
   *
   * @param currentEpochTime The current time of the simulation.
   * @return True - if the order has already been processed.
   */
  public boolean isFinished(Long currentEpochTime) {
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
  public boolean isActive(Long currentEpochTime) {
    if (currentEpochTime >= startEpochTime && !this.isFinished(currentEpochTime))
      return true;
    else
      return false;
  }


  /** Passes the given sequence of nodes (path) to the cart.
   *
   * @param group The group object to which a cart is added to be visible.
   * @param path The sequence of nodes (path).
   * @param nodes The list of all the nodes.
   * @param shelves All the shelfs in the warehouse.
   * @param cartList The list into currently loaded goods is loaded upon clicking
   *                  on the cart.
   * @param capacity The cart's capacity.
   */
  public void addCart(Group group, List<Pair<Integer, Pair<String, Integer>>> path, Hashtable<Integer, NodeCircle> nodes, Hashtable<Integer, ShelfRectangle> shelves, ListView<String> cartList, int capacity) {
    int startX = nodes.get(path.get(0).getKey()).getX();
    int startY = nodes.get(path.get(0).getKey()).getY();

    //remove the old cart if there was any
    if (this.cart != null){
      //make the cart invisible
      this.cart.setRadius(0);
      group.getChildren().remove(this.cart);
    }

    this.cart = new Cart(startX, startY, 0, shelves, startEpochTime, cartList, capacity);
    group.getChildren().add(this.cart);
    this.cart.addPath(path, nodes);
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
   * @return false -- The path needs to be recalculated.
   *         true -- The cart's position has been updated successfully.
   */
  public boolean drawCart(Long currentEpochTime, Hashtable<Integer, NodeCircle> nodes){
    //if update position returns false, it was the last update, which means
    //that the order is finished
    String rc = this.cart.updatePosition(currentEpochTime, nodes);
    if (rc == "Finished"){
      this.endEpochTime = currentEpochTime;
      return true;
    } else if (rc == "Success"){
      //do nothing
      return true;
    } else if (rc == "Stopped"){
      //we need to recalculate the path
      return false;
    }

    return true;
  }


  /** Signals to the cart to update it's path.
   *
   * @param path The sequence of nodes (path).
   * @param nodes The list of all the nodes.
   */
  public void updateCartPath(List<Pair<Integer, Pair<String, Integer>>> path, Hashtable<Integer, NodeCircle> nodes){
    this.cart.addPath(path, nodes);
  }
}
