package org.tanzu.factory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.tanzu.factory.factory.IoTDevice;
import org.tanzu.factory.factory.ManufacturingStage;
import org.tanzu.factory.factory.ProductionMetrics;
import org.tanzu.factory.factory.IoTDeviceRepository;
import org.tanzu.factory.factory.ManufacturingStageRepository;
import org.tanzu.factory.factory.ProductionMetricsRepository;
import org.tanzu.factory.supplychain.DailyTarget;
import org.tanzu.factory.supplychain.DailyTargetRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
public class DataInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final ManufacturingStageRepository stageRepository;
    private final IoTDeviceRepository deviceRepository;
    private final ProductionMetricsRepository metricsRepository;
    private final DailyTargetRepository targetRepository;
    private final Random random = new Random();

    private static final int MIN_DAILY_TARGET = 100;
    private static final int MAX_DAILY_TARGET = 160;
    private static final int MIN_STAGE_UNITS = 90;
    private static final int MAX_STAGE_UNITS = 170;
    private static final double DEFECT_RATE_MIN = 0.01;
    private static final double DEFECT_RATE_MAX = 0.06;
    private static final double DEVICE_FAILURE_PROBABILITY = 0.15;
    private static final double MIN_HEALTHY_SCORE = 70.0;
    private static final double MIN_DEGRADED_SCORE = 20.0;
    private static final double MAX_DEGRADED_SCORE = 60.0;

    public DataInitializer(ManufacturingStageRepository stageRepository,
                           IoTDeviceRepository deviceRepository,
                           ProductionMetricsRepository metricsRepository,
                           DailyTargetRepository targetRepository) {
        this.stageRepository = stageRepository;
        this.deviceRepository = deviceRepository;
        this.metricsRepository = metricsRepository;
        this.targetRepository = targetRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (stageRepository.count() > 0) {
            return;
        }

        log.info("Generating random factory data for today's date...");

        ManufacturingStage bodyAssembly = new ManufacturingStage("Body Assembly", 1,
                "Vehicle body assembly and welding");
        ManufacturingStage paintShop = new ManufacturingStage("Paint Shop", 2,
                "Vehicle painting and coating");
        ManufacturingStage finalAssembly = new ManufacturingStage("Final Assembly", 3,
                "Engine, interior, and final component assembly");

        stageRepository.saveAll(List.of(bodyAssembly, paintShop, finalAssembly));

        List<IoTDevice> allDevices = createDevices(bodyAssembly, paintShop, finalAssembly);
        randomizeDeviceHealth(allDevices);
        deviceRepository.saveAll(allDevices);

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        int todayTargetUnits = randomBetween(MIN_DAILY_TARGET, MAX_DAILY_TARGET);
        int yesterdayTargetUnits = randomBetween(MIN_DAILY_TARGET, MAX_DAILY_TARGET);
        targetRepository.save(new DailyTarget(today, todayTargetUnits));
        targetRepository.save(new DailyTarget(yesterday, yesterdayTargetUnits));
        log.info("Daily targets — today: {}, yesterday: {}", todayTargetUnits, yesterdayTargetUnits);

        LocalDateTime todayShiftStart = LocalDateTime.of(today, LocalTime.of(8, 0));
        LocalDateTime yesterdayShiftStart = LocalDateTime.of(yesterday, LocalTime.of(8, 0));

        for (ManufacturingStage stage : List.of(bodyAssembly, paintShop, finalAssembly)) {
            int todayUnits = randomBetween(MIN_STAGE_UNITS, MAX_STAGE_UNITS);
            int todayDefects = randomDefects(todayUnits);
            generateMetrics(stage.getId(), todayShiftStart, todayUnits, todayDefects);
            log.info("Today — {}: {} units, {} defects", stage.getName(), todayUnits, todayDefects);

            int yesterdayUnits = randomBetween(MIN_STAGE_UNITS, MAX_STAGE_UNITS);
            int yesterdayDefects = randomDefects(yesterdayUnits);
            generateMetrics(stage.getId(), yesterdayShiftStart, yesterdayUnits, yesterdayDefects);
            log.info("Yesterday — {}: {} units, {} defects", stage.getName(), yesterdayUnits, yesterdayDefects);
        }

        log.info("Random factory data generation complete.");
    }

    private List<IoTDevice> createDevices(ManufacturingStage bodyAssembly,
                                          ManufacturingStage paintShop,
                                          ManufacturingStage finalAssembly) {
        List<IoTDevice> devices = new ArrayList<>();

        devices.add(new IoTDevice("BA-WR-001", "Welding Robot 1", "WELDING_ROBOT", bodyAssembly));
        devices.add(new IoTDevice("BA-WR-002", "Welding Robot 2", "WELDING_ROBOT", bodyAssembly));
        devices.add(new IoTDevice("BA-SP-001", "Stamping Press 1", "STAMPING_PRESS", bodyAssembly));
        devices.add(new IoTDevice("BA-QS-001", "Quality Scanner 1", "QUALITY_SCANNER", bodyAssembly));

        devices.add(new IoTDevice("PS-PR-001", "Paint Robot 1", "PAINT_ROBOT", paintShop));
        devices.add(new IoTDevice("PS-PR-002", "Paint Robot 2", "PAINT_ROBOT", paintShop));
        devices.add(new IoTDevice("PS-DO-001", "Drying Oven 1", "DRYING_OVEN", paintShop));
        devices.add(new IoTDevice("PS-CM-001", "Color Mixer 1", "COLOR_MIXER", paintShop));

        devices.add(new IoTDevice("FA-DA-001", "Dashboard Assembler", "ASSEMBLY_ROBOT", finalAssembly));
        devices.add(new IoTDevice("FA-EM-001", "Engine Mounting Robot", "ASSEMBLY_ROBOT", finalAssembly));
        devices.add(new IoTDevice("FA-WL-001", "Wire Loom Installer", "ASSEMBLY_ROBOT", finalAssembly));
        devices.add(new IoTDevice("FA-FI-001", "Final Inspection Scanner", "QUALITY_SCANNER", finalAssembly));

        return devices;
    }

    private void randomizeDeviceHealth(List<IoTDevice> devices) {
        for (IoTDevice device : devices) {
            if (random.nextDouble() < DEVICE_FAILURE_PROBABILITY) {
                device.setOperational(false);
                device.setHealthScore(roundTo1Decimal(
                        MIN_DEGRADED_SCORE + random.nextDouble() * (MAX_DEGRADED_SCORE - MIN_DEGRADED_SCORE)));
                log.info("Device {} is non-operational (health: {})", device.getName(), device.getHealthScore());
            } else {
                device.setOperational(true);
                double healthScore = MIN_HEALTHY_SCORE + random.nextDouble() * (100.0 - MIN_HEALTHY_SCORE);
                device.setHealthScore(roundTo1Decimal(healthScore));
            }
        }

        // Ensure at least one device per stage is operational
        ensureMinOperationalPerStage(devices);
    }

    private void ensureMinOperationalPerStage(List<IoTDevice> devices) {
        var byStage = new java.util.HashMap<Long, List<IoTDevice>>();
        for (IoTDevice d : devices) {
            byStage.computeIfAbsent(d.getStage().getId(), k -> new ArrayList<>()).add(d);
        }
        for (var entry : byStage.entrySet()) {
            boolean hasOperational = entry.getValue().stream().anyMatch(IoTDevice::isOperational);
            if (!hasOperational) {
                IoTDevice picked = entry.getValue().get(random.nextInt(entry.getValue().size()));
                picked.setOperational(true);
                picked.setHealthScore(roundTo1Decimal(
                        MIN_HEALTHY_SCORE + random.nextDouble() * (100.0 - MIN_HEALTHY_SCORE)));
                log.info("Ensured {} is operational for its stage", picked.getName());
            }
        }
    }

    private void generateMetrics(Long stageId, LocalDateTime startTime, int totalUnits, int totalDefects) {
        List<IoTDevice> stageDevices = deviceRepository.findByStage(
                stageRepository.findById(stageId).orElseThrow());

        List<IoTDevice> operationalDevices = stageDevices.stream()
                .filter(IoTDevice::isOperational)
                .toList();

        int devicesCount = operationalDevices.size();
        if (devicesCount == 0) return;

        int baseUnitsPerDevice = totalUnits / devicesCount;
        int remainingUnits = totalUnits % devicesCount;

        int baseDefectsPerDevice = totalDefects / devicesCount;
        int remainingDefects = totalDefects % devicesCount;

        int hoursInShift = 8;

        for (int i = 0; i < operationalDevices.size(); i++) {
            IoTDevice device = operationalDevices.get(i);

            int deviceUnits = baseUnitsPerDevice + (i < remainingUnits ? 1 : 0);
            int deviceDefects = baseDefectsPerDevice + (i < remainingDefects ? 1 : 0);

            int baseUnitsPerHour = deviceUnits / hoursInShift;
            int remainingUnitsHourly = deviceUnits % hoursInShift;

            int baseDefectsPerHour = deviceDefects / hoursInShift;
            int remainingDefectsHourly = deviceDefects % hoursInShift;

            for (int hour = 0; hour < hoursInShift; hour++) {
                LocalDateTime hourTime = startTime.plusHours(hour);

                int hourlyUnits = baseUnitsPerHour + (hour < remainingUnitsHourly ? 1 : 0);
                int hourlyDefects = baseDefectsPerHour + (hour < remainingDefectsHourly ? 1 : 0);

                double cycleTimeMinutes = roundTo1Decimal(4.0 + random.nextDouble() * 4.0);

                ProductionMetrics metrics = new ProductionMetrics(
                        hourTime, hourlyUnits, hourlyDefects, cycleTimeMinutes, device);
                metricsRepository.save(metrics);
            }
        }
    }

    private int randomBetween(int min, int max) {
        return min + random.nextInt(max - min + 1);
    }

    private int randomDefects(int totalUnits) {
        double defectRate = DEFECT_RATE_MIN + random.nextDouble() * (DEFECT_RATE_MAX - DEFECT_RATE_MIN);
        return Math.max(1, (int) Math.round(totalUnits * defectRate));
    }

    private double roundTo1Decimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}