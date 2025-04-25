package org.tanzu.factory.supplychain;

import org.tanzu.factory.factory.ProductionOutputDto;
import java.time.LocalDate;
import java.util.List;

public record SupplyChainStatusDto(
        LocalDate date,
        int dailyTarget,
        int currentOutput,
        int projectedEndOfDayOutput,
        double targetCompletionPercentage,
        boolean onTrack,
        List<ProductionOutputDto> stageOutputs
) {}