# Instructions for implementing Business Modules with Domain-Driven Design and Ports and Adapters

## Example Structure of a Module with Domain-Driven Design, Ports and Adapters, and Tests

```
src/
  {feature-name}/
    {feature.name}.controller.ts # HTTP adapter implementations
    {feature.name}.service.ts  # Primary port implementations, facade for module functionality
    {aggregate.name}.ts # Aggregate implementation
    {feature.name}.model.ts # Immutable value objects
    {feature.name}.events.ts  # Immutable domain events
    {feature.name}.repository.ts # Port and provider export and private adapter implementation for persistence
    {external.system.adapter}.ts # Port and provider export and private adapter implementation for external system
    {feature.name}.module.ts # NextJS module

test/
  {feature-name}/
    {feature.name}.fixtures.ts # Implementation of helper functions preparing example data used in module tests
    {aggregate.name}.spec.ts # Unit test implementation for aggregate
    {feature.name}.model.ts # Implementation of value object logic
    {feature.name}.repository.ts # Integration test implementation for adapter using testcontainers
    {external.system.adapter}.ts # Adapter test implementation using fake external system API
    {feature.name}.e2e-spec.ts # End-to-end test implementations
    {feature.name}.api.http # Http client file allowing manual testing / experimenting with module API
```

## Domain-Driven Design - Key Patterns

### Aggregate (Aggregate Root)

Aggregates encapsulate key business activities that require validation, consistency, and business sense for processes or edited data.

An aggregate is a collection of objects treated as a whole, with an ID and mutable state, with a clearly designated main object (aggregate root). Only the root can be referenced from outside, and any state changes must be made through the root with rules enforcement, ensuring data consistency.

**How to implement:**

- in aggregate name and file, do not use aggregate suffix, use clean name
- prefer value objects from {feature.name}.model.ts as fields
- if a field should be a mutable object, define it in the aggregate file {aggregate.name}.ts and do not export it
- never place aggregate snapshot as an aggregate field, add a method returning snapshot
- do not place read methods for single fields, getters, get{field name} methods in aggregate
- implement business rules as private methods (name them according to business rule)

```typescript
export class DeviceConfigurationEditor {
    // Private fields - only aggregate root can modify them
    constructor(
        readonly deviceId: string,
        readonly events: DomainEvent[],
        private ownership: Ownership,
        private location: Location | null,
        private openingHours: OpeningHours,
        private settings: Settings,
    ) {}

    // Static factory - readable way to create new aggregates
    static newDeviceConfiguration(deviceId: string): DeviceConfigurationEditor {
        return new DeviceConfigurationEditor(
            deviceId,
            [],
            Ownership.unowned(),
            null,
            OpeningHours.alwaysOpened(),
            Settings.defaultSettings(),
        );
    }

    // Public methods modifying internal state and emitting events
    assignTo(ownership: Ownership): void {
        // Ensuring additional business rules
        this.ensureCanAssigne(ownership);

        // Ensuring idempotency of processed commands
        if (!this.ownership.equals(ownership)) {
            // Changing internal state
            this.ownership = ownership;
            // Formulating event and storing event for emission during persistence
            this.events.push(new OwnershipUpdated(this.deviceId, ownership));

            // Ensuring additional business rules
            if (ownership.isUnowned()) {
                this.resetToDefaults();
            }
        }
    }

    // Method creating state snapshot
    toDeviceConfiguration(): DeviceConfiguration {
        const violations = this.checkViolations();
        return new DeviceConfiguration(
            this.deviceId,
            this.ownership,
            this.location,
            this.settings,
            this.openingHours,
            violations,
        );
    }
}
```

**Best practices:**

- Aggregate controls access to its internal objects
- All state changes are visible through emitted events
- Validations and business rules are enforced inside aggregate
- Aggregate identifier (ID) is unique in the entire system
- Aggregate should have a constructor accepting all fields except events field
- Aggregate functionality is always exposed through Service (Primary Port), which manages aggregate lifecycle:
    - obtains existing instances from repository,
    - creates new aggregate instance in appropriate business situations
    - ensures repository save call
    - may coordinate cooperation with other services / modules / external dependencies

### Value Objects

Value objects are immutable, have no identity, and represent domain concepts. Two value objects with the same attributes are considered equal.

**How to implement:**

```typescript
export class Ownership {
    // Immutable fields - no setters
    constructor(
        readonly operator: string | null,
        readonly provider: string | null,
    ) {
        // Validation in constructor
        if (
            (operator === null && provider !== null) ||
            (operator !== null && provider === null)
        ) {
            throw new Error('Ownership must be either owned or unowned');
        }
    }

    // Factory methods for commonly used instances
    static unowned(): Ownership {
        return new Ownership(null, null);
    }

    static of(operator: string, provider: string): Ownership {
        return new Ownership(operator, provider);
    }

    // Predicate methods expressing domain concepts
    isUnowned(): boolean {
        return this.operator === null && this.provider === null;
    }

    // Comparison by value, not by reference
    equals(ownership: Ownership) {
        return (
            this.operator === ownership.operator &&
            this.provider === ownership.provider
        );
    }
}
```

**Best practices:**

- Always implement `equals()` method
- Implement static factory methods for readability
- Validate data in constructor
- Do not use setters, create new instances when changing
- Method names should reflect domain language

### Domain Events

Domain events represent facts that occurred in the business domain and may trigger further actions.

**How to implement:**

```typescript
// Marker interface
export interface DomainEvent {
    readonly deviceId: string;
}

// Concrete events inheriting
export class OwnershipUpdated implements DomainEvent {
    constructor(
        readonly deviceId: string,
        readonly ownership: Ownership,
    ) {}
}

export class LocationUpdated implements DomainEvent {
    constructor(
        readonly deviceId: string,
        readonly location: Location | null,
    ) {}
}
```

**Best practices:**

