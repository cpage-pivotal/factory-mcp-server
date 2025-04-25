package org.tanzu.factory.factory;

import org.springframework.web.bind.annotation.RestController;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/factory")
public class FactoryController {
    private final FactoryService factoryService;

    public FactoryController(FactoryService factoryService) {
        this.factoryService = factoryService;
    }

    @GetMapping("/stages/health")
    public ResponseEntity<List<StageHealthDto>> getAllStagesHealth() {
        return ResponseEntity.ok(factoryService.getManufacturingStagesHealth());
    }

    @GetMapping("/stages/{stageId}/health")
    public ResponseEntity<StageHealthDto> getStageHealth(@PathVariable Long stageId) {
        StageHealthDto healthDto = factoryService.getStageHealth(stageId);
        if (healthDto != null) {
            return ResponseEntity.ok(healthDto);
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/devices/{deviceId}/health")
    public ResponseEntity<Void> updateDeviceHealth(
            @PathVariable Long deviceId,
            @RequestBody Map<String, Object> updateData) {

        boolean operational = (boolean) updateData.get("operational");
        double healthScore = ((Number) updateData.get("healthScore")).doubleValue();

        factoryService.updateDeviceHealth(deviceId, operational, healthScore);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/stages/{stageOrder}/output")
    public ResponseEntity<ProductionOutputDto> getStageOutput(
            @PathVariable int stageOrder,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        ProductionOutputDto output = factoryService.getStageOutput(stageOrder, startTime, endTime);
        if (output != null) {
            return ResponseEntity.ok(output);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/output")
    public ResponseEntity<List<ProductionOutputDto>> getAllStagesOutput(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        return ResponseEntity.ok(factoryService.getAllStagesOutput(startTime, endTime));
    }

    @PostMapping("/devices/{deviceId}/metrics")
    public ResponseEntity<Void> recordProductionMetrics(
            @PathVariable Long deviceId,
            @RequestBody Map<String, Object> metrics) {

        int unitsProduced = ((Number) metrics.get("unitsProduced")).intValue();
        int defectiveUnits = ((Number) metrics.get("defectiveUnits")).intValue();
        double cycleTimeMinutes = ((Number) metrics.get("cycleTimeMinutes")).doubleValue();

        factoryService.recordProductionMetrics(deviceId, unitsProduced, defectiveUnits, cycleTimeMinutes);
        return ResponseEntity.ok().build();
    }
}
