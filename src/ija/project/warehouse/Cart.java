package ija.project.warehouse;

import javafx.scene.shape.Circle;
import java.util.*;
import javafx.scene.paint.Color;
import javafx.util.*;
import javafx.animation.TranslateTransition;
import javafx.scene.*;


/** Represents a cart which picks up the goods. The class is inherited from
 *  the Circle class in JavaFX.
 *
 * @author Tomas Beranek (xberan46)
 */
public class Cart extends Circle {
  private int lastEpochTime = 0;
  private int capacity = 500;
  private List<Pair<Integer, Pair<String, Integer>>> path = null;
  private int waitUntilTime = -1;
  private int pathLen = 0;
  private int traveledLen = 0;
  private int speed = 2;     //pixels/s
  private TranslateTransition animation = null;


  /**
   * @param x The cart's x coordinate.
   * @param y The cart's y coordinate.
   * @param r The cart's radius for a visualization.
   */
  public Cart(float x, float y, float r){
    super(x, y, r);
    this.setCache(true);
    this.setCacheHint(CacheHint.SPEED);
  }


  public int getPathLen(Hashtable<Integer, NodeCircle> nodes) {
    if (this.path == null)
      return 0;

    int len = 0;
    int startNodeID;
    int nextNodeID;

    for (int i = 1; i < this.path.size(); i++){
      startNodeID = this.path.get(i-1).getKey();
      nextNodeID = this.path.get(i).getKey();

      len += nodes.get(startNodeID).distance(nodes.get(nextNodeID));
    }

    return len;
  }

  /** Stores the given sequence of nodes (path).
   *
   * @param path The sequence of nodes (path).
   * @param nodes The list of all the nodes.
   */
  public void addPath(List<Pair<Integer, Pair<String, Integer>>> path, Hashtable<Integer, NodeCircle> nodes) {
    this.path = path;
    this.pathLen = getPathLen(nodes);
  }


  /** Updates cart's position to current the given time.
   *
   * @param currentEpochTime The current epoch time.
   * @param nodes The list of all the nodes.
   */
  public boolean updatePosition(Integer currentEpochTime, Hashtable<Integer, NodeCircle> nodes){
    if (this.lastEpochTime == 0)
      this.lastEpochTime = currentEpochTime; //this the first update called

    int nodeID = this.path.get(0).getKey();

    //DEBUG:
    //System.out.println(this.pathLen);
    //for (int i = 0; i < this.path.size(); i++){
    //  System.out.print(this.path.get(i).getKey() + "  ");
    //}
    //System.out.println("");

    this.setRadius(10); //make sure it is visible
    this.setFill(Color.GREY);


    //index in path not node ID!
    Pair<Integer, Integer> p = getLastVisitedNodeIndexAndLen(nodes);
    int lastVisitedNodeIndex = p.getKey();
    int lastVisitedNodeDistance = p.getValue();
    NodeCircle lastNode = nodes.get(this.path.get(lastVisitedNodeIndex).getKey());
    NodeCircle nextNode = nodes.get(this.path.get(lastVisitedNodeIndex+1).getKey());
    int duration = currentEpochTime - this.lastEpochTime;
    int distanceToTravel = duration*this.speed;
    this.lastEpochTime = currentEpochTime;

    System.out.println("Last node: " + lastNode);
    System.out.println("Next node: " + nextNode);
    System.out.println("Traveled:  " + this.traveledLen);
    System.out.println("Last node ID:  " + nodes.get(this.path.get(lastVisitedNodeIndex).getKey()).ID);

    // 3 things can happen:
    //  1) the next position will be still between the same two points
    if (this.traveledLen + distanceToTravel < lastVisitedNodeDistance + lastNode.distance(nextNode)){
      System.out.println("Moving between nodes");

      float percentage = (float)(this.traveledLen + distanceToTravel - lastVisitedNodeDistance) / lastNode.distance(nextNode);
      int nextPositionX = (int)((nextNode.getX() - lastNode.getX()) * percentage) + lastNode.getX();
      int nextPositionY = (int)((nextNode.getY() - lastNode.getY()) * percentage) + lastNode.getY();
      System.out.println("X: " + nextPositionX);
      System.out.println("Y: " + nextPositionY);
      System.out.println("Percentage: " + percentage);

      this.setCenterX(nextPositionX);
      this.setCenterY(nextPositionY);
      this.traveledLen += distanceToTravel;
      //playAnimation(nextPositionX, nextPositionY, duration);
      return true;
    }
    //  2) the next position will be between the next two points (including next point)
    else {
      if (lastVisitedNodeIndex + 2 != this.path.size()){
        System.out.println("Jumping over node");
        //substract remaining distance to the next node
        distanceToTravel -= lastNode.distance(nextNode) - (this.traveledLen - lastVisitedNodeDistance);
        this.traveledLen += lastNode.distance(nextNode) - (this.traveledLen - lastVisitedNodeDistance);
        System.out.println("Distance to travel: " + distanceToTravel);

        lastVisitedNodeDistance = this.traveledLen;
        lastNode = nextNode;
        nextNode = nodes.get(this.path.get(lastVisitedNodeIndex+2).getKey());

        float percentage = (float)(this.traveledLen + distanceToTravel - lastVisitedNodeDistance) / lastNode.distance(nextNode);
        int nextPositionX = (int)((nextNode.getX() - lastNode.getX()) * percentage) + lastNode.getX();
        int nextPositionY = (int)((nextNode.getY() - lastNode.getY()) * percentage) + lastNode.getY();

        System.out.println("Shifted last node: " + lastNode);
        System.out.println("Shifted next node: " + nextNode);
        System.out.println("X: " + nextPositionX);
        System.out.println("Y: " + nextPositionY);
        System.out.println("Percentage: " + percentage);


        this.setCenterX(nextPositionX);
        this.setCenterY(nextPositionY);
        this.traveledLen += distanceToTravel;
        //playAnimation(nextPositionX, nextPositionY, duration);
        return true;
      }
      //  3) there is no next position (next node is also end point)
      else {
        //playAnimation(nextNode.getX(), nextNode.getY(), duration);
        return false;
      }
    }
  }


  private void playAnimation(int x, int y, int duration) {
    if (this.animation == null){
      this.animation = new TranslateTransition();
      this.animation.setNode(this);
    }
    this.animation.setDuration(Duration.millis(duration*100));
    //make realtive position from aboslute, since animation cant handle absolute...
    this.animation.setToX(x - this.getCenterX());
    this.animation.setToY(y - this.getCenterY());
    this.animation.play();
  }


  // return the last visited node for the traveled len
  // and its distance from the beginning
  private Pair<Integer, Integer> getLastVisitedNodeIndexAndLen(Hashtable<Integer, NodeCircle> nodes){
    int l = 0, startNodeID, nextNodeID, lPrev = 0;

    for (int i = 1; i < this.path.size(); i++){
      startNodeID = this.path.get(i-1).getKey();
      nextNodeID = this.path.get(i).getKey();

      l += nodes.get(startNodeID).distance(nodes.get(nextNodeID));
      if (traveledLen < l)
        return new Pair<>(i-1, lPrev);
      lPrev = l;
    }

    return new Pair<>(-1, this.pathLen);
  }
}
