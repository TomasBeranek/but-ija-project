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

public class PathFinder {
  private int[][] distance; //matrix with distances between nodes
  private int[][] next; //matrix with best way between nodes

  public PathFinder(Hashtable<Integer, NodeCircle> nodes) {
    int number_nodes = nodes.size();
    // creates a distance of nodes distances
    this.distance = new int[number_nodes][number_nodes];
    this.next = new int[number_nodes][number_nodes];
    setMatrix(nodes);
  }

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

  //set matrix distance and next by floydWarshall method
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

  //from edges from node to neighbours
  private void createAllEdges(Hashtable<Integer, NodeCircle> nodes, int number){

    Point2D firstNode = new Point2D(nodes.get(number).getX(),nodes.get(number).getY());
    Point2D secondNode = new Point2D(0,0);
    List<Integer> neighbours = nodes.get(number).getNeighbours(); //get id of neighbours

    for(int i = 0; i < neighbours.size();i++){

      if(this.distance[number][neighbours.get(i)] == Integer.MAX_VALUE){  //if distance between nodes is not set
        //get neighbour position
        secondNode.add(nodes.get(neighbours.get(i)).getX(), nodes.get(neighbours.get(i)).getY());
        //set edge
        createEdge(number, neighbours.get(i), (int)firstNode.distance(secondNode));
        createAllEdges(nodes, neighbours.get(i));
      }
    }
  }

  //insert new edge to matrix distance and next
  private void createEdge(int u, int v, int distance){
    //all edges are double-sided
    this.distance[u][v] = distance;
    this.distance[v][u] = distance;
    this.next[u][v] = v;
    this.next[v][u] = u;
  }

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

  //return node numbers with goods in semioptimal order
  private List<Pair<Integer,Pair<String, Integer>>> orderProcessing(List<Pair<String, Integer>> order,
                              Hashtable<Integer, ShelfRectangle> shelfs, int actualNode) {
      Hashtable<Integer,Integer> requiredNodes = new Hashtable<Integer, Integer>();
      shelfs.forEach((i,s) -> {
        for(int j = 0; j < order.size(); j++){
          if(s.getGoods().equals(order.get(j).getKey())){
            requiredNodes.put(shelfs.get(i).nodeID,i);
            break;
          }
        }
      });

      List<Pair<Integer,Pair<String, Integer>>> path = new ArrayList<Pair<Integer,Pair<String, Integer>>>();
      Pair<String, Integer> pickUp;
      int nearest = -1;
      String goodName = "";
      while(requiredNodes.size() != 0){
        nearest = nearestNode(requiredNodes, actualNode);
        goodName = shelfs.get(requiredNodes.get(nearest)).getGoods();
        for(int j = 0; j < order.size(); j++){
          if(order.get(j).getValue() == 0)
            continue;
          if(order.get(j).getKey().equals(goodName)){
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

  //returns the path between two given nodes, without first and last node
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

  private List<Pair<Integer, Pair<String, Integer>>> getCompletPath(
            List<Pair<String, Integer>> order,
            Hashtable<Integer, ShelfRectangle> shelfs,
            int actualNode){

    Pair<String, Integer> doNotPickUp = new Pair<>("", 0);
    List<Pair<Integer,Pair<String, Integer>>> nodes = orderProcessing(order,shelfs, actualNode);  //wanted nodes with goods
    List<Pair<Integer,Pair<String, Integer>>> nodePath = new ArrayList<>(); //path

    //add path from actual node to first wanted node
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

  public void setMatrix(Hashtable<Integer, NodeCircle> nodes){
    defaultMatrix();
    createAllEdges(nodes, 0);
    floydWarshall();
    return;
  }

  //
  public List<Pair<Integer, Pair<String, Integer>>> findPath(
          List<Pair<String, Integer>> order,
          Hashtable<Integer, ShelfRectangle> shelfs,
          int actualNode){
      // calculates the optimal path for a cart

      return getCompletPath(order,shelfs,actualNode);
  }
}
