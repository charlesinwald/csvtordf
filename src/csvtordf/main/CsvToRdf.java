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


// TBD: How to handle multiple CSV input files?
//      Converge all into one model?
//      For now only support one at a time.


class MultiThreadCsvProcessor implements Runnable {
  private final String[] lines;
  private final int startNum, endNum;

  public MultiThreadCsvProcessor(String[] lines, int startNum, int endNum) {
    this.lines = lines;
    this.startNum = startNum;
    this.endNum = endNum;
  }

  @Override
  public void run() {
    int arrayLength = lines.length;
    // Do as much parsing outside the critical section as possible
    String[][] tokens = new String[arrayLength][];
    for (int i = startNum; i <= endNum; i++) {
      tokens[i % arrayLength] = lines[i % arrayLength].split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
    }

    // Acquire lock once for entire batch
    CsvToRdf.model.enterCriticalSection(Lock.WRITE);
    try {
      for (int i = startNum; i <= endNum; i++) {
        if (CsvToRdf.g_verbosity >= 2) System.out.println("    Res " + i + ": " + Arrays.toString(tokens[i % arrayLength]));
        //The row is the subject
        // TBD: Write locks are expensive
        Resource instance = CsvToRdf.model.createResource(CsvToRdf.prefix + "res" + i);
        for (int j = 0; j < tokens[i % arrayLength].length; j++) {
          //jth property is the predicate
          //cell is the object
          instance.addProperty(CsvToRdf.properties.get(j), tokens[i % arrayLength][j]);
        }
      }
    } finally {
      CsvToRdf.model.leaveCriticalSection();
    }
  }
}

public class CsvToRdf extends Object {

  // debug
  public static int g_verbosity = 0;
  private static long lastExecTime;
  private static String lastErrorMsg;

  // Jena model definitions
  public static Model model;
  public static ArrayList<Property> properties = new ArrayList<>();
  public static String prefix = "http://example.org/csv#";

  // Set once model is loaded the first time
  private static boolean initialized = false;

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
    //options.addOption(new Option("s", "schema", true, "XML Schema to use as offset data"));
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
    //System.out.println("  Schema      : " + schema);
    System.out.println("  Threads     : " + threads);
    System.out.println("  CSV File    : " + csvfile);
    System.out.println("  Output File : " + output);
    System.out.println("");

    // Will load Jena Model
    System.out.println("Reading in CSV file...");
    if (!readInputFile(csvfile, threads)) {
      System.exit(1);
    }

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
   * @param threads - number of threads for multithreaded parsing
   *
   * return boolean - true if successful, false otherwise.
   */
  public static boolean readInputFile(String inputFilePath, int threads) {
    try {
      //Construct buffered reader from supplied command line argument of file path
      FileInputStream fIn = new FileInputStream(inputFilePath);
      BufferedReader br = new BufferedReader(new InputStreamReader(fIn));

      // Get header line (first line)
      String line = br.readLine(); // reads the first line, or nothing
      if (!initialized) {
        //Split header line into properties
        String[] tokens = line.split(",");
        // initialize model with properties
        initModel(tokens);
      }

      // Start timer
      long startTime = System.nanoTime();

      // Create thread pool
      ExecutorService service = Executors.newFixedThreadPool(threads);

      // Pass off line to thread in pool
      // TODO: Play around with the batch size/make it configurable?
      int batchSize = 100;
      int num = 0;
      String[] linesArray = new String[batchSize];
      while ((line = br.readLine()) != null) {
        linesArray[num % batchSize] = line;
	num++;
	if (num % batchSize == 0) {
	  service.execute(new MultiThreadCsvProcessor(linesArray, num - batchSize, num - 1));
	}
      }
      // Launch any remaining
      if (num % batchSize != 0) {
        service.execute(new MultiThreadCsvProcessor(linesArray, (num / batchSize) * batchSize, num - 1));
      }

      // Wait for completion
      service.shutdown();
      service.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

      br.close();

      // stop timer and log
      long endTime = System.nanoTime();
      lastExecTime = (endTime - startTime) / 1000000; // milliseconds
      System.out.println("  Processed CSV file in " + lastExecTime + " ms");

    } catch (FileNotFoundException e) {
      lastErrorMsg = "File not found: " + inputFilePath;
      System.err.println(lastErrorMsg);
      return false;
    } catch (IOException e) {
      lastErrorMsg = e.getMessage();
      System.err.println(lastErrorMsg);
      return false;
    } catch (InterruptedException e) {
      lastErrorMsg = e.getMessage();
      System.err.println(lastErrorMsg);
      return false;
    }

    if (g_verbosity >= 3) printModel();
    return true;
  }

  /**
   * Create a model and create properties from CSV headers
   *
   * @param headers - first line of CSV file, split on commas
   *
   */
  public static void initModel(String[] headers) {
    //create an empty model
    if (g_verbosity >= 1) System.out.println("  Initializing model with " + headers.length + " properties: " + Arrays.toString(headers));
    model = ModelFactory.createDefaultModel();
    model.setNsPrefix("csv", prefix);

    //Iterate through headers, creating them as properties to model
    for (String header : headers) {
      // TODO: check for valid XML element name syntax
      // It appears Jena will escape invalid characters in literals, but certain malicious headers currently crash
      // the plugin
      Property property = model.createProperty(prefix, header);
      properties.add(property);
    }

    initialized = true;
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

  /* Basic getters and setters */
  public static long getLastExecTime() { return lastExecTime; }
  public static String getLastErrorMsg() { return lastErrorMsg; }
}
