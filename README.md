# Weather Data Analyzer

A small JavaFX application that reads daily weather data from a CSV file and answers three questions about it:

1. What was the **average temperature** in a given month?
2. Which days were **hotter than a threshold** I choose?
3. How many days were **rainy**?

It also sorts every day into a weather category (**Hot / Warm / Mild / Cold**) and shows everything in a simple window styled with CSS.

The project is the Module 6 assignment. Its main rule is that the code is written **without explicit classes**: only records, interfaces and functional constructs (lambdas and streams).

---

## Requirements

| Tool  | Version |
|-------|---------|
| JDK   | 21 or newer |
| Maven | 3.9+ (or use the included `mvnw` wrapper) |

JavaFX 21 is downloaded by Maven automatically

On start-up the app loads `weatherdata.csv` from the project root. Use the **Load CSV…** button to open any other file in the same format.

## Documentation

All types and methods are documented with Markdown Javadoc comments (`///`), including fenced code examples. Rendering them as HTML needs a JDK 23 or newer `javadoc` tool; the application itself builds and runs on JDK 21.

```bash
mvn javadoc:javadoc
```
