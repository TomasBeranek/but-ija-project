package ija.project.warehouse;

import java.util.*;

import javafx.scene.shape.Rectangle;


/** Represents a shelf in a warehouse. A shelf can contain only a single type
 *  of goods. The class is inherited from the Rectangle class in JavaFX.
 *
 * @author Tomas Beranek (xberan46)
 */
public class ShelfRectangle extends Rectangle {
  public int shelfID = -1;
  public int nodeID = -1;
  private String goodsName;
  private Integer goodsQuantity;


  /**
   * @param x The shelf’s x coordinate of the top left corner.
   * @param y The shelf’s y coordinate of the top left corner.
   * @param w The shelf’s width.
   * @param h The shelf’s height.
   * @param ID The shelf’s ID.
   * @param nodeID The associated node's ID.
   */
  public ShelfRectangle(int x, int y, int w, int h, int ID, int nodeID) {
    super(x, y, w, h);
    this.shelfID = ID;
    this.nodeID = nodeID;
  }


  /** Overwrittes goods in the shelf.
   *
   * @param name The name of the goods.
   * @param quantity The quantinty of the goods in the shelf.
   */
  public void addGoods(String name, Integer quantity) {
    this.goodsName = name;
    this.goodsQuantity = quantity;
  }


  /** Gets the name of the goods stored in the shelf.
   *
   * @return The name of the goods.
   */
  public String getGoods() {
    return this.goodsName;
  }


  /** Gets the ID of the assigned node.
   *
   * @return The ID of the assigned node.
   */
  public int getNode() {
    return this.nodeID;
  }


  /** Gets the quantity of the goods stored in the shelf.
   *
   * @return The quantity of the goods.
   */
  public Integer getQuantity() {
    return this.goodsQuantity;
  }


  /** Sets the quantity of the goods stored in the shelf.
   *
   * @param quantity The new quantity of the goods.
   */
  public void setQuantity(Integer quantity) {
    this.goodsQuantity = quantity;
  }


  /** Deacreases the quantity of the goods stored in the shelf by toRemove.
   *
   * @param toRemove The new quantity of the goods.
   */
  public void decreaseQuantity(int toRemove) {
    this.goodsQuantity -= toRemove;
  }
}
