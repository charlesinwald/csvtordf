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
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.event.*;

// Jena imports
import org.apache.jena.base.Sys;
import org.apache.jena.rdf.model.*;
import org.apache.jena.shared.*;

public class CsvWizard extends Application {

  @Override
  public void start(Stage stage) {
    Label l = new Label("Prefix:");
    TextField prefixField = new TextField(CsvToRdf.prefix);
    prefixField.setPrefColumnCount(30);
    Button submit = new Button("Submit");
    HBox hb = new HBox();
    hb.getChildren().add(l);
    hb.getChildren().add(prefixField);
    hb.getChildren().add(submit);
    Scene scene = new Scene(hb, 640, 480);
    
    stage.setScene(scene);
    stage.show();

    submit.setOnAction(new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent e) {
        CsvToRdf.prefix = prefixField.getText();
        stage.close();
      }
    });

  }
}
