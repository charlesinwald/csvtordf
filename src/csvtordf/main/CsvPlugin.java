/**
 * CsvPlugin.java
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
import java.awt.event.ActionEvent;

// Java GUI
import javafx.application.Application;
import javax.swing.JOptionPane;

//  Protege
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.protege.editor.owl.ui.action.ProtegeOWLAction;

public class CsvPlugin extends ProtegeOWLAction {

  public void initialise() throws Exception {}

  public void dispose() throws Exception {}

  public void actionPerformed(ActionEvent event) {
    String[] args = {};
    Application.launch(CsvWizard.class, args);
  }
}
