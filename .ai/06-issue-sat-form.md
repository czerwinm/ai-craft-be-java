---
name: Business Feature
about: Template for ensuring Github Issues for Business Feature are ready for implementation
title: '[Business Feature] SAT Form (Questionnaire)'
labels:
    - 'business-feature'
assignees: ''
---

## Business Context

[Link to event storming board / frame](https://miro.com/app/board/uXjVLHsoB8Q=/?share_link_id=???&moveToWidget=???)

[Link to Mockups in Figma](https://www.figma.com/design/...)

### Intro
The SAT (Site Acceptance Test) Form subdomain handles the dynamic data collection required during the installation process. It acts as a specialized "data collector" that is separate from the main process flow state.

Its primary purpose is to:
- Render appropriate questions based on the installation context (e.g., client type, location).
- Collect answers in various formats (text, photos, selections).
- Validate input and manage internal form logic (e.g., conditional visibility of sections).

### Business Rules

#### Form Rendering & Logic
- The form structure (sections and questions) is dynamic and determined by rules defined in the **Section Management** subdomain.
- The specific form to render is decided based on the **Installation Context** (Installation ID, Client Type, etc.) provided by the **Installation Workflow**.
- Sections may be conditionally visible based on answers to previous questions.

#### Data Collection & Validation
- The system must support various answer types:
    - Text input
    - Single/Multi selection
    - Photo uploads
- All required fields must be validated before the form can be submitted.
- Photos taken during installation must be uploaded to an external **Blob Storage**.

#### Event Emission
- `FormStarted`: Emitted when the installer begins filling out the form.
- `AnswersUpdated`: Emitted when answers are saved/updated (draft state).
- `FormSubmitted`: A Domain Event emitted when the form is successfully completed and validated.

## API Contract FE vs BE:

1. Get Form Definition for Installation:
```
GET /installations/{installationId}/sat-form

Response:
{
  "installationId": "inst-123",
  "sections": [
    {
      "id": "sec-1",
      "title": "Site Inspection",
      "questions": [
        {
          "id": "q-1",
          "type": "TEXT",
          "label": "Distance to power source",
          "required": true
        },
        {
          "id": "q-2",
          "type": "PHOTO",
          "label": "Photo of mounting surface",
          "required": true
        }
      ]
    }
  ]
}
```

2. Save/Update Answers:
```
PUT /installations/{installationId}/sat-form/answers

Request:
{
  "answers": [
    {
      "questionId": "q-1",
      "value": "5 meters"
    },
    {
      "questionId": "q-2",
      "value": "https://blob-storage.com/photo-123.jpg"
    }
  ]
}
```

3. Submit Form:
```
POST /installations/{installationId}/sat-form/submit

Request:
(Empty body, relies on saved state or includes final validation check)
```

## Technical Requirements

For that feature our [Technical Standard](/src/docs/architecture-and-patterns.md)

## In Scope

- Domain Model for Form, Sections, Questions, and Answers.
- Logic for selecting the correct form definition based on Installation Context.
- Integration with **Section Management** (Read) to fetch definitions.
- Integration with **Installation Workflow** (Read) to get context.
- Integration with **Blob Storage** (Write) for photo uploads.
- Validation logic for answers.
- End-to-end tests for form rendering and submission scenarios.

## Out of Scope

- The admin interface for defining sections and rules (handled by **Section Management**).
- The core state machine of the installation process (handled by **Installation Workflow**).
- User management or authentication of the Installer.

---

# SAT Form — Gherkin Scenarios

## 1. Form Rendering

### 1.1 Load correct form based on context
```gherkin
Scenario: Load form for Corporate Client
  Given an installation exists for a "Corporate" client
  And the Section Management defines a "Corporate Section"
  When the SAT form is requested for this installation
  Then the response includes the "Corporate Section"
```

## 2. Data Collection

### 2.1 Validate required fields
```gherkin
Scenario: Cannot submit form with missing required answers
  Given a form has a required question "q-1"
  And "q-1" has not been answered
  When the installer attempts to submit the form
  Then the submission is rejected with a validation error
```

### 2.2 Photo Upload
```gherkin
Scenario: Upload photo answer
  Given a question "q-photo" requires a photo
  When the installer uploads an image file
  Then the image is stored in Blob Storage
  And the answer for "q-photo" contains the storage URL
```

## 3. Event Emission

### 3.1 Form Submission
```gherkin
Scenario: Emit FormSubmitted on successful completion
  Given all required questions are answered validly
  When the installer submits the form
  Then the form status changes to "Submitted"
  And a FormSubmitted event is emitted
```
