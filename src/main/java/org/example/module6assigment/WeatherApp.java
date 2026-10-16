package org.example.module6assigment;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.swing.*;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.function.Function;


/// Javafx end for the Weather Data Analyzer
///
/// There is no 'Application' sublcass and no controller classs. Javafx is started
/// with {@link Platform#startup(Runnable)} from the 'main' method of this interface.
/// The layout lives in 'weather-view.fxml' and 'weather.css' and the FXML nodes are
/// wired to behavior with lambdas
///
/// Run with:
/// '''
/// mvn clean javafx:run
/// '''

public interface WeatherApp {
    /// The CSV file loaded on start-up, relative to the working directory
    String DEFAULT_CSV = "weatherdata.csv";


    /// The FXML nodes this screen needs, grouped so they can be passed around as one value.
    ///
    /// @param table          shows every loaded reading
    /// @param monthBox       month chooser for the average temperature
    /// @param thresholdField the "hot day" threshold in °C
    /// @param outputArea     shows the text-block report
    /// @param statusLabel    shows which file is loaded or what went wrong
    /// @param loadButton     opens a file chooser
    /// @param analyzeButton  re-runs the analysis

    record Ui(
            TableView<WeatherReading> table,
            ComboBox<YearMonth> monthBox,
            TextField thresholdField,
            TextArea outputArea,
            Label statusLabel,
            Button loadButton,
            Button analyzeButton) {
    }


    /// Application entry point. Starts the Javafx toolkit and opens the main window
    ///
    /// @param args ignored
    static void main(String[] args) {
        Platform.startup(() -> {
            try {
                show(new Stage());
            } catch (IOException e) {
                throw new UncheckedIOException("Could not load weather-view.fxml", e);
            }
        });
    }

    /// Builds the window from FXML, connects the controls and loads the default CSV.
    ///
    /// @param stage the window to show
    /// @throws IOException if the FXML file cannot be loaded

    static void show(Stage stage) throws IOException {
        var loader = new FXMLLoader(WeatherApp.class.getResource("weather-view.fxml"));
        Parent root = loader.load();

        var ui = new Ui(
                ref(loader, "table"),
                ref(loader, "monthBox"),
                ref(loader, "thresholdField"),
                ref(loader, "outputArea"),
                ref(loader, "statusLabel"),
                ref(loader, "loadButton"),
                ref(loader, "analyzeButton"));

        ui.table().getColumns().addAll(
                column("Date", 110, r -> r.date().toString()),
                column("Temp (°C)", 100, r -> "%.1f".formatted(r.temperature())),
                column("Humidity (%)", 110, r -> String.valueOf(r.humidity())),
                column("Rain (mm)", 100, r -> "%.1f".formatted(r.precipitation())),
                categoryColumn());
        ui.analyzeButton().setOnAction(e -> analyze(ui));
        ui.monthBox().setOnAction(e -> analyze(ui));
        ui.thresholdField().setOnAction(e -> analyze(ui));
        ui.loadButton().setOnAction(e -> {
            var chooser = new FileChooser();
            chooser.setTitle("Choose a weather CSV file");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
            var file = chooser.showOpenDialog(stage);
            if (file != null) {
                load(file.toPath(), ui);
            }
        });

        stage.setTitle("Weather Data Analyzer");
        stage.setScene(new Scene(root));
        stage.show();

        load(Path.of(DEFAULT_CSV), ui);
    }

    /// Reads a CSV file on a virtual thread so the window never freezes, then
    /// fills the table and month list back on the JavaFX thread.
    ///
    /// @param path the CSV file to read
    /// @param ui   the screen to update
    static void load(Path path, Ui ui) {
        var absolute = path.toAbsolutePath();
        ui.statusLabel().setText("Loading " + absolute + " …");

        Thread.ofVirtual().start(() -> {
            try {
                var readings = WeatherAnalyzer.load(path);
                Platform.runLater(() -> {
                    ui.table().getItems().setAll(readings);
                    var months = WeatherAnalyzer.months(readings);
                    ui.monthBox().getItems().setAll(months);
                    if (!months.isEmpty()) {
                        ui.monthBox().getSelectionModel().selectFirst();
                    }
                    ui.statusLabel().setText("Loaded %d readings from %s".formatted(readings.size(), absolute));
                    analyze(ui);
                });
            } catch (IOException | RuntimeException e) {
                Platform.runLater(() -> {
                    ui.statusLabel().setText("Could not read " + absolute + ": " + e.getMessage()
                            + "  (use \"Load CSV…\" to pick a file)");
                    ui.table().getItems().clear();
                    ui.monthBox().getItems().clear();
                    ui.outputArea().clear();
                });
            }
        });
    }

    /// Runs the analysis for the selected month and threshold and shows the report.
    ///
    /// @param ui the screen to read from and write to
    static void analyze(Ui ui) {
        var readings = ui.table().getItems();
        var month = ui.monthBox().getValue();
        if (readings.isEmpty() || month == null) {
            return;
        }
        try {
            double threshold = Double.parseDouble(ui.thresholdField().getText().strip());
            ui.outputArea().setText(WeatherAnalyzer.report(readings, month, threshold).toText());
        } catch (NumberFormatException e) {
            ui.outputArea().setText("Please enter the threshold as a number, for example 30 or 28.5");
        }
    }

    /// Creates a plain text table column from a lambda.
    ///
    /// @param title the column heading
    /// @param width the preferred width in pixels
    /// @param value extracts the cell text from a reading
    /// @return a ready-to-add column
    static TableColumn<WeatherReading, String> column(String title, double width, Function<WeatherReading, String> value) {
        var column = new TableColumn<WeatherReading, String>(title);
        column.setPrefWidth(width);
        column.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        return column;
    }

    /// Creates the "Category" column, tagging each cell with a CSS style class
    /// (`hot`, `warm`, `mild` or `cold`) so `weather.css` can colour it.
    ///
    /// @return the category column
    static TableColumn<WeatherReading, String> categoryColumn() {
        var column = column("Category", 100, r -> r.category().label());
        column.setCellFactory(col -> {
            var cell = new TableCell<WeatherReading, String>();
            cell.itemProperty().addListener((observable, oldValue, newValue) -> {
                cell.setText(newValue);
                cell.getStyleClass().removeAll("hot", "warm", "mild", "cold");
                if (newValue != null) {
                    cell.getStyleClass().add(newValue.toLowerCase());
                }
            });
            return cell;
        });
        return column;
    }

    /// Looks up a node that FXML created for an `fx:id`.
    ///
    /// @param loader the loader that has already loaded the FXML
    /// @param id     the `fx:id` value
    /// @param <T>    the expected node type
    /// @return the node
    @SuppressWarnings("unchecked")
    private static <T> T ref(FXMLLoader loader, String id) {
        return (T) loader.getNamespace().get(id);
    }
}






