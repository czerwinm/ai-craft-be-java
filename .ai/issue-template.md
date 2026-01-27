---
name: Business Feature
about: Template for ensuring Github Issues for Business Feature are ready for implementation
title: '[Business Feature] '
labels:
    - 'business-feature'
assignees: ''
---

## Business Context

[Link to event storming board / frame](https://miro.com/app/board/uXjVLHsoB8Q=/?share_link_id=???&moveToWidget=???)

[Link to Mockups in Figma](https://www.figma.com/design/...)

### Intro
Some information about device need to be stored in central system database (device itself is not aware about those informations).

Informations like:
ownership of device: operator (tenant), and provider (entity providing charging service)
location address and coordinates
opening hours: availability of device
some settings like: device should be public or not, visible on map etc.

### Business Rules

#### Initial business rules:

New device should be configured as:
- UNOWNED - ownership with both operator and provider set to null
- location = null
- openingHours = Always Open
- settings = default settings (all false)

**NOTE**: For robustness, always keep ownership, openingHours, and settings as NOT NULL—use the above defaults instead.
For owned devices, both operator and provider in ownership must always be set.
In location only coordinates, longitude and latitude are mandatory.

#### Tenant Isolation Rule:
- Customers are tenant-scoped
- All event operations are tenant-scoped
- Users can only perform actions on events within their tenant
- Event visibility and all operations (read, update, assign, archive) require tenant membership


## API Contract FE vs BE:

1. Read device details:
```
GET /devices/{deviceId}

{
  deviceId: "ALF-98262561",
  ownership: {
    operator: "Devicex.nl",
    provider: "public-devices"
  },
  location: {
    street: "Rakietowa",
    houseNumber: "1A",
    city: "Wrocław",
    postalCode: "54-621",
    country: "POL",
    coordinates: {
      longitude: 16.931752852309156,
      latitude: 51.09836221719513
    }
  },
  openingHours: {
    alwaysOpen: true
  },
  settings: {
    autoStart: false,
    remoteControl: false,
    billing: false,
    reimbursement: false,
    showOnMap: false,
    publicAccess: false
  }
}
```

2. Change device owner:
```
PATCH /devices/{deviceId}

{
  ownership: {
    operator: "Devicex.nl",
    provider: "public-devices"
  }
}
```
3. Set device UNOWNED:
```
PATCH /devices/{deviceId}

{
  ownership: {
    operator: null,
    provider: null
  }
}
```

3. Set new location:
```
PATCH /devices/{deviceId}

{
  location: {
    street: "Rakietowa",
    houseNumber: "2A",
    city: "Wrocław",
    postalCode: "54-621",
    country: "POL",
    coordinates: {
      longitude: 16.931752852309156,
      latitude: 51.09836221719513
    }
}
```

4. Change selectively some settings:
```
PATCH /devices/{deviceId}

{
  settings: {
    showOnMap: true,
    publicAccess: true
  }
}
```

## Technical Requirements

For that feature our [Technical Standard](/src/docs/architecture-and-patterns.md)

## In Scope

- Domain Model matching rules with unit tests
- Controller fulfilling API Contract
- NoSQL persistence
- Business Module (Vertical Slice)
- End-to-end tests for Business Scenarios

## Out of Scope

- UI
- JWT security implementation
- Adapter for Integration with OpenAI
- Adapter for Integration with Emails
- Implementation of `Customers Module`
- Scenario: `Event Manager` generates Brief based on Notes with use of AI


---

# Configuration Management — Gherkin Scenarios

Scenarios for `devices.configuration.management` derived from `DeviceConfigurationEditor`, `Visibility`, and `Violations`. Grouped by responsibility.



## 1. DeviceConfigurationEditor — Interaction

Scenarios for creating/loading a device, updating ownership/location/settings, event emission, and producing `DeviceConfiguration`.

### 1.1 Device creation

- **Create device with valid ID** — `createDevice(deviceId)` initialises editor with default ownership, no location, default settings, and emits `DeviceCreated`.

```gherkin
Scenario: Create device with valid ID
  When a device is created with id "device-123"
  Then the editor has device id "device-123"
  And exactly one domain event is emitted
  And the event is DeviceCreated with deviceId "device-123"
  And ownership is unowned
  And location is null
  And settings are default
```

- **Reject null device ID** — `createDevice(null)` throws `IllegalArgumentException`.

```gherkin
Scenario: Reject null device ID
  When a device is created with null id
  Then an IllegalArgumentException is thrown
```

- **Reject empty device ID** — `createDevice("")` throws `IllegalArgumentException`.

```gherkin
Scenario: Reject empty device ID
  When a device is created with id ""
  Then an IllegalArgumentException is thrown
```

- **Reject blank device ID** — `createDevice("   ")` throws `IllegalArgumentException`.

```gherkin
Scenario: Reject blank device ID
  When a device is created with id "   "
  Then an IllegalArgumentException is thrown
```

### 1.2 Ownership updates

- **Emit OwnershipChanged when ownership changes** — `updateOwnership(newOwnership)` updates state and appends one `OwnershipChanged` event.

```gherkin
Scenario: Emit OwnershipChanged when ownership changes
  Given a device was created with id "device-123"
  And the creation event is ignored
  When ownership is updated to operator "op1" provider "pr1"
  Then exactly one OwnershipChanged event is emitted
  And the event has deviceId "device-123" and the new ownership
```

- **Do not emit event when ownership is unchanged** — Calling `updateOwnership(sameOwnership)` again leaves event list unchanged.

```gherkin
Scenario: Do not emit OwnershipChanged when ownership is unchanged
  Given a device was created with id "device-123"
  And ownership was set to operator "op" provider "pr"
  And subsequent events are ignored
  When ownership is updated to operator "op" provider "pr" again
  Then no new events are emitted
```

- **Reject null ownership** — `updateOwnership(null)` throws `NullPointerException`.

```gherkin
Scenario: Reject null ownership update
  Given a device was created with id "device-123"
  When ownership is updated to null
  Then a NullPointerException is thrown
```

### 1.3 Location updates

- **Emit LocationChanged when location changes** — `updateLocation(newLocation)` updates location and appends one `LocationChanged` event.

```gherkin
Scenario: Emit LocationChanged when location changes
  Given a device was created with id "device-123"
  And the creation event is ignored
  When location is updated to a valid location
  Then exactly one LocationChanged event is emitted
  And the event has deviceId "device-123" and the new location
```

- **Do not emit event when location is unchanged** — Calling `updateLocation(sameLocation)` again leaves event list unchanged.

```gherkin
Scenario: Do not emit LocationChanged when location is unchanged
  Given a device was created with id "device-123"
  And location was set to a valid location
  And subsequent events are ignored
  When location is updated to the same location again
  Then no new events are emitted
```

- **Allow clearing location (set to null)** — `updateLocation(null)` is allowed and emits `LocationChanged` with null location.

```gherkin
Scenario: Allow clearing location to null
  Given a device was created with id "device-123"
  And location was set to a valid location
  And subsequent events are ignored
  When location is updated to null
  Then exactly one LocationChanged event is emitted
  And the event has null location
```

### 1.4 Settings updates

- **Emit SettingsChanged when settings change** — `updateSettings(settingsDiff)` merges diff, updates settings when different, and appends one `SettingsChanged` event.

```gherkin
Scenario: Emit SettingsChanged when settings change
  Given a device was created with id "device-123"
  And the creation event is ignored
  When settings are updated with a non-empty diff
  Then exactly one SettingsChanged event is emitted
  And the event has deviceId "device-123" and merged settings
```

- **Do not emit event when merged settings are unchanged** — `updateSettings(diff)` with diff that yields same effective settings emits no new event.

```gherkin
Scenario: Do not emit SettingsChanged when merged settings are unchanged
  Given a device was created with id "device-123"
  And subsequent events are ignored
  When settings are updated with diff that does not change effective values
  Then no new events are emitted
```

- **Reject null settings diff** — `updateSettings(null)` throws `NullPointerException`.

```gherkin
Scenario: Reject null settings update
  Given a device was created with id "device-123"
  When settings are updated with null
  Then a NullPointerException is thrown
```

### 1.5 Multiple updates and snapshot

- **Multiple updates emit multiple events** — A sequence of create, updateOwnership, updateLocation, updateSettings yields creation plus one event per actual change.

```gherkin
Scenario: Multiple updates emit multiple events
  Given a device was created with id "device-123"
  When ownership is updated to operator "op" provider "pr"
  And location is updated to a valid location
  And settings are updated with a diff that changes values
  Then there are 4 domain events in order: DeviceCreated, OwnershipChanged, LocationChanged, SettingsChanged
```

- **toDeviceConfiguration produces full snapshot** — `toDeviceConfiguration()` returns a `DeviceConfiguration` with current state, violations from `checkViolations()`, and visibility from `calculateVisibility(violations)`.

```gherkin
Scenario: toDeviceConfiguration produces full snapshot
  Given a device was created with id "device-123"
  And ownership location and settings were updated
  When toDeviceConfiguration is called
  Then the result has deviceId "device-123"
  And the result has current ownership location openingHours and settings
  And the result has violations from checkViolations
  And the result has visibility from calculateVisibility for those violations
```

## 2. Visibility — Logic

Scenarios for `Visibility.of(violations, settings)`: `ForCustomer` (USABLE_AND_VISIBLE_ON_MAP, USABLE_BUT_HIDDEN_ON_MAP, INACCESSIBLE_AND_HIDDEN_ON_MAP) and `roamingEnabled`.

### 2.1 Usable and visible

- **No violations, public access, show on map** — Device is usable and visible on map; roaming enabled.

```gherkin
Scenario: No violations and public access and show on map yields usable and visible
  Given violations have no flags set
  And settings have publicAccess true and showOnMap true
  When visibility is calculated from those violations and settings
  Then forCustomer is USABLE_AND_VISIBLE_ON_MAP
  And roamingEnabled is true
```

### 2.2 Usable but hidden

- **No violations, public access, hide from map** — Device is usable but not shown on map; roaming enabled.

```gherkin
Scenario: No violations and public access but not show on map yields usable but hidden
  Given violations have no flags set
  And settings have publicAccess true and showOnMap false
  When visibility is calculated from those violations and settings
  Then forCustomer is USABLE_BUT_HIDDEN_ON_MAP
  And roamingEnabled is true
```

### 2.3 Inaccessible and hidden

- **Any violation makes device inaccessible** — At least one violation implies INACCESSIBLE_AND_HIDDEN_ON_MAP and roaming disabled.

```gherkin
Scenario: Any violation yields inaccessible and hidden
  Given violations have at least one flag set
  And settings have publicAccess true and showOnMap true
  When visibility is calculated from those violations and settings
  Then forCustomer is INACCESSIBLE_AND_HIDDEN_ON_MAP
  And roamingEnabled is false
```

- **No violations but private (no public access)** — No violations but `publicAccess` false implies inaccessible and roaming disabled.

```gherkin
Scenario: No violations but private yields inaccessible
  Given violations have no flags set
  And settings have publicAccess false
  When visibility is calculated from those violations and settings
  Then forCustomer is INACCESSIBLE_AND_HIDDEN_ON_MAP
  And roamingEnabled is false
```

- **Null settings yields inaccessible** — Treated as no public access; device is inaccessible and roaming disabled.

```gherkin
Scenario: Null settings yields inaccessible
  Given violations have no flags set
  And settings are null
  When visibility is calculated from those violations and settings
  Then forCustomer is INACCESSIBLE_AND_HIDDEN_ON_MAP
  And roamingEnabled is false
```

### 2.4 Edge: null violations

- **Null violations treated as no violations** — If violations are null, device is treated as having no violations; result depends only on settings (e.g. USABLE_AND_VISIBLE when publicAccess and showOnMap).

```gherkin
Scenario: Null violations treated as no violations
  Given violations are null
  And settings have publicAccess true and showOnMap true
  When visibility is calculated from those violations and settings
  Then forCustomer is USABLE_AND_VISIBLE_ON_MAP
  And roamingEnabled is true
```

## 3. Violations — Calculation

Scenarios for how `Violations` are computed from editor state in `checkViolations()`: operator/provider/location and show-on-map–related flags.

### 3.1 Ownership-based flags

- **Operator not assigned when ownership operator is null** — Unowned or missing operator sets `operatorNotAssigned` true.

```gherkin
Scenario: Operator not assigned when ownership operator is null
  Given the editor has ownership with operator null and provider null
  When checkViolations is called
  Then operatorNotAssigned is true
  And providerNotAssigned is true
```

- **Operator and provider assigned when ownership is set** — Both operator and provider set clears those flags.

```gherkin
Scenario: Operator and provider assigned when ownership is set
  Given the editor has ownership with operator "Op" and provider "Pr"
  When checkViolations is called
  Then operatorNotAssigned is false
  And providerNotAssigned is false
```

### 3.2 Location-based flags

- **Location missing when location is null** — `locationMissing` is true when editor has no location.

```gherkin
Scenario: Location missing when location is null
  Given the editor has location null
  When checkViolations is called
  Then locationMissing is true
```

- **Location present when location is set** — A non-null location clears `locationMissing`.

```gherkin
Scenario: Location present when location is set
  Given the editor has a valid location
  When checkViolations is called
  Then locationMissing is false
```

### 3.3 Show-on-map–related flags

- **showOnMap but no location** — `showOnMap` true and location null sets `showOnMapButMissingLocation` true.

```gherkin
Scenario: showOnMap but missing location
  Given the editor has settings with showOnMap true
  And the editor has location null
  When checkViolations is called
  Then showOnMapButMissingLocation is true
  And locationMissing is true
```

- **showOnMap but no public access** — `showOnMap` true and `publicAccess` false sets `showOnMapButNoPublicAccess` true.

```gherkin
Scenario: showOnMap but no public access
  Given the editor has settings with showOnMap true and publicAccess false
  When checkViolations is called
  Then showOnMapButNoPublicAccess is true
```

- **showOnMap off: no show-on-map violations** — When `showOnMap` is false, `showOnMapButMissingLocation` and `showOnMapButNoPublicAccess` are false even if location is missing or publicAccess is false.

```gherkin
Scenario: showOnMap false yields no show-on-map violations
  Given the editor has settings with showOnMap false
  And the editor has location null
  When checkViolations is called
  Then showOnMapButMissingLocation is false
  And showOnMapButNoPublicAccess is false
```

### 3.4 Complete and partial state

- **New device: operator, provider, location missing** — After `createDevice(id)`, operator/provider/location violations are true; show-on-map flags false if showOnMap is false.

```gherkin
Scenario: New device has operator provider and location violations
  Given a device was created with id "device-123"
  When checkViolations is called
  Then operatorNotAssigned is true
  And providerNotAssigned is true
  And locationMissing is true
  And showOnMapButMissingLocation is false
  And showOnMapButNoPublicAccess is false
```

- **Fully configured device: no violations** — Ownership set, location set, and showOnMap false (or showOnMap true with location and publicAccess) yields all violation flags false.

```gherkin
Scenario: Fully configured device has no violations
  Given the editor has ownership with operator "Op" and provider "Pr"
  And the editor has a valid location
  And the editor has default settings or settings where showOnMap implies location and publicAccess
  When checkViolations is called
  Then operatorNotAssigned is false
  And providerNotAssigned is false
  And locationMissing is false
  And showOnMapButMissingLocation is false
  And showOnMapButNoPublicAccess is false
```

- **Null safety: null ownership and location** — Editor constructed with null ownership and null location still computes violations; operator/provider/location flags true; show-on-map flags false when settings are null.

```gherkin
Scenario: Null ownership and location still produce violations
  Given the editor was built with null ownership null location and null settings
  When checkViolations is called
  Then operatorNotAssigned is true
  And providerNotAssigned is true
  And locationMissing is true
  And showOnMapButMissingLocation is false
  And showOnMapButNoPublicAccess is false
```