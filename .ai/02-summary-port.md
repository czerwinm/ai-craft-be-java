# Podsumowanie: Porty dla Persystencji Konfiguracji Urządzeń

## Zadanie
Implementacja persystencji obiektów Device jako dokumentów JSONB w PostgreSQL z:
- Pełnymi operacjami CRUD
- Optymistycznym blokowaniem
- Odczytem wszystkich z paginacją

GitHub Issue: https://github.com/michal-michaluk/ai-craft-be-java/issues/2

## Przegląd Architektury

Zgodnie z wzorcem **Architektura Heksagonalna (Porty i Adaptery)**, implementacja rozdziela:
- **Warstwę Domenową**: Logika biznesowa (agregat DeviceConfiguration)
- **Porty**: Interfejsy definiujące kontrakty
- **Adaptery**: Implementacje infrastrukturalne (do zaimplementowania później)

## Port Pierwotny (Warstwa Serwisowa)

Warstwa serwisowa działa jako **fasada** dla modułu Device Configuration, zarządzając cyklem życia agregatów i koordynując operacje.

### DeviceConfigurationService

**Lokalizacja**: `src/main/java/devices/configuration/management/DeviceConfigurationService.java`

**Cel**: Port pierwotny udostępniający funkcjonalność modułu. Zarządza cyklem życia agregatu DeviceConfiguration:
- Tworzy nowe instancje urządzeń
- Pobiera istniejące urządzenia
- Aktualizuje konfigurację urządzeń
- Usuwa urządzenia
- Obsługuje paginację przy listowaniu urządzeń

**Odpowiedzialności**:
1. Pobieranie instancji urządzeń z repozytorium
2. Tworzenie nowych instancji agregatów
3. Aplikowanie operacji biznesowych poprzez metody agregatu
4. Zapewnienie wywołań zapisu w repozytorium
5. Konwersja agregatów do migawek dla zewnętrznego użycia
6. Obsługa konfliktów optymistycznego blokowania

**Interfejs**:
```java
@Service
@Transactional
@RequiredArgsConstructor
public class DeviceConfigurationService {
    
    private final DeviceConfigurationRepository repository;
    
    /**
     * Tworzy nowe urządzenie z domyślną konfiguracją
     * @param deviceId unikalny identyfikator urządzenia
     * @return migawka utworzonej konfiguracji urządzenia
     * @throws DeviceAlreadyExistsException jeśli urządzenie o podanym ID już istnieje
     */
    public DeviceConfigurationSnapshot createDevice(String deviceId);
    
    /**
     * Pobiera konfigurację urządzenia po ID
     * @param deviceId unikalny identyfikator urządzenia
     * @return opcjonalna migawka konfiguracji urządzenia
     */
    @Transactional(readOnly = true)
    public Optional<DeviceConfigurationSnapshot> getDevice(String deviceId);
    
    /**
     * Aktualizuje własność urządzenia
     * @param deviceId unikalny identyfikator urządzenia
     * @param ownership nowe informacje o własności
     * @param version aktualna wersja dla optymistycznego blokowania
     * @return migawka zaktualizowanej konfiguracji urządzenia
     * @throws DeviceNotFoundException jeśli urządzenie nie istnieje
     * @throws OptimisticLockException jeśli wersje nie pasują
     */
    public DeviceConfigurationSnapshot updateOwnership(
        String deviceId, 
        Ownership ownership, 
        long version
    );
    
    /**
     * Aktualizuje lokalizację urządzenia
     * @param deviceId unikalny identyfikator urządzenia
     * @param location nowe informacje o lokalizacji
     * @param version aktualna wersja dla optymistycznego blokowania
     * @return migawka zaktualizowanej konfiguracji urządzenia
     * @throws DeviceNotFoundException jeśli urządzenie nie istnieje
     * @throws OptimisticLockException jeśli wersje nie pasują
     */
    public DeviceConfigurationSnapshot updateLocation(
        String deviceId, 
        Location location, 
        long version
    );
    
    /**
     * Aktualizuje godziny otwarcia urządzenia
     * @param deviceId unikalny identyfikator urządzenia
     * @param openingHours nowe godziny otwarcia
     * @param version aktualna wersja dla optymistycznego blokowania
     * @return migawka zaktualizowanej konfiguracji urządzenia
     * @throws DeviceNotFoundException jeśli urządzenie nie istnieje
     * @throws OptimisticLockException jeśli wersje nie pasują
     */
    public DeviceConfigurationSnapshot updateOpeningHours(
        String deviceId, 
        OpeningHours openingHours, 
        long version
    );
    
    /**
     * Aktualizuje ustawienia urządzenia
     * @param deviceId unikalny identyfikator urządzenia
     * @param settings nowe ustawienia
     * @param version aktualna wersja dla optymistycznego blokowania
     * @return migawka zaktualizowanej konfiguracji urządzenia
     * @throws DeviceNotFoundException jeśli urządzenie nie istnieje
     * @throws OptimisticLockException jeśli wersje nie pasują
     */
    public DeviceConfigurationSnapshot updateSettings(
        String deviceId, 
        Settings settings, 
        long version
    );
    
    /**
     * Usuwa konfigurację urządzenia
     * @param deviceId unikalny identyfikator urządzenia
     * @param version aktualna wersja dla optymistycznego blokowania
     * @throws DeviceNotFoundException jeśli urządzenie nie istnieje
     * @throws OptimisticLockException jeśli wersje nie pasują
     */
    public void deleteDevice(String deviceId, long version);
    
    /**
     * Listuje wszystkie urządzenia z paginacją
     * @param pageable parametry paginacji (numer strony, rozmiar, sortowanie)
     * @return strona migawek konfiguracji urządzeń
     */
    @Transactional(readOnly = true)
    public Page<DeviceConfigurationSnapshot> listDevices(Pageable pageable);
}
```

