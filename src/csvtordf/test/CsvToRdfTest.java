package csvtordf.test;

import org.junit.Test;
import csvtordf.main.CsvToRdf;

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
    }

    @Test
    public void markSkipped() {
    }

    @Test
    public void printModel() {
    }

    @Test
    public void outputModel() {
    }

    @Test
    public void clearModel() {
    }

    @Test
    public void setPrefix() {
    }

    @Test
    public void getPrefix() {
    }

    @Test
    public void getModel() {
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