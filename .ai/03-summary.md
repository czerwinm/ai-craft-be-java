# Issue #3: Model domenowy weryfikacji brakujących informacji i błędów w konfiguracji urządzenia

## Podsumowanie implementacji

Ten dokument opisuje implementację logiki walidacji konfiguracji urządzeń zgodnie ze specyfikacją z GitHub issue #3.

## Wprowadzone zmiany

### 1. Utworzenie rekordu `Violations`

Utworzono nowy niezmienny obiekt wartości (value object) reprezentujący naruszenia walidacji w konfiguracji urządzenia.

**Plik:** `src/main/java/devices/configuration/management/Violations.java`

```java
@Builder
public record Violations(
        boolean operatorNotAssigned,
        boolean providerNotAssigned,
        boolean locationMissing,
        boolean showOnMapButMissingLocation,
        boolean showOnMapButNoPublicAccess
) {
    public boolean isValid() {
        return !operatorNotAssigned
                && !providerNotAssigned
                && !locationMissing
                && !showOnMapButMissingLocation
                && !showOnMapButNoPublicAccess;
    }
}
```

**Kluczowe cechy:**
- Wykorzystuje adnotację `@Builder` z Lombok dla implementacji wzorca builder
- Niezmienny rekord zgodny ze wzorcem value object z DDD
- Zawiera pola boolean dla każdej reguły walidacji:
  - `operatorNotAssigned` - true gdy własność jest nieprzypisana (zarówno operator jak i provider są null)
  - `providerNotAssigned` - true gdy własność jest nieprzypisana (zarówno operator jak i provider są null)
  - `locationMissing` - true gdy lokalizacja jest null
  - `showOnMapButMissingLocation` - true gdy urządzenie ma być pokazane na mapie, ale nie ma lokalizacji
  - `showOnMapButNoPublicAccess` - true gdy urządzenie ma być pokazane na mapie, ale nie ma publicznego dostępu
- Dostarcza metodę `isValid()` sprawdzającą czy wszystkie walidacje przechodzą

### 2. Dodanie logiki weryfikacji do agregatu `DeviceConfiguration`

**Plik:** `src/main/java/devices/configuration/management/DeviceConfiguration.java`

Dodano prywatną metodę `checkViolations()` implementującą logikę walidacji:

```java
private Violations checkViolations() {
    return Violations.builder()
            .operatorNotAssigned(ownership.isUnowned())
            .providerNotAssigned(ownership.isUnowned())
            .locationMissing(location == null)
            .showOnMapButMissingLocation(settings.showOnMap() && location == null)
            .showOnMapButNoPublicAccess(settings.showOnMap() && !settings.publicAccess())
            .build();
}
```

**Reguły walidacji:**
1. **Operator nie przypisany**: Sprawdza czy własność jest nieprzypisana (zarówno operator jak i provider są null)
2. **Provider nie przypisany**: Sprawdza czy własność jest nieprzypisana (zarówno operator jak i provider są null)
3. **Brak lokalizacji**: Sprawdza czy pole location jest null
4. **Pokazuj na mapie ale brak lokalizacji**: Sprawdza czy `settings.showOnMap` jest true, ale location jest null
5. **Pokazuj na mapie ale brak dostępu publicznego**: Sprawdza czy `settings.showOnMap` jest true, ale `settings.publicAccess` jest false

### 3. Aktualizacja rekordu `DeviceConfigurationSnapshot`

**Plik:** `src/main/java/devices/configuration/management/DeviceConfigurationSnapshot.java`

Dodano pole `Violations` do rekordu snapshot:

```java
public record DeviceConfigurationSnapshot(
        String deviceId,
        Ownership ownership,
        Location location,
        OpeningHours openingHours,
        Settings settings,
        Violations violations
) {
}
```

### 4. Aktualizacja metody `toSnapshot()`

Zmodyfikowano metodę `toSnapshot()` w agregacie `DeviceConfiguration` aby uwzględniała violations:

```java
DeviceConfigurationSnapshot toSnapshot() {
    return new DeviceConfigurationSnapshot(
            deviceId,
            ownership,
            location,
            openingHours,
            settings,
            checkViolations()
    );
}
```

Metoda wywołuje teraz `checkViolations()` aby obliczyć naruszenia w momencie tworzenia snapshota.

## Pokrycie testami

Dodano kompleksowe testy jednostkowe weryfikujące logikę walidacji:

**Plik:** `src/test/java/devices/configuration/management/DeviceConfigurationTest.java`

### Dodane przypadki testowe:

