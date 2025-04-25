package org.tanzu.factory.factory;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "manufacturing_stages")
public class ManufacturingStage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private int sequenceOrder;
    private String description;

    @OneToMany(mappedBy = "stage")
    private Set<IoTDevice> devices = new HashSet<>();

    public ManufacturingStage() {
    }

    public ManufacturingStage(String name, int sequenceOrder, String description) {
        this.name = name;
        this.sequenceOrder = sequenceOrder;
        this.description = description;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSequenceOrder() {
        return sequenceOrder;
    }

    public void setSequenceOrder(int sequenceOrder) {
        this.sequenceOrder = sequenceOrder;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<IoTDevice> getDevices() {
        return devices;
    }

    public void setDevices(Set<IoTDevice> devices) {
        this.devices = devices;
    }
}