## Porty Wtórne (Interfejsy Repozytoriów)

Porty wtórne definiują kontrakty dla zagadnień infrastrukturalnych. Warstwa domenowa zależy od tych interfejsów, które są implementowane przez adaptery.

### DeviceConfigurationRepository

**Lokalizacja**: `src/main/java/devices/configuration/management/DeviceConfigurationRepository.java`

**Cel**: Port repozytorium dla operacji persystencji urządzeń

**Widoczność**: Package-private (używany tylko w obrębie modułu)

**Interfejs**:
```java
/**
 * Port repozytorium dla persystencji agregatu DeviceConfiguration
 * Implementacje powinny obsługiwać przechowywanie JSONB i optymistyczne blokowanie
 */
interface DeviceConfigurationRepository {
    
    /**
     * Pobiera urządzenie po ID z informacją o wersji
     * @param deviceId unikalny identyfikator urządzenia
     * @return opcjonalne urządzenie z wersją
     */
    Optional<VersionedDevice> findById(String deviceId);
    
    /**
     * Zapisuje konfigurację urządzenia (tworzy lub aktualizuje)
     * Przy aktualizacji waliduje wersję dla optymistycznego blokowania
     * @param device agregat urządzenia do zapisu
     * @param expectedVersion oczekiwana wersja dla optymistycznego blokowania (null dla nowych urządzeń)
     * @return nowa wersja po zapisie
     * @throws OptimisticLockException jeśli wersje nie pasują przy aktualizacji
     */
    long save(DeviceConfiguration device, Long expectedVersion);
    
    /**
     * Usuwa konfigurację urządzenia
     * @param deviceId unikalny identyfikator urządzenia
     * @param expectedVersion oczekiwana wersja dla optymistycznego blokowania
     * @throws DeviceNotFoundException jeśli urządzenie nie istnieje
     * @throws OptimisticLockException jeśli wersje nie pasują
     */
    void delete(String deviceId, long expectedVersion);
    
    /**
     * Sprawdza czy urządzenie istnieje
     * @param deviceId unikalny identyfikator urządzenia
     * @return true jeśli urządzenie istnieje
     */
    boolean exists(String deviceId);
    
    /**
     * Listuje wszystkie urządzenia z paginacją
     * @param pageable parametry paginacji
     * @return strona urządzeń z wersjami
     */
    Page<VersionedDevice> findAll(Pageable pageable);
}
```

### VersionedDevice

**Lokalizacja**: `src/main/java/devices/configuration/management/VersionedDevice.java`

**Cel**: Obiekt wartości opakowujący agregat urządzenia z informacją o wersji dla optymistycznego blokowania

**Interfejs**:
```java
/**
 * Wrapper dla agregatu urządzenia z informacją o wersji
 * Używany do optymistycznego blokowania
 */
public record VersionedDevice(
    DeviceConfiguration device,
    long version
) {
    public VersionedDevice {
        if (device == null) {
            throw new IllegalArgumentException("Device cannot be null");
        }
        if (version < 0) {
            throw new IllegalArgumentException("Version must be non-negative");
        }
    }
}
```

