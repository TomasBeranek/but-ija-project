package ija.project.warehouse;

import ija.project.warehouse.ShelfRectangle;
import ija.project.warehouse.NodeCircle;
import ija.project.warehouse.Order;
import ija.project.warehouse.PathFinder;

import java.util.*;
import java.lang.Math;
import java.util.Map.Entry;
import java.util.concurrent.*;
import java.io.*;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.shape.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.transform.Scale;
import javafx.scene.paint.Color;
import javafx.scene.control.*;
import javafx.scene.text.*;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.geometry.Point2D;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.util.*;

//http://www.java2s.com/Code/JarDownload/json-simple/json-simple-1.1.jar.zip
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


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
   private int cartCapacity = 500;
   private boolean dontPrintSelected = false;
   private ListView<String> cartList;
   private Rectangle warehouseRect;
   private Rectangle dispensingPointRec;

   private int sideGUIWidth = 300;
   private int sideGUIHeight = 750;
   private int bottomGUIWidth = 900;
   private int bottomGUIHeight = 70;

   private Long currentEpochTime = 0L; // in ms
   private Long updateSpeed = 20L; // in ms
   private PathFinder pathFinder;

   private TextField inputGoodsName;
   private TextField inputGoodsQuantinty;
   private ListView<String> goodsList;
   private ObservableList<String> goodsLitems;
   private List<Pair<String, Integer>> inputGoodsList = new ArrayList<>();
   private List<Pair<Long, List<Pair<String, Integer>>>> addedOrdersInfo = new ArrayList<>();

   private double currZoom = 1;
   private double zoomX;
   private double zoomY;


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
       shelfRec.setStrokeType(StrokeType.INSIDE);

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
             highLightedShelfID.setText("ID: " + shelfRec.shelfID +
                                        "\nAssociated node's ID: " + shelfRec.nodeID +
                                        "\nContent: \n" + shelfRec.getGoods() +
                                        "\nQuantity: " + shelfRec.getQuantity());
          }
       };

       shelfRec.addEventFilter(MouseEvent.MOUSE_CLICKED, shelfClickHandler);
       group.getChildren().add(shelfRec);
       shelfs.put(shelfRec.shelfID, shelfRec);
     }

     //caption of highilighted shelf info
     Text highLightedShelfCaption = new Text("Selected shelf:");
     highLightedShelfCaption.setX(this.warehouseWidth + 25);
     highLightedShelfCaption.setY(150);
     highLightedShelfCaption.setFont(Font.font ("Sans-serif", 20));
     group.getChildren().add(highLightedShelfCaption);

     // add a text which shows ID of the selected shelf
     highLightedShelfID = new Text("ID: -\nAssociated node's ID: -\nContent:\n-\nQuantity: -");
     highLightedShelfID.setX(this.warehouseWidth + 25);
     highLightedShelfID.setY(180);
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
               highLightedNodeID.setText("ID: " + nodeCircle.ID + "\nNeighbours: " + nodeCircle.getNeighbours());
            }
         };

         nodeCircle.addEventFilter(MouseEvent.MOUSE_CLICKED, nodeClickHandler);
         group.getChildren().add(nodeCircle);
       }
       nodes.put(nodeCircle.ID, nodeCircle);
     }

     //caption of highilighted node info
     Text highLightedNodeCaption = new Text("Selected node:");
     highLightedNodeCaption.setX(this.warehouseWidth + 25);
     highLightedNodeCaption.setY(40);
     highLightedNodeCaption.setFont(Font.font ("Sans-serif", 20));
     group.getChildren().add(highLightedNodeCaption);

     // add a text which shows ID of the selected shelf
     highLightedNodeID = new Text("ID: -\nNeighbours: -");
     highLightedNodeID.setX(this.warehouseWidth + 25);
     highLightedNodeID.setY(70);
     group.getChildren().add(highLightedNodeID);
   }


   /** Displays the warehouse background in the window.
    *
    * @param group The group object to which a warehouse is added to be visible.
    * @param warehouseCords The coordinates of the warehouse background.
    */
   public void displayWarehouse(Group group, Pair<Point2D, Point2D> warehouseCords) {
     this.warehouseRect = new Rectangle(
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
     this.dispensingPointRec = new Rectangle(
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
       //make copy of initial state
       shelfsInitialQuantity.put(shelf.shelfID, shelf.getQuantity());
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
     if (highLightedNodeID != null && highLightedNode != null && !dontPrintSelected){
       this.highLightedNodeID.setText("ID: " + this.highLightedNode.ID + "\nNeighbours: " + this.highLightedNode.getNeighbours());
     }
     if (highLightedShelfID != null && highLightedShelf != null && !dontPrintSelected) {
       highLightedShelfID.setText("ID: " + highLightedShelf.shelfID +
                                  "\nAssociated node's ID: " + highLightedShelf.nodeID +
                                  "\nContent: \n" + highLightedShelf.getGoods() +
                                  "\nQuantity: " + highLightedShelf.getQuantity());
     }

     for (int i = 0; i < orders.size(); i++) {
       if (orders.get(i).isActive(this.currentEpochTime)){
         if (!orders.get(i).hasCart()){
           orders.get(i).addCart(this.group, pathFinder.findPath(orders.get(i).goods, shelfs, 0, 0), nodes, shelfs, this.cartList, this.cartCapacity);
         }

         //draw the cart
         if (!orders.get(i).drawCart(this.currentEpochTime, nodes)){
           //we need to recalculate the path
           List<Pair<Integer, Pair<String, Integer>>> remainingPath = orders.get(i).cart.getRemainingPath();
           System.out.println("remaining asdasdas");
           System.out.println(remainingPath);
           System.out.println("refind  asdasd");
           System.out.println(pathFinder.refindPath(remainingPath, orders.get(i).goods, shelfs, orders.get(i).cart.currCapacity));
           orders.get(i).updateCartPath(pathFinder.refindPath(remainingPath, orders.get(i).goods, shelfs, orders.get(i).cart.currCapacity), nodes);
         }
       }
     }

     //increment simulation time
     this.currentEpochTime += this.updateSpeed;
   }


   /** Closes the routes with a black color.
    */
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


   /** Opens the routes with a dark red color.
    */
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


   /** Zooms the scene (shelves, nodes, routes, warehouse and dispensing point)
    *  according to current zoom factor.
    */
   private void zoomScene(){
     //create scale
     Scale scale = new Scale();
     scale.setX(this.currZoom);
     scale.setY(this.currZoom);
     scale.setPivotX(this.zoomX);
     scale.setPivotY(this.zoomY);



     //carts
     for (int i = 0; i < this.orders.size(); i++) {
       this.orders.get(i).cart.getTransforms().clear();
       this.orders.get(i).cart.getTransforms().add(scale);
       this.orders.get(i).cart.toBack();
     }

     //nodes
     Set<Entry<Integer, NodeCircle>> itNode = this.nodes.entrySet();

     for (Entry<Integer, NodeCircle> node : itNode) {
       node.getValue().getTransforms().clear();
       node.getValue().getTransforms().add(scale);
       node.getValue().toBack();
     }

     //routes
     Set<Entry<String, Line>> itRoute = this.routes.entrySet();

     for (Entry<String, Line> route : itRoute) {
       route.getValue().getTransforms().clear();
       route.getValue().getTransforms().add(scale);
       route.getValue().toBack();
     }

     //shelves
     Set<Entry<Integer, ShelfRectangle>> itShelf = this.shelfs.entrySet();

     for (Entry<Integer, ShelfRectangle> shelf : itShelf) {
       shelf.getValue().getTransforms().clear();
       shelf.getValue().getTransforms().add(scale);
       shelf.getValue().toBack();
     }

     //dispensing point
     this.dispensingPointRec.getTransforms().clear();
     this.dispensingPointRec.getTransforms().add(scale);
     this.dispensingPointRec.toBack();

     //warehouse background
     this.warehouseRect.getTransforms().clear();
     this.warehouseRect.getTransforms().add(scale);
     this.warehouseRect.toBack();
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

      this.warehouseWidth = (int)Math.round(warehouseCords.getValue().getX()) +
                            (int)Math.round(warehouseCords.getKey().getX());
      this.warehouseHeight = (int)Math.round(warehouseCords.getValue().getY()) +
                             (int)Math.round(warehouseCords.getKey().getY());

      if (sideGUIHeight > this.warehouseHeight)
        this.warehouseHeight = sideGUIHeight;

      if (bottomGUIWidth > this.warehouseWidth)
        this.warehouseWidth = bottomGUIWidth;

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

      //display bottom GUI
      Rectangle bottomGUI = new Rectangle(0, this.warehouseHeight, this.warehouseWidth, bottomGUIHeight);
      bottomGUI.setFill(Color.WHITE);
      bottomGUI.setStrokeWidth(2);
      bottomGUI.setStroke(Color.LIGHTGREY);
      group.getChildren().add(bottomGUI);
      bottomGUI.toBack();

      //display side GUI
      Rectangle sideGUI = new Rectangle(this.warehouseWidth, 0, sideGUIWidth, this.warehouseHeight + bottomGUIHeight);
      sideGUI.setFill(Color.WHITE);
      sideGUI.setStrokeWidth(2);
      sideGUI.setStroke(Color.LIGHTGREY);
      group.getChildren().add(sideGUI);
      sideGUI.toBack();

      //display timer
      this.timer = new Text("00:00:00");
      this.timer.setX(30);
      this.timer.setY(this.warehouseHeight + 45);
      this.timer.setFont(Font.font ("Sans-serif", 30));
      group.getChildren().add(this.timer);

      // separate Timer from the rest of the GUI
      Line timerDelimiter = new Line(190, this.warehouseHeight, 190, this.warehouseHeight + bottomGUIHeight);
      timerDelimiter.setStrokeWidth(2);
      timerDelimiter.setStroke(Color.LIGHTGREY);
      group.getChildren().add(timerDelimiter);

      // display a button for simulation speed increase
      Button speedPlusButton = new Button("+");
      speedPlusButton.setPrefHeight(40);
      speedPlusButton.setPrefWidth(40);
      speedPlusButton.setFont(Font.font ("Sans-serif", 20));
      speedPlusButton.setLayoutX(190 + 15);
      speedPlusButton.setLayoutY(this.warehouseHeight + 15);
      speedPlusButton.setOnAction(actionEvent -> {
        if (updateSpeed/20 < 32)
          updateSpeed *= 2;
        speed.setText(String.format("%dx", updateSpeed/20));
      });
      group.getChildren().add(speedPlusButton);

      // display a button for simulation speed decrease
      Button speedMinusButton = new Button("-");
      speedMinusButton.setPrefHeight(40);
      speedMinusButton.setPrefWidth(40);
      speedMinusButton.setFont(Font.font ("Sans-serif", 20));
      speedMinusButton.setLayoutX(190 + 150);
      speedMinusButton.setLayoutY(this.warehouseHeight + 15);
      speedMinusButton.setOnAction(actionEvent -> {
        if (updateSpeed/20 > 1)
          updateSpeed /= 2;
        speed.setText(String.format("%dx", updateSpeed/20));
      });
      group.getChildren().add(speedMinusButton);

      // speed info
      this.speed = new Text("1x");
      this.speed.setX(280);
      this.speed.setY(this.warehouseHeight + 42);
      this.speed.setFont(Font.font ("Sans-serif", 20));
      this.speed.setTextAlignment(TextAlignment.CENTER);
      group.getChildren().add(this.speed);

      // separate speed controls from the rest of the GUI
      Line speedDelimiter = new Line(395, this.warehouseHeight, 395, this.warehouseHeight + bottomGUIHeight);
      speedDelimiter.setStrokeWidth(2);
      speedDelimiter.setStroke(Color.LIGHTGREY);
      group.getChildren().add(speedDelimiter);

      // separate reset timer from the rest of the GUI
      Line resetDelimiter = new Line(540, this.warehouseHeight, 540, this.warehouseHeight + bottomGUIHeight);
      resetDelimiter.setStrokeWidth(2);
      resetDelimiter.setStroke(Color.LIGHTGREY);
      group.getChildren().add(resetDelimiter);

      // display a button for route closing
      Button closeRouteButton = new Button("Apply route restrictions");
      closeRouteButton.setPrefHeight(40);
      closeRouteButton.setPrefWidth(200);
      closeRouteButton.setFont(Font.font ("Sans-serif", 13));
      closeRouteButton.setLayoutX(this.warehouseWidth - 15 - 200);
      closeRouteButton.setLayoutY(this.warehouseHeight + 15);
      closeRouteButton.setOnAction(actionEvent -> {
          closeBlackRoutes();
          openDarkredRoutes();
          pathFinder.setMatrix(nodes);
      });
      group.getChildren().add(closeRouteButton);

      // separate route restrictions from the rest of the GUI
      Line restrictionsDelimiter = new Line(this.warehouseWidth - 30 - 200, this.warehouseHeight, this.warehouseWidth - 30 - 200, this.warehouseHeight + bottomGUIHeight);
      restrictionsDelimiter.setStrokeWidth(2);
      restrictionsDelimiter.setStroke(Color.LIGHTGREY);
      group.getChildren().add(restrictionsDelimiter);

      // separate node info from the rest of the GUI
      Line nodeInfoDelimiter = new Line(this.warehouseWidth, 110, this.warehouseWidth + this.sideGUIWidth, 110);
      nodeInfoDelimiter.setStrokeWidth(2);
      nodeInfoDelimiter.setStroke(Color.LIGHTGREY);
      group.getChildren().add(nodeInfoDelimiter);

      // separate shelf info from the rest of the GUI
      Line shelfInfoDelimiter = new Line(this.warehouseWidth, 265, this.warehouseWidth + this.sideGUIWidth, 265);
      shelfInfoDelimiter.setStrokeWidth(2);
      shelfInfoDelimiter.setStroke(Color.LIGHTGREY);
      group.getChildren().add(shelfInfoDelimiter);

      //caption of highilighted cart info
      Text highLightedCartCaption = new Text("Goods on a cart:");
      highLightedCartCaption.setX(this.warehouseWidth + 25);
      highLightedCartCaption.setY(305);
      highLightedCartCaption.setFont(Font.font ("Sans-serif", 20));
      group.getChildren().add(highLightedCartCaption);

      // a list of loaded goods on a cart
      this.cartList = new ListView<String>();
      ObservableList<String> cartLitems = FXCollections.observableArrayList ("No cart selected");
      cartList.setItems(cartLitems);
      cartList.setPrefWidth(250);
      cartList.setPrefHeight(150);
      cartList.setLayoutX(this.warehouseWidth + 25);
      cartList.setLayoutY(325);
      group.getChildren().add(cartList);

      // separate a list of goods on a cart from the rest of the GUI
      Line cartInfoDelimiter = new Line(this.warehouseWidth, 505, this.warehouseWidth + this.sideGUIWidth, 505);
      cartInfoDelimiter.setStrokeWidth(2);
      cartInfoDelimiter.setStroke(Color.LIGHTGREY);
      group.getChildren().add(cartInfoDelimiter);

      //caption of "add an order"
      Text orderCaption = new Text("Add an order:");
      orderCaption.setX(this.warehouseWidth + 25);
      orderCaption.setY(545);
      orderCaption.setFont(Font.font ("Sans-serif", 20));
      group.getChildren().add(orderCaption);

      // goods name input
      this.inputGoodsName = new TextField ();
      this.inputGoodsName.setLayoutX(this.warehouseWidth + 25);
      this.inputGoodsName.setLayoutY(565);
      this.inputGoodsName.setPrefWidth(250);
      this.inputGoodsName.setPrefHeight(30);
      this.inputGoodsName.setPromptText("Type of goods");
      this.inputGoodsName.setFont(Font.font ("Sans-serif", 13));
      group.getChildren().add(this.inputGoodsName);

      // goods wuantity input
      this.inputGoodsQuantinty = new TextField ();
      this.inputGoodsQuantinty.setLayoutX(this.warehouseWidth + 25);
      this.inputGoodsQuantinty.setLayoutY(605);
      this.inputGoodsQuantinty.setPrefWidth(100);
      this.inputGoodsQuantinty.setPrefHeight(30);
      this.inputGoodsQuantinty.setPromptText("Quantity");
      this.inputGoodsQuantinty.setFont(Font.font ("Sans-serif", 13));
      group.getChildren().add(this.inputGoodsQuantinty);

      // add goods to order button
      Button addGoodsButton = new Button("Add");
      addGoodsButton.setPrefHeight(30);
      addGoodsButton.setPrefWidth(70);
      addGoodsButton.setFont(Font.font ("Sans-serif", 13));
      addGoodsButton.setLayoutX(this.warehouseWidth + this.sideGUIWidth - 25 - 70);
      addGoodsButton.setLayoutY(605);
      addGoodsButton.setOnAction(actionEvent -> {
        //check for invalid input
          this.goodsLitems.add(this.inputGoodsQuantinty.getText() + "x\t " + this.inputGoodsName.getText());
          this.goodsList.setItems(this.goodsLitems);
          int quantity = Integer.parseInt(this.inputGoodsQuantinty.getText().trim());
          this.inputGoodsList.add(new Pair<>(this.inputGoodsName.getText(), quantity));
          this.inputGoodsName.setText("");
          this.inputGoodsQuantinty.setText("");
      });
      group.getChildren().add(addGoodsButton);

      // add goods to order button
      Button addActiveGoodsButton = new Button("Add");
      addActiveGoodsButton.setPrefHeight(30);
      addActiveGoodsButton.setPrefWidth(70);
      addActiveGoodsButton.setFont(Font.font ("Sans-serif", 13));
      addActiveGoodsButton.setLayoutX(this.warehouseWidth + this.sideGUIWidth - 25 - 70);
      addActiveGoodsButton.setLayoutY(128);
      addActiveGoodsButton.setOnAction(actionEvent -> {
        if (this.highLightedShelf != null)
          this.inputGoodsName.setText(this.highLightedShelf.getGoods());
      });
      group.getChildren().add(addActiveGoodsButton);

      // a list of ordered goods
      this.goodsList = new ListView<String>();
      this.goodsLitems = FXCollections.observableArrayList ();
      goodsList.setItems(goodsLitems);
      goodsList.setPrefWidth(250);
      goodsList.setPrefHeight(105);
      goodsList.setLayoutX(this.warehouseWidth + 25);
      goodsList.setLayoutY(645);
      group.getChildren().add(goodsList);

      // confirm order button
      Button confirmOrderButton = new Button("Confirm order");
      confirmOrderButton.setPrefHeight(40);
      confirmOrderButton.setPrefWidth(115);
      confirmOrderButton.setFont(Font.font ("Sans-serif", 13));
      confirmOrderButton.setLayoutX(this.warehouseWidth + 25);
      confirmOrderButton.setLayoutY(645 + 105 + 15);
      confirmOrderButton.setOnAction(actionEvent -> {
        List<Pair<String, Integer>> goodsTmp  = new ArrayList<>();

        for (int i = 0; i < this.inputGoodsList.size(); i++){
          Pair<String, Integer> tmp = new Pair<>(this.inputGoodsList.get(i).getKey(), this.inputGoodsList.get(i).getValue());
          goodsTmp.add(tmp);
        }

        Order newOrder = new Order(this.currentEpochTime, goodsTmp);
        this.addedOrdersInfo.add(new Pair<>(this.currentEpochTime, this.inputGoodsList));

        this.inputGoodsList = new ArrayList<>();
        this.orders.add(newOrder);
        this.goodsLitems.clear();
        this.inputGoodsList.clear();
        this.goodsList.setItems(goodsLitems);
        this.inputGoodsName.setText("");
        this.inputGoodsQuantinty.setText("");
      });
      group.getChildren().add(confirmOrderButton);

      // delete order button
      Button clearOrderButton = new Button("Clear order");
      clearOrderButton.setPrefHeight(40);
      clearOrderButton.setPrefWidth(125);
      clearOrderButton.setFont(Font.font ("Sans-serif", 13));
      clearOrderButton.setLayoutX(this.warehouseWidth + 25 + 115 + 15);
      clearOrderButton.setLayoutY(645 + 105 + 15);
      clearOrderButton.setOnAction(actionEvent -> {
        this.goodsLitems.clear();
        this.inputGoodsList.clear();
        this.goodsList.setItems(goodsLitems);
        this.inputGoodsName.setText("");
        this.inputGoodsQuantinty.setText("");
      });
      group.getChildren().add(clearOrderButton);

      // set time button
      Button setTimeButton = new Button("Reset time");
      setTimeButton.setPrefHeight(40);
      setTimeButton.setPrefWidth(115);
      setTimeButton.setFont(Font.font ("Sans-serif", 13));
      setTimeButton.setLayoutX(395 + 15);
      setTimeButton.setLayoutY(this.warehouseHeight + 15);
      setTimeButton.setOnAction(actionEvent -> {
        currentEpochTime = 0L;

        for (int i = 0; i < orders.size(); i++) {
          orders.get(i).cart.setRadius(0);
          group.getChildren().remove(orders.get(i).cart);
        }

        dontPrintSelected = true;
        if (this.highLightedNodeID != null){
          this.highLightedNodeID.setText("ID: -\nNeighbours: -");
        }

        highLightedNode = null;

        for(Integer nodeID : nodes.keySet()){
          nodes.get(nodeID).setFill(Color.RED);
          nodes.get(nodeID).setRadius(5);
        }

        //group.getChildren().remove(highLightedShelfID);
        highLightedShelfID.setText("ID: -\nAssociated node's ID: -\nContent:\n-\nQuantity: -");
        highLightedNode = null;

        this.cartList.setItems(FXCollections.observableArrayList ("No cart selected"));

        this.orders = new ArrayList<>();
        this.orders = getAllOrders(data.get(2));

        for (int i = 0; i < this.addedOrdersInfo.size(); i++) {
          this.orders.add(new Order(this.addedOrdersInfo.get(i).getKey(), this.addedOrdersInfo.get(i).getValue()));
        }


        //set shelves to initial state
        Set<Entry<Integer, ShelfRectangle>> itShelf = this.shelfs.entrySet();

        for (Entry<Integer, ShelfRectangle> shelf : itShelf) {
          shelf.getValue().setQuantity(this.shelfsInitialQuantity.get(shelf.getValue().shelfID));
        }

        //reset pathFinder
        this.pathFinder = new PathFinder(this.nodes, this.shelfs, this.cartCapacity);

        //reset all carts
        for (int i = 0; i < orders.size(); i++) {
          orders.get(i).addCart(this.group, pathFinder.findPath(orders.get(i).goods, shelfs, 0, 0), nodes, shelfs, this.cartList, this.cartCapacity);
        }

        //zoom newly created carts
        this.zoomScene();
      });
      group.getChildren().add(setTimeButton);

      //Creating a Scene by passing the group object, height and width
      Scene scene = new Scene(group ,this.warehouseWidth + sideGUIWidth, this.warehouseHeight + bottomGUIHeight);

      WarehouseSimulation thisCopy = this;

      //Creating the mouse event handler
      scene.addEventFilter(ScrollEvent.SCROLL, new EventHandler<ScrollEvent>() {
          @Override
          public void handle(ScrollEvent event) {
            thisCopy.zoomX = event.getX();
            thisCopy.zoomY = event.getY();

            if (event.getDeltaY() > 0){
              if (currZoom < 8)
                currZoom *= 2;
            } else if (event.getDeltaY() < 0) {
              if (currZoom > 1.0/8)
                currZoom /= 2;
            }

            thisCopy.zoomScene();
          }
      });

      //Setting the title to Stage
      primaryStage.setTitle("Warehouse Simulation");

      //disable resizing
      primaryStage.setResizable(false);

      //Adding the scene to Stage
      primaryStage.setScene(scene);

      //Displaying the contents of the stage
      primaryStage.show();

      //initialize PathFinder -- creates a matrix of distances
      pathFinder = new PathFinder(this.nodes, this.shelfs, this.cartCapacity);

      //run the simulation
      //ugly,ugly nesting
      ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

      Runnable updater = new Runnable() {
        @Override
        public void run() {
          Runnable drawState = new Runnable() {
            @Override
            public void run() {
              try {
                 drawCurrentState();
              } catch (Exception e) {
                 e.printStackTrace();
                 e.getMessage();
              }
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
