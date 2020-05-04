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
import java.lang.Exception;
import java.util.Set;
import java.util.HashSet;
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


// TBD: How to handle multiple CSV input files? Converge all into one model?
//      Current implementation will use the headers of the first file only.
//      Any future file will either need to have identical headers,
//      or the model will need to be cleared and the new file
//      will be its own model.

/**
 *
 * Execution class for parallel processing of CSV file
 *
 */
class MultiThreadCsvProcessor implements Callable<Void> {
  private final String[] lines;
  private final String prefix;
  private final int startNum, endNum;
  private Model model;
  private final ArrayList<Property> properties;
  private final Set<Property> skipProps;

  public MultiThreadCsvProcessor(Model model, String prefix, ArrayList<Property> properties, Set<Property> skipProps,
		                 String[] lines, int startNum, int endNum) {
    this.model = model;
    this.prefix = prefix;
    this.properties = properties;
    this.skipProps = skipProps;
    this.lines = lines;
    this.startNum = startNum;
    this.endNum = endNum;
  }

  public Void call() throws Exception {
    int arrayLength = lines.length;
    // Do as much parsing outside the critical section as possible
    String[][] tokens = new String[arrayLength][];
    for (int i = startNum; i <= endNum; i++) {
      tokens[i % arrayLength] = lines[i % arrayLength].split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
      if (tokens[i % arrayLength].length < properties.size()) {
        throw new Exception("Line " + (i+1) + " too short, should contain " + properties.size() + " fields");
      } else if (tokens[i % arrayLength].length > properties.size()) {
        throw new Exception("Line " + (i+1) + " too long, should contain " + properties.size() + " fields");
      }
    }

    // Locks are expensive, but tested with having each thread maintain its own model and merge at the end,
    // and performance was the same or worse. Batching/locking had the best results.
    model.enterCriticalSection(Lock.WRITE);
    try {
      for (int i = startNum; i <= endNum; i++) {
        if (CsvToRdf.g_verbosity >= 2) System.out.println("    Res " + i + ": " + Arrays.toString(tokens[i % arrayLength]));
        //The row is the subject
        Resource instance = model.createResource(prefix + "res" + i);
        for (int j = 0; j < tokens[i % arrayLength].length; j++) {
          //jth property is the predicate
          //cell is the object
          Property property = properties.get(j);
          if (!skipProps.contains(property)) {
            instance.addProperty(property, tokens[i % arrayLength][j]);
          }
        }
      }
    } finally {
      model.leaveCriticalSection();
    }
    return null;
  }
}

/**
 *
 * Class for handling conversion from CSV file to Jena RDF Model
 *
 */
public class CsvToRdf extends Object {

  // debug
  public static int g_verbosity = 0;
  private long lastExecTime;
  private String lastErrorMsg;

  // Jena model definitions
  private Model model;
  private ArrayList<Property> properties = new ArrayList<>();
  private Set<Property> skipProps = new HashSet<Property>();
  private String prefix = "http://example.org/csv#";

  // Set once model is loaded the first time
  private boolean initialized = false;

  // How many lines to process at a time per-thread
  // TBD: Make it configurable?
  private final int BATCH_SIZE = 100;

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
    options.addOption(new Option("v", "verbosity", true, "Verbose logging level"));
    HelpFormatter formatter = new HelpFormatter();

    // Parse arguments
    String csvfile = "";
    int threads = 1;
    String output = "STDOUT"; // technically disallows a user creating a file named "STDOUT"
    CommandLineParser parser = new DefaultParser();
    try {
      CommandLine line = parser.parse(options, args);
      if(line.hasOption("v")) g_verbosity = Integer.parseInt(line.getOptionValue("v"));
      if(line.hasOption("o")) output = line.getOptionValue("o");
      if(line.hasOption("t")) threads = Integer.parseInt(line.getOptionValue("t"));
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
    System.out.println("  Threads     : " + threads);
    System.out.println("  CSV File    : " + csvfile);
    System.out.println("  Output File : " + output);
    System.out.println("");

    // Will load Jena Model
    System.out.println("Reading in CSV file...");
    CsvToRdf csvHandler = new CsvToRdf();
    if (!csvHandler.readInputFile(csvfile, threads)) {
      System.exit(1);
    }

    // Will output RDF file (or stdout)
    System.out.println("Writing RDF XML to " + output + "...");
    csvHandler.outputModel(output);

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
  public boolean readInputFile(String inputFilePath, int threads) {
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

      // Pass off batch of lines to thread in pool
      int num = 0;
      String[] linesArray = new String[BATCH_SIZE];
      ArrayList<Future> jobResults = new ArrayList<>();
      while ((line = br.readLine()) != null) {
        linesArray[num % BATCH_SIZE] = line;
        num++;
        if (num % BATCH_SIZE == 0) {
          jobResults.add(service.submit(
              new MultiThreadCsvProcessor(model, prefix, properties, skipProps,
                                          linesArray, num - BATCH_SIZE, num - 1)));
        }
      }
      // Launch any remaining
      if (num % BATCH_SIZE != 0) {
        jobResults.add(service.submit(
            new MultiThreadCsvProcessor(model, prefix, properties, skipProps,
                                        linesArray, (num / BATCH_SIZE) * BATCH_SIZE, num - 1)));
      }
      // Wait for completion
      service.shutdown();
      service.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
      br.close();
      // Check if any failed
      for (Future jobResult : jobResults) {
        jobResult.get(); // will throw exception if job threw exception
      }
 
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
    } catch (Exception e) {
      // some unknown error, don't crash
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
  public void initModel(String[] headers) {
    //create an empty model
    if (g_verbosity >= 1) System.out.println("  Initializing model with " + headers.length + " properties: " + Arrays.toString(headers));
    model = ModelFactory.createDefaultModel();
    model.setNsPrefix("csv", prefix); // use "csv" prefix for any line resource

    //Iterate through headers, creating them as properties to model
    for (String header : headers) {
      Property property = model.createProperty(prefix, cleanStr(header));
      properties.add(property);
    }

    initialized = true;
  }

  /*
   *
   * Mark a property to skip importing
   *
   * @param property - Property to skip
   *
   */
  public void markSkipped(Property property) {
    skipProps.add(property);
  }

  /**
   *
   * Clean string for XML/RDF syntax
   *
   * @param str - String to be cleaned
   *
   * return String - cleaned string
   *
   */
  private String cleanStr(String str) {
    // TBD: Does this cover all invalid characters?
    //      Should they be replaced more uniquely than underscore?
    String fmtStr = str;
    String replaceString = " &<>";
    for (int i=0; i < replaceString.length(); i++) {
      fmtStr = fmtStr.replaceAll(Character.toString(replaceString.charAt(i)), "_");
    }
    return fmtStr;
  }

  /**
   *
   * Print model for debugging purposes
   *
   */
  public void printModel() {
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
  public void outputModel(String outFilePath) {
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

  /**
   *
   * Clear Jena Model and properties to prepare for
   * a new CSV conversion
   *
   */
  public void clearModel() {
    model = ModelFactory.createDefaultModel();
    properties = new ArrayList<>();
    initialized = false;
  }

  /* Basic getters and setters */
  public void setPrefix(String p) { prefix = p; }
  public String getPrefix() { return prefix; }
  public Model getModel() { return model; }
  public ArrayList<Property> getProperties() { return properties; }
  public long getLastExecTime() { return lastExecTime; }
  public String getLastErrorMsg() { return lastErrorMsg; }
}
