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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.event.*;

// Jena imports
import org.apache.jena.base.Sys;
import org.apache.jena.rdf.model.*;
import org.apache.jena.shared.*;
import org.junit.jupiter.params.shadow.com.univocity.parsers.csv.Csv;

public class CsvWizard extends Application {

  private String selectedFilePath;

  @Override
  public void start(Stage stage) {

    BorderPane border = new BorderPane();
    HBox hbox = addHBox();
    border.setTop(hbox);
    border.setLeft(addVBox());
    Scene scene = new Scene(border, 1024, 768);
    scene.getStylesheets().add("csvtordf/main/CsvWizard.css");

    stage.setScene(scene);
    stage.setTitle("CSVtoRDF");
    stage.getIcons().add(
            new Image(
                    CsvWizard.class.getResourceAsStream( "icon.png" )));
    Screen screen = Screen.getPrimary();
    Rectangle2D bounds = screen.getVisualBounds();

    stage.setX(bounds.getMinX());
    stage.setY(bounds.getMinY());
    stage.setWidth(bounds.getWidth());
    stage.setHeight(bounds.getHeight());
    stage.show();



  }

  public HBox addHBox() {
    Label l = new Label("Prefix:");
    l.setId("prefix-label");
    l.setAlignment(Pos.CENTER_RIGHT);
    l.setPadding(new Insets(5,5,5,5));

    Label currentFile = new Label();
    currentFile.setId("prefix-label");
    currentFile.setAlignment(Pos.CENTER_RIGHT);
    currentFile.setPadding(new Insets(5,5,5,5));


    TextField prefixField = new TextField(CsvToRdf.prefix);
    prefixField.setPrefColumnCount(30);

    Button submit = new Button("Submit");
    submit.setPrefSize(100, 20);
    submit.setId("submit-button");
    submit.setOnAction(new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent e) {
        CsvToRdf.prefix = prefixField.getText();
        //TODO UI elements for interactive, schema and threads
        CsvToRdf.readInputFile(selectedFilePath, false, "", 1);

//        stage.close();
      }
    });
    submit.setVisible(false);

    Button openfile = new Button("Open CSV");
    openfile.setPrefSize(100, 20);
    openfile.setId("openfile-button");
    openfile.setOnAction(new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent e) {
        FileChooser fileChooser = new FileChooser();
        File selectedFile = fileChooser.showOpenDialog(null);
        //TODO handle if they supply incorrect path
        selectedFilePath = selectedFile.getPath();
        System.out.println(selectedFilePath);
        currentFile.setText("CSV File: " + selectedFilePath);
        submit.setVisible(true);
      }
    });


    HBox hbox = new HBox();
    hbox.setPadding(new Insets(15, 12, 15, 12));
    hbox.setSpacing(10);
    hbox.setStyle("-fx-background-color: #ea6652;");



    hbox.getChildren().addAll(l, prefixField,openfile, currentFile, submit);

    return hbox;
  }

  public VBox addVBox()
  {
    VBox vbox = new VBox();
    vbox.setPadding(new Insets(10));
    vbox.setSpacing(8);

    Text title = new Text("Data");
    title.setFont(Font.font("Arial", FontWeight.BOLD, 14));
    vbox.getChildren().add(title);

    Text options[] = new Text[]{
            new Text("1"),
            new Text("2"),
            new Text("3"),
            new Text("4")};
    for (int i=0; i<4; i++) {
      VBox.setMargin(options[i], new Insets(0, 0, 0, 8));
      vbox.getChildren().add(options[i]);
    }
    return vbox;
  }
}
