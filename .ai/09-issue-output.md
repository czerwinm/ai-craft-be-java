# Event Storming Diagram Generation Prompt

You are tasked with analyzing code (primarily domain models, aggregates, or process managers) and generating Event Storming diagrams in **SVG** format.

## Objective

Create visual Event Storming diagrams that represent:
- **Commands** (blue cards) - actions that trigger behavior
- **Domain Events** (orange cards) - facts that happened in the domain
- **Business Rules** (yellow cards) - logic/policies that govern state transitions
- **Actors** (small yellow sticky notes) - who triggers commands
- **External Systems** (pink cards) - external dependencies
- **Aggregates/Domain Objects** (large yellow boundary) - grouping business rules

## Code Scope

**The user will provide the specific code scope** to analyze. This may include:
- Single class/aggregate
- Multiple related classes
- Entire bounded context
- Specific methods or workflows

**Focus only on the code provided by the user.** Do not make assumptions about code outside the given scope.

## Analysis Instructions

### 1. Identify Commands
- Look for **public methods** that modify state
- Method names often indicate commands: `createDevice()`, `updateOwnership()`, `assignInstaller()`
- Extract command name from method signature
- Note: Commands may originate from **Actors** (users, admins) or **External Systems** (APIs, services)

### 2. Identify Domain Events
- Look for **event creation** in code: `events.add(new DomainEvent.DeviceCreated(...))`
- Event names typically follow pattern: `[Entity][Action]` (e.g., `DeviceCreated`, `OwnershipChanged`)
- Track when events are emitted (after which command/business rule)

### 3. Identify Business Rules
- **Name business rules** based on the logic inside methods
- Examples:
  - Validation logic: "Device ID must not be empty"
  - Conditional logic: "Only emit event if ownership changed"
  - State guards: "Cannot change in final state"
  - Calculation logic: "Calculate visibility based on violations"
- Group rules by the **Domain Object** they belong to (e.g., `DeviceConfigurationEditor`, `InstallationProcess`)

### 4. Extract Additional Context
- **Actors**: Check code comments, method documentation, or related markdown files
- **External Systems**: Look for external API calls, messaging, or integration points
- **Read-Only Operations**: Methods that don't emit events (queries/views)

## Diagram Structure

### Flow Pattern
```
[Actor/External System] → [Command] → [Business Rule(s)] → [Domain Event(s)]
```

### Card Colors & Shapes
- **Blue Rectangle**: Command (#4A90E2)
- **Orange Rectangle**: Domain Event (#FF9500)
- **Yellow Rectangle**: Business Rule (#FFD700)
- **Small Yellow Note**: Actor (#FFEB3B)
- **Pink Rectangle**: External System (#FF69B4)
- **Large Yellow Boundary**: Aggregate/Domain Object name (stroke: #F4C542, fill: #FFFBEA)

### Layout Rules
1. **Left-to-right flow** representing time progression
2. **Group business rules** inside a large yellow box labeled with the Domain Object name
3. **Arrows show dependencies**:
   - Command → Business Rule(s)
   - Business Rule → Domain Event(s)
4. Multiple events from one rule should branch out
5. State transitions should be visually clear

## HTML Output Format

### Technical Requirements
- **Document**: Wrap diagrams in proper HTML (`<!DOCTYPE html>`, `<html>`, `<body>`)
- **ViewBox**: SVG inside HTML should still use responsive viewBox (e.g., `viewBox="0 0 1200 800"`)
- **Namespace**: Include proper SVG namespace within the HTML file
- **Styling**: Inline styles or `<style>` blocks inside `<head>`
- **Accessibility**: Add `<title>` and `<desc>` inside the SVG
- **Text**: Use `<text>` elements with appropriate font sizes and wrapping

### HTML Structure Template

Template assets now live in `.ai/event-storming-template.html`, so you can embed the rendered HTML file directly.

<iframe src="./event-storming-template.html" width="100%" height="480" style="border:1px solid #bfbfbf; border-radius:8px;"></iframe>

### Layout Guidelines
- **Spacing**: 150-200px between columns, 80-100px between rows
- **Card dimensions**: 
  - Commands/Events: 120x60px
  - Business Rules: 140x70px
  - Actors: 80x40px (sticky note shape with folded corner)
  - External Systems: 120x60px
- **Text wrapping**: Break long text into multiple `<tspan>` lines
- **Margins**: 50px from viewBox edges

## Example HTML Diagram

### Based on DeviceConfigurationEditor example:

<iframe src="./event-storming-device-configuration.html" width="100%" height="560" style="border:1px solid #bfbfbf; border-radius:8px;"></iframe>

## Special Considerations

- **State Machines**: Track status transitions (e.g., PENDING → ASSIGNED → COMPLETED)
- **Conditional Events**: Show branching when events only fire under certain conditions
- **Error Cases**: Include validation failures as separate paths if significant
- **Missing Information**: 
  - If actors are not in code, check documentation or use generic "User"
  - If external systems are not explicit, note them as "External API" or "Unknown System"
- **Read-Only Operations**: Show queries/projections in lighter blue to distinguish from commands

## Deliverable

Provide:
1. **Complete HTML diagram** (SVG inside an HTML wrapper) with proper structure and styling
2. **Embedded legend** explaining colors and shapes
3. **Comments** inside the SVG for major sections
4. **Responsive viewBox** that scales properly
5. **Notes** (as text comment) on any assumptions made about actors or external systems

---

## Quick Reference: SVG Color Codes

| Element | Fill Color | Stroke Color | Usage |
|---------|------------|--------------|-------|
| Command | #4A90E2 | #2E5C8A | User/system actions |
| Domain Event | #FF9500 | #CC7700 | Facts that happened |
| Business Rule | #FFD700 | #CCB000 | Logic/policies |
| Actor | #FFEB3B | #C5B800 | Who triggers commands |
| External System | #FF69B4 | #CC5490 | External dependencies |
| Aggregate Box | #FFFBEA | #F4C542 | Groups business rules |
| Read-Only Query | #90CAF9 | #1976D2 | Queries (optional) |
| Arrow | - | #333 | Flow direction |