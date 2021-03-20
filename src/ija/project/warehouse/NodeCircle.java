package ija.project.warehouse;

import javafx.scene.shape.Circle;
import java.util.*;


public class NodeCircle extends Circle {
  public int ID = -1;
  private int x;
  private int y;
  //private set of neighbours

  public NodeCircle(float x, float y, float r, int id){
    super(x, y, r);
    this.ID = id;
    this.x = (int)Math.round(x);
    this.y = (int)Math.round(y);
  }

  public int getX() {
    return this.x;
  }

  public int getY() {
    return this.y;
  }

  public void setX(int x) {
    this.x = x;
    this.setCenterX(x);
  }

  public void setY(int y) {
    this.y = y;
    this.setCenterY(y);
  }

  public List<Integer> getNeighbours() {
    return null;
  }
}