- Name events in past tense (e.g., `OwnershipUpdated`)
- Events should be immutable
- Each event should contain all data needed to understand what happened
- Events should be serializable
- Do not add event timestamp to events

## Ports and Adapters (Hexagonal Architecture)

### Primary Port

Module functionality is always exposed through Service (Primary Port), which manages domain object lifecycle:

- obtains instance from repository,
- creates new aggregate instance
- ensures repository save call
  And coordinates cooperation with other services / modules / external dependencies.

```typescript
@Injectable()
export class DeviceConfigurationService {
    constructor(private readonly repository: DeviceRepository) {}

    async getDevice(deviceId: string): Promise<DeviceConfiguration | null> {
        const device = await this.repository.findOne(deviceId);
        return device?.toDeviceConfiguration() || null;
    }

    @OnEvent('TaskCreated')
    async handleTaskCreated(payload: TaskCreatedEvent): Promise<void> {
        const device = await this.repository.findOne(payload.deviceId);
        if (!device) {
            return;
        }
        const update = taskToUpdateDevice(payload);
        update.apply(device);
        await this.repository.save(device);
    }

    async createNewDevice(
        deviceId: string,
        update: UpdateDevice,
    ): Promise<DeviceConfiguration> {
        const device =
            DeviceConfigurationEditor.newDeviceConfiguration(deviceId);
        update.apply(device);
        await this.repository.save(device);
        return device.toDeviceConfiguration();
    }

    async updateDevice(
        deviceId: string,
        update: UpdateDevice,
    ): Promise<DeviceConfiguration | null> {
        const device = await this.repository.findOne(deviceId);
        if (!device) {
            return null;
        }

        update.apply(device);
        await this.repository.save(device);
        return device.toDeviceConfiguration();
    }
}
```

### Primary Port Adapters

Primary port adapters are application entry points that initiate interaction with the business domain through primary ports. In hexagonal architecture, they include elements such as API controllers, event listeners, user interfaces, or cron scripts.

### HTTP Adapter (API Controllers)

API controllers are primary port adapters that handle HTTP requests and translate them into domain service calls.

**How to implement:**

```typescript
@Controller()
export class BriefsController {
    constructor(private readonly briefService: BriefService) {}

    @Get('events/:eventId/brief')
    async getBrief(
        @Param('eventId') eventId: string,
        @UserId() userId: string,
        @TenantId() tenantId: string,
    ): Promise<BriefDto> {
        const identity = { tenantId, userId };
        const brief = await this.briefService.getBrief(identity, eventId);

        if (!brief) {
            throw new NotFoundException('Not found');
        }

        return brief;
    }

    @Put('events/:eventId/brief')
    async updateBrief(
        @Param('eventId') eventId: string,
        @Body() updateBriefDto: UpdateBriefDto,
        @UserId() userId: string,
        @TenantId() tenantId: string,
    ): Promise<BriefDto> {
        const identity = { tenantId, userId };
        const brief = await this.briefService.updateBrief(
            identity,
            eventId,
            updateBriefDto,
        );

        if (!brief) {
            throw new NotFoundException('Not found');
        }

        return brief;
    }

    // Other API endpoints...
}
```

**Best practices:**

- Controllers should be thin - they only handle security, input validation, primary port call, and response formatting
- Use decorators to extract data from requests (e.g., identity, parameters)
- Implement proper error handling and return appropriate HTTP codes
- Business logic should always be in application/domain layer, never in controller
- Group endpoints in controllers by business resources

### Message Queue and Message Broker Adapters

Message queue and message broker adapters allow integration with external asynchronous communication systems.

**How to implement:**

```typescript
@Injectable()
export class RabbitMqEventAdapter {
    constructor(
        @Inject('RabbitMqClient') private client: AmqpConnection,
        private readonly logger: LoggerService,
    ) {}

    @RabbitSubscribe({
        exchange: 'events',
        routingKey: 'event.*.created',
        queue: 'brief-service-event-created',
    })
    async handleEventCreated(message: EventCreatedMessage): Promise<void> {
        try {
            const { tenantId, eventId, name } = message;
            // Passing to primary port
            await this.eventEmitter.emit('event.created', {
                tenantId,
                eventId,
                name,
            });
        } catch (error) {
            this.logger.error(
                `Error processing event.created message: ${error.message}`,
            );
            throw error; // Reprocessing by broker
        }
    }
}
```

**Best practices:**

- Separate business logic from message broker integration details
- Implement appropriate message processing patterns (Circuit Breaker, Retry, Dead Letter Queue)
- Map messages from external format to domain objects
- Use specialized libraries for broker integration (e.g., @nestjs/microservices)
- Ensure proper error and exception handling
- Standardize queue and exchange naming according to project convention

### Cron Jobs Adapter

Cron job adapters trigger business processes at specified time intervals.

**How to implement:**

```typescript
@Injectable()
export class BriefCronJobs {
    constructor(
        private readonly briefService: BriefService,
        private readonly logger: LoggerService,
    ) {}

    @Cron('0 0 * * *') // Daily at midnight
    async sendDailyBriefSummaries(): Promise<void> {
        this.logger.info('Starting daily brief summaries job');
        try {
            const today = new Date();
            await this.briefService.generateAndSendDailySummaries(today);
        } catch (error) {
            this.logger.error(
                `Failed to send daily summaries: ${error.message}`,
            );
        }
    }
}
```

**Best practices:**

- Use decorators to define job schedules
- Implement robust error handling for cron jobs
- Monitor job execution through logs and metrics
- Avoid long operations blocking Node.js event loop
- Consider using separate workers for computationally intensive tasks

### Secondary Ports (Interfaces)

Ports define communication contracts between the domain layer and external systems. In hexagonal architecture, ports are contracts that specify how the domain communicates with the external world.

**How to implement:**

