# AI Software Craftsmanship - Notatki

**Prowadzący:** Michał Michaluk

## Repozytoria

| Opis | Link |
|------|------|
| Mój fork | https://github.com/czerwinm/ai-craft-be-java |
| Repo prowadzącego | https://github.com/michal-michaluk/ai-craft-be-java |

## Zadania

- Lista zadań: https://github.com/michal-michaluk/ai-craft-be-java/issues
- Kolejność: zaczęliśmy od 1, 3, 4 → później 2 i 5
- Rozmowa o installation process (po angielsku, nagrana) → transkrypt do analizy przez AI
- Inny możliwy proces do opracowania: SAT form editor

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
- **Open Code**
- **Cursor**

### Funkcje Cursor
- **Quality gate** – nie przerywaj, aż wszystkie testy przejdą na zielono
- **Work tree** – 4 agenty na 4 różnych branczach
- **Review** – Cursor robi dobre code review

### Google Gemini
- **Deep Research** – naprawdę dobra funkcja do głębokiego researchu

---

## Konfiguracja AI w projekcie

### AGENTS.md (standard dla wszystkich IDE)
- Główny plik konfiguracji dla agentów AI
- Można rozdzielić na mniejsze pliki: `src/docs/{name}.md`
- W każdym pliku opisane buzz wordy i konwencje
- **Uwaga:** W Copilot + IntelliJ jeszcze słabo działa

### GitHub Copilot
- Plik: `.github/copilot-instructions.md`
- Można pisać własnych agentów (np. do generowania dokumentacji, review)
- Globalne repo z agentami/promptami – możliwe

### Skróty
- `/` – używać do wywoływania komend

---

## Podejście do developmentu z AI

### Metody
- **Event Storming** – technika warsztatowa do odkrywania procesów biznesowych

### Wskazówki do pracy z AI
- Pokazuj przykłady (example-driven)
- Podawaj oczekiwany format odpowiedzi

### Architektura
- Architektura i wzorce projektowe (np. **porty i adaptery**) mają większe znaczenie w erze AI
- **DDD (Domain-Driven Design)** dobrze sprawdza się z AI
- Mniejsze znaczenie: skróty klawiszowe, ścisłe TDD
- **Dobra architektura procentuje!**

### Message-driven
- Data change triggers (jak w bazie danych) → **antypattern**
- Lepsze podejście: np. **pivotal events** (kluczowe zdarzenia biznesowe)

### Kolejność pisania kodu
1. **Modele dziedzinowe** (dobrze przetestowana domena)
2. **Serwisy**
3. **Adaptery** (persistence, REST)

### Podejście do testowania
- Klasyczne TDD jest trudne z AI
- Łatwiej zacząć od dobrze przetestowanej domeny
- **Acceptance Test Driven Development** – lepiej się sprawdza

### Pomysły architektoniczne
- Mikroserwisy, gdzie każdy jako modularny monolit z wykorzystaniem vertical slice architecture
- Mikroserwisy z architekturą heksagonalną (porty i adaptery)

---

## MCP (Model Context Protocol)

### Polecane serwery MCP
| Narzędzie | Opis |
|-----------|------|
| **exa** / **context7** | Alternatywne źródła kontekstu |
| **Playwright** | Automatyzacja przeglądarki |
| **Crystal DBA Postgres** | Baza danych |
| **markitdown** | Markdown ze specyfikacji (np. Figma) |

### Zasoby
- https://cursor.directory/mcp/markitdown
- Awesome MCP Servers

---

## Skille i customowe agenty

- Agent do tworzenia reguł
- W Cursor/Copilot można tworzyć **customowe subagenty**:
  - Generowanie dokumentacji
  - Code review

---

## Słowniczek

### DDD (Domain-Driven Design)
Podejście do projektowania oprogramowania, które stawia **domenę biznesową** w centrum uwagi. Kluczowe elementy:
- **Ubiquitous Language** – wspólny język między developerami a ekspertami domenowymi
- **Agregaty** – grupy obiektów traktowane jako całość
- **Value Objects** – niemutowalne obiekty reprezentujące wartości (np. `Money`, `Email`)
- **Domain Events** – zdarzenia opisujące co się wydarzyło w domenie

DDD dobrze współgra z AI, ponieważ jasna struktura i nazewnictwo ułatwiają agentom AI zrozumienie i generowanie kodu.

### Architektura heksagonalna (Ports and Adapters)
Wzorzec architektoniczny, w którym logika domenowa znajduje się w centrum aplikacji i jest odizolowana od świata zewnętrznego poprzez **porty** (interfejsy) i **adaptery** (implementacje). Pozwala to na:
- Niezależność domeny od frameworków, baz danych i UI
- Łatwą wymianę infrastruktury (np. zmiana bazy danych)
- Lepszą testowalność – domena może być testowana w izolacji

```
        [Adapter HTTP] → [Port wejściowy] → [DOMENA] → [Port wyjściowy] → [Adapter DB]
```

### Vertical Slice Architecture
Architektura, w której kod organizowany jest wokół **funkcjonalności (features)**, a nie warstw technicznych. Każdy "slice" zawiera wszystko potrzebne do obsługi danej funkcji – od kontrolera, przez logikę biznesową, po dostęp do danych.

**Tradycyjne warstwy:**
```
Controllers/ → Services/ → Repositories/
```

**Vertical slices:**
```
Features/
  CreateDevice/
    CreateDeviceController.java
    CreateDeviceService.java
    CreateDeviceRepository.java
  UpdateDevice/
    ...
```

Zalety: lepsza kohezja, mniejsze coupling między funkcjonalnościami, łatwiejsze zrozumienie kodu.

---

## TODO

### Pytania do prowadzącego
- [ ] Zapytać o pokazanie projektu!
- [ ] Zapytać jak przetrzymywać reguły AI dla wielu mikroserwisów

### Do powtórzenia po warsztacie
- [ ] Wzorzec Strategia (Strategy Pattern)


github issue tempalte


dobry pomysl -> zrobic agenta do tworzenia agentow commend skilli itp

prompt cowboy -> do generowania promptow

robienie wlasnych agentow np. na podstawie input i output

manus 



