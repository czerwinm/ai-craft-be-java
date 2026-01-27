# [Business Feature] Managing SAT Questions

**Subdomain:** Managing SAT Questions  
**Source:** [installation-and-sat-domains.md](../installation-and-sat-domains.md#2-managing-sat-questions)

---

## Business Context

[Link to event storming board / frame](https://miro.com/app/board/uXjVLHsoB8Q=/?share_link_id=???&moveToWidget=???)

[Link to Mockups in Figma](https://www.figma.com/design/...)

### Intro

The **Managing SAT Questions** subdomain owns the definition and structure of Site Acceptance Test (SAT) *content*: sections, questions, field types, validation rules, conditional logic, and allowed answers. It supports multiple form *variants* by customer segment, country, equipment category, and per-customer overrides. It does **not** decide “which form for this installation” (that is **Assigning SAT Form to installation**); it provides the question set and metadata that form-assignment and form-answering use.

**Component classification:**
- **Read Model**: Form definition query and retrieval
- **Editor** (admin): Create/edit form templates, sections, questions, variants

### Business Rules

#### Form template structure
- Form templates consist of **sections** and **questions** with explicit order.
- **Field types**: text, single-choice, multi-choice, photo upload, etc.
- **Validation rules** and **conditional logic** apply per question/field.
- Form templates are **versioned**; historical changes are tracked.

#### Form variants
- Variants are defined by:
  - **Segment**: private, corporate, municipality
  - **Country**: e.g. Poland, Germany
  - **Equipment category**: e.g. wall-mounted vs. ground-installed
- **Per-customer customizations**: additional questions or overrides for specific clients.
- Different procedure and questions for “equipment hangs on the wall” vs “on the ground”; different form for Poland vs Germany; some customers may have specific extra questions.

#### Content rules (from transcript)
- Questions often require **evidence** (e.g. “make picture”), especially for safety/site (“nothing to damage in proximity”, “cleanup”, etc.).
- Exactly one form variant is **defined** for a given criteria set (segment, country, category, customer); resolution “which form for this installation” is owned by **Assigning SAT Form to installation**.

#### Integrations
- **Assigning SAT Form to installation**: supplies form definitions, question sets, field specs for a given variant (Query/Read).
- **Site Acceptance Tests – form answering**: supplies question definitions, types, validation, options for rendering and validation (Query/Read).

#### Actor interactions
- **Administrator / form designer**: creates and edits form templates, sections, questions, variants, and validation rules.

---

## API Contract FE vs BE

_Adjust to actual contract once endpoints are defined. Below reflects subdomain responsibilities._

### 1. Get form definition by variant id (for Assigning SAT Form / Form Answering)

```
GET /sat-forms/definitions/{formVariantId}

Response:
{
  "formVariantId": "...",
  "version": "...",
  "sections": [
    {
      "id": "...",
      "title": "...",
      "order": 1,
      "questions": [
        {
          "id": "...",
          "type": "text" | "singleChoice" | "multiChoice" | "photoUpload" | "...",
          "label": "...",
          "required": true | false,
          "validationRules": { ... },
          "conditionalLogic": { ... },
          "options": [ ... ]
        }
      ]
    }
  ]
}
```

### 2. Admin: list form templates

```
GET /admin/sat-forms/templates

Response: list of template id, name, version, variant criteria summary.
```

### 3. Admin: create form template

```
POST /admin/sat-forms/templates

{
  "name": "...",
  "sections": [ ... ]
}

Response: template id, version.
```

### 4. Admin: get form template

```
GET /admin/sat-forms/templates/{templateId}

Response: full template with sections and questions.
```

### 5. Admin: update form template

```
PATCH /admin/sat-forms/templates/{templateId}

{
  "sections": [ ... ],
  "questions": [ ... ]
}

Response: new version.
```

### 6. Admin: list / create / update variants

```
GET    /admin/sat-forms/templates/{templateId}/variants
POST   /admin/sat-forms/templates/{templateId}/variants
PATCH  /admin/sat-forms/variants/{variantId}
```

_Detail request/response schemas when API is finalised._

---

## Technical Requirements

For this feature follow the [Technical Standard](/src/docs/architecture-and-patterns.md) and the docs referenced in [AGENTS.md](/AGENTS.md) (domain-model, ports, adapters).

---

## In Scope

- Domain model for form templates, sections, questions, field types, variants, and unit tests.
- Validation rules and conditional logic representation with unit tests.
- Versioning of templates and tracking of historical changes.
- Persistence adapter for form templates and variants.
- HTTP adapter fulfilling the API contract for form definition retrieval and admin operations.
- Business module (vertical slice) for Managing SAT Questions.
- End-to-end tests for form definition retrieval and admin CRUD scenarios.

---

## Out of Scope

- **Form selection policy** (“which form for this installation”) — owned by **Assigning SAT Form to installation**; this subdomain only stores and serves definitions.
- Full admin UI for form design (in-scope: API for admin operations; UI can be separate initiative).
- Implementation of **Device Installation Process**, **Assigning SAT Form to installation**, or **Site Acceptance Tests – form answering** (they consume this subdomain’s API).
- JWT/security implementation (reuse existing where applicable).
- Rich form builder UX (drag-and-drop, visual conditional logic editor).
