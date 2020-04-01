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
package csvtordf;

// Java imports

import java.io.*;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;

// Jena imports
import org.apache.jena.base.Sys;
import org.apache.jena.rdf.model.*;

// CLI parsing
import org.apache.commons.cli.*;

public class CsvToRdf extends Object {

  // debug verbosity level
  public static int g_verbosity = 0;

  // Jena model definitions
  private static Model model;
  private static ArrayList<Property> properties = new ArrayList<>();
  private static String prefix = "http://example.org/csv#";


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
    options.addOption(new Option("i", "interactive", false, "Interactively set RDF attributes"));
    options.addOption(new Option("s", "schema", true, "XML Schema to use as offset data"));
    options.addOption(new Option("v", "verbosity", true, "Verbose logging level"));
    HelpFormatter formatter = new HelpFormatter();

    // Parse arguments
    String schema = "";
    String csvfile = "";
    String output = "STDOUT"; // technically disallows a user creating a file named "STDOUT"
    boolean interactive = false; // TODO: Save off schema output somewhere
    CommandLineParser parser = new DefaultParser();
    try {
      CommandLine line = parser.parse(options, args);
      if(line.hasOption("v")) g_verbosity = Integer.parseInt(line.getOptionValue("v"));
      if(line.hasOption("s")) schema = line.getOptionValue("s");
      if(line.hasOption("o")) output = line.getOptionValue("o");
      interactive = line.hasOption("i");
      csvfile = line.getOptionValue("c");
    } catch(ParseException e) {
      // oops
      System.err.println(e.getMessage());
      formatter.printHelp("csvtordf", options);
      System.exit(2);
    }

    // Print application header info
    System.out.println("CSV-To-RDF");
    System.out.println("  Verbosity: " + g_verbosity);
    System.out.println("  Interactive: " + interactive);
    System.out.println("  Schema: " + schema);
    System.out.println("  CSV File: " + csvfile);
    System.out.println("  Output File: " + output);
    System.out.println("");

    // Will load Jena Model
    System.out.println("Reading in CSV file...");
    readInputFile(csvfile, interactive, schema);

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
   *
   */
  private static void readInputFile(String inputFilePath, boolean interactive, String schemaFilePath) {
    try {
      //Construct buffered reader from supplied command line argument of file path
      BufferedReader br = new BufferedReader(new FileReader(inputFilePath));

      // Get header line (first line)
      String line = br.readLine(); // reads the first line, or nothing
      //Split header line into properties
      String[] tokens = line.split(",");
      // initialize model with properties
      if (g_verbosity >= 1) System.out.println("  Initializing model with " + tokens.length + " properties: " + Arrays.toString(tokens));
      initModel(tokens);

      // Start gathering resources
      int num = 0; //first result URI is res0, second res1, and so on
      line = br.readLine();
      // TODO: Make this multithreaded
      if (g_verbosity >= 1) System.out.println("  Adding Resources");
      while (line != null) {
        // split on regex to handle commas existing within quotes for a field
        tokens = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
        createResource(num, tokens);
        line = br.readLine(); // this will eventually set line to null, terminating the loop
        num++;
      }
      if (g_verbosity >= 1) System.out.println("  Added " + num + " Resources");
    } catch (FileNotFoundException e) {
      System.err.println("File not found: " + inputFilePath);
      System.exit(1);
    } catch (IOException e) {
      System.err.println("Error parsing file");
      System.exit(1);
    }

    if (g_verbosity >= 3) printModel();

  }

  /**
   * Create a model and create properties from CSV headers
   *
   * @param headers - first line of CSV file, split on commas
   *
   */
  private static void initModel(String[] headers) {
    //create an empty model
    model = ModelFactory.createDefaultModel();
    model.setNsPrefix("csv", prefix);
    //Iterate through headers, creating them as properties to model
    for (String header : headers) {
      Property property = model.createProperty(prefix, header);
      properties.add(property);
    }
  }

  /**
   * Turns a row of CSV into a resource, adding each cell as a property
   *
   * @param tokens - the row of CSV
   *
   */
  private static void createResource(int num, String[] tokens) {
   if (g_verbosity >= 2) System.out.println("    Res " + num + ": " + Arrays.toString(tokens));
    //The row is the subject
    Resource instance = model.createResource(prefix + "res" + num);
    for (int i = 0; i < tokens.length; i++) {
      //ith property is the predicate
      //cell is the object
      instance.addProperty(properties.get(i), tokens[i]);
    }
  }


  /**
   *
   * Print model for debugging purposes
   *
   */
  private static void printModel() {
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
  private static void outputModel(String outFilePath) {
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
