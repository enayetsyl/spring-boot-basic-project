# Step 1 - Create Project

Use Spring Initializr

- Open start.spring.io.

- Configure:

  - Project: Maven

  - Language: Java

  - Spring Boot: Latest stable (for reference, Spring lists 3.5.5 as a current stable line). 
  - Java: 17 or 21

  - Group: reverse-DNS style (e.g., dev.danvega) — Maven’s coordinates recommend your domain reversed to keep names globally unique. 
- Medium

  - Artifact: runners

- Dependencies:

  - Spring Web – REST controllers & JSON support. 
  - Spring Boot DevTools – automatic restart & LiveReload during development. 

Click Generate, unzip, and open the folder in VS Code (or import as a Maven project in your IDE).

# Step 2 - Run the Project

```bash
./mvnw spring-boot:run
```

# Step 3 - Organize the project by features

Inside src/main/java/com/example/demo create a folder for a feature. Here changeable parts are com, example, demo which will be different based on the project. Inside the project for each feature there will be a folder. In our case the folder name is "run". 

# Step 4 - Create a record for the feature

The file name should be Run.java. In your case it should be feature name with first letter in capital. 

Here we will Table annotation that will hold database table name with uppercase letters. The record name should be same as feature name with first letter in capital. Inside parenthesis we will define columns name in db(in uppercase and snake_case format), type and java name in camelCase.


# Step 5 - Creating a repository

Inside the feature folder we have create a file like RunRepository.java. This file defines a repository interface using Spring Data. In simple terms, a repository is a mechanism for encapsulating storage, retrieval, and search behavior which emulates a collection of objects.You don't write the implementation class yourself. Spring Data will automatically create a proxy object that implements this interface when the application starts. This interface extends Spring Data's ListCrudRepository. This inheritance provides your repository with a rich set of pre-implemented methods for data manipulation.

Sample code snnipit below:

```java
package dev.danvega.runners.run;

import java.util.List;
import org.springframework.data.repository.ListCrudRepository;

public interface RunRepository extends ListCrudRepository<Run, Integer> {
 
}
```

- <Run, Integer>: These are generic parameters that tell Spring Data what this repository is for: 
  - Run: This is the domain type or entity the repository will manage. In this case, it's the Run record defined in Run.java.
  - Integer: This is the data type of the primary key (@Id) of the Run entity. If you look at Run.java, the id field is an Integer.

# Step 6 - Create a controller

Inside the feature folder we have create a file like RunController.java. This file defines a REST controller, which is a fundamental component in a Spring Boot web application. Its primary role is to handle incoming HTTP requests, process them, and return an HTTP response. The RunController exposes a set of RESTful endpoints (like GET, POST, PUT, DELETE) under the base path /api/runs. It uses the RunRepository to interact with the database for creating, reading, updating, and deleting Run records. Its code should be as follows:

```java
package dev.danvega.runners.run;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/runs")
public class RunController {

  private final RunRepository repository;

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
  public Run findById(@PathVariable Integer id) {
    return repository.findById(id).orElseThrow(() -> new RunNotFoundException(id));
  }

  // POST /api/runs
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Run create(@RequestBody Run run) {
    return repository.save(run);
  }

  // PUT /api/runs/{id}
  @PutMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void update(@PathVariable Integer id, @RequestBody Run run) {
    repository.save(new Run(
      id, run.title(), run.startedOn(), run.completedOn(), run.miles(), run.location(), run.version()
    ));
  }

  // DELETE /api/runs/{id}
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Integer id) {
    repository.deleteById(id);
  }
}
```
- @RestController marks it as a REST endpoint component
  
- @RequestMapping("/api/runs") sets the base path. This annotation maps all requests starting with /api/runs to this controller.

- @GetMapping: This maps HTTP GET requests for /api/runs to this method. It calls repository.findAll(), which retrieves all Run entities from the database, and returns them as a List<Run>. Spring Boot will serialize this list into a JSON array.

- @GetMapping("/{id}"): This maps HTTP GET requests to a path like /api/runs/1. The {id} part is a path variable. @PathVariable Integer id: This annotation extracts the value from the {id} path variable and converts it into an Integer. It returns an Optional<Run>. If a Run with the given id is found, the response will be a 200 OK with the Run object as JSON. If not found, the response will be a 200 OK with an empty body.

- @PostMapping: Maps HTTP POST requests for /api/runs to this method. @ResponseStatus(HttpStatus.CREATED): If the method executes successfully, it sets the HTTP response status code to 201 Created, which is the standard for resource creation.
@RequestBody Run run: This tells Spring to deserialize the JSON from the request body into a Run object. It calls repository.save(run) to persist the new Run to the database and returns the saved entity (which will now include the database-generated id).

- @DeleteMapping("/{id}"): Maps HTTP DELETE requests for a path like /api/runs/1. @ResponseStatus(HttpStatus.NO_CONTENT): Sets the response status to 204 No Content on success. It calls repository.deleteById(id) to remove the Run from the database.

# Step 7 - Add H2 for persistence

- Add following dependency in the pom.xml file

```xml
<dependencies>
  <!-- REST API -->
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
  </dependency>

  <!-- Spring Data JDBC -->
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jdbc</artifactId>
  </dependency>

  <!-- H2 (runtime) -->
  <dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
  </dependency>

  <!-- DevTools (dev only) -->
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
  </dependency>
</dependencies>

```


# Step 8 - H2 configuration and console

- In the src/main/resources/application.properties file add following line to configure H2

```properties
# Use a stable in-memory db name
spring.datasource.generate-unique-name=false
spring.datasource.name=runners

# Enable the H2 web console and keep default path /h2-console
spring.h2.console.enabled=true
# spring.h2.console.path=/h2-console  # (default)
```

# Step 9 - Add dependency for Jakarta validation

- Open the pom.xml file and add following dependency

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```


# Step 10 - Update Run.java to add constrain

```java

```

# Step 11 - Update RunController.java to add validation

- The code should be as follows:

```java
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Run create(@RequestBody @jakarta.validation.Valid Run run) {
    return repository.save(run);
  }

  @PutMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void update(@PathVariable Integer id, @RequestBody @jakarta.validation.Valid Run run) {
    repository.save(new Run(
        id, run.title(), run.startedOn(), run.completedOn(),
        run.miles(), run.location(), run.version()));
  }
```

- It will ensure that run variable is matched with Run object. On validation failure, Spring Boot automatically returns 400 Bad Request with an error payload.


# Step 12 - Testing with postman

- Open postman
- User following to check all routes