## Wyjątki Domenowe

Niestandardowe wyjątki dla obsługi błędów specyficznych dla domeny:

### DeviceNotFoundException

**Lokalizacja**: `src/main/java/devices/configuration/management/DeviceNotFoundException.java`

```java
/**
 * Rzucany gdy urządzenie o podanym ID nie istnieje
 */
public class DeviceNotFoundException extends RuntimeException {
    public DeviceNotFoundException(String deviceId) {
        super("Device not found: " + deviceId);
    }
}
```

### DeviceAlreadyExistsException

**Lokalizacja**: `src/main/java/devices/configuration/management/DeviceAlreadyExistsException.java`

```java
/**
 * Rzucany przy próbie utworzenia urządzenia z istniejącym ID
 */
public class DeviceAlreadyExistsException extends RuntimeException {
    public DeviceAlreadyExistsException(String deviceId) {
        super("Device already exists: " + deviceId);
    }
}
```

### OptimisticLockException

**Lokalizacja**: `src/main/java/devices/configuration/management/OptimisticLockException.java`

```java
/**
 * Rzucany gdy walidacja optymistycznego blokowania się nie powiedzie
 */
public class OptimisticLockException extends RuntimeException {
    public OptimisticLockException(String deviceId, long expectedVersion, long actualVersion) {
        super(String.format(
            "Optimistic lock failed for device %s: expected version %d, but found %d",
            deviceId, expectedVersion, actualVersion
        ));
    }
}
```

## Wymagane Zależności

Implementacja będzie wymagać:

### Spring Framework
```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.data:spring-data-commons' // Dla Page i Pageable
}
```

### Baza danych
```gradle
dependencies {
    runtimeOnly 'org.postgresql:postgresql'
}
```

## Kluczowe Decyzje Projektowe

### 1. Strategia Optymistycznego Blokowania
- Numer wersji zarządzany na poziomie repozytorium
- Warstwa serwisowa przekazuje oczekiwaną wersję przy aktualizacjach
- Repozytorium waliduje wersję przed zapisem
- OptimisticLockException rzucany przy niezgodności

### 2. Enkapsulacja Agregatu
- DeviceConfiguration pozostaje package-private
- Tylko migawki (DeviceConfigurationSnapshot) eksponowane na zewnątrz modułu
- Repozytorium zwraca VersionedDevice aby przenosić informację o wersji

### 3. Repozytorium jako Port Wtórny
- Interfejs zdefiniowany w pakiecie domenowym (package-private)
- Implementacja będzie w pakiecie adaptera
- Warstwa domenowa zależy od interfejsu, nie od implementacji

### 4. Operacje CRUD
- **Create**: `createDevice()` - sprawdza czy urządzenie istnieje
- **Read**: `getDevice()` i `listDevices()` - transakcje tylko do odczytu
- **Update**: Wiele metod aktualizacji dla różnych aspektów (ownership, location, settings, openingHours)
- **Delete**: `deleteDevice()` - z optymistycznym blokowaniem

### 5. Wsparcie Paginacji
- Używa abstrakcji `Page` i `Pageable` ze Spring Data
- Umożliwia elastyczne sortowanie i filtrowanie na poziomie adaptera

### 6. Zarządzanie Transakcjami
- Serwis oznaczony adnotacją `@Transactional`
- Operacje odczytu oznaczone jako `readOnly = true`
- Granice transakcji na poziomie serwisu

### 7. Zdarzenia Domenowe
- Zdarzenia akumulowane w agregacie podczas operacji
- Będą emitowane/publikowane w czasie persystencji (odpowiedzialność adaptera)

## Następne Kroki (Adaptery - Jeszcze Niezaimplementowane)

Następujące adaptery będą zaimplementowane w kolejnych fazach:

1. **Adapter Persystencji** (`DeviceConfigurationDocumentRepository`)
   - Repozytorium JPA dla przechowywania JSONB w PostgreSQL
   - Klasa encji z `@Type(JsonBinaryType.class)` dla JSONB
   - Pole wersji z `@Version` dla optymistycznego blokowania
   - Implementacja interfejsu DeviceConfigurationRepository

2. **Adapter HTTP** (`DeviceConfigurationController`)
   - Endpointy REST API dla operacji CRUD
   - DTO Request/Response
   - Handlery wyjątków dla wyjątków domenowych
   - Obsługa nagłówka wersji dla optymistycznego blokowania

