package devices.configuration.management;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

interface DeviceConfigurationRepository {

    Optional<VersionedDevice> findById(String deviceId);

    long save(DeviceConfiguration device, Long expectedVersion);

    void delete(String deviceId, long expectedVersion);

    boolean exists(String deviceId);

    Page<VersionedDevice> findAll(Pageable pageable);
}
