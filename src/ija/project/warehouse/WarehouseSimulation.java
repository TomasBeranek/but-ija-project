package ija.project.warehouse;

import ija.project.warehouse.ShelfRectangle;
import ija.project.warehouse.NodeCircle;
import ija.project.warehouse.Order;

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
import java.util.Map.Entry;
import java.util.concurrent.*;

//http://www.java2s.com/Code/JarDownload/json-simple/json-simple-1.1.jar.zip
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;

/** Controls the warehouse simulation. Sets the simulation time and speed. Loads
 *  a floor plan of a warehouse, goods and orders. Creates a GUI and updates the
 *  simulation if an interactive interventions like closing an alley, adding an
 *  order or changing the speed/time of the simulation happens.
 *
 * @author Tomas Beranek (xberan46)
 */
public class WarehouseSimulation extends Application {
   private ShelfRectangle highLightedShelf = null;
   private Text highLightedShelfID;
   private NodeCircle highLightedNode = null;
   private Text highLightedNodeID;
   private Hashtable<Integer, NodeCircle> nodes = new Hashtable<>();
   private Hashtable<Integer, ShelfRectangle> shelfs = new Hashtable<>();
   private boolean debug = false;
   private int warehouseWidth = 0;
   private int warehouseHeight = 0;
   private List<Order> orders = new ArrayList<>();

   private Integer currentEpochTime = 0;
   private Integer timeSpeed = 1;

   /** Loads files into JSON objects.
    *
    * @param fileNames The list of filenames:
    *           0) a floor plan
    *           1) goods
    *           2) orders
    * @return The list of created JSONObjects.
    */
   public List<JSONObject> loadJSONData(List<String> fileNames) {
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
       System.exit(1);
     }

     try {
       ordersData = (JSONObject) parser.parse(new FileReader(fileNames.get(2)));
       data.add(ordersData);
     } catch(Exception e) {
       System.err.printf("ERROR: failed to parse JSON file '%s'", fileNames.get(2));
       System.exit(1);
     }