```typescript
// Secondary/Driven Port - used by domain
export abstract class UserRepository {
    abstract save(user: UserEditor): Promise<UserEditor>;
    abstract findById(id: string, tenantId: string): Promise<UserEditor | null>;
    abstract findAll(tenantId: string, pagable: Pagable): Promise<Page<User>>;
}

// Secondary port for external service
export abstract class FilesRepository {
    abstract uploadFile(
        path: string[],
        file: Attachment,
    ): Promise<BriefNoteAttachment>;
}
```

**Best practices:**

- Define ports as abstract classes instead of interfaces to facilitate dependency injection in NestJS
- Port methods should clearly define expected input and output types
- Port names should reflect their role in the system (e.g., Repository, Service, Gateway)
- Ports should be independent of implementation details

### Secondary Port Adapters

Adapters are concrete implementations of ports that connect domains with external infrastructure such as databases, file systems, or external services.

### Persistence Adapter

**How to implement PostgreSQL Repository using node-postgres with class-transformer:**

Value Objects and Aggregates requires @Type decorators, validation decorators are welcome:

```typescript
import { Pool } from 'pg';
import { plainToClass, Type } from 'class-transformer';
import { IsString, IsNotEmpty, ValidateNested } from 'class-validator';
import { Injectable } from '@nestjs/common';

// Value Objects
class Ownership {
    @IsString()
    readonly operator: string | null;

    @IsString()
    readonly provider: string | null;

    constructor(operator: string | null, provider: string | null) {
        this.operator = operator;
        this.provider = provider;
    }

    isUnowned(): boolean {
        return !this.operator && !this.provider;
    }
}

class Settings {
    @IsString()
    readonly mode: string;

    @IsNotEmpty()
    readonly preferences: Record<string, any>;

    constructor(mode: string, preferences: Record<string, any>) {
        this.mode = mode;
        this.preferences = preferences;
    }
}

// Domain Object
class Device {
    @IsString()
    readonly deviceId: string;

    @ValidateNested()
    @Type(() => Ownership)
    readonly ownership: Ownership;

    @ValidateNested()
    @Type(() => Settings)
    readonly settings: Settings;

    constructor(deviceId: string, ownership: Ownership, settings: Settings) {
        this.deviceId = deviceId;
        this.ownership = ownership;
        this.settings = settings;
    }
}

class OptimisticLockError extends Error {
    constructor(deviceId: string) {
        super(`Device with id ${deviceId} was modified by another transaction`);
        this.name = 'OptimisticLockError';
    }
}

// Repository Implementation with node-postgres and optimistic locking
@Injectable()
class PostgresDeviceRepository implements DeviceRepository {
    constructor(private readonly pool: Pool) {}

    async findById(deviceId: string): Promise<Device | null> {
        const query =
            'SELECT data, version FROM device_documents WHERE device_id = $1';

        const result = await this.pool.query(query, [deviceId]);

        if (result.rows.length === 0) {
            return null;
        }

        // Transform the JSONB data into a Device instance and include version
        const device = plainToClass(Device, result.rows[0].data);
        device.version = result.rows[0].version;
        return device;
    }

    async findByOperator(operator: string): Promise<Device[]> {
        const query = `
            SELECT data, version 
            FROM device_documents 
            WHERE data->'ownership'->>'operator' = $1
        `;

        const result = await this.pool.query(query, [operator]);

        // Transform array of results including versions
        return result.rows.map((row) => {
            const device = plainToClass(Device, row.data);
            device.version = row.version;
            return device;
        });
    }

    async save(device: Device): Promise<void> {
        // Start a transaction
        const client = await this.pool.connect();
        try {
            await client.query('BEGIN');

            const upsertQuery = `
                INSERT INTO device_documents (device_id, data, version)
                VALUES ($1, $2, COALESCE($3, 0))
                ON CONFLICT (device_id) DO UPDATE 
                SET data = $2,
                    version = device_documents.version + 1
                WHERE device_documents.version = COALESCE($3, -1)
                RETURNING version
            `;

            const result = await client.query(upsertQuery, [
                device.deviceId,
                device,
                device.version,
            ]);

            // If no rows were affected, it means version mismatch
            if (result.rowCount === 0) {
                throw new OptimisticLockError(device.deviceId);
            }

            // Update the version in memory
            device.version = result.rows[0].version;

            await client.query('COMMIT');
        } catch (error) {
            await client.query('ROLLBACK');
            throw error;
        } finally {
            client.release();
        }
    }
}

export async function initializeDeviceSchema(pool: Pool): Promise<void> {
    // see chapter #### Schema Initialization Function
}

// Module configuration using DatabasePoolProvider
import { createDatabasePoolProvider } from '@common/database-pool.factory';
import { initializeDeviceSchema } from './device.repository';

@Module({
    providers: [
        createDatabasePoolProvider(initializeDeviceSchema),
        DeviceRepositoryProvider,
    ],
    exports: [DeviceRepository],
})
export class DeviceModule {}
```

Additional best practices for PostgreSQL with class-transformer:

- Use class-transformer decorators for automatic nested object transformation
- Combine with class-validator for validation of domain objects
- Leverage PostgreSQL JSONB operators for efficient querying
- Maintain domain object immutability through readonly properties
- Consider adding indexes for frequently queried JSONB paths
- Use parameterized queries to prevent SQL injection

#### Schema Initialization Function:

**Schema Initialization**

When working with PostgreSQL, it's important to have a reliable way to initialize and update the database schema. The `DatabasePoolProvider` from the common module provides a clean way to handle schema initialization automatically when the database pool is created.

