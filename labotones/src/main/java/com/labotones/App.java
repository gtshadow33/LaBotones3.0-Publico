package com.labotones;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    private static Controller controller;
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        
        FXMLLoader fxmlLoader = new FXMLLoader(
            App.class.getResource("/com/labotones/hello-view.fxml")
        );

        Scene scene = new Scene(fxmlLoader.load(), 900, 800);
        
        controller = fxmlLoader.getController();
        controller.setStage(stage);
        
        // ========== CORREGIDO: Solo pasar Stage ==========
        Navigator.initStage(stage);

        stage.setMinWidth(800);
        stage.setMinHeight(800);
        stage.setTitle("LaBotones - Gestión de equipos BCN ");
        stage.setScene(scene);
        
        stage.setOnCloseRequest(event -> {
            if (controller != null) {
                controller.shutdown();
            }
        });
        
        stage.show();
    }

    public static void setActiveController(Controller newController) {
        if (controller != null && controller != newController) {
            controller.shutdown();
        }
        controller = newController;
    }
    
    public static Controller getActiveController() {
        return controller;
    }
    
    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch();
    }
}