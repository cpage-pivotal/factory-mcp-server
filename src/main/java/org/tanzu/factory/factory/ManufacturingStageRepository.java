package org.tanzu.factory.factory;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ManufacturingStageRepository extends JpaRepository<ManufacturingStage, Long> {
    ManufacturingStage findBySequenceOrder(int sequenceOrder);
}
