package ija.project.warehouse;

import java.util.*;
import javafx.util.*;


public class PathFinder {
  // a matrix of nodes distances


  public PathFinder(Hashtable<Integer, NodeCircle> nodes) {
    // creates a matrix of nodes distances
  }


  public List<Pair<Integer, Pair<String, Integer>>> findPath(
          Order order,
          Hashtable<Integer, ShelfRectangle> shelfs) {
      // calculates the optimal path for a cart

      // DELETE: just a stump for WarehouseSimulation class development
      List<Pair<Integer, Pair<String, Integer>>> path = new ArrayList<>();
      Pair<String, Integer> doNotPickUp = new Pair<>("", 0);
      Pair<String, Integer> goodsToPickUp = new Pair<>("Šroub inbus půlkulatý s límcem M24 nerez A2", 50);

      for (int i = 0; i <= 22 ; i++) {
        path.add(new Pair<Integer, Pair<String, Integer>>(i, doNotPickUp));
      }
      path.add(new Pair<Integer, Pair<String, Integer>>(45, doNotPickUp));
      path.add(new Pair<Integer, Pair<String, Integer>>(68, doNotPickUp));
      path.add(new Pair<Integer, Pair<String, Integer>>(91, doNotPickUp));
      path.add(new Pair<Integer, Pair<String, Integer>>(114, doNotPickUp));
      // pick up goods
      path.add(new Pair<Integer, Pair<String, Integer>>(113, goodsToPickUp));

      for (int i = 112; i >= 92; i--) {
        path.add(new Pair<Integer, Pair<String, Integer>>(i, doNotPickUp));
      }

      path.add(new Pair<Integer, Pair<String, Integer>>(69, doNotPickUp));
      path.add(new Pair<Integer, Pair<String, Integer>>(46, doNotPickUp));
      path.add(new Pair<Integer, Pair<String, Integer>>(23, doNotPickUp));
      // ends in dispensing point
      path.add(new Pair<Integer, Pair<String, Integer>>(0, doNotPickUp));

      return path;
  }


  // may not be needed
  public void updateDistanceMatrix(Hashtable<Integer, NodeCircle> nodes) {
    // modifies a matrix of nodes distances
  }
}
