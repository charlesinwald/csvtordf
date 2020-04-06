/**
 * CsvToRdf.java
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

// Jena imports
import org.apache.jena.base.Sys;
import org.apache.jena.rdf.model.*;
import org.apache.jena.shared.*;

// CLI parsing
import org.apache.commons.cli.*;

class MultiThreadCsvProcessor implements Runnable {
  private final String line;
  private final int num;

  public MultiThreadCsvProcessor(String line, int num) {
    this.line = line;
    this.num = num;
  }

  @Override
  public void run() {
    // split on regex to handle commas existing within quotes for a field
    String[] tokens = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
    if (CsvToRdf.g_verbosity >= 2) System.out.println("    Res " + num + ": " + Arrays.toString(tokens));
    //The row is the subject
    // TBD: Write locks are expensive
    CsvToRdf.model.enterCriticalSection(Lock.WRITE);
    try {
      Resource instance = CsvToRdf.model.createResource(CsvToRdf.prefix + "res" + num);
      for (int i = 0; i < tokens.length; i++) {
        //ith property is the predicate
        //cell is the object
        instance.addProperty(CsvToRdf.properties.get(i), tokens[i]);
      }
    } finally {
      CsvToRdf.model.leaveCriticalSection();
    }
  }
}

public class CsvToRdf extends Object {

  // debug verbosity level
  public static int g_verbosity = 0;

  // Jena model definitions
  public static Model model;
  public static ArrayList<Property> properties = new ArrayList<>();
  public static String prefix = "http://example.org/csv#";


  static {
    org.apache.jena.atlas.logging.LogCtl.setCmdLogging();
  }

  public static void main(String[] args) {
    // Setup arguments to command line
    Options options = new Options();
    Option infile = new Option("c", "csv", true, "CSV file to convert to RDF");
    infile.setRequired(true);
    options.addOption(infile);
    options.addOption(new Option("o", "output", true, "Output RDF XML file (default: STDOUT)"));
    options.addOption(new Option("t", "threads", true, "Number of threads (default: 1)"));
    options.addOption(new Option("i", "interactive", false, "Interactively set RDF attributes"));
    options.addOption(new Option("s", "schema", true, "XML Schema to use as offset data"));
    options.addOption(new Option("v", "verbosity", true, "Verbose logging level"));
    HelpFormatter formatter = new HelpFormatter();

    // Parse arguments
    String schema = "";
    String csvfile = "";
    int threads = 1;
    String output = "STDOUT"; // technically disallows a user creating a file named "STDOUT"
    boolean interactive = false; // TODO: Save off schema output somewhere
    CommandLineParser parser = new DefaultParser();
    try {
      CommandLine line = parser.parse(options, args);
      if(line.hasOption("v")) g_verbosity = Integer.parseInt(line.getOptionValue("v"));
      if(line.hasOption("s")) schema = line.getOptionValue("s");
      if(line.hasOption("o")) output = line.getOptionValue("o");
      if(line.hasOption("t")) threads = Integer.parseInt(line.getOptionValue("t"));
      interactive = line.hasOption("i");
      csvfile = line.getOptionValue("c");
    } catch(NumberFormatException e) {
      System.err.println("Non-Integer Found! " + e.getMessage());
      formatter.printHelp("csvtordf", options);
      System.exit(2);
    } catch(ParseException e) {
      // oops
      System.err.println(e.getMessage());
      formatter.printHelp("csvtordf", options);
      System.exit(2);
    }

    if (threads <= 0) {
        System.err.println("Error: threads must be a positive integer");
        System.exit(2);
    }

    // Print application header info
    System.out.println("CSV-To-RDF");
    System.out.println("  Verbosity   : " + g_verbosity);
    System.out.println("  Interactive : " + interactive);
    System.out.println("  Schema      : " + schema);
    System.out.println("  Threads     : " + threads);
    System.out.println("  CSV File    : " + csvfile);
    System.out.println("  Output File : " + output);
    System.out.println("");

    // Will load Jena Model
    System.out.println("Reading in CSV file...");
    readInputFile(csvfile, interactive, schema, threads);

    // Will output RDF file (or stdout)
    System.out.println("Writing RDF XML to " + output + "...");
    outputModel(output);

    System.out.println("");
    System.out.println("Done!");
  }

  /**
   * Read in a CSV input file, breaking it down into strings
   *
   * @param inputFilePath - path to CSV input file relative to working directory
   * @param interactive - set whether to run interactively
   * @param schemaFilePath - path to RDF XML Schema file to augment CSV data
   * @param threads - number of threads for multithreaded parsing
   *
   */
  public static void readInputFile(String inputFilePath, boolean interactive, String schemaFilePath, int threads) {
    try {
      //Construct buffered reader from supplied command line argument of file path
      FileInputStream fIn = new FileInputStream(inputFilePath);
      BufferedReader br = new BufferedReader(new InputStreamReader(fIn));

      // Get header line (first line)
      String line = br.readLine(); // reads the first line, or nothing
      //Split header line into properties
      String[] tokens = line.split(",");
      // initialize model with properties
      if (g_verbosity >= 1) System.out.println("  Initializing model with " + tokens.length + " properties: " + Arrays.toString(tokens));
      initModel(tokens, interactive, schemaFilePath);

      // Start timer
      long startTime = System.nanoTime();

      // Create thread pool
      ExecutorService service = Executors.newFixedThreadPool(threads);

      // Pass off line to thread in pool
      // TODO: Probably better to group into say 100 lines at a time
      int num = 0;
      while ((line = br.readLine()) != null) {
        service.execute(new MultiThreadCsvProcessor(line, num));
        num++;
      }
      // Wait for completion
      service.shutdown();
      service.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

      br.close();

      // stop timer and log
      long endTime = System.nanoTime();
      long timeElapsed = endTime - startTime; // nanoseconds
      System.out.println("  Processed CSV file in " + timeElapsed/1000000 + " ms");

    } catch (FileNotFoundException e) {
      System.err.println("File not found: " + inputFilePath);
      System.exit(1);
    } catch (IOException e) {
      System.err.println("Error parsing file headers");
      System.err.println(e.getMessage());
      System.exit(1);
    } catch (InterruptedException e) {
      System.err.println("Error waiting for all threads to terminate");
      System.err.println(e.getMessage());
      System.exit(1);
    }

    if (g_verbosity >= 3) printModel();

  }

  /**
   * Create a model and create properties from CSV headers
   *
   * @param headers - first line of CSV file, split on commas
   * @param interactive - set whether to run interactively
   * @param schemaFilePath - path to schema XML relative to working directory
   *
   */
  public static void initModel(String[] headers, boolean interactive, String schemaFilePath) {
    //create an empty model
    model = ModelFactory.createDefaultModel();

    if (!schemaFilePath.equals("")) {
        // TODO: Read in schema file
    }
    if (interactive) {
        // TODO: Run wizard to create schema
        // TBD: How to handle if both schema and interactive provided?
        Application.launch(CsvWizard.class, headers);
    }

    model.setNsPrefix("csv", prefix);
    //Iterate through headers, creating them as properties to model
    for (String header : headers) {
      Property property = model.createProperty(prefix, header);
      properties.add(property);
    }

  }

  /**
   *
   * Print model for debugging purposes
   *
   */
  public static void printModel() {
    // list the statements in the Model
    StmtIterator iter = model.listStatements();

    // print out the predicate, subject and object of each statement
    while (iter.hasNext()) {
      Statement stmt      = iter.nextStatement();  // get next statement
      Resource subject   = stmt.getSubject();     // get the subject
      Property predicate = stmt.getPredicate();   // get the predicate
      RDFNode object    = stmt.getObject();      // get the object

      System.out.print(subject.toString());
      System.out.print(" " + predicate.toString() + " ");
      if (object instanceof Resource) {
        System.out.print(object.toString());
      } else {
        // object is a literal
        System.out.print(" \"" + object.toString() + "\"");
      }

      System.out.println(" .");
    }
  }

  /**
   *
   * Output Jena Model to XML file or stdout
   *
   * @params outFilePath - path to output RDF XML file or STDOUT
   *
   */
  public static void outputModel(String outFilePath) {
    try {
      if (outFilePath.equals("STDOUT")) {
        System.out.println("");
        model.write(System.out, "RDF/XML-ABBREV");
        System.out.println("");
      } else {
        File ofile = new File(outFilePath);
        FileOutputStream fostream = new FileOutputStream(ofile);
        if (!ofile.exists()) ofile.createNewFile();
        model.write(fostream, "RDF/XML-ABBREV");
        fostream.flush();
        fostream.close();
      }
    } catch (IOException e) {
      System.err.println("Error: Failed to write to " + outFilePath);
      //e.printStackTrace();
      System.exit(1);
    }
  }

}
