package ija.project.warehouse;

import javafx.scene.shape.Rectangle;
import java.util.*;


public class ShelfRectangle extends Rectangle {
  public int shelfID = -1;
  private Hashtable<String, Integer> goods = new Hashtable<>();
  private String goodsName;
  private Integer goodsQuantity;

  public ShelfRectangle(int x, int y, int w, int h, int id) {
    super(x, y, w, h);
    this.shelfID = id;
  }


  public void addGoods(String name, Integer quantity) {
    this.goodsName = name;
    this.goodsQuantity = quantity;
  }


  public String getGoods() {
    return this.goodsName;
  }


  public Integer getQuantity() {
    return this.goodsQuantity;
  }
}
