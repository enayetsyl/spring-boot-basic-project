

## Step 1 — Create Project (with all dependencies)

Use **start.spring.io** to bootstrap the project, then add the rest of the dependencies in `pom.xml`.

- **Project:** Maven  
- **Language:** Java  
- **Spring Boot:** latest 3.x (requires Java 17+)  
- **Java:** 17 or 21  
- **Group:** `dev.danvega` (reverse-DNS for uniqueness)  
- **Artifact:** `runners`  
- **Add at creation time:** Spring Web, Spring Boot DevTools  
- **Then open `pom.xml` and ensure the following dependencies are present:**

```xml
<dependencies>
  <!-- REST API -->
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
  </dependency>

  <!-- Spring Data JDBC (not JPA) -->
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jdbc</artifactId>
  </dependency>

  <!-- H2 in-memory database (runtime only) -->
  <dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
  </dependency>

  <!-- Live reload & auto-restart during development -->
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
  </dependency>

  <!-- Jakarta Bean Validation (for @Valid, @NotBlank, etc.) -->
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
  </dependency>
</dependencies>
```

**Why each dependency?**
- **spring-boot-starter-web:** REST controllers, JSON (Jackson), and HTTP handling.
- **spring-boot-starter-data-jdbc:** Simple persistence (repositories) without JPA/Hibernate.
- **H2:** In-memory dev database with a browser console.
- **DevTools:** Developer QoL—auto-restart on code changes.
- **Validation:** Enforce input rules with annotations (e.g., `@NotBlank`).

---

## Step 2 — Run the Project (plus H2 configuration & console)

1) **Run the app:**
```bash
./mvnw spring-boot:run
```
App will start at `http://localhost:8080`.

2) **Configure H2 for beginners:**  
Create `src/main/resources/application.properties` and add:

```properties
# Use a stable in-memory DB name so it persists while the app is running
spring.datasource.generate-unique-name=false
spring.datasource.name=runners

# Enable H2 console for viewing tables/data in your browser
spring.h2.console.enabled=true
# spring.h2.console.path=/h2-console  # default path
```

**Explanation (beginner-friendly):**
- **In-memory DB:** Data lives in RAM; it resets when the app stops.
- **`generate-unique-name=false` + `name=runners`:** Forces a predictable JDBC URL: `jdbc:h2:mem:runners`.
- **H2 Console:** Visit `http://localhost:8080/h2-console` to inspect the DB.
  - **JDBC URL:** `jdbc:h2:mem:runners`
  - **User:** `sa` (default)
  - **Password:** *(leave blank)*

---

## Step 3 — Organize by Feature

Create a single feature package for all “Run” files:

```
src/main/java/dev/danvega/runners/run
```

Everything for the “run” feature lives here: `Run.java`, `Location.java`, `RunRepository.java`, `RunController.java`.

---

## Step 4 — Create the `Run` record (with explanations)

**File:** `src/main/java/dev/danvega/runners/run/Run.java`

```java
package dev.danvega.runners.run;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Represents a single run/workout.
 * A Java 'record' is an immutable data carrier:
 * - You get getters, equals/hashCode, toString automatically.
 * - All fields are final; to "change" something you create a new instance.
 */
@Table("RUN") // maps to RUN table in DB
public record Run(

  @Id                 // primary key in DB
  Integer id,

  @NotBlank           // validation: cannot be null/blank
  @Column("TITLE")    // DB column name (UPPER_SNAKE_CASE)
  String title,

  @Column("STARTED_ON")
  LocalDateTime startedOn,

  @Column("COMPLETED_ON")
  LocalDateTime completedOn,

  @Positive           // must be > 0
  @Column("MILES")
  Integer miles,

  @NotNull            // must be provided
  @Column("LOCATION")
  Location location,

  @Version            // optimistic locking field
  @Column("VERSION")
  Integer version
) {
  /**
   * Compact constructor:
   * - Runs after field assignment.
   * - Perfect for invariants that must always hold true.
   * Below, if both timestamps are present, 'completedOn' must be after 'startedOn'.
   */
  public Run {
    if (startedOn != null && completedOn != null && !completedOn.isAfter(startedOn)) {
      throw new IllegalArgumentException("completedOn must be after startedOn");
    }
  }
}
```

