package csvtordf.test;

import org.apache.jena.base.Sys;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.junit.Test;
import csvtordf.main.CsvToRdf;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

public class CsvToRdfTest {

    /* Basic functionality */

    @Test
    public void main() {
        System.out.println("Test");
        CsvToRdf program = new CsvToRdf();
    }

    @Test
    public void readInputFile() {
        //Single threaded
        CsvToRdf program = new CsvToRdf();
        boolean result = program.readInputFile("sample",1);
        assertTrue(result);

        //Multithreaded
        int processors = Runtime.getRuntime().availableProcessors();
        boolean result2 = program.readInputFile("sample", processors);
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

    @Test
    public void markSkipped() {
        CsvToRdf program = new CsvToRdf();
        program.initModel(new String[]{"header1","header2"});

    }

    @Test
    public void printModel() {
        CsvToRdf program = new CsvToRdf();
        program.readInputFile("sample",1);
        program.printModel();
    }

    @Test
    public void outputModel() {
        CsvToRdf program = new CsvToRdf();
        program.readInputFile("sample",1);

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

    @Test
    public void clearModel() {
        CsvToRdf program = new CsvToRdf();
        program.readInputFile("sample",1);
        assertNotNull(program.getModel());
        StmtIterator iter = program.getModel().listStatements();
        assertTrue(iter.hasNext());
        program.clearModel();
        StmtIterator iter2 = program.getModel().listStatements();
        assertFalse(iter2.hasNext());
    }

    @Test
    public void setPrefix() {
        CsvToRdf program = new CsvToRdf();
        program.readInputFile("sample",1);
        program.setPrefix("test-prefix");
        assertEquals(program.getPrefix(),"test-prefix");
    }


    @Test
    public void getModel() {
        CsvToRdf program = new CsvToRdf();
        program.readInputFile("sample",1);
        assertNotNull(program.getModel());
    }

    @Test
    public void getProperties() {
    }

    @Test
    public void getLastExecTime() {
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