/**
 * <h1>CsvWizard.java<h1>
 * <p>
 *
 * @author Cody D'Ambrosio || cjd218 || cjd218@lehigh.edu
 * @author Charles Inwald  || cci219 || cci219@lehigh.edu
 * @author Paul Grocholske || pag314 || pag314@lehigh.edu
 * <p>
 * GUI Wizard for importing a CSV file and converting it to
 * RDF syntax. The GUI can handle opening a CSV file, converting
 * it, and either saving it to a new XML document or importing
 * it to Protege.
 * <p>
 * The Wizard can also guide a user through modifying parameters
 * of the CSV data before importing. For example, the RDF:type
 * of each row can be specified once and will be applied to
 * each line as imported.
 */
package csvtordf.main;

// Java imports

import java.io.*;
import java.util.concurrent.*;
import java.util.*;
import java.lang.Math;
import java.lang.Boolean;
import java.lang.Exception;

// Java GUI
import javafx.concurrent.Task;
import javafx.collections.FXCollections;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.Group;
import javafx.scene.control.*;
import javafx.scene.control.Alert.*;
import javafx.scene.control.ButtonType.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.event.*;

// Jena Imports
import org.apache.jena.ontology.OntModel;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.*;
import org.apache.jena.util.iterator.ExtendedIterator;

// Protege Imports
import org.semanticweb.owlapi.rdf.turtle.parser.TurtleParser;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.ChangeApplied;
import org.semanticweb.owlapi.rdf.rdfxml.parser.OWLRDFConsumer;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.protege.editor.owl.model.*;
import org.protege.editor.owl.ui.action.ProtegeOWLAction;

/**
 * Main class for launching CSV conversion application as a JavaFX GUI.
 */
public class CsvWizard extends Application {

