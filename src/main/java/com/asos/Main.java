package com.asos;

import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {
    
    @Override
    public void start(Stage primaryStage) {
        AsosApplication app = new AsosApplication();
        app.start(primaryStage);
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
