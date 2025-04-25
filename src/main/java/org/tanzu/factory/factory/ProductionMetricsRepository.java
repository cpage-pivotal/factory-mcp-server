package org.tanzu.factory.factory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;

public interface ProductionMetricsRepository extends JpaRepository<ProductionMetrics, Long> {
    List<ProductionMetrics> findByDeviceAndTimestampBetween(IoTDevice device, LocalDateTime start, LocalDateTime end);

    @Query("SELECT SUM(pm.unitsProduced) FROM ProductionMetrics pm " +
            "JOIN pm.device d JOIN d.stage s WHERE s.sequenceOrder = :stageOrder " +
            "AND pm.timestamp BETWEEN :startTime AND :endTime")
    Integer getTotalUnitsByStageAndTimeRange(int stageOrder, LocalDateTime startTime, LocalDateTime endTime);

    @Query("SELECT SUM(pm.defectiveUnits) FROM ProductionMetrics pm " +
            "JOIN pm.device d JOIN d.stage s WHERE s.sequenceOrder = :stageOrder " +
            "AND pm.timestamp BETWEEN :startTime AND :endTime")
    Integer getTotalDefectiveUnitsByStageAndTimeRange(int stageOrder, LocalDateTime startTime, LocalDateTime endTime);
}