```typescript
export async function initializeSchema(pool: Pool): Promise<void> {
    const client = await pool.connect();
    try {
        // IMPORTANT: DO NOT EDIT EXISTING QUERIES
        // always add new tables/columns/indexes
        // at the end of the function

        // IMPORTANT: remember to add IF NOT EXISTS

        // Create main table if not exists
        await client.query(`
            CREATE TABLE device_documents (
                device_id TEXT PRIMARY KEY,
                version INTEGER DEFAULT 0,
                data JSONB NOT NULL,
                created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
            );
        `);

        // Add any necessary indexes
        await client.query(`
                CREATE INDEX idx_device_operator ON device_documents ((data->'ownership'->>'operator'));
        `);
    } finally {
        client.release();
    }
}
```

**Testing with Testcontainers:**

For effective testing of PostgreSQL repositories, use Testcontainers to spin up isolated database instances. The test configuration should include the necessary environment variables.

```typescript
import {
    PostgreSqlContainer,
    StartedPostgreSqlContainer,
} from '@testcontainers/postgresql';
import { Pool } from 'pg';

describe('PostgresRepository', () => {
    let container: StartedPostgreSqlContainer;
    let repository: Repository;
    let pool: Pool;

    beforeAll(async () => {
        // Start PostgreSQL container
        container = await new PostgreSqlContainer('postgres:17.5-alpine')
            .withExposedPorts(5432)
            .withEnvironment({
                POSTGRES_USER: 'test',
                POSTGRES_PASSWORD: 'test',
                POSTGRES_DB: 'testdb',
            })
            .start();

        // Create database connection pool
        pool = new Pool({
            connectionString: container.getConnectionUri(),
            idleTimeoutMillis: 30000,
            max: 20,
        });

        await initializeSchema(pool);
        repository = new PostgresRepository(pool);
    }, 60000);

    afterAll(async () => {
        await pool.end();
        await container.stop();
    });

    it('should save and retrieve data', async () => {
        const id = 'test-id';
        const testData = { field: 'value' };

        await repository.save(id, testData);
        const retrieved = await repository.findById(id);

        expect(retrieved).toEqual(testData);
    });

    it('should handle non-existent data', async () => {
        const result = await repository.findById('non-existent-id');
        expect(result).toBeNull();
    });
});
```

**Additional Best Practices for PostgreSQL:**

- Always use IF NOT EXISTS in schema initialization queries
- Use testcontainers for integration testing with real PostgreSQL instances
- Clean up database resources after tests
- Set appropriate timeouts for container startup in tests
- Implement proper connection pool management
- Handle database transactions properly
- Use parameterized queries to prevent SQL injection

**Best practices:**

- Each adapter should implement only one port
- Adapters should be isolated to be easily replaceable
- Register adapters as providers in NestJS through abstract class tokens
- Adapter implementation should handle mapping between domain model and infrastructure model

### External System Adapter

```typescript
@Injectable()
class AzureStorageService extends FilesRepository {
    private containerClient;

    constructor(private configService: ConfigService) {
        super();
        this.containerClient = this.initContainerClient();
    }

    async uploadFile(
        path: string[],
        file: Attachment,
    ): Promise<BriefNoteAttachment> {
        const timestamp = new Date().getTime();
        const uniqueFileName = [
            ...path,
            `${timestamp}-${file.originalname}`,
        ].join('/');
        const blobClient = this.getBlobClient(uniqueFileName);

        await blobClient.uploadData(file.buffer, {
            blobHTTPHeaders: {
                blobContentType: file.mimetype,
            },
        });

        return {
            fileName: file.originalname,
            fileUrl: blobClient.url,
            contentType: file.mimetype,
            size: file.size,
        };
    }

    private initContainerClient() {
        const connectionString = this.configService.getOrThrow<string>(
            'AZURE_STORAGE_CONNECTION_STRING',
        );
        const containerName = this.configService.getOrThrow<string>(
            'AZURE_STORAGE_CONTAINER_NAME',
        );
        const blobServiceClient =
            BlobServiceClient.fromConnectionString(connectionString);
        return blobServiceClient.getContainerClient(containerName);
    }

    private getBlobClient(fileName: string): BlockBlobClient {
        return this.containerClient.getBlockBlobClient(fileName);
    }
}

export const FilesRepositoryProvider: Provider = {
    provide: FilesRepository,
    useClass: AzureStorageService,
};
```

**Best practices:**

- Each adapter should implement only one port
- Adapters should be isolated to be easily replaceable
- External system API / types / interfaces should be encapsulated in adapter file
- Register adapters as providers in NestJS through abstract class tokens
- Adapter implementation should handle mapping between domain model and infrastructure model

## Integration with NestJS Modules

To integrate ports and adapters with NestJS architecture, while encapsulating adapter implementation under contract, they should be properly defined in the module using provider.

**How to implement:**

```typescript
@Module({
    imports: [CommonModule, ConfigModule],
    controllers: [UsersController],
    providers: [
        UserService,
        UserRepositoryProvider,
        NotificationServiceProvider,
    ],
    exports: [UserService],
})
export class UsersModule {}
```

**Best practices:**

- Define providers as exported constants, with clearly specified token and implementation class
- Inject ports through tokens into application services
- Maintain all adapters for the same module in consistent directories

## Unit Testing

## Testing Adapters

Adapters should be tested in isolation to ensure correct integration with external systems. In particular, integration tests using test containers (Testcontainers) allow effective testing of real adapter implementations without using mocks.

**How to implement:**

