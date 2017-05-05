package application;

import java.io.File;
import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.Writer;
import java.nio.file.Files;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.stage.FileChooser;

public class MainWindowController {
    @FXML private TextArea textAreaScript;
    @FXML private TextArea textAreaStdout;
    @FXML private TextField textFieldStdin;

    private Thread runningThread = null;
    private PipedWriter stdinWriter = null;

    @FXML private void initialize() {
        Platform.runLater(() -> {
            ExecuteScriptTask task = new ExecuteScriptTask("print 'Jython loaded'");
            textAreaStdout.textProperty().bind(task.messageProperty());
            runningThread = new Thread(task);
            runningThread.start();
        });
    }
    
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
            for (File file : db.getFiles()) {
                try {
                    loadFile(file);
                    break;
                } catch (IOException e) {
                    // continue
                }
            }
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
        try {
            loadFile(file);
        } catch (IOException e) {
            // cancelled
        }
    }

    public void handleStdin() throws IOException {
        String line = textFieldStdin.getText() + "\n";
        if (stdinWriter != null) {
            stdinWriter.write(new String(line.getBytes("UTF-8"), "ISO-8859-1"));
        }
        textFieldStdin.clear();
    }

    public void handleRun() {
        if (runningThread != null) {
            runningThread.interrupt();
        }
        ExecuteScriptTask task = new ExecuteScriptTask(textAreaScript.getText());
        textAreaStdout.textProperty().bind(task.messageProperty());
        runningThread = new Thread(task);
        runningThread.start();
        textFieldStdin.requestFocus();
    }

    private void loadFile(File file) throws IOException {
        String script = new String(Files.readAllBytes(file.toPath()), "UTF-8");
        textAreaScript.setText(script);
    }

    private class ExecuteScriptTask extends Task<Void> {
        private String script;
        
        ExecuteScriptTask(String script) {
            this.script = script;
        }
        
        private class MessageWriter extends Writer {
            private StringBuffer sb;

            MessageWriter() {
                this.sb = new StringBuffer();
            }

            @Override
            public void write(char[] cbuf, int off, int len) throws IOException {
                sb.append(cbuf, off, len);
            }

            @Override
            public void flush() throws IOException {
                updateMessage(new String(sb.toString().getBytes("ISO-8859-1"), "UTF-8"));
            }

            @Override
            public void close() throws IOException {
            }
        }

        @Override
        protected Void call() throws Exception {
            Scene scene = textAreaScript.getScene();
            scene.setCursor(Cursor.WAIT);
            executeScript();
            scene.setCursor(Cursor.DEFAULT);
            return null;
        }

        private void executeScript() throws IOException {
            Writer stdoutWriter = new MessageWriter();
            ScriptEngine engine = new ScriptEngineManager().getEngineByName("python");
            ScriptContext context = engine.getContext();

            stdinWriter = new PipedWriter();
            context.setReader(new PipedReader(stdinWriter));
            context.setWriter(stdoutWriter);
            context.setErrorWriter(stdoutWriter);

            try {
                engine.eval(script);
            } catch (ScriptException e) {
                stdoutWriter.write(e.getMessage());
                stdoutWriter.flush();
            }

            stdinWriter = null;
        }
    }
}
