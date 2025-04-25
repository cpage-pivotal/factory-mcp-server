package org.tanzu.factory.supplychain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "daily_targets")
public class DailyTarget {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate date;
    private int targetUnits;

    public DailyTarget() {
    }

    public DailyTarget(LocalDate date, int targetUnits) {
        this.date = date;
        this.targetUnits = targetUnits;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public int getTargetUnits() {
        return targetUnits;
    }

    public void setTargetUnits(int targetUnits) {
        this.targetUnits = targetUnits;
    }
}