```typescript
describe('BriefAzureTableRepository', () => {
    let repository: BriefRepository;
    let azuriteContainer: StartedAzuriteContainer;

    beforeAll(async () => {
        // Starting Azurite container for testing Azure Table Storage integration
        azuriteContainer = await new AzuriteContainer()
            .withInMemoryPersistence()
            .withStartupTimeout(60000)
            .start();
    }, 90000);

    afterAll(async () => {
        // Stopping container after all tests
        await azuriteContainer?.stop();
    });

    beforeEach(async () => {
        const containerName = 'test-briefs';
        // Configuring test module with real repository
        const moduleFixture: TestingModule = await Test.createTestingModule({
            imports: [
                ConfigModule.forRoot({
                    isGlobal: true,
                    load: [
                        () => ({
                            AZURE_STORAGE_CONNECTION_STRING:
                                azuriteContainer.getConnectionString(),
                            AZURE_STORAGE_CLIENT_OPTIONS:
                                '{ "allowInsecureConnection": true }',
                        }),
                    ],
                }),
            ],
            providers: [BriefRepositoryProvider],
        }).compile();

        // Initializing Azure container before starting tests
        const { TableServiceClient } = require('@azure/data-tables');
        const tableServiceClient = TableServiceClient.fromConnectionString(
            azuriteContainer.getConnectionString(),
            { allowInsecureConnection: true },
        );
        try {
            await tableServiceClient.createTable('briefs');
            await tableServiceClient.createTable('briefnotes');
        } catch (error) {
            // Tables may already exist
        }

        repository = moduleFixture.get<BriefRepository>(BriefRepository);
    });

    it('should save and retrieve brief', async () => {
        // Creating example aggregate
        const identity = { tenantId: 'test-tenant' };
        const eventId = 'test-event-123';
        const briefEditor = BriefEditor.createNew(
            identity.tenantId,
            eventId,
            'Test Event',
        );

        // Testing save
        await repository.saveBrief(briefEditor);

        // Testing retrieval
        const retrievedBrief = await repository.getBrief(identity, eventId);

        expect(retrievedBrief).not.toBeNull();
        expect(retrievedBrief.eventId).toBe(eventId);
        expect(retrievedBrief.tenantId).toBe(identity.tenantId);
    });

    it('should return null when brief not found', async () => {
        // Testing behavior for non-existent item
        const retrievedBrief = await repository.getBrief(
            { tenantId: 'test-tenant' },
            'non-existent-event',
        );

        expect(retrievedBrief).toBeNull();
    });

    it('should save and retrieve notes', async () => {
        // Testing notes functionality
        const eventId = 'test-event-456';
        const note = new BriefNote(
            eventId,
            'note-1',
            'Test note content',
            new Date().toISOString(),
            'test-user',
            [],
        );

        await repository.saveNote(note);

        const retrievedNote = await repository.getNote(eventId, 'note-1');
        expect(retrievedNote).not.toBeNull();
        expect(retrievedNote.content).toBe('Test note content');

        // Testing pagination
        const notesPage = await repository.getNotes(eventId, {
            limit: 10,
            offset: 0,
        });
        expect(notesPage.data).toHaveLength(1);
        expect(notesPage.count.total).toBe(1);
    });
});
```

**Best practices:**

- Use Testcontainers to test real external dependencies (databases, services, etc.)
- Test all adapter methods in isolation from the rest of the system
- Prepare clean environment before each test to ensure their independence
- Test positive and negative scenarios (e.g., saving and retrieving correct data and error handling)
- Use adapters directly, bypassing API layer, to test specifically adapter functionality
- Remember about resources - always clean up after tests (stop containers, remove test data)
- Prefer assertions by comparing entire object instead of many single assertions
    - using toEqual method if we want to check all fields
    - using toMatchObject method if we want to match only some fields like IDs or dates to expected format

## Implementation Guidelines

1. **Domain layer should be independent of infrastructure:**
    - Do not import framework libraries in domain classes
    - Use ports to communicate with external systems

2. **Naming should reflect domain language:**
    - Use terms from domain experts' language
    - Avoid technical terms in domain classes

3. **Testing:**
    - Test aggregates in isolation
    - Test value objects with logic in isolation
    - Implement integration tests for adapters
    - Implement end-to-end tests for entire module functionality through API, along with persistence adapter, use testcontainers where possible

**General best practices:**

- do not use comments in code, if code block is larger prefer well-named private method
- do not add \_ prefix to private fields

## End-to-End Testing of Business Modules

End-to-end (e2e) tests verify the operation of the entire business module, from API controllers to infrastructure adapters, using real external dependencies. Containerization (Testcontainers) enables isolated and repeatable test environment.

**How to implement:**

