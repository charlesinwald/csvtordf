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
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;

// Jena imports
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.*;
import org.apache.jena.datatypes.xsd.XSDDatatype;


public class CsvToRdf extends Object {
    static { org.apache.jena.atlas.logging.LogCtl.setCmdLogging(); }

    public static void main (String[] args) {
        System.out.println("Hello world!");
    }
}
