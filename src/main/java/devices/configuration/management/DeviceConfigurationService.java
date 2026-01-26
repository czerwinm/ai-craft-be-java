package devices.configuration.management;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class DeviceConfigurationService {

    private final DeviceConfigurationRepository repository;

    public DeviceConfigurationSnapshot createDevice(String deviceId) {
        if (repository.exists(deviceId)) {
            throw new DeviceAlreadyExistsException(deviceId);
        }

        DeviceConfiguration device = DeviceConfiguration.newDevice(deviceId);
        repository.save(device, null);
        return device.toSnapshot();
    }

    @Transactional(readOnly = true)
    public Optional<DeviceConfigurationSnapshot> getDevice(String deviceId) {
        return repository.findById(deviceId)
                .map(VersionedDevice::device)
                .map(DeviceConfiguration::toSnapshot);
    }

    public DeviceConfigurationSnapshot updateOwnership(String deviceId, Ownership ownership, long version) {
        VersionedDevice versioned = repository.findById(deviceId)
                .orElseThrow(() -> new DeviceNotFoundException(deviceId));
        DeviceConfiguration device = versioned.device();
        device.changeOwnership(ownership);
        repository.save(device, version);
        return device.toSnapshot();
    }

    public DeviceConfigurationSnapshot updateLocation(String deviceId, Location location, long version) {
        VersionedDevice versioned = repository.findById(deviceId)
                .orElseThrow(() -> new DeviceNotFoundException(deviceId));
        DeviceConfiguration device = versioned.device();
        device.changeLocation(location);
        repository.save(device, version);
        return device.toSnapshot();
    }

    public DeviceConfigurationSnapshot updateOpeningHours(String deviceId, OpeningHours openingHours, long version) {
        VersionedDevice versioned = repository.findById(deviceId)
                .orElseThrow(() -> new DeviceNotFoundException(deviceId));
        DeviceConfiguration device = versioned.device();
        device.changeOpeningHours(openingHours);
        repository.save(device, version);
        return device.toSnapshot();
    }

    public DeviceConfigurationSnapshot updateSettings(String deviceId, Settings settings, long version) {
        VersionedDevice versioned = repository.findById(deviceId)
                .orElseThrow(() -> new DeviceNotFoundException(deviceId));
        DeviceConfiguration device = versioned.device();
        device.changeSettings(settings);
        repository.save(device, version);
        return device.toSnapshot();
    }

    public void deleteDevice(String deviceId, long version) {
        repository.delete(deviceId, version);
    }

    @Transactional(readOnly = true)
    public Page<DeviceConfigurationSnapshot> listDevices(Pageable pageable) {
        return repository.findAll(pageable)
                .map(VersionedDevice::device)
                .map(DeviceConfiguration::toSnapshot);
    }

    public DeviceConfigurationSnapshot patchDevice(String deviceId, DeviceConfigurationPatch patch, long version) {
        VersionedDevice versioned = repository.findById(deviceId)
                .orElseThrow(() -> new DeviceNotFoundException(deviceId));
        DeviceConfiguration device = versioned.device();
        DeviceConfigurationSnapshot currentSnapshot = device.toSnapshot();
        patch.applyTo(device, currentSnapshot);
        repository.save(device, version);
        return device.toSnapshot();
    }

    public long getCurrentVersion(String deviceId) {
        return repository.findById(deviceId)
                .map(VersionedDevice::version)
                .orElseThrow(() -> new DeviceNotFoundException(deviceId));
    }
}
