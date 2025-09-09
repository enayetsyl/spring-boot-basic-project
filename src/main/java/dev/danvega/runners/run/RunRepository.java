package dev.danvega.runners.run;

import java.util.List;

import org.springframework.data.repository.ListCrudRepository;

public interface RunRepository extends ListCrudRepository<Run, Integer> {
     List<Run> findAllByLocation(Location location);
}
