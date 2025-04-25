package org.tanzu.factory;

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
import java.util.List;
import java.util.Random;

@Component
public class DataInitializer implements CommandLineRunner {
    private final ManufacturingStageRepository stageRepository;
    private final IoTDeviceRepository deviceRepository;
    private final ProductionMetricsRepository metricsRepository;
    private final DailyTargetRepository targetRepository;
    private final Random random = new Random();

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
        // Only initialize if no data exists
        if (stageRepository.count() > 0) {
            return;
        }

        // Create manufacturing stages
        ManufacturingStage bodyAssembly = new ManufacturingStage("Body Assembly", 1,
                "Vehicle body assembly and welding");
        ManufacturingStage paintShop = new ManufacturingStage("Paint Shop", 2,
                "Vehicle painting and coating");
        ManufacturingStage finalAssembly = new ManufacturingStage("Final Assembly", 3,
                "Engine, interior, and final component assembly");

        stageRepository.saveAll(List.of(bodyAssembly, paintShop, finalAssembly));

        // Create IoT devices for each stage
        // Body Assembly devices
        IoTDevice weldingRobot1 = new IoTDevice("BA-WR-001", "Welding Robot 1", "WELDING_ROBOT", bodyAssembly);
        IoTDevice weldingRobot2 = new IoTDevice("BA-WR-002", "Welding Robot 2", "WELDING_ROBOT", bodyAssembly);
        IoTDevice stampingPress1 = new IoTDevice("BA-SP-001", "Stamping Press 1", "STAMPING_PRESS", bodyAssembly);
        IoTDevice qualityScanner1 = new IoTDevice("BA-QS-001", "Quality Scanner 1", "QUALITY_SCANNER", bodyAssembly);

        // Paint Shop devices
        IoTDevice paintRobot1 = new IoTDevice("PS-PR-001", "Paint Robot 1", "PAINT_ROBOT", paintShop);
        IoTDevice paintRobot2 = new IoTDevice("PS-PR-002", "Paint Robot 2", "PAINT_ROBOT", paintShop);
        IoTDevice dryingOven1 = new IoTDevice("PS-DO-001", "Drying Oven 1", "DRYING_OVEN", paintShop);
        IoTDevice colorMixer1 = new IoTDevice("PS-CM-001", "Color Mixer 1", "COLOR_MIXER", paintShop);

        // Final Assembly devices
        IoTDevice dashboardAssembler = new IoTDevice("FA-DA-001", "Dashboard Assembler", "ASSEMBLY_ROBOT", finalAssembly);
        IoTDevice engineMounter = new IoTDevice("FA-EM-001", "Engine Mounting Robot", "ASSEMBLY_ROBOT", finalAssembly);
        IoTDevice wireLoom = new IoTDevice("FA-WL-001", "Wire Loom Installer", "ASSEMBLY_ROBOT", finalAssembly);
        IoTDevice finalInspection = new IoTDevice("FA-FI-001", "Final Inspection Scanner", "QUALITY_SCANNER", finalAssembly);

        // Set a few devices to have issues (for demonstration)
        weldingRobot2.setOperational(false);
        weldingRobot2.setHealthScore(35.0);

        paintRobot1.setHealthScore(78.5);

        // Save all devices
        List<IoTDevice> allDevices = List.of(
                weldingRobot1, weldingRobot2, stampingPress1, qualityScanner1,
                paintRobot1, paintRobot2, dryingOven1, colorMixer1,
                dashboardAssembler, engineMounter, wireLoom, finalInspection
        );
        deviceRepository.saveAll(allDevices);

        // Generate sample production metrics for today
        LocalDate today = LocalDate.now();
        LocalDateTime shiftStart = LocalDateTime.of(today, LocalTime.of(8, 0));

        // Set daily target
        DailyTarget todayTarget = new DailyTarget(today, 120); // Target of 120 vehicles
        targetRepository.save(todayTarget);

        // Create some historical data for yesterday
        LocalDate yesterday = today.minusDays(1);
        DailyTarget yesterdayTarget = new DailyTarget(yesterday, 115);
        targetRepository.save(yesterdayTarget);

        // Generate metrics
        generateMetrics(bodyAssembly.getId(), shiftStart, 140, 6);
        generateMetrics(paintShop.getId(), shiftStart, 132, 4);
        generateMetrics(finalAssembly.getId(), shiftStart, 125, 3);

        // Generate yesterday's metrics
        LocalDateTime yesterdayStart = LocalDateTime.of(yesterday, LocalTime.of(8, 0));
        generateMetrics(bodyAssembly.getId(), yesterdayStart, 120, 5);
        generateMetrics(paintShop.getId(), yesterdayStart, 118, 3);
        generateMetrics(finalAssembly.getId(), yesterdayStart, 115, 2);
    }

    private void generateMetrics(Long stageId, LocalDateTime startTime, int totalUnits, int totalDefects) {
        List<IoTDevice> stageDevices = deviceRepository.findByStage(
                stageRepository.findById(stageId).orElseThrow());

        // Only use operational devices
        List<IoTDevice> operationalDevices = stageDevices.stream()
                .filter(IoTDevice::isOperational)
                .toList();

        int devicesCount = operationalDevices.size();
        if (devicesCount == 0) return;

        // Distribute production across operational devices
        int baseUnitsPerDevice = totalUnits / devicesCount;
        int remainingUnits = totalUnits % devicesCount;

        // Distribute defects across devices
        int baseDefectsPerDevice = totalDefects / devicesCount;
        int remainingDefects = totalDefects % devicesCount;

        // Number of hours in a shift
        int hoursInShift = 8;

        // Generate hourly metrics for each device
        for (int i = 0; i < operationalDevices.size(); i++) {
            IoTDevice device = operationalDevices.get(i);

            // Allocate units to this device
            int deviceUnits = baseUnitsPerDevice + (i < remainingUnits ? 1 : 0);
            int deviceDefects = baseDefectsPerDevice + (i < remainingDefects ? 1 : 0);

            // Distribute over the hours
            int baseUnitsPerHour = deviceUnits / hoursInShift;
            int remainingUnitsHourly = deviceUnits % hoursInShift;

            int baseDefectsPerHour = deviceDefects / hoursInShift;
            int remainingDefectsHourly = deviceDefects % hoursInShift;

            for (int hour = 0; hour < hoursInShift; hour++) {
                LocalDateTime hourTime = startTime.plusHours(hour);

                // Calculate this hour's metrics
                int hourlyUnits = baseUnitsPerHour + (hour < remainingUnitsHourly ? 1 : 0);
                int hourlyDefects = baseDefectsPerHour + (hour < remainingDefectsHourly ? 1 : 0);

                // Add some randomness to cycle time (between 4-8 minutes per unit)
                double cycleTimeMinutes = 4.0 + random.nextDouble() * 4.0;

                // Create and save metrics
                ProductionMetrics metrics = new ProductionMetrics(
                        hourTime, hourlyUnits, hourlyDefects, cycleTimeMinutes, device);
                metricsRepository.save(metrics);
            }
        }
    }
}