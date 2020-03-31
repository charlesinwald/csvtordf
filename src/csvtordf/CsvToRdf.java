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


public class CsvToRdf extends Object {

  private static Model model;
  private static ArrayList<Property> properties = new ArrayList<>();
  private static String prefix = "http://example.org/#";


  static {
    org.apache.jena.atlas.logging.LogCtl.setCmdLogging();
  }

  public static void main(String[] args) {
    readInputFile(args[0]);
  }

  /**
   * Read in a CSV input file, breaking it down into strings
   *
   * @param inputFilePath path to CSV input file relative to working directory
   */
  private static void readInputFile(String inputFilePath) {
    try {
      //Construct buffered reader from supplied command line argument of file path
      BufferedReader br = new BufferedReader(new FileReader(inputFilePath));
      String line = br.readLine(); // reads the first line, or nothing
      //Split header line into properties
      String[] tokens = line.split(",");
      initModel(tokens);
      int num = 0; //first result URI is res0, second res1, and so on
      //TODO handle edge cases of splitting on commas, i.e if string has literal comma
      while (line != null) {
        tokens = line.split(",");
        createResource(num, tokens);
        line = br.readLine(); // this will eventually set line to null, terminating the loop
        num++;
      }
    } catch (FileNotFoundException e) {
      System.err.println("Error reading file");
    } catch (IOException e) {
      System.err.println("Error parsing file");
    } catch (ArrayIndexOutOfBoundsException e) {
      System.err.println("No such file " + inputFilePath);
    }
    printModel();

  }

  /**
   * Turns a row of CSV into a resource, adding each cell as a property-
   * @param tokens the row of CSV
   */
  private static void createResource(int num, String[] tokens) {
//    System.out.println(Arrays.toString(tokens));
    //The row is the subject
    Resource instance = model.createResource("res" + num);
    for (int i = 0; i < tokens.length-1; i++) {
      //ith property is the predicate
      //cell is the object
      instance.addProperty(properties.get(i), tokens[i]);
    }
  }

  /**
   * Create a model and create properties from CSV headers
   *
   * @param headers first line of CSV file, split on commas
   */
  private static void initModel(String[] headers) {
    //create an empty model
    model = ModelFactory.createDefaultModel();
    System.out.println("Headers: " + Arrays.toString(headers));
    //Iterate through headers, creating them as properties to model
    for (String header : headers) {
      Property property = model.createProperty(header);
      properties.add(property);
    }
  }

  /**
   * Print model for debugging purposes
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
}