**What each item means (beginner):**
- **`@Table("RUN")`**: This Java class/record maps to the DB table **RUN**.
- **`@Column("...")`**: Exact DB column names (we use UPPER_SNAKE_CASE in DB; camelCase in Java).
- **`@Id Integer id`**: The primary key; `Integer` matches the DB’s numeric ID.
- **`String title` + `@NotBlank`**: Must have a non-blank title; validation fails with HTTP 400 if missing.
- **`LocalDateTime startedOn / completedOn`**: Timestamps for start/end.
- **`@Positive Integer miles`**: Must be greater than zero.
- **`Location location + @NotNull`**: Must be provided and must be one of `INDOOR` or `OUTDOOR`.
- **`@Version Integer version`**: Used by Spring Data JDBC for optimistic locking (prevents overwrites when two updates collide).
- **Compact constructor**: Enforces a rule across fields—here we ensure `completedOn` is after `startedOn` when both are provided.

**Enum:** `src/main/java/dev/danvega/runners/run/Location.java`
```java
package dev.danvega.runners.run;

public enum Location { INDOOR, OUTDOOR }
```

---

## Step 5 — Repository (with deeper explanations)

**File:** `src/main/java/dev/danvega/runners/run/RunRepository.java`

```java
package dev.danvega.runners.run;

import java.util.List;
import org.springframework.data.repository.ListCrudRepository;

/**
 * RunRepository is an interface (no code body needed).
 * Spring Data generates a runtime implementation automatically.
 */
public interface RunRepository extends ListCrudRepository<Run, Integer> {

  /**
   * Derived query method:
   * Spring looks at the name and generates SQL to fetch all rows
   * where LOCATION = ?
   */
  List<Run> findAllByLocation(Location location);
}
```

**Key ideas for beginners:**
- **Repository interface**: You only declare *what* you need; Spring Data provides the implementation at runtime.
- **`ListCrudRepository<Run, Integer>`**:
  - The first generic (`Run`) is the entity type this repo manages.
  - The second generic (`Integer`) is the type of the entity’s primary key (`id`).
  - It gives you ready-made methods like `findAll()`, `findById(id)`, `save(entity)`, `deleteById(id)`.
  - It returns **`List<Run>`** instead of `Iterable<Run>` for convenience.
- **`findAllByLocation(...)`**:
  - Called a **derived query**—Spring parses the method name and builds the query automatically.
  - Pass an enum value (e.g., `Location.INDOOR`) to filter results.

---

## Step 6 — Controller (with annotation & validation explanations)

**File:** `src/main/java/dev/danvega/runners/run/RunController.java`

```java
package dev.danvega.runners.run;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController                 // Marks this class as a REST controller (JSON in/out)
@RequestMapping("/api/runs")    // Base URL path for this feature
public class RunController {

  private final RunRepository repository;

  // Constructor Injection: Spring provides a RunRepository instance here
  public RunController(RunRepository repository) {
    this.repository = repository;
  }

  // GET /api/runs
  @GetMapping
  public List<Run> findAll() {
    return repository.findAll();
  }

  // GET /api/runs/{id}
  @GetMapping("/{id}")
  public Optional<Run> findById(@PathVariable Integer id) {
    return repository.findById(id);
  }

  // POST /api/runs
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)            // Return 201 on success
  public Run create(@RequestBody @jakarta.validation.Valid Run run) {
    return repository.save(run);
  }

  // PUT /api/runs/{id}
  @PutMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)         // Return 204 on success
  public void update(@PathVariable Integer id, @RequestBody @jakarta.validation.Valid Run run) {
    repository.save(new Run(
      id,
      run.title(),
      run.startedOn(),
      run.completedOn(),
      run.miles(),
      run.location(),
      run.version()
    ));
  }

  // DELETE /api/runs/{id}
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Integer id) {
    repository.deleteById(id);
  }

  // (Optional) GET /api/runs?location=INDOOR
  @GetMapping(params = "location")
  public List<Run> findByLocation(@RequestParam Location location) {
    return repository.findAllByLocation(location);
  }
}
```

