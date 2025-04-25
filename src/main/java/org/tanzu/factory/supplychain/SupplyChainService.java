package org.tanzu.factory.supplychain;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
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
//    @Tool(description = "Sets or updates the daily production target for a specific date, which is used to track production performance and forecasting")
    public DailyTarget setDailyTarget(@ToolParam(description = "The date for which to set the daily production target") LocalDate date,
                                      @ToolParam(description = "The number of units to be produced on the specified date") int targetUnits) {
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

    @Tool(description = "Retrieves the daily production target for a specific date, returning a default target of 0 if none exists")
    public DailyTarget getDailyTarget(@ToolParam(description = "The date for which to retrieve the daily production target") LocalDate date) {
        return targetRepository.findByDate(date)
                .orElse(new DailyTarget(date, 0)); // Return empty target if none exists
    }

    @Tool(description = "Gets the current supply chain status for today, including current production output, projections, target completion percentage, and whether production is on track")
    public SupplyChainStatusDto getCurrentSupplyChainStatus() {
        return getSupplyChainStatus(LocalDate.now());
    }

    @Tool(description = "Gets detailed supply chain status for a specific date, including production metrics, target completion, and projections based on current production rates")
    public SupplyChainStatusDto getSupplyChainStatus(@ToolParam(description = "The date for which to retrieve the supply chain status") LocalDate date) {
        LocalDateTime now = LocalDateTime.now();

        // Get shift start and end times for the given date
        LocalDateTime shiftStart = LocalDateTime.of(date, SHIFT_START);
        LocalDateTime shiftEnd = LocalDateTime.of(date, SHIFT_END);

        // Ensure we don't query future times
        LocalDateTime currentEndTime = now.isBefore(shiftEnd) ? now : shiftEnd;

        // If we're querying a past date, use the full shift time
        if (date.isBefore(now.toLocalDate())) {
            currentEndTime = shiftEnd;
        }

        // Get the daily target
        DailyTarget target = getDailyTarget(date);

        // Get production outputs for each stage
        List<ProductionOutputDto> stageOutputs = factoryService.getAllStagesOutput(shiftStart, currentEndTime);

        // Final stage output is our current total production
        // (assumes stages are ordered and last stage is final assembly)
        int finalStageOrder = stageOutputs.stream()
                .mapToInt(ProductionOutputDto::stageOrder)
                .max()
                .orElse(0);

        int currentOutput = stageOutputs.stream()
                .filter(output -> output.stageOrder() == finalStageOrder)
                .mapToInt(output -> output.unitsProduced() - output.defectiveUnits())
                .findFirst()
                .orElse(0);

        // Calculate projected end-of-day output based on current production rate
        double hoursElapsed = date.equals(now.toLocalDate())
                ? Duration.between(shiftStart, currentEndTime).toHours() + 1 // Add 1 to avoid division by zero
                : Duration.between(shiftStart, shiftEnd).toHours();

        double hoursTotal = Duration.between(shiftStart, shiftEnd).toHours();

        int projectedOutput = (int) (currentOutput * (hoursTotal / hoursElapsed));

        // If we're past shift end, projected output is actual output
        if (now.isAfter(shiftEnd) && date.equals(now.toLocalDate())) {
            projectedOutput = currentOutput;
        }

        // Determine if we're on track to meet daily target
        double targetCompletion = target.getTargetUnits() > 0
                ? (double) currentOutput / target.getTargetUnits() * 100
                : 100.0;

        boolean onTrack = projectedOutput >= target.getTargetUnits();

        // Return a new record instance
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