3. **Adapter Publikacji Zdarzeń**
   - Producer Kafka dla zdarzeń domenowych
   - Mapper i serializer zdarzeń
   - Integracja z operacją zapisu w repozytorium

## Struktura Plików

```
src/main/java/devices/configuration/management/
├── DeviceConfiguration.java              # Agregat (istniejący)
├── DeviceConfigurationSnapshot.java      # Migawka VO (istniejący)
├── DeviceConfigurationService.java       # Port Pierwotny (NOWY)
├── DeviceConfigurationRepository.java    # Port Wtórny (NOWY)
├── VersionedDevice.java                  # Obiekt Wartości (NOWY)
├── DeviceNotFoundException.java          # Wyjątek Domenowy (NOWY)
├── DeviceAlreadyExistsException.java    # Wyjątek Domenowy (NOWY)
├── OptimisticLockException.java         # Wyjątek Domenowy (NOWY)
├── DomainEvent.java                      # Zdarzenia Domenowe (istniejący)
├── Ownership.java                        # Obiekt Wartości (istniejący)
├── Location.java                         # Obiekt Wartości (istniejący)
├── Coordinates.java                      # Obiekt Wartości (istniejący)
├── OpeningHours.java                     # Obiekt Wartości (istniejący)
├── Settings.java                         # Obiekt Wartości (istniejący)
├── Violations.java                       # Obiekt Wartości (istniejący)
└── Visibility.java                       # Obiekt Wartości (istniejący)
```

## Strategia Testowania

### Testy Jednostkowe (Warstwa Portów)
- `DeviceConfigurationServiceTest.java`: Test logiki serwisu z mockowanym repozytorium
  - Test pomyślnych operacji CRUD
  - Test obsługi wyjątków (nie znaleziono, już istnieje, optymistyczne blokowanie)
  - Test inkrementacji wersji
  - Test paginacji

### Testy Integracyjne (Przyszłe - Adaptery)
- `DeviceConfigurationRepositoryTest.java`: Test z Testcontainers PostgreSQL
  - Test serializacji/deserializacji JSONB
  - Test zachowania optymistycznego blokowania
  - Test zapytań paginacji

### Testy End-to-End (Przyszłe - Adapter HTTP)
- `DeviceConfigurationControllerTest.java`: Pełny cykl request/response
  - Test endpointów REST API
  - Test obsługi nagłówka wersji
  - Test scenariuszy współbieżnych aktualizacji

## Podsumowanie

Implementacja portów ustanawia czyste granice między:
- **Logiką domenową** (agregat DeviceConfiguration)
- **Logiką aplikacyjną** (DeviceConfigurationService)
- **Infrastrukturą** (interfejs repozytorium, do zaimplementowania przez adaptery)

Projekt następuje:
- ✅ Niezależność Domeny
- ✅ Enkapsulacja Agregatu
- ✅ Wzorzec Portów i Adapterów
- ✅ Niezmienność (obiekty wartości)
- ✅ Zdarzeniowa (zdarzenia domenowe)
- ✅ Package-Private jako Domyślne

## Zaimplementowane Pliki

### Porty i Serwis
- ✅ `DeviceConfigurationService.java` - Port pierwotny z logiką serwisową
- ✅ `DeviceConfigurationRepository.java` - Port wtórny (interfejs repozytorium)

### Obiekty Wartości
- ✅ `VersionedDevice.java` - Wrapper agregatu z wersją

### Wyjątki Domenowe
- ✅ `DeviceNotFoundException.java` - Wyjątek gdy urządzenie nie istnieje
- ✅ `DeviceAlreadyExistsException.java` - Wyjątek gdy urządzenie już istnieje
- ✅ `OptimisticLockException.java` - Wyjątek konfliktu wersji

### Testy Jednostkowe
- ✅ `DeviceConfigurationServiceTest.java` - 11 testów pokrywających wszystkie operacje CRUD
  - Tworzenie urządzenia
  - Pobieranie urządzenia
  - Aktualizacje (ownership, location, openingHours, settings)
  - Usuwanie
  - Listowanie z paginacją
  - Obsługa wyjątków
- ✅ `VersionedDeviceTest.java` - 4 testy walidacji
  - Tworzenie z poprawnymi danymi
  - Walidacja null device
  - Walidacja ujemnej wersji

Wszystko skompilowane i przetestowane. Gotowe do implementacji adapterów w następnej fazie.
