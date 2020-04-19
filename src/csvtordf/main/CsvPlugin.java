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
import javafx.application.*;
import javafx.stage.Stage;
//import javax.swing.JOptionPane;

//  Protege
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.protege.editor.owl.ui.action.ProtegeOWLAction;

class LaunchApp implements Runnable {
  public LaunchApp() {
  }
  public void run() {
    String[] args = {"plugin"};
    Application.launch(CsvWizard.class, args);
  }
}

public class CsvPlugin extends ProtegeOWLAction {

  private boolean launched = false;

  public void initialise() throws Exception {}

  public void dispose() throws Exception {}

  public void actionPerformed(ActionEvent event) {
    String[] args = {"plugin"};
    CsvWizard.modelManager = getOWLModelManager();
    if (!launched) {
      LaunchApp launcher = new LaunchApp();
      Thread t = new Thread(launcher);
      t.start();
      launched = true;
    } else {
      Platform.runLater(new Runnable() {
        @Override public void run() {
          CsvWizard wiz = new CsvWizard();
	  wiz.runAsPlugin = true;
	  wiz.start(new Stage());
        }
      });
    }
  }
}
