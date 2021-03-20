/**
* This file contains an implementation of WarehouseSimulation class with the
* main method. This class creates a GUI and controls the simulation.
*
* @author  Tomas Beranek (xberan46)
* @author  Simon Slobodnik (xslobo06)
* @since   19.3.2021
*/

package ija.project.warehouse;

import ija.project.warehouse.ShelfRectangle;
import ija.project.warehouse.NodeCircle;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.*;
import java.util.*;
import javafx.geometry.Point2D;
import java.lang.Math;
import javafx.scene.shape.*;
import javafx.scene.paint.Color;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.*;

//http://www.java2s.com/Code/JarDownload/json-simple/json-simple-1.1.jar.zip
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;


public class WarehouseSimulation extends Application {
   private ShelfRectangle highLightedShelf = null;
   private Text highLightedShelfID;
   private NodeCircle highLightedNode = null;
   private Text highLightedNodeID;
   private Hashtable<Integer, NodeCircle> nodes = new Hashtable<>();
   private Hashtable<Integer, ShelfRectangle> shelfs = new Hashtable<>();

   private List<JSONObject> loadJSONData(List<String> fileNames) {
     JSONParser parser = new JSONParser();
     JSONObject warehouseData = new JSONObject();
     JSONObject goodsData;
     JSONObject ordersData;
     List<JSONObject> data = new ArrayList<>();

     try {
       warehouseData = (JSONObject) parser.parse(new FileReader(fileNames.get(0)));
       data.add(warehouseData);
     } catch(Exception e) {
       System.err.printf("ERROR: failed to parse JSON file '%s'", fileNames.get(0));
       System.exit(1);
     }

     try {
       goodsData = (JSONObject) parser.parse(new FileReader(fileNames.get(1)));
       data.add(goodsData);
     } catch(Exception e) {
       System.err.printf("ERROR: failed to parse JSON file '%s'", fileNames.get(1));
       //System.exit(1);
     }

     try {
       ordersData = (JSONObject) parser.parse(new FileReader(fileNames.get(2)));
       data.add(ordersData);
     } catch(Exception e) {
       System.err.printf("ERROR: failed to parse JSON file '%s'", fileNames.get(2));
       //System.exit(1);
     }

     return data;
   }


   private Pair<Point2D, Point2D> getWarehouseCords(JSONObject data) {
      JSONArray lefttopJSON = (JSONArray)((JSONObject)data.get("warehouse")).get("lefttop");
      Point2D lefttop = new Point2D(((Long)lefttopJSON.get(0)).doubleValue(), ((Long)lefttopJSON.get(1)).doubleValue());

      JSONArray rightbottomJSON = (JSONArray)((JSONObject)data.get("warehouse")).get("rightbottom");
      Point2D rightbottom = new Point2D(((Long)rightbottomJSON.get(0)).doubleValue(), ((Long)rightbottomJSON.get(1)).doubleValue());

      return new Pair<>(lefttop, rightbottom);
   }


   private Pair<Point2D, Point2D> getDispensingPointCords(JSONObject data) {
      JSONArray lefttopJSON = (JSONArray)((JSONObject)data.get("dispensingPoint")).get("lefttop");
      Point2D lefttop = new Point2D(((Long)lefttopJSON.get(0)).doubleValue(), ((Long)lefttopJSON.get(1)).doubleValue());

      JSONArray rightbottomJSON = (JSONArray)((JSONObject)data.get("dispensingPoint")).get("rightbottom");
      Point2D rightbottom = new Point2D(((Long)rightbottomJSON.get(0)).doubleValue(), ((Long)rightbottomJSON.get(1)).doubleValue());

      return new Pair<>(lefttop, rightbottom);
   }


   private List<Pair<Pair<Point2D, Point2D>, Integer>> getAllShelfsCords(JSONObject data) {
     List<Pair<Pair<Point2D, Point2D>, Integer>> shelfCords = new ArrayList<>();
     JSONArray shelfsJSON = (JSONArray)data.get("shelfs");
     Iterator it = shelfsJSON.iterator();

     while(it.hasNext()){
        shelfCords.add(getShelfCords((JSONObject)it.next()));
     }

     return shelfCords;
   }


   private Pair<Pair<Point2D, Point2D>, Integer> getShelfCords(JSONObject data) {
      JSONArray lefttopJSON = (JSONArray)data.get("lefttop");
      Point2D lefttop = new Point2D(((Long)lefttopJSON.get(0)).doubleValue(), ((Long)lefttopJSON.get(1)).doubleValue());

      JSONArray rightbottomJSON = (JSONArray)data.get("rightbottom");
      Point2D rightbottom = new Point2D(((Long)rightbottomJSON.get(0)).doubleValue(), ((Long)rightbottomJSON.get(1)).doubleValue());

      return new Pair<>(new Pair<>(lefttop, rightbottom), ((Long)data.get("id")).intValue());
   }