```typescript
import {
    PostgreSqlContainer,
    StartedPostgreSqlContainer,
} from '@testcontainers/postgresql';

describe('Briefs (e2e)', () => {
    let app: INestApplication;
    let azuriteContainer: StartedAzuriteContainer;
    let postgresContainer: StartedPostgreSqlContainer;
    let eventEmitter: EventEmitter;

    const eventId = generateRandomEventId();

    beforeAll(async () => {
        // Starting PostgreSQL container for database testing
        postgresContainer = await new PostgreSqlContainer(
            'postgres:17.5-alpine',
        )
            .withExposedPorts(5432)
            .withEnvironment({
                POSTGRES_USER: 'test',
                POSTGRES_PASSWORD: 'test',
                POSTGRES_DB: 'testdb',
            })
            .start();

        // Starting Azurite container imitating Azure Storage
        azuriteContainer = await new AzuriteContainer()
            .withInMemoryPersistence()
            .withStartupTimeout(60000)
            .start();
    }, 90000);

    afterAll(async () => {
        // Cleanup after all tests
        await azuriteContainer?.stop();
        await postgresContainer?.stop();
    });

    beforeEach(async () => {
        // Container and necessary resources configuration
        const containerName = 'briefs-atachements';

        // Migrate database schemas before running tests
        await migrateSchemas(postgresContainer.getConnectionUri(), [
            BriefDatabaseSchema,
            BriefNoteDatabaseSchema,
        ]);

        // Creating test module with real dependencies
        const moduleFixture: TestingModule = await Test.createTestingModule({
            imports: [
                ConfigModule.forRoot({
                    isGlobal: true,
                    load: [
                        () => ({
                            // Database configuration
                            DATABASE_URL: postgresContainer.getConnectionUri(),
                            POSTGRES_POOL_MAX: 10,
                            POSTGRES_IDLE_TIMEOUT_MS: 30000,
                            POSTGRES_CONNECTION_TIMEOUT_MS: 2000,
                            // Azure Storage configuration
                            AZURE_STORAGE_CONNECTION_STRING:
                                azuriteContainer.getConnectionString(),
                            AZURE_STORAGE_CONTAINER_NAME: containerName,
                            AZURE_STORAGE_CLIENT_OPTIONS:
                                '{ "allowInsecureConnection": true }',
                        }),
                    ],
                }),
                EventEmitterModule.forRoot(),
                DatabaseModule.forRoot(), // Required for database connectivity
                BriefsModule, // Tested business module
            ],
        }).compile();

        app = moduleFixture.createNestApplication();
        eventEmitter = app.get<EventEmitter>(EventEmitter);

        // Initializing external resources
        const { BlobServiceClient } = require('@azure/storage-blob');
        const blobServiceClient = BlobServiceClient.fromConnectionString(
            azuriteContainer.getConnectionString(),
        );
        const containerClient =
            blobServiceClient.getContainerClient(containerName);
        try {
            await containerClient.create();
        } catch (error) {}

        // Apply production-like setup configuration
        setup(app);
        await app.init();
    });

    afterEach(async () => {
        // Cleanup after each test
        await app.close();
    });

    // Negative case test
    it('should handle nonexistent resources gracefully', async () => {
        const response = await request(app.getHttpServer())
            .get(`/events/nonexistent-id/brief`)
            .expect(404);

        expect(response.body).toHaveProperty('message');
    });

    // Business flow test - from initiating event to full functionality
    describe('complete business flow', () => {
        it('should react to domain events', async () => {
            // Emitting domain event
            const eventCreated = {
                tenantId: 'tenant',
                eventId: eventId,
                name: 'Test Event',
            };
            await emitAndWait('event.created', eventCreated);

            // Checking system reaction to event
            const response = await request(app.getHttpServer())
                .get(`/events/${eventId}/brief`)
                .expect(200);

            expect(response.body).toMatchObject({
                eventId: eventId,
                tenantId: 'tenant',
                name: 'Test Event',
            });
        });

        it('should support complete business process', async () => {
            // Testing business data update
            await request(app.getHttpServer())
                .put(`/events/${eventId}/brief`)
                .send({
                    details: { location: 'Test Location' },
                })
                .expect(200);

            // Testing related resources creation
            const noteResponse = await request(app.getHttpServer())
                .post(`/events/${eventId}/brief/notes`)
                .field('content', 'Test note content')
                .attach('attachments', path.join(__dirname, 'test-file.txt'))
                .expect(201);

            expect(noteResponse.body).toMatchObject({
                eventId: eventId,
                content: 'Test note content',
                attachments: [
                    {
                        fileName: 'test-file.txt',
                        fileUrl: expect.any(String),
                    },
                ],
            });

            // Testing related resources list retrieval
            const listResponse = await request(app.getHttpServer())
                .get(`/events/${eventId}/brief/notes`)
                .expect(200);

            expect(listResponse.body.data).toHaveLength(1);
            expect(listResponse.body.data[0].content).toBe('Test note content');
        });
    });

    // Helper function for emitting and waiting for event processing
    async function emitAndWait(eventName: string, payload: any) {
        const processed = eventEmitter.waitFor(eventName + '.processed');
        eventEmitter.emit(eventName, payload);
        await processed;
    }
});
```

**Key e2e test configuration elements:**

1. **Database schema migration**: BeforeAll running tests, call `migrateSchemas()` with appropriate module schemas to create database tables and structures before first test runn.

2. **DatabaseModule.forRoot()**: Required in tests after setting environments, provides database connection pool configuration to all modules.

3. **setup(app)**: Call the setup function analogous to production application configuration, which sets up global filters, middleware and other configurations.

**Best practices:**

- Test complete business flows from start to finish
- Use Testcontainers to isolate external dependencies (databases, queues, services)
- Simulate events that come from outside the tested module
- Verify both positive and negative scenarios
- Check system reactions to domain events
- Test full resource lifecycle (creation, update, read, delete)
- Test integration between business module components
- Organize tests by business flows, not technical components
- Use helper functions for repeatable operations (e.g., external event emission)
- Use random IDs to ensure test isolation
- Prefer assertions by comparing entire object instead of many single assertions
    - using toEqual method if we want to check all fields
    - using toMatchObject method if we want to match only some fields like IDs or dates to expected format

Remember that e2e tests are the highest level of tests and should verify key business functionalities from the end-user perspective, not implementation details. These tests complement, not replace, unit and integration tests.

### Pagination with PostgreSQL

When implementing pagination in PostgreSQL repositories, it's important to handle both the data retrieval and total count efficiently. The `common.model.ts` provides helper types and functions for pagination:

```typescript
// Types and helper function from common.model.ts
export type Pagable = { limit: number; offset: number };

export function pagable(limit?: number, offset?: number): Pagable {
    return {
        limit: Math.min(100, Math.max(1, limit ?? 20)),
        offset: Math.max(0, offset ?? 0),
    };
}

export class Page<T> {
    constructor(
        readonly data: T[],
        readonly paging: {
            readonly limit: number;
            readonly offset: number;
            readonly total: number;
        },
    ) {}

    map<R>(mapper: (T: any) => R): Page<R> {
        return new Page(this.data.map(mapper), this.paging);
    }
}
```

Here's an example implementation using the `Page` and `Pagable` types with advanced filtering:

