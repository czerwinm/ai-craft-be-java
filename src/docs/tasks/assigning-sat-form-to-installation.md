# [Business Feature] Assigning SAT Form to Installation

**Subdomain:** Assigning SAT Form to installation  
**Source:** [installation-and-sat-domains.md](../installation-and-sat-domains.md#3-assigning-sat-form-to-installation)

---

## Business Context

[Link to event storming board / frame](https://miro.com/app/board/uXjVLHsoB8Q=/?share_link_id=???&moveToWidget=???)

[Link to Mockups in Figma](https://www.figma.com/design/...)

### Intro

The **Assigning SAT Form to Installation** subdomain decides *which* SAT form variant applies to a given installation and attaches that form to the work order/installation. It uses installation context (customer segment, country, equipment category, optional customer id) to resolve the form and may use fallbacks when no exact variant exists. It ensures the installer receives the correct form definition for **Site Acceptance Tests – form answering**.

Resolution happens **early**, on the very beginning of the process, before the installer starts.

**Component classification:**
- **Policy/Calculations**: Form selection logic (segment, country, category, customer; fallbacks)
- **Process**: Bind resolved form to work order/installation

### Business Rules

#### Resolution criteria
- **Segment**: private, corporate, municipality (from ownership/customer).
- **Country**: e.g. Poland, Germany.
- **Equipment category**: e.g. wall-mounted vs. ground-installed.
- **Customer id** (optional): per-customer overrides (e.g. “this corporate client has a few questions extra”).

#### Selection policy
- **Most specific match wins** (e.g. segment + country + category + customer override takes precedence over segment-only default).
- **Fallback** when no variant matches: define default (e.g. segment-only or global default); behaviour on “no match” must be explicit (e.g. 404 + fallback option or guaranteed default).

#### Binding
- Resolved form (variant id + definition) is **bound** to the work order/installation so the answering process always uses the right form.
- “Form for this installation” is exposed to the Device installation process and to the form-answering flow.

#### Integrations
- **Device installation process**: receives “installation context” (segment, country, category, customer); returns and attaches the chosen form (Query/Command).
- **Managing SAT Questions**: gets form definitions and variant metadata for resolution and for passing to answering (Read/Query).
- **Site Acceptance Tests – form answering**: supplies the form definition (sections, questions, types, validation) for the current installation (Provide Read).

#### Actor interactions
- **System / Order planning**: triggers resolution when an installation is planned or a work order is prepared (ownership and context are known).
- **Installer**: indirect beneficiary; receives the already-assigned form when opening the installation.

---

## API Contract FE vs BE

_Adjust to actual contract once endpoints are defined. Below reflects subdomain responsibilities._

### 1. Resolve form variant by criteria (for Device Installation Process / Order planning)

```
GET /sat-forms/variant?segment={segment}&country={country}&equipmentCategory={category}&customerId={customerId}

Response (200):
{
  "formVariantId": "...",
  "version": "...",
  "metadata": { ... }
}

Response (404): when no variant matches and no fallback is configured.
Response (200 with fallback): when fallback is used, include indicator "fallbackApplied": true.
```

### 2. Bind form to installation / work order (command side)

```
POST /installations/{installationId}/sat-form

{
  "formVariantId": "..."
}

Response: 200 when bound; 400 when variant not found or installation invalid.

Alternative: resolution and binding can be one step when starting preparation for an installation (e.g. POST /installations/work-orders/{id}/prepare with body { segment, country, equipmentCategory, customerId } returns form variant and binds it).
```

### 3. Get form for installation (for Form Answering / Installation Execution)

```
GET /installations/{installationId}/sat-form

Response:
{
  "formVariantId": "...",
  "definition": { ... }   // same shape as GET /sat-forms/definitions/{formVariantId}
}
```

---

## Technical Requirements

For this feature follow the [Technical Standard](/src/docs/architecture-and-patterns.md) and the docs referenced in [AGENTS.md](/AGENTS.md) (domain-model, ports, adapters).

---

## In Scope

- Domain model for form variant resolution criteria, selection policy, and binding to installation/work order, with unit tests.
- Form selection policy (resolution by segment, country, category, customer; fallback rules) with unit tests.
- Integration with **Managing SAT Questions** (read form definitions and variant metadata).
- Persistence or in-memory store for “installation ↔ form variant” binding (alignment with installation persistence).
- HTTP adapter fulfilling the API contract for variant resolution and form-for-installation retrieval.
- Business module (vertical slice) for Assigning SAT Form to installation.
- End-to-end tests for variant resolution and form binding scenarios (including fallback and per-customer override).

---

## Out of Scope

- Definition and storage of form **content** (sections, questions, validation) — owned by **Managing SAT Questions**; this subdomain only resolves and binds.
- Implementation of **Device Installation Process** or **Site Acceptance Tests – form answering** (this subdomain provides form to them).
- Full installer UI (in-scope: API; UI can be separate initiative).
- JWT/security implementation (reuse existing where applicable).
