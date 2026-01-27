# Business Problem Decomposition (Vertical Slices)

Based on the analysis of the transcript and diagram, here is the proposed decomposition of the Installation Process business problem into smaller, manageable subdomains (vertical slices).

## Subdomain: Installation Workflow
**Responsibilities:**
This subdomain is responsible for the core orchestration and state management of the installation process. It acts as the central coordinator (Aggregate) that tracks the progress of an installation from the initial Work Order to completion. It handles installer assignment, linking the physical device (via QR/Barcode), correlating hardware events (Boot Notification) with the installation context, capturing location data, and enforcing the process flow (e.g., ensuring device details match).

**Integrations:**
- **Other Subdomains:**
  - **SAT Form:** (Read/Event) Checks if the form is complete; listens for form submission events.
  - **Section Management:** (None direct, mediated via SAT Form).
  - **Device Configuration:** (Command/Event) Triggers the creation/handover of the fully installed device upon process completion.
- **External Systems:**
  - **Sales System (MS Dynamics):** (Event/Command) Receives `Work Order` created events to initiate the process.
  - **Identity/Access Management:** (Read) For installer authentication and assignment.
  - **Hardware/IoT Gateway:** (Event) Receives `Boot Notification Request` from the physical device.
- **Actors:**
  - **Installer:** Accepts tasks, scans devices, confirms device details, captures GPS, triggers completion.
  - **System:** Automatically matches Boot Notifications.

## Subdomain: SAT Form (Questionnaire)
**Responsibilities:**
This subdomain handles the dynamic data collection required during installation. It is responsible for rendering the appropriate questions based on the installation context (determined by Section Management rules), collecting answers (text, photos, selections), validating input, and managing the internal logic of the form (e.g., conditional visibility of sections). It acts as a "data collector" separate from the process flow state.

**Integrations:**
- **Other Subdomains:**
  - **Installation Workflow:** (Read) Provides the context (Installation ID, Client Type, etc.) to determine which form to render.
  - **Section Management:** (Read) Fetches the definitions of sections, questions, and applicability rules.
- **External Systems:**
  - **Blob Storage:** (Write) Stores uploaded photos of the installation.
- **Actors:**
  - **Installer:** Fills out the form, uploads photos.

## Subdomain: Section Management (Configuration)
**Responsibilities:**
This is a supporting subdomain (Generic/Supporting) focused on defining the structure and rules of the SAT Forms. It provides an editor for administrators to create and manage form sections, questions, and the metadata rules that dictate when a specific section applies (e.g., "Show this section for Corporate clients in Poland using Wallbox chargers"). It ensures that changes to definitions do not affect in-flight installations (versioning/snapshotting).

**Integrations:**
- **Other Subdomains:**
  - **SAT Form:** (Read) Provides the schema and rules for form rendering.
- **External Systems:**
  - None.
- **Actors:**
  - **Administrator:** Defines and updates sections and rules.

## Subdomain: Device Configuration (Existing)
**Responsibilities:**
This is the downstream subdomain that manages the lifecycle of the device *after* it has been successfully installed. It maintains the "Device Twin" or configuration state, ownership, and operational settings. The Installation process hands over responsibility to this subdomain upon successful completion.

**Integrations:**
- **Other Subdomains:**
  - **Installation Workflow:** (Event) Listens for `InstallationFinished` events to instantiate or update the device record with verified data (Serial Number, Location, Owner).
- **External Systems:**
  - **Sales System:** (Read) May sync customer data.
- **Actors:**
  - **Support/Operations:** Manages device settings post-installation.

---

## Component Classification & Event Strategy

### Installation Workflow
- **Component Type:** Process / Aggregate
- **Key Events:**
  - `WorkOrderReceived` (Start)
  - `InstallerAssigned`
  - `DeviceLinked` (QR scan)
  - `BootNotificationMatched`
  - `LocationCaptured`
  - `InstallationCompleted` (Pivotal Event)

### SAT Form
- **Component Type:** Data Editor / Collection
- **Key Events:**
  - `FormStarted`
  - `AnswersUpdated`
  - `FormSubmitted` (Domain Event)

### Section Management
- **Component Type:** Editor / Policy
- **Key Events:**
  - `SectionDefined`
  - `RulesUpdated` (Snapshot Events for SAT Form to consume)