```typescript
// Repository method implementation with filtering
async findAll(
    pagable: Pagable,
    type?: string | string[],
    query?: string,
): Promise<Page<{ id: string; metadata: DocumentMetadata }>> {
    const sql = `
        WITH filtered_documents AS (
            SELECT document_id, data
            FROM document_documents
            WHERE (data -> 'metadata' ->> 'type' = ANY($1) OR $1 IS NULL)
              AND (data -> 'metadata' ->> 'title' ILIKE $2 OR $2 IS NULL)
        )
        SELECT
            filtered.document_id as id,
            filtered.data -> 'metadata' as metadata,
            (SELECT COUNT(*) FROM filtered_documents) as total_count
        FROM filtered_documents filtered
        ORDER BY (filtered.data -> 'metadata' ->> 'title') ASC
        LIMIT $3
        OFFSET $4
    `;

    const result = await this.pool.query(sql, [
        type ? [type] : null,
        query ? `%${query}%` : null,
        pagable.limit,
        pagable.offset,
    ]);

    // Transform results to domain objects
    const documents = result.rows.map((row) => {
        return {
            id: row.id,
            metadata: row.metadata,
        };
    });

    // Create and return a Page instance
    return new Page(documents, {
        limit: pagable.limit,
        offset: pagable.offset,
        total: parseInt(result.rows[0]?.total_count || '0', 10),
    });
}

// Usage in service layer
@Injectable()
class DocumentService {
    constructor(private readonly repository: DocumentRepository) {}

    async listDocuments(
        pagable: Pagable,
        type?: string | string[],
        query?: string
    ): Promise<Page<{ id: string; metadata: DocumentMetadata }>> {
        return this.repository.findAll(pagable, type, query);
    }
}

// Controller implementation using helper function
@Controller('documents')
class DocumentController {
    constructor(private readonly service: DocumentService) {}

    @Get()
    async getDocuments(
        @Query('offset', new DefaultValuePipe(0), ParseIntPipe) offset: number,
        @Query('limit', new DefaultValuePipe(20), ParseIntPipe) limit: number,
        @Query('type') type?: string,
        @Query('query') query?: string
    ): Promise<Page<DocumentMetadata & { id: string }>> {
        const pagableParams = pagable(limit, offset);

        return this.service.listDocuments(pagableParams, type, query)
            .map(doc => ({id: doc.id, ...doc.metadata}));
    }
}
```

**Best practices for pagination:**

- Use the `pagable()` helper function to create pagination parameters with proper validation
- Use CTEs (Common Table Expressions) to efficiently get both data and total count in a single query
- Apply proper indexing on frequently used filtering and sorting columns
- Consider using cursor-based pagination for large datasets
- Implement reasonable defaults and limits for page size (helper function enforces max 100 items)
- Add proper validation for pagination parameters
- Use appropriate HTTP headers or response envelope for pagination metadata
- Consider caching total counts for large tables
- Use proper sorting to ensure consistent ordering across pages
- Leverage the `Page.map()` method for transforming paginated results

For JSONB columns, you might want to add appropriate indexes:

```sql
-- Index for sorting by title field in JSONB
CREATE INDEX idx_document_title ON document_documents ((data->'metadata'->>'title') ASC);

-- Index for filtering by type in JSONB
CREATE INDEX idx_document_type ON document_documents ((data->'metadata'->>'type'));

-- Index for text search on title
CREATE INDEX idx_document_title_search ON document_documents USING gin ((data->'metadata'->>'title') gin_trgm_ops);
```

## Logging Guidelines

Proper logging is essential for monitoring, debugging, and understanding system behavior. Follow these guidelines to ensure consistent and useful logging across the application.

### Controller Logging

Controllers should log incoming requests with method, path, parameters, and request data for debugging and monitoring purposes.

**How to implement:**

```typescript
@Controller()
export class PlansController {
    constructor(
        private readonly planService: PlanService,
        private readonly logger: LoggerService,
    ) {}

    @Put(':analysisId')
    async updatePlan(
        @Param('analysisId') analysisId: string,
        @Body() request: UpdatePlanDto,
        @UserId() userId: string,
    ): Promise<PlanDto> {
        this.logger.log(`PUT /plans/${analysisId}`, request);

        const plan = await this.planService.updatePlan(analysisId, request);

        if (!plan) {
            throw new NotFoundException('Plan not found');
        }

        return plan;
    }
}
```

**Best practices:**

- Log HTTP method, path, and request parameters
- Include request body for debugging purposes
- Use consistent format: `METHOD /path/params`, request
- Log at the beginning of controller methods

### Event Handler Logging

Event handlers with `@OnEvent` decorators should log the event name, payload, and the action being performed. Never throw exceptions from event handlers - always log errors instead.

**How to implement:**

```typescript
@Injectable()
export class PlanService {
    constructor(
        private readonly repository: PlanRepository,
        private readonly logger: LoggerService,
    ) {}

    @OnEvent('AnalysisComplete')
    async handleAnalysisComplete(
        payload: AnalysisCompleteEvent,
    ): Promise<void> {
        this.logger.log(`handle AnalysisComplete: creating new plan`, payload);

        try {
            const plan = PlanEditor.createNew(
                payload.analysisId,
                payload.tenantId,
                payload.data,
            );

            await this.repository.save(plan);

            this.logger.log(
                `Successfully created plan for analysis ${payload.analysisId}`,
            );
        } catch (error) {
            this.logger.error(
                `Failed to handle analysis complete event for plan: ${error.message}`,
                error.stack,
            );
            // Never throw exceptions from event handlers
        }
    }
}
```

**Best practices:**

- Log event name and payload at the beginning of event handlers
- Describe the action being performed in the log message
- Always wrap event handler logic in try-catch blocks
- Log errors with full stack trace but never re-throw exceptions
- Log successful completion of event processing

### Error Logging Guidelines

**When to log errors:**

- Log errors when you catch them but don't re-throw the same exception
- Log exceptional situations that complete successfully but in an unusual way
- Log errors in event handlers (never re-throw)

**When NOT to log errors:**

- Don't log errors if you're going to re-throw the same exception
- Don't log routine error conditions that are handled gracefully
- Don't log errors just before throwing them

**Example of proper error handling:**

