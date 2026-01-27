# [Business Feature] Device Installation Process

**Subdomain:** Device installation process  
**Source:** [installation-and-sat-domains.md](../installation-and-sat-domains.md#1-device-installation-process)

---

## Business Context

[Link to event storming board / frame](https://miro.com/app/board/uXjVLHsoB8Q=/?share_link_id=???&moveToWidget=???)

[Link to Mockups in Figma](https://www.figma.com/design/...)

### Intro

The **Device Installation Process** subdomain orchestrates the end-to-end physical installation of a device from the moment a work order exists until the device is ready for configuration. It covers work order intake, installer assignment, on-site steps (form answering, device assignment, boot confirmation, location), and completion. It ensures all mandatory evidence and confirmations are collected before the installation is considered finished and handoff to device configuration happens.

One work order corresponds to one device. Ownership (private / corporate / municipality) must be known from the very beginning. Completion requires: mandatory SAT questions answered, precise location set, and last relevant boot notification confirmed (only when something changed, e.g. firmware or device swap).

**Component classification:**
- **Process**: Installation workflow orchestration (state machine: initial → device assignment → boot confirmation → location → final → completion)
- **Invariant**: Completion rules (all mandatory questions answered, location confirmed, boot notification confirmed)

### Business Rules

#### Work order lifecycle
- Work order is for a **single device** (e.g. 2,000 devices ⇒ 2,000 work orders).
- Work order is received from sales system (e.g. Microsoft Dynamics); ownership is known from sales.
- Installation is planned/started when work order and ownership are available.

#### Installer assignment
- **One installer per work order** as the norm.
- **Force-reassign** only in exceptions (sick leave, unavailability).
- Installer can self-assign from list of open work orders or be assigned by supervisor.

#### Installation phases (sequence)
1. **Initial** — SAT form initial section (e.g. contact with customer, exact installation place, evidence photos).
2. **Device assignment** — Link physical device (barcode/serial/QR) to this work order/installation.
3. **Boot confirmation** — Device first boots to backend; installer confirms boot notification details (vendor, model, firmware, serial). Re-confirm only when something changed (firmware, device swap).
4. **Precise location** — Installer sets GPS or manual pin for device location (critical for data quality).
5. **Final** — Remaining SAT questions (e.g. cleanup, no debris, photos).
6. **Finish** — Allowed only when completion rules hold.

#### Completion rules (invariants)
- All **mandatory** SAT questions are answered.
- **Precise location** of the device is set.
- **Last relevant boot notification** is confirmed by installer.
- Edge cases: device swap during installation or wrong equipment at boot ⇒ re-assign device and re-confirm boot; then completion rules apply to the final device.

#### Integrations
- **Sales system**: consumes work order creation (“device sold, install this”).
- **Managing SAT Questions / Assigning SAT Form to installation**: resolve and attach SAT form for this installation.
- **Site Acceptance Tests – form answering**: run answering workflow; receive “all mandatory answered” for completion check.
- **Device / backend (OCPP)**: consumes boot notification; links to installation and drives confirmation.
- **Device Configuration (downstream)**: emits “installation finished” with full payload (device id, location, ownership, answers, boot details).

#### Actor interactions
- **Installer**: Primary actor; list of open/assigned work orders, assign self or be assigned, open work order, complete form, assign device, confirm boot, set location, finish installation.
- **Installation supervisor**: Assign/reassign installers; force-reassign in exceptional cases.

---

## API Contract FE vs BE

_Adjust to actual contract once endpoints are defined. Below reflects subdomain responsibilities._

### 1. List work orders (for installer / supervisor)

```
GET /installations/work-orders?status=open|assigned|in-progress&installerId={id}

Response: list of work orders with id, ownership, device spec, assigned installer (if any), status, SAT form ref.
```

### 2. Assign installer to work order

```
POST /installations/work-orders/{workOrderId}/assign

{
  "installerId": "...",
  "forceReassign": false
}

Response: 200 when assigned; 409 when already assigned (unless forceReassign).
```

### 3. Start installation (open work order for execution)

```
POST /installations/work-orders/{workOrderId}/start

Response: installation id, current phase, SAT form definition reference.
```

### 4. Assign device to installation (barcode/serial scan)

```
POST /installations/{installationId}/device

{
  "deviceIdentifier": "..."  // serial / barcode / QR
}

Response: 200; links device instance to this installation.
```

### 5. Confirm boot notification

```
POST /installations/{installationId}/boot-confirmation

{
  "bootNotificationId": "...",
  "confirmed": true
}

Response: 200; marks last relevant boot as confirmed for completion.
```

### 6. Set precise location

```
POST /installations/{installationId}/location

{
  "longitude": 16.93,
  "latitude": 51.09
}

Response: 200.
```

### 7. Finish installation

```
POST /installations/{installationId}/finish

Response: 200 when completion rules hold; 400 when mandatory questions / location / boot confirmation missing.
```

### 8. Consume work order (inbound from sales)

_Implementation-specific (e.g. Kafka, webhook). Event/message contract:_

- **WorkOrderReceived** (or equivalent): workOrderId, deviceSpec, ownership (segment, customerId), country, equipmentCategory, …  
- Triggers creation of work order and preparation (e.g. SAT form resolution).

---

## Technical Requirements

For this feature follow the [Technical Standard](/src/docs/architecture-and-patterns.md) and the docs referenced in [AGENTS.md](/AGENTS.md) (domain-model, ports, adapters).

---

## In Scope

- Domain model for work order, installation, phases, completion rules, and unit tests.
- Work order lifecycle and installer assignment rules with unit tests.
- Integration with Assigning SAT Form to installation (form resolution and binding).
- Integration with Site Acceptance Tests – form answering (completion check: all mandatory answered).
- Integration with boot notification flow (receive boot, require confirmation when relevant).
- Persistence for work orders and installations.
- HTTP adapter (or equivalent) fulfilling the API contract above.
- Inbound adapter for work order events from sales system (contract TBD).
- Business module (vertical slice) for Device Installation Process.
- End-to-end tests for work order intake, assignment, phase progression, and finish.

---

## Out of Scope

- Full UI for installer/supervisor (in-scope: API; UI can be separate initiative).
- Implementation of **Managing SAT Questions** or **Assigning SAT Form to installation** (this subdomain consumes them).
- Implementation of **Site Acceptance Tests – form answering** (this subdomain consumes it).
- Implementation of **Device Configuration** (this subdomain emits “installation finished” toward it).
- OCPP/boot notification server logic (may live in Boot Notification Handler; this subdomain consumes outcomes).
- JWT/security implementation (reuse existing where applicable).