    private static final String LEFT_PANE_TITLE = "Options";
    private static final String xsd[] = {"float", "double", "int", "long", "short", "byte", "unsignedByte", "unsignedShort",
            "unsignedInt", "unsignedLong", "decimal", "integer", "nonPositiveInteger",
            "nonNegativeInteger", "positiveInteger", "negativeInteger", "Boolean", "string",
            "normalizedString", "anyURI", "token", "Name", "QName", "language", "NMTOKEN", "ENTITIES",
            "NMTOKENS", "ENTITY", "ID", "NCName", "IDREF", "IDREFS", "NOTATION", "hexBinary",
            "base64Binary", "date", "time", "dateTime", "duration", "gDay", "gMonth", "gYear",
            "gYearMonth", "gMonthDay:"};
    private static final int DEFAULT_NUMBER_OF_THREADS = 1;
    //The maximum amount of threads they should be able to run
    int processors = Runtime.getRuntime().availableProcessors();
    private String selectedFilePath;
    private CheckBox interactiveCheckBox;
    private int numberOfThreads = DEFAULT_NUMBER_OF_THREADS;
    private Slider multithreadingSlider = new Slider(1, processors, DEFAULT_NUMBER_OF_THREADS);
    private Boolean modelLoaded = false;
    private VBox centerPane;
    private Button saveButton;
    private Label execTimeLabel;
    private ScrollPane scrollPane = new ScrollPane();
    public static OWLModelManager modelManager;
    public boolean runAsPlugin = false;
    private String selectedFileName;
    private CsvToRdf csvHandler = new CsvToRdf();
    private Image iconImage = new Image(CsvWizard.class.getResourceAsStream("icon.png"));

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
        stage.getIcons().add(iconImage);
        Screen screen = Screen.getPrimary();
        Rectangle2D bounds = screen.getVisualBounds();

        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());
        stage.show();


    }

    /**
     * Build top bar of GUI window, which is used to select the file to import.
     * This calls CsvToRdf on the button press to handle the conversion.
     *
     * @return HBox - Horizontal Box for top of GUI
     */
    private HBox buildTopBar() {
        Label l = new Label("Prefix:");
        l.setId("prefix-label");
        l.setAlignment(Pos.CENTER_RIGHT);
        l.setPadding(new Insets(5, 5, 5, 5));

        Label currentFile = new Label();
        currentFile.setId("prefix-label");
        currentFile.setAlignment(Pos.CENTER_RIGHT);
        currentFile.setPadding(new Insets(5, 5, 5, 5));

        TextField prefixField = new TextField(csvHandler.getPrefix());
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
                csvHandler.clearModel(); // clear out any previous
                csvHandler.setPrefix(prefixField.getText());
                boolean setupFailed = false;
                if (!setupModelProperties()) {
                    setupFailed = true;
                }
                if (!setupFailed && !modelLoaded) {
                    // setup was safely cancelled, but csvHandler previous model was still cleared.
                    // need to clear screen.
                    viewModel();
                    return;
                } else if (!setupFailed) {
                    // load rest of model
                    modelLoaded = csvHandler.readInputFile(selectedFilePath, numberOfThreads);
                } else {
                    // error already displayed, clear screen and exit
                    modelLoaded = false;
                    viewModel();
                    return;
                }

                if (!modelLoaded) {
                    // reading rest of model failed
                    Alert errorAlert = new Alert(AlertType.ERROR);
                    errorAlert.setHeaderText("CSV conversion error");
                    errorAlert.setContentText(csvHandler.getLastErrorMsg());
                    errorAlert.showAndWait();
                }

                // Will either display model or clear window
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
                if (selectedFile != null) {
                    selectedFileName = selectedFile.getName().replaceFirst("[.][^.]+$", "");
                    selectedFilePath = selectedFile.getPath();
                    System.out.println(selectedFilePath);
                    currentFile.setText("CSV File: " + selectedFilePath);
                    submit.setVisible(true);
                }
            }
        });


        HBox hbox = new HBox();
        hbox.setPadding(new Insets(15, 12, 15, 12));
        hbox.setSpacing(10);
        hbox.setStyle("-fx-background-color: #595959;");


        hbox.getChildren().addAll(l, prefixField, openfile, currentFile, submit);

        return hbox;
    }

    /**
     * Build left pane of GUI window, which is used to select the number of threads
     * and save the RDF generated.
     *
     * @return VBox - Vertical Box for left of GUI
     */
    private VBox buildLeftPane() {
        VBox leftPane = new VBox();
        leftPane.setPadding(new Insets(10));
        leftPane.setSpacing(16);
        leftPane.setId("left-pane");

        Label leftPaneTitle = new Label(LEFT_PANE_TITLE);
        leftPaneTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        leftPane.getChildren().add(leftPaneTitle);

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

        execTimeLabel = new Label("Execution Time: N/A");
        execTimeLabel.setVisible(false);
        leftPane.getChildren().add(execTimeLabel);

        saveButton = new Button("Save RDF");
        saveButton.setVisible(false);
        saveButton.setId("save-button");
        saveButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                if (runAsPlugin) {
                    // Load into open Ontology in Protege instead of separate file
                    saveToOntology();
                } else {
                    FileChooser fileChooser = new FileChooser();
                    fileChooser.setTitle("Save " + selectedFileName + " as RDF");
                    //Specify we are saving RDF files here
                    List<String> supportedExts = Arrays.asList("*.rdf", "*.xml", "*.owl");
                    FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("RDF files (*.rdf, *.xml, *.owl)", supportedExts);
                    fileChooser.getExtensionFilters().add(extFilter);
                    //Default name for file is the input file's name
                    fileChooser.setInitialFileName(selectedFileName + ".rdf");

                    //Show save file dialog
                    File file = fileChooser.showSaveDialog(null);

                    if (file != null) {
                        csvHandler.outputModel(file.getAbsolutePath());
                    }
                }
            }
        });
        ;
        leftPane.getChildren().add(saveButton);

        return leftPane;
    }

    /**
     * Build center pane of GUI window, which is used for displaying a previous of the generated
     * RDF XML model.
     *
     * @return VBox - Vertical Box for center of GUI
     */
    private VBox buildCenterPane() {
        centerPane = new VBox();
        centerPane.setPadding(new Insets(10));
        centerPane.setSpacing(16);
        centerPane.setId("center-pane");

        Label previewLabel = new Label("Preview:");
        previewLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        centerPane.getChildren().add(previewLabel);

        scrollPane.setVisible(false);
        centerPane.getChildren().add(scrollPane);

        return centerPane;
    }

    /**
     * Save Jena model to OWL Ontology from Protege
     */
    private void saveToOntology() {
        OWLOntology actOntology = modelManager.getActiveOntology();
        OWLOntologyManager ontManager = actOntology.getOWLOntologyManager();
        OWLDataFactory owlFactory = ontManager.getOWLDataFactory();
        Model model = csvHandler.getModel();
        StmtIterator stmtIt = model.listStatements();
        System.out.println("Importing CsvToRdf data to Protege...");

        // Want to show a nice progress bar since this can take a while,
        // Setup separate Task that will be run
        final long numStmts = model.size();
        Task<Void> saveOntTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                // load statements
                long i = 0;
                Set<OWLAxiom> newAxioms = new HashSet<OWLAxiom>();
                while (stmtIt.hasNext()) {
                    if (this.isCancelled()) {
                        System.out.println("Cancelling import...");
                        return null;
                    }
                    Statement stmt = stmtIt.next();
                    // update progress bar and message
                    i++;
                    if (i < numStmts) {
                        updateMessage("Preparing: " + (int) (100 * (double) i / numStmts) + "% complete");
                    } else {
                        updateMessage("Importing...");
                    }
                    updateProgress(i, numStmts);
                    // This seems to reduce chance of deadlocking later...
                    System.err.println("Adding statement (" + i + "/" + numStmts + "): <" + stmt.getSubject().toString() + ", " + stmt.getPredicate().toString() + ", " + stmt.getObject().toString() + ">");

                    // Add subject if not in Ontology
                    OWLNamedIndividual sub = owlFactory.getOWLNamedIndividual(IRI.create(stmt.getSubject().getURI()));
                    newAxioms.add(owlFactory.getOWLDeclarationAxiom(sub));
                    Property pred = stmt.getPredicate();
                    RDFNode obj = stmt.getObject();
                    // FIXME: Make this hardcoded type check prettier...
                    if (pred.toString().equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) {
                        // Create class for object if none exists
                        OWLClass newClass = owlFactory.getOWLClass(IRI.create(obj.toString()));
                        // Add the triple assertion
                        newAxioms.add(owlFactory.getOWLClassAssertionAxiom(newClass, sub));
                    } else if (obj.isResource()) {
                        // Add predicate as ObjectProperty
                        OWLObjectProperty owlPred = owlFactory.getOWLObjectProperty(IRI.create(pred.getURI()));
                        newAxioms.add(owlFactory.getOWLDeclarationAxiom(owlPred));
                        // Add object if not in Ontology
                        OWLNamedIndividual owlObj = owlFactory.getOWLNamedIndividual(IRI.create(obj.asResource().getURI()));
                        newAxioms.add(owlFactory.getOWLDeclarationAxiom(owlObj));
                        // Add object triple assertion
                        newAxioms.add(owlFactory.getOWLObjectPropertyAssertionAxiom(owlPred, sub, owlObj));
                    } else {
                        // Add predicate as DataProperty
                        OWLDataProperty owlPred = owlFactory.getOWLDataProperty(IRI.create(pred.getURI()));
                        newAxioms.add(owlFactory.getOWLDeclarationAxiom(owlPred));
                        // Add data triple assertion
                        newAxioms.add(owlFactory.getOWLDataPropertyAssertionAxiom(owlPred, sub, owlFactory.getOWLLiteral(obj.toString(), OWL2Datatype.RDF_PLAIN_LITERAL)));
                    }
                }
                // Need to add all at once or Protege can deadlock and throw exceptions.
                // This is a BUG in Protege/OWLAPI !!
                // https://github.com/protegeproject/protege/issues/954
                ChangeApplied status = ontManager.addAxioms(actOntology, newAxioms);
                if (status == ChangeApplied.UNSUCCESSFULLY) {
                    throw new Exception("Failed to add new axioms");
                }
                System.out.println("Successfully imported!");
                return null;
            }
        };
        Stage progStage = new Stage();
        ProgressBar pBar = new ProgressBar();
        pBar.setPrefSize(300, 24);
        pBar.progressProperty().bind(saveOntTask.progressProperty());
        Label progLabel = new Label("Processing: 0%\tETA:");
        progLabel.textProperty().bind(saveOntTask.messageProperty());
        Button cancelButton = new Button("Cancel");
        cancelButton.setOnMouseClicked(event -> {
            progStage.setAlwaysOnTop(false);
            Alert confirmCancel = new Alert(AlertType.CONFIRMATION,
                    "Are you sure you want to cancel import?",
                    ButtonType.YES, ButtonType.NO);
            confirmCancel.setTitle("Cancel Import");
            Optional<ButtonType> confirmRes = confirmCancel.showAndWait();
            if (confirmRes.get() == ButtonType.YES) {
                saveOntTask.cancel();
            } else {
                progStage.setAlwaysOnTop(true);
            }
        });
        VBox layout = new VBox(10);
        layout.getChildren().setAll(progLabel, pBar, cancelButton);
        layout.setPadding(new Insets(10));
        layout.setAlignment(Pos.CENTER);
        layout.getStylesheets().add(
                getClass().getResource(
                        "CsvWizard.css"
                ).toExternalForm()
        );
        progStage.setTitle("Importing RDF Data");
        progStage.getIcons().add(iconImage);
        progStage.setScene(new Scene(layout));
        progStage.setAlwaysOnTop(true);
        progStage.setResizable(false);
        saveOntTask.setOnSucceeded(event -> {
            progStage.close();
        });
        saveOntTask.setOnCancelled(event -> {
            progStage.close();
        });
        saveOntTask.setOnFailed(event -> {
            String errMsg = saveOntTask.getException().getMessage();
            System.err.println(errMsg);
            Alert errorAlert = new Alert(AlertType.ERROR);
            errorAlert.setHeaderText("Import Error");
            errorAlert.setContentText(errMsg);
            progStage.setAlwaysOnTop(false);
            errorAlert.showAndWait();
            progStage.close();
        });

        progStage.show();
        Thread t1 = new Thread(saveOntTask);
        t1.start();
    }

    /**
     * Display the generated model in the center pane scroll window.
     */
    private void viewModel() {
        if (modelLoaded) {
            int lineLimit = 500;
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(128);
            System.err.println("Start writing");
            csvHandler.getModel().write(byteArrayOutputStream, "RDF/XML-ABBREV");
            System.err.println("End writing");
            // only output the first few lines if the file is too big
            String[] lines = byteArrayOutputStream.toString().split("\n");
            String[] linesSubset = Arrays.copyOfRange(lines, 0, Math.min(lines.length, lineLimit));
            String linesJoined = String.join("\n", linesSubset);
            if (lines.length > lineLimit) {
                linesJoined += "\n... truncated " + (lines.length - lineLimit) + " lines ...";
            }
            Label rdfText = new Label(linesJoined);
            rdfText.setId("rdf-text");

            // Set content for ScrollPane
            scrollPane.setContent(rdfText);

            // Always show vertical scroll bar
            scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);

            // Horizontal scroll bar is only displayed when needed
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

            saveButton.setVisible(true);
            execTimeLabel.setText("Execution Time: " + csvHandler.getLastExecTime() + " ms");
            execTimeLabel.setVisible(true);
            scrollPane.setVisible(true);
        } else {
            // clear window
            csvHandler.clearModel(); // Just in case anything got partially loaded.
            saveButton.setVisible(false);
            scrollPane.setVisible(false);
            execTimeLabel.setVisible(false);
        }
    }

    /**
     * Open new window to guide user through setting up special
     * attributes of the model before importing every row in the CSV file.
     *
     * @return boolean - true if Model was setup properly, false otherwise.
     */
    private boolean setupModelProperties() {
        System.out.println("SETTING UP MODEL");
        modelLoaded = false;

        // Initialize model with header line from file
        String errMsg = null;
        try {
            FileInputStream fIn = new FileInputStream(selectedFilePath);
            BufferedReader br = new BufferedReader(new InputStreamReader(fIn));
            String line = br.readLine(); // reads the first line, or nothing
            if (line == null || line.trim().length() == 0) {
                throw new Exception("File is empty: " + selectedFilePath);
            }
            String[] tokens = line.split(",");
            csvHandler.initModel(tokens);
        } catch (FileNotFoundException e) {
            errMsg = "No such file: " + selectedFilePath;
        } catch (IOException e) {
            errMsg = "Failed reading file: " + selectedFilePath;
        } catch (Exception e) {
            errMsg = e.getMessage();
        } finally {
            if (errMsg != null) {
                Alert errorAlert = new Alert(AlertType.ERROR);
                errorAlert.setHeaderText("Initialization Error");
                errorAlert.setContentText(errMsg);
                errorAlert.showAndWait();
                return false;
            }
        }

        // TODO: Set up rest of stage for augmenting data
        // Setup options should include...
        // RDF Type for each entry-
        //    When run standalone, will create new resource for this type
        //    When run through Protege, select from existing Ontology classes?
        // Option to skip header
        //    Perhaps there is a column that doesn't matter to the user, allow
        //    option to not include it. The property will already have been
        //    imported into CsvToRdf.model at this point though, will need to
        //    somehow back it out.
        // Data vs Object Property for each header
        // If Data Property -
        //    Prompt for type (string/int/date etc...)
        // If Object Property -
        //    Create as a new Resource
        //      prompt for an RDF:type of that resource. Should also as the user
        //      if they want to always create it, or reuse previously created.
        //      i.e. if the property is "Country", and multiple rows contain
        //      "France", it should be smart enough to reuse that same country.
        //    Use previous row as Resource
        //      This would require a lot of thought though to make it robust,
        //      so this is lower priority stretch goal.
        Stage setupStage = new Stage();
        setupStage.setTitle("Setting Up RDF Model");
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 10, 10, 10));
        grid.addRow(0, new Label("Skip"), new Label("Literal"), new Label("Resource"), new Label("Property"), new Label("Type"));
        ArrayList<ToggleGroup> toggleGroupList = new ArrayList<>();
        ArrayList<ComboBox<String>> textFieldList = new ArrayList<>();
        ArrayList<Property> properties = csvHandler.getProperties();
        for (int i = 0; i < properties.size(); i++) {
            Property property = properties.get(i);
            // Add row for each property
            ToggleGroup tg = new ToggleGroup();
            RadioButton r1 = new RadioButton();
            r1.setUserData("Skip");
            RadioButton r2 = new RadioButton();
            r2.setUserData("Literal");
            r2.setSelected(true);
            RadioButton r3 = new RadioButton();
            r3.setUserData("Resource");
            r1.setToggleGroup(tg);
            r2.setToggleGroup(tg);
            r3.setToggleGroup(tg);
            toggleGroupList.add(tg);
            ComboBox<String> cb = new ComboBox<String>();
            cb.getItems().addAll(xsd);
            cb.setEditable(true);
            cb.setPromptText("literal type...");
            textFieldList.add(cb);

            // Set hint depending on which radio button is selected
            r1.selectedProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> obs, Boolean wasSel, Boolean isSel) {
                    if (isSel) {
                        cb.setPromptText("");
                        cb.setDisable(true);
                    }
                }
            });
            r2.selectedProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> obs, Boolean wasSel, Boolean isSel) {
                    if (isSel) {
                        cb.getItems().addAll(xsd);
                        cb.setPromptText("literal type...");
                        cb.setDisable(false);
                    }
                }
            });
            r3.selectedProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> obs, Boolean wasSel, Boolean isSel) {
                    if (isSel) {
                        cb.getItems().clear();
                        cb.setPromptText("resource type...");
                        cb.setDisable(false);
                    }
                }
            });
            grid.addRow(i + 1, r1, r2, r3, new Label(property.toString()), cb);
        }
        ScrollPane hScrollPane = new ScrollPane();
        hScrollPane.setContent(grid);
        hScrollPane.setPannable(true);

        VBox setupVbox = new VBox();
        setupVbox.setSpacing(10);
        setupVbox.setPadding(new Insets(10));

        Label rdfTypeLabel = new Label("RDF Type:");
        rdfTypeLabel.setPadding(new Insets(5, 5, 5, 5));
        TextField rdfTypeField = new TextField(csvHandler.getRdfType());
        if (runAsPlugin) {
            // Use Protege IRI
            String iri = modelManager.getActiveOntology().getOntologyID().getOntologyIRI().get().toString();
            rdfTypeField.setText(iri + "#CsvNode");
        }
        rdfTypeField.setPrefColumnCount(30);
        setupVbox.getChildren().add(new HBox(rdfTypeLabel, rdfTypeField));

        Label objLabel = new Label("Select Object Properties:");
        setupVbox.getChildren().add(objLabel);
        setupVbox.getChildren().add(hScrollPane);

        Button continueButton = new Button("Continue");
        continueButton.setOnMouseClicked(event -> {
                    String rdfType = rdfTypeField.getText();
                    if (!rdfType.equals("")) {
                        csvHandler.setRdfType(rdfType);
                    }

                    int i = 0;
                    for (Property property : properties) {
                        String propData = toggleGroupList.get(i).getSelectedToggle().getUserData().toString();
                        String propType = textFieldList.get(i).getEditor().getText();
                        switch (propData) {
                            case "Skip":
                                csvHandler.markSkipped(property);
                                break;
                            case "Literal":
                                if (!propType.isEmpty()) {
                                    csvHandler.setDatatypes(property, true, propType);
                                }
                                break;
                            case "Resource":
                                String resourceType = createResourceWizard(property);
                                //TODO set property type as the created resource
                                break;
                            default:
                                csvHandler.setDatatypes(property, false, propType);
                                break;
                        }
                        // TODO: Handle setting Resource properties in CsvToRdf
                        System.out.println(property.toString() + " -> " + propData + " (" + propType + ")");
                        i++;
                    }

                    setupStage.close();
                    modelLoaded = true;
                }
        );
        Button cancelButton = new Button("Cancel");
        cancelButton.setOnMouseClicked(event -> {
            setupStage.close();
        });
        HBox buttonsHbox = new HBox(continueButton, cancelButton);
        buttonsHbox.setSpacing(10);
        setupVbox.getChildren().add(buttonsHbox);
        Scene setupScene = new Scene(setupVbox, 1024, 768);
        setupStage.setScene(setupScene);
        setupStage.getIcons().add(iconImage);
        setupStage.showAndWait();
        if (modelLoaded) {
            System.out.println("FINISHED SETTING UP MODEL");
        } else {
            System.out.println("CANCELLED");
        }
        return true;
    }

    /**
     * Window for creating a new resource for a given property
     *
     * @param property property referring to this resource
     */
    //TODO finish this
    private String createResourceWizard(Property property) {
        Stage resourceWizardStage = new Stage();
        resourceWizardStage.setTitle("Create New Resource");
        VBox resourceWizardVBox = new VBox();
        GridPane gridPane = new GridPane();

        Label uriLabel = new Label("Local URI: " + property.getLocalName());
        Label fullUriLabel = new Label("Full URI: " + property.getURI());
        uriLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        fullUriLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        gridPane.setPadding(new Insets(10, 10, 10, 10));
        gridPane.add(uriLabel, 0, 0, 1, 1);
        gridPane.add(fullUriLabel, 0, 0, 2, 2);

        Label rdfTypeLabel = new Label("RDF Type:");
        rdfTypeLabel.setPadding(new Insets(5, 5, 5, 5));
        TextField rdfTypeField = new TextField();
        rdfTypeField.setPrefColumnCount(30);
        gridPane.add(new HBox(rdfTypeLabel, rdfTypeField), 1, 1);

        Button continueButton = new Button("Create");
        final String[] rdfTypeFieldText = {rdfTypeField.getText()};
        continueButton.setOnMouseClicked(event -> {
            rdfTypeFieldText[0] = rdfTypeField.getText();
            resourceWizardStage.close();
        });
        gridPane.add(continueButton, 1, 1);

        gridPane.setVgap(30);
        resourceWizardVBox.getChildren().add(gridPane);
        Scene resourceWizardScene = new Scene(resourceWizardVBox, 720, 480);
        resourceWizardStage.setScene(resourceWizardScene);
        resourceWizardStage.getIcons().add(iconImage);
        resourceWizardStage.showAndWait();
        return rdfTypeFieldText[0];
    }
}

