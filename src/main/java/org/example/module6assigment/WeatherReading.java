package org.example.module6assigment;

import java.time.LocalDate;
import java.time.YearMonth;

/// One day of weather observations, as read from a row of the CSV file.
///
/// ```java
/// var reading = WeatherReading.fromCsvLine("2023-08-02,35.0,60,0.2");
/// reading.temperature();  // 35.0
/// reading.isRainy();      // true
/// reading.category();     // Hot[]
/// ```
///
/// @param date          the calendar day of the observation
/// @param temperature   the temperature in degrees Celsius
/// @param humidity      relative humidity as a percentage from 0 to 100
/// @param precipitation rainfall in millimetres, zero or greater
public record WeatherReading(LocalDate date, double temperature, int humidity, double precipitation) {

    /// Validates the fields so that an invalid reading can never exist.
    ///
    /// @throws IllegalArgumentException if humidity is outside 0–100 or
    ///                                  precipitation is negative
    public WeatherReading {
        if (humidity < 0 || humidity > 100) {
            throw new IllegalArgumentException("Humidity must be between 0 and 100 but was " + humidity);
        }
        if (precipitation < 0) {
            throw new IllegalArgumentException("Precipitation cannot be negative but was " + precipitation);
        }
    }

    /// Parses one CSV row in the form `Date,Temperature,Humidity,Precipitation`.
    ///
    /// ```java
    /// var reading = WeatherReading.fromCsvLine("2023-08-01,32.5,65,0.0");
    /// ```
    ///
    /// @param line a single data row (not the header)
    /// @return the parsed reading
    /// @throws IllegalArgumentException if the row does not have four columns or a
    ///                                  value cannot be parsed
    public static WeatherReading fromCsvLine(String line) {
        var parts = line.split(",");
        if (parts.length != 4) {
            throw new IllegalArgumentException(
                    "Expected 4 columns but found %d in \"%s\"".formatted(parts.length, line));
        }
        return new WeatherReading(
                LocalDate.parse(parts[0].strip()),
                Double.parseDouble(parts[1].strip()),
                (int) Math.round(Double.parseDouble(parts[2].strip())),
                Double.parseDouble(parts[3].strip()));
    }

    /// The year and month this reading belongs to.
    ///
    /// ```java
    /// reading.month();  // 2023-08
    /// ```
    ///
    /// @return the {@link YearMonth} of {@link #date()}
    public YearMonth month() {
        return YearMonth.from(date);
    }

    /// Whether any rain fell on this day.
    ///
    /// @return `true` if {@link #precipitation()} is greater than zero
    public boolean isRainy() {
        return precipitation > 0;
    }

    /// The weather category derived from this day's temperature.
    ///
    /// @return the {@link WeatherCategory} for {@link #temperature()}
    public WeatherCategory category() {
        return WeatherCategory.of(temperature);
    }
}