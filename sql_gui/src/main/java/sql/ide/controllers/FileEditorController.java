package sql.ide.controllers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;
import javafx.application.Platform;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Duration;


//* richtext imports
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

//* Import everything related to database manager
import edu.upvictoria.fpoo.*;

public class FileEditorController {
    // Database manager dependency
    Interpreter interpreter = new Interpreter();

    // File editor variables
    private File loadedFileReference;
    private FileTime lastModifiedTime;

    @FXML
    private TreeView<String> treeView;

    @FXML
    private TextArea resultArea = new TextArea();

    @FXML
    private CodeArea codeArea = new CodeArea();

    public Label statusMessage;
    public ProgressBar progressBar;
    public Button loadChangesButton;
    //// public TextArea textArea;  // deprecated bc of codeArea
    public Label feedback;

    /**
     * Thread to lex and highlight the text area
     */
    Thread lexerThread = new Thread(() -> {
        while (true) {
            try {
                Thread.sleep(500);
                String text = codeArea.getText();
                Platform.runLater(() -> {
                    // Platform.runLater(() -> {}) se utiliza para ejecutar una 
                    // tarea en el hilo de aplicación de JavaFX 
                    // en un momento futuro no especificado
                    StyleSpans<Collection<String>> spans = computeHighlighting(text);
                    if (spans != null)
                        codeArea.setStyleSpans(0, spans);
                });
            } catch (Exception e) {
                e.printStackTrace();
            } catch (Error e) {
                e.printStackTrace();
            }
        }
    });

    /**
     * Compute the highlighting for the text area
     * 
     * @param text
     * @return
     */
    private StyleSpans<Collection<String>> computeHighlighting(String text) {
        Lexer lexer = new Lexer(text);
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        int lastPos = 0;
        List<Token> tokens = null;
        
        try {
            // scan tokens
            tokens = lexer.scanTokens();
        } catch (Error e) {
            // if the lexer returns an error, show it to the user
            feedback.setText(e.getMessage());
            return null;
        }

        // clear the issue field
        feedback.setText("");


        for (Token token : tokens) {
            String style = switch (token.type) {
                case NUMBER_DATA_TYPE, BOOLEAN_DATA_TYPE, DATE_DATA_TYPE, STRING_DATA_TYPE -> "data-type";
                case CREATE, DROP, USE -> "ddl";
                case SELECT, INSERT, UPDATE, DELETE -> "dml";
                case WHERE, FROM, ORDER, BY, LIMIT, VALUES, INTO, AND, OR, NOT, NULL, TRUE, FALSE, PRIMARY, KEY,
                        DATABASE, TABLE, ASC, DESC, SET, UNIQUE, AS, GROUP, IS, PIPE_PIPE -> "keyword";
                case NUMBER, STRING, IDENTIFIER -> "literal";
                case LEFT_PAREN, RIGHT_PAREN, COMMA, MINUS, PLUS, SLASH, STAR, SEMICOLON, MOD, DIV, UCASE, LCASE,
                        CAPITALIZE, FLOOR, ROUND, RAND, COUNT, DISTINCT, MIN, MAX, SUM, AVG, CEIL -> "operator";
                case BANG_EQUAL, BANG, EQUAL_EQUAL, EQUAL, PORCENTAJE, LESS_EQUAL, LESS, GREATER_EQUAL, GREATER -> "operator";
                case SHOW, TABLES -> "keyword";
                default -> "default";
            };

            if (token.start > lastPos) {
                spansBuilder.add(Collections.emptyList(), token.start - lastPos);
            }

            spansBuilder.add(Collections.singleton(style), token.end - token.start);
            lastPos = token.end;
        }

        return spansBuilder.create();
    }

