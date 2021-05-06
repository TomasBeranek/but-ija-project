package ija.project.warehouse;

import java.util.*;

import javafx.scene.shape.Circle;
import javafx.geometry.Point2D;


/** Represents a node on which a cart can travel. The node holds the information
 *  about all of it's current reachable neighbours. The class is inherited from
 *  the Circle class in JavaFX.
 *
 * @author Tomas Beranek (xberan46)
 * @author Simon Slobodnik (xslobo06)
 */
public class NodeCircle extends Circle {
  public int ID = -1;
  private int x;
  private int y;
  private Set<Integer> neighbours = new HashSet<>();


  /**
   * @param x The node's x coordinate.
   * @param y The node's y coordinate.
   * @param r The node's radius for a visualization.
   * @param id The node's ID.
   */
  public NodeCircle(float x, float y, float r, int id){
    super(x, y, r);
    this.ID = id;
    this.x = (int)Math.round(x);
    this.y = (int)Math.round(y);
  }


  /** Gets the node's x coordinate.
   *
   * @return The node's x coordinate.
   */
  public int getX() {
    return this.x;
  }


  /** Gets the node's y coordinate.
   *
   * @return The node's y coordinate.
   */
  public int getY() {
    return this.y;
  }


  /** Sets the node's x coordinate.
   *
   * @param x The node's x coordinate.
   */
  public void setX(int x) {
    this.x = x;
    this.setCenterX(x);
  }


  /** Sets the node's y coordinate.
   *
   * @param y The node's y coordinate.
   */
  public void setY(int y) {
    this.y = y;
    this.setCenterY(y);
  }


  /** Gets the list of the node's neighbours.
   *
   * @return The list of the node's neighbours.
   */
  public List<Integer> getNeighbours() {
    return new ArrayList<>(neighbours);
  }


  /** Adds a node to the node's neighbours.
   *
   * @param id The neighbour's ID.
   * @return True - if the neighbour has already been added.
   */
  public boolean addNeighbour(int id) {
    return this.neighbours.add(id);
  }


  /** Removes a node from the node's neighbours.
   *
   * @param id The neighbour's ID.
   * @return True - if the neighbour was present.
   */
  public boolean removeNeighbour(int id) {
    return this.neighbours.remove(id);
  }


  /** Calculates the distance between this node and given node.
   *
   * @param p A node to which a distance should be calculated.
   * @return The distance between this node in 'p'.
   */
  public int distance(NodeCircle p) {
    int x = p.getX();
    int y = p.getY();

    Point2D p1 = new Point2D(this.x, this.y);
    Point2D p2 = new Point2D(x, y);

    return (int)p1.distance(p2);
  }
}
