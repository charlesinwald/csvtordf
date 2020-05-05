package csvtordf.test;

import org.apache.jena.base.Sys;
import org.junit.Test;
import csvtordf.main.CsvToRdf;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

public class CsvToRdfTest {

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
}