    public void initialize() {
        loadChangesButton.setVisible(false); // hide load changes button
        // codeArea.setPromptText("SQL code goes here..."); // placeholder text
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea)); // line numbers
        // codeArea.replaceText("\n");
        // todo: place holder for codearea
        lexerThread.setDaemon(true);
        lexerThread.start(); // set thread as daemon
    }

    /**
     * Open file chooser dialog to select file to open
     * 
     * @param event
     */
    public void openFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        // only allow text files to be selected using chooser
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("SQL Files", "*.sql", "*.txt"));
        // set initial directory somewhere user will recognise
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        // let user select file
        File fileToLoad = fileChooser.showOpenDialog(null);
        // if file has been chosen, load it using asynchronous method (define later)
        if (fileToLoad != null) {
            loadFileToTextArea(fileToLoad);
        }
    }

    /**
     * Load file to text area
     * 
     * @param fileToLoad
     */
    private void loadFileToTextArea(File fileToLoad) {
        Task<String> loadTask = fileLoaderTask(fileToLoad);
        progressBar.progressProperty().bind(loadTask.progressProperty());
        loadTask.run();
    }

    /**
     * Load file to text area asynchronously
     * 
     * @param fileToLoad
     * @return
     */
    private Task<String> fileLoaderTask(File fileToLoad) {
        // Create a task to load the file asynchronously
        Task<String> loadFileTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                BufferedReader reader = new BufferedReader(new FileReader(fileToLoad));
                // Use Files.lines() to calculate total lines - used for progress
                long lineCount;
                try (Stream<String> stream = Files.lines(fileToLoad.toPath())) {
                    lineCount = stream.count();
                }
                // Load in all lines one by one into a StringBuilder separated by "\n" -
                // compatible with TextArea
                String line;
                StringBuilder totalFile = new StringBuilder();
                long linesLoaded = 0;
                while ((line = reader.readLine()) != null) {
                    totalFile.append(line);
                    totalFile.append("\n");
                    updateProgress(++linesLoaded, lineCount);
                }
                return totalFile.toString();
            }
        };
        // If successful, update the text area, display a success message and store the
        // loaded file reference
        loadFileTask.setOnSucceeded(workerStateEvent -> {
            try {
                // textArea.setText(loadFileTask.get());
                codeArea.replaceText(loadFileTask.get());
                statusMessage.setText("File loaded: " + fileToLoad.getName());
                loadedFileReference = fileToLoad;
                lastModifiedTime = Files.readAttributes(fileToLoad.toPath(), BasicFileAttributes.class)
                        .lastModifiedTime();
            } catch (InterruptedException | ExecutionException | IOException e) {
                // Logger.getLogger(getClass().getName()).log(SEVERE, null, e);
                // textArea.setText("Could not load file from:\n " +
                // fileToLoad.getAbsolutePath());
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("An error occurred");
                alert.setContentText("Could not load file from:\n " + fileToLoad.getAbsolutePath());
                alert.showAndWait();

                codeArea.clear();
            }
            scheduleFileChecking(loadedFileReference);
        });
        // If unsuccessful, set text area with error message and status message to
        // failed
        loadFileTask.setOnFailed(workerStateEvent -> {
            // textArea.setText("Could not load file from:\n " +
            // fileToLoad.getAbsolutePath());
            codeArea.clear();
            statusMessage.setText("Failed to load file");
        });
        return loadFileTask;
    }

    /**
     * Schedule file checking service, to check for changes in file
     * 
     * @param file
     */
    private void scheduleFileChecking(File file) {
        ScheduledService<Boolean> fileChangeCheckingService = createFileChangesCheckingService(file);
        fileChangeCheckingService.setOnSucceeded(workerStateEvent -> {
            if (fileChangeCheckingService.getLastValue() == null)
                return;
            if (fileChangeCheckingService.getLastValue()) {
                // no need to keep checking
                fileChangeCheckingService.cancel();
                notifyUserOfChanges();
            }
        });
        System.out.println("Starting Checking Service...");
        fileChangeCheckingService.start();
    }

    /**
     * Create file changes checking service, to check for changes in file
     * 
     * @param file
     * @return
     */
    private ScheduledService<Boolean> createFileChangesCheckingService(File file) {
        ScheduledService<Boolean> scheduledService = new ScheduledService<>() {
            @Override
            protected Task<Boolean> createTask() {
                return new Task<>() {
                    @Override
                    protected Boolean call() throws Exception {
                        FileTime lastModifiedAsOfNow = Files.readAttributes(file.toPath(), BasicFileAttributes.class)
                                .lastModifiedTime();
                        return lastModifiedAsOfNow.compareTo(lastModifiedTime) > 0;
                    }
                };
            }
        };
        scheduledService.setPeriod(Duration.seconds(1));
        return scheduledService;
    }

    /**
     * Notify user of changes in file, activate load changes button
     */
    private void notifyUserOfChanges() {
        loadChangesButton.setVisible(true);
    }

    /**
     * Load changes from file, when user clicks load changes button
     * 
     * @param event
     */
    public void loadChanges(ActionEvent event) {
        loadFileToTextArea(loadedFileReference);
        loadChangesButton.setVisible(false);
    }

    /**
     * Save file to disk
     * 
     * @param event
     */
    public void saveFile(ActionEvent event) {
        try {
            if (loadedFileReference == null) {
                // if no file is loaded, save as new file
                FileChooser fileChooser = new FileChooser();
                fileChooser.getExtensionFilters().add(
                        new FileChooser.ExtensionFilter("SQL Files", "*.sql", "*.txt"));
                fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
                File fileToSave = fileChooser.showSaveDialog(null);
                if (fileToSave == null)
                    return;
                loadedFileReference = fileToSave;

            }
            FileWriter myWriter = new FileWriter(loadedFileReference);
            // myWriter.write(textArea.getText());
            myWriter.write(codeArea.getText());
            myWriter.close();
            lastModifiedTime = FileTime.fromMillis(System.currentTimeMillis() + 3000);
            System.out.println("Successfully wrote to the file.");
        } catch (IOException e) {
            // Logger.getLogger(getClass().getName()).log(SEVERE, null, e);
            System.out.println("An error occurred while saving the file.");
            // TODO: add alert
        }
    }

    /**
     * Close file, clear text area and feedback that everything is cleared
     * 
     * @param event
     */
    public void closeFile(ActionEvent event) {
        // textArea.clear();
        codeArea.clear();
        resultArea.clear();
        feedback.setText("Everything is cleared.");
        loadedFileReference = null;
        loadChangesButton.setVisible(false);
    }

    /**
     * Exit application
     * 
     * @param event
     */
    public void exitApplication(ActionEvent event) {
        // TODO: verify if file is saved before exiting
        // close the thread
        lexerThread.interrupt();
        System.exit(0);
    }

    /**
     * Exit application when button close is clicked
     * @param event
     */
    public void closeApplication() {
        lexerThread.interrupt();
        System.exit(0);
    }

    /**
     * Run user's selected query
     * 
     * @param event
     */
    public void runQuery(ActionEvent event) {
        // get selected text
        // String selectedText = textArea.getSelectedText();
        String selectedText = codeArea.getSelectedText();

        // verify if text is empty (no query selected)
        if (selectedText.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("No query selected");
            alert.setContentText("Please select a query to run.");
            alert.showAndWait();
        } else {
            System.out.println("Selected text: " + selectedText);
            try {
                // lexer
                Lexer lexer = new Lexer(selectedText);
                List<Token> tokens = lexer.scanTokens();

                // parser
                Parser parser = new Parser(tokens);
                List<Clause> expressions = parser.parse();

                // interpreter
                for (Clause expression : expressions) {
                    interpreter.interpret(expression);
                    resultArea.appendText("Excecuting: " + expression.accept(new AstPrinter()) + "\n");
                    resultArea.appendText(interpreter.getResult());
                    resultArea.appendText("\n-----------------\n");
                }

            } catch (Error e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("An error occurred");
                alert.setContentText(e.getMessage());
                alert.showAndWait();
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("An error occurred");
                alert.setContentText("Something went wrong.");
                alert.showAndWait();
            }

        }

        updateTree();
    }

    /**
     * Run all queries in text area
     * 
     * @param event
     */
    public void runFile(ActionEvent event) {
        // get all text
        // String allText = textArea.getText();
        String allText = codeArea.getText();

        // verify if text is empty (no query selected)
        if (allText.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("No query selected");
            alert.setContentText("Please select a query to run.");
            alert.showAndWait();
        } else {
            try {
                // lexer
                Lexer lexer = new Lexer(allText);
                List<Token> tokens = lexer.scanTokens();

                // parser
                Parser parser = new Parser(tokens);
                List<Clause> expressions = parser.parse();

                // interpreter
                for (Clause expression : expressions) {
                    System.out.println("Excecuting: " + expression.accept(new AstPrinter()));
                    interpreter.interpret(expression);
                    resultArea.appendText("Excecuting: " + expression.accept(new AstPrinter()) + "\n");
                    resultArea.appendText("-----------------\n");
                }
                resultArea.setText(interpreter.getResult());
            } catch (Error e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("An error occurred");
                alert.setContentText(e.getMessage());
                alert.showAndWait();
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("An error occurred");
                alert.setContentText("Something went wrong.");
                alert.showAndWait();
            }

        }

        updateTree();
    }

    /****************************************************************/
    /* Tree View related methods */
    /****************************************************************/
    /**
     * TODO: the tree should be updating all the time using threads
     */
    public void updateTree() {
        Path path = interpreter.getDataBase();
        if (path != null) {
            try (Stream<Path> paths = Files.walk(path, 1)) {
                // filter the paths to only get the csv files
                List<String> csvFiles = paths.filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().endsWith(".csv"))
                        .map(p -> p.getFileName().toString()).toList();

                // create a new tree item to hold the csv files
                TreeItem<String> root = new TreeItem<>(path.toString());
                root.setExpanded(true);

                // read the first line of every csv file to get the columns
                for (String csvFile : csvFiles) {
                    try (BufferedReader reader = new BufferedReader(new FileReader(path.toString() + "/" + csvFile))) {
                        String line = reader.readLine();
                        String[] columns = line.split(",");
                        TreeItem<String> item = new TreeItem<>(csvFile);
                        for (String column : columns) {
                            item.getChildren().add(new TreeItem<>(column));
                        }
                        root.getChildren().add(item);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                // create a new tree view bc the old one is immutable
                TreeView<String> newTreeView = new TreeView<>(root);

                // set the new tree view
                treeView.setRoot(newTreeView.getRoot());
                treeView.setShowRoot(true);

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            treeView.setRoot(null);
            treeView.setShowRoot(false);
        }
    }

    /***************************************************************************
     * Set DataBase Menu
     ***************************************************************************/
    public void setDatabase(ActionEvent event) {
        Path path = null;

        // open folder chooser
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Database Folder");
        File selectedDirectory = directoryChooser.showDialog(null);

        if (selectedDirectory != null) {
            path = selectedDirectory.toPath();
        }

        interpreter.setDataBase(path);

        // update tree view
        updateTree();

        // update the feedback
        feedback.setText("Database connection successful.");
    }
}
