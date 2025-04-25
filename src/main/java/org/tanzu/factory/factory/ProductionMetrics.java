package org.tanzu.factory.factory;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "production_metrics")
public class ProductionMetrics {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime timestamp;
    private int unitsProduced;
    private int defectiveUnits;
    private double cycleTimeMinutes; // Time to produce one unit

    @ManyToOne
    @JoinColumn(name = "device_id")
    private IoTDevice device;

    public ProductionMetrics() {
    }

    public ProductionMetrics(LocalDateTime timestamp, int unitsProduced, int defectiveUnits,
                             double cycleTimeMinutes, IoTDevice device) {
        this.timestamp = timestamp;
        this.unitsProduced = unitsProduced;
        this.defectiveUnits = defectiveUnits;
        this.cycleTimeMinutes = cycleTimeMinutes;
        this.device = device;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public int getUnitsProduced() {
        return unitsProduced;
    }

    public void setUnitsProduced(int unitsProduced) {
        this.unitsProduced = unitsProduced;
    }

    public int getDefectiveUnits() {
        return defectiveUnits;
    }

    public void setDefectiveUnits(int defectiveUnits) {
        this.defectiveUnits = defectiveUnits;
    }

    public double getCycleTimeMinutes() {
        return cycleTimeMinutes;
    }

    public void setCycleTimeMinutes(double cycleTimeMinutes) {
        this.cycleTimeMinutes = cycleTimeMinutes;
    }

    public IoTDevice getDevice() {
        return device;
    }

    public void setDevice(IoTDevice device) {
        this.device = device;
    }
}
