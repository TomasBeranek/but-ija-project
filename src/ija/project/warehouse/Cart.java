package ija.project.warehouse;

import java.util.*;

import javafx.util.*;
import javafx.scene.*;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import javafx.scene.input.MouseEvent;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.animation.TranslateTransition;


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
  private ArrayList<Integer> highlightedShelfID = new ArrayList<>();
  private int lastVisitedNodeIndex = 0;
  private Hashtable<Integer, NodeCircle> nodes;
  private boolean cartIsActive = false;
  private Long startEpochTime = 0L;
  private ListView<String> cartList = null;
  private ArrayList<Pair<String, Integer>> pickedUpGoods = new ArrayList<>();
  private int lastVisitedNodeID;
  private boolean newPathRecentlyAdded = true;
  private int lastVisitedNodeIndexCopy = 0;


  /**
   * @param x The cart's x coordinate.
   * @param y The cart's y coordinate.
   * @param r The cart's radius for a visualization
   * @param shelves All the shelfs in the warehouse.
   * @param startEpochTime The time at which cart should start..
   * @param cartList The list into currently loaded goods is loaded upon clicking
   *                  on the cart.
   * @param capacity The cart's capacity.
   */
  public Cart(float x, float y, float r, Hashtable<Integer, ShelfRectangle> shelves, Long startEpochTime, ListView<String> cartList, int capacity){
    super(x, y, r);
    this.shelves = shelves;
    this.startEpochTime = startEpochTime;
    this.cartList = cartList;
    this.capacity = capacity;
  }


  /** Calculates the remaing path with the last visited node included.
   *
   * @return Remaining path with the last visited node included.
   */
  public List<Pair<Integer, Pair<String, Integer>>> getRemainingPath(){
    if (this.path == null){
      return this.pathCopy.subList(this.lastVisitedNodeIndexCopy, this.pathCopy.size());
    }
    else
      return this.path.subList(this.lastVisitedNodeIndex, this.path.size());
  }


  /** Calculates the lenght of the whole path.
   *
   * @param nodes The list of all the nodes.
   * @return A length of a whole path.
   */
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
      this.lastVisitedNodeIndexCopy = this.lastVisitedNodeIndex;
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

          for(Integer shelfID : thisCopy.shelves.keySet()) {
            if (thisCopy.shelves.get(shelfID).getStroke().equals(Color.GREEN)){
              thisCopy.shelves.get(shelfID).setStrokeWidth(4);
              thisCopy.shelves.get(shelfID).setStroke(Color.BLACK);
            }
          }

          //show shelfs
          //find shelf by nodeID and goods name and pick up goods
          for (int i = 0; i < thisCopy.path.size(); i++) {
            int nodeID = thisCopy.path.get(i).getKey();
            String goods = thisCopy.path.get(i).getValue().getKey();
            int quantity = thisCopy.path.get(i).getValue().getValue();

            if (goods.equals(""))
              continue;

            for(Integer shelfID : thisCopy.shelves.keySet()) {
              if (thisCopy.shelves.get(shelfID).getGoods().equals(goods)
                  && thisCopy.shelves.get(shelfID).getNode() == nodeID){

                    int shelfQuantity = thisCopy.shelves.get(shelfID).getQuantity();

                    //check if the there is enough goods in a shelf
                    if (quantity > shelfQuantity){
                      quantity -= shelfQuantity;
                    } else {
                      // if not, then search other shelf which are connected to the same node and ahve the same goods
                      quantity = 0;
                    }

                    //save previous color
                    thisCopy.shelves.get(shelfID).setStrokeWidth(6);
                    thisCopy.shelves.get(shelfID).setStroke(Color.GREEN);

                    // we have taken everything
                    if (quantity == 0)
                      break;
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
   * @return An information about updating:
   *          "Stopped" -- the cart cannot continue in the given path
   *          "Success" -- the cart successfully updated it's position
   *          "Finished" -- the cart reached the last node in path
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

      for (int i = 0; i < this.highlightedShelfID.size(); i++) {
        if (this.shelves.get(this.highlightedShelfID.get(i)).getStroke().equals(Color.GREY)){
          this.shelves.get(this.highlightedShelfID.get(i)).setStrokeWidth(4);
          this.shelves.get(this.highlightedShelfID.get(i)).setStroke(Color.BLACK);
        } else if (this.shelves.get(this.highlightedShelfID.get(i)).getStroke().equals(Color.DARKGREY)){
          this.shelves.get(this.highlightedShelfID.get(i)).setStrokeWidth(6);
          this.shelves.get(this.highlightedShelfID.get(i)).setStroke(Color.GREEN);
        }
      }
      this.highlightedShelfID.clear();


      float percentage = (float)(this.traveledLen + distanceToTravel - lastVisitedNodeDistance) / lastNode.distance(nextNode);
      int nextPositionX = (int)((nextNode.getX() - lastNode.getX()) * percentage) + lastNode.getX();
      int nextPositionY = (int)((nextNode.getY() - lastNode.getY()) * percentage) + lastNode.getY();

      this.setCenterX(nextPositionX);
      this.setCenterY(nextPositionY);
      this.traveledLen += distanceToTravel;
      return "Success";
    }
    //  2) the next position will be between the next two points (including next point)
    else {
      if (lastVisitedNodeIndex + 2 != this.path.size()){
        if (currentEpochTime < this.waitUntilTime)
          return "Success"; //if we are picking up goods, stay on the spot

          for (int i = 0; i < this.highlightedShelfID.size(); i++) {
            if (this.shelves.get(this.highlightedShelfID.get(i)).getStroke().equals(Color.GREY)){
              this.shelves.get(this.highlightedShelfID.get(i)).setStrokeWidth(4);
              this.shelves.get(this.highlightedShelfID.get(i)).setStroke(Color.BLACK);
            } else if (this.shelves.get(this.highlightedShelfID.get(i)).getStroke().equals(Color.DARKGREY)){
              this.shelves.get(this.highlightedShelfID.get(i)).setStrokeWidth(6);
              this.shelves.get(this.highlightedShelfID.get(i)).setStroke(Color.GREEN);
            }
          }
          this.highlightedShelfID.clear();

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
          this.waitUntilTime = currentEpochTime;

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
                    this.waitUntilTime += pickUpTime;

                    int shelfQuantity = this.shelves.get(shelfID).getQuantity();

                    //check if the there is enough goods in a shelf
                    if (quantity > shelfQuantity){
                      this.shelves.get(shelfID).decreaseQuantity(shelfQuantity);
                      quantity -= shelfQuantity;
                    } else {
                      // if not, then search other shelf which are connected to the same node and ahve the same goods
                      this.shelves.get(shelfID).decreaseQuantity(quantity);
                      quantity = 0;
                    }

                    //save previous color
                    if (this.shelves.get(shelfID).getStroke().equals(Color.GREEN)){
                      this.shelves.get(shelfID).setStrokeWidth(6);
                      this.shelves.get(shelfID).setStroke(Color.DARKGREY);
                      this.highlightedShelfID.add(shelfID);
                    } else {
                      this.shelves.get(shelfID).setStrokeWidth(6);
                      this.shelves.get(shelfID).setStroke(Color.GREY);
                      this.highlightedShelfID.add(shelfID);
                    }

                    // we have taken everything
                    if (quantity == 0)
                      break;
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


  // OPTIMIZE: delete this method? and save last visisted node in class variable instead?
  /** Calculates the last visited node's index in path and it's distance from the
   *  start.
   *
   * @param nodes The list of all the nodes.
   * @return A pair of values (index in path, distance from start).
   */
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