```typescript
// Good: Log error and handle gracefully
async processData(data: any): Promise<Result> {
    try {
        return await this.validateAndProcess(data);
    } catch (error) {
        this.logger.error(`Failed to process data: ${error.message}`, error.stack);
        return Result.failure('Processing failed');
    }
}

// Bad: Logging normal behaviour expressed in return signature
async findUser(id: string): Promise<User | null> {
    const user = await this.repository.findById(id);

    if (!user) {
        this.logger.warn(`User not found: ${id} - returning null`);
        return null;
    }

    return user;
}

// Bad: Logging before re-throwing
async processData(data: any): Promise<void> {
    try {
        await this.validateAndProcess(data);
    } catch (error) {
        this.logger.error(`Failed to process data: ${error.message}`); // Don't log if re-throwing
        throw error; // Re-throwing the same exception
    }
}
```

### General Logging Best Practices

- **Don't log method entry/exit** unless there's a specific debugging need
- **Log exceptional situations** that complete successfully but in an unusual way
- **Use appropriate log levels:**
    - `log()` for informational messages
    - `warn()` for warning conditions
    - `error()` for error conditions
- **Include relevant context** in log messages (IDs, parameters, etc.)
- **Use consistent message formatting** across the application
- **Avoid logging sensitive information** (passwords, tokens, etc.)
- **Log at the appropriate level** - don't log everything at error level

## Global Exception Filter

The Global Exception Filter is a centralized error handling mechanism that intercepts all unhandled exceptions thrown during HTTP request processing and transforms them into appropriate HTTP responses.

### Configuration

The Global Exception Filter is registered globally in the application setup:

```typescript
// src/setup.ts
import { GlobalExceptionFilter } from './common/global-exception.filter';

export function setup(app: INestApplication): void {
    // Register global exception filter
    app.useGlobalFilters(new GlobalExceptionFilter());

    // ... other setup
}
```

### Implementation

```typescript
// src/common/global-exception.filter.ts
@Catch()
export class GlobalExceptionFilter implements ExceptionFilter {
    private readonly logger = new Logger(GlobalExceptionFilter.name);

    catch(exception: unknown, host: ArgumentsHost) {
        const ctx = host.switchToHttp();
        const response = ctx.getResponse<Response>();
        const request = ctx.getRequest<Request>();

        let status = HttpStatus.INTERNAL_SERVER_ERROR;
        let message = 'Internal server error';
        let retryAfter: number | undefined;

        // Handle OptimisticLockError - return 409 Conflict with Retry-After header
        if (exception instanceof OptimisticLockError) {
            status = HttpStatus.CONFLICT;
            message = exception.message;
            retryAfter = 1; // Retry after 1 second
            this.logger.warn(`Optimistic lock conflict: ${exception.message}`, {
                url: request.url,
                method: request.method,
                body: request.body,
            });
        }
        // Handle CommandInvalidError - return 400 Bad Request
        else if (exception instanceof CommandInvalidError) {
            status = HttpStatus.BAD_REQUEST;
            message = exception.message;
            this.logger.warn(`Command invalid: ${exception.message}`, {
                url: request.url,
                method: request.method,
                body: request.body,
            });
        }
        // Handle HttpException (NestJS built-in exceptions)
        else if (exception instanceof HttpException) {
            status = exception.getStatus();
            message = exception.message;
        }
        // Handle other errors
        else {
            this.logger.error('Unhandled exception', {
                exception:
                    exception instanceof Error
                        ? exception.message
                        : String(exception),
                stack: exception instanceof Error ? exception.stack : undefined,
                url: request.url,
                method: request.method,
                body: request.body,
            });
        }

        const errorResponse = {
            statusCode: status,
            message,
            timestamp: new Date().toISOString(),
            path: request.url,
        };

        // Set Retry-After header for optimistic lock conflicts
        if (retryAfter) {
            response.setHeader('Retry-After', retryAfter);
        }

        response.status(status).json(errorResponse);
    }
}
```

### Role and Responsibilities

1. **Centralized Error Handling**: Catches all unhandled exceptions thrown during HTTP request processing
2. **HTTP Status Code Mapping**: Maps different exception types to appropriate HTTP status codes
3. **Consistent Error Response Format**: Ensures all error responses follow the same structure
4. **Logging**: Logs errors with relevant context (URL, method, request body)
5. **Special Headers**: Adds appropriate HTTP headers (e.g., Retry-After for optimistic lock conflicts)

### Supported Exception Types

#### Domain Exceptions

```typescript
// src/common/errors.ts
export class CommandInvalidError extends Error {
    constructor(object: any) {
        super(`Command invalid: ${JSON.stringify(object)}`);
        this.name = 'CommandInvalidError';
    }
}

export class OptimisticLockError extends Error {
    constructor(object: any) {
        super(
            `Object was modified by another transaction: ${JSON.stringify(object)}`,
        );
        this.name = 'OptimisticLockError';
    }
}
```

#### Exception Handling Rules

- **OptimisticLockError**: Returns HTTP 409 (Conflict) with Retry-After header
- **CommandInvalidError**: Returns HTTP 400 (Bad Request)
- **HttpException**: Returns the status code and message from the exception
- **Other exceptions**: Returns HTTP 500 (Internal Server Error) with detailed logging

### Error Response Format

All error responses follow a consistent format:

```json
{
    "statusCode": 400,
    "message": "Command invalid: {\"field\":\"value\"}",
    "timestamp": "2024-01-15T10:30:00.000Z",
    "path": "/api/plans/123"
}
```

### Best Practices

- **Don't log exceptions in controllers** - the global filter handles logging
- **Throw domain exceptions** instead of HTTP exceptions in business logic
- **Use appropriate exception types** for different error scenarios
- **Keep exception messages user-friendly** but informative
- **Include relevant context** in exception constructors
- **Test exception scenarios** to ensure proper HTTP status codes and responses
