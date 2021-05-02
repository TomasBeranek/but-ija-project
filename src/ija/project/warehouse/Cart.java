package ija.project.warehouse;

import javafx.scene.shape.Circle;
import java.util.*;
import javafx.scene.paint.Color;
import javafx.util.*;
import javafx.animation.TranslateTransition;
import javafx.scene.*;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;


/** Represents a cart which picks up the goods. The class is inherited from
 *  the Circle class in JavaFX.
 *
 * @author Tomas Beranek (xberan46)
 */
public class Cart extends Circle {
  private Long lastEpochTime = 0L;
  private int capacity;
  public int currCapacity = 0;
  public List<Pair<Integer, Pair<String, Integer>>> path = null;
  public List<Pair<Integer, Pair<String, Integer>>> pathCopy = null;
  private Long waitUntilTime = -1L;
  private int pathLen = 0;
  private int traveledLen = 0;
  private int speed = 50;     //pixels/s
  private TranslateTransition animation = null;
  private int pickUpTime = 4000; //ms
  private Hashtable<Integer, ShelfRectangle> shelves;
  private int highlightedShelfID = -1;
  private int lastVisitedNodeIndex = 0;
  private Hashtable<Integer, NodeCircle> nodes;
  private boolean cartIsActive = false;
  private Long startEpochTime = 0L;
  private ListView<String> cartList = null;
  private ArrayList<Pair<String, Integer>> pickedUpGoods = new ArrayList<>();
  private int lastVisitedNodeID;
  private boolean newPathRecentlyAdded = true;

  /**
   * @param x The cart's x coordinate.
   * @param y The cart's y coordinate.
   * @param r The cart's radius for a visualization.
   */
  public Cart(float x, float y, float r, Hashtable<Integer, ShelfRectangle> shelves, Long startEpochTime, ListView<String> cartList, int capacity){
    super(x, y, r);
    //this.setCache(true);
    //this.setCacheHint(CacheHint.SPEED);
    this.shelves = shelves;
    this.startEpochTime = startEpochTime;
    this.cartList = cartList;
    this.capacity = capacity;
  }


  public List<Pair<Integer, Pair<String, Integer>>> getRemainingPath(){
    if (this.path == null){
      return this.pathCopy.subList(lastVisitedNodeIndex, this.pathCopy.size());
      //Pair<String, Integer> goods = new Pair<>("", 0);
      //ArrayList<Pair<Integer, Pair<String, Integer>>> arr = new ArrayList<Pair<Integer, Pair<String, Integer>>>();
      //arr.add(new Pair<>(this.lastVisitedNodeID, goods));
      //return arr;
    }
    else
      return this.path.subList(lastVisitedNodeIndex, this.path.size());
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
    if (path == null && this.path != null){
      this.pathCopy = this.path;
      System.out.println(this.pathCopy);
    }

    this.path = path;
    this.pathLen = getPathLen(nodes);
    this.nodes = nodes;
    this.lastVisitedNodeIndex = 0;
    this.traveledLen = 0;
    this.newPathRecentlyAdded = true;

    // handler cannot reach 'this' variable
    Cart thisCopy = this;

    EventHandler<MouseEvent> cartClickHandler = new EventHandler<MouseEvent>() {
       @Override
       public void handle(MouseEvent e) {

          // set color of all the highlighed nodes to their default color
          for(Integer nodeID : thisCopy.nodes.keySet()){
            if (thisCopy.nodes.get(nodeID).getFill().equals(Color.GREEN)){
              thisCopy.nodes.get(nodeID).setFill(Color.RED);
              thisCopy.nodes.get(nodeID).setRadius(5);
            }
          }

          ObservableList<String> cartLitems = FXCollections.observableArrayList();

          for(int i = 0; i < pickedUpGoods.size(); i++){
            cartLitems.add(pickedUpGoods.get(i).getValue() + "x\t " + pickedUpGoods.get(i).getKey());
          }

          cartList.setItems(cartLitems);


          // highlight the current path
          if (thisCopy.path != null){
            for (int i = 0; i < thisCopy.path.size(); i++) {
              int nodeID = thisCopy.path.get(i).getKey();

              if (thisCopy.nodes.get(nodeID).getFill().equals(Color.RED)){
                thisCopy.nodes.get(nodeID).setFill(Color.GREEN);
                thisCopy.nodes.get(nodeID).setRadius(7);
              }
            }
          }
       }
    };
    this.addEventFilter(MouseEvent.MOUSE_CLICKED, cartClickHandler);
  }


