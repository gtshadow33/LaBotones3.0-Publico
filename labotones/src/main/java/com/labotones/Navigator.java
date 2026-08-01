package com.labotones;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import java.io.IOException;
import javafx.stage.Stage;

public class Navigator {

    private static Stage stage;

    public static void initStage(Stage st) {
        stage = st;
    }

    public static Object navegar(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(
                Navigator.class.getResource("/com/labotones/" + fxml)
            );
            Parent vista = loader.load();
            Object controller = loader.getController();

            Scene scene = new Scene(vista,
                                    stage.getScene().getWidth(),
                                    stage.getScene().getHeight());
            stage.setScene(scene);

            // ========== NUEVO: registrar el Controller activo si es el de home ==========
            if (controller instanceof Controller c) {
                App.setActiveController(c);
            }

            return controller;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void inicio() {
        navegar("hello-view.fxml");
    }
}