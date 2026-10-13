package org.example.module6assigment;

/// A description of how warm a day was
///
/// This is a *sealed interface* whose only implementations are four
/// records nested below, so a 'switch' over a 'Weather Category' is checked
/// for exhaustivness by the compiler.
///
/// | Category | Temperature *C |
/// | -------- | ---------------|
/// | 'Hot'    | 30 and above   |
/// | 'Warm'   | 20 to 30       |
/// | 'Cool'   | 10 to 20       |
/// | 'Cold'   | below 10       |
///

public sealed interface WeatherCategory {
    /// 30 ⁰C or above
    record Hot() implements WeatherCategory {
    }

    /// From 20 ⁰C up to 30 ⁰C
    record Warm() implements WeatherCategory {
    }

    /// From 10 ⁰C up to 20 ⁰C
    record Mild() implements WeatherCategory {
    }

    /// Below 10 ⁰C
    record Cold() implements WeatherCategory {
    }

    /// Classifies a temperature using enchanced 'switch' with guarded
    /// type patterns
    ///
    /// ''''java
    /// WeatherCategory.of(32.5); // Hot[]
    /// WeatherCategory.of(24.0); // Warm[]
    /// WeatherCategory.of(3.0); // Cool[]
    ///''''
    ///
    /// @param celsius the temperature in degrees celsius
    /// @return the matching category, never 'null'
    ///
    static WeatherCategory of(double celsius) {
        Double boxed = celsius;
        return switch (boxed) {
            case Double d when d >= 30.0 -> new Hot();
            case Double d when d >= 20.0 -> new Warm();
            case Double d when d >= 10.0 -> new Mild();
            default -> new Cold();
        };

    }

    /// Returns a human-readbable name using *record patterns* in an
    /// exhaustive 'switch'. Adding a fifth record to this interface without
    /// handling it here is a compile error
    ///
    /// '''java
    /// new WeatherCategory.Hot().name(); // "Hot"
    /// '''
    ///
    /// @return 'Hot', 'Warm', 'Mild' or 'Cold'
    default String label() {
        return switch (this) {
            case Hot() -> "Hot";
            case Warm() -> "Warm";
            case Mild() -> "Mild";
            case Cold() -> "Cold";
        };
    }
}