  /** Updates cart's position to current the given time.
   *
   * @param currentEpochTime The current epoch time.
   * @param nodes The list of all the nodes.
   */
  public String updatePosition(Long currentEpochTime, Hashtable<Integer, NodeCircle> nodes){
    if (this.lastEpochTime == 0){
      this.lastEpochTime = currentEpochTime; //this the first update called
      this.traveledLen = (int)(((currentEpochTime - this.startEpochTime)/1000.0)*this.speed);
    }

    if (this.newPathRecentlyAdded){
      this.lastEpochTime = currentEpochTime;
      this.traveledLen = 0;
      this.newPathRecentlyAdded = false;
    }

    //if there is not defined path, stay on the spot and ask for recalcucation
    if (this.path == null)
      return "Stopped";

    // check if path finder returned path from 0 to 0, if so end it
    if (this.path.size() >= (this.lastVisitedNodeIndex + 2) && this.path.get(lastVisitedNodeIndex).getKey() == 0 && this.path.get(lastVisitedNodeIndex+1).getKey() == 0){
      return "Finished";
    }

    int nodeID = this.path.get(0).getKey();

    this.setRadius(10); //make sure it is visible
    this.setFill(Color.GREY);

    //index in path not node ID!
    Pair<Integer, Integer> p = getLastVisitedNodeIndexAndLen(nodes);
    this.lastVisitedNodeIndex = p.getKey();
    int lastVisitedNodeDistance = p.getValue();
    NodeCircle lastNode = nodes.get(this.path.get(lastVisitedNodeIndex).getKey());
    NodeCircle nextNode = nodes.get(this.path.get(lastVisitedNodeIndex+1).getKey());
    Long duration = currentEpochTime - this.lastEpochTime;
    int distanceToTravel = (int)((duration/1000.0)*this.speed);
    this.lastEpochTime = currentEpochTime;
    this.lastVisitedNodeID = lastNode.ID;

    // 3 things can happen:
    //  1) the next position will be still between the same two points
    if (this.traveledLen + distanceToTravel < lastVisitedNodeDistance + lastNode.distance(nextNode)){
      if (currentEpochTime < this.waitUntilTime)
        return "Success"; //if we are picking up goods, stay on the spot

      // check if next node is reachable
      if (!lastNode.getNeighbours().contains(nextNode.ID)){
        // next node is not reachable
        return "Stopped";
      }

      if (this.highlightedShelfID != -1){
        this.shelves.get(highlightedShelfID).setStrokeWidth(4);
        this.shelves.get(highlightedShelfID).setStroke(Color.BLACK);
        this.highlightedShelfID = -1;
      }


      float percentage = (float)(this.traveledLen + distanceToTravel - lastVisitedNodeDistance) / lastNode.distance(nextNode);
      int nextPositionX = (int)((nextNode.getX() - lastNode.getX()) * percentage) + lastNode.getX();
      int nextPositionY = (int)((nextNode.getY() - lastNode.getY()) * percentage) + lastNode.getY();

      this.setCenterX(nextPositionX);
      this.setCenterY(nextPositionY);
      this.traveledLen += distanceToTravel;
      //playAnimation(nextPositionX, nextPositionY, duration);
      return "Success";
    }
    //  2) the next position will be between the next two points (including next point)
    else {
      if (lastVisitedNodeIndex + 2 != this.path.size()){
        if (currentEpochTime < this.waitUntilTime)
          return "Success"; //if we are picking up goods, stay on the spot

        if (this.highlightedShelfID != -1){
          this.shelves.get(highlightedShelfID).setStrokeWidth(4);
          this.shelves.get(highlightedShelfID).setStroke(Color.BLACK);
          this.highlightedShelfID = -1;
        }

        //substract remaining distance to the next node
        distanceToTravel -= lastNode.distance(nextNode) - (this.traveledLen - lastVisitedNodeDistance);
        this.traveledLen += lastNode.distance(nextNode) - (this.traveledLen - lastVisitedNodeDistance);

        lastVisitedNodeIndex++;
        lastVisitedNodeDistance = this.traveledLen;
        lastNode = nextNode;
        nextNode = nodes.get(this.path.get(lastVisitedNodeIndex+1).getKey());

        float percentage = (float)(this.traveledLen + distanceToTravel - lastVisitedNodeDistance) / lastNode.distance(nextNode);
        int nextPositionX = (int)((nextNode.getX() - lastNode.getX()) * percentage) + lastNode.getX();
        int nextPositionY = (int)((nextNode.getY() - lastNode.getY()) * percentage) + lastNode.getY();

        // check if on the last visisited node is goods that we have to pick up
        if (this.path.get(lastVisitedNodeIndex).getValue().getKey() != ""){
          //wait
          this.waitUntilTime = currentEpochTime + pickUpTime;

          int quantity = this.path.get(lastVisitedNodeIndex).getValue().getValue();
          String goodsName = this.path.get(lastVisitedNodeIndex).getValue().getKey();
          int lastNodeID = this.path.get(lastVisitedNodeIndex).getKey();

          //dispense all the goods
          if (goodsName.equals("dispense")){
            this.pickedUpGoods.clear();
            this.currCapacity = 0;
          } else {
            //find shelf by nodeID and goods name and pick up goods
            for(Integer shelfID : this.shelves.keySet()) {
              if (this.shelves.get(shelfID).getGoods().equals(goodsName)
                  && this.shelves.get(shelfID).getNode() == lastNodeID){
                    //pick the goods form the shelf
                    this.pickedUpGoods.add(new Pair<>(goodsName, quantity));
                    this.currCapacity += quantity;
                    this.shelves.get(shelfID).decreaseQuantity(quantity);

                    //save previous color
                    this.shelves.get(shelfID).toFront();
                    this.shelves.get(shelfID).setStrokeWidth(6);
                    this.shelves.get(shelfID).setStroke(Color.GREY);
                    this.highlightedShelfID = shelfID;
                  }
            }
          }
        }

        this.setCenterX(nextPositionX);
        this.setCenterY(nextPositionY);
        this.traveledLen += distanceToTravel;
        //playAnimation(nextPositionX, nextPositionY, duration);
        return "Success";
      }
      //  3) there is no next position (next node is also end point)
      else {
        this.setRadius(0);
        //playAnimation(nextNode.getX(), nextNode.getY(), duration);
        return "Finished";
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
  // OPTIMIZE: delete this method? and save last visisted node in class variable instead?
  private Pair<Integer, Integer> getLastVisitedNodeIndexAndLen(Hashtable<Integer, NodeCircle> nodes){
    int l = 0, startNodeID, nextNodeID, lPrev = 0;

    for (int i = 1; i < this.path.size(); i++){
      startNodeID = this.path.get(i-1).getKey();
      nextNodeID = this.path.get(i).getKey();

      l += nodes.get(startNodeID).distance(nodes.get(nextNodeID));
      if (traveledLen < l){
        this.lastVisitedNodeIndex = i-1;
        return new Pair<>(i-1, lPrev);
      }
      lPrev = l;
    }

    this.lastVisitedNodeIndex = -1;
    return new Pair<>(-1, this.pathLen);
  }
}
