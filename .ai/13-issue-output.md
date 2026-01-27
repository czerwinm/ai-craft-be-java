# [Issue 13] Plan implementacji — Device Installation Process

Źródło wymagań: `src/docs/tasks/device-installation-process.md`.
Standard: `AGENTS.md` + `src/docs/architecture-and-patterns.md` (DDD + Ports&Adapters, JSONB + Liquibase, testy).

## Cel
Zaimplementować pionowy wycinek (business module) „Device Installation Process” jako **process/aggregate**, który prowadzi work order przez fazy instalacji i egzekwuje **inwarianty ukończenia**:
- wszystkie **mandatory** SAT pytania są odpowiedziane,
- **precyzyjna lokalizacja** jest ustawiona,
- **ostatni relevant boot notification** jest potwierdzony (rekonfirmacja tylko gdy „coś się zmieniło”, np. firmware / device swap).

## Kolejność implementacji (3 kroki) + weryfikacja po każdym kroku
Zgodnie z ustaleniem: najpierw domena, potem endpointy, potem persistence. Po każdym kroku uruchamiamy statyczną analizę oraz testy.

- **Krok 1 — Domena**: aggregate `Installation` + VO + eventy + reguły faz/inwariantów + testy unit.
  - **Weryfikacja**:
    - `./gradlew spotlessCheck`
    - `./gradlew test`

- **Krok 2 — Endpointy (HTTP)**: kontrolery + DTO wg kontraktu, delegacja do `InstallationService`.
  - **Weryfikacja**:
    - `./gradlew spotlessCheck`
    - `./gradlew test`

- **Krok 3 — Persistence**: repo adapter (JSONB + optimistic locking) + Liquibase + testy integracyjne repo.
  - **Weryfikacja**:
    - `./gradlew spotlessCheck`
    - `./gradlew test`
    - (opcjonalnie) `./gradlew jacocoTestCoverageVerification`

## Zakres API (adapter HTTP)
Zaimplementować (lub dopasować) endpointy wg kontraktu z opisu zadania:
- `GET /installations/work-orders?...` — listowanie work orderów (filtry status/installer).
- `POST /installations/work-orders/{workOrderId}/assign` — assign / force-reassign.
- `POST /installations/work-orders/{workOrderId}/start` — start instalacji.
- `POST /installations/{installationId}/device` — przypisanie urządzenia (scan).
- `POST /installations/{installationId}/boot-confirmation` — potwierdzenie last relevant boot.
- `POST /installations/{installationId}/location` — ustawienie GPS/pin.
- `POST /installations/{installationId}/finish` — zakończenie (waliduje inwarianty).

## Model domeny (DDD)
Utworzyć nowy moduł, np. `devices.installation.process` (nazewnictwo dopasować do istniejącej struktury `devices.*`).

- **Aggregate**: `Installation` (package-private, bez sufiksu “Aggregate”)
  - **Stan**:
    - `phase` (enum: INITIAL, DEVICE_ASSIGNED, BOOT_CONFIRMATION_REQUIRED/BOOTED, LOCATION_SET, FINAL, FINISHED)
    - `workOrderId` / `installationId` (zdecydować jeden klucz; jeśli 1:1, `workOrderId` może być ID agregatu)
    - `assignedInstallerId` (+ reguły force reassign)
    - `ownership/segment` (znane od początku z sales)
    - `deviceIdentifier` (serial/QR/barcode) + obsługa swap
    - `lastRelevantBoot` + `bootConfirmed` (rekonfirmacja gdy zmieni się vendor/model/firmware/serial/protocol)
    - `location` (Coordinates)
    - `satCompletion` (np. `allMandatoryAnswered` + opcjonalnie brakujące IDs)
  - **Zachowania (komendy)**:
    - assign/reassign installer (z regułą force)
    - start
    - assign device (w tym swap → unieważnij boot confirmation)
    - register boot (ustal “relevant” i czy wymaga potwierdzenia)
    - confirm boot
    - set location
    - finish (sprawdza inwarianty, emituje event)

- **Value objects (records)**:
  - `WorkOrderId`, `InstallationId`, `InstallerId`
  - `DeviceIdentifier`
  - `BootDetails` (vendor/model/firmware/serial/protocol)
  - `Coordinates`
  - (opcjonalnie) `InstallationContext` (segment, country, equipmentCategory, customerId) jeśli potrzebne do integracji z SAT form assignment

- **Domain events**: sealed interface (np. `InstallationEvent`) z recordami:
  - `WorkOrderReceived`
  - `InstallerAssigned` / `InstallerReassigned`
  - `InstallationStarted`
  - `DeviceAssigned` / `DeviceSwapped`
  - `BootNotificationReceived` (opcjonalnie) + `BootConfirmationRequired`
  - `BootConfirmed`
  - `LocationSet`
  - `InstallationFinished` (payload do downstream: deviceId, ownership, location, answers ref, boot details)

## Porty (secondary) i integracje
Zdefiniować porty jako interfejsy (package-private tam gdzie możliwe), a implementacje jako adaptery.

- **Persistence port**: `InstallationRepository` (get/save + query do listowania).
- **Sales inbound port**: wejście dla `WorkOrderReceived` (Kafka/webhook; kontrakt TBD) → tworzy work order / instalację i inicjuje przygotowanie (np. form resolution).
- **SAT form assignment port** (konsumujemy): integracja z “Assigning SAT Form to installation” (rezolucja + bind na początku).
- **SAT answers completion port** (konsumujemy): odczyt `allMandatoryAnswered` (+ brakujące IDs) do weryfikacji przed `finish`.
- **Boot notifications port** (konsumujemy): nasłuch zdarzeń boot (np. mapowanie z istniejących kontrolerów OCPP/IoT16/IoT20 do eventu domenowego).
- **Downstream handover port**: publikacja `InstallationFinished` do „Device Configuration” (event na Spring bus / Kafka / REST — zgodnie z aktualnym stylem repo).

## Persistence (adapter + Liquibase)
Zaimplementować repository adapter zgodnie z `architecture-and-patterns.md`:
- dokument JSONB z agregatem + `@Version` (optimistic locking),
- osobna tabela na event log.

Plan zmian w `src/main/resources/db/db.changelog.yaml`:
- `installation_document` (id, version, installation jsonb)
- `installation_events` (uuid, installation_id/work_order_id, type, time, event jsonb)

## Testy
- **Unit (agregat + VO)**:
  - przejścia między fazami (happy path),
  - reguły assignment/force-reassign,
  - device swap → reset boot confirmation,
  - boot re-confirm wymagany tylko gdy zmieniły się istotne pola,
  - `finish` blokowane gdy brakuje: SAT mandatory / location / boot confirmation.

- **Integration (repo)**:
  - save/get agregatu,
  - optimistic lock conflict (jeśli łatwe do pokrycia),
  - emisja eventów (ApplicationEvents).

- **E2E (pełny flow)**:
  - work order intake → assign installer → start → assign device → boot → confirm boot → set location → (stub/fixture SAT completion = true) → finish → oczekiwana publikacja handover.

## Deliverables (Definition of Done dla modułu)
- kod domeny + porty + adapter HTTP + adapter persistence + adapter inbound (sales) w minimalnym zakresie,
- Liquibase changeSety dla nowych tabel,
- zestaw testów unit/integration/e2e,
- `./gradlew test` przechodzi.

