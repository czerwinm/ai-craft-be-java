package devices.configuration.management;

public class OptimisticLockException extends RuntimeException {
    public OptimisticLockException(String deviceId, long expectedVersion, long actualVersion) {
        super(String.format(
                "Optimistic lock failed for device %s: expected version %d, but found %d",
                deviceId, expectedVersion, actualVersion
        ));
    }
}
