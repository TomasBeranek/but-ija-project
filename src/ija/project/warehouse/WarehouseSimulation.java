/**
* This file contains an implementation of WarehouseSimulation class with the
* main method. This class creates a GUI and controls the simulation.
*
* @author  Tomas Beranek (xberan46)
* @author  Simon Slobodnik (xslobo06)
* @since   19.3.2021
*/

package ija.project.warehouse;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import java.util.*;

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
   private void loadJSONData(List<String> fileNames) {
     JSONParser parser = new JSONParser();
     JSONObject warehouseData = new JSONObject();
     JSONObject goodsData;
     JSONObject ordersData;

     try {
       warehouseData = (JSONObject) parser.parse(new FileReader(fileNames.get(0)));
     } catch(Exception e) {
       System.err.printf("ERROR: failed to parse JSON file '%s'", fileNames.get(0));
       System.exit(1);
     }

     try {
       goodsData = (JSONObject) parser.parse(new FileReader(fileNames.get(1)));
     } catch(Exception e) {
       System.err.printf("ERROR: failed to parse JSON file '%s'", fileNames.get(1));
       System.exit(1);
     }

     try {
       ordersData = (JSONObject) parser.parse(new FileReader(fileNames.get(2)));
     } catch(Exception e) {
       System.err.printf("ERROR: failed to parse JSON file '%s'", fileNames.get(2));
       System.exit(1);
     }
   }

   @Override
   public void start(Stage primaryStage) throws Exception {
      // get names of JSON files to load simulation data
      this.loadJSONData(getParameters().getRaw());


      //creating a Group object
      Group group = new Group();

      // width = warehouseX2 + warehousex1 + panelWidth
      //Creating a Scene by passing the group object, height and width
      Scene scene = new Scene(group ,1000, 700);

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
