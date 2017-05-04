package application;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.stage.FileChooser;

public class MainWindowController {
	@FXML private TextArea textAreaScript;
	@FXML private TextArea textAreaStdout;
	
	private ScriptEngine eng = new ScriptEngineManager().getEngineByName("python");
	
    public void handleDragOver(DragEvent event) {
        Dragboard db = event.getDragboard();
        if (db.hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY);
        } else {
            event.consume();
        }
    }

    public void handleDragDropped(DragEvent event) {
        Dragboard db = event.getDragboard();
        boolean success = false;
        if (db.hasFiles()) {
            success = true;
            String script = "";
            for (File file : db.getFiles()) {
    			try {
					script += new String(Files.readAllBytes(file.toPath()), "UTF-8");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
            textAreaScript.setText(script);
        }
        event.setDropCompleted(success);
        event.consume();
    }

    public void handleOpenFile() {
    	FileChooser fileChooser = new FileChooser();
    	fileChooser.setTitle("Open File");
    	fileChooser.getExtensionFilters().addAll(
    			new FileChooser.ExtensionFilter("Python script", "*.py"),
    			new FileChooser.ExtensionFilter("All files", "*.*")
    	);
    	File file = fileChooser.showOpenDialog(textAreaScript.getScene().getWindow());
    	String script;
		try {
			script = new String(Files.readAllBytes(file.toPath()), "UTF-8");
			textAreaScript.setText(script);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
    
	public void handleRun() {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                Scene scene = textAreaScript.getScene();
        		scene.setCursor(Cursor.WAIT);
        		executeScript();
        		scene.setCursor(Cursor.DEFAULT);
				return null;
            }
        };
        new Thread(task).start();
	}
	
	private void executeScript() {
		StringWriter writer = new StringWriter();
        ScriptContext context = eng.getContext();
        context.setWriter(new PrintWriter(writer));
        context.setErrorWriter(new PrintWriter(writer));
        
        String script = textAreaScript.getText();
        String stdout;
        try {
			eng.eval(script);
			stdout = decodeUtf8(writer.toString());
		} catch (ScriptException e) {
			stdout = e.getMessage();
		} catch (UnsupportedEncodingException e) {
			stdout = e.toString();
		}
        textAreaStdout.setText(stdout);
	}
	
	private String decodeUtf8(String s) throws UnsupportedEncodingException {
		return new String(s.getBytes("ISO-8859-1"), "UTF-8");
	}

}
