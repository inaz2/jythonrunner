package application;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
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

    private Thread runningThread = null;

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
                    String script = new String(Files.readAllBytes(file.toPath()), "UTF-8");
                    textAreaScript.setText(script);
                    break;
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
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
        String script;
        try {
            script = new String(Files.readAllBytes(file.toPath()), "UTF-8");
            textAreaScript.setText(script);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void handleRun() throws InterruptedException {
        if (runningThread != null) {
            runningThread.interrupt();
        }
        ExecuteScriptTask task = new ExecuteScriptTask();
        textAreaStdout.textProperty().bind(task.messageProperty());
        runningThread = new Thread(task);
        runningThread.start();
    }

    class ExecuteScriptTask extends Task<Void> {
        class MessageWriter extends Writer {
            private StringBuilder sb;

            MessageWriter() {
                this.sb = new StringBuilder();
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

        private void executeScript() {
            PrintWriter printWriter = new PrintWriter(new MessageWriter());
            ScriptEngine engine = new ScriptEngineManager().getEngineByName("python");
            ScriptContext context = engine.getContext();
            context.setWriter(printWriter);
            context.setErrorWriter(printWriter);

            String script = textAreaScript.getText();
            try {
                engine.eval(script);
            } catch (ScriptException e) {
                printWriter.println(e.getMessage());
                printWriter.flush();
            }
        }
    }
}
