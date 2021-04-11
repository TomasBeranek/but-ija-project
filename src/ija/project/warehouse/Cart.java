package ija.project.warehouse;

import javafx.scene.shape.Circle;
import java.util.*;
import javafx.scene.paint.Color;
import javafx.util.*;

/** Represents a cart which picks up the goods. The class is inherited from
 *  the Circle class in JavaFX.
 *
 * @author Tomas Beranek (xberan46)
 */
public class Cart extends Circle {
  private int lastNode = -1; //last visited node
  private int lastEpochTime = -1;
  private int capacity = 500;
  private List<Pair<Integer, Pair<String, Integer>>> path = null;
  private int waitUntilTime = -1;


  /**
   * @param x The node's x coordinate.
   * @param y The node's y coordinate.
   * @param r The node's radius for a visualization.
   * @param id The node's ID.
   */
  public Cart(float x, float y, float r){
    super(x, y, r);
  }


  /** Stores the given sequence of nodes (path).
   *
   * @param path The sequence of nodes (path).
   */
  public void addPath(List<Pair<Integer, Pair<String, Integer>>> path) {
    this.path = path;
  }


  /** Updates cart's position to current the given time.
   *
   * @param currentEpochTime The current epoch time.
   */
  public void updatePosition(Integer currentEpochTime, Hashtable<Integer, NodeCircle> nodes){
    int nodeID = this.path.get(0).getKey();

    this.setRadius(100); //make sure it is visible
    this.setFill(Color.BLACK);
    this.setCenterX(nodes.get(nodeID).getX());
    this.setCenterY(nodes.get(nodeID).getY());
  }
}
