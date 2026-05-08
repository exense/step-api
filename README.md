# step-api

[![License](https://img.shields.io/badge/License-AGPL%20v3-blue.svg)](LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/ch.exense.step/step-api)](https://central.sonatype.com/artifact/ch.exense.step/step-api)

The API for writing **Keywords** in the [Step automation platform](https://step.dev) in Java and .NET. This library is the only dependency needed to implement keyword libraries that can be executed by the Step engine.

For full documentation, architecture details, and getting started guides, see the main repository:
**[github.com/exense/step](https://github.com/exense/step)**

## Overview

A **Keyword** is a named, executable unit of automation — the building block of test plans in Step. Step supports keywords implemented in multiple languages and technologies. This repository covers:

- **Java Keywords** — annotated Java methods executed on JVM-based agents (see [Writing Java Keywords](https://step.dev/knowledgebase/keywords/java))
- **.NET Keywords** — C# classes executed on .NET agents (`step-api-net/` directory)

Step also supports additional keyword types through plugins, covering JavaScript (Node.js agents), Cypress, Grafana K6, JMeter, SoapUI, QF-Test, SikuliX, and more. See the full list in the [plugin-based keywords documentation](https://step.dev/knowledgebase/userdocs/keywords/#plugin-based-keywords).

For Java keywords specifically, you implement keywords as annotated Java methods and Step handles execution, input/output mapping, error reporting, attachments, and performance measurement.

```java
import step.handlers.javahandler.AbstractKeyword;
import step.handlers.javahandler.Keyword;

public class MyKeywords extends AbstractKeyword {

    @Keyword
    public void Login() {
        String url  = input.getString("url");
        String user = input.getString("username");
        // ... automation logic ...
        output.add("sessionId", "abc123");
    }

    @Keyword(name = "Search Product", timeout = 10000)
    public void searchProduct(@Input(name = "query", defaultValue = "step") String query) {
        // ... automation logic ...
        output.add("resultCount", 42);
    }
}
```

## Modules

| Module | Artifact ID | Description |
|--------|-------------|-------------|
| Function I/O | `step-api-function` | Generic `Input<T>` and `Output<T>` containers and the `OutputBuilder` fluent API |
| Keyword | `step-api-keyword` | `@Keyword` and `@Input` annotations, `AbstractKeyword` base class, and the keyword execution engine |
| Reporting | `step-api-reporting` | `Error`, `Measure`, `Attachment`, `LiveReporting` and related types for error handling, performance measurement, and real-time streaming |
| JSON Schema | `step-api-json-schema` | Derives a JSON schema from keyword method signatures for UI rendering and input validation |

## Requirements

- Java 11 or later
- Maven 3.6 or later

## Getting Started

Add a single dependency to your keyword project:

```xml
<dependency>
  <groupId>ch.exense.step</groupId>
  <artifactId>step-api-keyword</artifactId>
  <version>VERSION</version>
</dependency>
```

Replace `VERSION` with the latest release available on [Maven Central](https://central.sonatype.com/artifact/ch.exense.step/step-api).

## Writing Keywords

### 1. Extend `AbstractKeyword`

All keyword classes extend `AbstractKeyword`, which provides the execution context:

| Field | Type | Description |
|-------|------|-------------|
| `input` | `JsonObject` | Input parameters passed by the caller |
| `output` | `OutputBuilder` | Fluent builder for the keyword's return value |
| `properties` | `Map<String, String>` | Plan- and agent-level configuration properties |
| `session` | `AbstractSession` | Plan-scoped session shared across keywords in the same run |
| `tokenSession` | `AbstractSession` | Agent-scoped session shared across keywords on the same agent |
| `liveReporting` | `LiveReporting` | Real-time file upload and metric streaming |

### 2. Annotate methods with `@Keyword`

```java
@Keyword(
    name        = "My Keyword",   // defaults to method name
    description = "Does something useful",
    timeout     = 30000,          // ms, default 180000
    properties  = {"host"},       // required agent properties
    optionalProperties = {"port"} // optional agent properties
)
public void myKeyword() { ... }
```

### 3. Declare inputs with `@Input`

Annotate method parameters to let Step map JSON inputs to typed values automatically:

```java
@Keyword
public void CreateUser(
    @Input(name = "username")                          String username,
    @Input(name = "role",    defaultValue = "viewer")  String role,
    @Input(name = "active",  required = false)         boolean active
) { ... }
```

### 4. Build the output

```java
// Add output values
output.add("userId", 42);
output.add("token", "abc123");

// Report a business error (test failure)
output.setBusinessError("User already exists");

// Report a technical error (infrastructure failure)
output.setError("Database unreachable");

// Attach a file to the output
output.addAttachment(AttachmentHelper.generateAttachmentForException(e));
```

### 5. Record performance measurements

Use `startMeasure`/`stopMeasure` to time a block of code, or `addMeasure` to record a duration you already know.

```java
// Time a block — stopMeasure takes no name; it matches the last startMeasure
output.startMeasure("db-query");
// ... timed code ...
output.stopMeasure();

// Provide an explicit start timestamp (e.g. from a timestamp captured before OutputBuilder was available)
long begin = System.currentTimeMillis();
output.startMeasure("db-query", begin);
output.stopMeasure();

// Record an already-known duration directly
output.addMeasure("external-call", 350L);                             // name + ms
output.addMeasure("external-call", 350L, startTimestamp);             // + begin
output.addMeasure("external-call", 350L, Map.of("host", "api.example.com")); // + custom data
output.addMeasure("external-call", 350L, startTimestamp, data);       // all fields

// Add a Measure object directly (e.g. forwarded from a helper class)
output.addMeasure(myMeasure);
```

All `stopMeasure` overloads set an explicit outcome status and/or attach custom data to the measurement:

```java
output.startMeasure("login");
// ...
output.stopMeasure();                                          // status left unset (inherits keyword outcome)
output.stopMeasure(Measure.Status.PASSED);                     // explicit PASSED
output.stopMeasure(Measure.Status.FAILED);                     // marks this step as failed
output.stopMeasure(Measure.Status.TECHNICAL_ERROR);            // marks as technical error
output.stopMeasure(Map.of("user", "alice"));                   // custom data, no explicit status
output.stopMeasure(Measure.Status.FAILED, Map.of("user", "alice")); // status + data
```

`Measure.Status` values: `PASSED`, `FAILED`, `TECHNICAL_ERROR`.

Measurements are returned in `Output.getMeasures()` and are visible in the Step execution report.

### 6. Emit custom metrics

Metrics track numerical values across keyword calls and are forwarded to the Step metrics pipeline. Unlike measures, metrics aggregate observations (count, sum, min, max, distribution) and support key–value labels for grouping and filtering.

Three instrument types are available, all created via `OutputBuilder` factory methods:

```java
// Counter — a monotonically increasing total (e.g. request count, error count)
CounterMetric requests = output.newCounter("requests");
requests.increment();        // +1
requests.increment(5);       // +5

// Counter with labels
CounterMetric labeledCounter = output.newCounter("requests", Map.of("service", "checkout", "env", "prod"));
labeledCounter.increment(3);

// Gauge — a value that can rise and fall (e.g. queue depth, active connections)
GaugeMetric queueDepth = output.newGauge("queue_depth");
queueDepth.observe(12);
queueDepth.observe(7);

// Gauge with labels
GaugeMetric labeledGauge = output.newGauge("queue_depth", Map.of("region", "eu"));
labeledGauge.observe(5);

// Histogram — distribution of observed values (e.g. response times, payload sizes)
HistogramMetric responseTimes = output.newHistogram("response_time_ms");
responseTimes.observe(120);
responseTimes.observe(340);

// Histogram with labels
HistogramMetric labeledHistogram = output.newHistogram("response_time_ms", Map.of("endpoint", "/login"));
labeledHistogram.observe(200);
```

If you build a metric object separately (e.g. in a helper class), register it with `addMetric` instead:

```java
CounterMetric errors = new CounterMetric("errors");
errors.increment(2);
output.addMetric(errors);
```

All registered metrics are flushed into `Output.getMetrics()` as a list of `MetricSample` snapshots when `build()` is called. Each snapshot carries the instrument type, name, labels, count, sum, min, max, last value, and a value-distribution map.

### 7. Stream live measurements

Measures recorded via `output` (section 5) are sent to Step **when the keyword finishes**. Use `liveReporting.measures` instead to dispatch each measurement **immediately** during execution, making it visible in real time without waiting for the keyword to complete.

The `liveReporting` field is inherited from `AbstractKeyword` and is wired by the framework — do not instantiate `LiveReporting` yourself.

```java
@Keyword
public void ProcessOrder() {
    liveReporting.measures.startMeasure("db-lookup");
    // ... timed code ...
    liveReporting.measures.stopMeasure();                                               // dispatched immediately, status = PASSED

    liveReporting.measures.startMeasure("payment-call");
    // ... timed code ...
    liveReporting.measures.stopMeasure(Measure.Status.FAILED);                         // explicit status

    liveReporting.measures.startMeasure("notification");
    // ... timed code ...
    liveReporting.measures.stopMeasure(Measure.Status.PASSED, Map.of("channel", "email")); // status + data
}
```

Unlike the batch `output.stopMeasure()`, the live overloads require an explicit outcome because the keyword has not finished yet — the no-argument `stopMeasure()` defaults to `PASSED`. To submit a pre-built `Measure` directly (e.g. from a helper class), the measure must carry a non-null status and name:

```java
Measure m = new Measure("external-step", 350L, startTimestamp, customData, Measure.Status.PASSED);
liveReporting.measures.addMeasure(m);
```

### 8. Stream live metrics

Metrics registered via `output` (section 6) are flushed once when the keyword completes. Use `liveReporting.metrics` to register metrics that the framework flushes **periodically during execution**, so values are visible in real time throughout long-running keywords.

```java
@Keyword
public void BulkImport() {
    // Register once — the framework flushes snapshots on its own schedule
    CounterMetric processed = liveReporting.metrics.registerCounter("records_processed");
    GaugeMetric batchSize   = liveReporting.metrics.registerGauge("batch_size");
    HistogramMetric latency  = liveReporting.metrics.registerHistogram("batch_latency_ms");

    for (Batch batch : getBatches()) {
        long t0 = System.currentTimeMillis();
        process(batch);
        processed.increment(batch.size());
        batchSize.observe(batch.size());
        latency.observe(System.currentTimeMillis() - t0);
        // No manual flush needed — the framework dispatches snapshots on its own interval
    }
}
```

Labels are supported on all three registration methods:

```java
CounterMetric errors = liveReporting.metrics.registerCounter("errors", Map.of("service", "payment"));
GaugeMetric depth    = liveReporting.metrics.registerGauge("queue_depth", Map.of("region", "eu"));
HistogramMetric rt   = liveReporting.metrics.registerHistogram("response_time_ms", Map.of("env", "prod"));
```

To register a metric you built yourself, use `register`:

```java
CounterMetric custom = new CounterMetric("retries", Map.of("type", "transient"));
liveReporting.metrics.register(custom);
```

### 9. Lifecycle hooks

Override these methods in your keyword class for setup and teardown:

```java
@Override
public void beforeKeyword(String keywordName, Annotation annotation) { ... }

@Override
public void afterKeyword(String keywordName, Annotation annotation) { ... }

@Override
public boolean onError(Exception e) {
    // return true to suppress default error handling
    return false;
}
```

## Building from Source

```bash
git clone https://github.com/exense/step-api.git
cd step-api
mvn clean install
```

## Step Ecosystem

`step-api` is part of the [Step](https://step.dev) open-source automation platform. Related repositories:

| Repository | Description |
|------------|-------------|
| [step](https://github.com/exense/step) | Core backend — the Step controller that discovers and executes Keywords |
| [step-grid](https://github.com/exense/step-grid) | Distributed execution grid that routes keyword calls to agents |
| [step-framework](https://github.com/exense/step-framework) | Infrastructure library used by the Step platform |

For keyword development documentation see the [Keywords section](https://step.dev/knowledgebase/userdocs/keywords/) in the Step knowledgebase, including the [Java](https://step.dev/knowledgebase/keywords/java) and [plugin-based](https://step.dev/knowledgebase/userdocs/keywords/#plugin-based-keywords) keyword guides.

## Contributing

Contributions are welcome. Please open an issue to discuss a bug or feature request before submitting a pull request. All submissions are expected to include appropriate test coverage.

## License

This project is licensed under the [GNU Affero General Public License v3.0](LICENSE).
