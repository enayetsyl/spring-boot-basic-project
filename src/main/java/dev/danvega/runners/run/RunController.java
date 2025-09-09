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