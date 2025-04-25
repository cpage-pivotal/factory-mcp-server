package org.tanzu.factory.supplychain;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/supply-chain")
public class SupplyChainController {
    private final SupplyChainService supplyChainService;

    public SupplyChainController(SupplyChainService supplyChainService) {
        this.supplyChainService = supplyChainService;
    }

    @GetMapping("/status")
    public ResponseEntity<SupplyChainStatusDto> getCurrentStatus() {
        return ResponseEntity.ok(supplyChainService.getCurrentSupplyChainStatus());
    }

    @GetMapping("/status/{date}")
    public ResponseEntity<SupplyChainStatusDto> getStatusByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(supplyChainService.getSupplyChainStatus(date));
    }

    @GetMapping("/targets/{date}")
    public ResponseEntity<DailyTarget> getDailyTarget(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(supplyChainService.getDailyTarget(date));
    }

    @PostMapping("/targets")
    public ResponseEntity<DailyTarget> setDailyTarget(@RequestBody Map<String, Object> targetData) {
        LocalDate date = LocalDate.parse((String) targetData.get("date"));
        int targetUnits = ((Number) targetData.get("targetUnits")).intValue();

        DailyTarget target = supplyChainService.setDailyTarget(date, targetUnits);
        return ResponseEntity.ok(target);
    }
}