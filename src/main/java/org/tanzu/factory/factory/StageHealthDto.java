package org.tanzu.factory.factory;

import java.util.List;

public record StageHealthDto(
        Long stageId,
        String stageName,
        int sequenceOrder,
        double overallHealthScore,
        int totalDevices,
        int operationalDevices,
        List<DeviceHealthDto> devices
) {}