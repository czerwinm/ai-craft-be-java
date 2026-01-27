# [Business Feature] Site Acceptance Tests – Form Answering

**Subdomain:** Site Acceptance Tests – form answering  
**Source:** [installation-and-sat-domains.md](../installation-and-sat-domains.md#4-site-acceptance-tests--form-answering)

---

## Business Context

[Link to event storming board / frame](https://miro.com/app/board/uXjVLHsoB8Q=/?share_link_id=???&moveToWidget=???)

[Link to Mockups in Figma](https://www.figma.com/design/...)

### Intro

The **Site Acceptance Tests – Form Answering** subdomain runs the interactive “form answering” part of the installation: presenting questions to the installer in sequence, accepting answers (text, single/multi choice, photo upload), enforcing validation and mandatory completion, and feeding results back into the installation process. It does **not** own form *design* (**Managing SAT Questions**) or *which* form is used (**Assigning SAT Form to installation**); it owns the *runtime* behaviour of answering and the integrity of answers per installation.

**Component classification:**
- **Editor**: Interactive SAT form completion interface for installer (answer input, photo upload).
- **Process**: Section progression and integration with installation phases.
- **Invariant**: Enforce required fields and validation rules; report “all mandatory answered” for completion.

### Business Rules

#### Question types and input
- **Text**: free text (optional validation rules).
- **Single-choice**: pick one from predefined options.
- **Multi-choice**: pick multiple from predefined options.
- **Photo upload**: required for evidence (e.g. “make picture”); often coupled with a yes/no or short text.

#### Validation and mandatory
- **Required fields**: mandatory questions must be answered before progression or finish.
- **Validation rules** and **conditional logic** from the form definition (from Managing SAT Questions, via Assigning SAT Form) are enforced at runtime.

#### Section order and installation phases
- Form is organised in **sections**; progression follows installation phases, e.g.:
  - Initial section (e.g. contact with customer, exact place, photos).
  - After device assignment (e.g. power, meter, modem, preparation questions).
  - After boot confirmation.
  - Location (pin / GPS).
  - Final section (e.g. cleanup, no debris, photos).
- Answering subdomain reports **completion state** (all mandatory answered) so the Device installation process can allow “Finish installation.”

#### Storage and integrity
- **Answers and photos** are stored per installation and per question.
- Answers are evidence for acceptance and data quality (e.g. precise location is handled in installation process; form may capture “place confirmed” or similar).

#### Integrations
- **Device installation process**: receives current phase; enforces “all mandatory questions answered” before finish (Process/Invariant).
- **Assigning SAT Form to installation**: gets form definition (sections, questions, types, validation) for this installation (Read).
- **Managing SAT Questions**: question/field metadata is used via the assigned form (Read).

#### Actor interactions
- **Installer**: primary actor; reads questions, enters answers, uploads photos, moves through sections in the order defined by the installation process and form structure.

---

## API Contract FE vs BE

_Adjust to actual contract once endpoints are defined. Below reflects subdomain responsibilities._

### 1. Get form and current progress for installation (for installer UI)

```
GET /installations/{installationId}/sat-answers

Response:
{
  "formDefinition": { ... },     // from assigned form
  "currentSectionId": "...",
  "answers": [
    {
      "questionId": "...",
      "value": "..." | [ ... ] | { "uploadId": "..." }
    }
  ],
  "allMandatoryAnswered": false | true
}
```

### 2. Submit answer for a question

```
PUT /installations/{installationId}/sat-answers/questions/{questionId}

{
  "value": "..." | [ "option1", "option2" ] | { "photoUploadId": "..." }
}

Response: 200 when valid; 400 when validation fails or question not in current form.
```

### 3. Upload photo for a question

```
POST /installations/{installationId}/sat-answers/questions/{questionId}/photo

Content-Type: multipart/form-data (file)

Response: { "uploadId": "...", "url": "..." } or similar; client then submits via PUT above.
```

### 4. Get completion status (for Installation Process completion check)

```
GET /installations/{installationId}/sat-answers/completion

Response:
{
  "allMandatoryAnswered": true | false,
  "missingMandatoryQuestionIds": [ ... ]
}
```

### 5. Progress to next section (optional; may be implicit with phase transitions)

```
POST /installations/{installationId}/sat-answers/sections/{sectionId}/complete

Response: 200 when section is complete (mandatory in section answered); 400 otherwise.
```

---

## Technical Requirements

For this feature follow the [Technical Standard](/src/docs/architecture-and-patterns.md) and the docs referenced in [AGENTS.md](/AGENTS.md) (domain-model, ports, adapters).

---

## In Scope

- Domain model for SAT answers (per installation, per question), validation against form definition, and completion rules, with unit tests.
- Enforcement of required fields and validation rules (including conditional logic) with unit tests.
- Integration with **Assigning SAT Form to installation** (retrieve form definition for installation).
- Persistence for answers and photo references per installation.
- HTTP adapter fulfilling the API contract for answer submission, photo upload, and completion status.
- Business module (vertical slice) for Site Acceptance Tests – form answering.
- End-to-end tests for answer submission, validation, and completion scenarios.

---

## Out of Scope

- Form **content** definition (sections, questions, types) — owned by **Managing SAT Questions**.
- “Which form for this installation” — owned by **Assigning SAT Form to installation**.
- Installation phase orchestration and “Finish installation” — owned by **Device Installation Process**; this subdomain only reports completion and stores answers.
- Full installer mobile/web UI (in-scope: API; UI can be separate initiative).
- Photo storage backend details (e.g. S3, blob store); in-scope: API contract and references; storage adapter can be specified elsewhere.
- JWT/security implementation (reuse existing where applicable).
