# AI Software Craftsmanship - Notatki

**Prowadzący:** Michał Michaluk  
**Data szkolenia:** 26-28 stycznia 2026

## Spis treści
1. [Repozytoria](#repozytoria)
2. [Zadania](#zadania)
3. [Narzędzia i Technologie](#narzędzia-i-technologie)
4. [Konfiguracja projektu i AI](#konfiguracja-projektu-i-ai)
5. [Workflow pracy z AI](#workflow-pracy-z-ai)
6. [MCP (Model Context Protocol)](#mcp-model-context-protocol)
7. [Skille i customowe agenty](#skille-i-customowe-agenty)
8. [Architektura i wzorce](#architektura-i-wzorce)
9. [Testowanie z AI](#testowanie-z-ai)
10. [Praca z Legacy Code](#praca-z-legacy-code)
11. [UI i Frontend](#ui-i-frontend)
12. [Słowniczek](#słowniczek)
13. [TODO](#todo)

---

## Repozytoria

| Opis | Link |
|------|------|
| Mój fork | https://github.com/czerwinm/ai-craft-be-java |
| Repo prowadzącego | https://github.com/michal-michaluk/ai-craft-be-java |

---

## Zadania

### Lista zadań i kolejność
- Lista zadań: https://github.com/michal-michaluk/ai-craft-be-java/issues
- **Rekomendowana kolejność:** 1, 3, 4 → potem 5 → na koniec 2 (żeby dodać persystencję gdy domena jest już gotowa)
- **Uwaga:** W zadaniu 1 może być błędna wersja Gradle – trzeba poprawić

### Możliwe procesy do opracowania
- Installation process (z nagraną rozmową po angielsku → transkrypt do analizy przez AI)
- SAT form editor

### Issue 6: Installation Process
- Link: https://github.com/michal-michaluk/ai-craft-be-java/issues/6
- Dobry opis tworzenia **custom subagentów**:
  - Cursor: na podstawie [Cursor subagents docs](https://cursor.com/docs/context/subagents)
  - Copilot: na podstawie [Copilot agent docs](https://docs.github.com/en/copilot/how-tos/use-copilot-agents/coding-agent/create-custom-agents#creating-a-custom-agent-profile-in-jetbrains-ides)
- Użyć `Slice.md` jako blueprint
- Praca z transkryptem: `transcript-installation-sat.txt`
- Event Storming → podział na subdomeny (obrazki w issue)

### Issue 7: MCP (Model Context Protocol)
- Integracja MCP z Cursor
- Konfiguracja: `cmd + shift + p` → mcp settings

### Issue 8: Architecture Description Agent
- Link: https://github.com/michal-michaluk/ai-craft-be-java/issues/8
- Cel: Stworzenie agenta do generowania opisu architektury systemu

### Issue 9: Reverse Engineering Agent
- Link: https://github.com/michal-michaluk/ai-craft-be-java/issues/9
- Metoda: **Reverse Engineering**
- Podejście: Opisujemy dokładnie co jest na wejściu (input) i co ma być na wyjściu (output), a AI na tej podstawie generuje prompt/agenta

---

## Narzędzia i Technologie

### IDE z AI

| Narzędzie | Opis |
|-----------|------|
| **Cursor** | Główne narzędzie, bardzo dobre review |
| **OpenCode** | Opensource, konsolowy - https://opencode.ai/ |
| **IntelliJ + Copilot** | AI Chat z możliwością wyboru modelu (Claude Code, OpenCode itp.) |

### Modele AI
- **Anthropic (Claude)** – główny model do kodowania
- **Gemini** – świetny do researchu i eksploracji pomysłów
- **Gemini Deep Research** – "miażdży" w researchu, szukanie rozwiązań, pomysłów

### Funkcje Cursor

| Funkcja | Opis |
|---------|------|
| **Work tree** | 4 agenty na 4 różnych branczach lokalnie, potem merge |
| **Review** | Bardzo dobre code review, wyłapuje dużo rzeczy |
| **Agent Review** | Po lewej w sekcji commitów → "Find Issues" |
| **Quality gate** | Nie przerywaj, aż wszystkie testy przejdą na zielono |
| **Ask** | Eksplorowanie pomysłu, dopytywanie o wymagania |
| **Plan** | Przygotowanie planu (łatwiej zreviewować niż sam kod) |
| **Agent** | Tworzenie kodu |
| **Debug** | Tryb AI do trudnych sytuacji (logi, hipotezy, wskazówki) |

**Przydatne ustawienia Cursor:**
- Przejrzeć allowlisty i settingsy
- Dodać skróty do chowania zakładek (chat window, pliki)
- Śledzić release notes
- Prawym przyciskiem → "Add folder to workspace" (praca na wielu projektach)

### Inne narzędzia

| Narzędzie | Opis |
|-----------|------|
| **Playwright** | Automatyzacja przeglądarki (dostępne przez MCP) |
| **Prompt Cowboy** | https://www.promptcowboy.ai/ - prompt/context engineering |
| **n8n** | https://n8n.io/ - automatyzacja workflow |
| **Manus** | https://manus.im/ - drogie narzędzie, samo klika na kompie |
| **bolt.new** | Generowanie propozycji UI z kodem |
| **Lovable** | Prototypowanie UI |

### Prompt Cowboy - tryby
- **Deep Research Prompt** – do researchu z minimalnym ryzykiem halucynacji
- **CustomGPT/Agent Prompt** – do tworzenia agentów

### Skróty
- `/` – wywoływanie komend
- `/ <nazwa>` – wywołanie subagenta w Cursor

---

## Konfiguracja projektu i AI

### AGENTS.md (standard dla wszystkich IDE)
- Główny plik konfiguracji dla agentów AI
- Można rozdzielić na mniejsze pliki: `src/docs/{name}.md`
- W każdym pliku opisane buzz wordy i konwencje
- **Uwaga:** W Copilot + IntelliJ jeszcze słabo działa
- Warto dopytać agenta czy odczytuje automatycznie – jeśli nie, dodać osobny plik .md jako szablon
- Lokalizacja instrukcji: `/src/docs/` – jak agent ma tworzyć kod

### GitHub Copilot
- Plik konfiguracyjny: `.github/copilot-instructions.md`
- Można pisać własnych agentów (np. do generowania dokumentacji, review)
- Możliwość utworzenia globalnego repo z agentami/promptami

### Struktura folderów dla AI
- Dodać folder `.ai/` na instrukcje
- **Warto dodać do .gitignore:**
  - `.cursor/`
  - `agents.md`
  - Pliki `.md` z instrukcjami (opcjonalnie)

### Lokalizacja agentów w Cursor
- `.cursor/agents/` – folder na customowe agenty

### Reguły AI
- https://10xrules.ai/ – zbiór reguł do AI
- Architektoniczne instrukcje są **bardzo ważne** – trzymają w ryzach jak będzie pisany kod

### Quality Assurance (QA)
- **Pre-commit hooks** – uruchamianie lintera (np. `./gradlew check`) przed zatwierdzeniem zmian

---

## Workflow pracy z AI

### Workflow Michała (rekomendowany)

```
┌─────────────────────────────────────────────────────────────────────┐
│                        WORKFLOW MICHAŁA                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  GEMINI (eksploracja)                                               │
│     │                                                                │
│     ├─► Szukanie rozwiązań, pomysłów                                │
│     ├─► Rozmowa, rozpatrywanie różnych sytuacji                     │
│     ├─► Deep Research (bardzo dobry!)                               │
│     │                                                                │
│     ▼                                                                │
│  Poproś o plik .md z podsumowaniem                                  │
│     │                                                                │
│     ▼                                                                │
│  W nowym oknie: sprawdzenie pod kątem security                      │
│     │                                                                │
│     ▼                                                                │
│  CURSOR                                                              │
│     │                                                                │
│     ├─► ASK - eksplorowanie pomysłu                                 │
│     │                                                                │
│     ├─► PLAN - przygotowanie planu                                  │
│     │         (łatwiej zreviewować niż kod,                         │
│     │          ogranicza koszty)                                    │
│     │                                                                │
│     ├─► AGENT - implementacja                                       │
│     │                                                                │
│     ├─► DEBUG (opcjonalnie) - w trudnych sytuacjach                │
│     │         (dodaje logi, tworzy hipotezy,                        │
│     │          daje wskazówki jak naprawić)                         │
│     │                                                                │
│     └─► REVIEW - po zmianach                                        │
│                 (sekcja "Agent Review" → Find Issues                │
│                  lub przycisk Review przy zmianach)                 │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### Workflow ze specyfikacją biznesową

1. Masz specyfikację od biznesu (np. PDF)
2. Użyj **markitdown** (MCP) do konwersji PDF → MD
3. **Ask** – dopytaj co musisz zrobić i jakie są wymagania biznesu
4. Ciągnij i dopytuj co jest używane do czego w codebase
5. **Plan** – zrozumienie gdzie to wpleść w kod i jak zaimplementować
6. **Agent** – tworzenie kodu

### Kolejność pisania kodu
1. **Modele dziedzinowe** (dobrze przetestowana domena)
2. **Serwisy**
3. **Adaptery** (persistence, REST)

### Wskazówki do pracy z AI
- Pokazuj przykłady (example-driven approach)
- Podawaj oczekiwany format odpowiedzi
- Twórz agenta do tworzenia agentów, komend, skilli
- Po zaimplementowaniu domeny → dopytaj agenta o **dodatkowe testy**
- Wrzucaj opisy tasków, wykresiki, obrazki – składaj do kupy kontekst
- DevOps: zawsze przypominaj o **bezpieczeństwie**, rób customowe instrukcje security

---

## MCP (Model Context Protocol)

### Konfiguracja
- **Cursor:** `cmd + shift + p` → mcp settings
- **IntelliJ:** MCP Registry (context7)
- **Uwaga:** Uważać co się instaluje, dodawać tylko to co potrzebujemy
- Są serwery **zdalne** i **lokalne** – ogarnąć jak instalować

### Polecane serwery MCP

| Narzędzie | Opis |
|-----------|------|
| **exa** / **context7** | Sprawdzanie zależności, aktualności bibliotek |
| **Playwright** | Automatyzacja przeglądarki |
| **Crystal DBA Postgres** | Integracja z bazą danych, znajdowanie brakujących indexów |
| **markitdown** | Konwersja PDF/specyfikacji do Markdown |
| **Atlassian MCP** | Wypisywanie tasków przypisanych do użytkownika |

### Zasoby
- https://cursor.directory/mcp/markitdown
- https://github.com/punkpeye/awesome-mcp-servers
- https://cursor.directory/mcp

---

## Skille i customowe agenty

### Czym są skille?
- Oszczędzają kontekst → oszczędzają tokeny
- Wyspecjalizowane zadania w izolacji

### Koncepcja Subagenta
- **Czysty kontekst** – subagent startuje w nowym wątku (nowe context window)
- **Celowość** – izolacja kontekstu, np. czytanie dużej ilości plików bez zaśmiecania głównego wątku
- **Wywołanie w Cursor:** `/ <nazwa>` lub poproś agenta o użycie

### Możliwości agentów
- Agent do tworzenia reguł
- Agent do tworzenia innych agentów, komend, skilli
- Generowanie dokumentacji
- Code review
- Wyłuskiwanie specyfikacji z zadań

### Tworzenie własnych agentów

**Podejście 1: Z przykładów (Reverse Engineering)**
```
"To jest na wejściu, to jest na wyjściu" → napisz mi instrukcję do agenta
```

**Podejście 2: Iteracyjne**
1. Draft instrukcji (może być powierzchowny)
2. Użyj Prompt Cowboy lub Gemini do poprawienia
3. Testowanie
4. Poprawki
5. Powtarzaj

**Lokalizacja agentów:**
- `.cursor/agents/` – folder na agentów
- Można stworzyć osobne repo na agentów (dodać wszystko poza `.cursor` do gitignore)

### Agent Architect.md

**Workflow:**
1. Dodaj `Architect.md` do `.cursor/agents/`
2. Zapytaj: `/architect suggest topics to cover`
3. Przejrzyj wyniki – co w projekcie jest robione, jak
4. Twórz proponowane markdowny
5. Sprawdzaj czy są przykłady, czy ma sens
6. Podłączaj instrukcje pojedynczo do `agents.md`
7. Dodawaj kolejne instrukcje i sprawdzaj efekty

**Wskazówka:** Dodawaj instrukcje po kolei (np. architektura → domena → REST), żeby wiedzieć który element źle działa

**Ważne:** Agent Architect opisuje **jak jest aktualnie** – niekoniecznie dobrze. Może być z dupy kod i architektura – on tylko opisuje realia.

### Dopytywanie agenta
- Który controller jego zdaniem jest najlepiej zrobiony? → sugeruj przykłady do plików .md
- Dopracowuj i dopieszczaj instrukcje iteracyjnie
- Pamiętaj o **error handling** (AI często loguje/połyka błędy)

### Można poprosić agenta o:
- Przepisanie instrukcji na inny język (np. Kotlin)
- Eksplorację kodu
- Generowanie diagramów
- Generowanie scenariuszy

---

## Architektura i wzorce

### Znaczenie architektury w erze AI
- Architektura i wzorce projektowe mają **większe znaczenie**
- **DDD (Domain-Driven Design)** dobrze sprawdza się z AI
- **Dobra architektura procentuje!**
- Mniejsze znaczenie: skróty klawiszowe, ścisłe TDD

### Rekomendowane podejście architektoniczne
- **Modularny monolit** z **Vertical Slice Architecture**
- Wewnątrz każdego slice: **Architektura heksagonalna** (porty i adaptery)
- **1 hexagon per mikroserwis** jest OK, jeśli mikroserwis nie jest za duży

### Strategia migracji z monolitu do mikroserwisów

**Etapy migracji:**

```
MONOLIT LEGACY → MODULAR MONOLITH → MIKROSERWISY
```

**Krok 1: Przepisanie na Modular Monolith z Hexagonami**
- Wydziel **bounded contexts** wewnątrz monolitu
- Użyj wzorca **Bubble Context** – nowy kod w czystej architekturze heksagonalnej
- Adaptery mogą uderzać do **legacy DB** (stopniowa migracja danych)
- Każdy moduł ma własne porty i adaptery

**Krok 2: Wydzielenie mikroserwisów**
- Gdy moduł jest wystarczająco odizolowany → wydziel jako osobny mikroserwis
- Adaptery DB mogą nadal komunikować się z legacy bazą (stopniowa migracja)
- Komunikacja międzyserwisowa przez API/eventy

**Zalety podejścia:**
- Stopniowa migracja bez "big bang"
- Możliwość testowania każdego etapu
- Mniejsze ryzyko
- Bubble context izoluje nowy kod od legacy
- Adaptery dają elastyczność w dostępie do danych

**Schemat Bubble Context:**
```
┌─────────────────── MONOLIT ───────────────────┐
│                                                │
│  ┌────── Bubble Context (Nowy moduł) ──────┐  │
│  │                                          │  │
│  │     [Domena - czysta logika]            │  │
│  │              ↕                           │  │
│  │     [Porty - interfejsy]                │  │
│  │              ↕                           │  │
│  │  [Adaptery] → może uderzać do Legacy DB │  │
│  │                                          │  │
│  └──────────────────────────────────────────┘  │
│                                                │
│  [Legacy Code] ← stopniowo zastępowany         │
│                                                │
└────────────────────────────────────────────────┘
```

### Message-driven architecture
- Data change triggers (jak w bazie danych) → **antypattern**
- Lepsze podejście: **pivotal events** (kluczowe zdarzenia biznesowe)

### Event Storming
- Technika warsztatowa do odkrywania procesów biznesowych
- **Event Storming → DDD → Kodenie z AI** – sprawdzony flow

### Diagramy
- **PlantUML**, **mermaid** – dobrze działają z AI
- Można generować HTML, React komponenty (tsx/jsx/mdx)

---

## Testowanie z AI

### Podejście do testowania
- Klasyczne TDD jest trudne z AI
- Łatwiej zacząć od dobrze przetestowanej domeny
- **Acceptance Test Driven Development** – lepiej się sprawdza z AI
- **Testy E2E z całym flow są ważne** (a nie tylko unit testy)

### Refaktoryzacja z testami
1. Dobrze otestuj feature (e2e/integracyjnie, najlepiej na realnych danych)
2. Zapiąć agenta na przepisanie tej funkcji
3. Agent kręci aż testy przechodzą
4. **Agent NIE może zmieniać testów** – testy to kontrakt

---

## Praca z Legacy Code

### Możliwości AI w Legacy
- Eksploracja kodu
- Generowanie diagramów
- Generowanie scenariuszy

### Refaktoryzacja Legacy
1. Otestuj feature e2e/integracyjnie
2. Agent przepisuje, testy muszą przechodzić
3. Użyj **Bubble Context pattern** – wydziel domenę w środku monolitu
4. Feature po feature migruj do nowej architektury

---

## UI i Frontend

### Stack do szybkiego prototypowania
- **Next.js** + **Tailwind** – szybkie tworzenie apek
- **React** – główny framework

### Biblioteki komponentów
- **shadcn/ui** – https://ui.shadcn.com/ (rekomendowana)
- Komponenty do Tailwind, łatwa integracja

### Narzędzia do prototypowania UI
- **Lovable** / **bolt.new** → prototyp → wciągnij do repo (będą już shadcn, tailwind itp.)

---

## Lokalne modele AI

### Stawianie modelu lokalnie
- Potrzebny sprzęt: np. Mac Studio 512GB RAM (**RAM jest kluczowy**)
- W OpenCode można używać swojego modelu zamiast Claude czy Gemini
- Podpinanie subskrypcji Gemini do OpenCode (plugin)

### OpenCode - konfiguracja
- https://opencode.ai/docs/zen/ – wybór darmowych modeli
- Zjada kasę tylko gdy nic nie znajdzie

---

## Słowniczek

### Bubble Context
Wzorzec migracyjny, w którym nowy kod (z czystą architekturą) jest tworzony jako "bańka" wewnątrz legacy systemu. Bańka:
- Ma własną, czystą architekturę (np. heksagonalną)
- Jest odizolowana od legacy kodu
- Komunikuje się z legacy przez adaptery
- Może używać legacy bazy danych przez adapter
- Stopniowo rozrasta się, wypierając legacy kod

**Zalety:**
- Możliwość pisania nowego kodu w nowoczesnej architekturze
- Brak konieczności przepisywania całego systemu na raz
- Zmniejszone ryzyko migracji

### DDD (Domain-Driven Design)
Podejście do projektowania oprogramowania, które stawia **domenę biznesową** w centrum uwagi. Kluczowe elementy:
- **Ubiquitous Language** – wspólny język między developerami a ekspertami domenowymi
- **Agregaty** – grupy obiektów traktowane jako całość
- **Value Objects** – niemutowalne obiekty reprezentujące wartości (np. `Money`, `Email`)
- **Domain Events** – zdarzenia opisujące co się wydarzyło w domenie

DDD dobrze współgra z AI, ponieważ jasna struktura i nazewnictwo ułatwiają agentom AI zrozumienie i generowanie kodu.

### Architektura heksagonalna (Ports and Adapters)
Wzorzec architektoniczny, w którym logika domenowa znajduje się w centrum aplikacji i jest odizolowana od świata zewnętrznego poprzez **porty** (interfejsy) i **adaptery** (implementacje). 

**Zalety:**
- Niezależność domeny od frameworków, baz danych i UI
- Łatwa wymiana infrastruktury (np. zmiana bazy danych)
- Lepsza testowalność – domena może być testowana w izolacji

**Schemat:**
```
[Adapter HTTP] → [Port wejściowy] → [DOMENA] → [Port wyjściowy] → [Adapter DB]
```

### Vertical Slice Architecture
Architektura, w której kod organizowany jest wokół **funkcjonalności (features)**, a nie warstw technicznych. Każdy "slice" zawiera wszystko potrzebne do obsługi danej funkcji – od kontrolera, przez logikę biznesową, po dostęp do danych.

**Porównanie:**

Tradycyjne warstwy:
```
Controllers/ → Services/ → Repositories/
```

Vertical slices:
```
Features/
  CreateDevice/
    CreateDeviceController.java
    CreateDeviceService.java
    CreateDeviceRepository.java
  UpdateDevice/
    UpdateDeviceController.java
    UpdateDeviceService.java
    UpdateDeviceRepository.java
```

**Zalety:**
- Lepsza kohezja
- Mniejsze coupling między funkcjonalnościami
- Łatwiejsze zrozumienie kodu

### Skille
Wyspecjalizowane, oszczędne kontekstowo funkcje AI, które:
- Oszczędzają tokeny
- Wykonują konkretne zadania w izolacji
- Nie zaśmiecają głównego wątku rozmowy

---

## TODO

### Po warsztacie
- [ ] Przejrzeć dokumentację Cursora – co tam jest dostępne i możliwe
- [ ] Przejrzeć allowlisty w Cursorze – gdzie są, jak działają
- [ ] Dodać skróty w Cursor do chowania zakładek
- [ ] Śledzić Cursor release notes
- [ ] Obejrzeć filmy z Claude Code i OpenCode na YT

### Do zbadania
- [ ] Manus – https://manus.im/ (drogie narzędzie, klikanie na kompie)
- [ ] Prompt Cowboy – https://www.promptcowboy.ai/
- [ ] n8n – https://n8n.io/
- [ ] GitHub Issue Template – utworzenie szablonu dla projektu
- [ ] Spring Events – https://www.baeldung.com/spring-events

### Do powtórzenia
- [ ] Wzorzec Strategia (Strategy Pattern)
- [ ] Bubble Context pattern – refaktoryzacja monolitu
- [ ] DDD, hexagon, mikroserwisy

### Konfiguracja do zrobienia
- [ ] Dodać `.ai/` do `.gitignore` (lub zostawić)
- [ ] Dodać `.cursor/` do `.gitignore`
- [ ] Skonfigurować MCP w Cursor (exa/context7, postgres, markitdown)
- [ ] Stworzyć repo na agentów

### Pytania do prowadzącego
- [ ] Zapytać o pokazanie projektu!
- [ ] Zapytać jak przetrzymywać reguły AI dla wielu mikroserwisów

---

## Zasoby i linki

### Dokumentacja
- Cursor subagents: https://cursor.com/docs/context/subagents
- Copilot agents: https://docs.github.com/en/copilot/how-tos/use-copilot-agents/coding-agent/create-custom-agents
- OpenCode: https://opencode.ai/
- OpenCode zen: https://opencode.ai/docs/zen/

### Narzędzia
- Prompt Cowboy: https://www.promptcowboy.ai/
- n8n: https://n8n.io/
- Manus: https://manus.im/
- shadcn/ui: https://ui.shadcn.com/

### MCP
- Awesome MCP Servers: https://github.com/punkpeye/awesome-mcp-servers
- Cursor MCP Directory: https://cursor.directory/mcp
- markitdown: https://cursor.directory/mcp/markitdown

### Reguły i instrukcje
- 10x Rules: https://10xrules.ai/

### Nauka
- Spring Events: https://www.baeldung.com/spring-events
