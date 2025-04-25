package org.tanzu.factory.supplychain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;

public interface DailyTargetRepository extends JpaRepository<DailyTarget, Long> {
    Optional<DailyTarget> findByDate(LocalDate date);
}

