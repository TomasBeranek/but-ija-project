package ija.project.warehouse;
import javafx.scene.shape.Rectangle;

public class ShelfRectangle extends Rectangle {
  public int shelfID = -1;

  public ShelfRectangle(int x, int y, int w, int h, int id) {
    super(x, y, w, h);
    this.shelfID = id;
  }
}
