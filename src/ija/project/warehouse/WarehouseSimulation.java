package ija.project.warehouse;

import ija.project.warehouse.ShelfRectangle;
import ija.project.warehouse.NodeCircle;
import ija.project.warehouse.Order;
import ija.project.warehouse.PathFinder;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import java.util.Date;
import javafx.util.*;
import java.util.*;
import javafx.geometry.Point2D;
import java.lang.Math;
import javafx.scene.shape.*;
import javafx.scene.paint.Color;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.*;
import javafx.scene.control.Button;
import java.util.Map.Entry;
import java.util.concurrent.*;
import javafx.scene.control.TextField;

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
   private Hashtable<Integer, Integer> shelfsInitialQuantity = new Hashtable<>();
   private Hashtable<String, Line> routes = new Hashtable<>();
   private boolean debug = false;
   private int warehouseWidth = 0;
   private int warehouseHeight = 0;
   private List<Order> orders = new ArrayList<>();
   private Group group = new Group();
   private Text timer;
   private Text speed;

   private Long currentEpochTime = 0L; // in ms
   private Long updateSpeed = 20L; // in ms
   private PathFinder pathFinder;
   private TextField setTimeInput;


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
         0.0f,
         nodeInfo.getValue());

       nodeCircle.setFill(Color.RED);

       if (debug){
         nodeCircle.setStrokeWidth(0);
         nodeCircle.setRadius(5);

         //Creating the mouse event handler
         EventHandler<MouseEvent> nodeClickHandler = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent e) {
               for(Integer nodeID : nodes.keySet()){
                 nodes.get(nodeID).setFill(Color.RED);
                 nodes.get(nodeID).setRadius(5);
               }

               nodeCircle.setFill(Color.BLUE);
               nodeCircle.setRadius(7);
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
         line.setStrokeWidth(3);
         line.setStroke(Color.RED);

         EventHandler<MouseEvent> routeClickHandler = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent e) {
              if (line.getStroke().equals(Color.RED)){
                line.setStroke(Color.BLACK);
              } else if (line.getStroke().equals(Color.BLACK)){
                line.setStroke(Color.RED);
              } else if (line.getStroke().equals(Color.GREY)){
                line.setStroke(Color.DARKRED);
              } else if (line.getStroke().equals(Color.DARKRED)){
                line.setStroke(Color.GREY);
              }
            }
         };

         line.addEventFilter(MouseEvent.MOUSE_CLICKED, routeClickHandler);

         this.routes.put("" + start.ID + " " + end.ID, line);
         this.routes.put("" + end.ID + " " + start.ID, line);
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
       Long startEpochTime = (Long)singleOrder.get("startEpochTime");
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
     //cycle through orders
     this.timer.setText(String.format("%02d:%02d:%02d", (this.currentEpochTime/3600000)%24, (this.currentEpochTime/60000)%60, (this.currentEpochTime/1000)%60));

     for (int i = 0; i < orders.size(); i++) {
       if (orders.get(i).isActive(this.currentEpochTime)){
         if (!orders.get(i).hasCart()){
           orders.get(i).addCart(this.group, pathFinder.findPath(orders.get(i).goods, shelfs, 0), nodes, shelfs);
         }

         //draw the cart
         if (!orders.get(i).drawCart(this.currentEpochTime, nodes)){
           //we need to recalculate the path
           List<Pair<Integer, Pair<String, Integer>>> remainingPath = orders.get(i).cart.getRemainingPath();
           orders.get(i).updateCartPath(pathFinder.refindPath(remainingPath, orders.get(i).goods, shelfs), nodes);
         }
       }
     }

     //increment simulation time
     this.currentEpochTime += this.updateSpeed;
   }

   public void closeBlackRoutes(){
     for(String routeKey : this.routes.keySet()) {
       if (this.routes.get(routeKey).getStroke().equals(Color.BLACK)){
         int startNodeID = Integer.parseInt(routeKey.split(" ")[0]);
         int endNodeID = Integer.parseInt(routeKey.split(" ")[1]);

         this.nodes.get(startNodeID).removeNeighbour(endNodeID);
         this.nodes.get(endNodeID).removeNeighbour(startNodeID);

         this.routes.get(routeKey).setStroke(Color.GREY);
       }
     }
   }


   public void openDarkredRoutes(){
     for(String routeKey : this.routes.keySet()) {
       if (this.routes.get(routeKey).getStroke().equals(Color.DARKRED)){
         int startNodeID = Integer.parseInt(routeKey.split(" ")[0]);
         int endNodeID = Integer.parseInt(routeKey.split(" ")[1]);

         this.nodes.get(startNodeID).addNeighbour(endNodeID);
         this.nodes.get(endNodeID).addNeighbour(startNodeID);

         this.routes.get(routeKey).setStroke(Color.RED);
       }
     }
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

      // save initial shelves quantity
      for(Integer shelfID : this.shelfs.keySet()) {
        this.shelfsInitialQuantity.put(shelfID, this.shelfs.get(shelfID).getQuantity());
      }

      // move every node to front
      for(Integer nodeID : this.nodes.keySet()) {
        nodes.get(nodeID).toFront();
      }

      // display a button for route closing
      Button closeRouteButton = new Button("Apply route restrictions");
      closeRouteButton.setPrefHeight(40);
      closeRouteButton.setPrefWidth(200);
      closeRouteButton.setFont(Font.font ("Sans-serif", 13));
      closeRouteButton.setLayoutX(this.warehouseWidth + 50);
      closeRouteButton.setLayoutY(this.warehouseHeight - 70);
      closeRouteButton.setOnAction(actionEvent -> {
          closeBlackRoutes();
          openDarkredRoutes();
          pathFinder.setMatrix(nodes);
      });
      group.getChildren().add(closeRouteButton);

      // separate GUI from scene
      Line guiDelimiter = new Line(this.warehouseWidth, 0, this.warehouseWidth, this.warehouseHeight);
      guiDelimiter.setStrokeWidth(2);
      guiDelimiter.setStroke(Color.LIGHTGREY);
      group.getChildren().add(guiDelimiter);

      // timer
      this.timer = new Text("00:00:00");
      this.timer.setX(this.warehouseWidth + 60);
      this.timer.setY(70);
      this.timer.setFont(Font.font ("Sans-serif", 40));
      group.getChildren().add(this.timer);

      // separate Timer from the rest of GUI
      Line timerDelimiter = new Line(this.warehouseWidth, 110, this.warehouseWidth+GUIWidth, 110);
      timerDelimiter.setStrokeWidth(2);
      timerDelimiter.setStroke(Color.LIGHTGREY);
      group.getChildren().add(timerDelimiter);

      // separate Timer from the rest of GUI
      Line speedDelimiter = new Line(this.warehouseWidth, 170, this.warehouseWidth+GUIWidth, 170);
      speedDelimiter.setStrokeWidth(2);
      speedDelimiter.setStroke(Color.LIGHTGREY);
      group.getChildren().add(speedDelimiter);

      // display a button for simulation speed increase
      Button speedPlusButton = new Button("+");
      speedPlusButton.setPrefHeight(40);
      speedPlusButton.setPrefWidth(40);
      speedPlusButton.setFont(Font.font ("Sans-serif", 20));
      speedPlusButton.setLayoutX(this.warehouseWidth + 30);
      speedPlusButton.setLayoutY(120);
      speedPlusButton.setOnAction(actionEvent -> {
        if (updateSpeed/20 < 64)
          updateSpeed *= 2;
        speed.setText(String.format("%dx", updateSpeed/20));
      });
      group.getChildren().add(speedPlusButton);

      // display a button for simulation speed decrease
      Button speedMinusButton = new Button("-");
      speedMinusButton.setPrefHeight(40);
      speedMinusButton.setPrefWidth(40);
      speedMinusButton.setFont(Font.font ("Sans-serif", 20));
      speedMinusButton.setLayoutX(this.warehouseWidth + GUIWidth - 70);
      speedMinusButton.setLayoutY(120);
      speedMinusButton.setOnAction(actionEvent -> {
        if (updateSpeed/20 > 1)
          updateSpeed /= 2;
        speed.setText(String.format("%dx", updateSpeed/20));
      });
      group.getChildren().add(speedMinusButton);

      // speed info
      this.speed = new Text("1x");
      this.speed.setX(this.warehouseWidth + 135);
      this.speed.setY(147);
      this.speed.setFont(Font.font ("Sans-serif", 20));
      this.speed.setTextAlignment(TextAlignment.CENTER);
      group.getChildren().add(this.speed);

      // set time input
      this.setTimeInput = new TextField ();
      this.setTimeInput.setLayoutX(this.warehouseWidth + 25);
      this.setTimeInput.setLayoutY(190);
      this.setTimeInput.setPrefWidth(115);
      this.setTimeInput.setPrefHeight(40);
      this.setTimeInput.setPromptText("hh:mm:ss");
      this.setTimeInput.setFont(Font.font ("Sans-serif", 18));
      group.getChildren().add(this.setTimeInput);

      // set time button
      Button setTimeButton = new Button("Set time");
      setTimeButton.setPrefHeight(40);
      setTimeButton.setPrefWidth(115);
      setTimeButton.setFont(Font.font ("Sans-serif", 13));
      setTimeButton.setLayoutX(this.warehouseWidth + 160);
      setTimeButton.setLayoutY(190);
      setTimeButton.setOnAction(actionEvent -> {
        Long s = Long.parseLong(setTimeInput.getText().split(":")[2]);
        Long m = Long.parseLong(setTimeInput.getText().split(":")[1]);
        Long h = Long.parseLong(setTimeInput.getText().split(":")[0]);
        currentEpochTime = h*3600000 + m*60000 + s*1000;

        //set all the shelves into initial state
        for(Integer shelfID : this.shelfsInitialQuantity.keySet()) {
          this.shelfs.get(shelfID).setQuantity(this.shelfsInitialQuantity.get(shelfID));
        }

        for (int i = 0; i < orders.size(); i++) {
          orders.get(i).addCart(this.group, pathFinder.findPath(orders.get(i).goods, shelfs, 0), nodes, shelfs);
        }
      });
      group.getChildren().add(setTimeButton);

      //Creating a Scene by passing the group object, height and width
      Scene scene = new Scene(group ,this.warehouseWidth + GUIWidth, this.warehouseHeight);

      //Setting the title to Stage
      primaryStage.setTitle("Warehouse Simulation");

      //Adding the scene to Stage
      primaryStage.setScene(scene);

      //Displaying the contents of the stage
      primaryStage.show();

      //initialize PathFinder -- creates a matrix of distances
      //pathFinder = new PathFinder(nodes);
      pathFinder = new PathFinder(nodes, shelfs);

      //run the simulation
      //ugly,ugly nesting
      ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

      Runnable updater = new Runnable() {
        @Override
        public void run() {
          Runnable drawState = new Runnable() {
            @Override
            public void run() {
                drawCurrentState();
            }
          };
          Platform.runLater(drawState);
        }
      };

      scheduler.scheduleAtFixedRate(updater, 0, this.updateSpeed, TimeUnit.MILLISECONDS);
   }


   /** The entry point of the simulation.
    *
    * @param args Program arguments.
    */
   public static void main(String args[]){
      launch(args);
   }
}