   private void displayShelfs(Group group, List<Pair<Pair<Point2D, Point2D>, Integer>> shelfsCords) {
     Iterator<Pair<Pair<Point2D, Point2D>, Integer>> it = shelfsCords.iterator();

     while(it.hasNext()){
       Pair<Pair<Point2D, Point2D>, Integer> shelfInfo = (Pair<Pair<Point2D, Point2D>, Integer>)it.next();
       Pair<Point2D, Point2D> shelfCords = shelfInfo.getKey();
       ShelfRectangle shelfRec = new ShelfRectangle(
         (int)Math.round(shelfCords.getKey().getX()),
         (int)Math.round(shelfCords.getKey().getY()),
         (int)Math.round(shelfCords.getValue().getX()) - (int)Math.round(shelfCords.getKey().getX()),
         (int)Math.round(shelfCords.getValue().getY()) - (int)Math.round(shelfCords.getKey().getY()),
         shelfInfo.getValue());
       shelfRec.setFill(Color.BLUE);
       shelfRec.setStrokeWidth(4);
       shelfRec.setStroke(Color.BLACK);

       //Creating the mouse event handler
       EventHandler<MouseEvent> shelfClickHandler = new EventHandler<MouseEvent>() {
          @Override
          public void handle(MouseEvent e) {
             if (highLightedShelf != null)
               highLightedShelf.setFill(Color.BLUE);

             shelfRec.setFill(Color.RED);
             highLightedShelf = (ShelfRectangle)shelfRec;
             highLightedShelfID.setText("Selected shelf: " + shelfRec.shelfID);
          }
       };

       shelfRec.addEventFilter(MouseEvent.MOUSE_CLICKED, shelfClickHandler);
       group.getChildren().add(shelfRec);
       shelfs.put(shelfRec.shelfID, shelfRec);
     }

     // add a text which shows ID of the selected shelf
     highLightedShelfID = new Text("");
     highLightedShelfID.setX(10);
     highLightedShelfID.setY(20);
     group.getChildren().add(highLightedShelfID);
   }


   private List<Pair<Point2D, Integer>> getAllNodesCords(JSONObject data) {
     List<Pair<Point2D, Integer>> nodesCords = new ArrayList<>();
     JSONArray nodesJSON = (JSONArray)data.get("nodes");
     Iterator it = nodesJSON.iterator();

     while(it.hasNext()){
        nodesCords.add(getNodeCords((JSONObject)it.next()));
     }

     return nodesCords;
   }


   private Pair<Point2D, Integer> getNodeCords(JSONObject data) {
     JSONArray positionJSON = (JSONArray)data.get("position");
     Point2D position = new Point2D(((Long)positionJSON.get(0)).doubleValue(), ((Long)positionJSON.get(1)).doubleValue());

     return new Pair<>(position, ((Long)data.get("id")).intValue());
   }


   private void displayNodes(Group group, List<Pair<Point2D, Integer>> nodesCords){
     Iterator<Pair<Point2D, Integer>> it = nodesCords.iterator();

     while(it.hasNext()){
       Pair<Point2D, Integer> nodeInfo = (Pair<Point2D, Integer>)it.next();
       NodeCircle nodeCircle = new NodeCircle(
         (float)Math.round(nodeInfo.getKey().getX()),
         (float)Math.round(nodeInfo.getKey().getY()),
         5.0f,
         nodeInfo.getValue());
       nodeCircle.setFill(Color.RED);
       nodeCircle.setStrokeWidth(0);

       //Creating the mouse event handler
       EventHandler<MouseEvent> nodeClickHandler = new EventHandler<MouseEvent>() {
          @Override
          public void handle(MouseEvent e) {
             if (highLightedNode != null)
               highLightedNode.setFill(Color.RED);

             nodeCircle.setFill(Color.BLUE);
             highLightedNode = (NodeCircle)nodeCircle;
             highLightedNodeID.setText("Selected node: " + nodeCircle.ID);
          }
       };

       nodeCircle.addEventFilter(MouseEvent.MOUSE_CLICKED, nodeClickHandler);
       group.getChildren().add(nodeCircle);
       nodes.put(nodeCircle.ID, nodeCircle);
     }

     // add a text which shows ID of the selected shelf
     highLightedNodeID = new Text("");
     highLightedNodeID.setX(150);
     highLightedNodeID.setY(20);
     group.getChildren().add(highLightedNodeID);
   }


