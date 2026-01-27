package devices.installation;

import java.util.Optional;

interface InstallationRepository {
  Optional<Installation> get(WorkOrderId workOrderId);

  void save(Installation installation);
}
