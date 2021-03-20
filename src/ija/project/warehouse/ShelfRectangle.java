package ija.project.warehouse;

import javafx.scene.shape.Rectangle;
import java.util.*;


public class ShelfRectangle extends Rectangle {
  public int shelfID = -1;
  private Hashtable<String, Integer> goods = new Hashtable<>();

  public ShelfRectangle(int x, int y, int w, int h, int id) {
    super(x, y, w, h);
    this.shelfID = id;
  }
}
