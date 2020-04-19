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
import java.util.List;

// Java GUI
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
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

// Jena Imports
import org.apache.jena.rdf.model.*;

// Protege Imports
import org.protege.editor.owl.model.OWLModelManager;
import org.protege.editor.owl.ui.action.ProtegeOWLAction;


public class CsvWizard extends Application {

    public static final String LEFT_PANE_TITLE = "Options";
    public static final int DEFAULT_NUMBER_OF_THREADS = 1;
    //The maximum amount of threads they should be able to run
    int processors = Runtime.getRuntime().availableProcessors();
    private String selectedFilePath;
    private CheckBox interactiveCheckBox;
    private int numberOfThreads = DEFAULT_NUMBER_OF_THREADS;
    private Slider multithreadingSlider = new Slider(1, processors, DEFAULT_NUMBER_OF_THREADS);
    private Boolean modelLoaded = false;
    private VBox centerPane;
    private Button saveButton;
    public static OWLModelManager modelManager;
    public boolean runAsPlugin = false;
    private String selectedFileName;

    /**
     * The main entry point of the application, (running on the JavaFX application thread)
     *
     * @param stage Top level of the JavaFX container; most of the application is built upon it,
     *              stage.show() intuitively shows everything you add
     */
    @Override
    public void start(Stage stage) {
        // Set if called as a plugin for Protege
        if (!runAsPlugin) {
          Parameters params = getParameters();
          List<String> list = params.getRaw();
          if (list.size() > 0 && list.get(0).equals("plugin")) {
            runAsPlugin = true;
	    Platform.setImplicitExit(false);
	  }
	}


        // Setup scene
        BorderPane border = new BorderPane();
        HBox hbox = buildTopBar();
        border.setTop(hbox);
        border.setLeft(buildLeftPane());
        border.setCenter(buildCenterPane());
        Scene scene = new Scene(border, 1024, 768);
        scene.getStylesheets().add(this.getClass().getResource("CsvWizard.css").toExternalForm());

        stage.setScene(scene);
        stage.setTitle("CSV to RDF Converter");
        stage.getIcons().add(
                new Image(
                        CsvWizard.class.getResourceAsStream("icon.png")));
        Screen screen = Screen.getPrimary();
        Rectangle2D bounds = screen.getVisualBounds();

        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());
        stage.show();


    }

    public HBox buildTopBar() {
        Label l = new Label("Prefix:");
        l.setId("prefix-label");
        l.setAlignment(Pos.CENTER_RIGHT);
        l.setPadding(new Insets(5, 5, 5, 5));

        Label currentFile = new Label();
        currentFile.setId("prefix-label");
        currentFile.setAlignment(Pos.CENTER_RIGHT);
        currentFile.setPadding(new Insets(5, 5, 5, 5));

        TextField prefixField = new TextField(CsvToRdf.prefix);
        prefixField.setId("prefix-field");
        prefixField.setPrefColumnCount(30);
        if (runAsPlugin) {
          // Use Protege IRI
	  String iri = modelManager.getActiveOntology().getOntologyID().getOntologyIRI().get().toString() + "#";
          prefixField.setText(iri);
          prefixField.setDisable(true);
        }

        Button submit = new Button("Convert");
        submit.setPrefSize(100, 20);
        submit.setId("submit-button");
        submit.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                CsvToRdf.prefix = prefixField.getText();
                //TODO UI elements for schema
                CsvToRdf.readInputFile(selectedFilePath, false, "", numberOfThreads);
                modelLoaded = true;
                saveButton.setVisible(true);
                // TODO: Show conversion time
                viewModel();

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
                FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("CSV files (*.csv)", "*.csv");
                fileChooser.getExtensionFilters().add(extFilter);

                File selectedFile = fileChooser.showOpenDialog(null);
                //TODO handle if they supply incorrect path
                selectedFileName = selectedFile.getName().replaceFirst("[.][^.]+$", "");;
                selectedFilePath = selectedFile.getPath();
                System.out.println(selectedFilePath);
                currentFile.setText("CSV File: " + selectedFilePath);
                submit.setVisible(true);
            }
        });


        HBox hbox = new HBox();
        hbox.setPadding(new Insets(15, 12, 15, 12));
        hbox.setSpacing(10);
        hbox.setStyle("-fx-background-color: #595959;");


        hbox.getChildren().addAll(l, prefixField, openfile, currentFile, submit);

        return hbox;
    }

    public VBox buildLeftPane() {
        VBox leftPane = new VBox();
        leftPane.setPadding(new Insets(10));
        leftPane.setSpacing(16);
        leftPane.setId("left-pane");

        Label leftPaneTitle = new Label(LEFT_PANE_TITLE);
        leftPaneTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        leftPane.getChildren().add(leftPaneTitle);

//        interactiveCheckBox = new CheckBox("Interactive");
//        leftPane.getChildren().add(interactiveCheckBox);


        Label numberOfThreadsLabel = new Label(String.format("Using %d/%d available threads on your machine", DEFAULT_NUMBER_OF_THREADS, processors));
        leftPane.getChildren().add(numberOfThreadsLabel);


        multithreadingSlider.setBlockIncrement(1);
        multithreadingSlider.setMajorTickUnit(1);
        multithreadingSlider.setMinorTickCount(0);
        multithreadingSlider.setSnapToTicks(true);
        //multithreadingSlider.setShowTickLabels(true);
        multithreadingSlider.valueProperty().addListener(new ChangeListener<Number>() {
            public void changed(ObservableValue<? extends Number> ov,
                                Number old_val, Number new_val) {
                numberOfThreads = new_val.intValue();
                numberOfThreadsLabel.setText(String.format("Using %d/%d available threads on your machine", numberOfThreads, processors));
            }
        });
        leftPane.getChildren().add(multithreadingSlider);

        saveButton = new Button("Save RDF");
        saveButton.setVisible(false);
        saveButton.setId("save-button");
        saveButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Save " + selectedFileName + " as RDF");
                //Specify we are saving RDF files here
                FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("RDF files (*.rdf)", "*.rdf");
                fileChooser.getExtensionFilters().add(extFilter);
                //Default name for file is the input file's name
                fileChooser.setInitialFileName(selectedFileName + ".rdf");

                //Show save file dialog
                File file = fileChooser.showSaveDialog(null);

                if (file != null) {
                    CsvToRdf.outputModel(file.getAbsolutePath());
                }
            }
        });;
        leftPane.getChildren().add(saveButton);

        return leftPane;
    }

    public VBox buildCenterPane() {
        centerPane = new VBox();
        centerPane.setPadding(new Insets(10));
        centerPane.setSpacing(16);
        centerPane.setId("center-pane");


        return centerPane;
    }

    public void viewModel() {
        if (modelLoaded) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            CsvToRdf.model.write(byteArrayOutputStream, "RDF/XML-ABBREV");

            Label rdfText = new Label(byteArrayOutputStream.toString());
            rdfText.setId("rdf-text");
            ScrollPane scrollPane = new ScrollPane();
            // Set content for ScrollPane
            scrollPane.setContent(rdfText);

            // Always show vertical scroll bar
            scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);

            // Horizontal scroll bar is only displayed when needed
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

            centerPane.getChildren().add(scrollPane);
        }
    }
}