   private void displayWarehouse(Group group, Pair<Point2D, Point2D> warehouseCords) {
     Rectangle warehouseRect = new Rectangle(
       (int)Math.round(warehouseCords.getKey().getX()),
       (int)Math.round(warehouseCords.getKey().getY()),
       (int)Math.round(warehouseCords.getValue().getX()) - (int)Math.round(warehouseCords.getKey().getX()),
       (int)Math.round(warehouseCords.getValue().getY()) - (int)Math.round(warehouseCords.getKey().getY()));
     warehouseRect.setFill(Color.LIGHTGREY);
     warehouseRect.setStrokeWidth(4);
     warehouseRect.setStroke(Color.BLACK);

     group.getChildren().add(warehouseRect);
   }


   private void displayDispensingPoint(Group group, Pair<Point2D, Point2D> dispensingPointCords) {
     Rectangle dispensingPointRec = new Rectangle(
       (int)Math.round(dispensingPointCords.getKey().getX()),
       (int)Math.round(dispensingPointCords.getKey().getY()),
       (int)Math.round(dispensingPointCords.getValue().getX()) - (int)Math.round(dispensingPointCords.getKey().getX()),
       (int)Math.round(dispensingPointCords.getValue().getY()) - (int)Math.round(dispensingPointCords.getKey().getY()));
     dispensingPointRec.setFill(Color.GREEN);
     dispensingPointRec.setStrokeWidth(4);
     dispensingPointRec.setStroke(Color.BLACK);

     group.getChildren().add(dispensingPointRec);
   }


   private List<Pair<Integer, Integer>> getAllRoutes(JSONObject data) {
     List<Pair<Integer, Integer>> routes = new ArrayList<>();
     JSONArray routesJSON = (JSONArray)data.get("routes");
     Iterator it = routesJSON.iterator();

     while(it.hasNext()){
       JSONArray route = (JSONArray)it.next();
        routes.add(new Pair<Integer, Integer>(((Long)route.get(0)).intValue(), ((Long)route.get(1)).intValue()));
     }

     return routes;
   }


   public void displayRoutes(Group group, List<Pair<Integer, Integer>> routes) {
     Iterator<Pair<Integer, Integer>> it = routes.iterator();

     while (it.hasNext()) {
       Pair<Integer, Integer> route = it.next();
       NodeCircle start = nodes.get(route.getKey());
       NodeCircle end = nodes.get(route.getValue());
       Line line = new Line();

       line.setStartX(start.getX());
       line.setStartY(start.getY());
       line.setEndX(end.getX());
       line.setEndY(end.getY());
       line.setFill(Color.RED);
       line.setStrokeWidth(1);
       line.setStroke(Color.RED);

       group.getChildren().add(line);
     }
   }


   @Override
   public void start(Stage primaryStage) throws Exception {
      // get names of JSON files to load simulation data
      List<String> args = getParameters().getRaw();
      List<JSONObject> data = this.loadJSONData(args);

      Pair<Point2D, Point2D> warehouseCords = getWarehouseCords(data.get(0));
      Pair<Point2D, Point2D> dispensingPointCords = getDispensingPointCords(data.get(0));
      List<Pair<Pair<Point2D, Point2D>, Integer>> shelfsCords = getAllShelfsCords(data.get(0));
      List<Pair<Point2D, Integer>> nodesCords = getAllNodesCords(data.get(0));
      List<Pair<Integer, Integer>> routes = getAllRoutes(data.get(0));

      int GUIWidth = 0; //not yet
      int sceneWidth =  (int)Math.round(warehouseCords.getValue().getX()) +
                        (int)Math.round(warehouseCords.getKey().getX()) +
                        GUIWidth;
      int sceneHeight = (int)Math.round(warehouseCords.getValue().getY()) +
                        (int)Math.round(warehouseCords.getKey().getY());

      //creating a Group object
      Group group = new Group();

      // warehouse background
      displayWarehouse(group, warehouseCords);

      // dispensing point
      displayDispensingPoint(group, dispensingPointCords);

      // add shelfs
      displayShelfs(group, shelfsCords);

      // if the debug option is passed, make nodes visible
      if (args.size() > 3 && args.get(3).equals("--debug")){
        // add nodes
        displayNodes(group, nodesCords);
        displayRoutes(group, routes);
      }

      //Creating a Scene by passing the group object, height and width
      Scene scene = new Scene(group ,sceneWidth, sceneHeight);

      //Setting the title to Stage.
      primaryStage.setTitle("Warehouse Simulation");

      //Adding the scene to Stage
      primaryStage.setScene(scene);

      //Displaying the contents of the stage
      primaryStage.show();
   }


   public static void main(String args[]){
      launch(args);
   }
}
