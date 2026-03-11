# Legacy Components and Deprecation Plan

This document describes legacy entities and flows that predate the dev portal's interactive form builder. Once the dev portal flow (multistep, conditional forms, new routes) is the primary component clients use, these can be deprecated.

---

## Form Entity

**What it is:** A MongoDB document representing an "application form" linked to a PDF file. Stored in `formDao`, keyed by `fileId`.

**What it stores:**
- `fileId` — links to the PDF file
- `metadata` — title, description, etc.
- `body` — `FormSection` containing `FormQuestion[]`

**FormQuestion structure:** A flat Java object per field:
- `questionName` — PDF AcroForm field name
- `type` — FieldType (TEXT_FIELD, CHECKBOX, DATE_FIELD, etc.)
- `directive` — e.g. "client.currentName.first", "On"
- `options` — for radio/select
- `defaultValue`, `answerText`, etc.

**Where Form is created:**
- `CreateApplicationService` — when creating an application (empty body)
- `UploadAnnotatedPDFServiceV2` — when uploading an annotated PDF (parsed from PDF)

**Who uses Form today:**
- `get-questions-2` — loads Form, iterates formQuestions, matches directives to user profiles, returns `fields` + `resolvedProfiles`
- `saveInteractiveFormConfig` — when saving in builder, updates Form.body with generated FormQuestion[]
- `fill-pdf-2` — **no longer uses Form** (detached as of refactor); iterates `formAnswers` directly
- `upload-signed-pdf-2` — saves the `filledForm` (a *new* Form created by FillPDFServiceV2 as the record of what was filled)

---

## InteractiveFormConfig (Not Legacy)

**What it is:** The source of truth for the dev portal form builder. Stored in `interactiveFormConfigDao`, keyed by `fileId`.

**What it stores:**
- `jsonSchema` — JSON Schema for the form
- `uiSchema` — JSON Forms UI schema (Categorization, Control, steps, etc.)
- `builderState` — questions, output fields, auto-fill fields, signature placements

**Who uses it:**
- `get-interactive-form-config` — returns jsonSchema, uiSchema to client
- `saveInteractiveFormConfig` — saves when user saves in builder
- Client — uses jsonSchema + uiSchema to render the form via react-jsonforms

---

## Form vs InteractiveFormConfig

| Aspect | Form | InteractiveFormConfig |
|--------|------|------------------------|
| Structure | Flat FormQuestion[] | Nested jsonSchema + uiSchema |
| Used for rendering | No (legacy clients may have) | Yes (dev portal) |
| Used for fill-pdf | No (detached) | No (client sends formAnswers) |
| Source of truth | Derived from schema when saving | Primary |

Form.body (formQuestions) is a **derived copy** — generated from jsonSchema + uiSchema by `InteractiveFormConfigUtils.generateFormQuestions()` when saving. It is redundant for the dev portal flow.

---

## generateFormQuestions

**Location:** `Form/InteractiveFormConfigUtils.java`

**What it does:** Walks jsonSchema + uiSchema (as JSON), finds Controls with `options.pdfField`, produces `FormQuestion[]`.

**When called:** From `saveInteractiveFormConfig` — updates Form.body with the result.

**Legacy role:** Feeds Form for get-questions-2 and (formerly) fill-pdf. No longer needed for fill-pdf.

---

## get-questions-2

**What it does:** Loads Form, iterates formQuestions, matches directives to user/org profiles, returns `fields` + `resolvedProfiles`.

**What the dev portal uses:** Only `resolvedProfiles` (flattened client/worker/org data for directive resolution). The `fields` array is ignored.

**Legacy role:** The `fields` array comes from Form and may be used by other clients (e.g. mobile). The `resolvedProfiles` could be served by a simpler "get profiles for client" endpoint without Form.

---

## Deprecation Plan

Once the dev portal flow (multistep, conditional forms, new routes) is the component clients use:

### Phase 1: New Routes (Future)
- New fill route that only needs `applicationId` + `formAnswers` (already done for fill-pdf-2)
- New "get resolved profiles" route that returns flattened user/org data without Form
- Dev portal uses InteractiveFormConfig + new routes only

### Phase 2: Stop Updating Form on Save
- In `saveInteractiveFormConfig`, remove the block that calls `generateFormQuestions` and updates Form.body
- Form.body will go stale for new saves; get-questions-2 will return outdated fields (if any client still uses them)

### Phase 3: Deprecate Form for Template Forms
- Audit all consumers of Form (get-questions-2, CreateApplicationService, UploadAnnotatedPDFServiceV2)
- Migrate or remove dependencies
- Form may still be needed for `filledForm` (the record saved when user uploads signed PDF) — that's a different use case (audit trail of filled answers)

### Phase 4: Remove Form / FormQuestion for Template Flow
- If no client needs Form.body for template forms, remove:
  - Form.body update in saveInteractiveFormConfig
  - generateFormQuestions (or repurpose)
  - get-questions-2's Form iteration (replace with simpler profiles endpoint)
- Keep Form entity only if needed for filledForm records on upload

---

## Summary

| Component | Status | Action |
|-----------|--------|--------|
| fill-pdf-2 | Detached from Form | Done — iterates formAnswers only |
| Form (template) | Legacy | Deprecate when dev portal flow is primary |
| Form.body / formQuestions | Redundant for dev portal | Stop updating on save; remove when safe |
| get-questions-2 | Partially legacy | Replace with simpler profiles endpoint |
| InteractiveFormConfig | Current | Keep — source of truth for builder |
| filledForm (on upload) | Still needed | Keep — audit record of filled PDF |
