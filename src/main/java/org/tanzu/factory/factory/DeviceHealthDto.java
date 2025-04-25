package org.tanzu.factory.factory;

public record DeviceHealthDto(
        Long id,
        String deviceId,
        String name,
        String deviceType,
        boolean operational,
        double healthScore
) {}