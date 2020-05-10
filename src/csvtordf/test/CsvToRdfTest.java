package csvtordf.test;

import org.apache.jena.base.Sys;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.junit.Test;
import csvtordf.main.CsvToRdf;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

public class CsvToRdfTest {

    /* Basic functionality */

    @Test
    public void main() {
        System.out.println("Test");
        CsvToRdf program = new CsvToRdf();
    }

    /**
     * Tests that single threaded and multithreaded reads work, and that invalid paths gracefully fail
     */
    @Test
    public void readInputFile() {
        //Single threaded
        CsvToRdf program = new CsvToRdf();
        boolean result = program.readInputFile("samples/sample.csv",1);
        assertTrue(result);

        //Multithreaded
        int processors = Runtime.getRuntime().availableProcessors();
        boolean result2 = program.readInputFile("samples/sample.csv", processors);
        assertTrue(result2);

        //Invalid paths should gracefully fail
        System.err.println("In the terminal, the line below this should say \"File not found: invalidpath\"");
        boolean result3 = program.readInputFile("invalidpath", processors);
        assertFalse(result3);
    }

    @Test
    public void initModel() {
        CsvToRdf program = new CsvToRdf();
        program.initModel(new String[]{"header1","header2"});
        assertNotNull(program.getModel());
    }

    /**
     * Tests that a) unmarked properties don't skip b) marked properties skip c) clear model ensures that the markings
     * are reset when the model is cleared
     */
    @Test
    public void markSkipped() {
        CsvToRdf program = new CsvToRdf();
        //First we want to read it in without marking it to skip so we can ensure unskipped properties aren't skipped
        int processors = Runtime.getRuntime().availableProcessors();
        program.readInputFile("samples/sample.csv", processors);
        ArrayList<Property> properties = program.getProperties();
        Property propertyToSkip = properties.get(1);
        System.err.println(propertyToSkip);

        File file = new File("testOutputFile");
        try {
            boolean result = Files.deleteIfExists(file.toPath());
        } catch (IOException e) {
        }
        assertFalse(Files.exists(Paths.get("testOutputFile")));
        program.outputModel("testOutputFile");
        assertTrue(Files.exists(Paths.get("testOutputFile")));
        final Scanner scanner;
        boolean containsProperty = false;
        try {
            scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                final String lineFromFile = scanner.nextLine();
                if(lineFromFile.contains("Code")) {
                    containsProperty = true;
                    break;
                }
            }
            //Assert that unskipped properties weren't skipped
            assertTrue(containsProperty);
        } catch (FileNotFoundException e) {
            fail();
        }
        program.clearModel();
        //Then we want to make sure markSkipped actually skips
        program.markSkipped(propertyToSkip);
        program.readInputFile("samples/sample.csv", processors);
        try {
            boolean result = Files.deleteIfExists(file.toPath());
        } catch (IOException e) {
        }
        assertFalse(Files.exists(Paths.get("testOutputFile")));
        program.outputModel("testOutputFile");
        assertTrue(Files.exists(Paths.get("testOutputFile")));
        containsProperty = false;
        try {
            final Scanner scanner2 = new Scanner(file);
            while (scanner2.hasNextLine()) {
                final String lineFromFile = scanner2.nextLine();
                if(lineFromFile.contains("Code")) {
                    containsProperty = false;
                    break;
                }
            }
            //Assert that unskipped properties were skipped
            assertFalse(containsProperty);
        } catch (FileNotFoundException e) {
            fail();
        }

    }

    @Test
    public void printModel() {
        CsvToRdf program = new CsvToRdf();
        program.readInputFile("samples/sample.csv",1);
        program.printModel();
    }

    /**
     * Tests that the model can be outputted to STDOUT or input files
     */
    @Test
    public void outputModel() {
        CsvToRdf program = new CsvToRdf();
        program.readInputFile("samples/sample.csv",1);

        File file = new File("testOutputFile");
        try {
            boolean result = Files.deleteIfExists(file.toPath());
        } catch (IOException e) {

        }
        assertFalse(Files.exists(Paths.get("testOutputFile")));


        program.outputModel("STDOUT");
        program.outputModel("testOutputFile");
        assertTrue(Files.exists(Paths.get("testOutputFile")));
    }

    /**
     * Tests that clearing the model removes all statements
     * Note: testing that skipped properties are skipped are tested for in markSkipped()
     */
    @Test
    public void clearModel() {
        CsvToRdf program = new CsvToRdf();
        program.readInputFile("samples/sample.csv",1);
        assertNotNull(program.getModel());
        StmtIterator iter = program.getModel().listStatements();
        assertTrue(iter.hasNext());
        program.clearModel();
        StmtIterator iter2 = program.getModel().listStatements();
        assertFalse(iter2.hasNext());
    }

    /**
     * Tests both setPrefix and getPrefix
     */
    @Test
    public void setPrefix() {
        CsvToRdf program = new CsvToRdf();
        program.readInputFile("samples/sample.csv",1);
        program.setPrefix("test-prefix");
        assertEquals(program.getPrefix(),"test-prefix");
    }


    @Test
    public void getModel() {
        CsvToRdf program = new CsvToRdf();
        program.readInputFile("samples/sample.csv",1);
        assertNotNull(program.getModel());
    }

    @Test
    public void getProperties() {
        CsvToRdf program = new CsvToRdf();
        program.readInputFile("samples/sample.csv", 1);
        ArrayList<Property> properties = program.getProperties();
        assertEquals(properties.get(0).getLocalName(), "Name");
        assertEquals(properties.get(1).getLocalName(), "Code");
    }

    @Test
    public void getLastExecTime() {
        CsvToRdf program = new CsvToRdf();
        program.readInputFile("samples/sample.csv", 1);
        assertTrue(program.getLastExecTime() > 0);
    }

    @Test
    public void getLastErrorMsg() {
    }

    /* Tests relating to our "stretch goal" features */

    @Test
    public void initModelWithMaliciousInput() {

    }

    @Test
    public void readAugmentationMetadata() {

    }

    @Test
    public void initModelUsingMetadata() {

    }

    @Test
    public void loadOntology() {

    }

    @Test
    public void initModelUsingOntologyMetadata() {

    }


}