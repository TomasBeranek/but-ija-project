package ija.project.warehouse;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class WarehouseSimulation extends Application {
   @Override
   public void start(Stage primaryStage) throws Exception {
      //creating a Group object
      Group group = new Group();

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
