# AI Software Craftsmanship - Notatki

**Prowadzący:** Michał Michaluk

## Spis treści
1. [Repozytoria](#repozytoria)
2. [Zadania](#zadania)
3. [Narzędzia AI](#narzędzia-ai)
4. [Konfiguracja AI w projekcie](#konfiguracja-ai-w-projekcie)
5. [Podejście do developmentu z AI](#podejście-do-developmentu-z-ai)
6. [MCP (Model Context Protocol)](#mcp-model-context-protocol)
7. [Skille i customowe agenty](#skille-i-customowe-agenty)
8. [Słowniczek](#słowniczek)
9. [TODO](#todo)

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
- Kolejność realizacji: zaczęliśmy od 1, 3, 4 → później 2 i 5

### Możliwe procesy do opracowania
- Installation process (z nagraną rozmową po angielsku → transkrypt do analizy przez AI)
- SAT form editor

### Issue 6: Installation Process
- Link: https://github.com/michal-michaluk/ai-craft-be-java/issues/6
- Dobry opis tworzenia **custom subagentów**:
  - Cursor: na podstawie Cursor subagents docs
  - Copilot: na podstawie Copilot agent docs
- Użyć `Slice.md` jako blueprint
- Praca z transkryptem: `transcript-installation-sat.txt`
- Event Storming → podział na subdomeny (obrazki w issue)

---

## Narzędzia AI

### IDE z AI
- **Cursor** – główne narzędzie
- **Open Code** – alternatywa

### Funkcje Cursor
- **Quality gate** – nie przerywaj, aż wszystkie testy przejdą na zielono
- **Work tree** – 4 agenty na 4 różnych branczach
- **Review** – Cursor robi dobre code review

### Inne narzędzia
- **Google Gemini Deep Research** – naprawdę dobra funkcja do głębokiego researchu
- **Playwright** – automatyzacja przeglądarki (dostępne przez MCP)

### Skróty
- `/` – używać do wywoływania komend

---

## Konfiguracja AI w projekcie

### AGENTS.md (standard dla wszystkich IDE)
- Główny plik konfiguracji dla agentów AI
- Można rozdzielić na mniejsze pliki: `src/docs/{name}.md`
- W każdym pliku opisane buzz wordy i konwencje
- **Uwaga:** W Copilot + IntelliJ jeszcze słabo działa

### GitHub Copilot
- Plik konfiguracyjny: `.github/copilot-instructions.md`
- Można pisać własnych agentów (np. do generowania dokumentacji, review)
- Możliwość utworzenia globalnego repo z agentami/promptami

---

## Podejście do developmentu z AI

### Kluczowe metody
- **Event Storming** – technika warsztatowa do odkrywania procesów biznesowych

### Wskazówki do pracy z AI
- Pokazuj przykłady (example-driven approach)
- Podawaj oczekiwany format odpowiedzi
- Twórz agenta do tworzenia agentów, komend, skilli itp.
- **Prompt Cowboy** – narzędzie do generowania promptów

### Architektura

#### Znaczenie architektury
- Architektura i wzorce projektowe (np. **porty i adaptery**) mają **większe znaczenie w erze AI**
- **DDD (Domain-Driven Design)** dobrze sprawdza się z AI
- **Dobra architektura procentuje!**
- Mniejsze znaczenie: skróty klawiszowe, ścisłe TDD

#### Rekomendowane podejście architektoniczne
- **Modularny monolit** z **Vertical Slice Architecture**
- Wewnątrz każdego slice: **Architektura heksagonalna** (porty i adaptery)
- **1 hexagon per mikroserwis** jest OK, jeśli mikroserwis nie jest za duży
- Może być też **modularny monolit na poziomie mikroserwisu**, i w każdym module (slice) osobny hexagon

#### Strategia migracji z monolitu do mikroserwisów

**Etapy migracji:**

1. **Monolit Legacy** → 2. **Modular Monolith** → 3. **Mikroserwisy**

**Krok 1: Przepisanie na Modular Monolith z Hexagonami**
- Wydziel **bounded contexts** wewnątrz monolitu
- Użyj wzorca **Bubble Context** – nowy kod w czystej architekturze heksagonalnej
- Adaptery mogą uderzać do **legacy DB** (stopniowa migracja danych)
- Każdy moduł ma własne porty i adaptery
- Moduły komunikują się przez dobrze zdefiniowane interfejsy (porty)

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

#### Message-driven architecture
- Data change triggers (jak w bazie danych) → **antypattern**
- Lepsze podejście: **pivotal events** (kluczowe zdarzenia biznesowe)

### Kolejność pisania kodu
1. **Modele dziedzinowe** (dobrze przetestowana domena)
2. **Serwisy**
3. **Adaptery** (persistence, REST)

### Podejście do testowania
- Klasyczne TDD jest trudne z AI
- Łatwiej zacząć od dobrze przetestowanej domeny
- **Acceptance Test Driven Development** – lepiej się sprawdza z AI
- **Testy E2E z całym flow są ważne** (a nie tylko unity testy)

---

## MCP (Model Context Protocol)

### Polecane serwery MCP
| Narzędzie | Opis |
|-----------|------|
| **exa** / **context7** | Alternatywne źródła kontekstu |
| **Playwright** | Automatyzacja przeglądarki |
| **Crystal DBA Postgres** | Integracja z bazą danych |
| **markitdown** | Konwersja specyfikacji do Markdown (np. z Figma) |

### Zasoby
- https://cursor.directory/mcp/markitdown
- Awesome MCP Servers

---

## Skille i customowe agenty

### Możliwości
- Agent do tworzenia reguł
- Agent do tworzenia innych agentów, komend, skilli
- W Cursor/Copilot można tworzyć **customowe subagenty**:
  - Generowanie dokumentacji
  - Code review
  - Tworzenie na podstawie input i output

### Narzędzia wspomagające
- **Manus** – (do zbadania)
- **GitHub Issue Template** – do standaryzacji zadań

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

---

## TODO

### Pytania do prowadzącego
- [ ] Zapytać o pokazanie projektu!
- [ ] Zapytać jak przetrzymywać reguły AI dla wielu mikroserwisów

### Do powtórzenia po warsztacie
- [ ] Wzorzec Strategia (Strategy Pattern)

### Do zbadania
- [ ] Manus – co to za narzędzie?
- [ ] Prompt Cowboy – szczegóły
- [ ] GitHub Issue Template – utworzenie szablonu dla projektu
- [ ] Spring Events – https://www.baeldung.com/spring-events

agent do tworzenia opisu architektury:
https://github.com/michal-michaluk/ai-craft-be-java/issues/8

pisanie agenta, mozna zrobic na podstawie reverse eng tzn. opisujemy co jest na wejsciu 
i co jest na wyjsciu i zeby na podstawie tego zrobil agenta:
https://github.com/michal-michaluk/ai-craft-be-java/issues/9