1. **shouldDetectOperatorNotAssigned()** - Weryfikuje wykrycie nieprzypisanego urządzenia (brak operatora)
2. **shouldDetectProviderNotAssigned()** - Weryfikuje wykrycie nieprzypisanego urządzenia (brak providera)
3. **shouldDetectLocationMissing()** - Weryfikuje wykrycie braku lokalizacji
4. **shouldDetectShowOnMapButMissingLocation()** - Weryfikuje wykrycie gdy showOnMap jest włączone, ale brak lokalizacji
5. **shouldDetectShowOnMapButNoPublicAccess()** - Weryfikuje wykrycie gdy showOnMap jest włączone, ale publicAccess wyłączony
6. **shouldHaveNoViolationsWhenFullyConfigured()** - Weryfikuje że w pełni skonfigurowane urządzenie nie ma naruszeń
7. **shouldHaveNoViolationsWhenNotShowingOnMap()** - Weryfikuje że naruszenia związane z mapą nie występują gdy showOnMap jest false
8. **shouldDetectMultipleViolations()** - Weryfikuje wykrycie wielu naruszeń jednocześnie

Wszystkie testy stosują wzorzec Given-When-Then i używają asercji AssertJ dla czytelności intencji testu.

## Decyzje projektowe

### 1. Wzorzec Builder z Lombok
Użyto adnotacji `@Builder` zamiast ręcznej implementacji buildera dla czystszego kodu i zmniejszenia boilerplate'u.

### 2. Walidacja Ownership
Rekord `Ownership` wymusza, że zarówno operator jak i provider muszą być jednocześnie null lub nie-null (nigdy jeden null a drugi nie-null). Dlatego oba naruszenia `operatorNotAssigned` i `providerNotAssigned` są sprawdzane używając `ownership.isUnowned()`.

### 3. Walidacja w momencie tworzenia snapshota
Violations są obliczane podczas tworzenia snapshota, a nie przechowywane jako stan agregatu. Zapewnia to:
- Violations są zawsze aktualne w oparciu o najnowszy stan agregatu
- Brak potrzeby utrzymywania stanu violations w agregacie
- Zgodność z zasadą danych pochodnych

### 4. Niezmienność
Rekord `Violations` jest niezmienny (jak wszystkie rekordy), zgodnie z zasadami value object z DDD i zapewniając bezpieczeństwo wątkowe.

## Wyniki budowania i testów

Wszystkie testy przechodzą pomyślnie:
```bash
./gradlew test
BUILD SUCCESSFUL
```

Implementacja:
- ✅ Kompiluje się bez błędów
- ✅ Przechodzi wszystkie istniejące testy
- ✅ Przechodzi wszystkie nowe testy walidacji (8 nowych przypadków testowych)
- ✅ Zgodna ze standardami kodowania projektu i zasadami DDD
- ✅ Używa odpowiednich wzorców (Builder, Value Objects, Records)

## Zmodyfikowane pliki

1. **Utworzono:** `src/main/java/devices/configuration/management/Violations.java`
2. **Zmodyfikowano:** `src/main/java/devices/configuration/management/DeviceConfiguration.java`
3. **Zmodyfikowano:** `src/main/java/devices/configuration/management/DeviceConfigurationSnapshot.java`
4. **Zmodyfikowano:** `src/test/java/devices/configuration/management/DeviceConfigurationTest.java`

## Zgodność z architekturą

Implementacja jest zgodna z zasadami architektonicznymi projektu:

- **Niezależność domeny**: Logika walidacji znajduje się w agregacie domenowym, niezależnie od infrastruktury
- **Enkapsulacja agregatu**: Violations są obliczane przez agregat używając jego prywatnego stanu
- **Niezmienność**: Używa rekordów Java dla niezmiennych obiektów wartości
- **Projektowanie package-private**: Wewnętrzna metoda walidacji jest prywatna dla agregatu
- **Wzorzec Builder**: Zaimplementowany przy użyciu `@Builder` z Lombok dla czystego API
- **Testy jednostkowe**: Kompleksowe testy weryfikują logikę domenową w izolacji

## Uwagi

Implementacja interpretuje `DeviceConfigurationSnapshot` jako snapshot tylko do odczytu z violations (podobnie do tego, co issue nazywało "rekordem DeviceConfiguration"). Istniejąca klasa `DeviceConfiguration` jest agregatem zawierającym zachowanie i zarządzanie stanem.

Ten wybór projektowy utrzymuje spójność z istniejącą strukturą kodu, gdzie:
- `DeviceConfiguration` = Agregat (z zachowaniem, zdarzeniami i zmiennym stanem)
- `DeviceConfigurationSnapshot` = Niezmienny snapshot/model odczytu

Logika walidacji respektuje ograniczenia domenowe już obecne w obiekcie wartości `Ownership` i integruje się bezproblemowo z istniejącym modelem domenowym.
