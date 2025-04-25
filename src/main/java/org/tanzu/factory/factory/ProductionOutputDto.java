package org.tanzu.factory.factory;

import java.time.LocalDateTime;

public record ProductionOutputDto(
        int stageOrder,
        String stageName,
        int unitsProduced,
        int defectiveUnits,
        double effectiveYieldPercentage,
        LocalDateTime startTime,
        LocalDateTime endTime
) {}