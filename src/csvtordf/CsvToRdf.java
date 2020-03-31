/**
 * CsvToRdf.java
 *
 * Authors:
 *   Cody D'Ambrosio || cjd218 || cjd218@lehigh.edu
 *   Charles Inwald  || cci219 || cci219@lehigh.edu
 *   Paul Grocholske || pag314 || pag314@lehigh.edu
 *
 *
 * FIXME: Add description of application
 *
 */
package csvtordf;

// Java imports
import java.io.*;
import java.util.Arrays;
import java.util.Scanner;

// Jena imports
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.*;
import org.apache.jena.datatypes.xsd.XSDDatatype;


public class CsvToRdf extends Object {
    static { org.apache.jena.atlas.logging.LogCtl.setCmdLogging(); }

    public static void main (String[] args) {
        System.out.println("Hello world!");
        readInputFile(args[0]);
    }

  /**
   * Read in a CSV input file, breaking it down into strings
   * @param inputFilePath path to CSV input file relative to working directory
   */
  private static void readInputFile(String inputFilePath) {
    try {
      //Construct buffered reader from supplied command line argument of file path
      BufferedReader br = new BufferedReader(new FileReader(inputFilePath));
      String line = br.readLine(); // reads the first line, or nothing
      while (line != null) {
        String[] tokens = line.split("\t");
        System.out.println(Arrays.toString(tokens));
        line = br.readLine(); // this will eventually set line to null, terminating the loop
      }
    } catch (FileNotFoundException e) {
      System.err.println("Error reading file");
    } catch (IOException e) {
      System.err.println("Error parsing file");
    }
    catch (ArrayIndexOutOfBoundsException e) {
      System.err.println("No such file " + inputFilePath);
    }
  }
}
