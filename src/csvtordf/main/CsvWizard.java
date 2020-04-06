/**
 * CsvWizard.java
 * <p>
 * Authors:
 * Cody D'Ambrosio || cjd218 || cjd218@lehigh.edu
 * Charles Inwald  || cci219 || cci219@lehigh.edu
 * Paul Grocholske || pag314 || pag314@lehigh.edu
 * <p>
 * <p>
 * FIXME: Add description of application
 */
package csvtordf.main;

// Java imports
import java.io.*;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.concurrent.*;

// Java GUI
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class CsvWizard extends Application {

  @Override
  public void start(Stage stage) {
    Label l = new Label("Hello World!");
    Scene scene = new Scene(new StackPane(l), 640, 480);
    stage.setScene(scene);
    stage.show();
  }
}