**Annotation explanations (beginner):**
- **`@RestController`**: Tells Spring this class handles REST requests and returns JSON by default.
- **`@RequestMapping("/api/runs")`**: Sets a base path so all methods inside start with `/api/runs`.
- **`@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`**: Map HTTP methods to Java methods.
- **`@PathVariable`**: Reads a path piece like `/api/runs/1` → `id = 1`.
- **`@RequestBody`**: Deserializes incoming JSON into a Java object (`Run`).
- **`@ResponseStatus(HttpStatus.CREATED/NO_CONTENT)`**: Controls the HTTP status code when the method succeeds.
- **Validation (`@jakarta.validation.Valid`)**:
  - Triggers Jakarta Bean Validation on the `Run` object using annotations set in `Run.java` (`@NotBlank`, `@Positive`, etc.).
  - If validation fails, Spring returns **400 Bad Request** with error details.

> **Note:** We intentionally do **not** throw a custom 404 exception here (as requested). If `findById` returns `Optional.empty()`, the response will be `200 OK` with an empty body. Later, you can switch to `ResponseEntity` or an exception if you want a 404.

---

## Step 7 — Database Schema (with field-by-field explanation)

**File:** `src/main/resources/schema.sql`

```sql
CREATE TABLE IF NOT EXISTS RUN (
  ID INT PRIMARY KEY AUTO_INCREMENT,  -- Unique ID generated by DB
  TITLE VARCHAR(255) NOT NULL,        -- Run title; matches @NotBlank on Java side
  STARTED_ON TIMESTAMP,               -- Start datetime
  COMPLETED_ON TIMESTAMP,             -- End datetime
  MILES INT,                          -- Run distance; validated as @Positive
  LOCATION VARCHAR(10),               -- INDOOR / OUTDOOR (enum stored as string)
  VERSION INT                         -- Optimistic locking version
);
```

**How schema matches Java:**
- **`RUN`** ↔ `@Table("RUN")`
- **`TITLE`** ↔ `@Column("TITLE")` on `String title`
- **`MILES`** ↔ `@Column("MILES")` on `Integer miles`
- **`LOCATION`** stores the enum name as a string by default in JDBC
- **`VERSION`** ↔ `@Version` for optimistic locking

---

## Step 8 — Test Endpoints

You can use **Postman** or **curl**.

### Create
```bash
curl -X POST http://localhost:8080/api/runs \
  -H "Content-Type: application/json" \
  -d '{
        "title": "Evening Tempo",
        "startedOn": "2025-09-09T18:00:00",
        "completedOn": "2025-09-09T18:45:00",
        "miles": 5,
        "location": "OUTDOOR"
      }'
```

### List
```bash
curl http://localhost:8080/api/runs
```

### Get One
```bash
curl http://localhost:8080/api/runs/1
```

### Update
```bash
curl -X PUT http://localhost:8080/api/runs/1 \
  -H "Content-Type: application/json" \
  -d '{
        "title": "Evening Tempo (updated)",
        "startedOn": "2025-09-09T18:00:00",
        "completedOn": "2025-09-09T18:44:00",
        "miles": 5,
        "location": "OUTDOOR"
      }'
```

### Delete
```bash
curl -X DELETE http://localhost:8080/api/runs/1
```

### (Optional) Filter by Location
```bash
curl "http://localhost:8080/api/runs?location=INDOOR"
```

---


