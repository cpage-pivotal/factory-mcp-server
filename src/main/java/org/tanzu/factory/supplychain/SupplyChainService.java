package org.tanzu.factory.supplychain;

import org.springaicommunity.mcp.annotation.McpTool;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tanzu.factory.factory.FactoryService;
import org.tanzu.factory.factory.ProductionOutputDto;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
public class SupplyChainService {
    private final DailyTargetRepository targetRepository;
    private final FactoryService factoryService;

    // Assume 8-hour production day (8am to 4pm)
    private static final LocalTime SHIFT_START = LocalTime.of(8, 0);
    private static final LocalTime SHIFT_END = LocalTime.of(16, 0);

    public SupplyChainService(DailyTargetRepository targetRepository,
                              FactoryService factoryService) {
        this.targetRepository = targetRepository;
        this.factoryService = factoryService;
    }

    @Transactional
    public DailyTarget setDailyTarget(LocalDate date, int targetUnits) {
        Optional<DailyTarget> existingTarget = targetRepository.findByDate(date);

        if (existingTarget.isPresent()) {
            DailyTarget target = existingTarget.get();
            target.setTargetUnits(targetUnits);
            return targetRepository.save(target);
        } else {
            DailyTarget newTarget = new DailyTarget(date, targetUnits);
            return targetRepository.save(newTarget);
        }
    }

    @McpTool(description = "Retrieves the daily production target for a specific date, returning a default target of 0 if none exists")
    public DailyTarget getDailyTarget(LocalDate date) {
        return targetRepository.findByDate(date)
                .orElse(new DailyTarget(date, 0)); // Return empty target if none exists
    }

    @McpTool(description = "Gets the current supply chain status for today, including current production output, projections, target completion percentage, and whether production is on track")
    public SupplyChainStatusDto getCurrentSupplyChainStatus() {
        return getSupplyChainStatus(LocalDate.now());
    }

    @McpTool(description = "Gets detailed supply chain status for a specific date, including production metrics, target completion, and projections based on current production rates")
    public SupplyChainStatusDto getSupplyChainStatus(LocalDate date) {
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime shiftStart = LocalDateTime.of(date, SHIFT_START);
        LocalDateTime shiftEnd = LocalDateTime.of(date, SHIFT_END);

        // Always query the full shift window so pre-seeded simulation data is included
        LocalDateTime queryEndTime = shiftEnd;

        // For today, determine how far into the shift we are (for projections)
        boolean isToday = date.equals(now.toLocalDate());
        boolean shiftInProgress = isToday && now.isAfter(shiftStart) && now.isBefore(shiftEnd);

        // Get the daily target
        DailyTarget target = getDailyTarget(date);

        // Get production outputs for each stage using the full shift window
        List<ProductionOutputDto> stageOutputs = factoryService.getAllStagesOutput(shiftStart, queryEndTime);

        // Final stage output is our current total production
        int finalStageOrder = stageOutputs.stream()
                .mapToInt(ProductionOutputDto::stageOrder)
                .max()
                .orElse(0);

        int currentOutput = stageOutputs.stream()
                .filter(output -> output.stageOrder() == finalStageOrder)
                .mapToInt(output -> output.unitsProduced() - output.defectiveUnits())
                .findFirst()
                .orElse(0);

        // For projections: if mid-shift, project based on elapsed time; otherwise use actual output
        int projectedOutput;
        if (shiftInProgress) {
            double hoursElapsed = Duration.between(shiftStart, now).toMinutes() / 60.0;
            double hoursTotal = Duration.between(shiftStart, shiftEnd).toMinutes() / 60.0;
            projectedOutput = hoursElapsed > 0
                    ? (int) (currentOutput * (hoursTotal / hoursElapsed))
                    : currentOutput;
        } else {
            projectedOutput = currentOutput;
        }

        double targetCompletion = target.getTargetUnits() > 0
                ? (double) currentOutput / target.getTargetUnits() * 100
                : 100.0;

        boolean onTrack = projectedOutput >= target.getTargetUnits();

        return new SupplyChainStatusDto(
                date,
                target.getTargetUnits(),
                currentOutput,
                projectedOutput,
                targetCompletion,
                onTrack,
                stageOutputs
        );
    }
}