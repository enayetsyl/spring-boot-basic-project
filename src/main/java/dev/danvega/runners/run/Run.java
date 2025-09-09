package dev.danvega.runners.run;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;


@Table("RUN")
public record Run(
  @Id Integer id,
  @Column("TITLE") String title,
  @Column("STARTED_ON") LocalDateTime startedOn,
  @Column ("COMPLETED_ON") LocalDateTime completedOn,
  @Column ("MILES") Integer miles,
  @Column("LOCATION") Location location,
  @Version @Column("VERSION") Integer version
) {

  public Run {
    if(startedOn != null && completedOn != null && completedOn.isBefore(startedOn)){
      throw new IllegalArgumentException("Completed on must be after started on");
    }
  }
}
