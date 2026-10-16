package org.example.module6assigment;

import java.util.List;
import java.io.IOException;
import java.nio.file.Path;
import java.time.YearMonth;
import java.nio.file.Files;
import java.util.stream.Collectors;
import java.util.Comparator;
import java.util.stream.Stream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;


/// Pure, stateless functions for laoding and analysing weather data
///
/// Everything here is a 'static' method on a interface, so there is no
/// instance state and no explicit class. All processing uses lambdas and streams.
///
/// '''java
/// var readings = WeatherAnalyzer.loadCsv(Path.of("data.csv"));
/// var report = WeatherAnalyzer.report(readings, YearMonth.of(2023, 8));
/// System.out.println(report.toText());
/// '''
///
public interface WeatherAnalyzer {

    /// Reads a CSV file with the header `Date,Temperature,Humidity,Precipitation`.
    ///
    /// The header row, blank lines and a leading byte-order mark are ignored.
    /// Rows are returned sorted by date.
    ///
    /// ```java
    /// List<WeatherReading> readings = WeatherAnalyzer.load(Path.of("weatherdata.csv"));
    /// ```
    ///
    /// @param csv path to the CSV file
    /// @return the readings in chronological order
    /// @throws IOException              if the file cannot be read
    /// @throws IllegalArgumentException if any row is malformed

    static List<WeatherReading> loadCsv(Path csv) throws IOException {
        try (Stream<String> lines = Files.lines(csv)) {
            return lines
                    .map(line -> line.replace("\uFEFF", "").strip())
                    .filter(line -> !line.isEmpty() && !line.toLowerCase().startsWith("date"))
                    .map(WeatherReading::fromCsvLine)
                    .sorted(Comparator.comparing(WeatherReading::date))
                    .toList();
        }
    }
    /// Lists the distinct months present in the data, oldest first.
    ///
    /// @param readings the reading to inspect
    /// @return each {@link YearMonth} that has at lead one reading

    static List<YearMonth> months(List<WeatherReading> readings) {
        return readings.stream()
                .map(WeatherReading::month)
                .distinct()
                .sorted()
                .toList();
    }

    /// Average temperature for a specific month
    ///
    /// '''java
    /// WeatherAnalyzer.averageTemperature(readings, YearMonth.of(2023, 8));
    ///         .ifPresent(temp -> System.out.printlf("August: %.1f °C%n", avg));
    /// '''
    ///
    /// @param readings the readings to search
    /// @param month    the month to average
    /// @return the mean temperature, or an empty {@link OptionalDouble} if the
    ///         month has no readings
    static OptionalDouble averageTemperature(List<WeatherReading> readings, YearMonth month) {
        return readings.stream()
                .filter(r -> r.month().equals(month))
                .mapToDouble(WeatherReading::temperature)
                .average();
    }

    /// Days whose temperature is strictly above a threshold.
    ///
    /// ```java
    /// List<WeatherReading> hot = WeatherAnalyzer.daysAbove(readings, 30.0);
    /// ```
    ///
    /// @param readings  the readings to filter
    /// @param threshold the temperature in °C that a day must exceed
    /// @return the matching days in their original order
    static List<WeatherReading> daysAbove(List<WeatherReading> readings, double threshold) {
        return readings.stream()
                .filter(r -> r.temperature() > threshold)
                .toList();
    }

    /// Number of days with any precipitation.
    ///
    /// ```java
    /// long rainy = WeatherAnalyzer.rainyDayCount(readings);
    /// ```
    ///
    /// @param readings the readings to count
    /// @return how many readings have precipitation greater than zero
    static long rainyDayCount(List<WeatherReading> readings) {
        return readings.stream()
                .filter(WeatherReading::isRainy)
                .count();
    }
    /// Counts days per weather category, always in the order Hot, Warm, Mild, Cold.
    ///
    /// ```java
    /// Map<String, Long> counts = WeatherAnalyzer.categoryCounts(readings);
    /// // {Hot=12, Warm=45, Mild=20, Cold=15}
    /// ```
    ///
    /// @param readings the readings to classify

    static Map<String, Long> categoryCounts(List<WeatherReading> readings) {
        return Stream.<WeatherCategory>of(
                new WeatherCategory.Hot(), new WeatherCategory.Warm(),
                new WeatherCategory.Mild(), new WeatherCategory.Cold()
        ).collect(Collectors.toMap(
                WeatherCategory::label,
                category -> readings.stream()
                        .filter(r -> r.category().getClass().equals(category.getClass()))
                        .count(),
                (a, b) -> a,
                LinkedHashMap::new));
    }
/// Runs every analysis at once and bundles the answers.
///
/// ```java
/// var report = WeatherAnalyzer.report(readings, YearMonth.of(2023, 9), 28.0);
/// System.out.println(report.toText());
/// ```
///
/// @param readings  all loaded readings
/// @param month     the month for the average temperature
/// @param threshold the temperature in °C for the "days above" list
/// @return an immutable {@link Report}
static Report report(List<WeatherReading> readings, YearMonth month, double threshold) {
    return new Report(
            readings.size(),
            month,
            averageTemperature(readings, month),
            threshold,
            daysAbove(readings, threshold),
            rainyDayCount(readings),
            categoryCounts(readings));
}

/// The combined result of an analysis run.
///
/// @param totalReadings       how many readings were analysed
/// @param month               the month used for the average
/// @param averageTemperature  mean temperature of {@code month}, if any data exists
/// @param threshold           the "days above" threshold in °C
/// @param daysAboveThreshold  every day hotter than the threshold
/// @param rainyDays           number of days with precipitation
/// @param categoryCounts      days per weather category
    record Report(
            int totalReadings,
            YearMonth month,
            OptionalDouble averageTemperature,
            double threshold,
            List<WeatherReading> daysAboveThreshold,
            long rainyDays,
            Map<String, Long> categoryCounts) {

        /// Formats the report as plain text using a text block.
        ///
        /// ```java
        /// String text = report.toText();
        /// ```
        ///
        /// @return a multi-line, human-readable summary
        public String toText() {
            var average = averageTemperature.isPresent()
                    ? "%.1f °C".formatted(averageTemperature.getAsDouble())
                    : "no data for this month";

            var hotDays = daysAboveThreshold.isEmpty()
                    ? "    (none)"
                    : daysAboveThreshold.stream()
                      .map(r -> "    %s   %.1f °C   %s".formatted(r.date(), r.temperature(), r.category().label()))
                      .collect(Collectors.joining("\n"));

            var categories = categoryCounts.entrySet().stream()
                    .map(e -> "    %-5s %d days".formatted(e.getKey(), e.getValue()))
                    .collect(Collectors.joining("\n"));

            return """
                    WEATHER REPORT  (%d readings)
                    ==============================

                    1) Average temperature in %s
                       %s

                    2) Days above %.1f °C  (all data): %d
                    %s

                    3) Rainy days (precipitation > 0)  (all data)
                       %d

                    4) Days per category  (all data)
                    %s
                    """.formatted(
                    totalReadings,
                    month, average,
                    threshold, daysAboveThreshold.size(), hotDays,
                    rainyDays,
                    categories);
        }
    }
}





