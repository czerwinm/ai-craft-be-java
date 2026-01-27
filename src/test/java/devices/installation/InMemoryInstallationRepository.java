package devices.installation;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

class InMemoryInstallationRepository implements InstallationRepository {

  private final Map<WorkOrderId, Installation> store = new HashMap<>();

  @Override
  public Optional<Installation> get(WorkOrderId workOrderId) {
    return Optional.ofNullable(store.get(workOrderId));
  }

  @Override
  public void save(Installation installation) {
    store.put(installation.workOrderId, installation);
  }
}
