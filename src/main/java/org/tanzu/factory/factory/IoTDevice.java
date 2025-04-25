package org.tanzu.factory.factory;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "iot_devices")
public class IoTDevice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String deviceId;
    private String name;
    private String deviceType;
    private boolean operational;
    private double healthScore; // 0-100 scale

    @ManyToOne
    @JoinColumn(name = "stage_id")
    private ManufacturingStage stage;

    @OneToMany(mappedBy = "device")
    private List<ProductionMetrics> metrics = new ArrayList<>();

    public IoTDevice() {
    }

    public IoTDevice(String deviceId, String name, String deviceType, ManufacturingStage stage) {
        this.deviceId = deviceId;
        this.name = name;
        this.deviceType = deviceType;
        this.stage = stage;
        this.operational = true;
        this.healthScore = 100.0;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public boolean isOperational() {
        return operational;
    }

    public void setOperational(boolean operational) {
        this.operational = operational;
    }

    public double getHealthScore() {
        return healthScore;
    }

    public void setHealthScore(double healthScore) {
        this.healthScore = healthScore;
    }

    public ManufacturingStage getStage() {
        return stage;
    }

    public void setStage(ManufacturingStage stage) {
        this.stage = stage;
    }

    public List<ProductionMetrics> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<ProductionMetrics> metrics) {
        this.metrics = metrics;
    }
}
