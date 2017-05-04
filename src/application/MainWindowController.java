package application;

import java.io.File;
import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.UnsupportedEncodingException;
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
                loadFile(file);
                break;
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
        loadFile(file);
    }

    public void handleStdin() {
        String line = textFieldStdin.getText() + "\n";
        if (stdinWriter != null) {
            try {
                stdinWriter.write(new String(line.getBytes("UTF-8"), "ISO-8859-1"));
            } catch (IOException e) {
            }
        }
        textFieldStdin.clear();
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

    private void loadFile(File file) {
        try {
            String script = new String(Files.readAllBytes(file.toPath()), "UTF-8");
            textAreaScript.setText(script);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private class ExecuteScriptTask extends Task<Void> {
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
            public void flush() {
                try {
                    updateMessage(new String(sb.toString().getBytes("ISO-8859-1"), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
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
            Writer stdoutWriter = new MessageWriter();
            ScriptEngine engine = new ScriptEngineManager().getEngineByName("python");
            ScriptContext context = engine.getContext();

            stdinWriter = new PipedWriter();
            try {
                context.setReader(new PipedReader(stdinWriter));
            } catch (IOException e2) {
                // TODO Auto-generated catch block
                e2.printStackTrace();
            }
            context.setWriter(stdoutWriter);
            context.setErrorWriter(stdoutWriter);

            String script = textAreaScript.getText();
            try {
                engine.eval(script);
            } catch (ScriptException e) {
                try {
                    stdoutWriter.write(e.getMessage());
                    stdoutWriter.flush();
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
        }
    }
}
