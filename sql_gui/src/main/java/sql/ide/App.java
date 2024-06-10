package sql.ide;

import java.io.File;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import sql.ide.controllers.FileEditorController;

/**
 * JavaFX App
 */
public class App extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("fxml/SimpleFileEditor.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("css/highlight.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.setTitle("Uriegas SQL IDE");

        // set the close event to call the closeApplication function
        primaryStage.setOnCloseRequest(
            // exitApplication function on FileEditorController
            event -> ((FileEditorController) loader.getController()).closeApplication()
        );
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch();
    }

}