package sql.ide.controllers;

import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.ExecutionException;
// import java.util.logging.Logger;
import java.util.stream.Stream;
// import static java.util.logging.Level.SEVERE;

//? Import everything related to database classes
import edu.upvictoria.fpoo.*;
import java.util.List;

public class FileEditorController {
    // declare a "global" interpreter
    Interpreter interpreter = new Interpreter();

    private File loadedFileReference;
    private FileTime lastModifiedTime;
    @FXML
    private TreeView<String> treeView;

    public Label statusMessage;
    public ProgressBar progressBar;
    public Button loadChangesButton;
    public TextArea textArea;
    public Label feedback;
    public Path folder;

    public void initialize() {
        loadChangesButton.setVisible(false); // hide load changes button
        textArea.setPromptText("SQL code goes here..."); // placeholder text
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
                textArea.setText(loadFileTask.get());
                statusMessage.setText("File loaded: " + fileToLoad.getName());
                loadedFileReference = fileToLoad;
                lastModifiedTime = Files.readAttributes(fileToLoad.toPath(), BasicFileAttributes.class)
                        .lastModifiedTime();
            } catch (InterruptedException | ExecutionException | IOException e) {
                // Logger.getLogger(getClass().getName()).log(SEVERE, null, e);
                textArea.setText("Could not load file from:\n " + fileToLoad.getAbsolutePath());
            }
            scheduleFileChecking(loadedFileReference);
        });
        // If unsuccessful, set text area with error message and status message to
        // failed
        loadFileTask.setOnFailed(workerStateEvent -> {
            textArea.setText("Could not load file from:\n " + fileToLoad.getAbsolutePath());
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
            myWriter.write(textArea.getText());
            myWriter.close();
            lastModifiedTime = FileTime.fromMillis(System.currentTimeMillis() + 3000);
            System.out.println("Successfully wrote to the file.");
        } catch (IOException e) {
            // Logger.getLogger(getClass().getName()).log(SEVERE, null, e);
            System.out.println("An error occurred while saving the file.");
        }
    }

    /**
     * Close file, clear text area and feedback that everything is cleared
     * 
     * @param event
     */
    public void closeFile(ActionEvent event) {
        textArea.clear();
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
        System.exit(0);
    }

    /**
     * Run selected query
     * 
     * @param event
     */
    public void runQuery(ActionEvent event) {
        // get selected text
        String selectedText = textArea.getSelectedText();

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
    }

    /**
     * Run all queries in text area
     * 
     * @param event
     */
    public void runFile(ActionEvent event) {
        // get all text
        String allText = textArea.getText();

        // verify if text is empty (no query selected)
        if (allText.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("No query selected");
            alert.setContentText("Please select a query to run.");
            alert.showAndWait();
        } else {
            System.out.println("All text: " + allText);
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
                    System.out.println();
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
    }

    /****************************************************************/
    /* Tree View related methods */
    /****************************************************************/
    /**
     * Tree view of the DataBase
     * TODO: this class should be updating all the time
     * it should show the current state of the database
     * (interpreter.getDataBase()) have the database path
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
