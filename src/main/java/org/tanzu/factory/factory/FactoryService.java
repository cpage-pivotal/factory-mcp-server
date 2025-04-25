package org.tanzu.factory.factory;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FactoryService {
    private final ManufacturingStageRepository stageRepository;
    private final IoTDeviceRepository deviceRepository;
    private final ProductionMetricsRepository metricsRepository;

    public FactoryService(ManufacturingStageRepository stageRepository,
                          IoTDeviceRepository deviceRepository,
                          ProductionMetricsRepository metricsRepository) {
        this.stageRepository = stageRepository;
        this.deviceRepository = deviceRepository;
        this.metricsRepository = metricsRepository;
    }

    @Tool(description = "Retrieves the health status of all manufacturing stages in the factory, including overall health scores and device status information for each stage")
    public List<StageHealthDto> getManufacturingStagesHealth() {
        List<ManufacturingStage> stages = stageRepository.findAll();
        return stages.stream()
                .map(this::convertToStageHealthDto)
                .collect(Collectors.toList());
    }

    @Tool(description = "Gets detailed health information for a specific manufacturing stage, including its overall health score, device statuses, and operational metrics")
    public StageHealthDto getStageHealth(@ToolParam(description = "The unique identifier of the manufacturing stage to retrieve health information for") Long stageId) {
        Optional<ManufacturingStage> stageOpt = stageRepository.findById(stageId);
        return stageOpt.map(this::convertToStageHealthDto).orElse(null);
    }

    private StageHealthDto convertToStageHealthDto(ManufacturingStage stage) {
        List<IoTDevice> devices = deviceRepository.findByStage(stage);
        List<IoTDevice> operationalDevices = deviceRepository.findByStageAndOperationalTrue(stage);

        // Calculate overall health score as average of all device health scores
        double overallHealth = devices.stream()
                .mapToDouble(IoTDevice::getHealthScore)
                .average()
                .orElse(0.0);

        // Map devices to DTOs
        List<DeviceHealthDto> deviceDtos = devices.stream()
                .map(this::convertToDeviceHealthDto)
                .collect(Collectors.toList());

        return new StageHealthDto(
                stage.getId(),
                stage.getName(),
                stage.getSequenceOrder(),
                overallHealth,
                devices.size(),
                operationalDevices.size(),
                deviceDtos
        );
    }

    private DeviceHealthDto convertToDeviceHealthDto(IoTDevice device) {
        return new DeviceHealthDto(
                device.getId(),
                device.getDeviceId(),
                device.getName(),
                device.getDeviceType(),
                device.isOperational(),
                device.getHealthScore()
        );
    }

    @Transactional
//    @Tool(description = "Updates the operational status and health score of a specific IoT device in the factory")
    public void updateDeviceHealth(@ToolParam(description = "The unique identifier of the IoT device to update") Long deviceId,
                                   @ToolParam(description = "Indicates whether the device is currently operational (true) or offline (false)") boolean operational,
                                   @ToolParam(description = "The health score of the device on a scale from 0-100, where 100 represents perfect health") double healthScore) {
        deviceRepository.findById(deviceId).ifPresent(device -> {
            device.setOperational(operational);
            device.setHealthScore(healthScore);
            deviceRepository.save(device);
        });
    }

    @Tool(description = "Retrieves production output metrics for a specific manufacturing stage during a specified time period, including units produced, defective units, and effective yield")
    public ProductionOutputDto getStageOutput(@ToolParam(description = "The sequence order number of the manufacturing stage (e.g., 1 for Body Assembly, 2 for Paint Shop, etc.)") int stageOrder,
                                              @ToolParam(description = "The start timestamp for the production metrics period") LocalDateTime startTime,
                                              @ToolParam(description = "The end timestamp for the production metrics period") LocalDateTime endTime) {
        ManufacturingStage stage = stageRepository.findBySequenceOrder(stageOrder);
        if (stage == null) {
            return null;
        }

        Integer unitsProduced = metricsRepository.getTotalUnitsByStageAndTimeRange(
                stageOrder, startTime, endTime);
        Integer defectiveUnits = metricsRepository.getTotalDefectiveUnitsByStageAndTimeRange(
                stageOrder, startTime, endTime);

        // Handle null values from the database
        unitsProduced = unitsProduced != null ? unitsProduced : 0;
        defectiveUnits = defectiveUnits != null ? defectiveUnits : 0;

        double effectiveYield = unitsProduced > 0
                ? 100.0 * (unitsProduced - defectiveUnits) / unitsProduced
                : 0.0;

        return new ProductionOutputDto(
                stageOrder,
                stage.getName(),
                unitsProduced,
                defectiveUnits,
                effectiveYield,
                startTime,
                endTime
        );
    }

    @Tool(description = "Retrieves production output metrics for all manufacturing stages in the factory during a specified time period")
    public List<ProductionOutputDto> getAllStagesOutput(@ToolParam(description = "The start timestamp for the production metrics period") LocalDateTime startTime,
                                                        @ToolParam(description = "The end timestamp for the production metrics period") LocalDateTime endTime) {
        List<ManufacturingStage> stages = stageRepository.findAll();
        List<ProductionOutputDto> outputs = new ArrayList<>();

        for (ManufacturingStage stage : stages) {
            ProductionOutputDto output = getStageOutput(stage.getSequenceOrder(), startTime, endTime);
            if (output != null) {
                outputs.add(output);
            }
        }

        return outputs;
    }

    @Transactional
//    @Tool(description = "Records production metrics for a specific IoT device, including units produced, defective units, and cycle time")
    public void recordProductionMetrics(@ToolParam(description = "The unique identifier of the IoT device for which to record metrics") Long deviceId,
                                        @ToolParam(description = "The number of units produced by the device") int unitsProduced,
                                        @ToolParam(description = "The number of defective units produced by the device") int defectiveUnits,
                                        @ToolParam(description = "The time in minutes required to produce one unit") double cycleTimeMinutes) {
        deviceRepository.findById(deviceId).ifPresent(device -> {
            ProductionMetrics metrics = new ProductionMetrics(
                    LocalDateTime.now(), unitsProduced, defectiveUnits, cycleTimeMinutes, device);
            metricsRepository.save(metrics);
        });
    }
}