package ija.project.warehouse;

import java.util.*;
import javafx.util.*;
import java.lang.Math;
import javafx.geometry.Point2D;
import java.util.ArrayList;
import javafx.util.Pair;
import java.util.Iterator;
import java.util.Collections;
import java.util.HashSet;

/** Represent navigator for carts holding informations about nearest ways
 *  between nodes in matrixes
 *
 * @author Simon Slobodnik (xslobo06)
 */
public class PathFinder {
  private int[][] distance; //matrix with distances between nodes
  private int[][] next; //matrix with best way between nodes

  /**
   * @param nodes Hashrable of nodes ID and NodeCircle
   */
  public PathFinder(Hashtable<Integer, NodeCircle> nodes) {
    int number_nodes = nodes.size();
    // creates a distance of nodes distances
    this.distance = new int[number_nodes][number_nodes];
    this.next = new int[number_nodes][number_nodes];
    setMatrix(nodes);
  }

  /** Set default matrix for Floyd-Warhall processing
    */
  private void defaultMatrix(){
    for (int i = 0; i < this.distance.length; i++) {
      for (int j = 0; j < this.distance[i].length; j++) {
        if (i == j){
          this.distance[i][j] = 0;
          this.next[i][j] = i;
        }
        else{
          this.distance[i][j] = Integer.MAX_VALUE;
          this.next[i][j] = -1;
        }
      }
    }
  }

  /** Calculate minimal distances between nodes by Flayd-Warshall method
    */
  private void floydWarshall() {
    for (int k = 0; k < this.distance.length; k++) {
      for (int i = 0; i < this.distance.length; i++) {
        if (this.distance[i][k] >= Integer.MAX_VALUE)
          continue;
        for (int j = 0; j < this.distance.length; j++) {
          if (this.distance[k][j] >= Integer.MAX_VALUE)
            continue;
          if (this.distance[i][j] > this.distance[i][k] + this.distance[k][j]){
            this.distance[i][j] = this.distance[i][k] + this.distance[k][j];
            next[i][j] = next[i][k];
          }
        }
      }
    }
  }

  /** Set connection between actual node and his neighbours (recursive)
    *
    * @param nodes Hashtable with nodes and
    * @param number number of actual node
    */
  private void createAllEdges(Hashtable<Integer, NodeCircle> nodes, int number){

    Point2D firstNode = new Point2D(nodes.get(number).getX(),nodes.get(number).getY());
    List<Integer> neighbours = nodes.get(number).getNeighbours(); //get id of neighbours

    for(int i = 0; i < neighbours.size();i++){
      if(this.distance[number][neighbours.get(i)] == Integer.MAX_VALUE){  //if distance between nodes is not set
        //get neighbour position
        Point2D secondNode = new Point2D(nodes.get(neighbours.get(i)).getX(), nodes.get(neighbours.get(i)).getY());
        //set edge
        createEdge(number, neighbours.get(i), (int)firstNode.distance(secondNode));
        createAllEdges(nodes, neighbours.get(i));
      }
    }
  }

  /** Insert new edge to matrix distance and next
   *
   * @param u Number of first node
   * @param v Number of second node
   * @param distance Distance between given nodes
   */
  private void createEdge(int u, int v, int distance){
    //all edges are double-sided
    this.distance[u][v] = distance;
    this.distance[v][u] = distance;
    this.next[u][v] = v;
    this.next[v][u] = u;
  }
  /** Search for nearest node
   *
   * @param requiredNodes Selected nodes containing the searched goods
   * @param actualNode Number of node
   * @return Number of node with the smallest distance between him and actual node
   */
  private Integer nearestNode(Hashtable<Integer, Integer> requiredNodes, int actualNode){

    int min = Integer.MAX_VALUE;
    int node = -1;

    Set<Integer> nodeIDs = requiredNodes.keySet();
    for(int nodeID: nodeIDs){
      if(this.distance[actualNode][nodeID] > 0 && this.distance[actualNode][nodeID] < min){
        min = this.distance[actualNode][nodeID];
        node = nodeID;
      }
    }

    return node;
  }