     return data;
   }


   /** Loads the warheouse position from a JSONObject that contains a floor plan.
    *
    * @param data JSONObject with a floor plan of a warehouse.
    * @return A loaded pair of warehouse coordinates (left top, right bottom).
    */
   public Pair<Point2D, Point2D> getWarehouseCords(JSONObject data) {
      JSONArray lefttopJSON = (JSONArray)((JSONObject)data.get("warehouse")).get("lefttop");
      Point2D lefttop = new Point2D(((Long)lefttopJSON.get(0)).doubleValue(), ((Long)lefttopJSON.get(1)).doubleValue());

      JSONArray rightbottomJSON = (JSONArray)((JSONObject)data.get("warehouse")).get("rightbottom");
      Point2D rightbottom = new Point2D(((Long)rightbottomJSON.get(0)).doubleValue(), ((Long)rightbottomJSON.get(1)).doubleValue());

      return new Pair<>(lefttop, rightbottom);
   }


   /** Loads the dispensing point position from a JSONObject that contains
    *  a floor plan.
    *
    * @param data JSONObject with a floor plan of a warehouse.
    * @return A loaded pair of dispensing point coordinates (left top, right bottom)
    */
   public Pair<Point2D, Point2D> getDispensingPointCords(JSONObject data) {
      JSONArray lefttopJSON = (JSONArray)((JSONObject)data.get("dispensingPoint")).get("lefttop");
      Point2D lefttop = new Point2D(((Long)lefttopJSON.get(0)).doubleValue(), ((Long)lefttopJSON.get(1)).doubleValue());

      JSONArray rightbottomJSON = (JSONArray)((JSONObject)data.get("dispensingPoint")).get("rightbottom");
      Point2D rightbottom = new Point2D(((Long)rightbottomJSON.get(0)).doubleValue(), ((Long)rightbottomJSON.get(1)).doubleValue());

      return new Pair<>(lefttop, rightbottom);
   }


   /** Loads all the shelves data from a JSONObject that contains a floor plan.
    *
    * @param data JSONObject with a floor plan of a warehouse.
    * @return A list of loaded shelves informations. Each shelf's information contains --
    *     left top point, right bottom point, ID and nodeID.
    */
   public List<Pair<Pair<Pair<Point2D, Point2D>, Integer>, Integer>> getAllShelfsCords(JSONObject data) {
     List<Pair<Pair<Pair<Point2D, Point2D>, Integer>, Integer>> shelfCords = new ArrayList<>();
     JSONArray shelfsJSON = (JSONArray)data.get("shelfs");
     Iterator it = shelfsJSON.iterator();

     while(it.hasNext()){
        shelfCords.add(getShelfCords((JSONObject)it.next()));
     }

     return shelfCords;
   }


   /** Loads the shelf's position from a JSONObject that contains a single shelf.
    *
    * @param data JSONObject with a single shelf.
    * @return Loaded shelf's information -- left top point, right bottom point, ID
    *    and nodeID.
    */
   public Pair<Pair<Pair<Point2D, Point2D>, Integer>, Integer> getShelfCords(JSONObject data) {
      JSONArray lefttopJSON = (JSONArray)data.get("lefttop");
      Point2D lefttop = new Point2D(((Long)lefttopJSON.get(0)).doubleValue(), ((Long)lefttopJSON.get(1)).doubleValue());

      JSONArray rightbottomJSON = (JSONArray)data.get("rightbottom");
      Point2D rightbottom = new Point2D(((Long)rightbottomJSON.get(0)).doubleValue(), ((Long)rightbottomJSON.get(1)).doubleValue());

      Integer shelfID = ((Long)data.get("id")).intValue();
      Integer nodeID = ((Long)data.get("node")).intValue();

      return new Pair<>(new Pair<>(new Pair<>(lefttop, rightbottom), shelfID), nodeID);
   }


   /** Displays all the shelfs in the window.
    *
    * @param group The group object to which a shelf is added to be visible.
    * @param shelfsCords A list of shelves informations -- left top point,
    *    right bottom point and ID.
    */
   public void displayShelfs(Group group, List<Pair<Pair<Pair<Point2D, Point2D>, Integer>, Integer>> shelfsCords) {
     Iterator<Pair<Pair<Pair<Point2D, Point2D>, Integer>, Integer>> it = shelfsCords.iterator();

     while(it.hasNext()){
       Pair<Pair<Pair<Point2D, Point2D>, Integer>, Integer> shelfInfo = it.next();
       Pair<Point2D, Point2D> shelfCords = shelfInfo.getKey().getKey();
       ShelfRectangle shelfRec = new ShelfRectangle(
         (int)Math.round(shelfCords.getKey().getX()),
         (int)Math.round(shelfCords.getKey().getY()),
         (int)Math.round(shelfCords.getValue().getX()) - (int)Math.round(shelfCords.getKey().getX()),
         (int)Math.round(shelfCords.getValue().getY()) - (int)Math.round(shelfCords.getKey().getY()),
         shelfInfo.getKey().getValue(),
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

             // display shelfs with the same goods as the selected one
             if (debug) {
                 Set<Entry<Integer, ShelfRectangle>> it = shelfs.entrySet();

                 for (Entry<Integer, ShelfRectangle> shelf : it) {
                    if (shelfRec.getGoods().equals(shelf.getValue().getGoods()))
                      shelf.getValue().setFill(Color.ORANGE);
                    else
                      shelf.getValue().setFill(Color.BLUE);
                 }
             }

             shelfRec.setFill(Color.RED);
             highLightedShelf = (ShelfRectangle)shelfRec;
             highLightedShelfID.setText("Selected shelf's ID: " + shelfRec.shelfID +
                                        "\nAssociated node's ID: " + shelfRec.nodeID +
                                        "\nShelf's content: " + "\n " + shelfRec.getGoods() +
                                        "\nGoods quantity: " + shelfRec.getQuantity());
          }
       };

       shelfRec.addEventFilter(MouseEvent.MOUSE_CLICKED, shelfClickHandler);
       group.getChildren().add(shelfRec);
       shelfs.put(shelfRec.shelfID, shelfRec);
     }

     // add a text which shows ID of the selected shelf
     highLightedShelfID = new Text("");
     highLightedShelfID.setX(this.warehouseWidth);
     highLightedShelfID.setY(20);
     group.getChildren().add(highLightedShelfID);
   }


   /** Loads all the nodes data from a JSONObject that contains a floor plan.
    *
    * @param data JSONObject with a floor plan of a warehouse.
    * @return A list of loaded nodes informations. Each node's information contains --
    *     position and ID.
    */
   public List<Pair<Point2D, Integer>> getAllNodesCords(JSONObject data) {
     List<Pair<Point2D, Integer>> nodesCords = new ArrayList<>();
     JSONArray nodesJSON = (JSONArray)data.get("nodes");
     Iterator it = nodesJSON.iterator();

     while(it.hasNext()){
        nodesCords.add(getNodeCords((JSONObject)it.next()));
     }

     return nodesCords;
   }


   /** Loads the node's position from a JSONObject that contains a single node.
    *
    * @param data JSONObject with a single node.
    * @return Loaded node's information -- position and ID.
    */
   public Pair<Point2D, Integer> getNodeCords(JSONObject data) {
     JSONArray positionJSON = (JSONArray)data.get("position");
     Point2D position = new Point2D(((Long)positionJSON.get(0)).doubleValue(), ((Long)positionJSON.get(1)).doubleValue());

     return new Pair<>(position, ((Long)data.get("id")).intValue());
   }


   /** Displays all the nodes in the window if the program is in debug mode,
    *  otherwise only prepare them for the simulation.
    *
    * @param group The group object to which a node is added to be visible.
    * @param nodesCords A list of nodes informations -- position and ID.
    */
   public void displayNodes(Group group, List<Pair<Point2D, Integer>> nodesCords){
     Iterator<Pair<Point2D, Integer>> it = nodesCords.iterator();

     while(it.hasNext()){
       Pair<Point2D, Integer> nodeInfo = (Pair<Point2D, Integer>)it.next();
       NodeCircle nodeCircle = new NodeCircle(
         (float)Math.round(nodeInfo.getKey().getX()),
         (float)Math.round(nodeInfo.getKey().getY()),
         5.0f,
         nodeInfo.getValue());

       if (debug){
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
               highLightedNodeID.setText("Selected node: " + nodeCircle.ID+ " neighbours: " + nodeCircle.getNeighbours());
            }
         };

         nodeCircle.addEventFilter(MouseEvent.MOUSE_CLICKED, nodeClickHandler);
         group.getChildren().add(nodeCircle);
       }
       nodes.put(nodeCircle.ID, nodeCircle);
     }

     // add a text which shows ID of the selected shelf
     highLightedNodeID = new Text("");
     highLightedNodeID.setX(150);
     highLightedNodeID.setY(20);
     group.getChildren().add(highLightedNodeID);
   }


   /** Displays the warehouse background in the window.
    *
    * @param group The group object to which a warehouse is added to be visible.
    * @param warehouseCords The coordinates of the warehouse background.
    */
   public void displayWarehouse(Group group, Pair<Point2D, Point2D> warehouseCords) {
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


   /** Displays the dispensing point in the window.
    *
    * @param group The group object to which a dispensing point is added to be visible.
    * @param dispensingPointCords The coordinates of the dispensing point.
    */
   public void displayDispensingPoint(Group group, Pair<Point2D, Point2D> dispensingPointCords) {
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


   /** Loads all the routes between nodes from a JSONObject that contains a floor
    *  plan.
    *
    * @param data JSONObject with a floor plan of a warehouse.
    * @return A list of loaded routes specified as -- node1ID and node2ID.
    */
   public List<Pair<Integer, Integer>> getAllRoutes(JSONObject data) {
     List<Pair<Integer, Integer>> routes = new ArrayList<>();
     JSONArray routesJSON = (JSONArray)data.get("routes");
     Iterator it = routesJSON.iterator();

     while(it.hasNext()){
       JSONArray route = (JSONArray)it.next();
        routes.add(new Pair<Integer, Integer>(((Long)route.get(0)).intValue(), ((Long)route.get(1)).intValue()));
     }

     return routes;
   }


   /** Displays all the routes in the window if the program is in debug mode,
    *  otherwise only prepare them for the simulation.
    *
    * @param group The group object to which a route is added to be visible.
    * @param routes A list of routes spcified as -- node1ID and node2ID.
    */
   public void displayRoutes(Group group, List<Pair<Integer, Integer>> routes) {
     Iterator<Pair<Integer, Integer>> it = routes.iterator();

     while (it.hasNext()) {
       Pair<Integer, Integer> route = it.next();
       NodeCircle start = nodes.get(route.getKey());
       NodeCircle end = nodes.get(route.getValue());

       if (debug){
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

       // save each other to neighbours
       start.addNeighbour(end.ID);
       end.addNeighbour(start.ID);
     }
   }


   /** Loads all the goods from a JSONObject that contains a goods info.
    *
    * @param data JSONObject with a goods info.
    * @return A list of loaded goods informations. Each item's information contains --
    *     name, shelfID and quantity.
    */
   public List<Pair<Integer, Pair<String, Integer>>> getAllGoods(JSONObject data) {
     List<Pair<Integer, Pair<String, Integer>>> goods = new ArrayList<>();
     JSONArray goodsJSON = (JSONArray) data.get("goodsList");
     Iterator it = goodsJSON.iterator();

     while(it.hasNext()) {
       JSONObject singleGoods = (JSONObject)it.next();
       Integer shelfID = ((Long)singleGoods.get("shelf")).intValue();
       String goodsName = (String)singleGoods.get("goods");
       Integer goodsQuantity = ((Long)singleGoods.get("quantity")).intValue();
       goods.add(new Pair<>(shelfID, new Pair<>(goodsName, goodsQuantity)));
     }

     return goods;
   }


   /** Loads all the goods into shelves.
    *
    * @param goods A list of goods informations. Each item's information contains --
    *     name, shelfID and quantity.
    */
   public void loadGoodsToShelfs(List<Pair<Integer, Pair<String, Integer>>> goods) {
     Iterator<Pair<Integer, Pair<String, Integer>>> it = goods.iterator();

     while (it.hasNext()) {
       Pair<Integer, Pair<String, Integer>> singleGoods = (Pair<Integer, Pair<String, Integer>>)it.next();
       ShelfRectangle shelf = this.shelfs.get(singleGoods.getKey());

       // add all goods to existing shelfs
       shelf.addGoods(singleGoods.getValue().getKey(), singleGoods.getValue().getValue());
     }
   }


   /** Loads all the orders data from a JSONObject that contains an orders info.
    *
    * @param data JSONObject with an orders info.
    * @return A list of loaded orders.
    */
   public List<Order> getAllOrders(JSONObject data) {
     List<Order> orders = new ArrayList<>();
     JSONArray ordersJSON = (JSONArray) data.get("ordersList");
     Iterator it = ordersJSON.iterator();

     while(it.hasNext()) {
       JSONObject singleOrder = (JSONObject)it.next();
       Integer startEpochTime = ((Long)singleOrder.get("startEpochTime")).intValue();
       List<Pair<String, Integer>> goods = new ArrayList<>();

       JSONArray goodsJSON = (JSONArray) singleOrder.get("goods");
       Iterator goodsIt = goodsJSON.iterator();

       while(goodsIt.hasNext()) {
         JSONObject singleGoods = (JSONObject)goodsIt.next();
         String goodsName = (String)singleGoods.get("name");
         Integer goodsQuantity = ((Long)singleGoods.get("quantity")).intValue();
         goods.add(new Pair<>(goodsName, goodsQuantity));
       }


       orders.add(new Order(startEpochTime, goods));
     }

     return orders;
   }


   /** Controls the simulation -- increments time and redraws the canvas.
    */
   public void drawCurrentState() {
     // update cart cords

     this.currentEpochTime += this.timeSpeed;
   }

   /** Creates a GUI, loads data and starts the simulation.
    *
    * @param primaryStage The main stage.
    */
   @Override
   public void start(Stage primaryStage) throws Exception {
      // close the whole app after closing the window
      primaryStage.setOnCloseRequest(e -> System.exit(0));

      // get names of JSON files to load simulation data
      List<String> args = getParameters().getRaw();

      // if the debug option is passed, make nodes and routes visible
      if (args.size() > 3 && args.get(3).equals("--debug"))
        this.debug = true;

      List<JSONObject> data = this.loadJSONData(args);

      // loading data from JSON files
      Pair<Point2D, Point2D> warehouseCords = getWarehouseCords(data.get(0));
      Pair<Point2D, Point2D> dispensingPointCords = getDispensingPointCords(data.get(0));
      List<Pair<Pair<Pair<Point2D, Point2D>, Integer>, Integer>> shelfsCords = getAllShelfsCords(data.get(0));
      List<Pair<Point2D, Integer>> nodesCords = getAllNodesCords(data.get(0));
      List<Pair<Integer, Integer>> routes = getAllRoutes(data.get(0));
      List<Pair<Integer, Pair<String, Integer>>> goods = getAllGoods(data.get(1));
      this.orders = getAllOrders(data.get(2));

      int GUIWidth = 300;
      this.warehouseWidth = (int)Math.round(warehouseCords.getValue().getX()) +
                            (int)Math.round(warehouseCords.getKey().getX());
      this.warehouseHeight = (int)Math.round(warehouseCords.getValue().getY()) +
                             (int)Math.round(warehouseCords.getKey().getY());

      //creating a Group object
      Group group = new Group();

      // warehouse background
      displayWarehouse(group, warehouseCords);

      // dispensing point
      displayDispensingPoint(group, dispensingPointCords);

      // add shelfs
      displayShelfs(group, shelfsCords);

      // load goods into shelfs
      loadGoodsToShelfs(goods);

      // add nodes
      displayNodes(group, nodesCords);

      // add routes
      displayRoutes(group, routes);

      //Creating a Scene by passing the group object, height and width
      Scene scene = new Scene(group ,this.warehouseWidth + GUIWidth, this.warehouseHeight);

      //Setting the title to Stage.
      primaryStage.setTitle("Warehouse Simulation");

      //Adding the scene to Stage
      primaryStage.setScene(scene);

      //Displaying the contents of the stage
      primaryStage.show();

      //run the simulation
      ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
      Runnable drawState = () -> drawCurrentState();
      ScheduledFuture<?> drawStateHandle = scheduler.scheduleAtFixedRate(drawState, 0, 1000, TimeUnit.MILLISECONDS);
   }


   /** The entry point of the simulation.
    *
    * @param args Program arguments.
    */
   public static void main(String args[]){
      launch(args);
   }
}
