# step-api

[![License](https://img.shields.io/badge/License-AGPL%20v3-blue.svg)](LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/ch.exense.step/step-api)](https://central.sonatype.com/artifact/ch.exense.step/step-api)

The API for writing **Keywords** in the [Step automation platform](https://step.exense.ch) in Java and .NET. This library is the only dependency needed to implement keyword libraries that can be executed by the Step engine.

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

// Add a performance measurement
output.startMeasure("db-query");
// ... measured code ...
output.stopMeasure("db-query");

// Attach a file to the output
output.addAttachment(AttachmentHelper.generateAttachmentForException(e));
```

### 5. Lifecycle hooks

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