  /** Find all nodes which are next to shelf with wanted goods and they process them into a sorted form
   *  ideal for picking up goods, without a path between the found nodes
   *
   *  @param order List of orders with pairs of goods name and their number
   *  @param shelfs Hashtable of shelfs ID and ShelfRectangles
   *  @param actualNode The node number where the cart is currently located
   *  @return Number of nodes with required goods and good name and quantity in sub-optimal order
   */
  private List<Pair<Integer,Pair<String, Integer>>> orderProcessing(List<Pair<String, Integer>> order,
                              Hashtable<Integer, ShelfRectangle> shelfs, int actualNode) {

    Hashtable<Integer,Integer> requiredNodes = new Hashtable<Integer, Integer>();
    Set<Integer> Keys = shelfs.keySet();
    for(int i: Keys){
      for(int j = 0; j < order.size(); j++){

        if(shelfs.get(i).getGoods().equals(order.get(j).getKey())){
          requiredNodes.put(shelfs.get(i).nodeID,i);
          break;
        }
      }
    }

    List<Pair<Integer,Pair<String, Integer>>> path = new ArrayList<Pair<Integer,Pair<String, Integer>>>();
    Pair<String, Integer> pickUp;
    int nearest = -1;
    String goodName = "";
    while(requiredNodes.size() != 0){
      nearest = nearestNode(requiredNodes, actualNode);
      goodName = shelfs.get(requiredNodes.get(nearest)).getGoods();
      for(int j = 0; j < order.size(); j++){
        if(order.get(j).getValue() == 0)  //the order has already been processed
          continue;
        if(order.get(j).getKey().equals(goodName)){ //shelf has required good
          if(order.get(j).getValue() <= shelfs.get(requiredNodes.get(nearest)).getQuantity()){  //shelf has enought goods
            pickUp = new Pair<String, Integer>(order.get(j).getKey(), order.get(j).getValue());
            path.add(new Pair<Integer, Pair<String, Integer>>(nearest, pickUp));
            order.set(j, new Pair<String, Integer>(goodName,0));
          }
          else{ //shelf hasn't enought goods
            pickUp = new Pair<String, Integer>(order.get(j).getKey(), shelfs.get(requiredNodes.get(nearest)).getQuantity());
            path.add(new Pair<Integer, Pair<String, Integer>>(nearest, pickUp));
            order.set(j, new Pair<String, Integer>(goodName,order.get(j).getValue() - shelfs.get(requiredNodes.get(nearest)).getQuantity()));
          }
        }
      }
      requiredNodes.remove(nearest);
    }

    return path;
  }

  /** Get path between two given nodes, without first and last node
   *
   *  @param u Number of start node
   *  @param v Number of target node
   *  @return Path between start and target nodes
   */
  private List<Pair<Integer, Pair<String, Integer>>> constructPath(int u, int v){

    if (this.next[u][v] == -1)  //no path between nodes
        return null;

    Pair<String, Integer> doNotPickUp = new Pair<String,Integer>("", 0);
    List<Pair<Integer,Pair<String, Integer>>> path = new ArrayList<Pair<Integer,Pair<String, Integer>>>();

    while (u != v){
        u = this.next[u][v];
        if(u == v)  //final node
          break;
        path.add(new Pair<Integer, Pair<String, Integer>>(u, doNotPickUp));
    }

    return path;
  }

  /** Call all functions for setting distance matrix and next matrix
   *
   *  @param nodes Hashrable of nodes ID and NodeCircle
   */
  public void setMatrix(Hashtable<Integer, NodeCircle> nodes){
    defaultMatrix();
    createAllEdges(nodes, 0);
    floydWarshall();
    return;
  }

  /** Finding complet the shortest path for a given order with data about collection of goods
   *
   *  @param order List of orders with pairs of goods name and their number
   *  @param shelfs Hashtable of shelfs ID and ShelfRectangles
   *  @param actualNode The node number where the cart is currently located
   *  @return A list of nodes representing the path and the name and number
   *          of items to be picked up for that node
   */
  public List<Pair<Integer, Pair<String, Integer>>> findPath(
          List<Pair<String, Integer>> order,
          Hashtable<Integer, ShelfRectangle> shelfs,
          int actualNode){

    Pair<String, Integer> doNotPickUp = new Pair<>("", 0);
    List<Pair<Integer,Pair<String, Integer>>> nodes = orderProcessing(order,shelfs, actualNode);  //wanted nodes with goods
    List<Pair<Integer,Pair<String, Integer>>> nodePath = new ArrayList<>(); //path

    //add path from actual node to first wanted node
    nodePath.add(new Pair<Integer, Pair<String, Integer>>(0, doNotPickUp));
    nodePath.addAll(constructPath(actualNode,nodes.get(0).getKey()));
    nodePath.add(nodes.get(0));

    for(int i = 0; i < (nodes.size()-1);i++){
      nodePath.addAll(constructPath(nodes.get(i).getKey(),nodes.get(i+1).getKey()));
      nodePath.add(nodes.get(i+1));
    }

    nodePath.addAll(constructPath(nodes.get(nodes.size() - 1).getKey(),0)); //add path between last wanted node and starting point(default 0)
    nodePath.add(new Pair<Integer, Pair<String, Integer>>(0, doNotPickUp)); //add starting node

    return nodePath;
  }
}
