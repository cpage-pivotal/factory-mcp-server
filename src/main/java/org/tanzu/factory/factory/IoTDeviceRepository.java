package org.tanzu.factory.factory;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IoTDeviceRepository extends JpaRepository<IoTDevice, Long> {
    List<IoTDevice> findByStage(ManufacturingStage stage);
    List<IoTDevice> findByStageAndOperationalTrue(ManufacturingStage stage);
}
